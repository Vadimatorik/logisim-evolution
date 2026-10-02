/*
 * Self-check for a 74HC595 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * SRCLK shifts SER into QA and toward QH. RCLK copies that register into storage.
 * SRCLR low clears only the shift register. OE high releases QA..QH.
 * QH' follows the shift register and stays driven. Hi-Z is detected by precharge.
 */

const uint8_t PIN_SRCLR = 2;
const uint8_t PIN_OE = 3;
const uint8_t PIN_SER = 4;
const uint8_t PIN_QHP = 5;
const uint8_t PIN_SRCLK = 8;
const uint8_t PIN_RCLK = 9;
// QA..QH. QA is package pin 15. QF is D13, so the board LED follows QF.
const uint8_t PIN_Q[8] = {6, 7, 10, 11, 12, 13, A0, A1};

bool failed = false;
char resultLine[140];

void noteFailure(const char* step, const char* detail) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s %s", step, detail);
}

void idle() {
  digitalWrite(PIN_SRCLR, HIGH);
  digitalWrite(PIN_OE, LOW);
  digitalWrite(PIN_SER, LOW);
  digitalWrite(PIN_SRCLK, LOW);
  digitalWrite(PIN_RCLK, LOW);
}

void pulsePin(uint8_t pin) {
  digitalWrite(pin, HIGH);
  delayMicroseconds(2);
  digitalWrite(pin, LOW);
  delayMicroseconds(2);
}

// D8 (SRCLK) and D9 (RCLK) are PORTB bits 0 and 1. One write raises both edges.
void pulseBothClocks() {
  PORTB &= ~(_BV(PB0) | _BV(PB1));
  delayMicroseconds(2);
  PORTB |= (_BV(PB0) | _BV(PB1));
  delayMicroseconds(2);
  PORTB &= ~(_BV(PB0) | _BV(PB1));
}

void shiftZeros(uint8_t count) {
  digitalWrite(PIN_SER, LOW);
  for (uint8_t bit = 0; bit < count; bit++) pulsePin(PIN_SRCLK);
}

uint8_t readOutputs() {
  uint8_t value = 0;
  for (uint8_t stage = 0; stage < 8; stage++) {
    pinMode(PIN_Q[stage], INPUT);
    if (digitalRead(PIN_Q[stage]) == HIGH) value |= (1 << stage);
  }
  return value;
}

bool readSerialHigh() {
  pinMode(PIN_QHP, INPUT);
  return digitalRead(PIN_QHP) == HIGH;
}

void expectParallel(const char* step, uint8_t expected) {
  delay(1);
  const uint8_t actual = readOutputs();
  Serial.print(step);
  Serial.print(" parallel expected=0x");
  Serial.print(expected, HEX);
  Serial.print(" actual=0x");
  Serial.print(actual, HEX);
  const bool pass = expected == actual;
  Serial.println(pass ? " PASS" : " FAIL");
  if (!pass) {
    char detail[40];
    snprintf(detail, sizeof(detail), "expected=0x%02X actual=0x%02X", expected, actual);
    noteFailure(step, detail);
  }
}

bool serialIsDriven(bool expectHigh) {
  pinMode(PIN_QHP, OUTPUT);
  digitalWrite(PIN_QHP, expectHigh ? LOW : HIGH);
  delayMicroseconds(20);
  pinMode(PIN_QHP, INPUT);
  delayMicroseconds(5);
  return digitalRead(PIN_QHP) == (expectHigh ? HIGH : LOW);
}

void expectSerial(const char* step, bool high) {
  delay(1);
  const bool actual = readSerialHigh();
  const bool driven = serialIsDriven(high);
  Serial.print(step);
  Serial.print(" QH' expected=");
  Serial.print(high ? "1" : "0");
  Serial.print(" actual=");
  Serial.print(actual ? "1" : "0");
  Serial.print(driven ? " driven" : " floating");
  const bool pass = actual == high && driven;
  Serial.println(pass ? " PASS" : " FAIL");
  if (!pass) {
    char detail[48];
    snprintf(
        detail,
        sizeof(detail),
        "expected=%d actual=%d driven=%d",
        high ? 1 : 0,
        actual ? 1 : 0,
        driven ? 1 : 0);
    noteFailure(step, detail);
  }
}

