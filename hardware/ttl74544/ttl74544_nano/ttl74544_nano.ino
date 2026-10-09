/*
 * Self-check for a 74HC544 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * After PHASE1 PASS, move the four data wires and send another character.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * nE and nLE both low make that latch transparent. A rising nLE while nE is low,
 * or a rising nE while nLE is low, stores the bus. The opposite bus is the
 * inverted stored word only while nE and nOE are both low. Bit 0 is A0/B0.
 * Each data pin has a 100k pulldown. Enables stay high until the pins are set.
 */

const uint8_t PIN_nLEBA = 2;
const uint8_t PIN_nOEBA = 3;
const uint8_t PIN_nEAB = 4;
const uint8_t PIN_nOEAB = 5;
const uint8_t PIN_nLEAB = 6;
const uint8_t PIN_nEBA = 7;

const uint8_t LEVEL_LOW = 0;
const uint8_t LEVEL_HIGH = 1;
const uint8_t LEVEL_Z = 2;
const uint8_t PIN_NONE = 255;

uint8_t pinA[8];
uint8_t pinB[8];
uint8_t activeBits[6];
uint8_t activeCount = 0;

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

void controls(uint8_t nEab, uint8_t nLeab, uint8_t nOeab, uint8_t nEba, uint8_t nLeba, uint8_t nOeba) {
  digitalWrite(PIN_nEAB, nEab);
  digitalWrite(PIN_nLEAB, nLeab);
  digitalWrite(PIN_nOEAB, nOeab);
  digitalWrite(PIN_nEBA, nEba);
  digitalWrite(PIN_nLEBA, nLeba);
  digitalWrite(PIN_nOEBA, nOeba);
  settle();
}

void releaseChip() { controls(HIGH, HIGH, HIGH, HIGH, HIGH, HIGH); }

void assignPins(const uint8_t* bits, const uint8_t* aPins, const uint8_t* bPins, uint8_t count) {
  for (uint8_t bit = 0; bit < 8; bit++) {
    pinA[bit] = PIN_NONE;
    pinB[bit] = PIN_NONE;
  }
  activeCount = count;
  for (uint8_t index = 0; index < count; index++) {
    activeBits[index] = bits[index];
    pinA[bits[index]] = aPins[index];
    pinB[bits[index]] = bPins[index];
  }
}

void loadPhase(uint8_t phase) {
  if (phase == 0) {
    const uint8_t bits[] = {0, 1, 2, 3, 4, 5};
    const uint8_t aPins[] = {8, 9, 10, 11, 12, 13};
    const uint8_t bPins[] = {A0, A1, A2, A3, A4, A5};
    assignPins(bits, aPins, bPins, 6);
  } else {
    const uint8_t bits[] = {2, 3, 4, 5, 6, 7};
    const uint8_t aPins[] = {10, 11, 12, 13, 8, 9};
    const uint8_t bPins[] = {A2, A3, A4, A5, A0, A1};
    assignPins(bits, aPins, bPins, 6);
  }
}

void floatBus(const uint8_t pins[8]) {
  for (uint8_t index = 0; index < activeCount; index++) {
    pinMode(pins[activeBits[index]], INPUT);
  }
}

void driveBus(const uint8_t pins[8], uint8_t value) {
  for (uint8_t index = 0; index < activeCount; index++) {
    const uint8_t bit = activeBits[index];
    pinMode(pins[bit], OUTPUT);
    digitalWrite(pins[bit], (value >> bit) & 1 ? HIGH : LOW);
  }
  settle();
}

uint8_t connectedMask() {
  uint8_t mask = 0;
  for (uint8_t index = 0; index < activeCount; index++) {
    mask |= static_cast<uint8_t>(1u << activeBits[index]);
  }
  return mask;
}

void checkBus(const uint8_t pins[8], bool enabled, uint8_t value, const char* step) {
  for (uint8_t index = 0; index < activeCount; index++) {
    const uint8_t bit = activeBits[index];
    const uint8_t actual = sense(pins[bit]);
    const uint8_t expected = enabled ? ((value >> bit) & 1) : LEVEL_Z;
    if (actual == expected) continue;
    char detail[48];
    snprintf(detail, sizeof(detail), "bit %u expected %u got %u", bit, expected, actual);
    noteFailure(step, detail);
    return;
  }
}

void label(char* dest, uint8_t size, const char* phase, const char* name) {
  snprintf(dest, size, "%s-%s", phase, name);
}

