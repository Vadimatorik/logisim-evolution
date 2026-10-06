/*
 * Self-check for a 74HC4050 wired to an Arduino Nano, as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * Each pattern is printed as IN/OUT/EXP. The last line is "PASS 64/64" or "FAIL n/64".
 *
 * The leftmost bit is channel 1 and the rightmost bit is channel 6.
 * Each output copies its own input. Pins 13 and 16 of the chip stay unconnected.
 * Supply is 5 V only: do not drive an input above the Nano's logic level.
 */

const uint8_t INPUT_PINS[] = {2, 3, 4, 5, 6, 7};
const uint8_t OUTPUT_PINS[] = {8, 9, 10, 11, 12, 13};
const uint8_t CHANNELS = 6;
const uint16_t PATTERN_COUNT = 64;

uint8_t failures = 0;

void settle() {
  delayMicroseconds(100);
}

void writeInputs(uint8_t pattern) {
  for (uint8_t channel = 0; channel < CHANNELS; channel++) {
    digitalWrite(INPUT_PINS[channel], ((pattern >> channel) & 1) ? HIGH : LOW);
  }
}

uint8_t readOutputs() {
  uint8_t value = 0;
  for (uint8_t channel = 0; channel < CHANNELS; channel++) {
    if (digitalRead(OUTPUT_PINS[channel]) == HIGH) {
      value |= (uint8_t)(1 << channel);
    }
  }
  return value;
}

void printBits(uint8_t value) {
  for (uint8_t channel = 0; channel < CHANNELS; channel++) {
    Serial.print(((value >> channel) & 1) ? '1' : '0');
  }
}

void report(uint8_t pattern, uint8_t actual) {
  const bool ok = actual == pattern;
  if (!ok) {
    failures++;
  }
  Serial.print("IN=");
  printBits(pattern);
  Serial.print(" OUT=");
  printBits(actual);
  Serial.print(" EXP=");
  printBits(pattern);
  Serial.println(ok ? " OK" : " FAIL");
}

void setup() {
  for (uint8_t channel = 0; channel < CHANNELS; channel++) {
    digitalWrite(INPUT_PINS[channel], LOW);
    pinMode(INPUT_PINS[channel], OUTPUT);
    pinMode(OUTPUT_PINS[channel], INPUT);
  }

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  for (uint16_t pattern = 0; pattern < PATTERN_COUNT; pattern++) {
    writeInputs((uint8_t)pattern);
    settle();
    report((uint8_t)pattern, readOutputs());
  }

  if (failures == 0) {
    Serial.println("PASS 64/64");
  } else {
    Serial.print("FAIL ");
    Serial.print(failures);
    Serial.println("/64");
  }
}

void loop() {
}