bool pinIsReleased(uint8_t pin) {
  pinMode(pin, OUTPUT);
  digitalWrite(pin, LOW);
  delayMicroseconds(20);
  pinMode(pin, INPUT);
  delayMicroseconds(5);
  const bool heldLow = digitalRead(pin) == LOW;

  pinMode(pin, OUTPUT);
  digitalWrite(pin, HIGH);
  delayMicroseconds(20);
  pinMode(pin, INPUT);
  delayMicroseconds(5);
  const bool heldHigh = digitalRead(pin) == HIGH;
  return heldLow && heldHigh;
}

void expectReleased(const char* step) {
  delay(1);
  int stuck = -1;
  for (uint8_t stage = 0; stage < 8; stage++) {
    if (!pinIsReleased(PIN_Q[stage])) {
      stuck = stage;
      break;
    }
  }
  Serial.print(step);
  Serial.println(stuck < 0 ? " released PASS" : " released FAIL");
  if (stuck >= 0) {
    char detail[24];
    snprintf(detail, sizeof(detail), "stuckQA+%d", stuck);
    noteFailure(step, detail);
  }
}

void clearAndLatch() {
  digitalWrite(PIN_SRCLR, LOW);
  delayMicroseconds(2);
  pulsePin(PIN_RCLK);
  digitalWrite(PIN_SRCLR, HIGH);
}

void runChecks() {
  idle();
  clearAndLatch();
  expectParallel("cleared", 0x00);
  expectSerial("cleared", false);

  // 0x59 shifted LSB first becomes 0x9A once it is latched. QH' rises on bit 7.
  for (uint8_t bit = 0; bit < 8; bit++) {
    digitalWrite(PIN_SER, (0x59 & (1 << bit)) ? HIGH : LOW);
    delayMicroseconds(2);
    pulsePin(PIN_SRCLK);
    char step[24];
    snprintf(step, sizeof(step), "shift bit %u", bit);
    expectSerial(step, bit == 7);
    expectParallel(step, 0x00);
  }
  digitalWrite(PIN_SER, LOW);
  pulsePin(PIN_RCLK);
  expectParallel("latched", 0x9A);
  expectSerial("latched", true);

  digitalWrite(PIN_SRCLR, LOW);
  delayMicroseconds(2);
  expectSerial("clear shift", false);
  expectParallel("clear shift", 0x9A);

  digitalWrite(PIN_SER, HIGH);
  pulsePin(PIN_SRCLK);
  expectSerial("clock while clear", false);
  expectParallel("clock while clear", 0x9A);

  digitalWrite(PIN_SRCLR, HIGH);
  pulsePin(PIN_SRCLK);
  pulsePin(PIN_RCLK);
  digitalWrite(PIN_SER, LOW);
  expectParallel("one bit after clear", 0x01);
  expectSerial("one bit after clear", false);

  // The single 1 walks to QH. Storage still holds 0x01 because RCLK does not pulse.
  shiftZeros(7);
  expectSerial("walked to QH", true);
  digitalWrite(PIN_OE, HIGH);
  delayMicroseconds(2);
  expectReleased("OE high");
  expectSerial("QH' while OE high", true);
  digitalWrite(PIN_OE, LOW);
  delayMicroseconds(2);
  expectParallel("outputs restored", 0x01);

  clearAndLatch();
  digitalWrite(PIN_SER, HIGH);
  pulseBothClocks();
  digitalWrite(PIN_SER, LOW);
  expectParallel("tied clock 1", 0x00);
  expectSerial("tied clock 1", false);

  pulseBothClocks();
  expectParallel("tied clock 2", 0x01);
  expectSerial("tied clock 2", false);

  pulseBothClocks();
  expectParallel("tied clock 3", 0x02);
  expectSerial("tied clock 3", false);
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_SRCLR, OUTPUT);
  pinMode(PIN_OE, OUTPUT);
  pinMode(PIN_SER, OUTPUT);
  pinMode(PIN_SRCLK, OUTPUT);
  pinMode(PIN_RCLK, OUTPUT);
  pinMode(PIN_QHP, INPUT);
  for (uint8_t stage = 0; stage < 8; stage++) pinMode(PIN_Q[stage], INPUT);
  idle();
  Serial.println("74HC595 bench. Send any character to start.");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  Serial.println(failed ? resultLine : "RESULT PASS");
}
