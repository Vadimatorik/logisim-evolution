/*
 * Self-check for a 74HC251 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * S2 S1 S0 select one of I0..I7. A low nOE drives that bit onto Y and its
 * complement onto nY. A high nOE releases both outputs. Y and nY each have a
 * 100k pulldown, so a floating pin reads low until the internal pull-up is
 * enabled. nOE stays high until the pins are set.
 */

const uint8_t PIN_nOE = 8;
const uint8_t PIN_Y = 6;
const uint8_t PIN_nY = 7;
const uint8_t PIN_S0 = 11;
const uint8_t PIN_S1 = 10;
const uint8_t PIN_S2 = 9;
const uint8_t PIN_I[8] = {5, 4, 3, 2, A1, A0, 13, 12};

const uint8_t LEVEL_LOW = 0;
const uint8_t LEVEL_HIGH = 1;
const uint8_t LEVEL_Z = 2;

bool failed = false;
char resultLine[96];
bool outputsOn = false;
uint8_t presented = 0;
uint8_t address = 0;

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

void expect(const char* step, uint8_t pin, const char* name, uint8_t expected) {
  const uint8_t actual = sense(pin);
  if (actual == expected) return;
  char detail[48];
  snprintf(detail, sizeof(detail), "%s expected %u got %u", name, expected, actual);
  noteFailure(step, detail);
}

void writeData(uint8_t value) {
  presented = value;
  for (uint8_t bit = 0; bit < 8; bit++) {
    digitalWrite(PIN_I[bit], (value >> bit) & 1 ? HIGH : LOW);
  }
  settle();
}

void writeAddress(uint8_t channel) {
  address = channel & 7;
  digitalWrite(PIN_S0, address & 1 ? HIGH : LOW);
  digitalWrite(PIN_S1, address & 2 ? HIGH : LOW);
  digitalWrite(PIN_S2, address & 4 ? HIGH : LOW);
  settle();
}

void setOutputEnable(bool enabled) {
  outputsOn = enabled;
  digitalWrite(PIN_nOE, enabled ? LOW : HIGH);
  settle();
}

void checkEnabled(const char* step) {
  if (failed || !outputsOn) return;
  const uint8_t bit = (presented >> address) & 1;
  expect(step, PIN_Y, "Y", bit ? LEVEL_HIGH : LEVEL_LOW);
  if (failed) return;
  expect(step, PIN_nY, "nY", bit ? LEVEL_LOW : LEVEL_HIGH);
}

void checkReleased(const char* step) {
  if (failed || outputsOn) return;
  expect(step, PIN_Y, "Y", LEVEL_Z);
  if (failed) return;
  expect(step, PIN_nY, "nY", LEVEL_Z);
}

void checkPattern(uint8_t value, const char* label) {
  writeData(value);
  for (uint8_t channel = 0; channel < 8; channel++) {
    char step[24];
    snprintf(step, sizeof(step), "%s-%u", label, channel);
    writeAddress(channel);
    checkEnabled(step);
  }
}

void runChecks() {
  setOutputEnable(true);
  checkPattern(0x00, "d00");
  checkPattern(0xFF, "dff");
  checkPattern(0x55, "d55");
  checkPattern(0xAA, "daa");
  for (uint8_t bit = 0; bit < 8; bit++) {
    char label[12];
    snprintf(label, sizeof(label), "walk%u", bit);
    checkPattern(static_cast<uint8_t>(1u << bit), label);
  }

  setOutputEnable(false);
  writeData(0x00);
  writeAddress(0);
  checkReleased("z-00");
  writeData(0xFF);
  writeAddress(7);
  checkReleased("z-ff");

  setOutputEnable(true);
  checkEnabled("oe-after-z");
}

void setup() {
  digitalWrite(PIN_nOE, HIGH);
  pinMode(PIN_nOE, OUTPUT);
  digitalWrite(PIN_S0, LOW);
  digitalWrite(PIN_S1, LOW);
  digitalWrite(PIN_S2, LOW);
  pinMode(PIN_S0, OUTPUT);
  pinMode(PIN_S1, OUTPUT);
  pinMode(PIN_S2, OUTPUT);
  for (uint8_t bit = 0; bit < 8; bit++) {
    digitalWrite(PIN_I[bit], LOW);
    pinMode(PIN_I[bit], OUTPUT);
  }
  pinMode(PIN_Y, INPUT);
  pinMode(PIN_nY, INPUT);

  Serial.begin(115200);
  Serial.println("Send any character to test the 74HC251");
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
