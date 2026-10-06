/*
 * Self-check for a 74HC4094 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * A rising CP shifts D into QP0 and toward QP7. QS1 is the last shift stage.
 * A falling CP copies QS1 into QS2. STR high makes storage follow the shift
 * register, including a rising STR with CP held still. OE low releases QP0..QP7
 * and leaves QS1 and QS2 driven. Hi-Z is detected by precharge.
 *
 * Bytes are shifted most significant bit first, so QP0 is the least significant bit.
 */

const uint8_t PIN_CP = 2;
const uint8_t PIN_STR = 3;
const uint8_t PIN_D = 4;
const uint8_t PIN_OE = 5;
// QP0..QP7. QP7 is D13, so the board LED follows QP7.
const uint8_t PIN_QP[8] = {6, 7, 8, 9, 10, 11, 12, 13};
const uint8_t PIN_QS1 = A0;
const uint8_t PIN_QS2 = A1;

bool failed = false;
char resultLine[96];

uint8_t shiftReg = 0;
uint8_t storeReg = 0;
bool qs2High = false;
bool strobeHigh = true;
bool outputEnabled = true;

void noteFailure(const char* step, const char* detail) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s %s", step, detail);
}

void settle() { delayMicroseconds(5); }

uint8_t readParallel() {
  uint8_t value = 0;
  for (uint8_t stage = 0; stage < 8; stage++) {
    pinMode(PIN_QP[stage], INPUT);
    if (digitalRead(PIN_QP[stage]) == HIGH) value |= (uint8_t)(1u << stage);
  }
  return value;
}

bool readHigh(uint8_t pin) {
  pinMode(pin, INPUT);
  return digitalRead(pin) == HIGH;
}

void expectParallel(const char* step, uint8_t expected) {
  delay(1);
  const uint8_t actual = readParallel();
  Serial.print(step);
  Serial.print(" QP expected=0x");
  Serial.print(expected, HEX);
  Serial.print(" actual=0x");
  Serial.print(actual, HEX);
  const bool pass = actual == expected;
  Serial.println(pass ? " PASS" : " FAIL");
  if (!pass) {
    char detail[40];
    snprintf(detail, sizeof(detail), "expected=0x%02X actual=0x%02X", expected, actual);
    noteFailure(step, detail);
  }
}

void expectLevel(const char* step, const char* name, uint8_t pin, bool high) {
  delay(1);
  const bool actual = readHigh(pin);
  Serial.print(step);
  Serial.print(" ");
  Serial.print(name);
  Serial.print(high ? " expected=1" : " expected=0");
  Serial.print(actual ? " actual=1" : " actual=0");
  const bool pass = actual == high;
  Serial.println(pass ? " PASS" : " FAIL");
  if (!pass) {
    char detail[40];
    snprintf(detail, sizeof(detail), "%s expected=%d actual=%d", name, high ? 1 : 0, actual ? 1 : 0);
    noteFailure(step, detail);
  }
}

bool pinFollowsPrecharge(uint8_t pin, bool prechargeHigh) {
  pinMode(pin, OUTPUT);
  digitalWrite(pin, prechargeHigh ? HIGH : LOW);
  delayMicroseconds(20);
  pinMode(pin, INPUT);
  delayMicroseconds(5);
  return digitalRead(pin) == (prechargeHigh ? HIGH : LOW);
}

bool pinIsReleased(uint8_t pin) {
  return pinFollowsPrecharge(pin, false) && pinFollowsPrecharge(pin, true);
}

bool pinIsDriven(uint8_t pin, bool expectHigh) {
  return !pinFollowsPrecharge(pin, !expectHigh);
}

void expectReleased(const char* step) {
  delay(1);
  int stuck = -1;
  for (uint8_t stage = 0; stage < 8; stage++) {
    if (!pinIsReleased(PIN_QP[stage])) {
      stuck = stage;
      break;
    }
  }
  Serial.print(step);
  Serial.println(stuck < 0 ? " released PASS" : " released FAIL");
  if (stuck >= 0) {
    char detail[24];
    snprintf(detail, sizeof(detail), "stuckQP%d", stuck);
    noteFailure(step, detail);
  }
}

void expectDriven(const char* step, const char* name, uint8_t pin, bool high) {
  delay(1);
  const bool actual = readHigh(pin);
  const bool driven = pinIsDriven(pin, high);
  Serial.print(step);
  Serial.print(" ");
  Serial.print(name);
  Serial.print(driven ? " driven" : " floating");
  const bool pass = actual == high && driven;
  Serial.println(pass ? " PASS" : " FAIL");
  if (!pass) {
    char detail[48];
    snprintf(detail, sizeof(detail), "%s expected=%d driven=%d", name, high ? 1 : 0, driven ? 1 : 0);
    noteFailure(step, detail);
  }
}

