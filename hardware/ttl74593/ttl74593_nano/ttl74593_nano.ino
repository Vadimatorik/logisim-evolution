/*
 * Self-check for a 74HC593 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * RCK rising with RCKEN low stores the bus in the input register. CLOAD low
 * copies that register into the counter. CCLR low clears only the counter and
 * wins if CLOAD is also low. CCK rising counts when CCKEN is high or CCKEN-bar
 * is low. The bus is driven only when G is high and G-bar is low. RCO is low
 * only at 0xFF. Each bus pin has a 100k pulldown, so a floating pin reads low
 * until the internal pull-up is enabled. Outputs are turned off before the Nano
 * drives the bus.
 */

const uint8_t PIN_NG = 2;
const uint8_t PIN_G = 3;
const uint8_t PIN_BUS[8] = {4, 5, 6, 7, 8, 9, 10, 11};
const uint8_t PIN_CLOAD = 12;
const uint8_t PIN_RCO = 13;
const uint8_t PIN_CCLR = A0;
const uint8_t PIN_CCK = A1;
const uint8_t PIN_NCCKEN = A2;
const uint8_t PIN_CCKEN = A3;
const uint8_t PIN_RCK = A4;
const uint8_t PIN_NRCKEN = A5;

const uint8_t LEVEL_LOW = 0;
const uint8_t LEVEL_HIGH = 1;
const uint8_t LEVEL_Z = 2;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, const char* detail) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s %s", step, detail);
}

void settle() { delay(1); }

void releaseBus() {
  for (uint8_t bit = 0; bit < 8; bit++) pinMode(PIN_BUS[bit], INPUT);
}

void outputsOff() {
  digitalWrite(PIN_G, LOW);
  digitalWrite(PIN_NG, HIGH);
  settle();
  releaseBus();
}

void driveBus(uint8_t value) {
  outputsOff();
  for (uint8_t bit = 0; bit < 8; bit++) {
    pinMode(PIN_BUS[bit], OUTPUT);
    digitalWrite(PIN_BUS[bit], (value >> bit) & 1 ? HIGH : LOW);
  }
  settle();
}

uint8_t sense(uint8_t pin) {
  pinMode(pin, INPUT);
  delay(1);
  const bool released = digitalRead(pin);
  pinMode(pin, INPUT_PULLUP);
  delay(1);
  const bool pulled = digitalRead(pin);
  pinMode(pin, INPUT);
  if (!released && !pulled) return LEVEL_LOW;
  if (released && pulled) return LEVEL_HIGH;
  if (!released && pulled) return LEVEL_Z;
  return 255;
}

void expectBus(uint8_t value, const char* step) {
  for (uint8_t bit = 0; bit < 8; bit++) {
    const uint8_t actual = sense(PIN_BUS[bit]);
    const uint8_t expected = (value >> bit) & 1;
    if (actual == expected) continue;
    char detail[40];
    snprintf(detail, sizeof(detail), "bit %u expected %u got %u", bit, expected, actual);
    noteFailure(step, detail);
    return;
  }
}

void expectZ(const char* step) {
  for (uint8_t bit = 0; bit < 8; bit++) {
    const uint8_t actual = sense(PIN_BUS[bit]);
    if (actual == LEVEL_Z) continue;
    char detail[40];
    snprintf(detail, sizeof(detail), "bit %u expected Z got %u", bit, actual);
    noteFailure(step, detail);
    return;
  }
}

void expectRco(bool active, const char* step) {
  const uint8_t actual = digitalRead(PIN_RCO) ? LEVEL_HIGH : LEVEL_LOW;
  const uint8_t expected = active ? LEVEL_LOW : LEVEL_HIGH;
  if (actual == expected) return;
  char detail[32];
  snprintf(detail, sizeof(detail), "rco expected %u got %u", expected, actual);
  noteFailure(step, detail);
}

void expectCount(uint8_t value, const char* step) {
  releaseBus();
  digitalWrite(PIN_NG, LOW);
  digitalWrite(PIN_G, HIGH);
  settle();
  expectBus(value, step);
  expectRco(value == 0xFF, step);
  outputsOff();
}

void pulse(uint8_t pin) {
  digitalWrite(pin, LOW);
  settle();
  digitalWrite(pin, HIGH);
  settle();
  digitalWrite(pin, LOW);
  settle();
}

void pulseLow(uint8_t pin) {
  digitalWrite(pin, LOW);
  settle();
  digitalWrite(pin, HIGH);
  settle();
}

void loadRegister(uint8_t value) {
  driveBus(value);
  digitalWrite(PIN_NRCKEN, LOW);
  settle();
  pulse(PIN_RCK);
  digitalWrite(PIN_NRCKEN, HIGH);
  releaseBus();
}

void loadCounter() { pulseLow(PIN_CLOAD); }

void clearCounter() { pulseLow(PIN_CCLR); }

