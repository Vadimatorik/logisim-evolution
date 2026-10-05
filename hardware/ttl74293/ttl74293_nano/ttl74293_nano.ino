/*
 * Self-check for a 74HC293 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * A toggles QA on the falling edge. B advances QB, QC and QD through 0..7.
 * Both R0 inputs clear every output and override the clocks. One R0 input
 * does nothing. QA is not wired to B on this bench. This pinout is the
 * 74293 corner-power package, not the 74HC93.
 */

const uint8_t PIN_A = 2;
const uint8_t PIN_B = 3;
const uint8_t PIN_R01 = 4;
const uint8_t PIN_R02 = 5;
const uint8_t PIN_QA = 6;
const uint8_t PIN_QB = 7;
const uint8_t PIN_QC = 8;
const uint8_t PIN_QD = 9;

bool failed = false;
char resultLine[96];
bool clockA = false;
bool clockB = false;
bool r01 = true;
bool r02 = true;
bool qa = false;
bool qb = false;
bool qc = false;
bool qd = false;

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

void settle() { delayMicroseconds(20); }

bool resetToZero() { return r01 && r02; }

uint8_t modelNibble() {
  return (qa ? 1 : 0) | (qb ? 2 : 0) | (qc ? 4 : 0) | (qd ? 8 : 0);
}

uint8_t readNibble() {
  uint8_t value = 0;
  if (digitalRead(PIN_QA)) value |= 1;
  if (digitalRead(PIN_QB)) value |= 2;
  if (digitalRead(PIN_QC)) value |= 4;
  if (digitalRead(PIN_QD)) value |= 8;
  return value;
}

void check(const char* step) {
  const uint8_t actual = readNibble();
  const uint8_t expected = modelNibble();
  if (actual != expected) noteFailure(step, expected, actual);
}

void applyAsync() {
  if (resetToZero()) {
    qa = false;
    qb = false;
    qc = false;
    qd = false;
  }
}

void onFallingA() {
  if (resetToZero()) {
    applyAsync();
    return;
  }
  qa = !qa;
}

void onFallingB() {
  if (resetToZero()) {
    applyAsync();
    return;
  }
  uint8_t code = (qb ? 1 : 0) | (qc ? 2 : 0) | (qd ? 4 : 0);
  code = (code + 1) & 7;
  qb = code & 1;
  qc = code & 2;
  qd = code & 4;
}

void writeLevel(uint8_t pin, bool high) {
  digitalWrite(pin, high ? HIGH : LOW);
}

void setA(bool high, const char* step) {
  const bool falling = clockA && !high;
  clockA = high;
  writeLevel(PIN_A, high);
  settle();
  if (falling) onFallingA();
  check(step);
}

void setB(bool high, const char* step) {
  const bool falling = clockB && !high;
  clockB = high;
  writeLevel(PIN_B, high);
  settle();
  if (falling) onFallingB();
  check(step);
}

void setReset(uint8_t pin, bool& level, bool high, const char* step) {
  level = high;
  writeLevel(pin, high);
  settle();
  applyAsync();
  check(step);
}

void pulseA(const char* step) {
  setA(true, step);
  setA(false, step);
}

void pulseB(const char* step) {
  setB(true, step);
  setB(false, step);
}

void runChecks() {
  delay(1);
  check("held-reset");
  pulseA("a-during-reset");
  pulseB("b-during-reset");

  setReset(PIN_R02, r02, false, "release-r02");
  setA(true, "a-rise-one-reset");
  setA(false, "a-fall-one-reset");
  setReset(PIN_R02, r02, true, "both-reset-clear");

  setReset(PIN_R01, r01, false, "release-r01");
  setReset(PIN_R02, r02, false, "release-r02-count");
  setA(true, "a-rising");
  setA(false, "a-first-fall");
  pulseA("a-second-fall");

  setB(true, "b-rising");
  setB(false, "b-fall-1");
  pulseB("b-fall-2");
  pulseB("b-fall-3");
  pulseB("b-fall-4");
  pulseB("b-fall-5");
  pulseB("b-fall-6");
  pulseB("b-fall-7");
  pulseB("b-fall-8");

  pulseA("qa-before-single-reset");
  setReset(PIN_R01, r01, true, "r01-only");
  setReset(PIN_R01, r01, false, "release-r01-again");
  setReset(PIN_R02, r02, true, "r02-only");
  setReset(PIN_R01, r01, true, "both-reset-final");
  pulseA("a-during-final-reset");
  pulseB("b-during-final-reset");
}

void setup() {
  pinMode(PIN_QA, INPUT);
  pinMode(PIN_QB, INPUT);
  pinMode(PIN_QC, INPUT);
  pinMode(PIN_QD, INPUT);

  writeLevel(PIN_A, LOW);
  writeLevel(PIN_B, LOW);
  writeLevel(PIN_R01, HIGH);
  writeLevel(PIN_R02, HIGH);
  pinMode(PIN_A, OUTPUT);
  pinMode(PIN_B, OUTPUT);
  pinMode(PIN_R01, OUTPUT);
  pinMode(PIN_R02, OUTPUT);

  Serial.begin(115200);
  Serial.println("Send any character to test the 74HC293");
  while (Serial.available() == 0) {
  }
  while (Serial.available() > 0) Serial.read();
  runChecks();
  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
