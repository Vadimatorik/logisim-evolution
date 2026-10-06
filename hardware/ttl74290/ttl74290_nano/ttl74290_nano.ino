/*
 * Self-check for a 74HC290 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * CP0 toggles Q0 on the falling edge. CP1 advances Q1, Q2 and Q3 through
 * 0, 1, 2, 3 and 4. Both MR inputs clear every output. Both MS inputs force
 * Q3 Q2 Q1 Q0 to 1001 and override the reset. One input of either pair does
 * nothing. Q0 is not wired to CP1 on this bench.
 */

const uint8_t PIN_CP1 = 2;
const uint8_t PIN_MR1 = 3;
const uint8_t PIN_MR2 = 4;
const uint8_t PIN_MS1 = 5;
const uint8_t PIN_MS2 = 6;
const uint8_t PIN_Q2 = 7;
const uint8_t PIN_Q1 = 8;
const uint8_t PIN_Q3 = 9;
const uint8_t PIN_Q0 = 10;
const uint8_t PIN_CP0 = 13;

bool failed = false;
char resultLine[96];
bool cp0 = false;
bool cp1 = false;
bool mr1 = true;
bool mr2 = true;
bool ms1 = false;
bool ms2 = false;
bool q0 = false;
bool q1 = false;
bool q2 = false;
bool q3 = false;

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

bool setToNine() { return ms1 && ms2; }

bool resetToZero() { return mr1 && mr2; }

uint8_t modelNibble() {
  return (q0 ? 1 : 0) | (q1 ? 2 : 0) | (q2 ? 4 : 0) | (q3 ? 8 : 0);
}

uint8_t readNibble() {
  uint8_t value = 0;
  if (digitalRead(PIN_Q0)) value |= 1;
  if (digitalRead(PIN_Q1)) value |= 2;
  if (digitalRead(PIN_Q2)) value |= 4;
  if (digitalRead(PIN_Q3)) value |= 8;
  return value;
}

void check(const char* step) {
  const uint8_t actual = readNibble();
  const uint8_t expected = modelNibble();
  if (actual != expected) noteFailure(step, expected, actual);
}

void applyAsync() {
  if (setToNine()) {
    q0 = true;
    q1 = false;
    q2 = false;
    q3 = true;
  } else if (resetToZero()) {
    q0 = false;
    q1 = false;
    q2 = false;
    q3 = false;
  }
}

void onFallingCp0() {
  if (setToNine() || resetToZero()) {
    applyAsync();
  } else {
    q0 = !q0;
  }
}

void onFallingCp1() {
  if (setToNine() || resetToZero()) {
    applyAsync();
    return;
  }
  uint8_t code = (q1 ? 1 : 0) | (q2 ? 2 : 0) | (q3 ? 4 : 0);
  code = code >= 4 ? 0 : code + 1;
  q1 = code & 1;
  q2 = code & 2;
  q3 = code & 4;
}

void writeLevel(uint8_t pin, bool high) {
  digitalWrite(pin, high ? HIGH : LOW);
}

void setCp0(bool high, const char* step) {
  const bool falling = cp0 && !high;
  cp0 = high;
  writeLevel(PIN_CP0, high);
  settle();
  if (falling) onFallingCp0();
  check(step);
}

void setCp1(bool high, const char* step) {
  const bool falling = cp1 && !high;
  cp1 = high;
  writeLevel(PIN_CP1, high);
  settle();
  if (falling) onFallingCp1();
  check(step);
}

void setReset(uint8_t pin, bool& level, bool high, const char* step) {
  level = high;
  writeLevel(pin, high);
  settle();
  applyAsync();
  check(step);
}

void setNine(uint8_t pin, bool& level, bool high, const char* step) {
  level = high;
  writeLevel(pin, high);
  settle();
  applyAsync();
  check(step);
}

void pulseCp0(const char* step) {
  setCp0(true, step);
  setCp0(false, step);
}

void pulseCp1(const char* step) {
  setCp1(true, step);
  setCp1(false, step);
}

void runChecks() {
  delay(1);
  check("held-reset");
  pulseCp0("cp0-during-reset");
  pulseCp1("cp1-during-reset");

  setReset(PIN_MR1, mr1, false, "release-one-reset");
  pulseCp0("cp0-after-one-reset");

  setReset(PIN_MR1, mr1, true, "both-reset-again");
  setReset(PIN_MR1, mr1, false, "release-mr1");
  setReset(PIN_MR2, mr2, false, "release-mr2");
  setCp0(true, "cp0-rising");
  setCp0(false, "cp0-first-fall");
  pulseCp0("cp0-second-fall");

  pulseCp1("cp1-1");
  pulseCp1("cp1-2");
  pulseCp1("cp1-3");
  pulseCp1("cp1-4");
  pulseCp1("cp1-5");

  setReset(PIN_MR1, mr1, true, "mr1-before-set");
  setReset(PIN_MR2, mr2, true, "mr2-before-set");
  setNine(PIN_MS1, ms1, true, "ms1-only");
  setNine(PIN_MS2, ms2, true, "set-to-nine");
  pulseCp0("cp0-during-set");
  pulseCp1("cp1-during-set");
  setNine(PIN_MS1, ms1, false, "release-ms1");
  setNine(PIN_MS2, ms2, false, "reset-after-set");

  setReset(PIN_MR1, mr1, false, "release-reset-mr1");
  setReset(PIN_MR2, mr2, false, "release-reset-mr2");
  setNine(PIN_MS1, ms1, true, "one-set-input");
  pulseCp1("cp1-with-one-set");
  pulseCp0("cp0-with-one-set");
}

void setup() {
  pinMode(PIN_Q0, INPUT);
  pinMode(PIN_Q1, INPUT);
  pinMode(PIN_Q2, INPUT);
  pinMode(PIN_Q3, INPUT);

  writeLevel(PIN_CP0, LOW);
  writeLevel(PIN_CP1, LOW);
  writeLevel(PIN_MR1, HIGH);
  writeLevel(PIN_MR2, HIGH);
  writeLevel(PIN_MS1, LOW);
  writeLevel(PIN_MS2, LOW);
  pinMode(PIN_CP0, OUTPUT);
  pinMode(PIN_CP1, OUTPUT);
  pinMode(PIN_MR1, OUTPUT);
  pinMode(PIN_MR2, OUTPUT);
  pinMode(PIN_MS1, OUTPUT);
  pinMode(PIN_MS2, OUTPUT);

  Serial.begin(115200);
  Serial.println("Send any character to test the 74HC290");
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
