/*
 * Self-check for a 74HC237 wired to an Arduino Nano, as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL".
 *
 * Output bits are printed with Y7 on the left and Y0 on the right.
 * LE is raised before the address is changed, so the held code is the one
 * present while the latch was still transparent.
 */

const uint8_t PIN_A0 = 2;
const uint8_t PIN_A1 = 3;
const uint8_t PIN_A2 = 4;
const uint8_t PIN_LE = 5;
const uint8_t PIN_E1 = 6;
const uint8_t PIN_E2 = 7;

// Index 0 is Y0. Y7..Y2 use D8..D13, Y1 uses A0, Y0 uses A1.
const uint8_t OUTPUT_PINS[] = {A1, A0, 13, 12, 11, 10, 9, 8};

bool failed = false;

void settle() {
  delayMicroseconds(100);
}

void setAddress(uint8_t address) {
  digitalWrite(PIN_A0, (address & 1) ? HIGH : LOW);
  digitalWrite(PIN_A1, (address & 2) ? HIGH : LOW);
  digitalWrite(PIN_A2, (address & 4) ? HIGH : LOW);
}

void drive(uint8_t address, bool leHigh, bool e1High, bool e2High) {
  digitalWrite(PIN_LE, leHigh ? HIGH : LOW);
  digitalWrite(PIN_E1, e1High ? HIGH : LOW);
  digitalWrite(PIN_E2, e2High ? HIGH : LOW);
  setAddress(address);
  settle();
}

uint8_t readOutputs() {
  uint8_t value = 0;
  for (uint8_t index = 0; index < 8; index++) {
    if (digitalRead(OUTPUT_PINS[index]) == HIGH) {
      value |= (uint8_t)(1 << index);
    }
  }
  return value;
}

void printOutputs(uint8_t value) {
  for (int8_t bit = 7; bit >= 0; bit--) {
    Serial.print(((value >> bit) & 1) ? '1' : '0');
  }
}

void expect(const char* step, uint8_t actual, uint8_t expected) {
  const bool ok = actual == expected;
  if (!ok) {
    failed = true;
  }
  Serial.print(step);
  Serial.print(" OUT=");
  printOutputs(actual);
  Serial.print(" EXP=");
  printOutputs(expected);
  Serial.println(ok ? " OK" : " FAIL");
}

uint8_t oneHot(uint8_t address) {
  return (uint8_t)(1 << address);
}

void setup() {
  const uint8_t inputPins[] = {PIN_A0, PIN_A1, PIN_A2, PIN_LE, PIN_E1, PIN_E2};
  for (uint8_t index = 0; index < 6; index++) {
    digitalWrite(inputPins[index], LOW);
    pinMode(inputPins[index], OUTPUT);
  }
  digitalWrite(PIN_E2, HIGH);
  for (uint8_t index = 0; index < 8; index++) {
    pinMode(OUTPUT_PINS[index], INPUT);
  }

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  for (uint8_t address = 0; address < 8; address++) {
    char step[24];
    drive(address, false, false, true);
    snprintf(step, sizeof(step), "ADDR=%u LE=0 E1=0 E2=1", (unsigned)address);
    expect(step, readOutputs(), oneHot(address));

    drive(address, false, true, true);
    snprintf(step, sizeof(step), "ADDR=%u LE=0 E1=1 E2=1", (unsigned)address);
    expect(step, readOutputs(), 0);

    drive(address, false, false, false);
    snprintf(step, sizeof(step), "ADDR=%u LE=0 E1=0 E2=0", (unsigned)address);
    expect(step, readOutputs(), 0);
  }

  drive(5, false, false, true);
  expect("HOLD before edge", readOutputs(), oneHot(5));
  digitalWrite(PIN_LE, HIGH);
  settle();
  setAddress(2);
  settle();
  expect("HOLD after edge", readOutputs(), oneHot(5));
  digitalWrite(PIN_LE, LOW);
  settle();
  expect("HOLD transparent again", readOutputs(), oneHot(2));

  drive(4, false, false, true);
  digitalWrite(PIN_LE, HIGH);
  settle();
  setAddress(1);
  settle();
  expect("BLANK stored", readOutputs(), oneHot(4));
  digitalWrite(PIN_E1, HIGH);
  settle();
  expect("BLANK nE1 high", readOutputs(), 0);
  digitalWrite(PIN_E1, LOW);
  settle();
  expect("BLANK nE1 restored", readOutputs(), oneHot(4));
  digitalWrite(PIN_E2, LOW);
  settle();
  expect("BLANK E2 low", readOutputs(), 0);
  digitalWrite(PIN_E2, HIGH);
  settle();
  expect("BLANK E2 restored", readOutputs(), oneHot(4));

  Serial.println(failed ? "RESULT FAIL" : "RESULT PASS");
}

void loop() {
}