void checkTowardB(const char* phase) {
  char step[32];
  const uint8_t patterns[] = {0x00, 0xFF, 0x55, 0xAA};
  const char* names[] = {"b-00", "b-ff", "b-55", "b-aa"};
  const uint8_t mask = connectedMask();

  for (uint8_t index = 0; index < 4; index++) {
    const uint8_t pattern = patterns[index] & mask;
    releaseChip();
    floatBus(pinB);
    driveBus(pinA, pattern);
    controls(LOW, LOW, LOW, HIGH, HIGH, HIGH);
    label(step, sizeof(step), phase, names[index]);
    checkBus(pinB, true, static_cast<uint8_t>(~pattern), step);
  }

  for (uint8_t index = 0; index < activeCount; index++) {
    const uint8_t pattern = static_cast<uint8_t>(1u << activeBits[index]);
    releaseChip();
    floatBus(pinB);
    driveBus(pinA, pattern);
    controls(LOW, LOW, LOW, HIGH, HIGH, HIGH);
    char name[16];
    snprintf(name, sizeof(name), "b-walk-%u", activeBits[index]);
    label(step, sizeof(step), phase, name);
    checkBus(pinB, true, static_cast<uint8_t>(~pattern), step);
  }

  const uint8_t first = mask & 0x55;
  const uint8_t second = mask & 0xAA;
  releaseChip();
  floatBus(pinB);
  driveBus(pinA, first);
  controls(LOW, LOW, LOW, HIGH, HIGH, HIGH);
  controls(LOW, HIGH, LOW, HIGH, HIGH, HIGH);
  driveBus(pinA, second);
  label(step, sizeof(step), phase, "b-hold");
  checkBus(pinB, true, static_cast<uint8_t>(~first), step);

  releaseChip();
  driveBus(pinA, first);
  controls(LOW, LOW, LOW, HIGH, HIGH, HIGH);
  controls(HIGH, LOW, LOW, HIGH, HIGH, HIGH);
  label(step, sizeof(step), phase, "b-e-z");
  checkBus(pinB, false, 0, step);
  driveBus(pinA, second);
  controls(HIGH, HIGH, LOW, HIGH, HIGH, HIGH);
  controls(LOW, HIGH, LOW, HIGH, HIGH, HIGH);
  label(step, sizeof(step), phase, "b-e-hold");
  checkBus(pinB, true, static_cast<uint8_t>(~first), step);

  releaseChip();
  driveBus(pinA, first);
  controls(LOW, LOW, LOW, HIGH, HIGH, HIGH);
  controls(LOW, HIGH, HIGH, HIGH, HIGH, HIGH);
  label(step, sizeof(step), phase, "b-oe-z");
  checkBus(pinB, false, 0, step);
  driveBus(pinA, second);
  controls(LOW, HIGH, LOW, HIGH, HIGH, HIGH);
  label(step, sizeof(step), phase, "b-oe-hold");
  checkBus(pinB, true, static_cast<uint8_t>(~first), step);

  releaseChip();
  driveBus(pinA, first);
  controls(LOW, LOW, HIGH, HIGH, HIGH, HIGH);
  label(step, sizeof(step), phase, "b-hidden-z");
  checkBus(pinB, false, 0, step);
  controls(LOW, HIGH, HIGH, HIGH, HIGH, HIGH);
  driveBus(pinA, second);
  controls(LOW, HIGH, LOW, HIGH, HIGH, HIGH);
  label(step, sizeof(step), phase, "b-hidden-store");
  checkBus(pinB, true, static_cast<uint8_t>(~first), step);
}

