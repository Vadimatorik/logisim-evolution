/*
 * Self-check for a 74HC147 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Inputs I1..I9 and outputs Y0..Y3 are active low. I9 has the highest
 * priority. The outputs are the complement of the BCD number of the highest
 * asserted input. All inputs high encodes decimal 0, so every output is high.
 * Outputs are push-pull, so the Y pins are read directly. Pin 15 stays open.
 * Every input stays high until the check starts.
 */

const uint8_t PIN_I4 = 2;
const uint8_t PIN_I5 = 3;
const uint8_t PIN_I6 = 4;
const uint8_t PIN_I7 = 5;
const uint8_t PIN_I8 = 6;
const uint8_t PIN_Y2 = 7;
const uint8_t PIN_Y1 = 8;
const uint8_t PIN_Y3 = 9;
const uint8_t PIN_I3 = 10;
const uint8_t PIN_I2 = 11;
const uint8_t PIN_I1 = 12;
const uint8_t PIN_I9 = 13;
const uint8_t PIN_Y0 = A0;

const uint8_t INPUT_PINS[9] = {
  PIN_I1, PIN_I2, PIN_I3, PIN_I4, PIN_I5, PIN_I6, PIN_I7, PIN_I8, PIN_I9
};

bool failed = false;
char resultLine[96];

void noteFailure(uint16_t inputs, uint8_t expected, uint8_t actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL inputs %03X expected %X got %X",
      inputs,
      expected,
      actual);
}

void settle() { delay(1); }

void driveInputs(uint16_t asserted) {
  for (uint8_t index = 0; index < 9; index++) {
    const bool active = (asserted & (1u << index)) != 0;
    digitalWrite(INPUT_PINS[index], active ? LOW : HIGH);
  }
}

void holdInputsHigh() { driveInputs(0); }

uint8_t readCode() {
  uint8_t code = 0;
  if (digitalRead(PIN_Y0) == LOW) code |= 1;
  if (digitalRead(PIN_Y1) == LOW) code |= 2;
  if (digitalRead(PIN_Y2) == LOW) code |= 4;
  if (digitalRead(PIN_Y3) == LOW) code |= 8;
  return code;
}

uint8_t expectedCode(uint16_t asserted) {
  for (int number = 9; number >= 1; number--) {
    if ((asserted & (1u << (number - 1))) != 0) return static_cast<uint8_t>(number);
  }
  return 0;
}

void expect(uint16_t inputs, uint8_t expected) {
  settle();
  const uint8_t actual = readCode();
  if (actual != expected) noteFailure(inputs, expected, actual);
}

void runChecks() {
  for (uint16_t asserted = 0; asserted < 512; asserted++) {
    driveInputs(asserted);
    expect(asserted, expectedCode(asserted));
  }
  holdInputsHigh();
}

void setup() {
  Serial.begin(115200);
  const uint8_t outputs[] = {
    PIN_I1, PIN_I2, PIN_I3, PIN_I4, PIN_I5, PIN_I6, PIN_I7, PIN_I8, PIN_I9
  };
  for (uint8_t index = 0; index < 9; index++) {
    digitalWrite(outputs[index], HIGH);
    pinMode(outputs[index], OUTPUT);
  }
  pinMode(PIN_Y0, INPUT);
  pinMode(PIN_Y1, INPUT);
  pinMode(PIN_Y2, INPUT);
  pinMode(PIN_Y3, INPUT);
  holdInputsHigh();
  Serial.println("74HC147 ready. Send any character to start.");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  Serial.println(failed ? resultLine : "RESULT PASS");
}
