/*
 * Self-check for a 74HC4022 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * MR high selects output 0. With MR low, a rising CP0 advances while CP1 is low,
 * and a falling CP1 advances while CP0 is high. Q4-7 is high for counts 0 to 3.
 */

const uint8_t PIN_CP1 = 2;
const uint8_t PIN_CP0 = 3;
const uint8_t PIN_MR = 4;
const uint8_t PIN_Q[8] = {5, 6, 7, 8, 9, 10, 11, 12};
const uint8_t PIN_CARRY = 13;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s", step);
}

void settle() { delayMicroseconds(50); }

void expectCount(const char* step, uint8_t count) {
  settle();
  uint8_t actual[8];
  for (uint8_t output = 0; output < 8; output++) {
    actual[output] = digitalRead(PIN_Q[output]);
  }
  const uint8_t carry = digitalRead(PIN_CARRY);

  Serial.print(step);
  Serial.print(" Q=");
  for (uint8_t output = 0; output < 8; output++) {
    Serial.print(actual[output] == HIGH ? '1' : '0');
  }
  Serial.print(" C=");
  Serial.println(carry == HIGH ? '1' : '0');

  for (uint8_t output = 0; output < 8; output++) {
    const uint8_t expected = output == count ? HIGH : LOW;
    if (actual[output] != expected) noteFailure(step);
  }
  if (carry != (count < 4 ? HIGH : LOW)) noteFailure(step);
}

void clocks(uint8_t cp0, uint8_t cp1, uint8_t mr) {
  digitalWrite(PIN_CP0, cp0);
  digitalWrite(PIN_CP1, cp1);
  digitalWrite(PIN_MR, mr);
}

void checkResetHolds() {
  clocks(LOW, LOW, HIGH);
  expectCount("mr low clocks", 0);
  clocks(HIGH, LOW, HIGH);
  expectCount("mr cp0 high", 0);
  clocks(HIGH, HIGH, HIGH);
  expectCount("mr both clocks high", 0);
  clocks(LOW, HIGH, HIGH);
  expectCount("mr cp1 high", 0);
  clocks(LOW, LOW, HIGH);
  expectCount("mr idle", 0);
}

void checkCp0Octave() {
  clocks(LOW, LOW, LOW);
  expectCount("released", 0);

  uint8_t count = 0;
  char step[40];
  for (uint8_t pulse = 0; pulse < 8; pulse++) {
    digitalWrite(PIN_CP0, HIGH);
    count = (count + 1) % 8;
    snprintf(step, sizeof(step), "cp0 rise %u", count);
    expectCount(step, count);
    digitalWrite(PIN_CP0, LOW);
    snprintf(step, sizeof(step), "cp0 fall %u", count);
    expectCount(step, count);
  }
}

void checkCp0IgnoredWhileCp1High() {
  digitalWrite(PIN_CP1, HIGH);
  digitalWrite(PIN_CP0, HIGH);
  expectCount("cp0 rise cp1 high", 0);
  digitalWrite(PIN_CP0, LOW);
  expectCount("cp0 fall cp1 high", 0);
}

void checkCp1Edge() {
  digitalWrite(PIN_CP0, HIGH);
  expectCount("cp0 high cp1 high", 0);
  digitalWrite(PIN_CP1, LOW);
  expectCount("cp1 fall cp0 high", 1);
  digitalWrite(PIN_CP1, HIGH);
  expectCount("cp1 rise", 1);
  digitalWrite(PIN_CP0, LOW);
  expectCount("cp0 fall after cp1", 1);
  digitalWrite(PIN_CP1, LOW);
  expectCount("cp1 fall cp0 low", 1);
}

void checkResetClearsAgain() {
  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_CP0, HIGH);
  expectCount("mr clears", 0);
  digitalWrite(PIN_CP0, LOW);
  digitalWrite(PIN_CP1, LOW);
  expectCount("mr still held", 0);
  digitalWrite(PIN_MR, LOW);
  expectCount("mr released again", 0);
}

void runChecks() {
  checkResetHolds();
  checkCp0Octave();
  checkCp0IgnoredWhileCp1High();
  checkCp1Edge();
  checkResetClearsAgain();
}

void setup() {
  pinMode(PIN_CP1, OUTPUT);
  pinMode(PIN_CP0, OUTPUT);
  pinMode(PIN_MR, OUTPUT);
  for (uint8_t output = 0; output < 8; output++) {
    pinMode(PIN_Q[output], INPUT);
  }
  pinMode(PIN_CARRY, INPUT);

  clocks(LOW, LOW, HIGH);

  Serial.begin(115200);
  Serial.println("READY 74HC4022, send a character");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  failed = false;
  runChecks();
  if (!failed) Serial.println("RESULT PASS");
  else Serial.println(resultLine);
}
