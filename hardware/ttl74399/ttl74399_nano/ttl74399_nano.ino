/*
 * Self-check for a 74HC399 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * The model follows SN74LS399 and the pin-compatible 74AC399/74F399.
 * The register changes on the rising edge of CP.
 * S low stores port 0 (I0a I0b I0c I0d). S high stores port 1 (I1a I1b I1c I1d).
 * Bit 0 of a code is a/Qa and bit 3 is d/Qd.
 */

const uint8_t PIN_S = 2;
const uint8_t PIN_CP = 3;
const uint8_t PIN_I0A = 4;
const uint8_t PIN_I1A = 5;
const uint8_t PIN_I0B = 6;
const uint8_t PIN_I1B = 7;
const uint8_t PIN_I0C = 8;
const uint8_t PIN_I1C = 9;
const uint8_t PIN_I0D = 10;
const uint8_t PIN_I1D = 11;
const uint8_t PIN_QA = 12;
const uint8_t PIN_QB = A0;
const uint8_t PIN_QC = A1;
const uint8_t PIN_QD = A2;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s", step);
}

void setPort(uint8_t code, bool port1) {
  const uint8_t pinA = port1 ? PIN_I1A : PIN_I0A;
  const uint8_t pinB = port1 ? PIN_I1B : PIN_I0B;
  const uint8_t pinC = port1 ? PIN_I1C : PIN_I0C;
  const uint8_t pinD = port1 ? PIN_I1D : PIN_I0D;
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

void rise() {
  digitalWrite(PIN_CP, LOW);
  settle();
  digitalWrite(PIN_CP, HIGH);
  settle();
  digitalWrite(PIN_CP, LOW);
  settle();
}

void capture(bool port1, uint8_t code) {
  digitalWrite(PIN_S, port1 ? HIGH : LOW);
  setPort(code, port1);
  setPort(code ^ 0xF, !port1);
  rise();
}

void checkPort0() {
  char step[40];
  for (uint8_t code = 0; code < 16; code++) {
    capture(false, code);
    snprintf(step, sizeof(step), "port0 %u", code);
    expectWord(step, code);
  }
}

void checkPort1() {
  char step[40];
  for (uint8_t code = 0; code < 16; code++) {
    capture(true, code);
    snprintf(step, sizeof(step), "port1 %u", code);
    expectWord(step, code);
  }
}

void checkEdges() {
  capture(false, 0x5);
  digitalWrite(PIN_S, HIGH);
  setPort(0x3, false);
  setPort(0xC, true);
  expectWord("hold while low", 0x5);

  digitalWrite(PIN_CP, HIGH);
  expectWord("port1 on rise", 0xC);

  digitalWrite(PIN_S, LOW);
  setPort(0xA, false);
  setPort(0x1, true);
  expectWord("hold while high", 0xC);

  digitalWrite(PIN_CP, LOW);
  expectWord("hold on fall", 0xC);

  digitalWrite(PIN_CP, HIGH);
  expectWord("port0 on next rise", 0xA);
  digitalWrite(PIN_CP, LOW);
}

void runChecks() {
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_S, LOW);
  setPort(0, false);
  setPort(0, true);
  settle();

  checkPort0();
  checkPort1();
  checkEdges();
}

void setup() {
  pinMode(PIN_S, OUTPUT);
  pinMode(PIN_CP, OUTPUT);
  pinMode(PIN_I0A, OUTPUT);
  pinMode(PIN_I1A, OUTPUT);
  pinMode(PIN_I0B, OUTPUT);
  pinMode(PIN_I1B, OUTPUT);
  pinMode(PIN_I0C, OUTPUT);
  pinMode(PIN_I1C, OUTPUT);
  pinMode(PIN_I0D, OUTPUT);
  pinMode(PIN_I1D, OUTPUT);
  pinMode(PIN_QA, INPUT);
  pinMode(PIN_QB, INPUT);
  pinMode(PIN_QC, INPUT);
  pinMode(PIN_QD, INPUT);

  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_S, LOW);
  setPort(0, false);
  setPort(0, true);

  Serial.begin(115200);
  Serial.println("READY 74HC399, send a character");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  failed = false;
  runChecks();
  if (!failed) Serial.println("RESULT PASS");
  else Serial.println(resultLine);
}
