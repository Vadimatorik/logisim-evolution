/*
 * Self-check for a 74HC4510 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * MR asynchronously clears the counter and overrides PL. While PL is high and
 * MR is low, Q follows D with no clock. Counting is the rising edge of CP
 * while MR, PL and CE are low. UP/DN high counts up through the decade cycle.
 * TC is active low. It is low only while CE is low, and either the direction
 * is up with both Q0 and Q3 high, or the direction is down and the code is 0.
 * Outputs are push-pull, so Q and TC are read directly. MR stays high, CP
 * stays low, PL stays low and CE stays high until the check starts, so the
 * counter is already clear.
 */

const uint8_t PIN_PL = 2;
const uint8_t PIN_D3 = 3;
const uint8_t PIN_D0 = 4;
const uint8_t PIN_CE = 5;
const uint8_t PIN_MR = 6;
const uint8_t PIN_UP = 7;
const uint8_t PIN_D1 = 8;
const uint8_t PIN_D2 = 9;
const uint8_t PIN_CP = 10;
const uint8_t PIN_Q3 = 11;
const uint8_t PIN_Q0 = 12;
const uint8_t PIN_TC = A0;
const uint8_t PIN_Q1 = A1;
const uint8_t PIN_Q2 = A2;

const uint8_t NEXT_UP[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 11, 6, 13, 4, 15, 2};
const uint8_t NEXT_DOWN[] = {9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 13, 10, 3, 12, 1, 14};

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

void setData(uint8_t code) {
  digitalWrite(PIN_D0, (code & 1) ? HIGH : LOW);
  digitalWrite(PIN_D1, (code & 2) ? HIGH : LOW);
  digitalWrite(PIN_D2, (code & 4) ? HIGH : LOW);
  digitalWrite(PIN_D3, (code & 8) ? HIGH : LOW);
}

uint8_t readCount() {
  uint8_t value = 0;
  if (digitalRead(PIN_Q0)) value |= 1;
  if (digitalRead(PIN_Q1)) value |= 2;
  if (digitalRead(PIN_Q2)) value |= 4;
  if (digitalRead(PIN_Q3)) value |= 8;
  return value;
}

void expectCount(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readCount();
  if (actual != expected) noteFailure(step, expected, actual);
}

void expectTc(bool high, const char* step) {
  settle();
  const uint8_t actual = digitalRead(PIN_TC) ? 1 : 0;
  const uint8_t expected = high ? 1 : 0;
  if (actual != expected) noteFailure(step, expected, actual);
}

bool terminalUp(uint8_t code) { return (code & 0x9) == 0x9; }

void clockPulse() {
  digitalWrite(PIN_CP, LOW);
  settle();
  digitalWrite(PIN_CP, HIGH);
  settle();
  digitalWrite(PIN_CP, LOW);
  settle();
}

void countEnabled(bool up) {
  digitalWrite(PIN_MR, LOW);
  digitalWrite(PIN_PL, LOW);
  digitalWrite(PIN_CE, LOW);
  digitalWrite(PIN_UP, up ? HIGH : LOW);
}

void load(uint8_t code) {
  digitalWrite(PIN_MR, LOW);
  digitalWrite(PIN_CE, HIGH);
  digitalWrite(PIN_PL, HIGH);
  setData(code);
  settle();
  digitalWrite(PIN_PL, LOW);
  settle();
}

void checkResetOverridesLoad() {
  load(12);
  digitalWrite(PIN_PL, HIGH);
  setData(9);
  digitalWrite(PIN_MR, HIGH);
  settle();
  expectCount(0, "reset overrides load");
  expectTc(true, "tc while reset and ce high");

  digitalWrite(PIN_CE, LOW);
  digitalWrite(PIN_UP, LOW);
  settle();
  expectCount(0, "reset holds zero");
  expectTc(false, "tc down at zero");

  digitalWrite(PIN_MR, LOW);
  settle();
  expectCount(9, "load resumes");
  expectTc(true, "tc down at nine");
  digitalWrite(PIN_PL, LOW);
}

void checkTransparentLoad() {
  for (uint8_t code = 0; code < 16; code++) {
    load(code);
    expectCount(code, "load");
  }

  digitalWrite(PIN_MR, LOW);
  digitalWrite(PIN_CE, HIGH);
  digitalWrite(PIN_PL, HIGH);
  setData(6);
  expectCount(6, "transparent 6");
  setData(10);
  expectCount(10, "transparent 10");
  digitalWrite(PIN_PL, LOW);
  expectCount(10, "load sticks");
}

