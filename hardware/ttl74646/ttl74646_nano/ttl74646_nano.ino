/*
 * Self-check for a 74HC646 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Only A0-A5 and B0-B5 are connected. A6, A7, B6 and B7 are tied low and are
 * not checked. A low nOE and a high DIR copy A onto B. A low nOE and a low DIR
 * copy B onto A. SAB and SBA select the stored register instead of the live bus.
 * CPAB and CPBA store on the rising edge only. Each connected data pin has a
 * 100k pulldown, so a floating pin reads low until the internal pull-up is
 * enabled. nOE is raised before the pin modes change, so the Nano and the chip
 * never drive the same line.
 */

const uint8_t PIN_CPAB = 2;
const uint8_t PIN_SAB = 3;
const uint8_t PIN_DIR = 4;
const uint8_t PIN_nOE = 5;
const uint8_t PIN_SBA = 6;
const uint8_t PIN_CPBA = 7;
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

void controls(bool dirHigh, bool sabHigh, bool sbaHigh) {
  digitalWrite(PIN_DIR, dirHigh ? HIGH : LOW);
  digitalWrite(PIN_SAB, sabHigh ? HIGH : LOW);
  digitalWrite(PIN_SBA, sbaHigh ? HIGH : LOW);
  settle();
}

void outputEnable(bool enabled) {
  digitalWrite(PIN_nOE, enabled ? LOW : HIGH);
  settle();
}

void clocksLow() {
  digitalWrite(PIN_CPAB, LOW);
  digitalWrite(PIN_CPBA, LOW);
  settle();
}

void pulse(uint8_t pin) {
  digitalWrite(pin, LOW);
  settle();
  digitalWrite(pin, HIGH);
  settle();
  digitalWrite(pin, LOW);
  settle();
}

void rise(uint8_t pin) {
  digitalWrite(pin, LOW);
  settle();
  digitalWrite(pin, HIGH);
  settle();
}

void checkLive(bool aToB, const char* prefix) {
  outputEnable(false);
  controls(aToB, false, false);
  const uint8_t* source = aToB ? PIN_A : PIN_B;
  const uint8_t* dest = aToB ? PIN_B : PIN_A;
  const char* name = aToB ? "B" : "A";
  release(dest);
  const uint8_t patterns[] = {0x00, 0x3F, 0x15, 0x2A};
  drive(source, patterns[0]);
  outputEnable(true);
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
  outputEnable(false);
}

void checkIsolation() {
  outputEnable(false);
  controls(true, false, false);
  release(PIN_B);
  drive(PIN_A, 0x00);
  checkWord(PIN_B, "B", true, 0, "iso-b-00");
  drive(PIN_A, 0x3F);
  checkWord(PIN_B, "B", true, 0, "iso-b-ff");

  controls(false, false, false);
  release(PIN_A);
  drive(PIN_B, 0x00);
  checkWord(PIN_A, "A", true, 0, "iso-a-00");
  drive(PIN_B, 0x3F);
  checkWord(PIN_A, "A", true, 0, "iso-a-ff");
}

void checkStoreA() {
  outputEnable(false);
  clocksLow();
  controls(true, false, false);
  release(PIN_B);
  drive(PIN_A, 0x15);
  rise(PIN_CPAB);
  drive(PIN_A, 0x2A);
  controls(true, true, false);
  outputEnable(true);
  checkWord(PIN_B, "B", false, 0x15, "store-a-level");

  outputEnable(false);
  digitalWrite(PIN_CPAB, LOW);
  settle();
  outputEnable(true);
  checkWord(PIN_B, "B", false, 0x15, "store-a-fall");
  outputEnable(false);
  clocksLow();
}

void checkIndependentRegisters() {
  outputEnable(false);
  clocksLow();
  controls(true, false, false);
  release(PIN_B);
  drive(PIN_A, 0x15);
  pulse(PIN_CPAB);
  release(PIN_A);
  drive(PIN_B, 0x2A);
  pulse(PIN_CPBA);

  controls(true, true, false);
  release(PIN_B);
  drive(PIN_A, 0x00);
  outputEnable(true);
  checkWord(PIN_B, "B", false, 0x15, "reg-a");

  outputEnable(false);
  controls(false, false, true);
  release(PIN_A);
  drive(PIN_B, 0x00);
  outputEnable(true);
  checkWord(PIN_A, "A", false, 0x2A, "reg-b");
  outputEnable(false);
  clocksLow();
}

void checkCaptureFromTheDrivenPin() {
  outputEnable(false);
  clocksLow();
  controls(false, false, false);
  release(PIN_A);
  drive(PIN_B, 0x15);
  outputEnable(true);
  pulse(PIN_CPAB);

  outputEnable(false);
  controls(true, true, false);
  release(PIN_B);
  drive(PIN_A, 0x00);
  outputEnable(true);
  checkWord(PIN_B, "B", false, 0x15, "capture-a");
  outputEnable(false);
  clocksLow();
}

void setup() {
  pinMode(PIN_CPAB, OUTPUT);
  pinMode(PIN_SAB, OUTPUT);
  pinMode(PIN_DIR, OUTPUT);
  pinMode(PIN_nOE, OUTPUT);
  pinMode(PIN_SBA, OUTPUT);
  pinMode(PIN_CPBA, OUTPUT);
  outputEnable(false);
  clocksLow();
  controls(false, false, false);
  release(PIN_A);
  release(PIN_B);

  Serial.begin(115200);
  Serial.println(F("Send a character to start the 74HC646 check"));
  while (Serial.read() < 0) {
  }
  while (Serial.read() >= 0) {
  }

  checkIsolation();
  checkLive(true, "ab");
  checkLive(false, "ba");
  checkStoreA();
  checkIndependentRegisters();
  checkCaptureFromTheDrivenPin();
  outputEnable(false);
  release(PIN_A);
  release(PIN_B);

  if (!failed) {
    Serial.println(F("RESULT PASS"));
  } else {
    Serial.println(resultLine);
  }
}

void loop() {}
