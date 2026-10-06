/*
 * Self-check for a 74HC7032 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each gate is Y = A OR B. Outputs are push-pull, so Y is read directly.
 * All eight inputs stay low until the check starts. Schmitt hysteresis is
 * not measured: Nano drives fast rail-to-rail edges.
 */

const uint8_t PIN_1A = 2;
const uint8_t PIN_1B = 3;
const uint8_t PIN_1Y = 4;
const uint8_t PIN_2A = 5;
const uint8_t PIN_2B = 6;
const uint8_t PIN_2Y = 7;
const uint8_t PIN_3Y = 8;
const uint8_t PIN_3A = 9;
const uint8_t PIN_3B = 10;
const uint8_t PIN_4Y = 11;
const uint8_t PIN_4A = 12;
const uint8_t PIN_4B = A0;

struct Gate {
  uint8_t pinA;
  uint8_t pinB;
  uint8_t pinY;
  const char* name;
};

const Gate GATES[] = {
    {PIN_1A, PIN_1B, PIN_1Y, "1Y"},
    {PIN_2A, PIN_2B, PIN_2Y, "2Y"},
    {PIN_3A, PIN_3B, PIN_3Y, "3Y"},
    {PIN_4A, PIN_4B, PIN_4Y, "4Y"},
};

const uint8_t GATE_COUNT = sizeof(GATES) / sizeof(GATES[0]);

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, uint8_t expected, uint8_t actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s expected %u got %u",
      step,
      expected,
      actual);
}

void settle() { delay(1); }

void driveAllLow() {
  for (uint8_t index = 0; index < GATE_COUNT; index++) {
    digitalWrite(GATES[index].pinA, LOW);
    digitalWrite(GATES[index].pinB, LOW);
  }
}

void setInput(uint8_t pin, bool high) { digitalWrite(pin, high ? HIGH : LOW); }

void expectOutput(uint8_t pinY, bool high, const char* step) {
  settle();
  const uint8_t actual = digitalRead(pinY) ? 1 : 0;
  const uint8_t expected = high ? 1 : 0;
  if (actual != expected) noteFailure(step, expected, actual);
}

void checkGateAlone(uint8_t index) {
  const Gate& gate = GATES[index];
  const uint8_t patterns[4][3] = {
      {0, 0, 0},
      {0, 1, 1},
      {1, 0, 1},
      {1, 1, 1},
  };
  char step[16];

  for (uint8_t pattern = 0; pattern < 4; pattern++) {
    driveAllLow();
    setInput(gate.pinA, patterns[pattern][0]);
    setInput(gate.pinB, patterns[pattern][1]);
    snprintf(step, sizeof(step), "%s %u%u", gate.name, patterns[pattern][0], patterns[pattern][1]);
    expectOutput(gate.pinY, patterns[pattern][2], step);
    for (uint8_t other = 0; other < GATE_COUNT; other++) {
      if (other == index) continue;
      snprintf(step, sizeof(step), "%s quiet", GATES[other].name);
      expectOutput(GATES[other].pinY, false, step);
    }
  }
}

void checkIndependentGates() {
  driveAllLow();
  setInput(PIN_1A, true);
  setInput(PIN_3B, true);
  setInput(PIN_4A, true);
  setInput(PIN_4B, true);
  expectOutput(PIN_1Y, true, "mix 1Y");
  expectOutput(PIN_2Y, false, "mix 2Y");
  expectOutput(PIN_3Y, true, "mix 3Y");
  expectOutput(PIN_4Y, true, "mix 4Y");
}

void setup() {
  for (uint8_t index = 0; index < GATE_COUNT; index++) {
    pinMode(GATES[index].pinA, OUTPUT);
    pinMode(GATES[index].pinB, OUTPUT);
    pinMode(GATES[index].pinY, INPUT);
  }
  driveAllLow();
  Serial.begin(115200);
  Serial.println("74HC7032 ready, send any character");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();

  failed = false;
  resultLine[0] = '\0';
  for (uint8_t index = 0; index < GATE_COUNT; index++) checkGateAlone(index);
  checkIndependentGates();
  driveAllLow();

  if (!failed) Serial.println("RESULT PASS");
  else Serial.println(resultLine);
}
