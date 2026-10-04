/*
 * Self-check for a 74HC1G14 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * A low input produces a high output, and a high input produces a low output.
 * Pin 1 stays open. The output is push-pull, so Y is read directly.
 * GPIO levels are 0 V and 5 V, so Schmitt-trigger thresholds are not checked.
 * A stays low until the check starts.
 */

const uint8_t PIN_A = 2;
const uint8_t PIN_Y = 3;

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

void drive(bool high, const char* step, bool yHigh) {
  digitalWrite(PIN_A, high ? HIGH : LOW);
  expectY(yHigh, step);
}

void setup() {
  pinMode(PIN_A, OUTPUT);
  pinMode(PIN_Y, INPUT);
  digitalWrite(PIN_A, LOW);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  drive(false, "A low", true);
  drive(true, "A high", false);

  for (uint8_t index = 0; index < 32; index++) {
    drive(false, "toggle low", true);
    drive(true, "toggle high", false);
  }
  drive(false, "final low", true);

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
