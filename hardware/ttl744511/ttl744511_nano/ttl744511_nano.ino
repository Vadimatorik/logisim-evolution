/*
 * Self-check for a 74HC4511 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * LT, BI and LE are active low. LE low is transparent. LE high holds the BCD
 * code. LT low forces every segment high, even when BI is low. With LT high,
 * BI low forces every segment low. LT and BI do not change the latch. Codes
 * 10..15 are blank. Segment bit 0 is a and bit 6 is g. Outputs are push-pull,
 * so the segment pins are read directly. Until the check starts, LE is high,
 * BI is low and LT is high, which keeps the segments off.
 */

const uint8_t PIN_B = 2;
const uint8_t PIN_C = 3;
const uint8_t PIN_LT = 4;
const uint8_t PIN_BI = 5;
const uint8_t PIN_LE = 6;
const uint8_t PIN_D = 7;
const uint8_t PIN_A = 8;
const uint8_t PIN_E = 9;
const uint8_t PIN_SEG_D = 10;
const uint8_t PIN_SEG_C = 11;
const uint8_t PIN_SEG_B = 12;
const uint8_t PIN_SEG_A = 13;
const uint8_t PIN_G = A0;
const uint8_t PIN_F = A1;

const uint8_t GLYPH[16] = {
    0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7C, 0x07,
    0x7F, 0x67, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

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

void setCode(uint8_t code) {
  digitalWrite(PIN_A, (code & 1) ? HIGH : LOW);
  digitalWrite(PIN_B, (code & 2) ? HIGH : LOW);
  digitalWrite(PIN_C, (code & 4) ? HIGH : LOW);
  digitalWrite(PIN_D, (code & 8) ? HIGH : LOW);
}

uint8_t readSegments() {
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
  const uint8_t actual = readSegments();
  if (actual != expected) noteFailure(step, expected, actual);
}

void enterFollow() {
  digitalWrite(PIN_LT, HIGH);
  digitalWrite(PIN_BI, HIGH);
  digitalWrite(PIN_LE, LOW);
  settle();
}

void enterHold() {
  digitalWrite(PIN_LE, HIGH);
  settle();
}

void runChecks() {
  enterFollow();
  for (uint8_t code = 0; code < 16; code++) {
    setCode(code);
    expect(GLYPH[code], "follow");
  }

  setCode(0);
  digitalWrite(PIN_BI, LOW);
  digitalWrite(PIN_LT, LOW);
  expect(0x7F, "lamp-over-blank");
  digitalWrite(PIN_LT, HIGH);
  digitalWrite(PIN_BI, HIGH);
  expect(GLYPH[0], "after-lamp");

  setCode(8);
  digitalWrite(PIN_BI, LOW);
  expect(0x00, "blank-eight");
  digitalWrite(PIN_BI, HIGH);
  expect(GLYPH[8], "after-blank");

  setCode(5);
  enterHold();
  setCode(0);
  expect(GLYPH[5], "hold-zero");
  setCode(9);
  expect(GLYPH[5], "hold-nine");

  digitalWrite(PIN_LT, LOW);
  expect(0x7F, "hold-lamp");
  digitalWrite(PIN_LT, HIGH);
  expect(GLYPH[5], "hold-after-lamp");

  digitalWrite(PIN_BI, LOW);
  expect(0x00, "hold-blank");
  digitalWrite(PIN_BI, HIGH);
  expect(GLYPH[5], "hold-after-blank");

  enterFollow();
  setCode(2);
  expect(GLYPH[2], "follow-again");

  setCode(0x0A);
  enterHold();
  setCode(3);
  expect(0x00, "hold-blank-code");
  digitalWrite(PIN_LT, LOW);
  expect(0x7F, "blank-code-lamp");
  digitalWrite(PIN_LT, HIGH);
  expect(0x00, "blank-code-after-lamp");
  enterFollow();
  expect(GLYPH[3], "admit-three");
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_B, OUTPUT);
  pinMode(PIN_C, OUTPUT);
  pinMode(PIN_LT, OUTPUT);
  pinMode(PIN_BI, OUTPUT);
  pinMode(PIN_LE, OUTPUT);
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
  digitalWrite(PIN_LE, HIGH);
  digitalWrite(PIN_BI, LOW);
  Serial.println("74HC4511 ready. Send any character to start.");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  Serial.println(failed ? resultLine : "RESULT PASS");
}