void checkUpCycle() {
  load(0);
  countEnabled(true);
  for (uint8_t code = 0; code < 10; code++) {
    expectCount(code, "up");
    expectTc(!terminalUp(code), "tc up");
    clockPulse();
  }
  expectCount(0, "up wrap");
  expectTc(true, "tc after up wrap");
}

void checkDownCycle() {
  load(0);
  countEnabled(false);
  expectCount(0, "down start");
  expectTc(false, "tc down at zero");
  clockPulse();
  for (uint8_t code = 9; code > 0; code--) {
    expectCount(code, "down");
    expectTc(true, "tc down");
    clockPulse();
  }
  expectCount(0, "down wrap");
  expectTc(false, "tc down again");
}

void checkIllegalCodes() {
  for (uint8_t code = 0; code < 16; code++) {
    load(code);
    countEnabled(true);
    expectTc(!terminalUp(code), "tc before up step");
    clockPulse();
    expectCount(NEXT_UP[code], "up step");
  }

  for (uint8_t code = 0; code < 16; code++) {
    load(code);
    countEnabled(false);
    expectTc(code != 0, "tc before down step");
    clockPulse();
    expectCount(NEXT_DOWN[code], "down step");
  }
}

void checkHoldAndFallingClock() {
  load(9);
  digitalWrite(PIN_MR, LOW);
  digitalWrite(PIN_PL, LOW);
  digitalWrite(PIN_CE, HIGH);
  digitalWrite(PIN_UP, HIGH);
  settle();
  expectTc(true, "tc inhibited at 9");
  clockPulse();
  expectCount(9, "ce high holds");
  expectTc(true, "tc stays inhibited");

  digitalWrite(PIN_CE, LOW);
  settle();
  expectCount(9, "still 9");
  expectTc(false, "tc active at 9");
  digitalWrite(PIN_UP, LOW);
  settle();
  expectCount(9, "direction holds the code");
  expectTc(true, "tc follows direction");

  digitalWrite(PIN_CE, HIGH);
  digitalWrite(PIN_CP, HIGH);
  settle();
  digitalWrite(PIN_CE, LOW);
  digitalWrite(PIN_UP, LOW);
  settle();
  digitalWrite(PIN_CP, LOW);
  settle();
  expectCount(9, "falling clock holds");
}

void checkResetReleaseDoesNotCount() {
  load(3);
  countEnabled(true);
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_MR, HIGH);
  settle();
  expectCount(0, "async reset");
  digitalWrite(PIN_CP, HIGH);
  settle();
  expectCount(0, "clock during reset");
  digitalWrite(PIN_MR, LOW);
  settle();
  expectCount(0, "release reset");
  digitalWrite(PIN_CP, LOW);
  settle();
  clockPulse();
  expectCount(1, "next rise counts");
}

void setup() {
  pinMode(PIN_PL, OUTPUT);
  pinMode(PIN_D0, OUTPUT);
  pinMode(PIN_D1, OUTPUT);
  pinMode(PIN_D2, OUTPUT);
  pinMode(PIN_D3, OUTPUT);
  pinMode(PIN_CE, OUTPUT);
  pinMode(PIN_MR, OUTPUT);
  pinMode(PIN_UP, OUTPUT);
  pinMode(PIN_CP, OUTPUT);
  pinMode(PIN_Q0, INPUT);
  pinMode(PIN_Q1, INPUT);
  pinMode(PIN_Q2, INPUT);
  pinMode(PIN_Q3, INPUT);
  pinMode(PIN_TC, INPUT);

  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_PL, LOW);
  digitalWrite(PIN_CE, HIGH);
  digitalWrite(PIN_UP, HIGH);
  setData(0);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  expectCount(0, "initial reset");
  digitalWrite(PIN_MR, LOW);
  settle();
  expectCount(0, "reset released");

  checkResetOverridesLoad();
  checkTransparentLoad();
  checkUpCycle();
  checkDownCycle();
  checkIllegalCodes();
  checkHoldAndFallingClock();
  checkResetReleaseDoesNotCount();

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
