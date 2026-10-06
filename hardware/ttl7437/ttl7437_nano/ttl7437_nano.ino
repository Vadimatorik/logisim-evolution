/*
 * Self-check for a 74HC37 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each output is the NAND of its two inputs. Outputs are push-pull, so no
 * pull-ups are required. Other inputs stay LOW while one gate is checked, so
 * their outputs must stay HIGH.
 */

struct Gate {
  const char* name;
  uint8_t pinA;
  uint8_t pinB;
  uint8_t pinY;
};

const Gate GATES[] = {
    {"1", 2, 3, 4},
    {"2", 5, 6, 7},
    {"3", 9, 10, 8},
    {"4", A0, A1, 11},
};
const uint8_t GATE_COUNT = sizeof(GATES) / sizeof(GATES[0]);

bool failed = false;
char resultLine[140];

void noteFailure(const char* name, int inputA, int inputB, int expected, int actual) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine),
           "RESULT FAIL gate=%s A=%d B=%d expected=%d actual=%d", name, inputA, inputB, expected,
           actual);
}

void driveAllLow() {
  for (uint8_t i = 0; i < GATE_COUNT; i++) {
    digitalWrite(GATES[i].pinA, LOW);
    digitalWrite(GATES[i].pinB, LOW);
  }
}

void checkLevel(const Gate& gate, int inputA, int inputB) {
  driveAllLow();
  digitalWrite(gate.pinA, inputA ? HIGH : LOW);
  digitalWrite(gate.pinB, inputB ? HIGH : LOW);
  delay(1);
  const int expected = (inputA && inputB) ? 0 : 1;
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

  for (uint8_t i = 0; i < GATE_COUNT; i++) {
    if (&GATES[i] == &gate) continue;
    const int other = digitalRead(GATES[i].pinY) == HIGH ? 1 : 0;
    if (other != 1) {
      Serial.print("gate ");
      Serial.print(GATES[i].name);
      Serial.println(" idle expected=1 FAIL");
      noteFailure(GATES[i].name, 0, 0, 1, other);
    }
  }
}

void checkGate(const Gate& gate) {
  checkLevel(gate, 0, 0);
  checkLevel(gate, 0, 1);
  checkLevel(gate, 1, 0);
  checkLevel(gate, 1, 1);
}

void runTest() {
  failed = false;
  resultLine[0] = '\0';
  Serial.println("74HC37 test");
  for (uint8_t i = 0; i < GATE_COUNT; i++) checkGate(GATES[i]);
  driveAllLow();
  Serial.println(failed ? resultLine : "RESULT PASS");
}

void setup() {
  Serial.begin(115200);
  for (uint8_t i = 0; i < GATE_COUNT; i++) {
    pinMode(GATES[i].pinA, OUTPUT);
    pinMode(GATES[i].pinB, OUTPUT);
    pinMode(GATES[i].pinY, INPUT);
  }
  driveAllLow();
  Serial.println("74HC37 ready. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  runTest();
}
