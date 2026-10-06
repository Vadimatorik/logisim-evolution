/*
 * Self-check for a 74HC114 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Clear and clock are shared. Releasing preset and clear together is printed
 * as INFO. The datasheet calls that transition unpredictable, so it does not
 * decide the result.
 */

const uint8_t PIN_CLR = 2;
const uint8_t PIN_CLK = 7;

struct Half {
  const char* name;
  uint8_t j;
  uint8_t k;
  uint8_t pr;
  uint8_t q;
  uint8_t nq;
};

const Half HALVES[] = {
    {"1", 4, 3, 5, 12, A0},
    {"2", 9, 8, 10, A2, A1},
};
const uint8_t HALF_COUNT = sizeof(HALVES) / sizeof(HALVES[0]);

bool failed = false;
char resultLine[140];

void noteFailure(const char* name, const char* step, int expectQ, int expectNq, int actualQ,
                 int actualNq) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine),
           "RESULT FAIL half=%s step=%s expected=%d/%d actual=%d/%d", name, step, expectQ,
           expectNq, actualQ, actualNq);
}

void hold(const Half& half) {
  digitalWrite(half.j, LOW);
  digitalWrite(half.k, LOW);
}

void driveIdle() {
  digitalWrite(PIN_CLR, HIGH);
  digitalWrite(PIN_CLK, HIGH);
  for (uint8_t i = 0; i < HALF_COUNT; i++) {
    hold(HALVES[i]);
    digitalWrite(HALVES[i].pr, HIGH);
  }
}

int level(uint8_t pin) {
  return digitalRead(pin) == HIGH ? 1 : 0;
}

void check(const Half& half, const char* step, int expectQ, int expectNq) {
  delay(1);
  const int actualQ = level(half.q);
  const int actualNq = level(half.nq);
  const bool pass = actualQ == expectQ && actualNq == expectNq;
  Serial.print("half ");
  Serial.print(half.name);
  Serial.print(" ");
  Serial.print(step);
  Serial.print(" expected=");
  Serial.print(expectQ);
  Serial.print("/");
  Serial.print(expectNq);
  Serial.print(" actual=");
  Serial.print(actualQ);
  Serial.print("/");
  Serial.print(actualNq);
  Serial.println(pass ? " PASS" : " FAIL");
  if (!pass) noteFailure(half.name, step, expectQ, expectNq, actualQ, actualNq);
}

void falling() {
  digitalWrite(PIN_CLK, HIGH);
  delay(1);
  digitalWrite(PIN_CLK, LOW);
  delay(1);
}

void clearBoth() {
  for (uint8_t i = 0; i < HALF_COUNT; i++) {
    digitalWrite(HALVES[i].pr, HIGH);
    hold(HALVES[i]);
  }
  digitalWrite(PIN_CLK, HIGH);
  digitalWrite(PIN_CLR, LOW);
  delay(1);
  digitalWrite(PIN_CLR, HIGH);
  delay(1);
}

const Half& otherHalf(const Half& half) {
  return (&half == &HALVES[0]) ? HALVES[1] : HALVES[0];
}

void capture(const Half& half, int j, int k) {
  const Half& other = otherHalf(half);
  digitalWrite(PIN_CLK, HIGH);
  digitalWrite(half.j, j ? HIGH : LOW);
  digitalWrite(half.k, k ? HIGH : LOW);
  hold(other);
  delay(1);
  falling();
}

void reportRelease(const Half& half) {
  digitalWrite(half.pr, LOW);
  digitalWrite(PIN_CLR, LOW);
  delay(1);
  digitalWrite(half.pr, HIGH);
  digitalWrite(PIN_CLR, HIGH);
  delay(1);
  Serial.print("half ");
  Serial.print(half.name);
  Serial.print(" release-both actual=");
  Serial.print(level(half.q));
  Serial.print("/");
  Serial.print(level(half.nq));
  Serial.println(" INFO");
}

