/*
 * Self-check for a 74HC390 wired to an Arduino Nano as described in ../wiring.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * BCD copies 1Q0 onto 1CP1 in software. Bi-quinary copies 2Q3 onto 2CP0.
 * No jumpers are required on the board.
 */

struct Half {
  const char* name;
  uint8_t cp0;
  uint8_t cp1;
  uint8_t mr;
  uint8_t q0;
  uint8_t q1;
  uint8_t q2;
  uint8_t q3;
};

const Half HALF1 = {"1", 2, 4, 3, 8, 9, 10, 11};
const Half HALF2 = {"2", 5, 7, 6, 12, 13, A0, A1};
const int BCD[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0};
const int BIQUINARY[] = {2, 4, 6, 8, 1, 3, 5, 7, 9, 0};
const uint8_t BCD_LENGTH = sizeof(BCD) / sizeof(BCD[0]);

bool failed = false;
char resultLine[160];

void noteFailure(const char* half, unsigned count, int expected, int actual, const char* note) {
  if (failed) return;
  failed = true;
  if (note == nullptr) {
    snprintf(resultLine, sizeof(resultLine),
             "RESULT FAIL half=%s count=%u expected=%d actual=%d", half, count, expected, actual);
  } else {
    snprintf(resultLine, sizeof(resultLine),
             "RESULT FAIL half=%s count=%u expected=%d actual=%d note=%s", half, count, expected,
             actual, note);
  }
}

int readBit(uint8_t pin) { return digitalRead(pin) == HIGH ? 1 : 0; }

int readValue(const Half& half) {
  return readBit(half.q0) | (readBit(half.q1) << 1) | (readBit(half.q2) << 2) |
         (readBit(half.q3) << 3);
}

void expectValue(const Half& half, unsigned count, int expected, const char* note) {
  const int actual = readValue(half);
  if (actual != expected) {
    noteFailure(half.name, count, expected, actual, note);
  }
}

void pulse(uint8_t clockPin) {
  digitalWrite(clockPin, HIGH);
  digitalWrite(clockPin, LOW);
}

void resetHalf(const Half& half) {
  digitalWrite(half.cp0, LOW);
  digitalWrite(half.cp1, LOW);
  digitalWrite(half.mr, HIGH);
  delayMicroseconds(10);
  digitalWrite(half.mr, LOW);
  delayMicroseconds(10);
}

void resetBoth() {
  resetHalf(HALF1);
  resetHalf(HALF2);
}

void bcdStep(const Half& half) {
  digitalWrite(half.cp1, digitalRead(half.q0));
  digitalWrite(half.cp0, HIGH);
  digitalWrite(half.cp1, digitalRead(half.q0));
  digitalWrite(half.cp0, LOW);
  digitalWrite(half.cp1, digitalRead(half.q0));
}

void biQuinaryStep(const Half& half) {
  digitalWrite(half.cp0, digitalRead(half.q3));
  digitalWrite(half.cp1, HIGH);
  digitalWrite(half.cp0, digitalRead(half.q3));
  digitalWrite(half.cp1, LOW);
  digitalWrite(half.cp0, digitalRead(half.q3));
}

void checkIndependentSections() {
  resetBoth();
  expectValue(HALF1, 0, 0, "reset");
  expectValue(HALF2, 0, 0, "reset");

  digitalWrite(HALF1.cp0, HIGH);
  expectValue(HALF1, 0, 0, "rising edge");
  digitalWrite(HALF1.cp0, LOW);
  expectValue(HALF1, 1, 1, "falling edge");
  pulse(HALF1.cp0);
  expectValue(HALF1, 2, 0, "divide-by-2");
  pulse(HALF1.cp0);
  expectValue(HALF1, 3, 1, "divide-by-2");
  expectValue(HALF2, 0, 0, "other half moved");

  resetHalf(HALF1);
  const int divideByFive[] = {2, 4, 6, 8, 0};
  for (uint8_t i = 0; i < 5; i++) {
    pulse(HALF1.cp1);
    expectValue(HALF1, i + 1, divideByFive[i], "divide-by-5");
  }
  expectValue(HALF2, 0, 0, "other half moved");
}

void checkResetOverridesClock() {
  resetBoth();
  pulse(HALF1.cp0);
  pulse(HALF2.cp0);
  expectValue(HALF1, 1, 1, nullptr);
  expectValue(HALF2, 1, 1, nullptr);

  digitalWrite(HALF1.cp0, HIGH);
  digitalWrite(HALF1.mr, HIGH);
  digitalWrite(HALF1.cp0, LOW);
  expectValue(HALF1, 0, 0, "reset did not override the clock");
  expectValue(HALF2, 1, 1, "reset cleared the other half");

  pulse(HALF1.cp0);
  expectValue(HALF1, 0, 0, "counted while reset was held");
  digitalWrite(HALF1.mr, LOW);
  expectValue(HALF1, 0, 0, nullptr);
  pulse(HALF1.cp0);
  expectValue(HALF1, 1, 1, "did not count after reset");
  expectValue(HALF2, 1, 1, nullptr);
}

void checkBcd() {
  resetBoth();
  pulse(HALF2.cp0);
  for (uint8_t i = 0; i < BCD_LENGTH; i++) {
    bcdStep(HALF1);
    const int actual = readValue(HALF1);
    Serial.print("BCD1 ");
    Serial.println(actual);
    expectValue(HALF1, i + 1, BCD[i], "bcd");
  }
  expectValue(HALF2, 1, 1, "bcd disturbed the other half");
}

void checkBiQuinary() {
  resetBoth();
  pulse(HALF1.cp0);
  for (uint8_t i = 0; i < BCD_LENGTH; i++) {
    biQuinaryStep(HALF2);
    const int actual = readValue(HALF2);
    Serial.print("BIQ2 ");
    Serial.println(actual);
    expectValue(HALF2, i + 1, BIQUINARY[i], "biquinary");
  }
  expectValue(HALF1, 1, 1, "biquinary disturbed the other half");
}

void runChecks() {
  failed = false;
  resultLine[0] = '\0';
  checkIndependentSections();
  checkResetOverridesClock();
  checkBcd();
  checkBiQuinary();
  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void setupOutput(uint8_t pin) {
  pinMode(pin, OUTPUT);
  digitalWrite(pin, LOW);
}

void setup() {
  setupOutput(HALF1.cp0);
  setupOutput(HALF1.cp1);
  setupOutput(HALF1.mr);
  setupOutput(HALF2.cp0);
  setupOutput(HALF2.cp1);
  setupOutput(HALF2.mr);
  digitalWrite(HALF1.mr, HIGH);
  digitalWrite(HALF2.mr, HIGH);
  pinMode(HALF1.q0, INPUT);
  pinMode(HALF1.q1, INPUT);
  pinMode(HALF1.q2, INPUT);
  pinMode(HALF1.q3, INPUT);
  pinMode(HALF2.q0, INPUT);
  pinMode(HALF2.q1, INPUT);
  pinMode(HALF2.q2, INPUT);
  pinMode(HALF2.q3, INPUT);

  Serial.begin(115200);
  Serial.println("74HC390 bench. Send any character to start.");
  while (Serial.read() < 0) {
    delay(10);
  }
  while (Serial.read() >= 0) {
  }
  runChecks();
}

void loop() {
  delay(2000);
  if (resultLine[0] != '\0') {
    Serial.println(resultLine);
  }
}
