/*
 * Self-check for a 74HC368 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud. The sketch runs once after reset.
 * Send "r" to repeat. The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each Y pin has a 10k resistor to BIAS. A driven HC output overrides that
 * resistor. A high-impedance output follows BIAS, which is how Z is observed.
 *
 * Both OE pins are active low and control separate groups. OE1 enables Y1-Y4.
 * OE2 enables Y5 and Y6. A high OE releases only its own group. Enabled
 * outputs are the complement of their data inputs.
 */

const uint8_t PIN_OE1 = 2;
const uint8_t A_PINS[6] = {3, 4, 5, 6, 7, 8};
const uint8_t PIN_OE2 = 9;
const uint8_t Y_PINS[6] = {10, 11, 12, 13, A1, A2};
const uint8_t BIAS = A0;

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
  const int inverted = (~pattern) & 0x3F;
  const int bias = biasLevel ? 0x3F : 0x00;
  const bool oe1High = (pattern >> 6) & 1;
  const bool oe2High = (pattern >> 7) & 1;
  const int group1 = oe1High ? (bias & 0x0F) : (inverted & 0x0F);
  const int group2 = oe2High ? (bias & 0x30) : (inverted & 0x30);
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

void runBench() {
  failed = false;
  resultLine[0] = '\0';
  digitalWrite(BIAS, LOW);
  applyInputs(0xC0);
  checkEveryCombination();
  Serial.println(failed ? resultLine : "RESULT PASS");
  Serial.println("Send r to repeat.");
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
  digitalWrite(PIN_OE1, HIGH);
  digitalWrite(PIN_OE2, HIGH);
  digitalWrite(BIAS, LOW);

  Serial.println("74HC368 bench.");
  runBench();
}

void loop() {
  if (Serial.available() == 0) return;
  const char command = Serial.read();
  if (command == 'r' || command == 'R') {
    while (Serial.available() > 0) Serial.read();
    runBench();
  }
}
