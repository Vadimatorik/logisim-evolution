/*
 * Self-check for a 74HC195 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * MR low clears Q0-Q3 and raises nQ3, with or without a clock. Parallel load
 * is the rising edge of CP while PE is low. PE high shifts Q0 toward Q3.
 * J and K both high set Q0; both low clear it; J high and K low toggles;
 * J low and K high retains. nQ3 is the complement of Q3.
 */

const uint8_t PIN_D0 = 2;
const uint8_t PIN_D1 = 3;
const uint8_t PIN_D2 = 4;
const uint8_t PIN_D3 = 5;
const uint8_t PIN_J = 6;
const uint8_t PIN_K = 7;
const uint8_t PIN_PE = 8;
const uint8_t PIN_MR = 9;
const uint8_t PIN_CP = 10;
const uint8_t PIN_Q0 = 11;
const uint8_t PIN_Q1 = 12;
const uint8_t PIN_Q2 = 13;
const uint8_t PIN_Q3 = A0;
const uint8_t PIN_NQ3 = A1;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s", step);
}

void setData(uint8_t code) {
  digitalWrite(PIN_D0, (code & 1) ? HIGH : LOW);
  digitalWrite(PIN_D1, (code & 2) ? HIGH : LOW);
  digitalWrite(PIN_D2, (code & 4) ? HIGH : LOW);
  digitalWrite(PIN_D3, (code & 8) ? HIGH : LOW);
}

uint8_t readWord() {
  uint8_t code = 0;
  if (digitalRead(PIN_Q0) == HIGH) code |= 1;
  if (digitalRead(PIN_Q1) == HIGH) code |= 2;
  if (digitalRead(PIN_Q2) == HIGH) code |= 4;
  if (digitalRead(PIN_Q3) == HIGH) code |= 8;
  return code;
}

void settle() { delayMicroseconds(20); }

void expectWord(const char* step, uint8_t code) {
  settle();
  const uint8_t actual = readWord();
  const bool nq3 = digitalRead(PIN_NQ3) == HIGH;
  const bool nq3Expected = (code & 8) == 0;
  Serial.print(step);
  Serial.print(" Q=");
  Serial.print(actual);
  Serial.print(" nQ3=");
  Serial.println(nq3 ? 1 : 0);
  if (actual != code || nq3 != nq3Expected) noteFailure(step);
}

void rise() {
  digitalWrite(PIN_CP, LOW);
  settle();
  digitalWrite(PIN_CP, HIGH);
  settle();
}

void load(uint8_t code) {
  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_PE, LOW);
  digitalWrite(PIN_J, LOW);
  digitalWrite(PIN_K, LOW);
  setData(code);
  rise();
}

void shiftLevel(bool jHigh, bool kHigh) {
  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_J, jHigh ? HIGH : LOW);
  digitalWrite(PIN_K, kHigh ? HIGH : LOW);
  rise();
}

void checkReset() {
  load(0xA);
  digitalWrite(PIN_MR, LOW);
  expectWord("reset", 0);

  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_PE, LOW);
  setData(0xF);
  expectWord("reset released", 0);
}

void checkLoad() {
  char step[40];
  for (uint8_t code = 0; code < 16; code++) {
    load(code);
    snprintf(step, sizeof(step), "load %u", code);
    expectWord(step, code);
  }
}

void checkHoldWithoutEdge() {
  load(0x5);
  digitalWrite(PIN_PE, LOW);
  setData(0xA);
  expectWord("hold high", 0x5);

  digitalWrite(PIN_CP, LOW);
  expectWord("hold low", 0x5);

  digitalWrite(PIN_CP, HIGH);
  expectWord("load after edge", 0xA);
}

void checkShift() {
  load(0b1010);
  shiftLevel(true, true);
  expectWord("shift 1010", 0b0101);

  load(0);
  const uint8_t serial[4] = {1, 1, 0, 1};
  uint8_t word = 0;
  char step[40];
  for (uint8_t index = 0; index < 4; index++) {
    const bool high = serial[index] != 0;
    shiftLevel(high, high);
    word = (uint8_t)(((word & 0x7) << 1) | serial[index]);
    snprintf(step, sizeof(step), "serial %u", index);
    expectWord(step, word);
  }
}

void checkJk() {
  load(0b0001);
  shiftLevel(true, true);
  expectWord("jk set", 0b0011);

  load(0b0001);
  shiftLevel(false, false);
  expectWord("jk clear", 0b0010);

  load(0b0001);
  shiftLevel(true, false);
  expectWord("jk toggle", 0b0010);

  load(0b0001);
  shiftLevel(false, true);
  expectWord("jk retain", 0b0011);
}

void checkResetOverridesClock() {
  load(0xF);
  digitalWrite(PIN_MR, LOW);
  digitalWrite(PIN_PE, LOW);
  setData(0xA);
  digitalWrite(PIN_CP, LOW);
  settle();
  digitalWrite(PIN_CP, HIGH);
  expectWord("reset over clock", 0);

  digitalWrite(PIN_MR, HIGH);
  expectWord("reset release holds", 0);

  rise();
  expectWord("load after reset", 0xA);
}

void runChecks() {
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_J, LOW);
  digitalWrite(PIN_K, HIGH);
  setData(0);
  settle();

  checkReset();
  checkLoad();
  checkHoldWithoutEdge();
  checkShift();
  checkJk();
  checkResetOverridesClock();
}

void setup() {
  pinMode(PIN_D0, OUTPUT);
  pinMode(PIN_D1, OUTPUT);
  pinMode(PIN_D2, OUTPUT);
  pinMode(PIN_D3, OUTPUT);
  pinMode(PIN_J, OUTPUT);
  pinMode(PIN_K, OUTPUT);
  pinMode(PIN_PE, OUTPUT);
  pinMode(PIN_MR, OUTPUT);
  pinMode(PIN_CP, OUTPUT);
  pinMode(PIN_Q0, INPUT);
  pinMode(PIN_Q1, INPUT);
  pinMode(PIN_Q2, INPUT);
  pinMode(PIN_Q3, INPUT);
  pinMode(PIN_NQ3, INPUT);

  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_J, LOW);
  digitalWrite(PIN_K, HIGH);
  setData(0);

  Serial.begin(115200);
  Serial.println("READY 74HC195, send a character");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  failed = false;
  runChecks();
  if (!failed) Serial.println("RESULT PASS");
  else Serial.println(resultLine);
}
