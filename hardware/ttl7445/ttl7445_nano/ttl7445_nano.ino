/*
 * Self-check for a 7445 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Outputs are open collector and active low. The Nano pins use INPUT_PULLUP,
 * so a released output reads high. Do not drive those pins or pull them above 5 V.
 * A, B, C and D stay low until the check starts.
 */

const uint8_t PIN_A = 2;
const uint8_t PIN_B = 3;
const uint8_t PIN_C = 4;
const uint8_t PIN_D = 5;

const uint8_t OUTPUT_PINS[10] = {6, 7, 8, 9, 10, 11, 12, 13, A0, A1};

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
    if (digitalRead(OUTPUT_PINS[index])) value |= (uint16_t) (1u << index);
  }
  return value;
}

void expectMask(uint8_t code, uint16_t expected) {
  settle();
  const uint16_t actual = readOutputs();
  if (actual != expected) {
    char step[16];
    snprintf(step, sizeof(step), "code %u", code);
    noteFailure(step, expected, actual);
  }
}

void checkCode(uint8_t code) {
  setCode(code);
  const uint16_t expected =
      code <= 9 ? (uint16_t) (0x3FF ^ (1u << code)) : 0x3FF;
  expectMask(code, expected);
}

void runChecks() {
  for (uint8_t code = 0; code <= 15; code++) checkCode(code);
}

void setup() {
  pinMode(PIN_A, OUTPUT);
  pinMode(PIN_B, OUTPUT);
  pinMode(PIN_C, OUTPUT);
  pinMode(PIN_D, OUTPUT);
  digitalWrite(PIN_A, LOW);
  digitalWrite(PIN_B, LOW);
  digitalWrite(PIN_C, LOW);
  digitalWrite(PIN_D, LOW);
  for (uint8_t index = 0; index < 10; index++) {
    pinMode(OUTPUT_PINS[index], INPUT_PULLUP);
  }

  Serial.begin(115200);
  Serial.println("7445 ready, send any character");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();

  failed = false;
  resultLine[0] = '\0';
  runChecks();
  Serial.println(failed ? resultLine : "RESULT PASS");
  while (true) {}
}
