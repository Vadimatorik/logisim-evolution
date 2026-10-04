/*
 * Self-check for an SN74LS598N wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * There is no 74HC598. This sketch expects the TI LS598: SRLOAD copies the
 * latch while it is low, including while SRCK is high. G is active-low output
 * enable. QH' stays driven when the parallel pins are released.
 * D0 and D1 stay on Serial. The bus is an input unless the sketch is writing
 * a parallel word, and G is high before those pins become outputs.
 */

const uint8_t BUS[8] = {2, 3, 4, 5, 6, 7, 8, 9};
const uint8_t PIN_SRLOAD = 10;
const uint8_t PIN_QH = 11;
const uint8_t PIN_SRCLR = 12;
const uint8_t PIN_SRCK = 13;
const uint8_t PIN_SRCKEN = A0;
const uint8_t PIN_RCK = A1;
const uint8_t PIN_OE = A2;
const uint8_t PIN_SER1 = A3;
const uint8_t PIN_SER0 = A4;
const uint8_t PIN_DS = A5;

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

void busInput() {
  for (uint8_t index = 0; index < 8; index++) pinMode(BUS[index], INPUT);
}

void busPullup() {
  for (uint8_t index = 0; index < 8; index++) pinMode(BUS[index], INPUT_PULLUP);
}

void writeBus(uint8_t value) {
  for (uint8_t index = 0; index < 8; index++) {
    pinMode(BUS[index], OUTPUT);
    digitalWrite(BUS[index], (value & (1 << index)) ? HIGH : LOW);
  }
}

uint8_t readBus() {
  uint8_t value = 0;
  for (uint8_t index = 0; index < 8; index++) {
    if (digitalRead(BUS[index])) value |= (1 << index);
  }
  return value;
}

void expectBus(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readBus();
  if (actual != expected) noteFailure(step, expected, actual);
}

void expectQh(bool high, const char* step) {
  settle();
  const uint8_t actual = digitalRead(PIN_QH) ? 1 : 0;
  const uint8_t expected = high ? 1 : 0;
  if (actual != expected) noteFailure(step, expected, actual);
}

void pulse(uint8_t pin) {
  digitalWrite(pin, LOW);
  settle();
  digitalWrite(pin, HIGH);
  settle();
  digitalWrite(pin, LOW);
  settle();
}

void showOutputs() {
  busInput();
  digitalWrite(PIN_OE, LOW);
}

void latch(uint8_t value) {
  digitalWrite(PIN_OE, HIGH);
  digitalWrite(PIN_SRLOAD, HIGH);
  writeBus(value);
  pulse(PIN_RCK);
  busInput();
}

void load() {
  showOutputs();
  digitalWrite(PIN_SRLOAD, LOW);
  settle();
  digitalWrite(PIN_SRLOAD, HIGH);
  settle();
}

void shiftIn(bool selectSer1, bool bit) {
  showOutputs();
  digitalWrite(PIN_SRCKEN, LOW);
  digitalWrite(PIN_DS, selectSer1 ? HIGH : LOW);
  digitalWrite(PIN_SER0, bit && !selectSer1 ? HIGH : LOW);
  digitalWrite(PIN_SER1, bit && selectSer1 ? HIGH : LOW);
  pulse(PIN_SRCK);
}

void idle() {
  digitalWrite(PIN_SRCLR, LOW);
  digitalWrite(PIN_SRLOAD, HIGH);
  digitalWrite(PIN_SRCK, LOW);
  digitalWrite(PIN_SRCKEN, HIGH);
  digitalWrite(PIN_RCK, LOW);
  digitalWrite(PIN_OE, HIGH);
  digitalWrite(PIN_DS, LOW);
  digitalWrite(PIN_SER0, LOW);
  digitalWrite(PIN_SER1, LOW);
  busInput();
}

void runChecks() {
  idle();
  settle();
  showOutputs();
  expectBus(0x00, "clear");
  expectQh(false, "clear qh");

  digitalWrite(PIN_SRCLR, HIGH);
  latch(0xA5);
  load();
  expectBus(0xA5, "load");
  expectQh(true, "load qh");

  digitalWrite(PIN_SRCLR, LOW);
  settle();
  expectBus(0x00, "clear shift");
  expectQh(false, "clear shift qh");
  digitalWrite(PIN_SRCLR, HIGH);
  settle();
  load();
  expectBus(0xA5, "latch kept");

  digitalWrite(PIN_SRCK, HIGH);
  settle();
  latch(0x3C);
  digitalWrite(PIN_SRLOAD, LOW);
  showOutputs();
  settle();
  expectBus(0x3C, "load while srck high");
  digitalWrite(PIN_SRLOAD, HIGH);
  digitalWrite(PIN_SRCK, LOW);

  latch(0x01);
  load();
  shiftIn(false, false);
  expectBus(0x02, "shift ser0");
  expectQh(false, "shift ser0 qh");
  shiftIn(true, true);
  expectBus(0x05, "shift ser1");

  const uint8_t held = readBus();
  digitalWrite(PIN_SRCKEN, HIGH);
  digitalWrite(PIN_SER0, HIGH);
  pulse(PIN_SRCK);
  expectBus(held, "srcken high");

  digitalWrite(PIN_SRCK, HIGH);
  settle();
  expectBus(held, "srck high");
  digitalWrite(PIN_SRCK, LOW);
  settle();
  expectBus(held, "srck fall");

  digitalWrite(PIN_OE, HIGH);
  writeBus(0x0F);
  digitalWrite(PIN_RCK, HIGH);
  settle();
  writeBus(0xF0);
  digitalWrite(PIN_RCK, LOW);
  settle();
  busInput();
  load();
  expectBus(0x0F, "rck fall");

  latch(0x80);
  load();
  expectQh(true, "z setup qh");
  digitalWrite(PIN_OE, HIGH);
  busPullup();
  expectBus(0xFF, "parallel z");
  expectQh(true, "qh while z");
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_SRLOAD, OUTPUT);
  pinMode(PIN_QH, INPUT);
  pinMode(PIN_SRCLR, OUTPUT);
  pinMode(PIN_SRCK, OUTPUT);
  pinMode(PIN_SRCKEN, OUTPUT);
  pinMode(PIN_RCK, OUTPUT);
  pinMode(PIN_OE, OUTPUT);
  pinMode(PIN_SER1, OUTPUT);
  pinMode(PIN_SER0, OUTPUT);
  pinMode(PIN_DS, OUTPUT);
  idle();
  Serial.println("SN74LS598 ready, send a character to start");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  if (!failed) {
    Serial.println("RESULT PASS");
  } else {
    Serial.println(resultLine);
  }
  idle();
}
