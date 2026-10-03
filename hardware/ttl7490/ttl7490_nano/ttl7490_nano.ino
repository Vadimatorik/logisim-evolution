/*
 * Self-check for a 74HC90 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * CKA toggles QA on the falling edge. CKB advances QB, QC and QD through
 * 0, 1, 2, 3 and 4. Both R0 inputs clear every output. Both R9 inputs force
 * QD QC QB QA to 1001 and override the reset. One input of either pair does
 * nothing. QA is not wired to CKB on this bench.
 */

const uint8_t PIN_CKB = 2;
const uint8_t PIN_R01 = 3;
const uint8_t PIN_R02 = 4;
const uint8_t PIN_R91 = 5;
const uint8_t PIN_R92 = 6;
const uint8_t PIN_QC = 7;
const uint8_t PIN_QB = 8;
const uint8_t PIN_QD = 9;
const uint8_t PIN_QA = 10;
const uint8_t PIN_CKA = 13;

bool failed = false;
char resultLine[96];
bool cka = false;
bool ckb = false;
bool r01 = true;
bool r02 = true;
bool r91 = false;
bool r92 = false;
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

bool setToNine() { return r91 && r92; }

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
  if (setToNine()) {
    qa = true;
    qb = false;
    qc = false;
    qd = true;
  } else if (resetToZero()) {
    qa = false;
    qb = false;
    qc = false;
    qd = false;
  }
}

void onFallingCka() {
  if (setToNine() || resetToZero()) {
    applyAsync();
  } else {
    qa = !qa;
  }
}

void onFallingCkb() {
  if (setToNine() || resetToZero()) {
    applyAsync();
    return;
  }
  uint8_t code = (qb ? 1 : 0) | (qc ? 2 : 0) | (qd ? 4 : 0);
  code = code >= 4 ? 0 : code + 1;
  qb = code & 1;
  qc = code & 2;
  qd = code & 4;
}

void writeLevel(uint8_t pin, bool high) {
  digitalWrite(pin, high ? HIGH : LOW);
}

void setCka(bool high, const char* step) {
  const bool falling = cka && !high;
  cka = high;
  writeLevel(PIN_CKA, high);
  settle();
  if (falling) onFallingCka();
  check(step);
}

void setCkb(bool high, const char* step) {
  const bool falling = ckb && !high;
  ckb = high;
  writeLevel(PIN_CKB, high);
  settle();
  if (falling) onFallingCkb();
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

void pulseCka(const char* step) {
  setCka(true, step);
  setCka(false, step);
}

void pulseCkb(const char* step) {
  setCkb(true, step);
  setCkb(false, step);
}

void runChecks() {
  delay(1);
  check("held-reset");
  pulseCka("cka-during-reset");
  pulseCkb("ckb-during-reset");

  setReset(PIN_R01, r01, false, "release-one-reset");
  pulseCka("cka-after-one-reset");

  setReset(PIN_R01, r01, true, "both-reset-again");
  setReset(PIN_R01, r01, false, "release-r01");
  setReset(PIN_R02, r02, false, "release-r02");
  setCka(true, "cka-rising");
  setCka(false, "cka-first-fall");
  pulseCka("cka-second-fall");

  pulseCkb("ckb-1");
  pulseCkb("ckb-2");
  pulseCkb("ckb-3");
  pulseCkb("ckb-4");
  pulseCkb("ckb-5");

  setReset(PIN_R01, r01, true, "r01-before-set");
  setReset(PIN_R02, r02, true, "r02-before-set");
  setNine(PIN_R91, r91, true, "r91-only");
  setNine(PIN_R92, r92, true, "set-to-nine");
  pulseCka("cka-during-set");
  pulseCkb("ckb-during-set");
  setNine(PIN_R91, r91, false, "release-r91");
  setNine(PIN_R92, r92, false, "reset-after-set");

  setReset(PIN_R01, r01, false, "release-reset-r01");
  setReset(PIN_R02, r02, false, "release-reset-r02");
  setNine(PIN_R91, r91, true, "one-set-input");
  pulseCkb("ckb-with-one-set");
  pulseCka("cka-with-one-set");
}

void setup() {
  pinMode(PIN_QA, INPUT);
  pinMode(PIN_QB, INPUT);
  pinMode(PIN_QC, INPUT);
  pinMode(PIN_QD, INPUT);

  writeLevel(PIN_CKA, LOW);
  writeLevel(PIN_CKB, LOW);
  writeLevel(PIN_R01, HIGH);
  writeLevel(PIN_R02, HIGH);
  writeLevel(PIN_R91, LOW);
  writeLevel(PIN_R92, LOW);
  pinMode(PIN_CKA, OUTPUT);
  pinMode(PIN_CKB, OUTPUT);
  pinMode(PIN_R01, OUTPUT);
  pinMode(PIN_R02, OUTPUT);
  pinMode(PIN_R91, OUTPUT);
  pinMode(PIN_R92, OUTPUT);

  Serial.begin(115200);
  Serial.println("Send any character to test the 74HC90");
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
