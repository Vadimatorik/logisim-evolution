/*
 * Self-check for a 74HC564 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * A rising CP edge stores D. A low nOE drives the complement onto nQ. A high
 * nOE releases nQ and does not change the register. Bit 0 is nQ0, datasheet Q0.
 * Each nQ pin has a 100k pulldown, so a floating pin reads low until the
 * internal pull-up is enabled. nOE stays high until the pins are set.
 */

const uint8_t PIN_nOE = 2;
const uint8_t PIN_CP = 11;
const uint8_t PIN_D[8] = {3, 4, 5, 6, 7, 8, 9, 10};
const uint8_t PIN_nQ[8] = {13, 12, A5, A4, A3, A2, A1, A0};

const uint8_t LEVEL_LOW = 0;
const uint8_t LEVEL_HIGH = 1;
const uint8_t LEVEL_Z = 2;

bool failed = false;
char resultLine[96];
bool outputsOn = false;
uint8_t presented = 0;
uint8_t stored = 0;

void noteFailure(const char* step, const char* detail) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s %s", step, detail);
}

void settle() { delayMicroseconds(20); }

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

void check(const char* step) {
  for (uint8_t bit = 0; bit < 8; bit++) {
    const uint8_t actual = sense(PIN_nQ[bit]);
    const uint8_t driven = ((stored >> bit) & 1) ^ 1;
    const uint8_t expected = outputsOn ? driven : LEVEL_Z;
    if (actual == expected) continue;
    char detail[48];
    snprintf(detail, sizeof(detail), "nQ%u expected %u got %u", bit, expected, actual);
    noteFailure(step, detail);
    return;
  }
}

void writeData(uint8_t value) {
  presented = value;
  for (uint8_t bit = 0; bit < 8; bit++) {
    digitalWrite(PIN_D[bit], (value >> bit) & 1 ? HIGH : LOW);
  }
  settle();
}

void risingEdge() {
  digitalWrite(PIN_CP, LOW);
  settle();
  digitalWrite(PIN_CP, HIGH);
  settle();
  stored = presented;
}

void fallingEdge() {
  digitalWrite(PIN_CP, LOW);
  settle();
}

void setOutputEnable(bool enabled) {
  outputsOn = enabled;
  digitalWrite(PIN_nOE, enabled ? LOW : HIGH);
  settle();
}

void loadAndCheck(uint8_t value, const char* step) {
  writeData(value);
  risingEdge();
  check(step);
}

void runChecks() {
  setOutputEnable(true);
  loadAndCheck(0x00, "load-00");
  loadAndCheck(0xFF, "load-ff");
  loadAndCheck(0x55, "load-55");
  loadAndCheck(0xAA, "load-aa");
  for (uint8_t bit = 0; bit < 8; bit++) {
    char label[16];
    snprintf(label, sizeof(label), "walk-%u", bit);
    loadAndCheck(static_cast<uint8_t>(1u << bit), label);
  }

  writeData(0x55);
  risingEdge();
  check("hold-before");
  writeData(0xAA);
  check("hold-level");
  fallingEdge();
  check("hold-fall");
  risingEdge();
  check("hold-rise");

  writeData(0x00);
  risingEdge();
  setOutputEnable(true);
  check("oe-00");
  setOutputEnable(false);
  check("z-00");

  writeData(0xFF);
  risingEdge();
  check("z-while-clock");
  setOutputEnable(true);
  check("oe-after-hidden-clock");
  setOutputEnable(false);
  check("z-ff");
}

void setup() {
  pinMode(PIN_nOE, OUTPUT);
  pinMode(PIN_CP, OUTPUT);
  digitalWrite(PIN_nOE, HIGH);
  digitalWrite(PIN_CP, LOW);
  for (uint8_t bit = 0; bit < 8; bit++) {
    pinMode(PIN_D[bit], OUTPUT);
    digitalWrite(PIN_D[bit], LOW);
    pinMode(PIN_nQ[bit], INPUT);
  }

  Serial.begin(115200);
  Serial.println("Send any character to test the 74HC564");
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
