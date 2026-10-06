/*
 * Self-check for a 74HC160 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * MR (TI CLR) low clears Q asynchronously. A rising CP loads D0-D3 when PE
 * (TI LOAD) is low. It counts when PE, CEP and CET are high. TC is high only
 * while CET is high and the code is 9. Illegal codes return to BCD within
 * two clocks: 10 to 11 to 6, 12 to 13 to 4, and 14 to 15 to 2.
 * MR stays low until the check starts.
 */

const uint8_t PIN_MR = 2;
const uint8_t PIN_CP = 3;
const uint8_t PIN_D0 = 4;
const uint8_t PIN_D1 = 5;
const uint8_t PIN_D2 = 6;
const uint8_t PIN_D3 = 7;
const uint8_t PIN_CEP = 8;
const uint8_t PIN_PE = 9;
const uint8_t PIN_CET = 10;
const uint8_t PIN_Q3 = 11;
const uint8_t PIN_Q2 = 12;
const uint8_t PIN_Q1 = 13;
const uint8_t PIN_Q0 = A0;
const uint8_t PIN_TC = A1;

// One count from each code. Index 10..15 are the illegal BCD codes.
const uint8_t NEXT_COUNT[16] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 11, 6, 13, 4, 15, 2};

bool failed = false;
char resultLine[120];

void noteFailure(const char* step, uint8_t expected, uint8_t actual, bool expectedTc, bool actualTc) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s expected Q=%u TC=%u got Q=%u TC=%u",
      step,
      expected,
      expectedTc ? 1 : 0,
      actual,
      actualTc ? 1 : 0);
}

void settle() { delayMicroseconds(50); }

void setData(uint8_t code) {
  digitalWrite(PIN_D0, (code & 1) ? HIGH : LOW);
  digitalWrite(PIN_D1, (code & 2) ? HIGH : LOW);
  digitalWrite(PIN_D2, (code & 4) ? HIGH : LOW);
  digitalWrite(PIN_D3, (code & 8) ? HIGH : LOW);
}

uint8_t readCount() {
  uint8_t code = 0;
  if (digitalRead(PIN_Q0) == HIGH) code |= 1;
  if (digitalRead(PIN_Q1) == HIGH) code |= 2;
  if (digitalRead(PIN_Q2) == HIGH) code |= 4;
  if (digitalRead(PIN_Q3) == HIGH) code |= 8;
  return code;
}

void expectWord(const char* step, uint8_t code, bool terminal) {
  settle();
  const uint8_t actual = readCount();
  const bool tc = digitalRead(PIN_TC) == HIGH;
  Serial.print(step);
  Serial.print(" Q=");
  Serial.print(actual);
  Serial.print(" TC=");
  Serial.println(tc ? 1 : 0);
  if (actual != code || tc != terminal) noteFailure(step, code, actual, terminal, tc);
}

void pulse() {
  digitalWrite(PIN_CP, LOW);
  settle();
  digitalWrite(PIN_CP, HIGH);
  settle();
  digitalWrite(PIN_CP, LOW);
  settle();
}

void load(uint8_t code) {
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_PE, LOW);
  digitalWrite(PIN_CET, HIGH);
  setData(code);
  pulse();
  digitalWrite(PIN_PE, HIGH);
}

void enableCount() {
  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_CEP, HIGH);
  digitalWrite(PIN_CET, HIGH);
}

void checkResetWinsOverLoadAndClock() {
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_PE, LOW);
  digitalWrite(PIN_CEP, HIGH);
  digitalWrite(PIN_CET, HIGH);
  setData(15);
  digitalWrite(PIN_MR, LOW);
  expectWord("reset", 0, false);

  digitalWrite(PIN_CP, HIGH);
  expectWord("reset during clock", 0, false);
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_PE, HIGH);
  expectWord("reset released", 0, false);
}

void checkLoad() {
  char step[24];
  for (uint8_t code = 0; code < 16; code++) {
    load(code);
    snprintf(step, sizeof(step), "load %u", code);
    expectWord(step, code, code == 9);
  }
}

void checkCount() {
  char step[24];
  for (uint8_t code = 0; code < 16; code++) {
    load(code);
    enableCount();
    pulse();
    const uint8_t next = NEXT_COUNT[code];
    snprintf(step, sizeof(step), "count %u", code);
    expectWord(step, next, next == 9);
  }
}

void checkTerminalCountAndHold() {
  load(9);
  digitalWrite(PIN_CET, LOW);
  expectWord("cet low at 9", 9, false);
  digitalWrite(PIN_CET, HIGH);
  expectWord("cet high at 9", 9, true);

  digitalWrite(PIN_CEP, LOW);
  setData(1);
  pulse();
  expectWord("cep hold", 9, true);

  digitalWrite(PIN_CEP, HIGH);
  digitalWrite(PIN_CET, LOW);
  pulse();
  expectWord("cet hold", 9, false);
}

void checkFallingEdgeAndLoadPriority() {
  load(0);
  enableCount();
  digitalWrite(PIN_CP, HIGH);
  expectWord("rising to 1", 1, false);
  digitalWrite(PIN_CP, LOW);
  expectWord("falling holds 1", 1, false);

  digitalWrite(PIN_PE, LOW);
  digitalWrite(PIN_CEP, HIGH);
  digitalWrite(PIN_CET, HIGH);
  setData(7);
  pulse();
  expectWord("load over count", 7, false);
}

void runChecks() {
  digitalWrite(PIN_MR, LOW);
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_CEP, LOW);
  digitalWrite(PIN_CET, LOW);
  setData(0);
  settle();

  checkResetWinsOverLoadAndClock();
  checkLoad();
  checkCount();
  checkTerminalCountAndHold();
  checkFallingEdgeAndLoadPriority();

  digitalWrite(PIN_MR, LOW);
  digitalWrite(PIN_CP, LOW);
}

void setup() {
  pinMode(PIN_MR, OUTPUT);
  pinMode(PIN_CP, OUTPUT);
  pinMode(PIN_D0, OUTPUT);
  pinMode(PIN_D1, OUTPUT);
  pinMode(PIN_D2, OUTPUT);
  pinMode(PIN_D3, OUTPUT);
  pinMode(PIN_CEP, OUTPUT);
  pinMode(PIN_PE, OUTPUT);
  pinMode(PIN_CET, OUTPUT);
  pinMode(PIN_Q0, INPUT);
  pinMode(PIN_Q1, INPUT);
  pinMode(PIN_Q2, INPUT);
  pinMode(PIN_Q3, INPUT);
  pinMode(PIN_TC, INPUT);

  digitalWrite(PIN_MR, LOW);
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_CEP, LOW);
  digitalWrite(PIN_CET, LOW);
  setData(0);

  Serial.begin(115200);
  Serial.println("READY 74HC160, send a character");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  failed = false;
  runChecks();
  if (!failed) Serial.println("RESULT PASS");
  else Serial.println(resultLine);
}
