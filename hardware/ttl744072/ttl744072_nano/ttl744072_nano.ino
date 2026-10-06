/*
 * Self-check for a 74HC4072 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each half is a 4-input OR gate. The output is high when any input is high
 * and low only when all four are low. Outputs are push-pull, so 1Y and 2Y
 * are read directly. All inputs stay low until the check starts.
 */

const uint8_t PIN_1A = 2;
const uint8_t PIN_1B = 3;
const uint8_t PIN_1C = 4;
const uint8_t PIN_1D = 5;
const uint8_t PIN_2A = 6;
const uint8_t PIN_2B = 7;
const uint8_t PIN_2C = 8;
const uint8_t PIN_2D = 9;
const uint8_t PIN_1Y = 10;
const uint8_t PIN_2Y = 11;

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

void setInputs(uint8_t mask1, uint8_t mask2) {
  digitalWrite(PIN_1A, (mask1 & 1) ? HIGH : LOW);
  digitalWrite(PIN_1B, (mask1 & 2) ? HIGH : LOW);
  digitalWrite(PIN_1C, (mask1 & 4) ? HIGH : LOW);
  digitalWrite(PIN_1D, (mask1 & 8) ? HIGH : LOW);
  digitalWrite(PIN_2A, (mask2 & 1) ? HIGH : LOW);
  digitalWrite(PIN_2B, (mask2 & 2) ? HIGH : LOW);
  digitalWrite(PIN_2C, (mask2 & 4) ? HIGH : LOW);
  digitalWrite(PIN_2D, (mask2 & 8) ? HIGH : LOW);
}

void expectBoth(uint8_t mask1, uint8_t mask2) {
  settle();
  const uint8_t expected = (mask1 != 0 ? 1 : 0) | (mask2 != 0 ? 2 : 0);
  uint8_t actual = 0;
  if (digitalRead(PIN_1Y) == HIGH) actual |= 1;
  if (digitalRead(PIN_2Y) == HIGH) actual |= 2;
  if (actual == expected) return;
  char step[32];
  snprintf(step, sizeof(step), "pattern %02X %02X", mask1, mask2);
  noteFailure(step, expected, actual);
}

void checkAllPatterns() {
  for (uint16_t pattern = 0; pattern < 256; pattern++) {
    const uint8_t mask1 = pattern & 0x0F;
    const uint8_t mask2 = pattern >> 4;
    setInputs(mask1, mask2);
    expectBoth(mask1, mask2);
  }
  setInputs(0, 0);
}

void setup() {
  pinMode(PIN_1A, OUTPUT);
  pinMode(PIN_1B, OUTPUT);
  pinMode(PIN_1C, OUTPUT);
  pinMode(PIN_1D, OUTPUT);
  pinMode(PIN_2A, OUTPUT);
  pinMode(PIN_2B, OUTPUT);
  pinMode(PIN_2C, OUTPUT);
  pinMode(PIN_2D, OUTPUT);
  pinMode(PIN_1Y, INPUT);
  pinMode(PIN_2Y, INPUT);
  setInputs(0, 0);

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
