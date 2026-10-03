/*
 * Self-check for a 74HC190 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * nPL low loads D0-D3 onto Q. Counting is the rising edge of CP while nPL
 * is high and nCE is low. D/U low counts up. TC is high only at 9 counting
 * up or at 0 counting down, and only while nCE is low. nRC is low only
 * while TC is high and CP is low.
 */

const uint8_t PIN_D0 = 2;
const uint8_t PIN_D1 = 3;
const uint8_t PIN_D2 = 4;
const uint8_t PIN_D3 = 5;
const uint8_t PIN_CE = 6;
const uint8_t PIN_DU = 7;
const uint8_t PIN_PL = 8;
const uint8_t PIN_CP = 9;
const uint8_t PIN_Q0 = 10;
const uint8_t PIN_Q1 = 11;
const uint8_t PIN_Q2 = 12;
const uint8_t PIN_Q3 = 13;
const uint8_t PIN_TC = A0;
const uint8_t PIN_RC = A1;

// TI CD74HC190 Figure 3. Index 10..15 are the illegal BCD codes.
const uint8_t COUNT_UP[16] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 9, 4, 9, 0, 9, 0};
const uint8_t COUNT_DOWN[16] = {9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 3, 4, 5, 6};

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

uint8_t readCount() {
  uint8_t code = 0;
  if (digitalRead(PIN_Q0) == HIGH) code |= 1;
  if (digitalRead(PIN_Q1) == HIGH) code |= 2;
  if (digitalRead(PIN_Q2) == HIGH) code |= 4;
  if (digitalRead(PIN_Q3) == HIGH) code |= 8;
  return code;
}

void settle() { delayMicroseconds(20); }

void expectWord(const char* step, uint8_t code, bool terminal, bool rippleHigh) {
  settle();
  const uint8_t actual = readCount();
  const bool tc = digitalRead(PIN_TC) == HIGH;
  const bool rc = digitalRead(PIN_RC) == HIGH;
  Serial.print(step);
  Serial.print(" Q=");
  Serial.print(actual);
  Serial.print(" TC=");
  Serial.print(tc ? 1 : 0);
  Serial.print(" nRC=");
  Serial.println(rc ? 1 : 0);
  if (actual != code || tc != terminal || rc != rippleHigh) noteFailure(step);
}

void load(uint8_t code) {
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_CE, HIGH);
  digitalWrite(PIN_PL, LOW);
  setData(code);
  settle();
  digitalWrite(PIN_PL, HIGH);
  settle();
}

void rise() {
  digitalWrite(PIN_CP, LOW);
  settle();
  digitalWrite(PIN_CP, HIGH);
  settle();
}

void checkLoad() {
  char step[40];
  for (uint8_t code = 0; code < 16; code++) {
    load(code);
    snprintf(step, sizeof(step), "load %u", code);
    expectWord(step, code, false, true);
  }
}

void checkDirection(bool down) {
  char step[48];
  const uint8_t* table = down ? COUNT_DOWN : COUNT_UP;
  for (uint8_t code = 0; code < 16; code++) {
    load(code);
    digitalWrite(PIN_DU, down ? HIGH : LOW);
    digitalWrite(PIN_CE, LOW);
    digitalWrite(PIN_CP, LOW);
    const bool terminal = down ? code == 0 : code == 9;
    snprintf(step, sizeof(step), "%s %u before", down ? "down" : "up", code);
    expectWord(step, code, terminal, !terminal);

    digitalWrite(PIN_CP, HIGH);
    const uint8_t next = table[code];
    const bool nextTerminal = down ? next == 0 : next == 9;
    snprintf(step, sizeof(step), "%s %u after", down ? "down" : "up", code);
    expectWord(step, next, nextTerminal, true);
    digitalWrite(PIN_CP, LOW);
  }
}

void checkInhibitAndLoadOverride() {
  load(3);
  digitalWrite(PIN_CE, HIGH);
  digitalWrite(PIN_DU, LOW);
  rise();
  digitalWrite(PIN_CP, LOW);
  expectWord("inhibit", 3, false, true);

  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_PL, LOW);
  setData(12);
  digitalWrite(PIN_CP, HIGH);
  expectWord("load over clock", 12, false, true);
  digitalWrite(PIN_PL, HIGH);
  digitalWrite(PIN_CP, LOW);
}

void runChecks() {
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_PL, HIGH);
  digitalWrite(PIN_CE, HIGH);
  digitalWrite(PIN_DU, LOW);
  setData(0);
  settle();

  checkLoad();
  checkDirection(false);
  checkDirection(true);
  checkInhibitAndLoadOverride();
}

void setup() {
  pinMode(PIN_D0, OUTPUT);
  pinMode(PIN_D1, OUTPUT);
  pinMode(PIN_D2, OUTPUT);
  pinMode(PIN_D3, OUTPUT);
  pinMode(PIN_CE, OUTPUT);
  pinMode(PIN_DU, OUTPUT);
  pinMode(PIN_PL, OUTPUT);
  pinMode(PIN_CP, OUTPUT);
  pinMode(PIN_Q0, INPUT);
  pinMode(PIN_Q1, INPUT);
  pinMode(PIN_Q2, INPUT);
  pinMode(PIN_Q3, INPUT);
  pinMode(PIN_TC, INPUT);
  pinMode(PIN_RC, INPUT);

  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_PL, HIGH);
  digitalWrite(PIN_CE, HIGH);
  digitalWrite(PIN_DU, LOW);
  setData(0);

  Serial.begin(115200);
  Serial.println("READY 74HC190, send a character");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  failed = false;
  runChecks();
  if (!failed) Serial.println("RESULT PASS");
  else Serial.println(resultLine);
}
