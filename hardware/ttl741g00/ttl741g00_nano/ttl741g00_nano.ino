/*
 * Self-check for a 74HC1G00 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Y is low only when both A and B are high. The output is push-pull, so Y is
 * read directly. A and B stay low until the check starts.
 */

const uint8_t PIN_A = 2;
const uint8_t PIN_B = 3;
const uint8_t PIN_Y = 4;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, uint8_t expected, uint8_t actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s expected %u got %u",
      step,
      expected,
      actual);
}

void settle() { delay(1); }

void expectY(bool high, const char* step) {
  settle();
  const uint8_t actual = digitalRead(PIN_Y) ? 1 : 0;
  const uint8_t expected = high ? 1 : 0;
  if (actual != expected) noteFailure(step, expected, actual);
}

void drive(bool aHigh, bool bHigh, const char* step) {
  digitalWrite(PIN_A, aHigh ? HIGH : LOW);
  digitalWrite(PIN_B, bHigh ? HIGH : LOW);
  expectY(!(aHigh && bHigh), step);
}

void setup() {
  pinMode(PIN_A, OUTPUT);
  pinMode(PIN_B, OUTPUT);
  pinMode(PIN_Y, INPUT);
  digitalWrite(PIN_A, LOW);
  digitalWrite(PIN_B, LOW);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  drive(false, false, "A low B low");
  drive(false, true, "A low B high");
  drive(true, false, "A high B low");
  drive(true, true, "A high B high");

  for (uint8_t index = 0; index < 8; index++) {
    drive(false, false, "repeat low low");
    drive(false, true, "repeat low high");
    drive(true, false, "repeat high low");
    drive(true, true, "repeat high high");
  }

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
