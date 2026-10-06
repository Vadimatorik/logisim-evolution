/*
 * Self-check for a 74HC145 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Q0-Q9 are open-drain and active low. Each output needs a 10k pull-up to 5V;
 * a released pin is not a driven high. Codes 0-9 pull only the matching Q low.
 * Codes 10-15 release every output. A (pin 15) is the least significant bit.
 * The address stays at 0000 until the check starts, so only Q0 is low.
 */

const uint8_t PIN_A = 2;
const uint8_t PIN_B = 3;
const uint8_t PIN_C = 4;
const uint8_t PIN_D = 5;

const uint8_t PIN_Q[] = {6, 7, 8, 9, 10, 11, 12, A0, A1, A2};

const uint16_t ALL_HIGH = 0x03FF;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, uint16_t expected, uint16_t actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s expected %03X got %03X",
      step,
      expected,
      actual);
}

void settle() { delay(1); }

void setCode(uint8_t code) {
  digitalWrite(PIN_A, (code & 1) ? HIGH : LOW);
  digitalWrite(PIN_B, (code & 2) ? HIGH : LOW);
  digitalWrite(PIN_C, (code & 4) ? HIGH : LOW);
  digitalWrite(PIN_D, (code & 8) ? HIGH : LOW);
}

uint16_t readOutputs() {
  uint16_t value = 0;
  for (uint8_t index = 0; index < 10; index++) {
    if (digitalRead(PIN_Q[index])) value |= (uint16_t)1 << index;
  }
  return value;
}

void expectMask(uint16_t expected, const char* step) {
  settle();
  const uint16_t actual = readOutputs();
  if (actual != expected) noteFailure(step, expected, actual);
}

void checkCodes() {
  char step[16];
  for (uint8_t code = 0; code < 16; code++) {
    setCode(code);
    const uint16_t expected = (code < 10) ? (ALL_HIGH & ~((uint16_t)1 << code)) : ALL_HIGH;
    snprintf(step, sizeof(step), "code %u", code);
    expectMask(expected, step);
  }
}

void setup() {
  pinMode(PIN_A, OUTPUT);
  pinMode(PIN_B, OUTPUT);
  pinMode(PIN_C, OUTPUT);
  pinMode(PIN_D, OUTPUT);
  for (uint8_t index = 0; index < 10; index++) {
    pinMode(PIN_Q[index], INPUT);
  }
  setCode(0);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  checkCodes();

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
