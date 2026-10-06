/*
 * Self-check for a 74HC4538 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * nA triggers on a falling edge only while nB is low and nCD is high.
 * nB triggers on a rising edge only while nA is high and nCD is high.
 * nCD low clears the pulse at once. Releasing nCD does not start one.
 * Each half uses 10 kohm and 1 uF, so the datasheet width is about 7 ms.
 * Outputs are push-pull. Both clears stay low until the check starts.
 */

const uint8_t PIN_1CD = 2;
const uint8_t PIN_1B = 3;
const uint8_t PIN_1A = 4;
const uint8_t PIN_1Q = 5;
const uint8_t PIN_1QBAR = 6;
const uint8_t PIN_2QBAR = 7;
const uint8_t PIN_2Q = 8;
const uint8_t PIN_2A = 9;
const uint8_t PIN_2B = 10;
const uint8_t PIN_2CD = 11;

const unsigned long MIN_WIDTH_US = 3500;
const unsigned long MAX_WIDTH_US = 10500;
const unsigned long RETRIGGER_AT_US = 2000;

struct Half {
  uint8_t cd;
  uint8_t b;
  uint8_t a;
  uint8_t q;
  uint8_t qbar;
  const char* name;
};

const Half HALF1 = {PIN_1CD, PIN_1B, PIN_1A, PIN_1Q, PIN_1QBAR, "1"};
const Half HALF2 = {PIN_2CD, PIN_2B, PIN_2A, PIN_2Q, PIN_2QBAR, "2"};

bool failed = false;
char resultLine[96];
unsigned long half1Width = 0;

void noteFailure(const char* step, unsigned long expected, unsigned long actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s expected %lu got %lu",
      step,
      expected,
      actual);
}

void settle() { delayMicroseconds(50); }

bool isHigh(uint8_t pin) { return digitalRead(pin) == HIGH; }

void expectLevel(uint8_t pin, bool high, const char* step) {
  settle();
  const unsigned long actual = isHigh(pin) ? 1 : 0;
  const unsigned long expected = high ? 1 : 0;
  if (actual != expected) noteFailure(step, expected, actual);
}

void expectIdle(const Half& half, const char* step) {
  expectLevel(half.q, false, step);
  expectLevel(half.qbar, true, step);
}

void holdReset(const Half& half) {
  digitalWrite(half.cd, LOW);
  digitalWrite(half.a, HIGH);
  digitalWrite(half.b, LOW);
}

void arm(const Half& half, bool aHigh, bool bHigh) {
  digitalWrite(half.cd, HIGH);
  digitalWrite(half.a, aHigh ? HIGH : LOW);
  digitalWrite(half.b, bHigh ? HIGH : LOW);
  settle();
}

bool sawHigh(uint8_t pin, unsigned long windowUs) {
  const unsigned long start = micros();
  while (micros() - start < windowUs) {
    if (isHigh(pin)) return true;
  }
  return false;
}

void expectNoPulse(const Half& half, const char* step) {
  if (sawHigh(half.q, 2000)) noteFailure(step, 0, 1);
  expectLevel(half.qbar, true, step);
}

unsigned long readPulse(const Half& half, const char* step) {
  const unsigned long start = micros();
  while (!isHigh(half.q)) {
    if (micros() - start > 2000) {
      noteFailure(step, 1, 0);
      return 0;
    }
  }
  const unsigned long rose = micros();
  expectLevel(half.qbar, false, step);
  while (isHigh(half.q)) {
    if (micros() - rose > 30000) {
      noteFailure(step, MAX_WIDTH_US, 30001);
      return 30001;
    }
  }
  return micros() - rose;
}

void expectWidth(unsigned long width, const char* step) {
  if (width < MIN_WIDTH_US || width > MAX_WIDTH_US) {
    noteFailure(step, (MIN_WIDTH_US + MAX_WIDTH_US) / 2, width);
  }
}

unsigned long triggerFallingA(const Half& half, const char* step) {
  arm(half, true, false);
  digitalWrite(half.a, LOW);
  return readPulse(half, step);
}

unsigned long triggerRisingB(const Half& half, const char* step) {
  arm(half, true, false);
  digitalWrite(half.b, HIGH);
  return readPulse(half, step);
}

void checkResetHoldsAndReleaseIsQuiet(const Half& half) {
  holdReset(half);
  settle();
  char step[32];
  snprintf(step, sizeof(step), "%s reset", half.name);
  expectIdle(half, step);
  digitalWrite(half.cd, HIGH);
  snprintf(step, sizeof(step), "%s release", half.name);
  expectNoPulse(half, step);
}

