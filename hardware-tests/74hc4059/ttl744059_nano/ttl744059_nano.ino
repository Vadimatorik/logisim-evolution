/*
 * Self-check for a 74HC4059 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * J13-J16 are tied to GND, so decade 4 stays 0. CP counts on the rising edge.
 * The first Q after master preset is N + 1 clocks late; later periods are N.
 */

const uint8_t PIN_CP = 2;
const uint8_t PIN_LE = 3;
const uint8_t PIN_KA = 4;
const uint8_t PIN_KB = 5;
const uint8_t PIN_KC = 6;
// J1..J12. J13..J16 are wired to GND.
const uint8_t PIN_J[12] = {7, 8, 9, 10, 11, 12, 13, A0, A1, A2, A3, A4};
const uint8_t PIN_Q = A5;
const unsigned HALF_PERIOD_US = 10;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, const char* detail) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s %s", step, detail);
}

void setJam(uint16_t jam) {
  for (uint8_t bit = 0; bit < 12; bit++) {
    digitalWrite(PIN_J[bit], (jam & (1U << bit)) ? HIGH : LOW);
  }
}

void setMode(int mode, bool latch) {
  bool kaHigh = false;
  bool kbHigh = false;
  bool kcHigh = false;
  if (mode == 2) {
    kaHigh = true;
    kbHigh = true;
    kcHigh = true;
  } else if (mode == 4) {
    kbHigh = true;
    kcHigh = true;
  } else if (mode == 5) {
    kaHigh = true;
    kcHigh = true;
  } else if (mode == 8) {
    kcHigh = true;
  } else if (mode == 10) {
    kaHigh = true;
    kbHigh = true;
  }
  digitalWrite(PIN_LE, latch ? HIGH : LOW);
  digitalWrite(PIN_KA, kaHigh ? HIGH : LOW);
  digitalWrite(PIN_KB, kbHigh ? HIGH : LOW);
  digitalWrite(PIN_KC, kcHigh ? HIGH : LOW);
}

uint16_t jamWord(int mode, int dec1, int dec2, int dec3, int dec5) {
  uint16_t word = (uint16_t) ((dec2 & 0xF) << 4) | (uint16_t) ((dec3 & 0xF) << 8);
  if (mode == 2) word |= (uint16_t) ((dec1 & 0x1) | ((dec5 & 0x7) << 1));
  else if (mode == 4) word |= (uint16_t) ((dec1 & 0x3) | ((dec5 & 0x3) << 2));
  else if (mode == 10) word |= (uint16_t) (dec1 & 0xF);
  else word |= (uint16_t) ((dec1 & 0x7) | ((dec5 & 0x1) << 3));
  return word;
}

bool riseAndRead() {
  digitalWrite(PIN_CP, HIGH);
  delayMicroseconds(HALF_PERIOD_US);
  const bool whileHigh = digitalRead(PIN_Q) == HIGH;
  digitalWrite(PIN_CP, LOW);
  delayMicroseconds(HALF_PERIOD_US);
  const bool whileLow = digitalRead(PIN_Q) == HIGH;
  if (whileHigh != whileLow) noteFailure("CP fall", "Q changed");
  return whileHigh;
}

bool masterPreset(uint16_t jam) {
  setJam(jam);
  digitalWrite(PIN_LE, LOW);
  digitalWrite(PIN_KA, LOW);
  digitalWrite(PIN_KB, LOW);
  digitalWrite(PIN_KC, LOW);
  digitalWrite(PIN_CP, LOW);
  delayMicroseconds(HALF_PERIOD_US);
  bool high = false;
  for (uint8_t clock = 0; clock < 3; clock++) high = riseAndRead() || high;
  return !high;
}

int edgesUntilHigh(int limit) {
  for (int edges = 1; edges <= limit; edges++) {
    if (riseAndRead()) return edges;
  }
  return -1;
}

void checkDivider(const char* name, int mode, uint16_t jam, int divisor) {
  const bool presetOk = masterPreset(jam);
  setMode(mode, false);
  const int first = edgesUntilHigh(divisor + 5);
  const int second = edgesUntilHigh(divisor + 5);
  const bool pass = presetOk && first == divisor + 1 && second == divisor;
  Serial.print(name);
  Serial.print(" first=");
  Serial.print(first);
  Serial.print(" second=");
  Serial.print(second);
  Serial.println(pass ? " PASS" : " FAIL");
  if (!pass) {
    char detail[48];
    snprintf(detail, sizeof(detail), "first=%d second=%d", first, second);
    noteFailure(name, detail);
  }
}

void checkTimerLatch() {
  const bool presetOk = masterPreset(jamWord(8, 3, 0, 0, 0));
  setMode(8, true);
  const int first = edgesUntilHigh(8);
  bool held = first == 4;
  for (uint8_t extra = 0; extra < 5; extra++) held = riseAndRead() && held;
  digitalWrite(PIN_LE, LOW);
  delayMicroseconds(HALF_PERIOD_US);
  const bool released = digitalRead(PIN_Q) == LOW;
  const bool nextPulse = riseAndRead();
  const bool pass = presetOk && held && released && nextPulse;
  Serial.println(pass ? "timer latch PASS" : "timer latch FAIL");
  if (!pass) noteFailure("timer latch", "Q did not follow LE");
}

