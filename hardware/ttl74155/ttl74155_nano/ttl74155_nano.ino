/*
 * Self-check for a 74HC155 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Section 1 selects an active-low 1Y output only while 1G is low and 1C is high.
 * Section 2 selects an active-low 2Y output only while 2G is high and 2C is low.
 * A is the shared least significant address bit and B is the most significant.
 * Outputs are push-pull, so the Y pins are read directly.
 * Until the check starts, 1G stays high, 1C stays low, 2G stays low and 2C stays high.
 */

const uint8_t PIN_C1 = 2;
const uint8_t PIN_G1 = 3;
const uint8_t PIN_B = 4;
const uint8_t PIN_A = 5;
const uint8_t PIN_G2 = 6;
const uint8_t PIN_C2 = 7;

const uint8_t PIN_Y1_3 = 8;
const uint8_t PIN_Y1_2 = 9;
const uint8_t PIN_Y1_1 = 10;
const uint8_t PIN_Y1_0 = 11;
const uint8_t PIN_Y2_0 = 12;
const uint8_t PIN_Y2_1 = 13;
const uint8_t PIN_Y2_2 = A0;
const uint8_t PIN_Y2_3 = A1;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, uint8_t expected, uint8_t actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s expected %02X got %02X",
      step,
      expected,
      actual);
}

void settle() { delay(1); }

void drive(uint8_t address, bool g1, bool c1, bool g2, bool c2) {
  digitalWrite(PIN_A, (address & 1) ? HIGH : LOW);
  digitalWrite(PIN_B, (address & 2) ? HIGH : LOW);
  digitalWrite(PIN_G1, g1 ? HIGH : LOW);
  digitalWrite(PIN_C1, c1 ? HIGH : LOW);
  digitalWrite(PIN_G2, g2 ? HIGH : LOW);
  digitalWrite(PIN_C2, c2 ? HIGH : LOW);
}

uint8_t readLevels() {
  uint8_t value = 0;
  if (digitalRead(PIN_Y1_0)) value |= 1;
  if (digitalRead(PIN_Y1_1)) value |= 2;
  if (digitalRead(PIN_Y1_2)) value |= 4;
  if (digitalRead(PIN_Y1_3)) value |= 8;
  if (digitalRead(PIN_Y2_0)) value |= 16;
  if (digitalRead(PIN_Y2_1)) value |= 32;
  if (digitalRead(PIN_Y2_2)) value |= 64;
  if (digitalRead(PIN_Y2_3)) value |= 128;
  return value;
}

uint8_t expectedLevels(uint8_t address, bool section1, bool section2) {
  uint8_t levels = 0xFF;
  if (section1) levels &= ~(1 << address);
  if (section2) levels &= ~(1 << (4 + address));
  return levels;
}

void expectLevels(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readLevels();
  if (actual != expected) noteFailure(step, expected, actual);
}

void checkInhibitAndSelect() {
  for (uint8_t address = 0; address < 4; address++) {
    drive(address, true, true, true, false);
    expectLevels(expectedLevels(address, false, true), "g1 high");
    drive(address, true, false, true, false);
    expectLevels(expectedLevels(address, false, true), "g1 high c1 low");
    drive(address, false, false, true, false);
    expectLevels(expectedLevels(address, false, true), "c1 low");

    drive(address, false, true, false, false);
    expectLevels(expectedLevels(address, true, false), "g2 low");
    drive(address, false, true, false, true);
    expectLevels(expectedLevels(address, true, false), "g2 low c2 high");
    drive(address, false, true, true, true);
    expectLevels(expectedLevels(address, true, false), "c2 high");

    drive(address, false, true, true, false);
    expectLevels(expectedLevels(address, true, true), "both sections");
  }
}

void checkDataPolarity() {
  drive(1, false, true, false, true);
  expectLevels(expectedLevels(1, true, false), "c1 high selects");
  digitalWrite(PIN_C1, LOW);
  expectLevels(0xFF, "c1 low clears");

  drive(1, true, false, true, false);
  expectLevels(expectedLevels(1, false, true), "c2 low selects");
  digitalWrite(PIN_C2, HIGH);
  expectLevels(0xFF, "c2 high clears");
}

void checkThreeToEight() {
  for (uint8_t code = 0; code < 8; code++) {
    const uint8_t address = code & 3;
    const bool select = (code & 4) != 0;
    drive(address, false, select, true, select);
    expectLevels(expectedLevels(address, select, !select), "three to eight");
  }
}

void setup() {
  pinMode(PIN_C1, OUTPUT);
  pinMode(PIN_G1, OUTPUT);
  pinMode(PIN_B, OUTPUT);
  pinMode(PIN_A, OUTPUT);
  pinMode(PIN_G2, OUTPUT);
  pinMode(PIN_C2, OUTPUT);
  pinMode(PIN_Y1_3, INPUT);
  pinMode(PIN_Y1_2, INPUT);
  pinMode(PIN_Y1_1, INPUT);
  pinMode(PIN_Y1_0, INPUT);
  pinMode(PIN_Y2_0, INPUT);
  pinMode(PIN_Y2_1, INPUT);
  pinMode(PIN_Y2_2, INPUT);
  pinMode(PIN_Y2_3, INPUT);

  drive(0, true, false, false, true);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  checkInhibitAndSelect();
  checkDataPolarity();
  checkThreeToEight();

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
