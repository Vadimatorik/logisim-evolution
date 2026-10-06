/*
 * Self-check for an SN7416 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each gate is an inverting open-collector buffer. A low input releases the
 * pin, so the 10k pull-up makes it read high. A high input sinks the pin.
 * Inputs stay high until the check starts, so the outputs are driven low
 * instead of floating.
 */

const uint8_t INPUTS[] = {2, 3, 4, 5, 6, 7};
const uint8_t OUTPUTS[] = {8, 9, 10, 11, 12, A0};
const uint8_t CHANNELS = 6;

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

void setInputs(uint8_t mask) {
  for (uint8_t channel = 0; channel < CHANNELS; channel++) {
    digitalWrite(INPUTS[channel], (mask & (1 << channel)) ? HIGH : LOW);
  }
}

uint8_t readOutputs() {
  uint8_t value = 0;
  for (uint8_t channel = 0; channel < CHANNELS; channel++) {
    if (digitalRead(OUTPUTS[channel])) value |= (1 << channel);
  }
  return value;
}

void expectOutputs(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readOutputs();
  if (actual != expected) noteFailure(step, expected, actual);
}

void checkAllLow() {
  setInputs(0x00);
  expectOutputs(0x3F, "all low");
}

void checkAllHigh() {
  setInputs(0x3F);
  expectOutputs(0x00, "all high");
}

void checkWalkingZero() {
  for (uint8_t channel = 0; channel < CHANNELS; channel++) {
    char step[16];
    snprintf(step, sizeof(step), "walk0 %u", channel);
    setInputs(0x3F ^ (1 << channel));
    expectOutputs(1 << channel, step);
  }
}

void checkWalkingOne() {
  for (uint8_t channel = 0; channel < CHANNELS; channel++) {
    char step[16];
    snprintf(step, sizeof(step), "walk1 %u", channel);
    setInputs(1 << channel);
    expectOutputs(0x3F ^ (1 << channel), step);
  }
}

void setup() {
  Serial.begin(115200);
  for (uint8_t channel = 0; channel < CHANNELS; channel++) {
    digitalWrite(INPUTS[channel], HIGH);
    pinMode(INPUTS[channel], OUTPUT);
    pinMode(OUTPUTS[channel], INPUT);
  }
  Serial.println("7416 bench ready. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();

  failed = false;
  checkAllLow();
  checkAllHigh();
  checkWalkingZero();
  checkWalkingOne();

  if (!failed) {
    Serial.println("RESULT PASS");
  } else {
    Serial.println(resultLine);
  }
}
