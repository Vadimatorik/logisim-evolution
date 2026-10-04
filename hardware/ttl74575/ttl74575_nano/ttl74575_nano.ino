/*
 * Self-check for an SN74ALS575A wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * A rising CLK edge stores D. A low nCLR on that edge stores zero and wins over D.
 * A low nCLR without a rising edge does not clear. A low nOE drives the stored
 * value onto Q. A high nOE releases Q. Bit 0 is TI 1D/1Q. Each Q pin has a
 * 10k divider to 5V and GND, so analogRead sees about 0V, 3.4V or 2.5V.
 * nOE stays high until the pins are set.
 */

const uint8_t PIN_nCLR = 2;
const uint8_t PIN_nOE = 3;
const uint8_t PIN_D[8] = {4, 5, 6, 7, 8, 9, 10, 11};
const uint8_t PIN_CLK = 12;
const uint8_t PIN_Q[8] = {A7, A6, A5, A4, A3, A2, A1, A0};

const int LOW_MAX = 204;
const int HIGH_MIN = 614;

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

void settle() { delay(2); }

uint8_t sense(uint8_t pin) {
  const int reading = analogRead(pin);
  if (reading <= LOW_MAX) return LEVEL_LOW;
  if (reading >= HIGH_MIN) return LEVEL_HIGH;
  return LEVEL_Z;
}

void check(const char* step) {
  for (uint8_t bit = 0; bit < 8; bit++) {
    const uint8_t actual = sense(PIN_Q[bit]);
    const uint8_t expected = outputsOn ? ((stored >> bit) & 1) : LEVEL_Z;
    if (actual == expected) continue;
    char detail[48];
    snprintf(detail, sizeof(detail), "Q%u expected %u got %u", bit, expected, actual);
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
  digitalWrite(PIN_CLK, LOW);
  settle();
  digitalWrite(PIN_CLK, HIGH);
  settle();
  stored = digitalRead(PIN_nCLR) == LOW ? 0 : presented;
}

void fallingEdge() {
  digitalWrite(PIN_CLK, LOW);
  settle();
}

void setOutputEnable(bool enabled) {
  outputsOn = enabled;
  digitalWrite(PIN_nOE, enabled ? LOW : HIGH);
  settle();
}

void setClear(bool asserted) {
  digitalWrite(PIN_nCLR, asserted ? LOW : HIGH);
  settle();
}

void loadAndCheck(uint8_t value, const char* step) {
  writeData(value);
  risingEdge();
  check(step);
}

void runChecks() {
  setOutputEnable(true);
  loadAndCheck(0xA5, "seed");

  setClear(true);
  writeData(0xFF);
  check("clear-level");
  fallingEdge();
  check("clear-fall");
  risingEdge();
  check("clear-wins");
  setClear(false);
  check("clear-release");

  loadAndCheck(0x00, "load-00");
  loadAndCheck(0xFF, "load-ff");
  loadAndCheck(0xA5, "load-a5");
  loadAndCheck(0x5A, "load-5a");
  for (uint8_t bit = 0; bit < 8; bit++) {
    char label[16];
    snprintf(label, sizeof(label), "walk-%u", bit);
    loadAndCheck(static_cast<uint8_t>(1u << bit), label);
  }

  writeData(0xA5);
  risingEdge();
  check("hold-before");
  writeData(0x5A);
  check("hold-level");
  fallingEdge();
  check("hold-fall");
  risingEdge();
  check("hold-rise");

  setOutputEnable(false);
  check("z-held");
  writeData(0xA5);
  risingEdge();
  check("z-while-clock");
  setOutputEnable(true);
  check("oe-after-hidden-clock");
}

void setup() {
  pinMode(PIN_nCLR, OUTPUT);
  pinMode(PIN_nOE, OUTPUT);
  pinMode(PIN_CLK, OUTPUT);
  digitalWrite(PIN_nCLR, HIGH);
  digitalWrite(PIN_nOE, HIGH);
  digitalWrite(PIN_CLK, LOW);
  for (uint8_t bit = 0; bit < 8; bit++) {
    pinMode(PIN_D[bit], OUTPUT);
    digitalWrite(PIN_D[bit], LOW);
  }

  Serial.begin(115200);
  Serial.println("Send any character to test the SN74ALS575A");
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
