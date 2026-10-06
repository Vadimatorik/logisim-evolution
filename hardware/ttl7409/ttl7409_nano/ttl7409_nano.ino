/*
 * Self-check for a 74HC09 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Outputs are open-drain. Each Y needs a 10k pull-up to 5V and is read as
 * INPUT, without the Nano pull-up. Y is high only when both inputs are high.
 * Inputs stay low until the check starts, so every output is driven low.
 */

const uint8_t PIN_1A = 2;
const uint8_t PIN_1B = 3;
const uint8_t PIN_2A = 4;
const uint8_t PIN_2B = 5;
const uint8_t PIN_3A = 6;
const uint8_t PIN_3B = 7;
const uint8_t PIN_4A = 8;
const uint8_t PIN_4B = 9;
const uint8_t PIN_1Y = 10;
const uint8_t PIN_2Y = 11;
const uint8_t PIN_3Y = 12;
const uint8_t PIN_4Y = A0;

bool failed = false;
char resultLine[96];

void noteFailure(uint8_t pattern, uint8_t expected, uint8_t actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL pattern %02X expected %X got %X",
      pattern,
      expected,
      actual);
}

void settle() { delay(1); }

void drive(uint8_t pattern) {
  digitalWrite(PIN_1A, (pattern & 0x01) ? HIGH : LOW);
  digitalWrite(PIN_1B, (pattern & 0x02) ? HIGH : LOW);
  digitalWrite(PIN_2A, (pattern & 0x04) ? HIGH : LOW);
  digitalWrite(PIN_2B, (pattern & 0x08) ? HIGH : LOW);
  digitalWrite(PIN_3A, (pattern & 0x10) ? HIGH : LOW);
  digitalWrite(PIN_3B, (pattern & 0x20) ? HIGH : LOW);
  digitalWrite(PIN_4A, (pattern & 0x40) ? HIGH : LOW);
  digitalWrite(PIN_4B, (pattern & 0x80) ? HIGH : LOW);
}

uint8_t readOutputs() {
  uint8_t value = 0;
  if (digitalRead(PIN_1Y)) value |= 0x01;
  if (digitalRead(PIN_2Y)) value |= 0x02;
  if (digitalRead(PIN_3Y)) value |= 0x04;
  if (digitalRead(PIN_4Y)) value |= 0x08;
  return value;
}

uint8_t expectedOutputs(uint8_t pattern) {
  uint8_t value = 0;
  if ((pattern & 0x01) && (pattern & 0x02)) value |= 0x01;
  if ((pattern & 0x04) && (pattern & 0x08)) value |= 0x02;
  if ((pattern & 0x10) && (pattern & 0x20)) value |= 0x04;
  if ((pattern & 0x40) && (pattern & 0x80)) value |= 0x08;
  return value;
}

void runTest() {
  failed = false;
  resultLine[0] = '\0';
  Serial.println("74HC09 test, 256 patterns");
  for (int pattern = 0; pattern < 256; pattern++) {
    drive((uint8_t) pattern);
    settle();
    const uint8_t expected = expectedOutputs((uint8_t) pattern);
    const uint8_t actual = readOutputs();
    if (actual != expected) noteFailure((uint8_t) pattern, expected, actual);
  }
  drive(0);
  Serial.println(failed ? resultLine : "RESULT PASS");
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_1A, OUTPUT);
  pinMode(PIN_1B, OUTPUT);
  pinMode(PIN_2A, OUTPUT);
  pinMode(PIN_2B, OUTPUT);
  pinMode(PIN_3A, OUTPUT);
  pinMode(PIN_3B, OUTPUT);
  pinMode(PIN_4A, OUTPUT);
  pinMode(PIN_4B, OUTPUT);
  pinMode(PIN_1Y, INPUT);
  pinMode(PIN_2Y, INPUT);
  pinMode(PIN_3Y, INPUT);
  pinMode(PIN_4Y, INPUT);
  drive(0);
  Serial.println("74HC09 ready. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  runTest();
}
