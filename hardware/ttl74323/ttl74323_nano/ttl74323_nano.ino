/*
 * Self-check for a 74HC323 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * CLR is active low and is sampled on the rising edge of CP. It overrides
 * load and shift. S1 S0 select hold, shift right, shift left and load.
 * The I/O bus is high-Z when either OE is high or both mode selects are high.
 * Q0 and Q7 stay push-pull. Nano drives the bus only while the chip is loading.
 * CLR and CP stay low, both OE stay high and the bus stays an input until the
 * check starts. The first clock then clears the register.
 */

const uint8_t PIN_S0 = 2;
const uint8_t PIN_OE1 = 3;
const uint8_t PIN_OE2 = 4;
const uint8_t PIN_CLR = 5;
const uint8_t PIN_SR = 6;
const uint8_t PIN_CP = 7;
const uint8_t PIN_SL = 8;
const uint8_t PIN_S1 = 9;
const uint8_t PIN_Q0 = 10;
const uint8_t PIN_Q7 = 11;
const uint8_t IO_PINS[8] = {12, 13, A0, A1, A2, A3, A4, A5};

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

void busRelease() {
  for (uint8_t bit = 0; bit < 8; bit++) pinMode(IO_PINS[bit], INPUT);
}

void busPullup() {
  for (uint8_t bit = 0; bit < 8; bit++) pinMode(IO_PINS[bit], INPUT_PULLUP);
}

void busDrive(uint8_t value) {
  for (uint8_t bit = 0; bit < 8; bit++) {
    pinMode(IO_PINS[bit], OUTPUT);
    digitalWrite(IO_PINS[bit], (value & (1 << bit)) ? HIGH : LOW);
  }
}

void selectMode(bool s1, bool s0) {
  digitalWrite(PIN_S1, s1 ? HIGH : LOW);
  digitalWrite(PIN_S0, s0 ? HIGH : LOW);
}

uint8_t readBus() {
  uint8_t value = 0;
  for (uint8_t bit = 0; bit < 8; bit++) {
    if (digitalRead(IO_PINS[bit])) value |= (uint8_t) (1 << bit);
  }
  return value;
}

uint8_t readSerial() {
  uint8_t value = 0;
  if (digitalRead(PIN_Q0)) value |= 0x01;
  if (digitalRead(PIN_Q7)) value |= 0x80;
  return value;
}

void expectBus(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readBus();
  if (actual != expected) noteFailure(step, expected, actual);
}

void expectSerial(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readSerial();
  if (actual != (expected & 0x81)) noteFailure(step, expected & 0x81, actual);
}

void expectReleased(const char* step) {
  busPullup();
  settle();
  const uint8_t actual = readBus();
  if (actual != 0xFF) noteFailure(step, 0xFF, actual);
  busRelease();
}

void clockPulse() {
  digitalWrite(PIN_CP, LOW);
  settle();
  digitalWrite(PIN_CP, HIGH);
  settle();
  digitalWrite(PIN_CP, LOW);
  settle();
}

void enableBus() {
  busRelease();
  digitalWrite(PIN_OE1, LOW);
  digitalWrite(PIN_OE2, LOW);
  selectMode(false, false);
  settle();
}

void load(uint8_t value) {
  busRelease();
  digitalWrite(PIN_CLR, HIGH);
  digitalWrite(PIN_OE1, LOW);
  digitalWrite(PIN_OE2, LOW);
  selectMode(true, true);
  settle();
  busDrive(value);
  settle();
  clockPulse();
  busRelease();
  selectMode(false, false);
  settle();
}

void checkResetOverridesLoad() {
  load(0x81);
  expectBus(0x81, "loaded before reset");
  digitalWrite(PIN_CLR, LOW);
  settle();
  expectBus(0x81, "reset before clock");
  expectSerial(0x81, "serial before reset");
  clockPulse();
  expectBus(0x00, "sync reset");
  expectSerial(0x00, "serial after reset");

  load(0x81);
  busRelease();
  digitalWrite(PIN_CLR, LOW);
  selectMode(true, true);
  settle();
  expectSerial(0x81, "serial while load waits");
  expectReleased("load releases bus");
  busDrive(0xFF);
  clockPulse();
  busRelease();
  selectMode(false, false);
  digitalWrite(PIN_CLR, HIGH);
  settle();
  expectBus(0x00, "reset overrides load");
}

