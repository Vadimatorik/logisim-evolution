/*
 * Self-check for a 74HC40103 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * MR asynchronously loads 255 and wins over PL. PL asynchronously loads P7..P0.
 * PE loads on the rising edge of CP and ignores TE. Counting needs TE low.
 * TC is active low only while the code is 0 and TE is low. The count itself
 * is not pinned out, so each check infers it from TC.
 * MR stays low, CP stays low, PL and PE stay high and TE stays high until
 * the check starts. The counter is therefore already at 255.
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
const uint8_t PIN_TC = A0;
const uint8_t PIN_PE = A1;

const uint8_t LENGTHS[] = {1, 2, 7, 8, 15, 16, 127, 128, 254, 255};

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

void setData(uint8_t code) {
  digitalWrite(PIN_P0, (code & 1) ? HIGH : LOW);
  digitalWrite(PIN_P1, (code & 2) ? HIGH : LOW);
  digitalWrite(PIN_P2, (code & 4) ? HIGH : LOW);
  digitalWrite(PIN_P3, (code & 8) ? HIGH : LOW);
  digitalWrite(PIN_P4, (code & 16) ? HIGH : LOW);
  digitalWrite(PIN_P5, (code & 32) ? HIGH : LOW);
  digitalWrite(PIN_P6, (code & 64) ? HIGH : LOW);
  digitalWrite(PIN_P7, (code & 128) ? HIGH : LOW);
}

void expectTc(bool low, const char* step) {
  settle();
  const uint8_t actual = digitalRead(PIN_TC) ? 1 : 0;
  const uint8_t expected = low ? 0 : 1;
  if (actual != expected) noteFailure(step, expected, actual);
}

void clockPulse() {
  digitalWrite(PIN_CP, LOW);
  settle();
  digitalWrite(PIN_CP, HIGH);
  settle();
  digitalWrite(PIN_CP, LOW);
  settle();
}

void clockPulses(int count) {
  for (int i = 0; i < count; i++) clockPulse();
}

void masterReset() {
  digitalWrite(PIN_MR, LOW);
  digitalWrite(PIN_PL, HIGH);
  digitalWrite(PIN_PE, HIGH);
  settle();
  digitalWrite(PIN_MR, HIGH);
  settle();
}

void asyncLoad(uint8_t code) {
  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_PL, LOW);
  setData(code);
  settle();
  digitalWrite(PIN_PL, HIGH);
  settle();
}

void checkResetOverridesPreset() {
  digitalWrite(PIN_TE, LOW);
  digitalWrite(PIN_PL, LOW);
  digitalWrite(PIN_PE, LOW);
  setData(0);
  digitalWrite(PIN_MR, LOW);
  expectTc(false, "reset over preset");
  digitalWrite(PIN_PL, HIGH);
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_MR, HIGH);
  expectTc(false, "count after reset");
}

void checkAsyncZero() {
  digitalWrite(PIN_TE, LOW);
  digitalWrite(PIN_PL, LOW);
  setData(0);
  expectTc(true, "async zero");
  digitalWrite(PIN_TE, HIGH);
  expectTc(false, "te releases tc");
  digitalWrite(PIN_TE, LOW);
  expectTc(true, "te restores tc");
  digitalWrite(PIN_PL, HIGH);
  setData(7);
  expectTc(true, "jam ignored after preset");
}

void checkSyncZero() {
  masterReset();
  digitalWrite(PIN_TE, LOW);
  digitalWrite(PIN_PE, LOW);
  digitalWrite(PIN_PL, HIGH);
  setData(0);
  expectTc(false, "sync zero before clock");
  clockPulse();
  expectTc(true, "sync zero");
  digitalWrite(PIN_PE, HIGH);
}

void checkLengths() {
  for (uint8_t index = 0; index < sizeof(LENGTHS); index++) {
    const uint8_t code = LENGTHS[index];
    asyncLoad(code);
    digitalWrite(PIN_TE, LOW);
    digitalWrite(PIN_PE, HIGH);
    expectTc(false, "before terminal");
    clockPulses(code);
    expectTc(true, "terminal");
    clockPulse();
    expectTc(false, "wrap after terminal");
  }
}

void checkInhibit() {
  asyncLoad(1);
  digitalWrite(PIN_TE, HIGH);
  digitalWrite(PIN_PE, HIGH);
  clockPulses(4);
  expectTc(false, "inhibit");
  digitalWrite(PIN_TE, LOW);
  clockPulse();
  expectTc(true, "count after inhibit");
}

void checkSyncLoadIgnoresTe() {
  masterReset();
  digitalWrite(PIN_TE, HIGH);
  digitalWrite(PIN_PE, LOW);
  digitalWrite(PIN_PL, HIGH);
  setData(0);
  clockPulse();
  expectTc(false, "loaded zero while te high");
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_TE, LOW);
  expectTc(true, "te after sync load");
}

void checkFallingClock() {
  masterReset();
  digitalWrite(PIN_TE, HIGH);
  digitalWrite(PIN_CP, HIGH);
  settle();
  asyncLoad(1);
  digitalWrite(PIN_TE, LOW);
  expectTc(false, "before falling clock");
  digitalWrite(PIN_CP, LOW);
  expectTc(false, "falling clock");
  digitalWrite(PIN_CP, HIGH);
  expectTc(true, "rising clock");
  digitalWrite(PIN_CP, LOW);
}

void checkModulusAfterReset() {
  masterReset();
  digitalWrite(PIN_TE, LOW);
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_PL, HIGH);
  clockPulses(254);
  expectTc(false, "before modulus");
  clockPulse();
  expectTc(true, "modulus");
}

void runChecks() {
  checkResetOverridesPreset();
  checkAsyncZero();
  checkSyncZero();
  checkLengths();
  checkInhibit();
  checkSyncLoadIgnoresTe();
  checkFallingClock();
  checkModulusAfterReset();
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

  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_MR, LOW);
  digitalWrite(PIN_PL, HIGH);
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_TE, HIGH);
  setData(0);

  Serial.begin(115200);
  Serial.println("74HC40103 Nano check. Send any character to start.");
}

void loop() {
  if (Serial.available() <= 0) return;
  while (Serial.available() > 0) Serial.read();
  failed = false;
  runChecks();
  Serial.println(failed ? resultLine : "RESULT PASS");
  while (true) delay(1000);
}