void checkBlockedEdges(const Half& half) {
  holdReset(half);
  digitalWrite(half.a, LOW);
  digitalWrite(half.b, HIGH);
  settle();
  digitalWrite(half.cd, HIGH);
  settle();
  digitalWrite(half.a, HIGH);
  settle();
  digitalWrite(half.a, LOW);
  char step[40];
  snprintf(step, sizeof(step), "%s A while B high", half.name);
  expectNoPulse(half, step);

  digitalWrite(half.b, LOW);
  settle();
  digitalWrite(half.b, HIGH);
  snprintf(step, sizeof(step), "%s B while A low", half.name);
  expectNoPulse(half, step);
}

void checkFallingA(const Half& half, bool remember) {
  char step[32];
  snprintf(step, sizeof(step), "%s falling A", half.name);
  const unsigned long width = triggerFallingA(half, step);
  Serial.print(step);
  Serial.print(" ");
  Serial.println(width);
  expectWidth(width, step);
  if (remember) half1Width = width;
}

void checkRisingB(const Half& half) {
  char step[32];
  snprintf(step, sizeof(step), "%s rising B", half.name);
  const unsigned long width = triggerRisingB(half, step);
  Serial.print(step);
  Serial.print(" ");
  Serial.println(width);
  expectWidth(width, step);
}

void checkRetrigger() {
  if (half1Width < MIN_WIDTH_US) return;
  arm(HALF1, true, false);
  const unsigned long start = micros();
  digitalWrite(PIN_1A, LOW);
  delayMicroseconds(RETRIGGER_AT_US);
  if (!isHigh(PIN_1Q)) noteFailure("1 retrigger still high", 1, 0);
  digitalWrite(PIN_1A, HIGH);
  delayMicroseconds(50);
  digitalWrite(PIN_1A, LOW);
  while (isHigh(PIN_1Q)) {
    if (micros() - start > 40000) break;
  }
  const unsigned long total = micros() - start;
  Serial.print("1 retrigger ");
  Serial.println(total);
  if (total < half1Width + 500 || total > half1Width * 2 + 4000) {
    noteFailure("1 retrigger", half1Width + RETRIGGER_AT_US, total);
  }
}

void checkResetAborts(const Half& half) {
  arm(half, true, false);
  digitalWrite(half.a, LOW);
  delayMicroseconds(500);
  char step[40];
  snprintf(step, sizeof(step), "%s abort armed", half.name);
  expectLevel(half.q, true, step);
  digitalWrite(half.cd, LOW);
  delayMicroseconds(100);
  snprintf(step, sizeof(step), "%s abort", half.name);
  expectIdle(half, step);
  delay(2);
  snprintf(step, sizeof(step), "%s abort stays", half.name);
  expectIdle(half, step);
  digitalWrite(half.cd, HIGH);
  snprintf(step, sizeof(step), "%s abort release", half.name);
  expectNoPulse(half, step);
}

void checkHalvesAreIndependent() {
  arm(HALF1, true, false);
  expectIdle(HALF1, "1 idle before 2");
  arm(HALF2, true, false);
  const unsigned long start = micros();
  digitalWrite(PIN_2A, LOW);
  delayMicroseconds(200);
  expectLevel(PIN_2Q, true, "2 during");
  expectLevel(PIN_2QBAR, false, "2 during qbar");
  expectIdle(HALF1, "1 idle during 2");
  while (isHigh(PIN_2Q)) {
    if (micros() - start > 30000) break;
  }
  const unsigned long width = micros() - start;
  Serial.print("2 falling A ");
  Serial.println(width);
  expectWidth(width, "2 falling A");
}

void setup() {
  const uint8_t outputs[] = {
      PIN_1CD, PIN_1B, PIN_1A, PIN_2A, PIN_2B, PIN_2CD};
  for (uint8_t index = 0; index < sizeof(outputs); index++) {
    pinMode(outputs[index], OUTPUT);
  }
  pinMode(PIN_1Q, INPUT);
  pinMode(PIN_1QBAR, INPUT);
  pinMode(PIN_2Q, INPUT);
  pinMode(PIN_2QBAR, INPUT);
  holdReset(HALF1);
  holdReset(HALF2);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  checkResetHoldsAndReleaseIsQuiet(HALF1);
  checkResetHoldsAndReleaseIsQuiet(HALF2);
  checkBlockedEdges(HALF1);
  checkFallingA(HALF1, true);
  checkRetrigger();
  checkResetAborts(HALF1);
  checkRisingB(HALF1);
  checkBlockedEdges(HALF2);
  checkHalvesAreIndependent();
  checkResetAborts(HALF2);
  checkRisingB(HALF2);

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
