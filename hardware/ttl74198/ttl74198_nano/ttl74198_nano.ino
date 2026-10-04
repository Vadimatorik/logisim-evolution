/*
 * Self-check for a 74HC198 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * MR is an asynchronous active-low clear. Load, shift right, shift left and
 * hold are sampled on the rising edge of CP. Q0/Q1 share A6 and Q2/Q3 share
 * A7 through 10k and 30k resistors. MR stays low and CP stays low until the
 * check starts, so the outputs should already be zero.
 */

const uint8_t PIN_S0 = 2;
const uint8_t PIN_DSR = 3;
const uint8_t PIN_D0 = 4;
const uint8_t PIN_D1 = 5;
const uint8_t PIN_D2 = 6;
const uint8_t PIN_D3 = 7;
const uint8_t PIN_CP = 8;
const uint8_t PIN_MR = 9;
const uint8_t PIN_DSL = 10;
const uint8_t PIN_D4 = 11;
const uint8_t PIN_D5 = 12;
const uint8_t PIN_D6 = 13;
const uint8_t PIN_D7 = A0;
const uint8_t PIN_S1 = A1;
const uint8_t PIN_Q4 = A2;
const uint8_t PIN_Q5 = A3;
const uint8_t PIN_Q6 = A4;
const uint8_t PIN_Q7 = A5;

const uint8_t PAIR_BOTH_LOW = 0;
const uint8_t PAIR_LOW_10K = 1;
const uint8_t PAIR_LOW_30K = 2;
const uint8_t PAIR_BOTH_HIGH = 3;

bool failed = false;
char resultLine[160];
int lastAdc6 = 0;
int lastAdc7 = 0;

void noteFailure(const char* step, uint8_t expected, uint8_t actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s expected %02X got %02X adc6=%d adc7=%d",
      step,
      expected,
      actual,
      lastAdc6,
      lastAdc7);
}

void settle() { delay(1); }

void setData(uint8_t code) {
  digitalWrite(PIN_D0, (code & 0x01) ? HIGH : LOW);
  digitalWrite(PIN_D1, (code & 0x02) ? HIGH : LOW);
  digitalWrite(PIN_D2, (code & 0x04) ? HIGH : LOW);
  digitalWrite(PIN_D3, (code & 0x08) ? HIGH : LOW);
  digitalWrite(PIN_D4, (code & 0x10) ? HIGH : LOW);
  digitalWrite(PIN_D5, (code & 0x20) ? HIGH : LOW);
  digitalWrite(PIN_D6, (code & 0x40) ? HIGH : LOW);
  digitalWrite(PIN_D7, (code & 0x80) ? HIGH : LOW);
}

void setMode(bool s1High, bool s0High) {
  digitalWrite(PIN_S1, s1High ? HIGH : LOW);
  digitalWrite(PIN_S0, s0High ? HIGH : LOW);
}

int readAdc(uint8_t pin) {
  analogRead(pin);
  delayMicroseconds(300);
  long sum = 0;
  for (uint8_t sample = 0; sample < 4; sample++) {
    sum += analogRead(pin);
  }
  return (int)(sum / 4);
}

uint8_t classify(int adc) {
  if (adc < 128) return PAIR_BOTH_LOW;
  if (adc < 512) return PAIR_LOW_10K;
  if (adc < 896) return PAIR_LOW_30K;
  return PAIR_BOTH_HIGH;
}

bool pairBit(uint8_t pair, bool tenKSide) {
  if (pair == PAIR_BOTH_HIGH) return true;
  if (pair == PAIR_BOTH_LOW) return false;
  if (tenKSide) return pair == PAIR_LOW_30K;
  return pair == PAIR_LOW_10K;
}

uint8_t readWord() {
  lastAdc6 = readAdc(A6);
  lastAdc7 = readAdc(A7);
  const uint8_t pair6 = classify(lastAdc6);
  const uint8_t pair7 = classify(lastAdc7);
  uint8_t value = 0;
  if (pairBit(pair6, true)) value |= 0x01;
  if (pairBit(pair6, false)) value |= 0x02;
  if (pairBit(pair7, true)) value |= 0x04;
  if (pairBit(pair7, false)) value |= 0x08;
  if (digitalRead(PIN_Q4)) value |= 0x10;
  if (digitalRead(PIN_Q5)) value |= 0x20;
  if (digitalRead(PIN_Q6)) value |= 0x40;
  if (digitalRead(PIN_Q7)) value |= 0x80;
  return value;
}