void setStrobe(bool high) {
  digitalWrite(PIN_STR, high ? HIGH : LOW);
  strobeHigh = high;
  if (high) storeReg = shiftReg;
  settle();
}

void setOutputEnable(bool high) {
  digitalWrite(PIN_OE, high ? HIGH : LOW);
  outputEnabled = high;
  settle();
}

void riseClock(bool data) {
  digitalWrite(PIN_D, data ? HIGH : LOW);
  delayMicroseconds(2);
  digitalWrite(PIN_CP, HIGH);
  settle();
  shiftReg = (uint8_t)((shiftReg << 1) | (data ? 1 : 0));
  if (strobeHigh) storeReg = shiftReg;
}

void fallClock() {
  digitalWrite(PIN_CP, LOW);
  settle();
  qs2High = (shiftReg & 0x80) != 0;
}

void clockBit(bool data, const char* step) {
  const bool qs2Before = qs2High;
  riseClock(data);
  expectLevel(step, "QS1", PIN_QS1, (shiftReg & 0x80) != 0);
  expectLevel(step, "QS2", PIN_QS2, qs2Before);
  if (outputEnabled) expectParallel(step, storeReg);
  fallClock();
  expectLevel(step, "QS2", PIN_QS2, qs2High);
}

void shiftByte(uint8_t bits, const char* step) {
  for (int8_t bit = 7; bit >= 0; bit--) {
    clockBit((bits & (1u << bit)) != 0, step);
  }
}

void flushZeros() {
  setStrobe(true);
  setOutputEnable(true);
  digitalWrite(PIN_D, LOW);
  digitalWrite(PIN_CP, LOW);
  shiftReg = 0;
  storeReg = 0;
  qs2High = false;
  for (uint8_t step = 0; step < 8; step++) {
    riseClock(false);
    fallClock();
  }
  expectParallel("flush", 0x00);
  expectLevel("flush", "QS1", PIN_QS1, false);
  expectLevel("flush", "QS2", PIN_QS2, false);
}

void runChecks() {
  flushZeros();

  // One 1, then zeros. It reaches QS1 on the eighth rising edge and QS2 on the fall.
  clockBit(true, "walk");
  for (uint8_t step = 0; step < 6; step++) clockBit(false, "walk");
  const bool qs2Before = qs2High;
  riseClock(false);
  expectLevel("qs2-high", "QS1", PIN_QS1, true);
  expectLevel("qs2-high", "QS2", PIN_QS2, qs2Before);
  expectParallel("qs2-high", 0x80);
  fallClock();
  expectLevel("qs2-fall", "QS2", PIN_QS2, true);
  expectParallel("qs2-fall", 0x80);

  shiftByte(0xA5, "load");
  expectParallel("load", 0xA5);
  const uint8_t held = storeReg;
  setStrobe(false);
  shiftByte(0x5A, "hold");
  expectParallel("hold", held);
  expectLevel("hold", "QS1", PIN_QS1, false);

  setStrobe(true);
  expectParallel("capture", shiftReg);
  const uint8_t captured = storeReg;
  setStrobe(false);
  clockBit(true, "held-after-capture");
  expectParallel("held-after-capture", captured);

  setOutputEnable(false);
  expectReleased("oe-low");
  expectDriven("oe-low", "QS1", PIN_QS1, (shiftReg & 0x80) != 0);
  expectDriven("oe-low", "QS2", PIN_QS2, qs2High);
  setOutputEnable(true);
  expectParallel("oe-high", storeReg);
}

void setup() {
  Serial.begin(115200);
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_STR, HIGH);
  digitalWrite(PIN_D, LOW);
  digitalWrite(PIN_OE, HIGH);
  pinMode(PIN_CP, OUTPUT);
  pinMode(PIN_STR, OUTPUT);
  pinMode(PIN_D, OUTPUT);
  pinMode(PIN_OE, OUTPUT);
  pinMode(PIN_QS1, INPUT);
  pinMode(PIN_QS2, INPUT);
  for (uint8_t stage = 0; stage < 8; stage++) pinMode(PIN_QP[stage], INPUT);
  Serial.println("READY 74HC4094, send a character");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  failed = false;
  shiftReg = 0;
  storeReg = 0;
  qs2High = false;
  strobeHigh = true;
  outputEnabled = true;
  runChecks();
  Serial.println(failed ? resultLine : "RESULT PASS");
}
