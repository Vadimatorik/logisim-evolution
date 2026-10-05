/*
 * Self-check for a 74LS15 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each Y pin needs an external 10k pull-up to 5V. A released output reads HIGH.
 * A low output reads LOW. Do not use the pin 13 LED as a pull-down.
 */

struct Gate {
  const char* name;
  uint8_t pinA;
  uint8_t pinB;
  uint8_t pinC;
  uint8_t pinY;
};

const Gate GATES[] = {
    {"1", 2, 3, 4, 5},
    {"2", 6, 7, 8, 9},
    {"3", 10, 11, 12, A0},
};
const uint8_t GATE_COUNT = sizeof(GATES) / sizeof(GATES[0]);

bool failed = false;
char resultLine[120];

void noteFailure(const char* gate, int inputA, int inputB, int inputC, int expected, int actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL gate=%s A=%d B=%d C=%d expected=%d actual=%d",
      gate,
      inputA,
      inputB,
      inputC,
      expected,
      actual);
}

void driveAllLow() {
  for (uint8_t i = 0; i < GATE_COUNT; i++) {
    digitalWrite(GATES[i].pinA, LOW);
    digitalWrite(GATES[i].pinB, LOW);
    digitalWrite(GATES[i].pinC, LOW);
  }
}

void checkCombination(const Gate& gate, int inputA, int inputB, int inputC) {
  driveAllLow();
  digitalWrite(gate.pinA, inputA ? HIGH : LOW);
  digitalWrite(gate.pinB, inputB ? HIGH : LOW);
  digitalWrite(gate.pinC, inputC ? HIGH : LOW);
  delay(1);
  const int expected = (inputA && inputB && inputC) ? 1 : 0;
  const int actual = digitalRead(gate.pinY) == HIGH ? 1 : 0;
  Serial.print("gate ");
  Serial.print(gate.name);
  Serial.print(" A=");
  Serial.print(inputA);
  Serial.print(" B=");
  Serial.print(inputB);
  Serial.print(" C=");
  Serial.print(inputC);
  Serial.print(" expected=");
  Serial.print(expected);
  Serial.print(" actual=");
  Serial.print(actual);
  Serial.println(expected == actual ? " PASS" : " FAIL");
  if (expected != actual) noteFailure(gate.name, inputA, inputB, inputC, expected, actual);
}

void checkGates() {
  for (uint8_t i = 0; i < GATE_COUNT; i++) {
    for (uint8_t code = 0; code < 8; code++) {
      checkCombination(GATES[i], (code >> 2) & 1, (code >> 1) & 1, code & 1);
    }
  }
}

void checkTogether() {
  driveAllLow();
  digitalWrite(GATES[0].pinA, HIGH);
  digitalWrite(GATES[0].pinB, HIGH);
  digitalWrite(GATES[0].pinC, HIGH);
  delay(1);
  const int actual1 = digitalRead(GATES[0].pinY) == HIGH ? 1 : 0;
  const int actual2 = digitalRead(GATES[1].pinY) == HIGH ? 1 : 0;
  const int actual3 = digitalRead(GATES[2].pinY) == HIGH ? 1 : 0;
  Serial.print("together expected=1,0,0 actual=");
  Serial.print(actual1);
  Serial.print(",");
  Serial.print(actual2);
  Serial.print(",");
  Serial.println(actual3);
  if (actual1 != 1) noteFailure("1", 1, 1, 1, 1, actual1);
  if (actual2 != 0) noteFailure("2", 0, 0, 0, 0, actual2);
  if (actual3 != 0) noteFailure("3", 0, 0, 0, 0, actual3);
}

void runTest() {
  failed = false;
  resultLine[0] = '\0';
  Serial.println("74LS15 test");
  checkGates();
  checkTogether();
  driveAllLow();
  Serial.println(failed ? resultLine : "RESULT PASS");
}

void setup() {
  Serial.begin(115200);
  for (uint8_t i = 0; i < GATE_COUNT; i++) {
    pinMode(GATES[i].pinA, OUTPUT);
    pinMode(GATES[i].pinB, OUTPUT);
    pinMode(GATES[i].pinC, OUTPUT);
    pinMode(GATES[i].pinY, INPUT);
    digitalWrite(GATES[i].pinA, LOW);
    digitalWrite(GATES[i].pinB, LOW);
    digitalWrite(GATES[i].pinC, LOW);
  }
  Serial.println("74LS15 ready. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  runTest();
}
