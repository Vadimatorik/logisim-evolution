/*
 * Self-check for a 74HC260 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each output is high only when all five inputs of that NOR gate are low.
 * Outputs are push-pull, so no bias resistor is used.
 *
 * Gate 1 inputs are pins 1, 2, 3, 12 and 13. Gate 2 inputs are pins 8, 9, 10,
 * 11 and 4. 1Y is pin 5 and 2Y is pin 6.
 */

const uint8_t GATE1_IN[5] = {2, 3, 4, 5, 6};
const uint8_t GATE2_IN[5] = {7, 8, 9, 10, 11};
const uint8_t PIN_1Y = 12;
const uint8_t PIN_2Y = A0;

bool failed = false;
char resultLine[160];

void noteFailure(const char* step, int expected, int actual) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s expected=0x%02X actual=0x%02X", step,
           expected, actual);
}

void applyInputs(int pattern) {
  for (uint8_t bit = 0; bit < 5; bit++) {
    digitalWrite(GATE1_IN[bit], (pattern >> bit) & 1 ? HIGH : LOW);
    digitalWrite(GATE2_IN[bit], (pattern >> (bit + 5)) & 1 ? HIGH : LOW);
  }
}

int readY() {
  int value = 0;
  if (digitalRead(PIN_1Y) == HIGH) value |= 1;
  if (digitalRead(PIN_2Y) == HIGH) value |= 2;
  return value;
}

int expectedY(int pattern) {
  int value = 0;
  if ((pattern & 0x1F) == 0) value |= 1;
  if (((pattern >> 5) & 0x1F) == 0) value |= 2;
  return value;
}

void expectY(const char* step, int expected) {
  delay(1);
  const int actual = readY();
  Serial.print(step);
  Serial.print(" expected=0x");
  Serial.print(expected, HEX);
  Serial.print(" actual=0x");
  Serial.print(actual, HEX);
  Serial.println(expected == actual ? " PASS" : " FAIL");
  if (expected != actual) noteFailure(step, expected, actual);
}

void checkEveryCombination() {
  for (int pattern = 0; pattern < 1024; pattern++) {
    applyInputs(pattern);
    char step[24];
    snprintf(step, sizeof(step), "in=0x%03X", pattern);
    expectY(step, expectedY(pattern));
  }
}

void setup() {
  Serial.begin(115200);
  for (uint8_t bit = 0; bit < 5; bit++) {
    pinMode(GATE1_IN[bit], OUTPUT);
    pinMode(GATE2_IN[bit], OUTPUT);
  }
  pinMode(PIN_1Y, INPUT);
  pinMode(PIN_2Y, INPUT);
  applyInputs(0x3FF);

  Serial.println("74HC260 bench. Send any character to start.");
  while (Serial.available() == 0) {
  }
  while (Serial.available() > 0) {
    Serial.read();
  }

  checkEveryCombination();
  Serial.println(failed ? resultLine : "RESULT PASS");
}

void loop() {}
