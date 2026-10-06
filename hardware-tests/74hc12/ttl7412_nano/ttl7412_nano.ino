/*
 * Self-check for a 74HC12 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * WIRED_AND stays 0 for the three independent gates. Set it to 1 only after
 * removing the pull-up on pin 6 and jumpering pin 12 to pin 6. The common
 * node is then read on D11, and the separate checks of gates 1 and 2 are
 * skipped.
 */

const uint8_t WIRED_AND = 0;

struct Gate {
  const char* name;
  uint8_t pinA;
  uint8_t pinB;
  uint8_t pinC;
  uint8_t pinY;
};

const Gate GATES[] = {
    {"1", 2, 3, 4, 11},
    {"2", 5, 6, 7, 12},
    {"3", 8, 9, 10, A0},
};
const uint8_t GATE_COUNT = sizeof(GATES) / sizeof(GATES[0]);
const uint8_t PIN_COMMON = 11;

bool failed = false;
char resultLine[140];

void noteFailure(
    const char* gate, int inputA, int inputB, int inputC, int expected, int actual) {
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

void noteWired(const char* step, int expected, int actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL wired %s expected=%d actual=%d",
      step,
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
  const int expected = (inputA && inputB && inputC) ? 0 : 1;
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
  const int levels[] = {0, 1};
  for (uint8_t i = 0; i < GATE_COUNT; i++) {
    if (WIRED_AND && i < 2) continue;
    for (uint8_t a = 0; a < 2; a++) {
      for (uint8_t b = 0; b < 2; b++) {
        for (uint8_t c = 0; c < 2; c++) {
          checkCombination(GATES[i], levels[a], levels[b], levels[c]);
        }
      }
    }
  }
}

void expectCommon(bool high, const char* step) {
  delay(1);
  const int expected = high ? 1 : 0;
  const int actual = digitalRead(PIN_COMMON) == HIGH ? 1 : 0;
  Serial.print("wired ");
  Serial.print(step);
  Serial.print(" expected=");
  Serial.print(expected);
  Serial.print(" actual=");
  Serial.print(actual);
  Serial.println(expected == actual ? " PASS" : " FAIL");
  if (expected != actual) noteWired(step, expected, actual);
}

void pullGate(uint8_t index, bool pull) {
  const uint8_t level = pull ? HIGH : LOW;
  digitalWrite(GATES[index].pinA, level);
  digitalWrite(GATES[index].pinB, level);
  digitalWrite(GATES[index].pinC, level);
}

void checkWiredAnd() {
  driveAllLow();
  expectCommon(true, "released");

  pullGate(0, true);
  expectCommon(false, "gate1");

  driveAllLow();
  pullGate(1, true);
  expectCommon(false, "gate2");

  pullGate(0, true);
  expectCommon(false, "both");

  Serial.println(failed ? "wired-AND FAIL" : "wired-AND PASS");
}

void runTest() {
  failed = false;
  resultLine[0] = '\0';
  Serial.println(WIRED_AND ? "74HC12 test, wired-AND on" : "74HC12 test");
  checkGates();
  if (WIRED_AND) checkWiredAnd();
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
  Serial.println("74HC12 ready. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  runTest();
}