void expectWord(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readWord();
  if (actual != expected) noteFailure(step, expected, actual);
}

void clockRise() {
  digitalWrite(PIN_CP, HIGH);
  settle();
}

void clockFall() {
  digitalWrite(PIN_CP, LOW);
  settle();
}

void clockPulse() {
  clockFall();
  clockRise();
  clockFall();
}

void load(uint8_t code) {
  digitalWrite(PIN_MR, HIGH);
  setMode(true, true);
  setData(code);
  clockPulse();
}

void shiftRight(bool serialHigh) {
  digitalWrite(PIN_MR, HIGH);
  setMode(false, true);
  digitalWrite(PIN_DSR, serialHigh ? HIGH : LOW);
  clockPulse();
}

void shiftLeft(bool serialHigh) {
  digitalWrite(PIN_MR, HIGH);
  setMode(true, false);
  digitalWrite(PIN_DSL, serialHigh ? HIGH : LOW);
  clockPulse();
}

void checkResetOverridesLoad() {
  expectWord(0, "cleared before clock");

  digitalWrite(PIN_MR, HIGH);
  setMode(true, true);
  setData(0xA5);
  settle();
  expectWord(0, "load waits for edge");
  clockPulse();
  expectWord(0xA5, "load on edge");

  digitalWrite(PIN_MR, LOW);
  settle();
  expectWord(0, "async reset");

  setMode(true, true);
  setData(0x5A);
  clockPulse();
  expectWord(0, "reset overrides load");

  digitalWrite(PIN_MR, HIGH);
  settle();
  expectWord(0, "release holds");
}

void checkLoads() {
  const uint8_t codes[] = {0x00, 0xFF, 0xA5, 0x5A, 0x01, 0x80};
  for (uint8_t index = 0; index < 6; index++) {
    load(codes[index]);
    expectWord(codes[index], "load");
  }
}

void checkShiftRight() {
  load(0);
  shiftRight(true);
  expectWord(0x01, "shift right entry");
  for (uint8_t step = 1; step < 8; step++) {
    shiftRight(false);
    expectWord((uint8_t)(1U << step), "shift right walk");
  }
  shiftRight(false);
  expectWord(0, "shift right exit");
}

void checkShiftLeft() {
  load(0);
  shiftLeft(true);
  expectWord(0x80, "shift left entry");
  for (uint8_t step = 1; step < 8; step++) {
    shiftLeft(false);
    expectWord((uint8_t)(0x80 >> step), "shift left walk");
  }
  shiftLeft(false);
  expectWord(0, "shift left exit");
}

void checkHold() {
  load(0x96);
  setMode(false, false);
  digitalWrite(PIN_DSR, HIGH);
  digitalWrite(PIN_DSL, HIGH);
  setData(0x00);
  clockPulse();
  expectWord(0x96, "hold");
}

void checkFallingEdge() {
  load(0x3C);
  setMode(false, false);
  clockRise();
  expectWord(0x3C, "hold on rise");

  setMode(false, true);
  digitalWrite(PIN_DSR, HIGH);
  settle();
  expectWord(0x3C, "mode waits for edge");
  clockFall();
  expectWord(0x3C, "falling edge");
  clockRise();
  expectWord(0x79, "shift on next rise");
}

void setup() {
  pinMode(PIN_S0, OUTPUT);
  pinMode(PIN_DSR, OUTPUT);
  pinMode(PIN_D0, OUTPUT);
  pinMode(PIN_D1, OUTPUT);
  pinMode(PIN_D2, OUTPUT);
  pinMode(PIN_D3, OUTPUT);
  pinMode(PIN_CP, OUTPUT);
  pinMode(PIN_MR, OUTPUT);
  pinMode(PIN_DSL, OUTPUT);
  pinMode(PIN_D4, OUTPUT);
  pinMode(PIN_D5, OUTPUT);
  pinMode(PIN_D6, OUTPUT);
  pinMode(PIN_D7, OUTPUT);
  pinMode(PIN_S1, OUTPUT);
  pinMode(PIN_Q4, INPUT);
  pinMode(PIN_Q5, INPUT);
  pinMode(PIN_Q6, INPUT);
  pinMode(PIN_Q7, INPUT);

  digitalWrite(PIN_MR, LOW);
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_DSR, LOW);
  digitalWrite(PIN_DSL, LOW);
  setMode(false, false);
  setData(0);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  checkResetOverridesLoad();
  checkLoads();
  checkShiftRight();
  checkShiftLeft();
  checkHold();
  checkFallingEdge();

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
