/*
 * Self-check for a 74HC4078 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Y is the OR of A through H. X is the NOR of the same inputs.
 * Outputs are push-pull, so Y and X are read directly.
 * Every input stays low until the check starts.
 */

const uint8_t PIN_A = 2;
const uint8_t PIN_B = 3;
const uint8_t PIN_C = 4;
const uint8_t PIN_D = 5;
const uint8_t PIN_E = 6;
const uint8_t PIN_F = 7;
const uint8_t PIN_G = 8;
const uint8_t PIN_H = 9;
const uint8_t PIN_Y = 10;
const uint8_t PIN_X = 11;

const uint8_t INPUTS[] = {PIN_A, PIN_B, PIN_C, PIN_D, PIN_E, PIN_F, PIN_G, PIN_H};

bool failed = false;
char resultLine[96];

void noteFailure(uint8_t code, char outputName, uint8_t expected, uint8_t actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL code %02X %c expected %u got %u",
      code,
      outputName,
      expected,
      actual);
}

void settle() { delay(1); }

void writeInputs(uint8_t code) {
  for (uint8_t bit = 0; bit < 8; bit++) {
    digitalWrite(INPUTS[bit], (code & (1 << bit)) ? HIGH : LOW);
  }
}

void expectOutputs(uint8_t code) {
  settle();
  const uint8_t anyHigh = code != 0;
  const uint8_t actualY = digitalRead(PIN_Y) ? 1 : 0;
  const uint8_t actualX = digitalRead(PIN_X) ? 1 : 0;
  if (actualY != anyHigh) noteFailure(code, 'Y', anyHigh, actualY);
  if (actualX != (anyHigh ? 0 : 1)) noteFailure(code, 'X', anyHigh ? 0 : 1, actualX);
}

void setup() {
  for (uint8_t bit = 0; bit < 8; bit++) {
    pinMode(INPUTS[bit], OUTPUT);
    digitalWrite(INPUTS[bit], LOW);
  }
  pinMode(PIN_Y, INPUT);
  pinMode(PIN_X, INPUT);
  Serial.begin(115200);
  Serial.println(F("74HC4078: send any character to start"));
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();

  failed = false;
  resultLine[0] = '\0';
  for (int code = 0; code < 256; code++) {
    writeInputs((uint8_t) code);
    expectOutputs((uint8_t) code);
  }
  writeInputs(0);

  if (!failed) Serial.println(F("RESULT PASS"));
  else Serial.println(resultLine);
}
