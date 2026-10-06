/*
 * Self-check for a 74HC73 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each half is a negative-edge J-K flip-flop. nR low forces Q low and nQ high
 * and overrides the clock. With nR high, a falling nCP loads 1 (J=1, K=0),
 * loads 0 (J=0, K=1), holds (J=K=0), or toggles (J=K=1). A rising edge does
 * not capture. Outputs are push-pull, so Q and nQ are read directly.
 * Both nR pins stay low and both clocks stay low until the check starts.
 */

struct Half {
  uint8_t cp;
  uint8_t r;
  uint8_t j;
  uint8_t k;
  uint8_t q;
  uint8_t nq;
  const char* name;
};

const Half FF1 = {2, 3, 13, 4, 11, 12, "ff1"};
const Half FF2 = {5, 6, 7, 10, 9, 8, "ff2"};

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, bool expectedQ, bool actualQ, bool actualNq) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s expected Q=%d nQ=%d got Q=%d nQ=%d",
      step,
      expectedQ ? 1 : 0,
      expectedQ ? 0 : 1,
      actualQ ? 1 : 0,
      actualNq ? 1 : 0);
}

void settle() { delay(1); }

void setJk(const Half& half, bool j, bool k) {
  digitalWrite(half.j, j ? HIGH : LOW);
  digitalWrite(half.k, k ? HIGH : LOW);
}

void expectQ(const Half& half, bool q, const char* step) {
  settle();
  const bool actualQ = digitalRead(half.q) == HIGH;
  const bool actualNq = digitalRead(half.nq) == HIGH;
  if (actualQ != q || actualNq == q) noteFailure(step, q, actualQ, actualNq);
}

void fall(const Half& half) {
  digitalWrite(half.cp, HIGH);
  settle();
  digitalWrite(half.cp, LOW);
  settle();
}

char stepName[32];

const char* step(const Half& half, const char* action) {
  snprintf(stepName, sizeof(stepName), "%s-%s", half.name, action);
  return stepName;
}

void resetHalf(const Half& half, const Half& other, bool otherQ) {
  digitalWrite(half.cp, LOW);
  setJk(half, false, false);
  digitalWrite(half.r, LOW);
  expectQ(half, false, step(half, "reset"));
  expectQ(other, otherQ, step(other, "untouched-reset"));
  digitalWrite(half.r, HIGH);
  expectQ(half, false, step(half, "reset-release"));
  expectQ(other, otherQ, step(other, "untouched-release"));
}

void checkHalf(const Half& half, const Half& other, bool otherQ) {
  resetHalf(half, other, otherQ);

  setJk(half, true, false);
  fall(half);
  expectQ(half, true, step(half, "load-1"));
  expectQ(other, otherQ, step(other, "untouched-load-1"));

  setJk(half, false, false);
  fall(half);
  expectQ(half, true, step(half, "hold"));

  setJk(half, true, true);
  fall(half);
  expectQ(half, false, step(half, "toggle-0"));
  fall(half);
  expectQ(half, true, step(half, "toggle-1"));

  setJk(half, false, true);
  fall(half);
  expectQ(half, false, step(half, "load-0"));

  setJk(half, true, false);
  digitalWrite(half.cp, HIGH);
  expectQ(half, false, step(half, "rising-edge"));
  digitalWrite(half.cp, LOW);
  expectQ(half, true, step(half, "capture-after-rise"));

  setJk(half, false, false);
  digitalWrite(half.cp, HIGH);
  expectQ(half, true, step(half, "clock-high-hold"));
  setJk(half, false, true);
  expectQ(half, true, step(half, "jk-while-high"));
  digitalWrite(half.cp, LOW);
  expectQ(half, false, step(half, "capture-at-fall"));

  setJk(half, true, false);
  fall(half);
  expectQ(half, true, step(half, "load-1-again"));
  digitalWrite(half.cp, HIGH);
  digitalWrite(half.r, LOW);
  expectQ(half, false, step(half, "async-reset"));
  digitalWrite(half.cp, LOW);
  expectQ(half, false, step(half, "edge-during-reset"));
  digitalWrite(half.r, HIGH);
  expectQ(half, false, step(half, "release-no-capture"));
  fall(half);
  expectQ(half, true, step(half, "capture-after-reset"));
  expectQ(other, otherQ, step(other, "untouched-end"));
}

void runChecks() {
  digitalWrite(FF1.cp, LOW);
  digitalWrite(FF2.cp, LOW);
  setJk(FF1, false, false);
  setJk(FF2, false, false);
  digitalWrite(FF1.r, LOW);
  digitalWrite(FF2.r, LOW);
  expectQ(FF1, false, "ff1-both-reset");
  expectQ(FF2, false, "ff2-both-reset");

  digitalWrite(FF1.r, HIGH);
  digitalWrite(FF2.r, HIGH);
  expectQ(FF1, false, "ff1-both-release");
  expectQ(FF2, false, "ff2-both-release");

  checkHalf(FF1, FF2, false);
  checkHalf(FF2, FF1, true);
}

void setup() {
  Serial.begin(115200);
  const uint8_t outputs[] = {FF1.cp, FF1.r, FF1.j, FF1.k, FF2.cp, FF2.r, FF2.j, FF2.k};
  for (uint8_t index = 0; index < sizeof(outputs); index++) pinMode(outputs[index], OUTPUT);
  pinMode(FF1.q, INPUT);
  pinMode(FF1.nq, INPUT);
  pinMode(FF2.q, INPUT);
  pinMode(FF2.nq, INPUT);
  digitalWrite(FF1.cp, LOW);
  digitalWrite(FF2.cp, LOW);
  setJk(FF1, false, false);
  setJk(FF2, false, false);
  digitalWrite(FF1.r, LOW);
  digitalWrite(FF2.r, LOW);
  Serial.println("74HC73 ready. Send any character to start.");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  Serial.println(failed ? resultLine : "RESULT PASS");
}
