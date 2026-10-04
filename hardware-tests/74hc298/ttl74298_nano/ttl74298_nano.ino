/*
 * Self-check for a 74HC298 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * The model follows HD74HC298: the register changes on the falling edge of CLK.
 * WS low stores word 1 (A1 B1 C1 D1). WS high stores word 2 (A2 B2 C2 D2).
 * Bit 0 of a code is A/QA and bit 3 is D/QD.
 */

const uint8_t PIN_B2 = 2;
const uint8_t PIN_A2 = 3;
const uint8_t PIN_A1 = 4;
const uint8_t PIN_B1 = 5;
const uint8_t PIN_C2 = 6;
const uint8_t PIN_D2 = 7;
const uint8_t PIN_D1 = 8;
const uint8_t PIN_C1 = 9;
const uint8_t PIN_WS = 10;
const uint8_t PIN_CLK = 11;
const uint8_t PIN_QD = 12;
const uint8_t PIN_QC = A0;
const uint8_t PIN_QB = A1;
const uint8_t PIN_QA = A2;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s", step);
}

void setWord(uint8_t code, bool word2) {
  const uint8_t pinA = word2 ? PIN_A2 : PIN_A1;
  const uint8_t pinB = word2 ? PIN_B2 : PIN_B1;
  const uint8_t pinC = word2 ? PIN_C2 : PIN_C1;
  const uint8_t pinD = word2 ? PIN_D2 : PIN_D1;
  digitalWrite(pinA, (code & 1) ? HIGH : LOW);
  digitalWrite(pinB, (code & 2) ? HIGH : LOW);
  digitalWrite(pinC, (code & 4) ? HIGH : LOW);
  digitalWrite(pinD, (code & 8) ? HIGH : LOW);
}

uint8_t readWord() {
  uint8_t code = 0;
  if (digitalRead(PIN_QA) == HIGH) code |= 1;
  if (digitalRead(PIN_QB) == HIGH) code |= 2;
  if (digitalRead(PIN_QC) == HIGH) code |= 4;
  if (digitalRead(PIN_QD) == HIGH) code |= 8;
  return code;
}

void settle() { delayMicroseconds(20); }

void expectWord(const char* step, uint8_t code) {
  settle();
  const uint8_t actual = readWord();
  Serial.print(step);
  Serial.print(" Q=");
  Serial.println(actual);
  if (actual != code) noteFailure(step);
}

void fall() {
  digitalWrite(PIN_CLK, HIGH);
  settle();
  digitalWrite(PIN_CLK, LOW);
  settle();
}

void capture(bool word2, uint8_t code) {
  digitalWrite(PIN_WS, word2 ? HIGH : LOW);
  setWord(code, word2);
  setWord(code ^ 0xF, !word2);
  fall();
}

void checkWord1() {
  char step[40];
  for (uint8_t code = 0; code < 16; code++) {
    capture(false, code);
    snprintf(step, sizeof(step), "word1 %u", code);
    expectWord(step, code);
  }
}

void checkWord2() {
  char step[40];
  for (uint8_t code = 0; code < 16; code++) {
    capture(true, code);
    snprintf(step, sizeof(step), "word2 %u", code);
    expectWord(step, code);
  }
}

void checkNoEdge() {
  capture(false, 0x5);
  digitalWrite(PIN_WS, HIGH);
  setWord(0x3, false);
  setWord(0xC, true);
  expectWord("hold while low", 0x5);

  digitalWrite(PIN_CLK, HIGH);
  expectWord("hold on rise", 0x5);

  digitalWrite(PIN_CLK, LOW);
  expectWord("word2 on fall", 0xC);
}

void runChecks() {
  digitalWrite(PIN_CLK, LOW);
  digitalWrite(PIN_WS, LOW);
  setWord(0, false);
  setWord(0, true);
  settle();

  checkWord1();
  checkWord2();
  checkNoEdge();
}

void setup() {
  pinMode(PIN_B2, OUTPUT);
  pinMode(PIN_A2, OUTPUT);
  pinMode(PIN_A1, OUTPUT);
  pinMode(PIN_B1, OUTPUT);
  pinMode(PIN_C2, OUTPUT);
  pinMode(PIN_D2, OUTPUT);
  pinMode(PIN_D1, OUTPUT);
  pinMode(PIN_C1, OUTPUT);
  pinMode(PIN_WS, OUTPUT);
  pinMode(PIN_CLK, OUTPUT);
  pinMode(PIN_QD, INPUT);
  pinMode(PIN_QC, INPUT);
  pinMode(PIN_QB, INPUT);
  pinMode(PIN_QA, INPUT);

  digitalWrite(PIN_CLK, LOW);
  digitalWrite(PIN_WS, LOW);
  setWord(0, false);
  setWord(0, true);

  Serial.begin(115200);
  Serial.println("READY 74HC298, send a character");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  failed = false;
  runChecks();
  if (!failed) Serial.println("RESULT PASS");
  else Serial.println(resultLine);
}
