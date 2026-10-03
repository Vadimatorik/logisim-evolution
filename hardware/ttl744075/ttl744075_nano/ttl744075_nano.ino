/*
 * Self-check for a 74HC4075 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each output is the OR of its three inputs. Output is LOW only when all
 * three inputs of that gate are LOW.
 */

const uint8_t PIN_2A = 2;
const uint8_t PIN_2B = 3;
const uint8_t PIN_1A = 4;
const uint8_t PIN_1B = 5;
const uint8_t PIN_1C = 6;
const uint8_t PIN_2C = 7;
const uint8_t PIN_3A = 8;
const uint8_t PIN_3B = 9;
const uint8_t PIN_3C = 10;
const uint8_t PIN_1Y = 11;
const uint8_t PIN_2Y = 12;
const uint8_t PIN_3Y = 13;

const uint8_t INPUTS[9] = {
  PIN_1A, PIN_1B, PIN_1C, PIN_2A, PIN_2B, PIN_2C, PIN_3A, PIN_3B, PIN_3C};

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, uint16_t pattern, uint8_t actual, uint8_t expected) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine),
           "RESULT FAIL %s pattern=%u actual=%u expected=%u", step, pattern, actual, expected);
}

void settle() { delayMicroseconds(50); }

void writeInputs(uint16_t pattern) {
  for (uint8_t bit = 0; bit < 9; bit++) {
    digitalWrite(INPUTS[bit], (pattern & (1u << bit)) ? HIGH : LOW);
  }
}

uint8_t readOutputs() {
  settle();
  uint8_t actual = 0;
  if (digitalRead(PIN_1Y) == HIGH) actual |= 1;
  if (digitalRead(PIN_2Y) == HIGH) actual |= 2;
  if (digitalRead(PIN_3Y) == HIGH) actual |= 4;
  return actual;
}

uint8_t expectedOutputs(uint16_t pattern) {
  uint8_t expected = 0;
  if ((pattern & 0x007) != 0) expected |= 1;
  if ((pattern & 0x038) != 0) expected |= 2;
  if ((pattern & 0x1C0) != 0) expected |= 4;
  return expected;
}

void checkAllPatterns() {
  for (uint16_t pattern = 0; pattern < 512 && !failed; pattern++) {
    writeInputs(pattern);
    const uint8_t actual = readOutputs();
    const uint8_t expected = expectedOutputs(pattern);
    if (actual != expected) noteFailure("or", pattern, actual, expected);
    if ((pattern & 0x7F) == 0) {
      Serial.print("pattern ");
      Serial.println(pattern);
    }
  }
}

void setup() {
  for (uint8_t bit = 0; bit < 9; bit++) {
    pinMode(INPUTS[bit], OUTPUT);
    digitalWrite(INPUTS[bit], LOW);
  }
  pinMode(PIN_1Y, INPUT);
  pinMode(PIN_2Y, INPUT);
  pinMode(PIN_3Y, INPUT);

  Serial.begin(115200);
  Serial.println("READY 74HC4075, send a character");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  failed = false;
  checkAllPatterns();
  if (!failed) Serial.println("RESULT PASS");
  else Serial.println(resultLine);
}
