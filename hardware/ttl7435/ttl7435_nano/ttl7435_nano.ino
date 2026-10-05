/*
 * Self-check for a 74HC35 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * The outputs are open-collector. External 10k pull-ups make a released output
 * read HIGH. The pins are also INPUT_PULLUP. D13 is left unused because the
 * board LED loads an open-collector pin.
 */

const uint8_t INPUTS[] = {2, 4, 6, 8, 10, 12};
const uint8_t OUTPUTS[] = {3, 5, 7, 9, 11, A0};
const char* NAMES[] = {"1", "2", "3", "4", "5", "6"};
const uint8_t CHANNELS = sizeof(INPUTS) / sizeof(INPUTS[0]);

bool failed = false;
char resultLine[96];

void noteFailure(uint8_t vector, const char* name, int expected, int actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL vector=%02X channel=%s expected=%d actual=%d",
      vector,
      name,
      expected,
      actual);
}

void drive(uint8_t vector) {
  for (uint8_t channel = 0; channel < CHANNELS; channel++) {
    digitalWrite(INPUTS[channel], (vector & (1 << channel)) ? HIGH : LOW);
  }
}

void checkVector(uint8_t vector) {
  drive(vector);
  delay(1);
  bool vectorFailed = false;
  for (uint8_t channel = 0; channel < CHANNELS; channel++) {
    const int expected = (vector & (1 << channel)) ? 1 : 0;
    const int actual = digitalRead(OUTPUTS[channel]) == HIGH ? 1 : 0;
    Serial.print(NAMES[channel]);
    Serial.print("A=");
    Serial.print(expected ? "H" : "L");
    Serial.print(" ");
    Serial.print(NAMES[channel]);
    Serial.print("Y");
    Serial.print(expected == actual ? "=" : "!=");
    Serial.print(actual ? "H" : "L");
    Serial.print(" ");
    if (expected != actual) {
      vectorFailed = true;
      noteFailure(vector, NAMES[channel], expected, actual);
    }
  }
  Serial.println(vectorFailed ? "FAIL" : "PASS");
}

void runTest() {
  failed = false;
  resultLine[0] = '\0';
  Serial.println("74HC35 test");
  for (uint8_t vector = 0; vector < 64; vector++) checkVector(vector);
  drive(0);
  Serial.println(failed ? resultLine : "RESULT PASS");
}

void setup() {
  Serial.begin(115200);
  for (uint8_t channel = 0; channel < CHANNELS; channel++) {
    pinMode(INPUTS[channel], OUTPUT);
    digitalWrite(INPUTS[channel], LOW);
    pinMode(OUTPUTS[channel], INPUT_PULLUP);
  }
  Serial.println("74HC35 ready. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  runTest();
}
