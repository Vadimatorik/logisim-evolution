/*
 * Self-check for a 74HC197 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * MR low clears every output and overrides load and both clocks. Otherwise PL
 * low copies P0..P3 onto Q0..Q3. Otherwise CP0 toggles Q0 on the falling edge
 * and CP1 advances Q1..Q3 modulo 8. Q0 is not wired to CP1 on this bench.
 */

const uint8_t PIN_PL = 2;
const uint8_t PIN_MR = 3;
const uint8_t PIN_CP0 = 4;
const uint8_t PIN_CP1 = 5;
const uint8_t PIN_P0 = 6;
const uint8_t PIN_P1 = 7;
const uint8_t PIN_P2 = 8;
const uint8_t PIN_P3 = 9;
const uint8_t PIN_Q0 = 10;
const uint8_t PIN_Q1 = 11;
const uint8_t PIN_Q2 = 12;
const uint8_t PIN_Q3 = A0;

bool failed = false;
char resultLine[96];
bool pl = true;
bool mr = false;
bool cp0 = false;
bool cp1 = false;
bool p0 = false;
bool p1 = false;
bool p2 = false;
bool p3 = false;
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
  if (!mr) {
    q0 = false;
    q1 = false;
    q2 = false;
    q3 = false;
  } else if (!pl) {
    q0 = p0;
    q1 = p1;
    q2 = p2;
    q3 = p3;
  }
}

void onFallingCp0() {
  if (!mr || !pl) {
    applyAsync();
  } else {
    q0 = !q0;
  }
}

void onFallingCp1() {
  if (!mr || !pl) {
    applyAsync();
    return;
  }
  uint8_t code = (q1 ? 1 : 0) | (q2 ? 2 : 0) | (q3 ? 4 : 0);
  code = (code + 1) & 7;
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

void setMr(bool high, const char* step) {
  mr = high;
  writeLevel(PIN_MR, high);
  settle();
  applyAsync();
  check(step);
}

void setPl(bool high, const char* step) {
  pl = high;
  writeLevel(PIN_PL, high);
  settle();
  applyAsync();
  check(step);
}

void setData(uint8_t code) {
  p0 = code & 1;
  p1 = code & 2;
  p2 = code & 4;
  p3 = code & 8;
  writeLevel(PIN_P0, p0);
  writeLevel(PIN_P1, p1);
  writeLevel(PIN_P2, p2);
  writeLevel(PIN_P3, p3);
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
  char step[24];
  delay(1);
  check("held-reset");
  pulseCp0("cp0-during-reset");
  pulseCp1("cp1-during-reset");

  setMr(true, "release-reset");
  setCp0(true, "cp0-rising");
  setCp0(false, "cp0-first-fall");
  pulseCp0("cp0-second-fall");

  for (uint8_t i = 1; i <= 8; i++) {
    snprintf(step, sizeof(step), "cp1-%u", i);
    pulseCp1(step);
  }

  setPl(false, "load-enable");
  for (uint8_t code = 0; code <= 15; code++) {
    setData(code);
    settle();
    applyAsync();
    snprintf(step, sizeof(step), "load-%u", code);
    check(step);
  }
  setPl(true, "load-hold");
  setData(0);
  settle();
  check("data-while-holding");

  setData(0xA);
  setPl(false, "load-ten");
  setMr(false, "reset-over-load");
  pulseCp0("cp0-during-reset-load");
  pulseCp1("cp1-during-reset-load");
  setMr(true, "load-after-reset");
  pulseCp0("cp0-during-load");
  setPl(true, "release-load");
  setCp0(false, "stale-cp0-fall");
  pulseCp0("cp0-after-load");

  setMr(false, "clear-before-cascade");
  setMr(true, "release-before-cascade");
  for (uint8_t i = 1; i <= 16; i++) {
    const bool wasHigh = q0;
    snprintf(step, sizeof(step), "cascade-%u", i);
    pulseCp0(step);
    if (wasHigh) pulseCp1(step);
  }
}

void setup() {
  pinMode(PIN_Q0, INPUT);
  pinMode(PIN_Q1, INPUT);
  pinMode(PIN_Q2, INPUT);
  pinMode(PIN_Q3, INPUT);

  writeLevel(PIN_PL, HIGH);
  writeLevel(PIN_MR, LOW);
  writeLevel(PIN_CP0, LOW);
  writeLevel(PIN_CP1, LOW);
  writeLevel(PIN_P0, LOW);
  writeLevel(PIN_P1, LOW);
  writeLevel(PIN_P2, LOW);
  writeLevel(PIN_P3, LOW);
  pinMode(PIN_PL, OUTPUT);
  pinMode(PIN_MR, OUTPUT);
  pinMode(PIN_CP0, OUTPUT);
  pinMode(PIN_CP1, OUTPUT);
  pinMode(PIN_P0, OUTPUT);
  pinMode(PIN_P1, OUTPUT);
  pinMode(PIN_P2, OUTPUT);
  pinMode(PIN_P3, OUTPUT);

  Serial.begin(115200);
  Serial.println("Send any character to test the 74HC197");
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
