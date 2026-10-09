/*
 * Self-check for a 74HC543 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Only A0-A5 and B0-B5 are connected. A6, A7, B6 and B7 are tied low and are
 * not checked. A latch is transparent while its nCE and nLE are both low.
 * The bus is driven only while nCE and nOE are both low. Each connected data
 * pin has a 100k pulldown, so a floating pin reads low until the internal
 * pull-up is enabled. Both nOE pins are raised before the pin modes change,
 * so the Nano and the chip never drive the same line.
 */

const uint8_t PIN_nLEBA = 2;
const uint8_t PIN_nOEBA = 3;
const uint8_t PIN_nCEAB = 4;
const uint8_t PIN_nOEAB = 5;
const uint8_t PIN_nLEAB = 6;
const uint8_t PIN_nCEBA = 7;
const uint8_t PIN_A[6] = {8, 9, 10, 11, 12, 13};
const uint8_t PIN_B[6] = {A0, A1, A2, A3, A4, A5};
const uint8_t BITS = 6;

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

void checkWord(
    const uint8_t* pins, const char* name, bool highZ, uint8_t expected, const char* step) {
  for (uint8_t bit = 0; bit < BITS; bit++) {
    const uint8_t actual = sense(pins[bit]);
    const uint8_t want = highZ ? LEVEL_Z : ((expected >> bit) & 1);
    if (actual == want) continue;
    char detail[48];
    snprintf(detail, sizeof(detail), "%s%u expected %u got %u", name, bit, want, actual);
    noteFailure(step, detail);
    return;
  }
}

void drive(const uint8_t* pins, uint8_t value) {
  for (uint8_t bit = 0; bit < BITS; bit++) {
    pinMode(pins[bit], OUTPUT);
    digitalWrite(pins[bit], (value >> bit) & 1 ? HIGH : LOW);
  }
  settle();
}

void release(const uint8_t* pins) {
  for (uint8_t bit = 0; bit < BITS; bit++) {
    pinMode(pins[bit], INPUT);
  }
  settle();
}

void disableOutputs() {
  digitalWrite(PIN_nOEAB, HIGH);
  digitalWrite(PIN_nOEBA, HIGH);
  settle();
}

void park() {
  digitalWrite(PIN_nCEAB, HIGH);
  digitalWrite(PIN_nLEAB, HIGH);
  digitalWrite(PIN_nOEAB, HIGH);
  digitalWrite(PIN_nCEBA, HIGH);
  digitalWrite(PIN_nLEBA, HIGH);
  digitalWrite(PIN_nOEBA, HIGH);
  settle();
}

void arm(const uint8_t* source, const uint8_t* dest, uint8_t value) {
  disableOutputs();
  release(source);
  release(dest);
  drive(source, value);
}

void checkLive(bool aToB, const char* prefix) {
  const uint8_t* source = aToB ? PIN_A : PIN_B;
  const uint8_t* dest = aToB ? PIN_B : PIN_A;
  const char* name = aToB ? "B" : "A";
  const uint8_t patterns[] = {0x00, 0x3F, 0x15, 0x2A};
  arm(source, dest, patterns[0]);
  if (aToB) {
    digitalWrite(PIN_nCEAB, LOW);
    digitalWrite(PIN_nLEAB, LOW);
    digitalWrite(PIN_nOEAB, LOW);
  } else {
    digitalWrite(PIN_nCEBA, LOW);
    digitalWrite(PIN_nLEBA, LOW);
    digitalWrite(PIN_nOEBA, LOW);
  }
  settle();
  for (uint8_t index = 0; index < 4; index++) {
    drive(source, patterns[index]);
    char label[24];
    snprintf(label, sizeof(label), "%s-%02X", prefix, patterns[index]);
    checkWord(dest, name, false, patterns[index], label);
  }
  for (uint8_t bit = 0; bit < BITS; bit++) {
    const uint8_t pattern = static_cast<uint8_t>(1u << bit);
    drive(source, pattern);
    char label[24];
    snprintf(label, sizeof(label), "%s-walk-%u", prefix, bit);
    checkWord(dest, name, false, pattern, label);
  }
  park();
}

void checkIsolation() {
  arm(PIN_A, PIN_B, 0x00);
  digitalWrite(PIN_nCEAB, LOW);
  digitalWrite(PIN_nLEAB, LOW);
  settle();
  checkWord(PIN_B, "B", true, 0, "iso-oe-b-00");
  drive(PIN_A, 0x3F);
  checkWord(PIN_B, "B", true, 0, "iso-oe-b-ff");

  digitalWrite(PIN_nCEAB, HIGH);
  digitalWrite(PIN_nOEAB, LOW);
  settle();
  drive(PIN_A, 0x00);
  checkWord(PIN_B, "B", true, 0, "iso-ce-b-00");
  drive(PIN_A, 0x3F);
  checkWord(PIN_B, "B", true, 0, "iso-ce-b-ff");

  arm(PIN_B, PIN_A, 0x00);
  digitalWrite(PIN_nCEBA, LOW);
  digitalWrite(PIN_nLEBA, LOW);
  settle();
  checkWord(PIN_A, "A", true, 0, "iso-oe-a-00");
  drive(PIN_B, 0x3F);
  checkWord(PIN_A, "A", true, 0, "iso-oe-a-ff");

  digitalWrite(PIN_nCEBA, HIGH);
  digitalWrite(PIN_nOEBA, LOW);
  settle();
  drive(PIN_B, 0x00);
  checkWord(PIN_A, "A", true, 0, "iso-ce-a-00");
  drive(PIN_B, 0x3F);
  checkWord(PIN_A, "A", true, 0, "iso-ce-a-ff");
  park();
}

