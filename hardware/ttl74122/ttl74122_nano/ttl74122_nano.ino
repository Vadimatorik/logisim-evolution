/*
 * Self-check for a 74HC122 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * One retriggerable monostable. Q is high for about 450 us with 10 kOhm and
 * 100 nF; nQ is the complement. A low CLR clears the pulse. Rising B triggers
 * when the other B is high and at least one A is low. Falling A triggers when
 * both A inputs were high and both B inputs are high. Rising CLR triggers when
 * at least one A is low and both B inputs are high. Another valid edge during
 * the pulse extends it. CLR stays low until the check starts.
 */

const uint8_t PIN_A1 = 2;
const uint8_t PIN_A2 = 3;
const uint8_t PIN_B1 = 4;
const uint8_t PIN_B2 = 5;
const uint8_t PIN_CLR = 6;
const uint8_t PIN_NQ = 7;
const uint8_t PIN_Q = 8;

const unsigned long WIDTH_MIN_US = 250;
const unsigned long WIDTH_MAX_US = 700;
const unsigned long EDGE_TIMEOUT_US = 2000;
const unsigned long PULSE_TIMEOUT_US = 4000;

enum Kind { KIND_B1, KIND_B2, KIND_A1, KIND_A2, KIND_CLR };

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, const char* detail) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s %s", step, detail);
}

void settle() { delayMicroseconds(20); }

void drive(bool a1, bool a2, bool b1, bool b2, bool clr) {
  digitalWrite(PIN_A1, a1 ? HIGH : LOW);
  digitalWrite(PIN_A2, a2 ? HIGH : LOW);
  digitalWrite(PIN_B1, b1 ? HIGH : LOW);
  digitalWrite(PIN_B2, b2 ? HIGH : LOW);
  digitalWrite(PIN_CLR, clr ? HIGH : LOW);
}

void expectLevels(bool qHigh, const char* step) {
  settle();
  const bool q = digitalRead(PIN_Q) == HIGH;
  const bool nq = digitalRead(PIN_NQ) == HIGH;
  if (q != qHigh || nq == qHigh) {
    char detail[24];
    snprintf(detail, sizeof(detail), "Q=%d nQ=%d", q ? 1 : 0, nq ? 1 : 0);
    noteFailure(step, detail);
  }
}

void arm(Kind kind, const char* step) {
  digitalWrite(PIN_CLR, LOW);
  if (kind == KIND_B1) drive(false, true, false, true, false);
  else if (kind == KIND_B2) drive(true, false, true, false, false);
  else if (kind == KIND_CLR) drive(false, true, true, true, false);
  else drive(true, true, true, true, false);
  settle();
  if (kind != KIND_CLR) {
    digitalWrite(PIN_CLR, HIGH);
    settle();
  }
  expectLevels(false, step);
}

void fire(Kind kind) {
  if (kind == KIND_B1) digitalWrite(PIN_B1, HIGH);
  else if (kind == KIND_B2) digitalWrite(PIN_B2, HIGH);
  else if (kind == KIND_A1) digitalWrite(PIN_A1, LOW);
  else if (kind == KIND_A2) digitalWrite(PIN_A2, LOW);
  else digitalWrite(PIN_CLR, HIGH);
}

unsigned long waitUntil(unsigned long start, unsigned long limit, bool qHigh) {
  while ((unsigned long)(micros() - start) < limit) {
    if ((digitalRead(PIN_Q) == HIGH) == qHigh) return micros();
  }
  return 0;
}

unsigned long measurePulse(Kind kind, const char* step) {
  arm(kind, step);
  const unsigned long started = micros();
  fire(kind);
  const unsigned long rose = waitUntil(started, EDGE_TIMEOUT_US, true);
  if (rose == 0) {
    noteFailure(step, "Q stayed low");
    return 0;
  }
  expectLevels(true, step);
  const unsigned long fell = waitUntil(rose, PULSE_TIMEOUT_US, false);
  if (fell == 0) {
    noteFailure(step, "Q stayed high");
    return 0;
  }
  expectLevels(false, step);
  return fell - rose;
}

unsigned long checkWidth(Kind kind, const char* step) {
  const unsigned long width = measurePulse(kind, step);
  if (width == 0) return 0;
  Serial.print(step);
  Serial.print(' ');
  Serial.print(width);
  Serial.println(" us");
  if (width < WIDTH_MIN_US || width > WIDTH_MAX_US) {
    char detail[24];
    snprintf(detail, sizeof(detail), "%lu us", width);
    noteFailure(step, detail);
    return 0;
  }
  return width;
}

