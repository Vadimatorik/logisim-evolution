/*
 * Self-check for a 74HC258 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * S low selects nI0, S high selects nI1, and nY is the inverted selected bit.
 * OE high releases every output. Each nY has its own 10k resistor to the pull
 * bus on A3. A pin that follows the bus is high-impedance; a pin that holds
 * one level is driving it. Do not enable the Nano internal pull-ups on nY.
 * OE stays high until the check starts.
 */

const uint8_t PIN_S = 2;
const uint8_t PIN_I0[4] = {3, 6, 11, A1};
const uint8_t PIN_I1[4] = {4, 7, 10, A0};
const uint8_t PIN_Y[4] = {5, 8, 9, 12};
const uint8_t PIN_OE = A2;
const uint8_t PIN_PULL = A3;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, const char* expected, const char* actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s expected %s got %s",
      step,
      expected,
      actual);
}

void settle() { delay(1); }

void setBit(uint8_t pin, bool high) { digitalWrite(pin, high ? HIGH : LOW); }

void setSources(uint8_t source0, uint8_t source1) {
  for (uint8_t channel = 0; channel < 4; channel++) {
    setBit(PIN_I0[channel], source0 & (1u << channel));
    setBit(PIN_I1[channel], source1 & (1u << channel));
  }
}

void invertedPattern(uint8_t bits, char out[5]) {
  for (uint8_t channel = 0; channel < 4; channel++) {
    const bool high = bits & (1u << channel);
    out[channel] = high ? 'L' : 'H';
  }
  out[4] = '\0';
}

void readOutputs(char actual[5]) {
  bool whenPullHigh[4];
  digitalWrite(PIN_PULL, HIGH);
  settle();
  for (uint8_t channel = 0; channel < 4; channel++) {
    whenPullHigh[channel] = digitalRead(PIN_Y[channel]);
  }
  digitalWrite(PIN_PULL, LOW);
  settle();
  for (uint8_t channel = 0; channel < 4; channel++) {
    const bool whenPullLow = digitalRead(PIN_Y[channel]);
    if (whenPullHigh[channel] && whenPullLow) actual[channel] = 'H';
    else if (!whenPullHigh[channel] && !whenPullLow) actual[channel] = 'L';
    else if (whenPullHigh[channel] && !whenPullLow) actual[channel] = 'Z';
    else actual[channel] = '?';
  }
  actual[4] = '\0';
}

void expect(const char* expected, const char* step) {
  char actual[5];
  readOutputs(actual);
  if (strcmp(actual, expected) != 0) noteFailure(step, expected, actual);
}

void runChecks() {
  digitalWrite(PIN_OE, HIGH);
  for (uint8_t mask = 0; mask < 16; mask++) {
    setSources(mask, static_cast<uint8_t>(~mask));
    digitalWrite(PIN_S, LOW);
    expect("ZZZZ", "oe-high-s-low");
    digitalWrite(PIN_S, HIGH);
    expect("ZZZZ", "oe-high-s-high");
  }

  digitalWrite(PIN_OE, LOW);
  for (uint8_t mask = 0; mask < 16; mask++) {
    const uint8_t other = static_cast<uint8_t>(~mask);
    char expected[5];
    setSources(mask, other);
    digitalWrite(PIN_S, LOW);
    invertedPattern(mask, expected);
    expect(expected, "select-i0");
    digitalWrite(PIN_S, HIGH);
    invertedPattern(other, expected);
    expect(expected, "select-i1");
  }

  digitalWrite(PIN_OE, HIGH);
  expect("ZZZZ", "oe-release");
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_S, OUTPUT);
  pinMode(PIN_OE, OUTPUT);
  pinMode(PIN_PULL, OUTPUT);
  for (uint8_t channel = 0; channel < 4; channel++) {
    pinMode(PIN_I0[channel], OUTPUT);
    pinMode(PIN_I1[channel], OUTPUT);
    pinMode(PIN_Y[channel], INPUT);
  }
  digitalWrite(PIN_S, LOW);
  setSources(0, 0);
  digitalWrite(PIN_OE, HIGH);
  digitalWrite(PIN_PULL, LOW);
  Serial.println("74HC258 ready. Send any character to start.");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  Serial.println(failed ? resultLine : "RESULT PASS");
}
