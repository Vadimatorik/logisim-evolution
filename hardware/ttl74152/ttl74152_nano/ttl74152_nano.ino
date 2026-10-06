/*
 * Self-check for a 74HC152 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * A is the least significant select bit and C is the most significant.
 * Address CBA selects D0 through D7. W follows that input and is not inverted.
 * The output is push-pull, so W is read directly.
 * Every chip input stays low until the check starts.
 */

const uint8_t PIN_D4 = 2;
const uint8_t PIN_D3 = 3;
const uint8_t PIN_D2 = 4;
const uint8_t PIN_D1 = 5;
const uint8_t PIN_D0 = 6;
const uint8_t PIN_W = A0;
const uint8_t PIN_C = 7;
const uint8_t PIN_B = 8;
const uint8_t PIN_A = 9;
const uint8_t PIN_D7 = 10;
const uint8_t PIN_D6 = 11;
const uint8_t PIN_D5 = 12;

const uint8_t DATA_PINS[8] = {
    PIN_D0, PIN_D1, PIN_D2, PIN_D3, PIN_D4, PIN_D5, PIN_D6, PIN_D7};
const uint8_t SELECT_PINS[3] = {PIN_A, PIN_B, PIN_C};

bool failed = false;
char resultLine[96];

void noteFailure(uint16_t pattern, uint8_t expected, uint8_t actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL pattern %03X expected %u got %u",
      pattern,
      expected,
      actual);
}

void settle() { delay(1); }

void apply(uint16_t pattern) {
  for (uint8_t index = 0; index < 8; index++) {
    digitalWrite(DATA_PINS[index], (pattern & (1 << index)) ? HIGH : LOW);
  }
  for (uint8_t index = 0; index < 3; index++) {
    digitalWrite(SELECT_PINS[index], (pattern & (1 << (8 + index))) ? HIGH : LOW);
  }
}

uint8_t expectedBit(uint16_t pattern) {
  const uint8_t data = pattern & 0xFF;
  const uint8_t address = pattern >> 8;
  return (data >> address) & 1;
}

void checkAllPatterns() {
  for (uint16_t pattern = 0; pattern < 2048; pattern++) {
    apply(pattern);
    settle();
    const uint8_t expected = expectedBit(pattern);
    const uint8_t actual = digitalRead(PIN_W) ? 1 : 0;
    if (actual != expected) {
      noteFailure(pattern, expected, actual);
      return;
    }
  }
}

void setup() {
  for (uint8_t index = 0; index < 8; index++) {
    pinMode(DATA_PINS[index], OUTPUT);
    digitalWrite(DATA_PINS[index], LOW);
  }
  for (uint8_t index = 0; index < 3; index++) {
    pinMode(SELECT_PINS[index], OUTPUT);
    digitalWrite(SELECT_PINS[index], LOW);
  }
  pinMode(PIN_W, INPUT);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  checkAllPatterns();
  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
