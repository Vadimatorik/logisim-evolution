/*
 * Self-check for a 74HC221 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each half is a non-retriggerable monostable. Q is high for about 700 us
 * with 10 kOhm and 100 nF; nQ is the complement. A low RD clears the pulse.
 * Rising B triggers when A is low, falling A triggers when B is high, and
 * rising RD triggers when A is low and B is high. Another A/B edge during
 * the pulse does not extend it. Both RD pins stay low until the check starts.
 */

const unsigned long WIDTH_MIN_US = 400;
const unsigned long WIDTH_MAX_US = 1200;
const unsigned long EDGE_TIMEOUT_US = 2000;
const unsigned long PULSE_TIMEOUT_US = 4000;

struct Half {
  const char* name;
  uint8_t pinA;
  uint8_t pinB;
  uint8_t pinRd;
  uint8_t pinQ;
  uint8_t pinNq;
};

const Half HALF1 = {"1", 2, 3, 4, 11, 5};
const Half HALF2 = {"2", 7, 8, 9, 6, 10};

enum Edge { EDGE_B_RISE, EDGE_A_FALL, EDGE_RD_RISE };

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, const char* detail) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s %s", step, detail);
}

void settle() { delayMicroseconds(20); }

void configureHalf(const Half& half) {
  digitalWrite(half.pinA, HIGH);
  digitalWrite(half.pinB, LOW);
  digitalWrite(half.pinRd, LOW);
  pinMode(half.pinA, OUTPUT);
  pinMode(half.pinB, OUTPUT);
  pinMode(half.pinRd, OUTPUT);
  pinMode(half.pinQ, INPUT);
  pinMode(half.pinNq, INPUT);
}

void holdReset(const Half& half) {
  digitalWrite(half.pinA, HIGH);
  digitalWrite(half.pinB, LOW);
  digitalWrite(half.pinRd, LOW);
}

void expectLevels(const Half& half, bool qHigh, const char* step) {
  settle();
  const bool q = digitalRead(half.pinQ) == HIGH;
  const bool nq = digitalRead(half.pinNq) == HIGH;
  if (q != qHigh || nq == qHigh) {
    char detail[24];
    snprintf(detail, sizeof(detail), "Q=%d nQ=%d", q ? 1 : 0, nq ? 1 : 0);
    noteFailure(step, detail);
  }
}

void applyEdge(const Half& half, Edge edge) {
  if (edge == EDGE_B_RISE) digitalWrite(half.pinB, HIGH);
  else if (edge == EDGE_A_FALL) digitalWrite(half.pinA, LOW);
  else digitalWrite(half.pinRd, HIGH);
}

void arm(const Half& half, Edge edge, const char* step) {
  digitalWrite(half.pinRd, LOW);
  if (edge == EDGE_B_RISE) {
    digitalWrite(half.pinA, LOW);
    digitalWrite(half.pinB, LOW);
  } else if (edge == EDGE_A_FALL) {
    digitalWrite(half.pinA, HIGH);
    digitalWrite(half.pinB, HIGH);
  } else {
    digitalWrite(half.pinA, LOW);
    digitalWrite(half.pinB, HIGH);
  }
  settle();
  if (edge != EDGE_RD_RISE) {
    digitalWrite(half.pinRd, HIGH);
    settle();
  }
  expectLevels(half, false, step);
}

unsigned long waitUntil(unsigned long start, unsigned long limit, const Half& half, bool qHigh) {
  while ((unsigned long)(micros() - start) < limit) {
    if ((digitalRead(half.pinQ) == HIGH) == qHigh) return micros();
  }
  return 0;
}

unsigned long measurePulse(const Half& half, const Half& other, Edge edge, const char* step) {
  arm(half, edge, step);
  const unsigned long started = micros();
  applyEdge(half, edge);
  const unsigned long rose = waitUntil(started, EDGE_TIMEOUT_US, half, true);
  if (rose == 0) {
    noteFailure(step, "Q stayed low");
    return 0;
  }
  expectLevels(half, true, step);
  char otherDuring[32];
  snprintf(otherDuring, sizeof(otherDuring), "%s during %s", other.name, step);
  expectLevels(other, false, otherDuring);
  const unsigned long fell = waitUntil(rose, PULSE_TIMEOUT_US, half, false);
  if (fell == 0) {
    noteFailure(step, "Q stayed high");
    return 0;
  }
  expectLevels(half, false, step);
  return fell - rose;
}

