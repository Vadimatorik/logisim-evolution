/*
 * Self-check for a 74HC280 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * PE is high when an even number of I0..I8 are high, including none. PO is
 * high when that number is odd. Pin 3 is left open. Outputs are push-pull, so
 * PE and PO are read directly. Every input stays low until the check starts.
 */

const uint8_t PIN_I0 = 7;
const uint8_t PIN_I1 = 8;
const uint8_t PIN_I2 = 9;
const uint8_t PIN_I3 = 10;
const uint8_t PIN_I4 = 11;
const uint8_t PIN_I5 = 12;
const uint8_t PIN_I6 = 2;
const uint8_t PIN_I7 = 3;
const uint8_t PIN_I8 = 4;
const uint8_t PIN_PE = 5;
const uint8_t PIN_PO = 6;

const uint8_t INPUT_PINS[9] = {
  PIN_I0, PIN_I1, PIN_I2, PIN_I3, PIN_I4, PIN_I5, PIN_I6, PIN_I7, PIN_I8};

bool failed = false;
char resultLine[96];

void noteFailure(uint16_t mask, bool expectedPe, bool expectedPo) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL mask %03X expected PE=%d PO=%d got PE=%d PO=%d",
      mask,
      expectedPe ? 1 : 0,
      expectedPo ? 1 : 0,
      digitalRead(PIN_PE) ? 1 : 0,
      digitalRead(PIN_PO) ? 1 : 0);
}

void settle() { delay(1); }

void setInputs(uint16_t mask) {
  for (uint8_t bit = 0; bit < 9; bit++) {
    digitalWrite(INPUT_PINS[bit], (mask & (1u << bit)) ? HIGH : LOW);
  }
}

void expectParity(uint16_t mask) {
  settle();
  const bool odd = (__builtin_popcount(mask) & 1) != 0;
  const bool expectedPe = !odd;
  const bool expectedPo = odd;
  const bool actualPe = digitalRead(PIN_PE) == HIGH;
  const bool actualPo = digitalRead(PIN_PO) == HIGH;
  if (actualPe != expectedPe || actualPo != expectedPo) {
    noteFailure(mask, expectedPe, expectedPo);
  }
}

void runChecks() {
  for (uint16_t mask = 0; mask < 512; mask++) {
    setInputs(mask);
    expectParity(mask);
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_PE, INPUT);
  pinMode(PIN_PO, INPUT);
  for (uint8_t bit = 0; bit < 9; bit++) pinMode(INPUT_PINS[bit], OUTPUT);
  setInputs(0);
  Serial.println("74HC280 ready. Send any character to start.");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  setInputs(0);
  Serial.println(failed ? resultLine : "RESULT PASS");
}
