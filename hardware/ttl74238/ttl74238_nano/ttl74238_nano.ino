/*
 * Self-check for a 74HC238 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Outputs stay low unless E1 and E2 are low and E3 is high. Then Yn selected
 * by A2 A1 A0 goes high. A0 is the least significant address bit.
 */

const uint8_t ADDR_PINS[3] = {2, 3, 4};
const uint8_t PIN_E1 = 5;
const uint8_t PIN_E2 = 6;
const uint8_t PIN_E3 = 7;
const uint8_t Y_PINS[8] = {8, 9, 10, 11, 12, A0, A1, A2};

bool failed = false;
char resultLine[160];

void noteFailure(const char* step, int expected, int actual) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s expected=0x%02X actual=0x%02X", step,
           expected, actual);
}

void applyInputs(int pattern) {
  for (uint8_t bit = 0; bit < 3; bit++) {
    digitalWrite(ADDR_PINS[bit], (pattern >> bit) & 1 ? HIGH : LOW);
  }
  digitalWrite(PIN_E1, (pattern >> 3) & 1 ? HIGH : LOW);
  digitalWrite(PIN_E2, (pattern >> 4) & 1 ? HIGH : LOW);
  digitalWrite(PIN_E3, (pattern >> 5) & 1 ? HIGH : LOW);
}

int readY() {
  int value = 0;
  for (uint8_t bit = 0; bit < 8; bit++) {
    if (digitalRead(Y_PINS[bit]) == HIGH) value |= 1 << bit;
  }
  return value;
}

int expectedY(int pattern) {
  const bool e1High = (pattern >> 3) & 1;
  const bool e2High = (pattern >> 4) & 1;
  const bool e3High = (pattern >> 5) & 1;
  if (e1High || e2High || !e3High) return 0;
  return 1 << (pattern & 7);
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
  for (uint8_t bit = 0; bit < 3; bit++) pinMode(ADDR_PINS[bit], OUTPUT);
  pinMode(PIN_E1, OUTPUT);
  pinMode(PIN_E2, OUTPUT);
  pinMode(PIN_E3, OUTPUT);
  for (uint8_t bit = 0; bit < 8; bit++) pinMode(Y_PINS[bit], INPUT);
  applyInputs(0x18);

  Serial.println("74HC238 bench ready. Send any character to start.");
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
