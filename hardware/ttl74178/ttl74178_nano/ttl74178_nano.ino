/*
 * Self-check for a 74HC178 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * The model follows SN74178: the register changes on the falling edge of CLK.
 * SHIFT high moves QA toward QD and copies SER into QA, ignoring LOAD.
 * SHIFT low and LOAD high copies A B C D. Both low hold the word.
 * Bit 0 of a code is QA/A and bit 3 is QD/D.
 */

const uint8_t PIN_B = 2;
const uint8_t PIN_A = 3;
const uint8_t PIN_SER = 4;
const uint8_t PIN_CLK = 5;
const uint8_t PIN_LOAD = 6;
const uint8_t PIN_SHIFT = 7;
const uint8_t PIN_D = 8;
const uint8_t PIN_C = 9;
const uint8_t PIN_QA = 10;
const uint8_t PIN_QB = 11;
const uint8_t PIN_QC = 12;
const uint8_t PIN_QD = 13;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s", step);
}

void setParallel(uint8_t code) {
  digitalWrite(PIN_A, (code & 1) ? HIGH : LOW);
  digitalWrite(PIN_B, (code & 2) ? HIGH : LOW);
  digitalWrite(PIN_C, (code & 4) ? HIGH : LOW);
  digitalWrite(PIN_D, (code & 8) ? HIGH : LOW);
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

void load(uint8_t code) {
  digitalWrite(PIN_SHIFT, LOW);
  digitalWrite(PIN_LOAD, HIGH);
  digitalWrite(PIN_SER, LOW);
  setParallel(code);
  fall();
}

void shiftLevel(bool serialHigh, bool loadHigh, uint8_t parallel) {
  digitalWrite(PIN_SHIFT, HIGH);
  digitalWrite(PIN_LOAD, loadHigh ? HIGH : LOW);
  digitalWrite(PIN_SER, serialHigh ? HIGH : LOW);
  setParallel(parallel);
  fall();
}

void checkLoad() {
  char step[40];
  for (uint8_t code = 0; code < 16; code++) {
    load(code);
    snprintf(step, sizeof(step), "load %u", code);
    expectWord(step, code);
  }
}

void checkNoEdge() {
  load(0x5);
  digitalWrite(PIN_SHIFT, LOW);
  digitalWrite(PIN_LOAD, HIGH);
  setParallel(0xA);
  expectWord("hold while low", 0x5);

  digitalWrite(PIN_CLK, HIGH);
  expectWord("hold on rise", 0x5);

  digitalWrite(PIN_CLK, LOW);
  expectWord("load on fall", 0xA);
}

void checkHold() {
  load(0x9);
  digitalWrite(PIN_SHIFT, LOW);
  digitalWrite(PIN_LOAD, LOW);
  digitalWrite(PIN_SER, HIGH);
  setParallel(0x6);
  fall();
  fall();
  expectWord("hold while clock runs", 0x9);
}

void checkShift() {
  load(0b1010);
  shiftLevel(true, false, 0);
  expectWord("shift toward QD", 0b0101);

  load(0b0001);
  shiftLevel(false, true, 0xF);
  expectWord("shift ignores load", 0b0010);
}

void runChecks() {
  digitalWrite(PIN_CLK, LOW);
  digitalWrite(PIN_SHIFT, LOW);
  digitalWrite(PIN_LOAD, LOW);
  digitalWrite(PIN_SER, LOW);
  setParallel(0);
  settle();

  checkLoad();
  checkNoEdge();
  checkHold();
  checkShift();
}

void setup() {
  pinMode(PIN_B, OUTPUT);
  pinMode(PIN_A, OUTPUT);
  pinMode(PIN_SER, OUTPUT);
  pinMode(PIN_CLK, OUTPUT);
  pinMode(PIN_LOAD, OUTPUT);
  pinMode(PIN_SHIFT, OUTPUT);
  pinMode(PIN_D, OUTPUT);
  pinMode(PIN_C, OUTPUT);
  pinMode(PIN_QA, INPUT);
  pinMode(PIN_QB, INPUT);
  pinMode(PIN_QC, INPUT);
  pinMode(PIN_QD, INPUT);

  digitalWrite(PIN_CLK, LOW);
  digitalWrite(PIN_SHIFT, LOW);
  digitalWrite(PIN_LOAD, LOW);
  digitalWrite(PIN_SER, LOW);
  setParallel(0);

  Serial.begin(115200);
  Serial.println("READY 74HC178, send a character");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  failed = false;
  runChecks();
  if (!failed) Serial.println("RESULT PASS");
  else Serial.println(resultLine);
}
