/*
 * Self-check for a 74HC423 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each half is a retriggerable monostable. Q is high for about 450 us with
 * 10 kOhm and 100 nF; nQ is the complement. A low RD clears the pulse.
 * Rising B triggers when A is low, and falling A triggers when B is high.
 * Rising RD does not trigger. Another B edge during the pulse, at least
 * 200 us after the first, starts the width again. Both RD pins stay low
 * until the check starts.
 */

const unsigned long WIDTH_MIN_US = 250;
const unsigned long WIDTH_MAX_US = 800;
const unsigned long EDGE_TIMEOUT_US = 2000;
const unsigned long PULSE_TIMEOUT_US = 4000;
const unsigned long RETRIGGER_GAP_US = 200;
const unsigned long QUIET_WATCH_US = 2000;

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

enum Edge { EDGE_B_RISE, EDGE_A_FALL };

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

bool stayedIdle(const Half& half, unsigned long limitUs) {
  const unsigned long started = micros();
  while ((unsigned long)(micros() - started) < limitUs) {
    if (digitalRead(half.pinQ) == HIGH || digitalRead(half.pinNq) == LOW) return false;
  }
  return true;
}

void applyEdge(const Half& half, Edge edge) {
  if (edge == EDGE_B_RISE) digitalWrite(half.pinB, HIGH);
  else digitalWrite(half.pinA, LOW);
}

void arm(const Half& half, Edge edge, const char* step) {
  digitalWrite(half.pinRd, LOW);
  if (edge == EDGE_B_RISE) {
    digitalWrite(half.pinA, LOW);
    digitalWrite(half.pinB, LOW);
  } else {
    digitalWrite(half.pinA, HIGH);
    digitalWrite(half.pinB, HIGH);
  }
  settle();
  digitalWrite(half.pinRd, HIGH);
  settle();
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

void checkResetDoesNotTrigger(const Half& half) {
  char step[24];
  snprintf(step, sizeof(step), "%s rise-rd", half.name);
  digitalWrite(half.pinRd, LOW);
  digitalWrite(half.pinA, LOW);
  digitalWrite(half.pinB, HIGH);
  settle();
  expectLevels(half, false, step);
  digitalWrite(half.pinRd, HIGH);
  if (!stayedIdle(half, QUIET_WATCH_US)) noteFailure(step, "Q rose");
  expectLevels(half, false, step);
}

void checkRetrigger(const Half& half, unsigned long width) {
  if (width == 0) return;
  char step[24];
  snprintf(step, sizeof(step), "%s retrigger", half.name);
  arm(half, EDGE_B_RISE, step);
  const unsigned long started = micros();
  applyEdge(half, EDGE_B_RISE);
  const unsigned long rose = waitUntil(started, EDGE_TIMEOUT_US, half, true);
  if (rose == 0) {
    noteFailure(step, "Q stayed low");
    return;
  }
  while ((unsigned long)(micros() - rose) < RETRIGGER_GAP_US) {
    if (digitalRead(half.pinQ) == LOW) {
      noteFailure(step, "ended early");
      return;
    }
  }
  digitalWrite(half.pinB, LOW);
  delayMicroseconds(20);
  digitalWrite(half.pinB, HIGH);
  const unsigned long retriggered = micros();
  const unsigned long fell = waitUntil(retriggered, PULSE_TIMEOUT_US, half, false);
  if (fell == 0) {
    noteFailure(step, "Q stayed high");
    return;
  }
  const unsigned long total = fell - rose;
  const unsigned long again = fell - retriggered;
  Serial.print(step);
  Serial.print(' ');
  Serial.print(total);
  Serial.print(" us total, ");
  Serial.print(again);
  Serial.println(" us from retrigger");
  if (total < width + 100) {
    noteFailure(step, "not extended");
    return;
  }
  if (again < WIDTH_MIN_US || again > WIDTH_MAX_US) {
    char detail[24];
    snprintf(detail, sizeof(detail), "%lu us", again);
    noteFailure(step, detail);
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
  if (!stayedIdle(half, 800)) noteFailure(step, "Q rose");
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
  if (!stayedIdle(half, QUIET_WATCH_US)) noteFailure(step, "Q rose");

  snprintf(step, sizeof(step), "%s block-b", half.name);
  digitalWrite(half.pinRd, LOW);
  digitalWrite(half.pinA, HIGH);
  digitalWrite(half.pinB, LOW);
  settle();
  digitalWrite(half.pinRd, HIGH);
  settle();
  digitalWrite(half.pinA, LOW);
  if (!stayedIdle(half, QUIET_WATCH_US)) noteFailure(step, "Q rose");

  snprintf(step, sizeof(step), "%s block-rd", half.name);
  digitalWrite(half.pinRd, LOW);
  digitalWrite(half.pinA, LOW);
  digitalWrite(half.pinB, LOW);
  settle();
  digitalWrite(half.pinB, HIGH);
  if (!stayedIdle(half, QUIET_WATCH_US)) noteFailure(step, "Q rose");
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
  checkResetDoesNotTrigger(half);
  checkRetrigger(half, width);
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
  Serial.println("74HC423 bench. Send any character to start.");
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