void checkClearBlocks() {
  const char* step = "clear-hold";
  drive(true, true, false, false, false);
  settle();
  expectLevels(false, step);
  drive(false, true, true, true, false);
  delayMicroseconds(1500);
  expectLevels(false, step);
}

void checkOtherADoesNotRetrigger() {
  const char* step = "a1-while-a2-low";
  const unsigned long width = checkWidth(KIND_A2, "fall-a2");
  if (width == 0) return;
  digitalWrite(PIN_A1, LOW);
  delayMicroseconds(1500);
  expectLevels(false, step);
}

void checkRetrigger(unsigned long width) {
  if (width == 0) return;
  const char* step = "retrigger";
  arm(KIND_B1, step);
  const unsigned long started = micros();
  fire(KIND_B1);
  const unsigned long rose = waitUntil(started, EDGE_TIMEOUT_US, true);
  if (rose == 0) {
    noteFailure(step, "Q stayed low");
    return;
  }
  const unsigned long halfway = rose + width / 2;
  while ((long)(halfway - micros()) > 0) {
  }
  if (digitalRead(PIN_Q) == LOW) {
    noteFailure(step, "ended early");
    return;
  }
  digitalWrite(PIN_B1, LOW);
  delayMicroseconds(8);
  digitalWrite(PIN_B1, HIGH);
  const unsigned long stillHighAt = rose + width + 50;
  while ((long)(stillHighAt - micros()) > 0) {
  }
  expectLevels(true, step);
  const unsigned long fell = waitUntil(micros(), width + 500, false);
  if (fell == 0) {
    noteFailure(step, "Q stayed high");
    return;
  }
  expectLevels(false, step);
}

void checkResetTruncates() {
  const char* step = "reset-cut";
  arm(KIND_B1, step);
  const unsigned long started = micros();
  fire(KIND_B1);
  if (waitUntil(started, EDGE_TIMEOUT_US, true) == 0) {
    noteFailure(step, "Q stayed low");
    return;
  }
  delayMicroseconds(80);
  digitalWrite(PIN_CLR, LOW);
  const unsigned long cut = micros();
  if (waitUntil(cut, 150, false) == 0) {
    noteFailure(step, "Q stayed high");
    return;
  }
  delayMicroseconds(800);
  expectLevels(false, step);
}

void checkBlocked() {
  const char* step = "block-other-b";
  digitalWrite(PIN_CLR, LOW);
  drive(false, true, false, false, false);
  settle();
  digitalWrite(PIN_CLR, HIGH);
  settle();
  digitalWrite(PIN_B1, HIGH);
  delayMicroseconds(1500);
  expectLevels(false, step);

  const char* bothA = "block-both-a";
  digitalWrite(PIN_CLR, LOW);
  drive(true, true, false, true, false);
  settle();
  digitalWrite(PIN_CLR, HIGH);
  settle();
  digitalWrite(PIN_B1, HIGH);
  delayMicroseconds(1500);
  expectLevels(false, bothA);

  const char* lowB = "block-b-low";
  digitalWrite(PIN_CLR, LOW);
  drive(true, true, true, false, false);
  settle();
  digitalWrite(PIN_CLR, HIGH);
  settle();
  digitalWrite(PIN_A1, LOW);
  delayMicroseconds(1500);
  expectLevels(false, lowB);
}

void runCheck() {
  checkClearBlocks();
  const unsigned long width = checkWidth(KIND_B1, "rise-b1");
  checkWidth(KIND_B2, "rise-b2");
  checkWidth(KIND_A1, "fall-a1");
  checkOtherADoesNotRetrigger();
  checkWidth(KIND_CLR, "rise-clr");
  checkRetrigger(width);
  checkResetTruncates();
  checkBlocked();
  drive(true, true, false, false, false);
}

void setup() {
  digitalWrite(PIN_A1, HIGH);
  digitalWrite(PIN_A2, HIGH);
  digitalWrite(PIN_B1, LOW);
  digitalWrite(PIN_B2, LOW);
  digitalWrite(PIN_CLR, LOW);
  pinMode(PIN_A1, OUTPUT);
  pinMode(PIN_A2, OUTPUT);
  pinMode(PIN_B1, OUTPUT);
  pinMode(PIN_B2, OUTPUT);
  pinMode(PIN_CLR, OUTPUT);
  pinMode(PIN_Q, INPUT);
  pinMode(PIN_NQ, INPUT);
  Serial.begin(115200);
  Serial.println("74HC122 bench. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  failed = false;
  resultLine[0] = '\0';
  runCheck();
  if (!failed) Serial.println("RESULT PASS");
  else Serial.println(resultLine);
}
