/*
 * Self-check for a 74HC239 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each half stays low unless its enable is low. Then Yn selected by A1 A0 goes
 * high. A0 is the least significant address bit. The halves are independent.
 */

const uint8_t PIN_NE1 = 2;
const uint8_t PIN_A0_1 = 3;
const uint8_t PIN_A1_1 = 4;
const uint8_t PIN_NE2 = 5;
const uint8_t PIN_A0_2 = 6;
const uint8_t PIN_A1_2 = 7;
const uint8_t Y1_PINS[4] = {8, 9, 10, 11};
const uint8_t Y2_PINS[4] = {12, A0, A1, A2};

bool failed = false;
char resultLine[160];

void noteFailure(const char* step, int expected, int actual) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s expected=0x%02X actual=0x%02X", step,
           expected, actual);
}

void applyInputs(int pattern) {
  digitalWrite(PIN_A0_1, (pattern >> 0) & 1 ? HIGH : LOW);
  digitalWrite(PIN_A1_1, (pattern >> 1) & 1 ? HIGH : LOW);
  digitalWrite(PIN_NE1, (pattern >> 2) & 1 ? HIGH : LOW);
  digitalWrite(PIN_A0_2, (pattern >> 3) & 1 ? HIGH : LOW);
  digitalWrite(PIN_A1_2, (pattern >> 4) & 1 ? HIGH : LOW);
  digitalWrite(PIN_NE2, (pattern >> 5) & 1 ? HIGH : LOW);
}

int readY() {
  int value = 0;
  for (uint8_t bit = 0; bit < 4; bit++) {
    if (digitalRead(Y1_PINS[bit]) == HIGH) value |= 1 << bit;
    if (digitalRead(Y2_PINS[bit]) == HIGH) value |= 1 << (bit + 4);
  }
  return value;
}

int expectedHalf(int address, bool enableHigh) {
  if (enableHigh) return 0;
  return 1 << address;
}

int expectedY(int pattern) {
  const int address1 = pattern & 0x3;
  const bool enable1High = (pattern >> 2) & 1;
  const int address2 = (pattern >> 3) & 0x3;
  const bool enable2High = (pattern >> 5) & 1;
  return expectedHalf(address1, enable1High) | (expectedHalf(address2, enable2High) << 4);
}

void expectY(const char* step, int expected) {
  delay(1);
  const int actual = readY();
  Serial.print(step);
  Serial.print(" expected=0x");
  Serial.print(expected, HEX);
  Serial.print(" actual=0x");
  Serial.print(actual, HEX);
  Serial.println(expected == actual ? " PASS" : " FAIL");
  if (expected != actual) noteFailure(step, expected, actual);
}

void checkEveryCombination() {
  for (int pattern = 0; pattern < 64; pattern++) {
    applyInputs(pattern);
    char step[24];
    snprintf(step, sizeof(step), "in=0x%02X", pattern);
    expectY(step, expectedY(pattern));
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_NE1, OUTPUT);
  pinMode(PIN_A0_1, OUTPUT);
  pinMode(PIN_A1_1, OUTPUT);
  pinMode(PIN_NE2, OUTPUT);
  pinMode(PIN_A0_2, OUTPUT);
  pinMode(PIN_A1_2, OUTPUT);
  for (uint8_t bit = 0; bit < 4; bit++) {
    pinMode(Y1_PINS[bit], INPUT);
    pinMode(Y2_PINS[bit], INPUT);
  }
  applyInputs(0x24);

  Serial.println("74HC239 bench ready. Send any character to start.");
  while (Serial.available() == 0) {
  }
  while (Serial.available() > 0) Serial.read();

  checkEveryCombination();
  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {
}
