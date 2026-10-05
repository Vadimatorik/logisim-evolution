/*
 * Self-check for a 74x28 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each gate is Y = NOT (A OR B). Outputs are push-pull, so Y is read directly.
 * All inputs stay low until the check starts, which holds every Y high.
 */

const uint8_t PIN_1A = 2;
const uint8_t PIN_1B = 3;
const uint8_t PIN_2A = 4;
const uint8_t PIN_2B = 5;
const uint8_t PIN_3A = 6;
const uint8_t PIN_3B = 7;
const uint8_t PIN_4A = 8;
const uint8_t PIN_4B = 9;
const uint8_t PIN_1Y = 11;
const uint8_t PIN_2Y = 12;
const uint8_t PIN_3Y = A0;
const uint8_t PIN_4Y = A1;

const uint8_t INPUT_A[4] = {PIN_1A, PIN_2A, PIN_3A, PIN_4A};
const uint8_t INPUT_B[4] = {PIN_1B, PIN_2B, PIN_3B, PIN_4B};
const uint8_t OUTPUT_Y[4] = {PIN_1Y, PIN_2Y, PIN_3Y, PIN_4Y};

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

void settle() { delay(1); }

void setAllLow() {
  for (uint8_t gate = 0; gate < 4; gate++) {
    digitalWrite(INPUT_A[gate], LOW);
    digitalWrite(INPUT_B[gate], LOW);
  }
}

void setGate(uint8_t gate, uint8_t a, uint8_t b) {
  digitalWrite(INPUT_A[gate], a ? HIGH : LOW);
  digitalWrite(INPUT_B[gate], b ? HIGH : LOW);
}

uint8_t readY(uint8_t gate) {
  return digitalRead(OUTPUT_Y[gate]) ? 1 : 0;
}

void expectY(uint8_t gate, uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readY(gate);
  if (actual != expected) noteFailure(step, expected, actual);
}

void checkIdleHigh() {
  setAllLow();
  for (uint8_t gate = 0; gate < 4; gate++) {
    expectY(gate, 1, "idle");
  }
}

void checkEachGate() {
  const uint8_t rows[4][2] = {{0, 0}, {0, 1}, {1, 0}, {1, 1}};
  const uint8_t expect[4] = {1, 0, 0, 0};
  for (uint8_t gate = 0; gate < 4; gate++) {
    for (uint8_t row = 0; row < 4; row++) {
      setAllLow();
      setGate(gate, rows[row][0], rows[row][1]);
      expectY(gate, expect[row], "truth");
      for (uint8_t other = 0; other < 4; other++) {
        if (other == gate) continue;
        expectY(other, 1, "other idle");
      }
    }
  }
}

void checkTogether() {
  const uint8_t a0[] = {0, 1, 0};
  const uint8_t b0[] = {0, 1, 1};
  const uint8_t a1[] = {0, 1, 1};
  const uint8_t b1[] = {1, 0, 1};
  const uint8_t a2[] = {1, 0, 0};
  const uint8_t b2[] = {0, 1, 0};
  const uint8_t a3[] = {1, 0, 1};
  const uint8_t b3[] = {1, 0, 0};
  const uint8_t y0[] = {1, 0, 0};
  const uint8_t y1[] = {0, 0, 0};
  const uint8_t y2[] = {0, 0, 1};
  const uint8_t y3[] = {0, 1, 0};
  for (uint8_t index = 0; index < 3; index++) {
    setGate(0, a0[index], b0[index]);
    setGate(1, a1[index], b1[index]);
    setGate(2, a2[index], b2[index]);
    setGate(3, a3[index], b3[index]);
    expectY(0, y0[index], "together 1");
    expectY(1, y1[index], "together 2");
    expectY(2, y2[index], "together 3");
    expectY(3, y3[index], "together 4");
  }
}

void setup() {
  pinMode(PIN_1A, OUTPUT);
  pinMode(PIN_1B, OUTPUT);
  pinMode(PIN_2A, OUTPUT);
  pinMode(PIN_2B, OUTPUT);
  pinMode(PIN_3A, OUTPUT);
  pinMode(PIN_3B, OUTPUT);
  pinMode(PIN_4A, OUTPUT);
  pinMode(PIN_4B, OUTPUT);
  pinMode(PIN_1Y, INPUT);
  pinMode(PIN_2Y, INPUT);
  pinMode(PIN_3Y, INPUT);
  pinMode(PIN_4Y, INPUT);

  setAllLow();

  Serial.begin(115200);
  Serial.println("7428 ready");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();

  failed = false;
  checkIdleHigh();
  checkEachGate();
  checkTogether();
  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}
