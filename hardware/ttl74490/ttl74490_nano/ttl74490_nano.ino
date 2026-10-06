/*
 * Self-check for a 74HC490 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each half counts BCD 0..9 on the falling edge of its clock. CLR high clears
 * that half at once. SET9 high forces code 9 and overrides CLR. Both CLR pins
 * stay high, both clocks stay low and both SET9 pins stay low until the check
 * starts, so the outputs are already zero.
 */

const uint8_t PIN_1CLK = 2;
const uint8_t PIN_1CLR = 3;
const uint8_t PIN_1SET9 = 4;
const uint8_t PIN_2CLK = 5;
const uint8_t PIN_2CLR = 6;
const uint8_t PIN_2SET9 = 7;
const uint8_t PIN_1QA = 8;
const uint8_t PIN_1QB = 9;
const uint8_t PIN_1QC = 10;
const uint8_t PIN_1QD = 11;
const uint8_t PIN_2QA = 12;
const uint8_t PIN_2QB = A0;
const uint8_t PIN_2QC = A1;
const uint8_t PIN_2QD = A2;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, uint8_t expected, uint8_t actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s expected %02X got %02X",
      step,
      expected,
      actual);
}

void settle() { delay(1); }

uint8_t readNibble(uint8_t qa, uint8_t qb, uint8_t qc, uint8_t qd) {
  uint8_t value = 0;
  if (digitalRead(qa)) value |= 1;
  if (digitalRead(qb)) value |= 2;
  if (digitalRead(qc)) value |= 4;
  if (digitalRead(qd)) value |= 8;
  return value;
}

uint8_t readHalf(uint8_t half) {
  if (half == 1) return readNibble(PIN_1QA, PIN_1QB, PIN_1QC, PIN_1QD);
  return readNibble(PIN_2QA, PIN_2QB, PIN_2QC, PIN_2QD);
}

void expectHalf(uint8_t half, uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readHalf(half);
  if (actual != expected) noteFailure(step, expected, actual);
}

void clockHigh(uint8_t pin) {
  digitalWrite(pin, HIGH);
  settle();
}

void clockLow(uint8_t pin) {
  digitalWrite(pin, LOW);
  settle();
}

void releaseControls() {
  digitalWrite(PIN_1CLR, LOW);
  digitalWrite(PIN_2CLR, LOW);
  digitalWrite(PIN_1SET9, LOW);
  digitalWrite(PIN_2SET9, LOW);
  digitalWrite(PIN_1CLK, LOW);
  digitalWrite(PIN_2CLK, LOW);
  settle();
}

void checkDecade(uint8_t half, uint8_t clockPin, const char* step) {
  for (uint8_t code = 0; code < 10; code++) {
    expectHalf(half, code, step);
    clockHigh(clockPin);
    expectHalf(half, code, "rising edge");
    clockLow(clockPin);
  }
  expectHalf(half, 0, "wrap");
}

void checkAsyncClearAndSet() {
  releaseControls();
  for (uint8_t i = 0; i < 4; i++) {
    clockHigh(PIN_1CLK);
    clockLow(PIN_1CLK);
  }
  expectHalf(1, 4, "count before clear");
  clockHigh(PIN_1CLK);
  digitalWrite(PIN_1CLR, HIGH);
  expectHalf(1, 0, "async clear");
  clockLow(PIN_1CLK);
  expectHalf(1, 0, "clock while clear");
  digitalWrite(PIN_1CLR, LOW);
  expectHalf(1, 0, "clear release");

  digitalWrite(PIN_2SET9, HIGH);
  expectHalf(2, 9, "async set to 9");
  digitalWrite(PIN_2SET9, LOW);
  expectHalf(2, 9, "set release");
  clockHigh(PIN_2CLK);
  expectHalf(2, 9, "rising after set");
  clockLow(PIN_2CLK);
  expectHalf(2, 0, "fall after set");
}

void checkSetOverridesClear() {
  releaseControls();
  digitalWrite(PIN_1CLR, HIGH);
  digitalWrite(PIN_1SET9, HIGH);
  digitalWrite(PIN_2CLR, HIGH);
  digitalWrite(PIN_2SET9, HIGH);
  expectHalf(1, 9, "set overrides clear");
  expectHalf(2, 9, "set overrides clear 2");
  digitalWrite(PIN_1SET9, LOW);
  expectHalf(1, 0, "clear after set release");
  expectHalf(2, 9, "other half stays at 9");
  digitalWrite(PIN_1CLR, LOW);
  digitalWrite(PIN_2CLR, LOW);
  digitalWrite(PIN_2SET9, LOW);
}

void checkHalvesAreIndependent() {
  releaseControls();
  digitalWrite(PIN_2SET9, HIGH);
  expectHalf(2, 9, "hold half 2 at 9");
  digitalWrite(PIN_2SET9, LOW);
  clockHigh(PIN_1CLK);
  clockLow(PIN_1CLK);
  clockHigh(PIN_1CLK);
  clockLow(PIN_1CLK);
  expectHalf(1, 2, "half 1 counted");
  expectHalf(2, 9, "half 2 held");
  digitalWrite(PIN_1CLR, HIGH);
  expectHalf(1, 0, "clear half 1");
  expectHalf(2, 9, "half 2 still held");
  digitalWrite(PIN_1CLR, LOW);
}

void setup() {
  pinMode(PIN_1CLK, OUTPUT);
  pinMode(PIN_1CLR, OUTPUT);
  pinMode(PIN_1SET9, OUTPUT);
  pinMode(PIN_2CLK, OUTPUT);
  pinMode(PIN_2CLR, OUTPUT);
  pinMode(PIN_2SET9, OUTPUT);
  pinMode(PIN_1QA, INPUT);
  pinMode(PIN_1QB, INPUT);
  pinMode(PIN_1QC, INPUT);
  pinMode(PIN_1QD, INPUT);
  pinMode(PIN_2QA, INPUT);
  pinMode(PIN_2QB, INPUT);
  pinMode(PIN_2QC, INPUT);
  pinMode(PIN_2QD, INPUT);

  digitalWrite(PIN_1CLK, LOW);
  digitalWrite(PIN_2CLK, LOW);
  digitalWrite(PIN_1SET9, LOW);
  digitalWrite(PIN_2SET9, LOW);
  digitalWrite(PIN_1CLR, HIGH);
  digitalWrite(PIN_2CLR, HIGH);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  expectHalf(1, 0, "initial clear");
  expectHalf(2, 0, "initial clear 2");
  releaseControls();
  expectHalf(1, 0, "clear released");
  expectHalf(2, 0, "clear released 2");

  checkDecade(1, PIN_1CLK, "half 1");
  expectHalf(2, 0, "half 2 idle");
  checkDecade(2, PIN_2CLK, "half 2");
  expectHalf(1, 0, "half 1 idle");

  checkAsyncClearAndSet();
  checkSetOverridesClear();
  checkHalvesAreIndependent();

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
