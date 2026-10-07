/*
 * Self-check for a 74HC4028 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * A0 is the least significant address bit. Codes 0-9 raise exactly one output,
 * Yn, where n is the code. Codes 10-15 hold every output low. Outputs are
 * push-pull. Address lines stay low until the check starts.
 */

const uint8_t PIN_A0 = 2;
const uint8_t PIN_A1 = 3;
const uint8_t PIN_A2 = 4;
const uint8_t PIN_A3 = 5;
const uint8_t PIN_Y[10] = {6, 7, 8, 9, 10, 11, 12, A0, A1, A2};

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, const char* detail) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s %s", step, detail);
}

void settle() { delayMicroseconds(20); }

void apply(uint8_t address) {
  digitalWrite(PIN_A0, (address & 1) ? HIGH : LOW);
  digitalWrite(PIN_A1, (address & 2) ? HIGH : LOW);
  digitalWrite(PIN_A2, (address & 4) ? HIGH : LOW);
  digitalWrite(PIN_A3, (address & 8) ? HIGH : LOW);
  settle();
}

uint16_t readOutputs() {
  uint16_t value = 0;
  for (uint8_t bit = 0; bit < 10; bit++) {
    if (digitalRead(PIN_Y[bit]) == HIGH) value |= (uint16_t)1 << bit;
  }
  return value;
}

uint16_t expectedMask(uint8_t address) {
  if (address > 9) return 0;
  return (uint16_t)1 << address;
}

void printOutputs(uint8_t address, uint16_t bits) {
  Serial.print("CODE ");
  Serial.print(address);
  Serial.print(" Y9..Y0 ");
  for (int bit = 9; bit >= 0; bit--) {
    Serial.print((bits & ((uint16_t)1 << bit)) ? 'H' : 'L');
  }
  Serial.println();
}

void checkCase(uint8_t address) {
  apply(address);
  const uint16_t actual = readOutputs();
  const uint16_t expected = expectedMask(address);
  printOutputs(address, actual);
  if (actual == expected) return;
  char step[16];
  char detail[48];
  snprintf(step, sizeof(step), "code-%u", address);
  snprintf(detail, sizeof(detail), "expected %u got %u", expected, actual);
  noteFailure(step, detail);
}

void runChecks() {
  for (uint8_t address = 0; address < 16; address++) {
    checkCase(address);
  }
}

void setup() {
  pinMode(PIN_A0, OUTPUT);
  pinMode(PIN_A1, OUTPUT);
  pinMode(PIN_A2, OUTPUT);
  pinMode(PIN_A3, OUTPUT);
  digitalWrite(PIN_A0, LOW);
  digitalWrite(PIN_A1, LOW);
  digitalWrite(PIN_A2, LOW);
  digitalWrite(PIN_A3, LOW);
  for (uint8_t bit = 0; bit < 10; bit++) {
    pinMode(PIN_Y[bit], INPUT);
  }

  Serial.begin(115200);
  Serial.println("Send any character to test the 74HC4028");
  while (Serial.available() == 0) {
  }
  while (Serial.available() > 0) Serial.read();
  runChecks();
  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
