/*
 * Self-check for a 74HC133 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Y is low only when every input A through M is high. The output is
 * push-pull, so Y is read directly. All inputs stay low until the check
 * starts.
 */

const uint8_t INPUTS[] = {2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, A0, A1};
const uint8_t INPUT_COUNT = sizeof(INPUTS) / sizeof(INPUTS[0]);
const uint8_t PIN_Y = 9;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, uint8_t expected, uint8_t actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s expected %02X got %02X",
      step,
      expected,
      actual);
}

void settle() { delay(1); }

void setAll(uint8_t level) {
  for (uint8_t index = 0; index < INPUT_COUNT; index++) {
    digitalWrite(INPUTS[index], level);
  }
}

void expectY(bool high, const char* step) {
  settle();
  const uint8_t actual = digitalRead(PIN_Y) ? 1 : 0;
  const uint8_t expected = high ? 1 : 0;
  if (actual != expected) noteFailure(step, expected, actual);
}

void checkAllHigh() {
  setAll(HIGH);
  expectY(false, "all high");
}

void checkEachInputLow() {
  const char names[] = "ABCDEFGHIJKLM";
  char step[16];
  for (uint8_t index = 0; index < INPUT_COUNT; index++) {
    setAll(HIGH);
    digitalWrite(INPUTS[index], LOW);
    snprintf(step, sizeof(step), "%c low", names[index]);
    expectY(true, step);
  }
}

void checkAllLow() {
  setAll(LOW);
  expectY(true, "all low");
}

void setup() {
  for (uint8_t index = 0; index < INPUT_COUNT; index++) {
    pinMode(INPUTS[index], OUTPUT);
  }
  pinMode(PIN_Y, INPUT);
  setAll(LOW);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  checkAllHigh();
  checkEachInputLow();
  checkAllLow();

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
