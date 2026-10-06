/*
 * Self-check for a DIP-14 74x22 (SN74LS22) wired to an Arduino Nano as described
 * in ../README.md. Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * WIRED_AND stays 0 for the two independent gates. Set it to 1 only after
 * removing the pull-up on pin 8 and jumpering pin 6 to pin 8.
 */

const uint8_t WIRED_AND = 0;

struct Gate {
  const char* name;
  uint8_t pinA;
  uint8_t pinB;
  uint8_t pinC;
  uint8_t pinD;
  uint8_t pinY;
};

const Gate GATES[] = {
    {"1", 2, 3, 4, 5, 8},
    {"2", 6, 7, 10, 11, 9},
};
const uint8_t GATE_COUNT = sizeof(GATES) / sizeof(GATES[0]);

bool failed = false;
char resultLine[140];

void noteFailure(const char* name, int mask, int expected, int actual) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine),
           "RESULT FAIL gate=%s mask=%d expected=%d actual=%d", name, mask, expected, actual);
}

void drivePin(uint8_t pin, int level) {
  digitalWrite(pin, level ? HIGH : LOW);
}

void driveAllLow() {
  for (uint8_t i = 0; i < GATE_COUNT; i++) {
    drivePin(GATES[i].pinA, 0);
    drivePin(GATES[i].pinB, 0);
    drivePin(GATES[i].pinC, 0);
    drivePin(GATES[i].pinD, 0);
  }
}

void driveGate(const Gate& gate, int mask) {
  drivePin(gate.pinA, mask & 1);
  drivePin(gate.pinB, mask & 2);
  drivePin(gate.pinC, mask & 4);
  drivePin(gate.pinD, mask & 8);
}

void checkMask(const Gate& gate, int mask) {
  driveAllLow();
  driveGate(gate, mask);
  delay(1);
  const int actual = digitalRead(gate.pinY) == HIGH ? 1 : 0;
  const int expected = mask == 0xF ? 0 : 1;
  Serial.print("gate ");
  Serial.print(gate.name);
  Serial.print(" mask=");
  Serial.print(mask);
  Serial.print(" expected=");
  Serial.print(expected);
  Serial.print(" actual=");
  Serial.print(actual);
  Serial.println(expected == actual ? " PASS" : " FAIL");
  if (expected != actual) noteFailure(gate.name, mask, expected, actual);
}

void checkGates() {
  for (uint8_t i = 0; i < GATE_COUNT; i++) {
    for (int mask = 0; mask < 16; mask++) {
      checkMask(GATES[i], mask);
    }
  }
}

void checkWired(int mask1, int mask2, int expected) {
  driveAllLow();
  driveGate(GATES[0], mask1);
  driveGate(GATES[1], mask2);
  delay(1);
  const int actual = digitalRead(GATES[0].pinY) == HIGH ? 1 : 0;
  Serial.print("wired mask1=");
  Serial.print(mask1);
  Serial.print(" mask2=");
  Serial.print(mask2);
  Serial.print(" expected=");
  Serial.print(expected);
  Serial.print(" actual=");
  Serial.print(actual);
  Serial.println(expected == actual ? " PASS" : " FAIL");
  if (expected != actual) noteFailure("wired", mask1, expected, actual);
}

void checkWiredAnd() {
  checkWired(0x0, 0x0, 1);
  checkWired(0x7, 0x7, 1);
  checkWired(0xF, 0x0, 0);
  checkWired(0x0, 0xF, 0);
  checkWired(0xF, 0xF, 0);
  Serial.println(failed ? "wired-AND FAIL" : "wired-AND PASS");
}

void runTest() {
  failed = false;
  resultLine[0] = '\0';
  Serial.println(WIRED_AND ? "74x22 test, wired-AND on" : "74x22 test");
  if (WIRED_AND) checkWiredAnd();
  else checkGates();
  driveAllLow();
  Serial.println(failed ? resultLine : "RESULT PASS");
}

void setup() {
  Serial.begin(115200);
  for (uint8_t i = 0; i < GATE_COUNT; i++) {
    pinMode(GATES[i].pinA, OUTPUT);
    pinMode(GATES[i].pinB, OUTPUT);
    pinMode(GATES[i].pinC, OUTPUT);
    pinMode(GATES[i].pinD, OUTPUT);
    pinMode(GATES[i].pinY, INPUT);
  }
  driveAllLow();
  Serial.println("74x22 ready. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  runTest();
}