void checkLoads() {
  const uint8_t codes[] = {0x00, 0xFF, 0xA5, 0x5A};
  for (uint8_t index = 0; index < 4; index++) {
    load(codes[index]);
    expectBus(codes[index], "load");
    expectSerial(codes[index], "serial after load");
  }
}

void checkShiftRight() {
  load(0x01);
  digitalWrite(PIN_CLR, HIGH);
  digitalWrite(PIN_SR, LOW);
  selectMode(false, true);
  for (uint8_t bit = 0; bit < 8; bit++) {
    expectBus((uint8_t) (1 << bit), "shift right");
    clockPulse();
  }
  expectBus(0x00, "shift right off the end");

  load(0x80);
  digitalWrite(PIN_SR, HIGH);
  selectMode(false, true);
  clockPulse();
  expectBus(0x01, "shift right in a one");
}

void checkShiftLeft() {
  load(0x80);
  digitalWrite(PIN_CLR, HIGH);
  digitalWrite(PIN_SL, LOW);
  selectMode(true, false);
  for (uint8_t bit = 0; bit < 8; bit++) {
    expectBus((uint8_t) (0x80 >> bit), "shift left");
    clockPulse();
  }
  expectBus(0x00, "shift left off the end");

  load(0x01);
  digitalWrite(PIN_SL, HIGH);
  selectMode(true, false);
  clockPulse();
  expectBus(0x80, "shift left in a one");
}

void checkHold() {
  load(0xA5);
  digitalWrite(PIN_SR, HIGH);
  digitalWrite(PIN_SL, HIGH);
  selectMode(false, false);
  clockPulse();
  clockPulse();
  clockPulse();
  expectBus(0xA5, "hold");
}

void checkOutputEnable() {
  load(0x80);
  digitalWrite(PIN_OE1, HIGH);
  digitalWrite(PIN_SR, LOW);
  selectMode(false, true);
  expectSerial(0x80, "q7 before disabled shift");
  expectReleased("oe1 releases bus");
  clockPulse();
  expectSerial(0x00, "shifted while oe high");

  digitalWrite(PIN_OE1, LOW);
  enableBus();
  expectBus(0x00, "bus after disabled shift");

  load(0x01);
  digitalWrite(PIN_OE2, HIGH);
  settle();
  expectSerial(0x01, "q while oe2 high");
  expectReleased("oe2 releases bus");

  digitalWrite(PIN_OE2, LOW);
  selectMode(true, true);
  settle();
  expectSerial(0x01, "q while loading");
  expectReleased("load mode releases bus");
  enableBus();
  expectBus(0x01, "bus returns in hold");
}

void setup() {
  pinMode(PIN_S0, OUTPUT);
  pinMode(PIN_OE1, OUTPUT);
  pinMode(PIN_OE2, OUTPUT);
  pinMode(PIN_CLR, OUTPUT);
  pinMode(PIN_SR, OUTPUT);
  pinMode(PIN_CP, OUTPUT);
  pinMode(PIN_SL, OUTPUT);
  pinMode(PIN_S1, OUTPUT);
  pinMode(PIN_Q0, INPUT);
  pinMode(PIN_Q7, INPUT);
  busRelease();

  digitalWrite(PIN_CLR, LOW);
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_OE1, HIGH);
  digitalWrite(PIN_OE2, HIGH);
  digitalWrite(PIN_S0, LOW);
  digitalWrite(PIN_S1, LOW);
  digitalWrite(PIN_SR, LOW);
  digitalWrite(PIN_SL, LOW);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  clockPulse();
  expectSerial(0x00, "initial reset");

  checkResetOverridesLoad();
  checkLoads();
  checkShiftRight();
  checkShiftLeft();
  checkHold();
  checkOutputEnable();

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
