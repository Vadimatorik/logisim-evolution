/*
 * Self-check for a 74HC589 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * RCK rising stores A-H in the input latch. SLOAD low copies that latch into
 * the shift register at once, and SCK does not shift while SLOAD stays low.
 * SLOAD high and SCK rising shift SA in at stage A; QH then emits H, G, ... A.
 * OE high releases QH. A 10k resistor from BIAS to QH shows Z: a driven HC
 * output overrides the resistor, and a released output follows BIAS.
 * Clocks stay low, SLOAD stays high and OE stays high until the check starts.
 */

const uint8_t PIN_P[8] = {2, 3, 4, 5, 6, 7, 8, 9};
const uint8_t PIN_SA = 10;
const uint8_t PIN_SLOAD = 11;
const uint8_t PIN_RCK = 12;
const uint8_t PIN_SCK = 13;
const uint8_t PIN_OE = A0;
const uint8_t PIN_QH = A1;
const uint8_t PIN_BIAS = A2;

const int QH_LOW = 0;
const int QH_HIGH = 1;
const int QH_Z = 2;

bool failed = false;
char resultLine[96];

const char* qhName(int level) {
  if (level == QH_LOW) return "0";
  if (level == QH_HIGH) return "1";
  if (level == QH_Z) return "Z";
  return "?";
}

void noteFailure(const char* step, int expected, int actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s expected %s got %s",
      step,
      qhName(expected),
      qhName(actual));
}

void settle() { delay(1); }

void setParallel(uint8_t value) {
  for (uint8_t bit = 0; bit < 8; bit++) {
    digitalWrite(PIN_P[bit], (value & (1u << bit)) ? HIGH : LOW);
  }
}

void pulse(uint8_t pin) {
  digitalWrite(pin, LOW);
  settle();
  digitalWrite(pin, HIGH);
  settle();
  digitalWrite(pin, LOW);
  settle();
}

void riseBothClocks() {
  digitalWrite(PIN_SCK, LOW);
  digitalWrite(PIN_RCK, LOW);
  settle();
  digitalWrite(PIN_SCK, HIGH);
  digitalWrite(PIN_RCK, HIGH);
  settle();
  digitalWrite(PIN_SCK, LOW);
  digitalWrite(PIN_RCK, LOW);
  settle();
}

void loadParallel(uint8_t value) {
  setParallel(value);
  digitalWrite(PIN_SLOAD, LOW);
  settle();
  pulse(PIN_RCK);
}

int readQh() {
  digitalWrite(PIN_BIAS, LOW);
  settle();
  const bool lowBiasHigh = digitalRead(PIN_QH) == HIGH;
  digitalWrite(PIN_BIAS, HIGH);
  settle();
  const bool highBiasHigh = digitalRead(PIN_QH) == HIGH;
  digitalWrite(PIN_BIAS, LOW);
  if (!lowBiasHigh && !highBiasHigh) return QH_LOW;
  if (lowBiasHigh && highBiasHigh) return QH_HIGH;
  if (!lowBiasHigh && highBiasHigh) return QH_Z;
  return 3;
}

void expectLevel(int expected, const char* step) {
  settle();
  const int actual = readQh();
  if (actual != expected) noteFailure(step, expected, actual);
}

void shiftOutRestOf0xA5(const char* step) {
  const uint8_t afterH[] = {0, 1, 0, 0, 1, 0, 1};
  digitalWrite(PIN_SLOAD, HIGH);
  digitalWrite(PIN_SA, LOW);
  settle();
  for (uint8_t bit = 0; bit < 7; bit++) {
    pulse(PIN_SCK);
    expectLevel(afterH[bit] ? QH_HIGH : QH_LOW, step);
  }
}

