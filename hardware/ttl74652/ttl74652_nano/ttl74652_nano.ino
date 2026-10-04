/*
 * Self-check for a 74HC652 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Only A1-A6 and B1-B6 are connected. A7, A8, B7 and B8 are tied low and are
 * not checked. A high OEAB copies A onto B. A low OEBA copies B onto A. Both
 * can be active together. SAB and SBA select the stored register instead of
 * the live bus. CLKAB and CLKBA store on the rising edge only. Each connected
 * data pin has a 100k pulldown, so a floating pin reads low until the internal
 * pull-up is enabled. Both output enables are turned off before the pin modes
 * change, so the Nano and the chip never drive the same line.
 */

const uint8_t PIN_CLKAB = 2;
const uint8_t PIN_SAB = 3;
const uint8_t PIN_OEAB = 4;
const uint8_t PIN_OEBA = 5;
const uint8_t PIN_SBA = 6;
const uint8_t PIN_CLKBA = 7;
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
    snprintf(detail, sizeof(detail), "%s%u expected %u got %u", name, bit + 1, want, actual);
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

void selects(bool sabStored, bool sbaStored) {
  digitalWrite(PIN_SAB, sabStored ? HIGH : LOW);
  digitalWrite(PIN_SBA, sbaStored ? HIGH : LOW);
  settle();
}

void enables(bool driveB, bool driveA) {
  digitalWrite(PIN_OEAB, driveB ? HIGH : LOW);
  digitalWrite(PIN_OEBA, driveA ? LOW : HIGH);
  settle();
}

void clocksLow() {
  digitalWrite(PIN_CLKAB, LOW);
  digitalWrite(PIN_CLKBA, LOW);
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
  enables(false, false);
  selects(false, false);
  const uint8_t* source = aToB ? PIN_A : PIN_B;
  const uint8_t* dest = aToB ? PIN_B : PIN_A;
  const char* name = aToB ? "B" : "A";
  release(dest);
  const uint8_t patterns[] = {0x00, 0x3F, 0x15, 0x2A};
  drive(source, patterns[0]);
  enables(aToB, !aToB);
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
    snprintf(label, sizeof(label), "%s-walk-%u", prefix, bit + 1);
    checkWord(dest, name, false, pattern, label);
  }
  enables(false, false);
}

void checkIsolation() {
  enables(false, false);
  selects(false, false);
  release(PIN_B);
  drive(PIN_A, 0x00);
  checkWord(PIN_B, "B", true, 0, "iso-b-00");
  drive(PIN_A, 0x3F);
  checkWord(PIN_B, "B", true, 0, "iso-b-ff");

  release(PIN_A);
  drive(PIN_B, 0x00);
  checkWord(PIN_A, "A", true, 0, "iso-a-00");
  drive(PIN_B, 0x3F);
  checkWord(PIN_A, "A", true, 0, "iso-a-ff");
}

void checkStoreA() {
  enables(false, false);
  clocksLow();
  selects(false, false);
  release(PIN_B);
  drive(PIN_A, 0x15);
  rise(PIN_CLKAB);
  drive(PIN_A, 0x2A);
  selects(true, false);
  enables(true, false);
  checkWord(PIN_B, "B", false, 0x15, "store-a-level");

  enables(false, false);
  digitalWrite(PIN_CLKAB, LOW);
  settle();
  enables(true, false);
  checkWord(PIN_B, "B", false, 0x15, "store-a-fall");
  enables(false, false);
  clocksLow();
}

void checkBothStored() {
  enables(false, false);
  clocksLow();
  selects(false, false);
  release(PIN_B);
  drive(PIN_A, 0x15);
  pulse(PIN_CLKAB);
  release(PIN_A);
  drive(PIN_B, 0x2A);
  pulse(PIN_CLKBA);

  selects(true, true);
  release(PIN_A);
  release(PIN_B);
  enables(true, true);
  checkWord(PIN_B, "B", false, 0x15, "both-b");
  checkWord(PIN_A, "A", false, 0x2A, "both-a");
  enables(false, false);
  clocksLow();
}

void checkCaptureFromTheDrivenPin() {
  enables(false, false);
  clocksLow();
  selects(false, false);
  release(PIN_A);
  drive(PIN_B, 0x15);
  enables(false, true);
  pulse(PIN_CLKAB);

  enables(false, false);
  selects(true, false);
  release(PIN_B);
  drive(PIN_A, 0x00);
  enables(true, false);
  checkWord(PIN_B, "B", false, 0x15, "capture-a");
  enables(false, false);
  clocksLow();
}

void setup() {
  pinMode(PIN_CLKAB, OUTPUT);
  pinMode(PIN_SAB, OUTPUT);
  pinMode(PIN_OEAB, OUTPUT);
  pinMode(PIN_OEBA, OUTPUT);
  pinMode(PIN_SBA, OUTPUT);
  pinMode(PIN_CLKBA, OUTPUT);
  enables(false, false);
  clocksLow();
  selects(false, false);
  release(PIN_A);
  release(PIN_B);

  Serial.begin(115200);
  Serial.println(F("Send a character to start the 74HC652 check"));
  while (Serial.read() < 0) {
  }
  while (Serial.read() >= 0) {
  }

  checkIsolation();
  checkLive(true, "ab");
  checkLive(false, "ba");
  checkStoreA();
  checkBothStored();
  checkCaptureFromTheDrivenPin();
  enables(false, false);
  release(PIN_A);
  release(PIN_B);

  if (!failed) {
    Serial.println(F("RESULT PASS"));
  } else {
    Serial.println(resultLine);
  }
}

void loop() {}
