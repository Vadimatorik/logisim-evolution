/*
 * Self-check for a 74HC4543 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * LD high makes the latch follow the BCD inputs. LD low holds that code.
 * BI and PH are not stored. BI high blanks by driving every segment from PH.
 * Codes 10-15 are blank. Outputs are push-pull, so the segments are read directly.
 * LD, PH and BI stay low and the BCD inputs stay at 0 until the check starts.
 */

const uint8_t PIN_LD = 2;
const uint8_t PIN_D2 = 3;
const uint8_t PIN_D1 = 4;
const uint8_t PIN_D3 = 5;
const uint8_t PIN_D0 = 6;
const uint8_t PIN_PH = 7;
const uint8_t PIN_BI = 8;
const uint8_t PIN_QA = 9;
const uint8_t PIN_QB = 10;
const uint8_t PIN_QC = 11;
const uint8_t PIN_QD = 12;
const uint8_t PIN_QE = 13;
const uint8_t PIN_QG = A0;
const uint8_t PIN_QF = A1;

// Datasheet order a b c d e f g. Bit 6 is segment a. Codes 10-15 are blank.
const uint8_t GLYPHS[16] = {
    0b1111110, 0b0110000, 0b1101101, 0b1111001, 0b0110011, 0b1011011,
    0b1011111, 0b1110000, 0b1111111, 0b1111011, 0b0000000, 0b0000000,
    0b0000000, 0b0000000, 0b0000000, 0b0000000};

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
  digitalWrite(PIN_D0, (code & 1) ? HIGH : LOW);
  digitalWrite(PIN_D1, (code & 2) ? HIGH : LOW);
  digitalWrite(PIN_D2, (code & 4) ? HIGH : LOW);
  digitalWrite(PIN_D3, (code & 8) ? HIGH : LOW);
}

uint8_t readSegments() {
  uint8_t value = 0;
  if (digitalRead(PIN_QA)) value |= 1 << 6;
  if (digitalRead(PIN_QB)) value |= 1 << 5;
  if (digitalRead(PIN_QC)) value |= 1 << 4;
  if (digitalRead(PIN_QD)) value |= 1 << 3;
  if (digitalRead(PIN_QE)) value |= 1 << 2;
  if (digitalRead(PIN_QF)) value |= 1 << 1;
  if (digitalRead(PIN_QG)) value |= 1 << 0;
  return value;
}

void expectSegments(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readSegments();
  if (actual != expected) noteFailure(step, expected, actual);
}

uint8_t inverted(uint8_t glyph) { return (~glyph) & 0x7F; }

void apply(uint8_t code, uint8_t ld, uint8_t phase, uint8_t blank) {
  setCode(code);
  digitalWrite(PIN_LD, ld);
  digitalWrite(PIN_PH, phase);
  digitalWrite(PIN_BI, blank);
}

void expectCode(uint8_t code, uint8_t expected, const char* step) {
  char label[48];
  snprintf(label, sizeof(label), "%s %u", step, code);
  expectSegments(expected, label);
}

void checkGlyphs() {
  for (uint8_t code = 0; code < 16; code++) {
    apply(code, HIGH, LOW, LOW);
    expectCode(code, GLYPHS[code], "glyph");
    apply(code, HIGH, HIGH, LOW);
    expectCode(code, inverted(GLYPHS[code]), "phase");
  }
}

void checkBlanking() {
  apply(8, HIGH, LOW, HIGH);
  expectSegments(0x00, "blank ph0");
  apply(8, HIGH, HIGH, HIGH);
  expectSegments(0x7F, "blank ph1");
  apply(1, LOW, HIGH, HIGH);
  expectSegments(0x7F, "blank while latched");
  apply(1, LOW, LOW, LOW);
  expectSegments(GLYPHS[8], "unblank shows latched 8");
}

void checkHold() {
  apply(5, HIGH, LOW, LOW);
  expectSegments(GLYPHS[5], "capture 5");
  apply(2, LOW, LOW, LOW);
  expectSegments(GLYPHS[5], "hold 5");
  apply(2, LOW, HIGH, LOW);
  expectSegments(inverted(GLYPHS[5]), "phase while held");
  apply(2, LOW, LOW, HIGH);
  expectSegments(0x00, "blank while held");
  apply(2, LOW, LOW, LOW);
  expectSegments(GLYPHS[5], "restore held 5");
}

void checkCaptureDuringBlank() {
  apply(3, HIGH, LOW, HIGH);
  expectSegments(0x00, "capture 3 while blank");
  apply(9, LOW, LOW, LOW);
  expectSegments(GLYPHS[3], "held 3 captured during blank");
}

void runChecks() {
  failed = false;
  checkGlyphs();
  checkBlanking();
  checkHold();
  checkCaptureDuringBlank();
  if (!failed) {
    Serial.println("RESULT PASS");
  } else {
    Serial.println(resultLine);
  }
}

void setup() {
  const uint8_t outputs[] = {PIN_LD, PIN_D2, PIN_D1, PIN_D3, PIN_D0, PIN_PH, PIN_BI};
  const uint8_t inputs[] = {PIN_QA, PIN_QB, PIN_QC, PIN_QD, PIN_QE, PIN_QF, PIN_QG};
  for (uint8_t index = 0; index < sizeof(outputs); index++) {
    digitalWrite(outputs[index], LOW);
    pinMode(outputs[index], OUTPUT);
  }
  for (uint8_t index = 0; index < sizeof(inputs); index++) {
    pinMode(inputs[index], INPUT);
  }
  Serial.begin(115200);
  Serial.println("Send any character to start");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  runChecks();
}
