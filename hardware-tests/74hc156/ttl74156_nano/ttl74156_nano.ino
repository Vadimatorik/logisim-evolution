/*
 * Self-check for a 74HC156 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * The outputs are open-drain. External 10k pull-ups make a released output read HIGH.
 * D13 is left unused because the board LED loads an open-drain pin.
 */

const uint8_t PIN_C1 = 2;
const uint8_t PIN_G1 = 3;
const uint8_t PIN_B = 4;
const uint8_t PIN_A = 5;
const uint8_t PIN_G2 = 6;
const uint8_t PIN_C2 = 7;

const uint8_t OUTPUTS[] = {8, 9, 10, 11, 12, A0, A1, A2};
const char* OUTPUT_NAMES[] = {"1Y0", "1Y1", "1Y2", "1Y3", "2Y0", "2Y1", "2Y2", "2Y3"};
const uint8_t OUTPUT_COUNT = sizeof(OUTPUTS) / sizeof(OUTPUTS[0]);

bool failed = false;
char resultLine[160];

void noteFailure(uint8_t vector, const char* name, int expected, int actual) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine),
           "RESULT FAIL C1=%d 1G=%d B=%d A=%d 2G=%d 2C=%d output=%s expected=%d actual=%d",
           vector & 1, (vector >> 1) & 1, (vector >> 2) & 1, (vector >> 3) & 1,
           (vector >> 4) & 1, (vector >> 5) & 1, name, expected, actual);
}

int expectedLevel(uint8_t vector, uint8_t outputIndex) {
  const int c1 = vector & 1;
  const int g1 = (vector >> 1) & 1;
  const int b = (vector >> 2) & 1;
  const int a = (vector >> 3) & 1;
  const int g2 = (vector >> 4) & 1;
  const int c2 = (vector >> 5) & 1;
  const int selected = a + 2 * b;
  const bool section1 = g1 == 0 && c1 == 1;
  const bool section2 = g2 == 0 && c2 == 0;
  if (outputIndex < 4) return (section1 && selected == outputIndex) ? 0 : 1;
  return (section2 && selected == (outputIndex - 4)) ? 0 : 1;
}

void drive(uint8_t vector) {
  digitalWrite(PIN_C1, (vector & 0x01) ? HIGH : LOW);
  digitalWrite(PIN_G1, (vector & 0x02) ? HIGH : LOW);
  digitalWrite(PIN_B, (vector & 0x04) ? HIGH : LOW);
  digitalWrite(PIN_A, (vector & 0x08) ? HIGH : LOW);
  digitalWrite(PIN_G2, (vector & 0x10) ? HIGH : LOW);
  digitalWrite(PIN_C2, (vector & 0x20) ? HIGH : LOW);
}

void checkVector(uint8_t vector) {
  drive(vector);
  delay(1);
  Serial.print("C1=");
  Serial.print(vector & 1);
  Serial.print(" 1G=");
  Serial.print((vector >> 1) & 1);
  Serial.print(" B=");
  Serial.print((vector >> 2) & 1);
  Serial.print(" A=");
  Serial.print((vector >> 3) & 1);
  Serial.print(" 2G=");
  Serial.print((vector >> 4) & 1);
  Serial.print(" 2C=");
  Serial.print((vector >> 5) & 1);
  bool vectorFailed = false;
  for (uint8_t i = 0; i < OUTPUT_COUNT; i++) {
    const int expected = expectedLevel(vector, i);
    const int actual = digitalRead(OUTPUTS[i]) == HIGH ? 1 : 0;
    Serial.print(" ");
    Serial.print(OUTPUT_NAMES[i]);
    Serial.print(expected == actual ? "=" : "!=");
    Serial.print(actual ? "H" : "L");
    if (expected != actual) {
      vectorFailed = true;
      noteFailure(vector, OUTPUT_NAMES[i], expected, actual);
    }
  }
  Serial.println(vectorFailed ? " FAIL" : " PASS");
}

void runTest() {
  failed = false;
  resultLine[0] = '\0';
  Serial.println("74HC156 test");
  for (uint8_t vector = 0; vector < 64; vector++) checkVector(vector);
  drive(0);
  Serial.println(failed ? resultLine : "RESULT PASS");
}

void setup() {
  Serial.begin(115200);
  const uint8_t inputs[] = {PIN_C1, PIN_G1, PIN_B, PIN_A, PIN_G2, PIN_C2};
  const uint8_t inputCount = sizeof(inputs) / sizeof(inputs[0]);
  for (uint8_t i = 0; i < inputCount; i++) {
    pinMode(inputs[i], OUTPUT);
    digitalWrite(inputs[i], LOW);
  }
  for (uint8_t i = 0; i < OUTPUT_COUNT; i++) pinMode(OUTPUTS[i], INPUT);
  Serial.println("74HC156 ready. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  runTest();
}