void armCount(bool ccken, bool nccken) {
  digitalWrite(PIN_CCKEN, ccken ? HIGH : LOW);
  digitalWrite(PIN_NCCKEN, nccken ? HIGH : LOW);
  settle();
}

void checkClearKeepsTheRegister() {
  loadRegister(0x5A);
  loadCounter();
  expectCount(0x5A, "loaded");
  clearCounter();
  expectCount(0x00, "cleared");
  loadCounter();
  expectCount(0x5A, "register-kept");
}

void checkClearWinsOverLoad() {
  loadRegister(0xA5);
  digitalWrite(PIN_CLOAD, LOW);
  digitalWrite(PIN_CCLR, LOW);
  settle();
  expectCount(0x00, "clear-over-load");
  digitalWrite(PIN_CLOAD, HIGH);
  digitalWrite(PIN_CCLR, HIGH);
  settle();
}

void checkLoadUsesTheRegister() {
  loadRegister(0xA5);
  loadCounter();
  driveBus(0x5A);
  loadCounter();
  releaseBus();
  expectCount(0xA5, "load-register");
}

void checkRegisterIgnoresFallingClockAndHighEnable() {
  driveBus(0x11);
  digitalWrite(PIN_NRCKEN, LOW);
  digitalWrite(PIN_RCK, LOW);
  settle();
  digitalWrite(PIN_RCK, HIGH);
  settle();
  driveBus(0x22);
  digitalWrite(PIN_RCK, LOW);
  settle();
  digitalWrite(PIN_NRCKEN, HIGH);
  releaseBus();
  loadCounter();
  expectCount(0x11, "rck-fall");

  driveBus(0xC3);
  digitalWrite(PIN_NRCKEN, HIGH);
  pulse(PIN_RCK);
  releaseBus();
  loadCounter();
  expectCount(0x11, "rcken-high");
}

void checkCountAndCarry() {
  loadRegister(0x12);
  loadCounter();
  armCount(false, true);
  pulse(PIN_CCK);
  expectCount(0x12, "count-hold");

  armCount(false, false);
  pulse(PIN_CCK);
  expectCount(0x13, "count-nccken");

  armCount(true, true);
  pulse(PIN_CCK);
  expectCount(0x14, "count-ccken");

  armCount(true, false);
  pulse(PIN_CCK);
  expectCount(0x15, "count-either");

  armCount(false, true);
  loadRegister(0xFE);
  loadCounter();
  armCount(true, true);
  pulse(PIN_CCK);
  expectCount(0xFF, "count-ff");
  pulse(PIN_CCK);
  expectCount(0x00, "count-wrap");
  armCount(false, true);
}

void checkOutputEnable() {
  clearCounter();
  outputsOff();
  digitalWrite(PIN_G, LOW);
  digitalWrite(PIN_NG, LOW);
  settle();
  expectZ("oe-low-low");
  digitalWrite(PIN_G, LOW);
  digitalWrite(PIN_NG, HIGH);
  settle();
  expectZ("oe-low-high");
  digitalWrite(PIN_G, HIGH);
  digitalWrite(PIN_NG, HIGH);
  settle();
  expectZ("oe-high-high");
  expectCount(0x00, "oe-drive-zero");

  loadRegister(0xFF);
  loadCounter();
  expectCount(0xFF, "oe-drive-ff");
  outputsOff();
  expectZ("oe-off-at-ff");
  expectRco(true, "rco-while-z");
}

void runChecks() {
  checkClearKeepsTheRegister();
  checkClearWinsOverLoad();
  checkLoadUsesTheRegister();
  checkRegisterIgnoresFallingClockAndHighEnable();
  checkCountAndCarry();
  checkOutputEnable();
  outputsOff();
  armCount(false, true);
}

void setup() {
  pinMode(PIN_NG, OUTPUT);
  pinMode(PIN_G, OUTPUT);
  pinMode(PIN_CLOAD, OUTPUT);
  pinMode(PIN_CCLR, OUTPUT);
  pinMode(PIN_CCK, OUTPUT);
  pinMode(PIN_NCCKEN, OUTPUT);
  pinMode(PIN_CCKEN, OUTPUT);
  pinMode(PIN_RCK, OUTPUT);
  pinMode(PIN_NRCKEN, OUTPUT);
  pinMode(PIN_RCO, INPUT);
  digitalWrite(PIN_G, LOW);
  digitalWrite(PIN_NG, HIGH);
  digitalWrite(PIN_CLOAD, HIGH);
  digitalWrite(PIN_CCLR, HIGH);
  digitalWrite(PIN_CCK, LOW);
  digitalWrite(PIN_NCCKEN, HIGH);
  digitalWrite(PIN_CCKEN, LOW);
  digitalWrite(PIN_RCK, LOW);
  digitalWrite(PIN_NRCKEN, HIGH);
  releaseBus();

  Serial.begin(115200);
  Serial.println("Send any character to test the 74HC593");
  while (Serial.available() == 0) {
  }
  while (Serial.available() > 0) Serial.read();
  runChecks();
  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
