/*
 * Self-check for a 74HC573 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * While LE is high the outputs follow D. A low LE holds that word. A high nOE
 * releases every Q (high impedance) and does not clear the latch. Bit 0 is
 * Nexperia D0/Q0, which is TI 1D/1Q. Each Q has a 100k pulldown, so a floating
 * pin reads low until the internal pull-up is enabled.
 */

const uint8_t PIN_nOE = 2;
const uint8_t PIN_D[8] = {3, 4, 5, 6, 7, 8, 9, 10};
const uint8_t PIN_LE = 11;
const uint8_t PIN_Q[8] = {13, 12, A0, A1, A2, A3, A4, A5};

const uint8_t LEVEL_LOW = 0;
const uint8_t LEVEL_HIGH = 1;
const uint8_t LEVEL_Z = 2;

bool failed = false;
char resultLine[96];
bool latchOpen = false;
bool outputsOn = false;
uint8_t dataWord = 0;
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
    const uint8_t actual = sense(PIN_Q[bit]);
    const uint8_t expected = outputsOn ? ((stored >> bit) & 1) : LEVEL_Z;
    if (actual == expected) continue;
    char detail[48];
    snprintf(detail, sizeof(detail), "Q%u expected %u got %u", bit, expected, actual);
    noteFailure(step, detail);
    return;
  }
}

void writeData(uint8_t value, const char* step) {
  dataWord = value;
  for (uint8_t bit = 0; bit < 8; bit++) {
    digitalWrite(PIN_D[bit], (value >> bit) & 1 ? HIGH : LOW);
  }
  settle();
  if (latchOpen) stored = dataWord;
  check(step);
}

void setLatch(bool open, const char* step) {
  latchOpen = open;
  digitalWrite(PIN_LE, open ? HIGH : LOW);
  settle();
  if (open) stored = dataWord;
  check(step);
}

void setOutputs(bool enabled, const char* step) {
  outputsOn = enabled;
  digitalWrite(PIN_nOE, enabled ? LOW : HIGH);
  settle();
  check(step);
}

void runChecks() {
  setOutputs(false, "outputs-off");
  setLatch(true, "open");
  writeData(0x00, "transparent-00");
  setOutputs(true, "outputs-on");

  writeData(0xFF, "transparent-ff");
  writeData(0x55, "transparent-55");
  writeData(0xAA, "transparent-aa");
  for (uint8_t bit = 0; bit < 8; bit++) {
    char label[24];
    snprintf(label, sizeof(label), "walk-%u", bit);
    writeData(static_cast<uint8_t>(1u << bit), label);
  }

  setLatch(false, "close");
  writeData(0x00, "hold-00");
  writeData(0xFF, "hold-ff");

  setOutputs(false, "hide");
  setLatch(true, "rewrite-hidden");
  writeData(0x0F, "load-hidden");
  setLatch(false, "close-hidden");
  setOutputs(true, "reveal");
}

void setup() {
  pinMode(PIN_nOE, OUTPUT);
  pinMode(PIN_LE, OUTPUT);
  digitalWrite(PIN_nOE, HIGH);
  digitalWrite(PIN_LE, LOW);
  for (uint8_t bit = 0; bit < 8; bit++) {
    pinMode(PIN_D[bit], OUTPUT);
    digitalWrite(PIN_D[bit], LOW);
    pinMode(PIN_Q[bit], INPUT);
  }

  Serial.begin(115200);
  Serial.println("Send any character to test the 74HC573");
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
