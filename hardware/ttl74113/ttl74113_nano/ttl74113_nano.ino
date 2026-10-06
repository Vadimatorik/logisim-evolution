/*
 * Self-check for a 74HC113 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Power-up state is not specified, so each half is preset and then loaded with 0
 * before the checks that expect a known value.
 */

struct Half {
  const char* name;
  uint8_t ck;
  uint8_t j;
  uint8_t k;
  uint8_t pr;
  uint8_t q;
  uint8_t nq;
};

const Half HALVES[] = {
    {"1", 2, 4, 3, 5, 12, A0},
    {"2", 7, 9, 8, 10, A2, A1},
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

void driveIdle() {
  for (uint8_t i = 0; i < HALF_COUNT; i++) {
    digitalWrite(HALVES[i].ck, HIGH);
    digitalWrite(HALVES[i].j, LOW);
    digitalWrite(HALVES[i].k, LOW);
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

void checkOther(const Half& other) {
  check(other, "untouched", 0, 1);
}

void falling(const Half& half) {
  digitalWrite(half.ck, HIGH);
  delay(1);
  digitalWrite(half.ck, LOW);
  delay(1);
}

void knownZero(const Half& half) {
  digitalWrite(half.pr, LOW);
  digitalWrite(half.j, LOW);
  digitalWrite(half.k, HIGH);
  digitalWrite(half.ck, HIGH);
  delay(1);
  digitalWrite(half.pr, HIGH);
  delay(1);
  falling(half);
}

void capture(const Half& half, int j, int k) {
  digitalWrite(half.ck, HIGH);
  digitalWrite(half.j, j ? HIGH : LOW);
  digitalWrite(half.k, k ? HIGH : LOW);
  digitalWrite(half.pr, HIGH);
  delay(1);
  falling(half);
}

void checkHalf(const Half& half, const Half& other) {
  knownZero(half);
  knownZero(other);
  check(half, "load-0", 0, 1);
  checkOther(other);

  digitalWrite(half.ck, LOW);
  digitalWrite(half.j, LOW);
  digitalWrite(half.k, HIGH);
  digitalWrite(half.pr, LOW);
  delay(1);
  check(half, "preset", 1, 0);
  checkOther(other);

  digitalWrite(half.ck, HIGH);
  delay(1);
  falling(half);
  check(half, "preset-through-edge", 1, 0);
  checkOther(other);

  digitalWrite(half.pr, HIGH);
  digitalWrite(half.j, LOW);
  digitalWrite(half.k, LOW);
  delay(1);
  check(half, "preset-release", 1, 0);

  digitalWrite(half.j, LOW);
  digitalWrite(half.k, HIGH);
  digitalWrite(half.ck, LOW);
  delay(1);
  check(half, "data-while-low", 1, 0);
  digitalWrite(half.ck, HIGH);
  delay(1);
  check(half, "rising", 1, 0);
  falling(half);
  check(half, "load-0-after-rise", 0, 1);
  checkOther(other);

  capture(half, 0, 0);
  check(half, "hold", 0, 1);
  capture(half, 1, 0);
  check(half, "load-1", 1, 0);
  capture(half, 0, 0);
  check(half, "hold-1", 1, 0);
  capture(half, 0, 1);
  check(half, "load-0-again", 0, 1);
  capture(half, 1, 1);
  check(half, "toggle-1", 1, 0);
  capture(half, 1, 1);
  check(half, "toggle-0", 0, 1);
  checkOther(other);

  digitalWrite(half.j, LOW);
  digitalWrite(half.k, LOW);
  digitalWrite(half.ck, LOW);
  delay(1);
  check(half, "clock-low-hold", 0, 1);
  digitalWrite(half.j, HIGH);
  digitalWrite(half.k, LOW);
  delay(1);
  check(half, "data-while-low-2", 0, 1);
  digitalWrite(half.ck, HIGH);
  delay(1);
  check(half, "rising-2", 0, 1);
  falling(half);
  check(half, "falling-after-rise", 1, 0);
  checkOther(other);

  digitalWrite(half.j, LOW);
  digitalWrite(half.k, LOW);
  digitalWrite(half.ck, HIGH);
  digitalWrite(half.pr, HIGH);
}

void setup() {
  Serial.begin(115200);
  for (uint8_t i = 0; i < HALF_COUNT; i++) {
    pinMode(HALVES[i].ck, OUTPUT);
    pinMode(HALVES[i].j, OUTPUT);
    pinMode(HALVES[i].k, OUTPUT);
    pinMode(HALVES[i].pr, OUTPUT);
    pinMode(HALVES[i].q, INPUT);
    pinMode(HALVES[i].nq, INPUT);
  }
  driveIdle();
  Serial.println("74HC113 ready. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available()) Serial.read();
  failed = false;
  resultLine[0] = '\0';
  driveIdle();
  checkHalf(HALVES[0], HALVES[1]);
  checkHalf(HALVES[1], HALVES[0]);
  if (!failed) Serial.println("RESULT PASS");
  else Serial.println(resultLine);
}
