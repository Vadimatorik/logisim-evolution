/*
 * Self-check for a 74HC4049 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Six independent inverters. Outputs are push-pull, so Y is read directly.
 * VCC is pin 1. Pins 13 and 16 stay open; pin 16 is not a supply pin.
 * Inputs stay low until the check starts. This sketch checks the logic
 * table at 5 V and does not exercise the 15 V input tolerance.
 *
 * Nano outputs D2..D7 drive 1A, 2A, 3A, 4A, 5A, 6A (pins 3, 5, 7, 9, 11, 14).
 * Nano inputs D8..D13 read 1Y, 2Y, 3Y, 4Y, 5Y, 6Y (pins 2, 4, 6, 10, 12, 15).
 */

const uint8_t INPUT_PINS[6] = {2, 3, 4, 5, 6, 7};
const uint8_t OUTPUT_PINS[6] = {8, 9, 10, 11, 12, 13};

bool failed = false;
char resultLine[96];

void noteFailure(uint8_t pattern, uint8_t expected, uint8_t actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL pattern %02X expected %02X got %02X",
      pattern,
      expected,
      actual);
}

void driveInputs(uint8_t code) {
  for (uint8_t channel = 0; channel < 6; channel++) {
    digitalWrite(INPUT_PINS[channel], (code >> channel) & 1);
  }
}

uint8_t readOutputs() {
  uint8_t value = 0;
  for (uint8_t channel = 0; channel < 6; channel++) {
    if (digitalRead(OUTPUT_PINS[channel]) == HIGH) value |= (uint8_t) (1u << channel);
  }
  return value;
}

void setup() {
  Serial.begin(115200);
  for (uint8_t channel = 0; channel < 6; channel++) {
    pinMode(INPUT_PINS[channel], OUTPUT);
    pinMode(OUTPUT_PINS[channel], INPUT);
  }
  driveInputs(0);
  Serial.println("74HC4049 ready. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();

  failed = false;
  for (uint8_t pattern = 0; pattern < 64; pattern++) {
    driveInputs(pattern);
    delay(1);
    const uint8_t expected = (uint8_t) ((~pattern) & 0x3F);
    const uint8_t actual = readOutputs();
    if (actual != expected) noteFailure(pattern, expected, actual);
  }
  driveInputs(0);

  if (!failed) {
    Serial.println("RESULT PASS");
  } else {
    Serial.println(resultLine);
  }
}