void checkTowardA(const char* phase) {
  char step[32];
  const uint8_t patterns[] = {0x00, 0xFF, 0x55, 0xAA};
  const char* names[] = {"a-00", "a-ff", "a-55", "a-aa"};
  const uint8_t mask = connectedMask();

  for (uint8_t index = 0; index < 4; index++) {
    const uint8_t pattern = patterns[index] & mask;
    releaseChip();
    floatBus(pinA);
    driveBus(pinB, pattern);
    controls(HIGH, HIGH, HIGH, LOW, LOW, LOW);
    label(step, sizeof(step), phase, names[index]);
    checkBus(pinA, true, static_cast<uint8_t>(~pattern), step);
  }

  for (uint8_t index = 0; index < activeCount; index++) {
    const uint8_t pattern = static_cast<uint8_t>(1u << activeBits[index]);
    releaseChip();
    floatBus(pinA);
    driveBus(pinB, pattern);
    controls(HIGH, HIGH, HIGH, LOW, LOW, LOW);
    char name[16];
    snprintf(name, sizeof(name), "a-walk-%u", activeBits[index]);
    label(step, sizeof(step), phase, name);
    checkBus(pinA, true, static_cast<uint8_t>(~pattern), step);
  }

  const uint8_t first = mask & 0x55;
  const uint8_t second = mask & 0xAA;
  releaseChip();
  floatBus(pinA);
  driveBus(pinB, first);
  controls(HIGH, HIGH, HIGH, LOW, LOW, LOW);
  controls(HIGH, HIGH, HIGH, LOW, HIGH, LOW);
  driveBus(pinB, second);
  label(step, sizeof(step), phase, "a-hold");
  checkBus(pinA, true, static_cast<uint8_t>(~first), step);

  releaseChip();
  driveBus(pinB, first);
  controls(HIGH, HIGH, HIGH, LOW, LOW, LOW);
  controls(HIGH, HIGH, HIGH, HIGH, LOW, LOW);
  label(step, sizeof(step), phase, "a-e-z");
  checkBus(pinA, false, 0, step);
  driveBus(pinB, second);
  controls(HIGH, HIGH, HIGH, HIGH, HIGH, LOW);
  controls(HIGH, HIGH, HIGH, LOW, HIGH, LOW);
  label(step, sizeof(step), phase, "a-e-hold");
  checkBus(pinA, true, static_cast<uint8_t>(~first), step);

  releaseChip();
  driveBus(pinB, first);
  controls(HIGH, HIGH, HIGH, LOW, LOW, LOW);
  controls(HIGH, HIGH, HIGH, LOW, HIGH, HIGH);
  label(step, sizeof(step), phase, "a-oe-z");
  checkBus(pinA, false, 0, step);
  driveBus(pinB, second);
  controls(HIGH, HIGH, HIGH, LOW, HIGH, LOW);
  label(step, sizeof(step), phase, "a-oe-hold");
  checkBus(pinA, true, static_cast<uint8_t>(~first), step);

  releaseChip();
  driveBus(pinB, first);
  controls(HIGH, HIGH, HIGH, LOW, LOW, HIGH);
  label(step, sizeof(step), phase, "a-hidden-z");
  checkBus(pinA, false, 0, step);
  controls(HIGH, HIGH, HIGH, LOW, HIGH, HIGH);
  driveBus(pinB, second);
  controls(HIGH, HIGH, HIGH, LOW, HIGH, LOW);
  label(step, sizeof(step), phase, "a-hidden-store");
  checkBus(pinA, true, static_cast<uint8_t>(~first), step);
}

void runPhase(const char* phase) {
  checkTowardB(phase);
  if (failed) return;
  checkTowardA(phase);
}

void waitForCharacter() {
  while (Serial.available() == 0) {
  }
  while (Serial.available() > 0) Serial.read();
}

void setup() {
  const uint8_t dataPins[] = {8, 9, 10, 11, 12, 13, A0, A1, A2, A3, A4, A5};
  pinMode(PIN_nLEBA, OUTPUT);
  pinMode(PIN_nOEBA, OUTPUT);
  pinMode(PIN_nEAB, OUTPUT);
  pinMode(PIN_nOEAB, OUTPUT);
  pinMode(PIN_nLEAB, OUTPUT);
  pinMode(PIN_nEBA, OUTPUT);
  releaseChip();
  for (uint8_t index = 0; index < sizeof(dataPins); index++) {
    pinMode(dataPins[index], INPUT);
  }

  Serial.begin(115200);
  Serial.println("Send any character to test A0-A5 and B0-B5");
  waitForCharacter();
  loadPhase(0);
  runPhase("low");
  if (failed) {
    Serial.println(resultLine);
    return;
  }

  releaseChip();
  floatBus(pinA);
  floatBus(pinB);
  Serial.println("PHASE1 PASS");
  Serial.println("Move D8 from pin 3 A0 to pin 9 A6");
  Serial.println("Move D9 from pin 4 A1 to pin 10 A7");
  Serial.println("Move A0 from pin 22 B0 to pin 16 B6");
  Serial.println("Move A1 from pin 21 B1 to pin 15 B7");
  Serial.println("Send any character to continue");
  waitForCharacter();
  loadPhase(1);
  runPhase("high");
  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
