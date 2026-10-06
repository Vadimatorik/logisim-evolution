/*
 * Self-check for a 74HC136 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Equal inputs drive the open-drain output low. Different inputs release it,
 * and the 10k pull-up makes that level HIGH. WIRED_AND stays 0 for the four
 * independent gates. Set it to 1 only after removing the pull-up on pin 6
 * and jumpering pin 3 to pin 6.
 */

const uint8_t WIRED_AND = 0;

struct Gate {
  const char* name;
  uint8_t pinA;
  uint8_t pinB;
  uint8_t pinY;
};

const Gate GATES[] = {
    {"1", 2, 3, 10},
    {"2", 4, 5, 11},
    {"3", 6, 7, 12},
    {"4", 8, 9, A0},
};
const uint8_t GATE_COUNT = sizeof(GATES) / sizeof(GATES[0]);

bool failed = false;
char resultLine[120];

void noteFailure(const char* gate, int inputA, int inputB, int expected, int actual) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine),
           "RESULT FAIL gate=%s A=%d B=%d expected=%d actual=%d", gate, inputA, inputB, expected,
           actual);
}

void driveAllLow() {
  for (uint8_t i = 0; i < GATE_COUNT; i++) {
    digitalWrite(GATES[i].pinA, LOW);
    digitalWrite(GATES[i].pinB, LOW);
  }
}

void checkCombination(const Gate& gate, int inputA, int inputB) {
  driveAllLow();
  digitalWrite(gate.pinA, inputA ? HIGH : LOW);
  digitalWrite(gate.pinB, inputB ? HIGH : LOW);
  delay(1);
  const int expected = (inputA != inputB) ? 1 : 0;
  const int actual = digitalRead(gate.pinY) == HIGH ? 1 : 0;
  Serial.print("gate ");
  Serial.print(gate.name);
  Serial.print(" A=");
  Serial.print(inputA);
  Serial.print(" B=");
  Serial.print(inputB);
  Serial.print(" expected=");
  Serial.print(expected);
  Serial.print(" actual=");
  Serial.print(actual);
  Serial.println(expected == actual ? " PASS" : " FAIL");
  if (expected != actual) noteFailure(gate.name, inputA, inputB, expected, actual);
}

void checkGates() {
  const int levels[] = {0, 1};
  for (uint8_t i = 0; i < GATE_COUNT; i++) {
    for (uint8_t a = 0; a < 2; a++) {
      for (uint8_t b = 0; b < 2; b++) {
        checkCombination(GATES[i], levels[a], levels[b]);
      }
    }
  }
}

void expectNode(int inputA, int inputB, int expected) {
  delay(1);
  const int actual = digitalRead(10) == HIGH ? 1 : 0;
  Serial.print("wired A=");
  Serial.print(inputA);
  Serial.print(" B=");
  Serial.print(inputB);
  Serial.print(" expected=");
  Serial.print(expected);
  Serial.print(" actual=");
  Serial.println(actual);
  if (actual != expected) noteFailure("wired", inputA, inputB, expected, actual);
}

void checkWiredAnd() {
  // Both gates released: each pair differs, so the shared node is pulled high.
  driveAllLow();
  digitalWrite(3, HIGH);
  digitalWrite(5, HIGH);
  expectNode(0, 0, 1);

  // Gate 1 drives low (inputs equal). Gate 2 stays released.
  digitalWrite(3, LOW);
  expectNode(1, 0, 0);

  // Gate 2 drives low. Gate 1 is released again.
  digitalWrite(3, HIGH);
  digitalWrite(4, HIGH);
  digitalWrite(5, HIGH);
  expectNode(0, 1, 0);

  // Both gates drive low.
  digitalWrite(2, HIGH);
  digitalWrite(3, HIGH);
  expectNode(1, 1, 0);

  Serial.println(failed ? "wired-AND FAIL" : "wired-AND PASS");
}

void runTest() {
  failed = false;
  resultLine[0] = '\0';
  Serial.println(WIRED_AND ? "74HC136 test, wired-AND on" : "74HC136 test");
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
    pinMode(GATES[i].pinY, INPUT);
    digitalWrite(GATES[i].pinA, LOW);
    digitalWrite(GATES[i].pinB, LOW);
  }
  Serial.println("74HC136 ready. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  runTest();
}
