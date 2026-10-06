/*
 * Self-check for a 74HC643 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * A low nOE and a high DIR make B the complement of A. A low nOE and a low DIR
 * copy B onto A without inversion. A high nOE releases both buses. Bit 0 is
 * Renesas A1/B1, which is Philips A0/B0. Each data pin has a 100k pulldown, so a
 * floating pin reads low until the internal pull-up is enabled. nOE is raised
 * before the pin modes change, so the Nano and the chip never drive the same line.
 */

const uint8_t PIN_DIR = 2;
const uint8_t PIN_nOE = 3;
const uint8_t PIN_A[8] = {4, 5, 6, 7, 8, 9, 10, 11};
const uint8_t PIN_B[8] = {A5, A4, A3, A2, A1, A0, 13, 12};

const uint8_t LEVEL_LOW = 0;
const uint8_t LEVEL_HIGH = 1;
const uint8_t LEVEL_Z = 2;

bool failed = false;
char resultLine[96];
bool fromA = true;
bool outputsOn = false;
uint8_t drivenWord = 0;

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
  const uint8_t* pins = fromA ? PIN_B : PIN_A;
  const char* name = fromA ? "B" : "A";
  const uint8_t expectedWord = fromA ? static_cast<uint8_t>(~drivenWord) : drivenWord;
  for (uint8_t bit = 0; bit < 8; bit++) {
    const uint8_t actual = sense(pins[bit]);
    const uint8_t expected = outputsOn ? ((expectedWord >> bit) & 1) : LEVEL_Z;
    if (actual == expected) continue;
    char detail[48];
    snprintf(detail, sizeof(detail), "%s%u expected %u got %u", name, bit + 1, expected, actual);
    noteFailure(step, detail);
    return;
  }
}

void writeDriven(uint8_t value) {
  drivenWord = value;
  const uint8_t* pins = fromA ? PIN_A : PIN_B;
  for (uint8_t bit = 0; bit < 8; bit++) {
    digitalWrite(pins[bit], (value >> bit) & 1 ? HIGH : LOW);
  }
  settle();
}

void setDirection(bool aToB) {
  digitalWrite(PIN_nOE, HIGH);
  outputsOn = false;
  settle();
  fromA = aToB;
  digitalWrite(PIN_DIR, aToB ? HIGH : LOW);
  const uint8_t* releasePins = aToB ? PIN_B : PIN_A;
  const uint8_t* drivePins = aToB ? PIN_A : PIN_B;
  for (uint8_t bit = 0; bit < 8; bit++) {
    pinMode(releasePins[bit], INPUT);
  }
  for (uint8_t bit = 0; bit < 8; bit++) {
    pinMode(drivePins[bit], OUTPUT);
    digitalWrite(drivePins[bit], (drivenWord >> bit) & 1 ? HIGH : LOW);
  }
  settle();
}

void setOutputs(bool enabled, const char* step) {
  outputsOn = enabled;
  digitalWrite(PIN_nOE, enabled ? LOW : HIGH);
  settle();
  check(step);
}

void writeAndCheck(uint8_t value, const char* step) {
  writeDriven(value);
  check(step);
}

void transmit(bool aToB, const char* prefix) {
  setDirection(aToB);
  char label[32];
  snprintf(label, sizeof(label), "%s-enable", prefix);
  setOutputs(true, label);
  snprintf(label, sizeof(label), "%s-00", prefix);
  writeAndCheck(0x00, label);
  snprintf(label, sizeof(label), "%s-ff", prefix);
  writeAndCheck(0xFF, label);
  snprintf(label, sizeof(label), "%s-55", prefix);
  writeAndCheck(0x55, label);
  snprintf(label, sizeof(label), "%s-aa", prefix);
  writeAndCheck(0xAA, label);
  for (uint8_t bit = 0; bit < 8; bit++) {
    snprintf(label, sizeof(label), "%s-walk-%u", prefix, bit + 1);
    writeAndCheck(static_cast<uint8_t>(1u << bit), label);
  }
}

void isolate(bool aToB, const char* prefix) {
  setDirection(aToB);
  char label[32];
  snprintf(label, sizeof(label), "%s-00", prefix);
  writeAndCheck(0x00, label);
  snprintf(label, sizeof(label), "%s-ff", prefix);
  writeAndCheck(0xFF, label);
}

void runChecks() {
  transmit(true, "a-to-b");
  transmit(false, "b-to-a");
  isolate(true, "isolate-b");
  isolate(false, "isolate-a");
}

void setup() {
  pinMode(PIN_DIR, OUTPUT);
  pinMode(PIN_nOE, OUTPUT);
  digitalWrite(PIN_DIR, HIGH);
  digitalWrite(PIN_nOE, HIGH);
  for (uint8_t bit = 0; bit < 8; bit++) {
    pinMode(PIN_A[bit], INPUT);
    pinMode(PIN_B[bit], INPUT);
  }

  Serial.begin(115200);
  Serial.println("Send any character to test the 74HC643");
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