void checkHalf(const Half& half, const Half& other) {
  clearBoth();
  check(half, "reset", 0, 1);
  check(other, "reset", 0, 1);

  digitalWrite(half.pr, LOW);
  digitalWrite(half.j, LOW);
  digitalWrite(half.k, HIGH);
  hold(other);
  delay(1);
  falling();
  check(half, "set", 1, 0);
  check(other, "untouched", 0, 1);

  digitalWrite(half.pr, HIGH);
  digitalWrite(other.pr, LOW);
  delay(1);
  check(half, "set-held", 1, 0);
  check(other, "other-set", 1, 0);
  digitalWrite(other.pr, HIGH);
  delay(1);

  digitalWrite(PIN_CLR, LOW);
  digitalWrite(half.j, HIGH);
  digitalWrite(half.k, LOW);
  digitalWrite(PIN_CLK, HIGH);
  delay(1);
  falling();
  check(half, "clear", 0, 1);
  check(other, "clear-both", 0, 1);
  digitalWrite(PIN_CLR, HIGH);
  hold(half);
  hold(other);

  digitalWrite(half.pr, LOW);
  digitalWrite(PIN_CLR, LOW);
  delay(1);
  check(half, "both-low", 1, 1);
  check(other, "other-cleared", 0, 1);

  digitalWrite(half.pr, HIGH);
  delay(1);
  check(half, "clear-remains", 0, 1);

  digitalWrite(half.pr, LOW);
  digitalWrite(PIN_CLR, LOW);
  delay(1);
  digitalWrite(PIN_CLR, HIGH);
  delay(1);
  check(half, "set-remains", 1, 0);
  check(other, "other-still-clear", 0, 1);

  reportRelease(half);
  check(other, "other-after-release", 0, 1);
  clearBoth();
  check(half, "reset-after-release", 0, 1);
  check(other, "other-reset", 0, 1);

  capture(half, 0, 0);
  check(half, "hold", 0, 1);
  capture(half, 1, 0);
  check(half, "load-1", 1, 0);
  capture(half, 0, 0);
  check(half, "hold-1", 1, 0);
  capture(half, 0, 1);
  check(half, "load-0", 0, 1);
  capture(half, 1, 1);
  check(half, "toggle-1", 1, 0);
  capture(half, 1, 1);
  check(half, "toggle-0", 0, 1);
  check(other, "other-held", 0, 1);

  hold(half);
  digitalWrite(PIN_CLK, LOW);
  delay(1);
  check(half, "clock-low-hold", 0, 1);
  digitalWrite(half.j, HIGH);
  digitalWrite(half.k, LOW);
  delay(1);
  check(half, "data-while-low", 0, 1);
  digitalWrite(PIN_CLK, HIGH);
  delay(1);
  check(half, "rising", 0, 1);
  digitalWrite(PIN_CLK, LOW);
  delay(1);
  check(half, "falling-after-rise", 1, 0);
  check(other, "other-held-rise", 0, 1);

  hold(half);
  hold(other);
  digitalWrite(PIN_CLK, HIGH);
  digitalWrite(half.pr, HIGH);
  digitalWrite(other.pr, HIGH);
  digitalWrite(PIN_CLR, HIGH);
}

void sameEdge() {
  clearBoth();
  digitalWrite(PIN_CLK, HIGH);
  digitalWrite(HALVES[0].j, HIGH);
  digitalWrite(HALVES[0].k, LOW);
  digitalWrite(HALVES[1].j, HIGH);
  digitalWrite(HALVES[1].k, HIGH);
  delay(1);
  falling();
  check(HALVES[0], "same-edge-load-1", 1, 0);
  check(HALVES[1], "same-edge-toggle-1", 1, 0);

  digitalWrite(PIN_CLK, HIGH);
  digitalWrite(HALVES[0].j, LOW);
  digitalWrite(HALVES[0].k, LOW);
  digitalWrite(HALVES[1].j, LOW);
  digitalWrite(HALVES[1].k, HIGH);
  delay(1);
  falling();
  check(HALVES[0], "same-edge-hold", 1, 0);
  check(HALVES[1], "same-edge-load-0", 0, 1);
  driveIdle();
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_CLR, OUTPUT);
  pinMode(PIN_CLK, OUTPUT);
  for (uint8_t i = 0; i < HALF_COUNT; i++) {
    pinMode(HALVES[i].j, OUTPUT);
    pinMode(HALVES[i].k, OUTPUT);
    pinMode(HALVES[i].pr, OUTPUT);
    pinMode(HALVES[i].q, INPUT);
    pinMode(HALVES[i].nq, INPUT);
  }
  driveIdle();
  Serial.println("74HC114 ready. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available()) Serial.read();
  failed = false;
  resultLine[0] = '\0';
  driveIdle();
  checkHalf(HALVES[0], HALVES[1]);
  checkHalf(HALVES[1], HALVES[0]);
  sameEdge();
  if (!failed) Serial.println("RESULT PASS");
  else Serial.println(resultLine);
}
