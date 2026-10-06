/*
 * Self-check for a 74HC48 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * LT and RBI are active low. BI/RBO is bidirectional. BI low blanks every
 * segment and overrides LT. With BI released, LT low lights every segment.
 * RBI low blanks only code 0 and pulls BI/RBO low. Segment bit 0 is a and
 * bit 6 is g; a 1 in LIT means the segment is on, which is a high pin.
 * Digits 6 and 9 have no tails. Outputs are push-pull, so a-g need no
 * pull-ups. A 1k series resistor sits between D5 and BI/RBO. This sketch
 * never drives BI/RBO high. Until the check starts, BI/RBO is held low and
 * the segments stay off.
 */

const uint8_t PIN_B = 2;
const uint8_t PIN_C = 3;
const uint8_t PIN_LT = 4;
const uint8_t PIN_BI = 5;
const uint8_t PIN_RBI = 6;
const uint8_t PIN_D = 7;
const uint8_t PIN_A = 8;
const uint8_t PIN_E = 9;
const uint8_t PIN_SEG_D = 10;
const uint8_t PIN_SEG_C = 11;
const uint8_t PIN_SEG_B = 12;
const uint8_t PIN_SEG_A = 13;
const uint8_t PIN_G = A0;
const uint8_t PIN_F = A1;

const uint8_t LIT[16] = {
    0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7C, 0x07,
    0x7F, 0x67, 0x58, 0x4C, 0x62, 0x69, 0x78, 0x00};

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

void noteRbo(const char* step, bool expectedLow) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s expected RBO %s",
      step,
      expectedLow ? "LOW" : "HIGH");
}

void settle() { delay(1); }

void setCode(uint8_t code) {
  digitalWrite(PIN_A, (code & 1) ? HIGH : LOW);
  digitalWrite(PIN_B, (code & 2) ? HIGH : LOW);
  digitalWrite(PIN_C, (code & 4) ? HIGH : LOW);
  digitalWrite(PIN_D, (code & 8) ? HIGH : LOW);
}

void driveBiLow() {
  pinMode(PIN_BI, OUTPUT);
  digitalWrite(PIN_BI, LOW);
}

void releaseBi() { pinMode(PIN_BI, INPUT_PULLUP); }

uint8_t readLit() {
  uint8_t value = 0;
  if (digitalRead(PIN_SEG_A)) value |= 1u << 0;
  if (digitalRead(PIN_SEG_B)) value |= 1u << 1;
  if (digitalRead(PIN_SEG_C)) value |= 1u << 2;
  if (digitalRead(PIN_SEG_D)) value |= 1u << 3;
  if (digitalRead(PIN_E)) value |= 1u << 4;
  if (digitalRead(PIN_F)) value |= 1u << 5;
  if (digitalRead(PIN_G)) value |= 1u << 6;
  return value;
}

void expect(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readLit();
  if (actual != expected) noteFailure(step, expected, actual);
}

void expectRbo(bool expectedLow, const char* step) {
  settle();
  if (digitalRead(PIN_BI) == LOW) {
    if (!expectedLow) noteRbo(step, false);
  } else if (expectedLow) {
    noteRbo(step, true);
  }
}

void enterIdle() {
  digitalWrite(PIN_LT, HIGH);
  digitalWrite(PIN_RBI, HIGH);
  releaseBi();
  settle();
}

void runChecks() {
  enterIdle();
  for (uint8_t code = 0; code < 16; code++) {
    setCode(code);
    expect(LIT[code], "follow");
    expectRbo(false, "follow-rbo");
  }

  setCode(8);
  driveBiLow();
  digitalWrite(PIN_LT, LOW);
  expect(0x00, "blank-over-lamp");
  releaseBi();
  expect(0x7F, "lamp");
  expectRbo(false, "lamp-rbo");
  digitalWrite(PIN_LT, HIGH);
  expect(LIT[8], "after-lamp");

  setCode(0);
  digitalWrite(PIN_RBI, LOW);
  expect(0x00, "ripple-zero");
  expectRbo(true, "ripple-zero-rbo");

  setCode(5);
  expect(LIT[5], "ripple-five");
  expectRbo(false, "ripple-five-rbo");

  digitalWrite(PIN_RBI, HIGH);
  setCode(0);
  expect(LIT[0], "zero");
  expectRbo(false, "zero-rbo");

  digitalWrite(PIN_LT, LOW);
  digitalWrite(PIN_RBI, LOW);
  expect(0x7F, "lamp-over-ripple");
  expectRbo(false, "lamp-over-ripple-rbo");
  digitalWrite(PIN_LT, HIGH);
  digitalWrite(PIN_RBI, HIGH);
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_B, OUTPUT);
  pinMode(PIN_C, OUTPUT);
  pinMode(PIN_LT, OUTPUT);
  pinMode(PIN_RBI, OUTPUT);
  pinMode(PIN_D, OUTPUT);
  pinMode(PIN_A, OUTPUT);
  pinMode(PIN_E, INPUT);
  pinMode(PIN_SEG_D, INPUT);
  pinMode(PIN_SEG_C, INPUT);
  pinMode(PIN_SEG_B, INPUT);
  pinMode(PIN_SEG_A, INPUT);
  pinMode(PIN_G, INPUT);
  pinMode(PIN_F, INPUT);
  setCode(0);
  digitalWrite(PIN_LT, HIGH);
  digitalWrite(PIN_RBI, HIGH);
  driveBiLow();
  Serial.println("74HC48 ready. Send any character to start.");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  Serial.println(failed ? resultLine : "RESULT PASS");
}