void runChecks() {
  digitalWrite(PIN_OE, HIGH);
  expectLevel(QH_Z, "oe-high");

  digitalWrite(PIN_OE, LOW);
  loadParallel(0x00);
  expectLevel(QH_LOW, "load-zero");

  setParallel(0x80);
  digitalWrite(PIN_SLOAD, HIGH);
  settle();
  pulse(PIN_RCK);
  expectLevel(QH_LOW, "latch-holds-qh");

  digitalWrite(PIN_SLOAD, LOW);
  expectLevel(QH_HIGH, "sload-copies-h");

  digitalWrite(PIN_SA, HIGH);
  for (uint8_t clock = 0; clock < 8; clock++) pulse(PIN_SCK);
  expectLevel(QH_HIGH, "sck-ignored");

  loadParallel(0xA5);
  expectLevel(QH_HIGH, "load-a5-h");
  digitalWrite(PIN_SLOAD, HIGH);
  digitalWrite(PIN_SA, LOW);
  settle();
  digitalWrite(PIN_SCK, HIGH);
  expectLevel(QH_LOW, "sck-rising");
  expectLevel(QH_LOW, "sck-held");
  digitalWrite(PIN_SCK, LOW);
  expectLevel(QH_LOW, "sck-falling");
  const uint8_t afterFirst[] = {1, 0, 0, 1, 0, 1};
  for (uint8_t bit = 0; bit < 6; bit++) {
    pulse(PIN_SCK);
    expectLevel(afterFirst[bit] ? QH_HIGH : QH_LOW, "shift-a5");
  }
  pulse(PIN_SCK);
  expectLevel(QH_LOW, "shift-sa0");
  digitalWrite(PIN_SA, HIGH);
  settle();
  for (uint8_t clock = 0; clock < 7; clock++) {
    pulse(PIN_SCK);
    expectLevel(QH_LOW, "shift-sa1-delay");
  }
  pulse(PIN_SCK);
  expectLevel(QH_HIGH, "shift-sa1");

  loadParallel(0x00);
  expectLevel(QH_LOW, "both-preload");
  digitalWrite(PIN_SLOAD, HIGH);
  setParallel(0xA5);
  digitalWrite(PIN_SA, HIGH);
  settle();
  riseBothClocks();
  expectLevel(QH_LOW, "both-clocks-shift");
  digitalWrite(PIN_SLOAD, LOW);
  expectLevel(QH_HIGH, "both-clocks-latch");
  shiftOutRestOf0xA5("both-clocks-rest");

  loadParallel(0x80);
  digitalWrite(PIN_OE, LOW);
  expectLevel(QH_HIGH, "oe-before");
  digitalWrite(PIN_OE, HIGH);
  expectLevel(QH_Z, "oe-hides");
  digitalWrite(PIN_SLOAD, HIGH);
  digitalWrite(PIN_SA, LOW);
  settle();
  pulse(PIN_SCK);
  expectLevel(QH_Z, "oe-during-shift");
  digitalWrite(PIN_OE, LOW);
  expectLevel(QH_LOW, "oe-shift-continues");
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_SA, OUTPUT);
  pinMode(PIN_SLOAD, OUTPUT);
  pinMode(PIN_RCK, OUTPUT);
  pinMode(PIN_SCK, OUTPUT);
  pinMode(PIN_OE, OUTPUT);
  pinMode(PIN_QH, INPUT);
  pinMode(PIN_BIAS, OUTPUT);
  for (uint8_t bit = 0; bit < 8; bit++) pinMode(PIN_P[bit], OUTPUT);

  setParallel(0);
  digitalWrite(PIN_SA, LOW);
  digitalWrite(PIN_SLOAD, HIGH);
  digitalWrite(PIN_RCK, LOW);
  digitalWrite(PIN_SCK, LOW);
  digitalWrite(PIN_OE, HIGH);
  digitalWrite(PIN_BIAS, LOW);
  Serial.println("74HC589 ready. Send any character to start.");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  Serial.println(failed ? resultLine : "RESULT PASS");
}
