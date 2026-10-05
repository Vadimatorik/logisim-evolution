/*
 * Self-check for a 74HC46 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Segment outputs are active-low open-collector. A set bit in the expected mask is a
 * high pin, which means that segment is off. Bit 0 is a and bit 6 is g.
 * BI/RBO is never driven high: blanking drives it low, and reading RBO releases it
 * with the pin pull-up. LT low turns every segment on unless BI is low.
 * RBI low blanks a zero and pulls RBO low.
 */

const uint8_t PIN_B = 2;
const uint8_t PIN_C = 3;
const uint8_t PIN_LT = 4;
const uint8_t PIN_BI = 5;
const uint8_t PIN_RBI = 6;
const uint8_t PIN_D = 7;
const uint8_t PIN_A = 8;
const uint8_t PIN_SEGE = 9;
const uint8_t PIN_SEGD = 10;
const uint8_t PIN_SEGC = 11;
const uint8_t PIN_SEGB = 12;
const uint8_t PIN_SEGA = A0;
const uint8_t PIN_SEGG = A1;
const uint8_t PIN_SEGF = A2;

// High bit = segment off. Bit 0 is a, bit 6 is g. Same table as Ttl7446Test.
const uint8_t SEGMENT_LEVELS[16] = {
    0x40, 0x79, 0x24, 0x30, 0x19, 0x12, 0x03, 0x78,
    0x00, 0x18, 0x27, 0x33, 0x1D, 0x16, 0x07, 0x7F};

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

void releaseBi() { pinMode(PIN_BI, INPUT_PULLUP); }

void driveBiLow() {
  digitalWrite(PIN_BI, LOW);
  pinMode(PIN_BI, OUTPUT);
  digitalWrite(PIN_BI, LOW);
}

void setCode(uint8_t code) {
  digitalWrite(PIN_A, (code & 1) ? HIGH : LOW);
  digitalWrite(PIN_B, (code & 2) ? HIGH : LOW);
  digitalWrite(PIN_C, (code & 4) ? HIGH : LOW);
  digitalWrite(PIN_D, (code & 8) ? HIGH : LOW);
}

uint8_t readSegments() {
  uint8_t value = 0;
  if (digitalRead(PIN_SEGA)) value |= 0x01;
  if (digitalRead(PIN_SEGB)) value |= 0x02;
  if (digitalRead(PIN_SEGC)) value |= 0x04;
  if (digitalRead(PIN_SEGD)) value |= 0x08;
  if (digitalRead(PIN_SEGE)) value |= 0x10;
  if (digitalRead(PIN_SEGF)) value |= 0x20;
  if (digitalRead(PIN_SEGG)) value |= 0x40;
  return value;
}

void expectSegments(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readSegments();
  if (actual != expected) noteFailure(step, expected, actual);
}

void expectRbo(bool low, const char* step) {
  settle();
  const uint8_t actual = digitalRead(PIN_BI) ? 1 : 0;
  const uint8_t expected = low ? 0 : 1;
  if (actual != expected) noteFailure(step, expected, actual);
}

void show(uint8_t code, bool lamp, bool ripple) {
  releaseBi();
  digitalWrite(PIN_LT, lamp ? LOW : HIGH);
  digitalWrite(PIN_RBI, ripple ? LOW : HIGH);
  setCode(code);
}

void checkPatterns() {
  for (uint8_t code = 0; code < 16; code++) {
    char step[24];
    show(code, false, false);
    snprintf(step, sizeof(step), "code %02X", code);
    expectSegments(SEGMENT_LEVELS[code], step);
    expectRbo(false, "rbo idle");
  }
}

void checkBlanking() {
  const uint8_t codes[] = {0, 5, 15};
  for (uint8_t index = 0; index < 3; index++) {
    show(codes[index], true, true);
    driveBiLow();
    expectSegments(0x7F, "bi blanks");
  }
  releaseBi();
}

void checkLampTest() {
  show(0, true, true);
  expectSegments(0x00, "lamp on zero");
  expectRbo(false, "rbo during lamp");

  show(5, true, false);
  expectSegments(0x00, "lamp on five");
  expectRbo(false, "rbo during lamp");
}

void checkRippleBlanking() {
  show(0, false, true);
  expectSegments(0x7F, "rbi blanks zero");
  expectRbo(true, "rbo on blanked zero");

  show(5, false, true);
  expectSegments(SEGMENT_LEVELS[5], "rbi keeps five");
  expectRbo(false, "rbo on nonzero");
}

void setup() {
  pinMode(PIN_B, OUTPUT);
  pinMode(PIN_C, OUTPUT);
  pinMode(PIN_LT, OUTPUT);
  pinMode(PIN_RBI, OUTPUT);
  pinMode(PIN_D, OUTPUT);
  pinMode(PIN_A, OUTPUT);
  pinMode(PIN_SEGA, INPUT_PULLUP);
  pinMode(PIN_SEGB, INPUT_PULLUP);
  pinMode(PIN_SEGC, INPUT_PULLUP);
  pinMode(PIN_SEGD, INPUT_PULLUP);
  pinMode(PIN_SEGE, INPUT_PULLUP);
  pinMode(PIN_SEGF, INPUT_PULLUP);
  pinMode(PIN_SEGG, INPUT_PULLUP);

  digitalWrite(PIN_LT, HIGH);
  digitalWrite(PIN_RBI, HIGH);
  setCode(0);
  releaseBi();

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  checkPatterns();
  checkBlanking();
  checkLampTest();
  checkRippleBlanking();

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
