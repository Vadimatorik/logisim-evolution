/*
 * Self-check for a 74HC83 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * This is the classic 7483 pinout: VCC is pin 5 and GND is pin 12, not the
 * 74HC283 arrangement. C4,S4..S1 equals A + B + C0. Outputs are push-pull, so
 * they are read directly. Every input stays low until the check starts. D13
 * reads S3, and the board LED follows that output.
 */

const uint8_t PIN_A1 = 2;
const uint8_t PIN_A2 = 3;
const uint8_t PIN_A3 = 4;
const uint8_t PIN_A4 = 5;
const uint8_t PIN_B1 = 6;
const uint8_t PIN_B2 = 7;
const uint8_t PIN_B3 = 8;
const uint8_t PIN_B4 = 9;
const uint8_t PIN_C0 = 10;
const uint8_t PIN_S1 = 11;
const uint8_t PIN_S2 = 12;
const uint8_t PIN_S3 = 13;
const uint8_t PIN_S4 = A0;
const uint8_t PIN_C4 = A1;

const uint8_t INPUT_PINS[9] = {
  PIN_A1, PIN_A2, PIN_A3, PIN_A4, PIN_B1, PIN_B2, PIN_B3, PIN_B4, PIN_C0};
const uint8_t OUTPUT_PINS[5] = {PIN_S1, PIN_S2, PIN_S3, PIN_S4, PIN_C4};

bool failed = false;
char resultLine[96];

void noteFailure(uint16_t mask, uint8_t expected, uint8_t actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL mask %03X expected %02X got %02X",
      mask,
      expected,
      actual);
}

void settle() { delay(1); }

void setInputs(uint16_t mask) {
  for (uint8_t bit = 0; bit < 9; bit++) {
    digitalWrite(INPUT_PINS[bit], (mask & (1u << bit)) ? HIGH : LOW);
  }
}

uint8_t readSum() {
  uint8_t sum = 0;
  for (uint8_t bit = 0; bit < 5; bit++) {
    if (digitalRead(OUTPUT_PINS[bit]) == HIGH) sum |= static_cast<uint8_t>(1u << bit);
  }
  return sum;
}

void expectSum(uint16_t mask) {
  settle();
  const uint8_t a = mask & 0x0F;
  const uint8_t b = (mask >> 4) & 0x0F;
  const uint8_t carryIn = (mask >> 8) & 0x01;
  const uint8_t expected = static_cast<uint8_t>(a + b + carryIn);
  const uint8_t actual = readSum();
  if (actual != expected) noteFailure(mask, expected, actual);
}

void runChecks() {
  for (uint16_t mask = 0; mask < 512; mask++) {
    setInputs(mask);
    expectSum(mask);
  }
}

void setup() {
  Serial.begin(115200);
  for (uint8_t bit = 0; bit < 9; bit++) pinMode(INPUT_PINS[bit], OUTPUT);
  for (uint8_t bit = 0; bit < 5; bit++) pinMode(OUTPUT_PINS[bit], INPUT);
  setInputs(0);
  Serial.println("74HC83 ready. Send any character to start.");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  setInputs(0);
  Serial.println(failed ? resultLine : "RESULT PASS");
}
