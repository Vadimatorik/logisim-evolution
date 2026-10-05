/*
 * Self-check for a 74HC7014 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each output copies its input. Schmitt thresholds are not measured.
 * Inputs stay low until the check starts. Outputs are push-pull.
 */

struct Gate {
  const char* name;
  uint8_t pinA;
  uint8_t pinY;
};

const Gate GATES[] = {
    {"1", 2, 3},
    {"2", 4, 5},
    {"3", 6, 7},
    {"4", 9, 8},
    {"5", 11, 10},
    {"6", 13, 12},
};
const uint8_t GATE_COUNT = sizeof(GATES) / sizeof(GATES[0]);
const uint8_t ALL_HIGH = (1 << 6) - 1;

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

void drive(uint8_t mask) {
  for (uint8_t i = 0; i < GATE_COUNT; i++) {
    digitalWrite(GATES[i].pinA, (mask & (1 << i)) ? HIGH : LOW);
  }
}

uint8_t readOutputs() {
  uint8_t value = 0;
  for (uint8_t i = 0; i < GATE_COUNT; i++) {
    if (digitalRead(GATES[i].pinY) == HIGH) value |= (1 << i);
  }
  return value;
}

void expectMask(uint8_t expected, const char* step) {
  drive(expected);
  delay(1);
  const uint8_t actual = readOutputs();
  Serial.print(step);
  Serial.print(" expected ");
  Serial.print(expected, BIN);
  Serial.print(" actual ");
  Serial.print(actual, BIN);
  Serial.println(actual == expected ? " PASS" : " FAIL");
  if (actual != expected) noteFailure(step, expected, actual);
}

void runTest() {
  failed = false;
  resultLine[0] = '\0';
  Serial.println("74HC7014 test");

  expectMask(0, "all low");
  expectMask(ALL_HIGH, "all high");

  for (uint8_t i = 0; i < GATE_COUNT; i++) {
    char step[24];
    snprintf(step, sizeof(step), "only %s high", GATES[i].name);
    expectMask(1 << i, step);
  }

  for (uint8_t i = 0; i < GATE_COUNT; i++) {
    char step[24];
    snprintf(step, sizeof(step), "only %s low", GATES[i].name);
    expectMask(ALL_HIGH ^ (1 << i), step);
  }

  expectMask(0x15, "pattern 101010");
  expectMask(0x2A, "pattern 010101");

  drive(0);
  Serial.println(failed ? resultLine : "RESULT PASS");
}

void setup() {
  Serial.begin(115200);
  for (uint8_t i = 0; i < GATE_COUNT; i++) {
    pinMode(GATES[i].pinA, OUTPUT);
    pinMode(GATES[i].pinY, INPUT);
  }
  drive(0);
  Serial.println("74HC7014 ready. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  runTest();
}
