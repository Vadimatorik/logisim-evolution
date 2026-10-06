/*
 * Self-check for a 74HC4024 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * MR high clears every stage. With MR low, a falling CP advances the count.
 * Q1 is the least significant bit. Output names follow the TI data sheet.
 */

const uint8_t PIN_CP = 2;
const uint8_t PIN_MR = 3;
const uint8_t PIN_Q[7] = {4, 5, 6, 7, 8, 9, 13};

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, uint8_t actual, uint8_t expected) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s actual=%u expected=%u", step, actual,
           expected);
}

void settle() { delayMicroseconds(50); }

uint8_t readCount() {
  settle();
  uint8_t actual = 0;
  for (uint8_t bit = 0; bit < 7; bit++) {
    if (digitalRead(PIN_Q[bit]) == HIGH) actual |= (uint8_t)1 << bit;
  }
  return actual;
}

void expectCount(const char* step, uint8_t count) {
  const uint8_t actual = readCount();
  Serial.print(step);
  Serial.print(" count=");
  Serial.println(actual);
  if (actual != count) noteFailure(step, actual, count);
}

void clocks(uint8_t cp, uint8_t mr) {
  digitalWrite(PIN_CP, cp);
  digitalWrite(PIN_MR, mr);
}

void checkResetHolds() {
  clocks(LOW, HIGH);
  expectCount("mr idle", 0);
  clocks(HIGH, HIGH);
  expectCount("mr cp high", 0);
  clocks(LOW, HIGH);
  expectCount("mr cp fall", 0);
}

void checkFallingEdge() {
  clocks(LOW, LOW);
  expectCount("released", 0);
  clocks(HIGH, LOW);
  expectCount("cp rise", 0);
  clocks(LOW, LOW);
  expectCount("cp fall", 1);
}

void checkSweep() {
  uint8_t count = 1;
  while (count < 127 && !failed) {
    digitalWrite(PIN_CP, HIGH);
    const uint8_t atHigh = readCount();
    if (atHigh != count) {
      noteFailure("rise during sweep", atHigh, count);
      break;
    }
    digitalWrite(PIN_CP, LOW);
    count++;
    const uint8_t atLow = readCount();
    if (atLow != count) {
      noteFailure("fall during sweep", atLow, count);
      break;
    }
    if ((count & (count - 1)) == 0 || count == 127) {
      Serial.print("sweep ");
      Serial.println(count);
    }
  }
  if (failed) return;

  expectCount("sweep 127", 127);
  digitalWrite(PIN_CP, HIGH);
  const uint8_t atHigh = readCount();
  if (atHigh != 127) noteFailure("rise at 127", atHigh, 127);
  digitalWrite(PIN_CP, LOW);
  expectCount("wrap to 0", 0);
}

void checkResetClearsAgain() {
  clocks(HIGH, LOW);
  expectCount("cp rise before reset", 0);
  clocks(LOW, LOW);
  expectCount("counted before reset", 1);
  clocks(HIGH, HIGH);
  expectCount("mr clears", 0);
  clocks(LOW, HIGH);
  expectCount("mr still held", 0);
  clocks(LOW, LOW);
  expectCount("mr released again", 0);
}

void runChecks() {
  checkResetHolds();
  checkFallingEdge();
  checkSweep();
  checkResetClearsAgain();
}

void setup() {
  pinMode(PIN_CP, OUTPUT);
  pinMode(PIN_MR, OUTPUT);
  for (uint8_t bit = 0; bit < 7; bit++) {
    pinMode(PIN_Q[bit], INPUT);
  }

  clocks(LOW, HIGH);

  Serial.begin(115200);
  Serial.println("READY 74HC4024, send a character");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  failed = false;
  runChecks();
  if (!failed) Serial.println("RESULT PASS");
  else Serial.println(resultLine);
}
