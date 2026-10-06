/*
 * Self-check for a 74HC40102 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * The package has no Q pins. TC is active low only while the code is 00 and TE
 * is low, including while CP is high. MR asynchronously loads 99 and overrides
 * PL. PL asynchronously jams P0..P7 and stays transparent. On the rising edge
 * of CP, PE low loads the jam inputs even if TE is high; TE low counts down.
 * A zero nibble wraps to 9 and borrows. Illegal nibbles A..F use that same rule.
 * MR stays low, CP stays low, PL and PE stay high and TE stays high until the
 * check starts.
 */

const uint8_t PIN_CP = 2;
const uint8_t PIN_MR = 3;
const uint8_t PIN_TE = 4;
const uint8_t PIN_P0 = 5;
const uint8_t PIN_P1 = 6;
const uint8_t PIN_P2 = 7;
const uint8_t PIN_P3 = 8;
const uint8_t PIN_PL = 9;
const uint8_t PIN_P4 = 10;
const uint8_t PIN_P5 = 11;
const uint8_t PIN_P6 = 12;
const uint8_t PIN_P7 = 13;
const uint8_t PIN_PE = A0;
const uint8_t PIN_TC = A1;

bool failed = false;
char resultLine[96];

void noteFailure(const char* text) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s", text);
}

void noteNumbers(const char* step, int expected, int actual) {
  if (failed) return;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s expected %d got %d",
      step,
      expected,
      actual);
  failed = true;
}

void settle() { delay(1); }

void setJam(uint8_t code) {
  digitalWrite(PIN_P0, (code & 0x01) ? HIGH : LOW);
  digitalWrite(PIN_P1, (code & 0x02) ? HIGH : LOW);
  digitalWrite(PIN_P2, (code & 0x04) ? HIGH : LOW);
  digitalWrite(PIN_P3, (code & 0x08) ? HIGH : LOW);
  digitalWrite(PIN_P4, (code & 0x10) ? HIGH : LOW);
  digitalWrite(PIN_P5, (code & 0x20) ? HIGH : LOW);
  digitalWrite(PIN_P6, (code & 0x40) ? HIGH : LOW);
  digitalWrite(PIN_P7, (code & 0x80) ? HIGH : LOW);
}

void expectTc(bool wantLow, const char* step) {
  settle();
  const bool actualLow = digitalRead(PIN_TC) == LOW;
  if (actualLow == wantLow) return;
  char text[96];
  snprintf(
      text,
      sizeof(text),
      "%s expected TC %s got %s",
      step,
      wantLow ? "low" : "high",
      actualLow ? "low" : "high");
  noteFailure(text);
}

void clockPulse() {
  digitalWrite(PIN_CP, LOW);
  settle();
  digitalWrite(PIN_CP, HIGH);
  settle();
  digitalWrite(PIN_CP, LOW);
  settle();
}

void asyncLoad(uint8_t code) {
  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_PL, LOW);
  setJam(code);
  settle();
}

void releasePreset() {
  digitalWrite(PIN_PL, HIGH);
  digitalWrite(PIN_PE, HIGH);
  settle();
}

void armCount() {
  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_PL, HIGH);
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_TE, LOW);
  settle();
}

int clocksUntilTc(int limit) {
  for (int clocks = 1; clocks <= limit; clocks++) {
    clockPulse();
    if (digitalRead(PIN_TC) == LOW) return clocks;
  }
  return -1;
}

void expectClocks(uint8_t code, int expected, const char* step) {
  asyncLoad(code);
  armCount();
  expectTc(false, step);
  const int actual = clocksUntilTc(expected + 2);
  if (actual != expected) noteNumbers(step, expected, actual);
  expectTc(true, step);
}

void checkTransparentZeroAndClockLevel() {
  asyncLoad(0x00);
  digitalWrite(PIN_TE, LOW);
  settle();
  expectTc(true, "async zero");

  digitalWrite(PIN_CP, HIGH);
  expectTc(true, "tc while cp high");
  digitalWrite(PIN_CP, LOW);
  settle();

  digitalWrite(PIN_TE, HIGH);
  expectTc(false, "te high at zero");
  digitalWrite(PIN_TE, LOW);
  expectTc(true, "te low at zero");
}

void checkResetOverridesPreset() {
  asyncLoad(0x00);
  digitalWrite(PIN_TE, LOW);
  digitalWrite(PIN_MR, LOW);
  expectTc(false, "reset overrides preset");

  digitalWrite(PIN_MR, HIGH);
  expectTc(true, "preset returns after reset");
  releasePreset();
}

void checkSyncPresetBeatsCount() {
  asyncLoad(0x05);
  releasePreset();
  digitalWrite(PIN_TE, LOW);
  digitalWrite(PIN_PE, LOW);
  setJam(0x01);
  clockPulse();
  expectTc(false, "sync preset is not zero");
  digitalWrite(PIN_PE, HIGH);
  clockPulse();
  expectTc(true, "count from preset one");
}

void checkEnableHolds() {
  asyncLoad(0x05);
  releasePreset();
  digitalWrite(PIN_TE, HIGH);
  clockPulse();
  clockPulse();
  clockPulse();
  digitalWrite(PIN_TE, LOW);
  expectTc(false, "hold left the code above zero");
  const int actual = clocksUntilTc(7);
  if (actual != 5) noteNumbers("clocks after hold", 5, actual);
}

void checkDecadeDistances() {
  expectClocks(0x10, 10, "bcd 10");
  expectClocks(0x20, 20, "bcd 20");
}

void checkResetCycle() {
  digitalWrite(PIN_PL, HIGH);
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_TE, LOW);
  digitalWrite(PIN_MR, LOW);
  expectTc(false, "reset is not zero");
  digitalWrite(PIN_MR, HIGH);
  settle();
  const int actual = clocksUntilTc(101);
  if (actual != 99) noteNumbers("clocks from 99", 99, actual);
  clockPulse();
  expectTc(false, "leave zero for 99");
}

void checkIllegalCodes() {
  expectClocks(0x0A, 10, "illegal 0x0A");
  expectClocks(0x0F, 15, "illegal 0x0F");
  expectClocks(0xA0, 100, "illegal 0xA0");
  expectClocks(0xFF, 165, "illegal 0xFF");
}

void setup() {
  pinMode(PIN_CP, OUTPUT);
  pinMode(PIN_MR, OUTPUT);
  pinMode(PIN_TE, OUTPUT);
  pinMode(PIN_P0, OUTPUT);
  pinMode(PIN_P1, OUTPUT);
  pinMode(PIN_P2, OUTPUT);
  pinMode(PIN_P3, OUTPUT);
  pinMode(PIN_PL, OUTPUT);
  pinMode(PIN_P4, OUTPUT);
  pinMode(PIN_P5, OUTPUT);
  pinMode(PIN_P6, OUTPUT);
  pinMode(PIN_P7, OUTPUT);
  pinMode(PIN_PE, OUTPUT);
  pinMode(PIN_TC, INPUT);

  digitalWrite(PIN_MR, LOW);
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_PL, HIGH);
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_TE, HIGH);
  setJam(0);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  checkTransparentZeroAndClockLevel();
  checkResetOverridesPreset();
  checkSyncPresetBeatsCount();
  checkEnableHolds();
  checkDecadeDistances();
  checkResetCycle();
  checkIllegalCodes();

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
