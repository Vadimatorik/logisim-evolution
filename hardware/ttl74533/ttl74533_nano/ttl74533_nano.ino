/*
 * Self-check for a 74HC533 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * LE high makes the latch transparent: each Qn is the complement of Dn, with no edge.
 * LE low holds that complement. OE high releases the outputs and does not change the latch.
 * Q pins use INPUT_PULLUP, so a released low reads high and a driven low still reads low.
 * D13 drives D7. The Nano LED on D13 would load a Q pin and hide high-Z.
 * OE stays high and LE stays low until the check starts.
 */

const uint8_t PIN_OE = 2;
const uint8_t PIN_LE = 3;
const uint8_t PIN_D0 = 4;
const uint8_t PIN_D1 = 5;
const uint8_t PIN_D2 = 6;
const uint8_t PIN_D3 = 7;
const uint8_t PIN_D4 = 8;
const uint8_t PIN_D5 = 9;
const uint8_t PIN_D6 = 10;
const uint8_t PIN_D7 = 13;
const uint8_t PIN_Q0 = 12;
const uint8_t PIN_Q1 = 11;
const uint8_t PIN_Q2 = A0;
const uint8_t PIN_Q3 = A1;
const uint8_t PIN_Q4 = A2;
const uint8_t PIN_Q5 = A3;
const uint8_t PIN_Q6 = A4;
const uint8_t PIN_Q7 = A5;

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

void setData(uint8_t data) {
  digitalWrite(PIN_D0, (data & 0x01) ? HIGH : LOW);
  digitalWrite(PIN_D1, (data & 0x02) ? HIGH : LOW);
  digitalWrite(PIN_D2, (data & 0x04) ? HIGH : LOW);
  digitalWrite(PIN_D3, (data & 0x08) ? HIGH : LOW);
  digitalWrite(PIN_D4, (data & 0x10) ? HIGH : LOW);
  digitalWrite(PIN_D5, (data & 0x20) ? HIGH : LOW);
  digitalWrite(PIN_D6, (data & 0x40) ? HIGH : LOW);
  digitalWrite(PIN_D7, (data & 0x80) ? HIGH : LOW);
}

uint8_t readWord() {
  uint8_t value = 0;
  if (digitalRead(PIN_Q0)) value |= 0x01;
  if (digitalRead(PIN_Q1)) value |= 0x02;
  if (digitalRead(PIN_Q2)) value |= 0x04;
  if (digitalRead(PIN_Q3)) value |= 0x08;
  if (digitalRead(PIN_Q4)) value |= 0x10;
  if (digitalRead(PIN_Q5)) value |= 0x20;
  if (digitalRead(PIN_Q6)) value |= 0x40;
  if (digitalRead(PIN_Q7)) value |= 0x80;
  return value;
}

void expectWord(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readWord();
  if (actual != expected) noteFailure(step, expected, actual);
}

void drive(uint8_t data) {
  digitalWrite(PIN_OE, LOW);
  digitalWrite(PIN_LE, HIGH);
  setData(data);
}

void latch(uint8_t data) {
  drive(data);
  settle();
  digitalWrite(PIN_LE, LOW);
  settle();
}

void checkTransparent() {
  for (uint16_t data = 0; data < 256; data++) {
    drive((uint8_t) data);
    expectWord((uint8_t) ~data, "transparent");
  }
}

void checkHold() {
  const uint8_t samples[] = {
      0x00, 0xFF, 0x55, 0xAA, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80};
  for (uint8_t index = 0; index < sizeof(samples); index++) {
    const uint8_t data = samples[index];
    latch(data);
    setData((uint8_t) ~data);
    expectWord((uint8_t) ~data, "hold");
  }
}

void checkReleasedLowsReadHigh() {
  drive(0xFF);
  expectWord(0x00, "driven low");
  digitalWrite(PIN_LE, LOW);
  settle();
  digitalWrite(PIN_OE, HIGH);
  expectWord(0xFF, "released low");
  setData(0x00);
  expectWord(0xFF, "z ignores new data");
  digitalWrite(PIN_OE, LOW);
  expectWord(0x00, "latch kept lows");
}

void checkOutputEnableDoesNotChangeLatch() {
  latch(0xA5);
  digitalWrite(PIN_OE, HIGH);
  setData(0x5A);
  expectWord(0xFF, "z over mixed latch");
  digitalWrite(PIN_OE, LOW);
  expectWord((uint8_t) ~0xA5, "oe restore");

  digitalWrite(PIN_OE, HIGH);
  digitalWrite(PIN_LE, HIGH);
  setData(0x0F);
  expectWord(0xFF, "z while transparent");
  digitalWrite(PIN_OE, LOW);
  expectWord((uint8_t) ~0x0F, "tracked while disabled");
}

void setup() {
  pinMode(PIN_OE, OUTPUT);
  pinMode(PIN_LE, OUTPUT);
  pinMode(PIN_D0, OUTPUT);
  pinMode(PIN_D1, OUTPUT);
  pinMode(PIN_D2, OUTPUT);
  pinMode(PIN_D3, OUTPUT);
  pinMode(PIN_D4, OUTPUT);
  pinMode(PIN_D5, OUTPUT);
  pinMode(PIN_D6, OUTPUT);
  pinMode(PIN_D7, OUTPUT);
  pinMode(PIN_Q0, INPUT_PULLUP);
  pinMode(PIN_Q1, INPUT_PULLUP);
  pinMode(PIN_Q2, INPUT_PULLUP);
  pinMode(PIN_Q3, INPUT_PULLUP);
  pinMode(PIN_Q4, INPUT_PULLUP);
  pinMode(PIN_Q5, INPUT_PULLUP);
  pinMode(PIN_Q6, INPUT_PULLUP);
  pinMode(PIN_Q7, INPUT_PULLUP);

  digitalWrite(PIN_OE, HIGH);
  digitalWrite(PIN_LE, LOW);
  setData(0);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  checkTransparent();
  checkHold();
  checkReleasedLowsReadHigh();
  checkOutputEnableDoesNotChangeLatch();

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
