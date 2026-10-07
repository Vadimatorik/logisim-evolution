/*
 * Self-check for a 74HC243 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Both enables low make B a copy of A. Both enables high make A a copy of B.
 * Different levels release both buses. Bit 0 is Philips A0/B0. Each data pin
 * has a 100k pulldown, so a floating pin reads low until the internal pull-up
 * is enabled. Direction changes pass through OEA high and OEB low, and the
 * Nano releases its pins before the enables move.
 */

const uint8_t PIN_OEA = 2;
const uint8_t PIN_OEB = 11;
const uint8_t PIN_A[4] = {3, 4, 5, 6};
const uint8_t PIN_B[4] = {7, 8, 9, 10};

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

void releaseAll() {
  for (uint8_t bit = 0; bit < 4; bit++) {
    pinMode(PIN_A[bit], INPUT);
    pinMode(PIN_B[bit], INPUT);
  }
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

void check(const char* step) {
  const uint8_t* pins = fromA ? PIN_B : PIN_A;
  const char* name = fromA ? "B" : "A";
  for (uint8_t bit = 0; bit < 4; bit++) {
    const uint8_t actual = sense(pins[bit]);
    const uint8_t expected = outputsOn ? ((drivenWord >> bit) & 1) : LEVEL_Z;
    if (actual == expected) continue;
    char detail[48];
    snprintf(detail, sizeof(detail), "%s%u expected %u got %u", name, bit, expected, actual);
    noteFailure(step, detail);
    return;
  }
}

void writeDriven(uint8_t value) {
  drivenWord = value & 0x0F;
  const uint8_t* pins = fromA ? PIN_A : PIN_B;
  for (uint8_t bit = 0; bit < 4; bit++) {
    digitalWrite(pins[bit], (drivenWord >> bit) & 1 ? HIGH : LOW);
  }
  settle();
}

void parkHighImpedance() {
  releaseAll();
  digitalWrite(PIN_OEA, HIGH);
  digitalWrite(PIN_OEB, LOW);
  outputsOn = false;
  settle();
}

void driveSide(bool aToB) {
  fromA = aToB;
  const uint8_t* releasePins = aToB ? PIN_B : PIN_A;
  const uint8_t* drivePins = aToB ? PIN_A : PIN_B;
  for (uint8_t bit = 0; bit < 4; bit++) {
    pinMode(releasePins[bit], INPUT);
  }
  for (uint8_t bit = 0; bit < 4; bit++) {
    pinMode(drivePins[bit], OUTPUT);
    digitalWrite(drivePins[bit], (drivenWord >> bit) & 1 ? HIGH : LOW);
  }
  settle();
}

void enableAtoB() {
  parkHighImpedance();
  driveSide(true);
  digitalWrite(PIN_OEA, LOW);
  outputsOn = true;
  settle();
}

void enableBtoA() {
  parkHighImpedance();
  driveSide(false);
  digitalWrite(PIN_OEB, HIGH);
  outputsOn = true;
  settle();
}

void writeAndCheck(uint8_t value, const char* step) {
  writeDriven(value);
  check(step);
}

void transmit(bool aToB, const char* prefix) {
  if (aToB) enableAtoB();
  else enableBtoA();
  char label[32];
  snprintf(label, sizeof(label), "%s-00", prefix);
  writeAndCheck(0x0, label);
  snprintf(label, sizeof(label), "%s-0f", prefix);
  writeAndCheck(0xF, label);
  snprintf(label, sizeof(label), "%s-05", prefix);
  writeAndCheck(0x5, label);
  snprintf(label, sizeof(label), "%s-0a", prefix);
  writeAndCheck(0xA, label);
  for (uint8_t bit = 0; bit < 4; bit++) {
    snprintf(label, sizeof(label), "%s-walk-%u", prefix, bit);
    writeAndCheck(static_cast<uint8_t>(1u << bit), label);
  }
}

void isolate(bool aToB, bool oeaHigh, bool oebHigh, const char* prefix) {
  releaseAll();
  digitalWrite(PIN_OEA, oeaHigh ? HIGH : LOW);
  digitalWrite(PIN_OEB, oebHigh ? HIGH : LOW);
  outputsOn = false;
  settle();
  driveSide(aToB);
  char label[32];
  snprintf(label, sizeof(label), "%s-00", prefix);
  writeAndCheck(0x0, label);
  snprintf(label, sizeof(label), "%s-0f", prefix);
  writeAndCheck(0xF, label);
}

void runChecks() {
  transmit(true, "a-to-b");
  transmit(false, "b-to-a");
  isolate(true, true, false, "isolate-b-hl");
  isolate(true, false, true, "isolate-b-lh");
  isolate(false, true, false, "isolate-a-hl");
  isolate(false, false, true, "isolate-a-lh");
}

void setup() {
  pinMode(PIN_OEA, OUTPUT);
  pinMode(PIN_OEB, OUTPUT);
  digitalWrite(PIN_OEA, HIGH);
  digitalWrite(PIN_OEB, LOW);
  releaseAll();

  Serial.begin(115200);
  Serial.println("Send any character to test the 74HC243");
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
