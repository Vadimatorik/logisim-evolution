/*
 * Self-check for a 74HC599 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * SRCLK shifts SER into QA toward QH on the rising edge. RCLK copies the
 * shift register into storage on the rising edge. A shared rising edge
 * latches the value from before the shift. SRCLR and RCLR are active low
 * and each clear only their own register. QA through QH are open-collector
 * and need 10k pull-ups to 5V. QH' is push-pull. Both clears and both
 * clocks stay low until the check starts.
 */

const uint8_t PIN_SRCLR = 2;
const uint8_t PIN_SRCLK = 3;
const uint8_t PIN_RCLK = 4;
const uint8_t PIN_RCLR = 5;
const uint8_t PIN_SER = 6;
const uint8_t PIN_QB = 8;
const uint8_t PIN_QC = 9;
const uint8_t PIN_QD = 10;
const uint8_t PIN_QE = 11;
const uint8_t PIN_QF = 12;
const uint8_t PIN_QG = 13;
const uint8_t PIN_QH = A0;
const uint8_t PIN_QHP = A1;
const uint8_t PIN_QA = A2;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, uint8_t expected, uint8_t actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s expected %02X got %02X",
      step,
      expected,
      actual);
}

void settle() { delay(1); }

void pulse(uint8_t pin) {
  digitalWrite(pin, LOW);
  settle();
  digitalWrite(pin, HIGH);
  settle();
  digitalWrite(pin, LOW);
  settle();
}

// D3 and D4 rise in one PORTD write, so the chip sees one shared edge.
void riseBoth() {
  PORTD &= ~(_BV(PORTD3) | _BV(PORTD4));
  settle();
  PORTD |= _BV(PORTD3) | _BV(PORTD4);
  settle();
  PORTD &= ~(_BV(PORTD3) | _BV(PORTD4));
  settle();
}

uint8_t readParallel() {
  uint8_t value = 0;
  if (digitalRead(PIN_QA)) value |= 1;
  if (digitalRead(PIN_QB)) value |= 2;
  if (digitalRead(PIN_QC)) value |= 4;
  if (digitalRead(PIN_QD)) value |= 8;
  if (digitalRead(PIN_QE)) value |= 16;
  if (digitalRead(PIN_QF)) value |= 32;
  if (digitalRead(PIN_QG)) value |= 64;
  if (digitalRead(PIN_QH)) value |= 128;
  return value;
}

void expectParallel(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readParallel();
  if (actual != expected) noteFailure(step, expected, actual);
}

void expectQhp(bool high, const char* step) {
  settle();
  const uint8_t actual = digitalRead(PIN_QHP) ? 1 : 0;
  const uint8_t expected = high ? 1 : 0;
  if (actual != expected) noteFailure(step, expected, actual);
}

void clearBoth() {
  digitalWrite(PIN_SRCLR, LOW);
  digitalWrite(PIN_RCLR, LOW);
  settle();
  digitalWrite(PIN_SRCLR, HIGH);
  digitalWrite(PIN_RCLR, HIGH);
  settle();
}

void shiftByte(uint8_t bits) {
  for (uint8_t index = 0; index < 8; index++) {
    digitalWrite(PIN_SER, (bits & 1) ? HIGH : LOW);
    bits >>= 1;
    pulse(PIN_SRCLK);
  }
}

void checkShiftThenLatch() {
  clearBoth();
  expectParallel(0x00, "cleared parallel");
  expectQhp(false, "cleared qhp");

  uint8_t bits = 0x59;
  for (uint8_t index = 0; index < 8; index++) {
    digitalWrite(PIN_SER, (bits & 1) ? HIGH : LOW);
    bits >>= 1;
    pulse(PIN_SRCLK);
    expectParallel(0x00, "shift before latch");
    expectQhp(index == 7, "qhp during shift");
  }

  pulse(PIN_RCLK);
  expectParallel(0x9A, "latched 59");
  expectQhp(true, "qhp after latch");
}

void checkTiedClocks() {
  clearBoth();
  digitalWrite(PIN_SER, HIGH);
  riseBoth();
  expectParallel(0x00, "tied first parallel");
  expectQhp(false, "tied first qhp");

  digitalWrite(PIN_SER, LOW);
  riseBoth();
  expectParallel(0x01, "tied second parallel");
  expectQhp(false, "tied second qhp");
}

void checkShiftClear() {
  clearBoth();
  shiftByte(0x59);
  pulse(PIN_RCLK);
  expectParallel(0x9A, "before shift clear");

  digitalWrite(PIN_SRCLR, LOW);
  settle();
  expectQhp(false, "shift clear qhp");
  expectParallel(0x9A, "shift clear keeps storage");

  digitalWrite(PIN_SER, HIGH);
  pulse(PIN_SRCLK);
  expectQhp(false, "shift while clear");
  expectParallel(0x9A, "storage during shift clear");

  pulse(PIN_RCLK);
  expectParallel(0x00, "latch cleared shift");
  expectQhp(false, "qhp stays clear");

  digitalWrite(PIN_SRCLR, HIGH);
  settle();
  pulse(PIN_SRCLK);
  pulse(PIN_RCLK);
  expectParallel(0x01, "shift after clear released");
}

void checkStorageClear() {
  clearBoth();
  shiftByte(0x59);
  pulse(PIN_RCLK);
  expectParallel(0x9A, "before storage clear");
  expectQhp(true, "qhp before storage clear");

  digitalWrite(PIN_RCLR, LOW);
  settle();
  expectParallel(0x00, "storage clear");
  expectQhp(true, "qhp during storage clear");

  pulse(PIN_RCLK);
  expectParallel(0x00, "latch while storage clear");
  expectQhp(true, "qhp while storage clear");

  digitalWrite(PIN_RCLR, HIGH);
  settle();
  expectParallel(0x00, "storage stays clear");
  pulse(PIN_RCLK);
  expectParallel(0x9A, "storage restored");
  expectQhp(true, "qhp after storage clear");
}

void setup() {
  pinMode(PIN_SRCLR, OUTPUT);
  pinMode(PIN_SRCLK, OUTPUT);
  pinMode(PIN_RCLK, OUTPUT);
  pinMode(PIN_RCLR, OUTPUT);
  pinMode(PIN_SER, OUTPUT);
  pinMode(PIN_QA, INPUT);
  pinMode(PIN_QB, INPUT);
  pinMode(PIN_QC, INPUT);
  pinMode(PIN_QD, INPUT);
  pinMode(PIN_QE, INPUT);
  pinMode(PIN_QF, INPUT);
  pinMode(PIN_QG, INPUT);
  pinMode(PIN_QH, INPUT);
  pinMode(PIN_QHP, INPUT);

  digitalWrite(PIN_SRCLR, LOW);
  digitalWrite(PIN_RCLR, LOW);
  digitalWrite(PIN_SRCLK, LOW);
  digitalWrite(PIN_RCLK, LOW);
  digitalWrite(PIN_SER, LOW);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  digitalWrite(PIN_SRCLR, HIGH);
  digitalWrite(PIN_RCLR, HIGH);
  settle();

  checkShiftThenLatch();
  checkTiedClocks();
  checkShiftClear();
  checkStorageClear();

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