unsigned long checkWidth(const Half& half, const Half& other, Edge edge, const char* step) {
  char named[24];
  snprintf(named, sizeof(named), "%s %s", half.name, step);
  const unsigned long width = measurePulse(half, other, edge, named);
  if (width == 0) return 0;
  Serial.print(named);
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

void checkNoRetrigger(const Half& half, unsigned long width) {
  if (width == 0) return;
  char step[20];
  snprintf(step, sizeof(step), "%s retrigger", half.name);
  arm(half, EDGE_B_RISE, step);
  const unsigned long started = micros();
  applyEdge(half, EDGE_B_RISE);
  if (waitUntil(started, EDGE_TIMEOUT_US, half, true) == 0) {
    noteFailure(step, "Q stayed low");
    return;
  }
  const unsigned long halfway = width / 2;
  while ((unsigned long)(micros() - started) < halfway) {
  }
  if (digitalRead(half.pinQ) == LOW) {
    noteFailure(step, "ended early");
    return;
  }
  digitalWrite(half.pinB, LOW);
  delayMicroseconds(8);
  digitalWrite(half.pinB, HIGH);
  const unsigned long late = (width * 6) / 5;
  while ((unsigned long)(micros() - started) < late) {
  }
  expectLevels(half, false, step);
}

void checkResetTruncates(const Half& half) {
  char step[20];
  snprintf(step, sizeof(step), "%s reset-cut", half.name);
  arm(half, EDGE_B_RISE, step);
  const unsigned long started = micros();
  applyEdge(half, EDGE_B_RISE);
  if (waitUntil(started, EDGE_TIMEOUT_US, half, true) == 0) {
    noteFailure(step, "Q stayed low");
    return;
  }
  delayMicroseconds(150);
  digitalWrite(half.pinRd, LOW);
  const unsigned long cut = micros();
  if (waitUntil(cut, 200, half, false) == 0) {
    noteFailure(step, "Q stayed high");
    return;
  }
  delayMicroseconds(800);
  expectLevels(half, false, step);
}

void checkBlocked(const Half& half) {
  char step[20];
  snprintf(step, sizeof(step), "%s block-a", half.name);
  digitalWrite(half.pinRd, LOW);
  digitalWrite(half.pinA, HIGH);
  digitalWrite(half.pinB, LOW);
  settle();
  digitalWrite(half.pinRd, HIGH);
  settle();
  digitalWrite(half.pinB, HIGH);
  delayMicroseconds(1500);
  expectLevels(half, false, step);

  snprintf(step, sizeof(step), "%s block-b", half.name);
  digitalWrite(half.pinRd, LOW);
  digitalWrite(half.pinA, HIGH);
  digitalWrite(half.pinB, LOW);
  settle();
  digitalWrite(half.pinRd, HIGH);
  settle();
  digitalWrite(half.pinA, LOW);
  delayMicroseconds(1500);
  expectLevels(half, false, step);

  snprintf(step, sizeof(step), "%s block-rd", half.name);
  digitalWrite(half.pinRd, LOW);
  digitalWrite(half.pinA, LOW);
  digitalWrite(half.pinB, LOW);
  settle();
  digitalWrite(half.pinB, HIGH);
  delayMicroseconds(1500);
  expectLevels(half, false, step);
}

void testHalf(const Half& half, const Half& other) {
  char resetStep[24];
  char otherStep[24];
  snprintf(resetStep, sizeof(resetStep), "%s reset", half.name);
  snprintf(otherStep, sizeof(otherStep), "%s held-reset", other.name);
  holdReset(other);
  holdReset(half);
  settle();
  expectLevels(half, false, resetStep);
  expectLevels(other, false, otherStep);

  const unsigned long width = checkWidth(half, other, EDGE_B_RISE, "rise-b");
  checkWidth(half, other, EDGE_A_FALL, "fall-a");
  checkWidth(half, other, EDGE_RD_RISE, "rise-rd");
  checkNoRetrigger(half, width);
  checkResetTruncates(half);
  checkBlocked(half);
  expectLevels(other, false, otherStep);
  holdReset(half);
}

void runCheck() {
  testHalf(HALF1, HALF2);
  testHalf(HALF2, HALF1);
  holdReset(HALF1);
  holdReset(HALF2);
}

void setup() {
  configureHalf(HALF1);
  configureHalf(HALF2);
  Serial.begin(115200);
  Serial.println("74HC221 bench. Send any character to start.");
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