void checkNoLatch() {
  const bool presetOk = masterPreset(jamWord(10, 0, 1, 0, 0));
  digitalWrite(PIN_LE, HIGH);
  digitalWrite(PIN_KA, LOW);
  digitalWrite(PIN_KB, HIGH);
  digitalWrite(PIN_KC, LOW);
  const int first = edgesUntilHigh(16);
  const int second = edgesUntilHigh(16);
  const bool pass = presetOk && first == 11 && second == 10;
  Serial.print("no latch first=");
  Serial.print(first);
  Serial.print(" second=");
  Serial.print(second);
  Serial.println(pass ? " PASS" : " FAIL");
  if (!pass) noteFailure("no latch", "Q was held or the period was wrong");
}

void checkPresetInhibit() {
  const bool presetOk = masterPreset(jamWord(10, 0, 1, 0, 0));
  digitalWrite(PIN_LE, LOW);
  digitalWrite(PIN_KA, LOW);
  digitalWrite(PIN_KB, HIGH);
  digitalWrite(PIN_KC, LOW);
  const int first = edgesUntilHigh(16);
  setJam(0x0FFF);
  const int second = edgesUntilHigh(10005);
  const bool pass = presetOk && first == 11 && second == 10000;
  Serial.print("preset inhibit first=");
  Serial.print(first);
  Serial.print(" second=");
  Serial.print(second);
  Serial.println(pass ? " PASS" : " FAIL");
  if (!pass) noteFailure("preset inhibit", "fixed divide-by-10000 failed");
}

void checkFallingEdge() {
  const bool presetOk = masterPreset(jamWord(8, 3, 0, 0, 0));
  setMode(8, false);
  const bool armedHigh = riseAndRead();
  digitalWrite(PIN_CP, HIGH);
  delayMicroseconds(HALF_PERIOD_US);
  const bool duringHigh = digitalRead(PIN_Q) == HIGH;
  digitalWrite(PIN_CP, LOW);
  delayMicroseconds(HALF_PERIOD_US);
  const bool duringLow = digitalRead(PIN_Q) == HIGH;
  const bool early = riseAndRead();
  const bool terminal = riseAndRead();
  const bool pass = presetOk && !armedHigh && !duringHigh && !duringLow && !early && terminal;
  Serial.println(pass ? "falling edge PASS" : "falling edge FAIL");
  if (!pass) noteFailure("falling edge", "CP fall changed the count");
}

void checkMasterPresetHold() {
  const uint16_t jam = jamWord(8, 3, 0, 0, 0);
  bool stayedLow = masterPreset(jam);
  for (uint8_t extra = 0; extra < 7; extra++) stayedLow = !riseAndRead() && stayedLow;
  setMode(8, false);
  const int first = edgesUntilHigh(8);
  const bool pass = stayedLow && first == 4;
  Serial.print("master preset hold first=");
  Serial.print(first);
  Serial.println(pass ? " PASS" : " FAIL");
  if (!pass) noteFailure("master preset", "preset clocks shortened N");
}

void runTest() {
  failed = false;
  resultLine[0] = '\0';
  Serial.println("74HC4059 test");
  checkDivider("divide-by-2", 2, jamWord(2, 1, 1, 0, 0), 3);
  checkDivider("divide-by-4", 4, jamWord(4, 3, 1, 0, 0), 7);
  checkDivider("divide-by-5", 5, jamWord(5, 4, 0, 0, 0), 4);
  checkDivider("divide-by-8", 8, jamWord(8, 3, 0, 0, 0), 3);
  checkDivider("divide-by-10", 10, jamWord(10, 0, 1, 0, 0), 10);
  checkDivider("divide-by-2000", 2, jamWord(2, 0, 0, 0, 1), 2000);
  checkTimerLatch();
  checkNoLatch();
  checkDivider("binary preset", 8, jamWord(8, 0, 15, 0, 0), 120);
  checkPresetInhibit();
  checkFallingEdge();
  checkMasterPresetHold();
  digitalWrite(PIN_CP, LOW);
  Serial.println(failed ? resultLine : "RESULT PASS");
}

void setup() {
  Serial.begin(115200);
  const uint8_t outputs[] = {
      PIN_CP, PIN_LE, PIN_KA, PIN_KB, PIN_KC,
      7, 8, 9, 10, 11, 12, 13, A0, A1, A2, A3, A4};
  for (uint8_t index = 0; index < sizeof(outputs); index++) {
    pinMode(outputs[index], OUTPUT);
    digitalWrite(outputs[index], LOW);
  }
  pinMode(PIN_Q, INPUT);
  Serial.println("74HC4059 ready. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  runTest();
}
