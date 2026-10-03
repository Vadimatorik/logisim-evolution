/*
 * Self-check for a 74HC4020 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * MR high clears every stage. With MR low, a falling CP advances the count.
 * Q1 is the least significant bit. Q2 and Q3 are not pinned out.
 * Output names follow the TI data sheet.
 */

const uint8_t PIN_CP = 2;
const uint8_t PIN_MR = 3;
const uint8_t PIN_NONE = 0xFF;
// Stage bit -> Nano pin. PIN_NONE marks Q2 and Q3, which stay inside the package.
const uint8_t PIN_Q[14] = {4, PIN_NONE, PIN_NONE, 5, 6, 7, 8, 9, 10, 11, 12, 13, A0, A1};
// All 14 stages except Q2 and Q3, which are not brought out to pins.
const uint16_t VISIBLE_MASK = (uint16_t)(((1u << 14) - 1) ^ 0x0006);
const uint16_t MODULUS = 16384;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, uint16_t actual, uint16_t expected) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s actual=%u expected=%u", step, actual,
           expected);
}

void settle() { delayMicroseconds(50); }

uint16_t readVisible() {
  settle();
  uint16_t actual = 0;
  for (uint8_t bit = 0; bit < 14; bit++) {
    if (PIN_Q[bit] == PIN_NONE) continue;
    if (digitalRead(PIN_Q[bit]) == HIGH) actual |= (uint16_t)1 << bit;
  }
  return actual;
}

void expectCount(const char* step, uint16_t count) {
  const uint16_t expected = count & VISIBLE_MASK;
  const uint16_t actual = readVisible();
  Serial.print(step);
  Serial.print(" count=");
  Serial.println(actual);
  if (actual != expected) noteFailure(step, actual, expected);
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
  uint16_t count = 1;
  while (count < (MODULUS - 1) && !failed) {
    digitalWrite(PIN_CP, HIGH);
    const uint16_t atHigh = readVisible();
    if (atHigh != (count & VISIBLE_MASK)) {
      noteFailure("rise during sweep", atHigh, count & VISIBLE_MASK);
      break;
    }
    digitalWrite(PIN_CP, LOW);
    count++;
    const uint16_t atLow = readVisible();
    if (atLow != (count & VISIBLE_MASK)) {
      noteFailure("fall during sweep", atLow, count & VISIBLE_MASK);
      break;
    }
    if ((count & (count - 1)) == 0 || count == (MODULUS - 1)) {
      Serial.print("sweep ");
      Serial.println(count);
    }
  }
  if (failed) return;

  expectCount("sweep 16383", MODULUS - 1);
  digitalWrite(PIN_CP, HIGH);
  const uint16_t atHigh = readVisible();
  if (atHigh != ((MODULUS - 1) & VISIBLE_MASK)) {
    noteFailure("rise at 16383", atHigh, (MODULUS - 1) & VISIBLE_MASK);
  }
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
  for (uint8_t bit = 0; bit < 14; bit++) {
    if (PIN_Q[bit] == PIN_NONE) continue;
    pinMode(PIN_Q[bit], INPUT);
  }

  clocks(LOW, HIGH);

  Serial.begin(115200);
  Serial.println("READY 74HC4020, send a character");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  failed = false;
  runChecks();
  if (!failed) Serial.println("RESULT PASS");
  else Serial.println(resultLine);
}
