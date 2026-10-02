/*
 * Self-check for a 74HC4015 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each half shifts D into Q0 on the rising edge of CP. A high MR clears that half.
 * Outputs are push-pull, so no bias resistor is used.
 */

const uint8_t PIN_2CP = 2;
const uint8_t PIN_2Q3 = 3;
const uint8_t PIN_1Q2 = 4;
const uint8_t PIN_1Q1 = 5;
const uint8_t PIN_1Q0 = 6;
const uint8_t PIN_1MR = 7;
const uint8_t PIN_1D = 8;
const uint8_t PIN_1CP = 9;
const uint8_t PIN_1Q3 = 10;
const uint8_t PIN_2Q2 = 11;
const uint8_t PIN_2Q1 = 12;
const uint8_t PIN_2Q0 = A0;
const uint8_t PIN_2MR = A1;
const uint8_t PIN_2D = A2;

bool failed = false;
char resultLine[180];

void noteFailure(const char* step, int expected1, int actual1, int expected2, int actual2) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine),
           "RESULT FAIL %s reg1 expected=0x%X actual=0x%X reg2 expected=0x%X actual=0x%X", step,
           expected1, actual1, expected2, actual2);
}

void drive(bool cp1, bool d1, bool mr1, bool cp2, bool d2, bool mr2) {
  digitalWrite(PIN_1CP, cp1 ? HIGH : LOW);
  digitalWrite(PIN_1D, d1 ? HIGH : LOW);
  digitalWrite(PIN_1MR, mr1 ? HIGH : LOW);
  digitalWrite(PIN_2CP, cp2 ? HIGH : LOW);
  digitalWrite(PIN_2D, d2 ? HIGH : LOW);
  digitalWrite(PIN_2MR, mr2 ? HIGH : LOW);
}

int readNibble(uint8_t q0, uint8_t q1, uint8_t q2, uint8_t q3) {
  int value = 0;
  if (digitalRead(q0) == HIGH) value |= 1;
  if (digitalRead(q1) == HIGH) value |= 2;
  if (digitalRead(q2) == HIGH) value |= 4;
  if (digitalRead(q3) == HIGH) value |= 8;
  return value;
}

int readReg1() { return readNibble(PIN_1Q0, PIN_1Q1, PIN_1Q2, PIN_1Q3); }

int readReg2() { return readNibble(PIN_2Q0, PIN_2Q1, PIN_2Q2, PIN_2Q3); }

void expectBoth(const char* step, int expected1, int expected2) {
  delay(1);
  const int actual1 = readReg1();
  const int actual2 = readReg2();
  Serial.print(step);
  Serial.print(" reg1 expected=0x");
  Serial.print(expected1, HEX);
  Serial.print(" actual=0x");
  Serial.print(actual1, HEX);
  Serial.print(" reg2 expected=0x");
  Serial.print(expected2, HEX);
  Serial.print(" actual=0x");
  Serial.print(actual2, HEX);
  const bool pass = expected1 == actual1 && expected2 == actual2;
  Serial.println(pass ? " PASS" : " FAIL");
  if (!pass) noteFailure(step, expected1, actual1, expected2, actual2);
}

int shifted(int current, bool bit) { return ((current << 1) & 0x0E) | (bit ? 1 : 0); }

void shiftReg1(bool bit, bool otherData, int* reg1, int reg2, const char* step) {
  drive(false, bit, false, false, otherData, false);
  drive(true, bit, false, false, otherData, false);
  *reg1 = shifted(*reg1, bit);
  expectBoth(step, *reg1, reg2);
  drive(true, !bit, false, false, !otherData, false);
  expectBoth(step, *reg1, reg2);
  drive(false, !bit, false, false, !otherData, false);
  expectBoth(step, *reg1, reg2);
}

void shiftReg2(bool bit, bool otherData, int reg1, int* reg2, const char* step) {
  drive(false, otherData, false, false, bit, false);
  drive(false, otherData, false, true, bit, false);
  *reg2 = shifted(*reg2, bit);
  expectBoth(step, reg1, *reg2);
  drive(false, !otherData, false, true, !bit, false);
  expectBoth(step, reg1, *reg2);
  drive(false, !otherData, false, false, !bit, false);
  expectBoth(step, reg1, *reg2);
}

void checkBehavior() {
  drive(false, true, true, false, true, true);
  expectBoth("both MR", 0, 0);
  drive(false, true, false, false, true, false);
  expectBoth("MR released", 0, 0);

  const bool pattern[4] = {true, false, true, true};
  int reg1 = 0;
  int reg2 = 0;
  for (int i = 0; i < 4; i++) {
    char step[16];
    snprintf(step, sizeof(step), "reg1.%d", i);
    shiftReg1(pattern[i], (i & 1) == 0, &reg1, reg2, step);
  }

  for (int i = 0; i < 4; i++) {
    char step[16];
    snprintf(step, sizeof(step), "reg2.%d", i);
    shiftReg2(pattern[i], (i & 1) == 0, reg1, &reg2, step);
  }

  drive(true, true, true, false, false, false);
  expectBoth("MR1 overrides clock", 0, reg2);
  drive(false, true, true, false, false, false);
  drive(true, true, true, false, false, false);
  expectBoth("MR1 blocks shift", 0, reg2);
  drive(false, true, false, false, false, false);
  expectBoth("MR1 released", 0, reg2);

  drive(false, false, false, true, true, true);
  expectBoth("MR2 overrides clock", 0, 0);
  drive(false, false, false, false, true, true);
  drive(false, false, false, true, true, true);
  expectBoth("MR2 blocks shift", 0, 0);
}

void setup() {
  Serial.begin(115200);
  const uint8_t outputs[] = {PIN_1CP, PIN_1D, PIN_1MR, PIN_2CP, PIN_2D, PIN_2MR};
  for (uint8_t i = 0; i < sizeof(outputs); i++) pinMode(outputs[i], OUTPUT);
  pinMode(PIN_1Q0, INPUT);
  pinMode(PIN_1Q1, INPUT);
  pinMode(PIN_1Q2, INPUT);
  pinMode(PIN_1Q3, INPUT);
  pinMode(PIN_2Q0, INPUT);
  pinMode(PIN_2Q1, INPUT);
  pinMode(PIN_2Q2, INPUT);
  pinMode(PIN_2Q3, INPUT);
  drive(false, false, true, false, false, true);

  Serial.println("74HC4015 bench. Send any character to start.");
  while (Serial.available() == 0) {
  }
  while (Serial.available() > 0) Serial.read();

  checkBehavior();
  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
