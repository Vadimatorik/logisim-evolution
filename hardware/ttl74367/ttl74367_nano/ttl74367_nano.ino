/*
 * Self-check for a 74HC367 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each Y pin has a 10k resistor to BIAS. A driven HC output overrides that
 * resistor. A high-impedance output follows BIAS, which is how Z is observed.
 *
 * Both OE pins are active low and control separate groups. OE1 enables Y1-Y4.
 * OE2 enables Y5 and Y6. A high OE releases only its own group.
 */

const uint8_t PIN_OE1 = 2;
const uint8_t A_PINS[6] = {3, 5, 7, 10, 12, A2};
const uint8_t Y_PINS[6] = {4, 6, 8, 9, 11, A1};
const uint8_t PIN_OE2 = A3;
const uint8_t BIAS = A4;

bool failed = false;
char resultLine[160];

void noteFailure(const char* step, int expected, int actual) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s expected=0x%02X actual=0x%02X", step,
           expected, actual);
}

void applyInputs(int pattern) {
  for (uint8_t bit = 0; bit < 6; bit++) {
    digitalWrite(A_PINS[bit], (pattern >> bit) & 1 ? HIGH : LOW);
  }
  digitalWrite(PIN_OE1, (pattern >> 6) & 1 ? HIGH : LOW);
  digitalWrite(PIN_OE2, (pattern >> 7) & 1 ? HIGH : LOW);
}

int readY() {
  int value = 0;
  for (uint8_t bit = 0; bit < 6; bit++) {
    if (digitalRead(Y_PINS[bit]) == HIGH) value |= 1 << bit;
  }
  return value;
}

int expectedY(int pattern, int biasLevel) {
  const int data = pattern & 0x3F;
  const int bias = biasLevel ? 0x3F : 0x00;
  const bool oe1High = (pattern >> 6) & 1;
  const bool oe2High = (pattern >> 7) & 1;
  const int group1 = oe1High ? (bias & 0x0F) : (data & 0x0F);
  const int group2 = oe2High ? (bias & 0x30) : (data & 0x30);
  return group1 | group2;
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
  for (int pattern = 0; pattern < 256; pattern++) {
    applyInputs(pattern);
    for (int biasLevel = 0; biasLevel <= 1; biasLevel++) {
      digitalWrite(BIAS, biasLevel ? HIGH : LOW);
      char step[32];
      snprintf(step, sizeof(step), "in=0x%02X bias=%d", pattern, biasLevel);
      expectY(step, expectedY(pattern, biasLevel));
    }
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_OE1, OUTPUT);
  pinMode(PIN_OE2, OUTPUT);
  pinMode(BIAS, OUTPUT);
  for (uint8_t bit = 0; bit < 6; bit++) {
    pinMode(A_PINS[bit], OUTPUT);
    pinMode(Y_PINS[bit], INPUT);
  }
  digitalWrite(BIAS, LOW);
  applyInputs(0xC0);

  Serial.println("74HC367 bench. Send any character to start.");
  while (Serial.available() == 0) {
  }
  while (Serial.available() > 0) {
    Serial.read();
  }

  checkEveryCombination();
  Serial.println(failed ? resultLine : "RESULT PASS");
}

void loop() {}