void checkLatchEnable(bool aToB) {
  const uint8_t* source = aToB ? PIN_A : PIN_B;
  const uint8_t* dest = aToB ? PIN_B : PIN_A;
  const char* name = aToB ? "B" : "A";
  const char* step = aToB ? "le-ab" : "le-ba";
  const uint8_t enable = aToB ? PIN_nLEAB : PIN_nLEBA;
  const uint8_t outputEnable = aToB ? PIN_nOEAB : PIN_nOEBA;
  const uint8_t chipEnable = aToB ? PIN_nCEAB : PIN_nCEBA;

  arm(source, dest, 0x15);
  digitalWrite(chipEnable, LOW);
  digitalWrite(enable, LOW);
  digitalWrite(outputEnable, LOW);
  settle();
  digitalWrite(enable, HIGH);
  settle();
  drive(source, 0x2A);
  checkWord(dest, name, false, 0x15, step);
  park();
}

void checkChipEnable(bool aToB) {
  const uint8_t* source = aToB ? PIN_A : PIN_B;
  const uint8_t* dest = aToB ? PIN_B : PIN_A;
  const char* name = aToB ? "B" : "A";
  const char* stepZ = aToB ? "ce-ab-z" : "ce-ba-z";
  const char* stepHold = aToB ? "ce-ab-hold" : "ce-ba-hold";
  const uint8_t latchEnable = aToB ? PIN_nLEAB : PIN_nLEBA;
  const uint8_t outputEnable = aToB ? PIN_nOEAB : PIN_nOEBA;
  const uint8_t chipEnable = aToB ? PIN_nCEAB : PIN_nCEBA;

  arm(source, dest, 0x15);
  digitalWrite(chipEnable, LOW);
  digitalWrite(latchEnable, LOW);
  digitalWrite(outputEnable, LOW);
  settle();
  digitalWrite(chipEnable, HIGH);
  settle();
  drive(source, 0x2A);
  checkWord(dest, name, true, 0, stepZ);

  digitalWrite(latchEnable, HIGH);
  digitalWrite(chipEnable, LOW);
  settle();
  checkWord(dest, name, false, 0x15, stepHold);
  park();
}

void checkIndependentLatches() {
  arm(PIN_A, PIN_B, 0x15);
  digitalWrite(PIN_nCEAB, LOW);
  digitalWrite(PIN_nLEAB, LOW);
  digitalWrite(PIN_nOEAB, LOW);
  settle();
  digitalWrite(PIN_nLEAB, HIGH);
  settle();

  arm(PIN_B, PIN_A, 0x2A);
  digitalWrite(PIN_nCEBA, LOW);
  digitalWrite(PIN_nLEBA, LOW);
  digitalWrite(PIN_nOEBA, LOW);
  settle();
  digitalWrite(PIN_nLEBA, HIGH);
  settle();

  arm(PIN_A, PIN_B, 0x00);
  digitalWrite(PIN_nCEAB, LOW);
  digitalWrite(PIN_nLEAB, HIGH);
  digitalWrite(PIN_nOEAB, LOW);
  settle();
  checkWord(PIN_B, "B", false, 0x15, "reg-ab");

  arm(PIN_B, PIN_A, 0x00);
  digitalWrite(PIN_nCEBA, LOW);
  digitalWrite(PIN_nLEBA, HIGH);
  digitalWrite(PIN_nOEBA, LOW);
  settle();
  checkWord(PIN_A, "A", false, 0x2A, "reg-ba");
  park();
}

void setup() {
  pinMode(PIN_nLEBA, OUTPUT);
  pinMode(PIN_nOEBA, OUTPUT);
  pinMode(PIN_nCEAB, OUTPUT);
  pinMode(PIN_nOEAB, OUTPUT);
  pinMode(PIN_nLEAB, OUTPUT);
  pinMode(PIN_nCEBA, OUTPUT);
  park();
  release(PIN_A);
  release(PIN_B);

  Serial.begin(115200);
  Serial.println(F("Send a character to start the 74HC543 check"));
  while (Serial.read() < 0) {
  }
  while (Serial.read() >= 0) {
  }

  checkIsolation();
  checkLive(true, "ab");
  checkLive(false, "ba");
  checkLatchEnable(true);
  checkLatchEnable(false);
  checkChipEnable(true);
  checkChipEnable(false);
  checkIndependentLatches();
  park();
  release(PIN_A);
  release(PIN_B);

  if (!failed) {
    Serial.println(F("RESULT PASS"));
  } else {
    Serial.println(resultLine);
  }
}

void loop() {}
