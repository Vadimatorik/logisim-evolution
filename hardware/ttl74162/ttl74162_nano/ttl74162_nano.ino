/*
 * Self-check for a 74HC162 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * MR and PE are active low and are sampled on the rising edge of CP.
 * Both CEP and CET must be high to count. TC is high only at code 9 while
 * CET is high. Outputs are push-pull, so Q and TC are read directly.
 * MR stays low, CP stays low, PE stays high and both count enables stay low
 * until the check starts. The first clock then clears the counter.
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
const uint8_t PIN_Q1 = A0;
const uint8_t PIN_Q0 = A1;
const uint8_t PIN_TC = A2;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, uint8_t expected, uint8_t actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s expected %02X got %02X",
      step,
      expected,
      actual);
}

void settle() { delay(1); }

void setData(uint8_t code) {
  digitalWrite(PIN_D0, (code & 1) ? HIGH : LOW);
  digitalWrite(PIN_D1, (code & 2) ? HIGH : LOW);
  digitalWrite(PIN_D2, (code & 4) ? HIGH : LOW);
  digitalWrite(PIN_D3, (code & 8) ? HIGH : LOW);
}

uint8_t readCount() {
  uint8_t value = 0;
  if (digitalRead(PIN_Q0)) value |= 1;
  if (digitalRead(PIN_Q1)) value |= 2;
  if (digitalRead(PIN_Q2)) value |= 4;
  if (digitalRead(PIN_Q3)) value |= 8;
  return value;
}

void expectCount(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readCount();
  if (actual != expected) noteFailure(step, expected, actual);
}

void expectTc(bool high, const char* step) {
  settle();
  const uint8_t actual = digitalRead(PIN_TC) ? 1 : 0;
  const uint8_t expected = high ? 1 : 0;
  if (actual != expected) noteFailure(step, expected, actual);
}

void clockPulse() {
  digitalWrite(PIN_CP, LOW);
  settle();
  digitalWrite(PIN_CP, HIGH);
  settle();
  digitalWrite(PIN_CP, LOW);
  settle();
}

void enableCount() {
  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_CEP, HIGH);
  digitalWrite(PIN_CET, HIGH);
}

void load(uint8_t code) {
  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_CEP, LOW);
  digitalWrite(PIN_CET, LOW);
  digitalWrite(PIN_PE, LOW);
  setData(code);
  clockPulse();
  digitalWrite(PIN_PE, HIGH);
}

void checkResetOverridesLoad() {
  load(7);
  digitalWrite(PIN_MR, LOW);
  digitalWrite(PIN_PE, LOW);
  setData(12);
  settle();
  expectCount(7, "reset before clock");
  clockPulse();
  expectCount(0, "sync reset");
  expectTc(false, "tc after reset");
  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_PE, HIGH);
}

void checkLoads() {
  for (uint8_t code = 0; code < 16; code++) {
    load(code);
    expectCount(code, "load");
    expectTc(false, "tc while cet low");
  }
}

void checkDecadeAndTerminalCount() {
  load(0);
  enableCount();
  for (uint8_t code = 0; code < 10; code++) {
    expectCount(code, "decade");
    expectTc(code == 9, "tc at nine");
    clockPulse();
  }
  expectCount(0, "wrap");
  expectTc(false, "tc after wrap");
}

void checkEnablesHold() {
  load(4);
  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_CEP, LOW);
  digitalWrite(PIN_CET, HIGH);
  clockPulse();
  expectCount(4, "cep low holds");
  expectTc(false, "tc away from nine");

  load(9);
  digitalWrite(PIN_CEP, LOW);
  digitalWrite(PIN_CET, HIGH);
  settle();
  expectCount(9, "hold at nine");
  expectTc(true, "tc with cet high");
  clockPulse();
  expectCount(9, "cep low holds nine");
  expectTc(true, "tc stays with cet");

  digitalWrite(PIN_CET, LOW);
  settle();
  expectCount(9, "cet low keeps nine");
  expectTc(false, "cet low clears tc");

  digitalWrite(PIN_CEP, HIGH);
  clockPulse();
  expectCount(9, "cet low holds");
  expectTc(false, "tc stays low");
}

void checkIllegalCodes() {
  const uint8_t start[] = {10, 12, 14};
  const uint8_t first[] = {11, 13, 15};
  const uint8_t second[] = {6, 4, 2};
  enableCount();
  for (uint8_t index = 0; index < 3; index++) {
    load(start[index]);
    enableCount();
    clockPulse();
    expectCount(first[index], "illegal first");
    clockPulse();
    expectCount(second[index], "illegal second");
  }
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
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  clockPulse();
  expectCount(0, "initial reset");

  checkResetOverridesLoad();
  checkLoads();
  checkDecadeAndTerminalCount();
  checkEnablesHold();
  checkIllegalCodes();

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
