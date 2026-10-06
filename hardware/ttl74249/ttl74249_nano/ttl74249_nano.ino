/*
 * Self-check for a 74249 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Segment outputs are open collector and active high. Each of a–g needs the
 * 10 kOhm pull-up from the README; these pins are plain inputs. BI/RBO is
 * either INPUT_PULLUP or a strong low. Never drive BI/RBO high.
 * A, B, C and D stay low, and LT and RBI stay high, until the check starts.
 */

const uint8_t PIN_B = 2;
const uint8_t PIN_C = 3;
const uint8_t PIN_LT = 4;
const uint8_t PIN_BI = 5;
const uint8_t PIN_RBI = 6;
const uint8_t PIN_D = 7;
const uint8_t PIN_A = 8;

// Bit 0 = a ... bit 6 = g. Six is 0x7D and nine is 0x6F because both have tails.
const uint8_t SEGMENT_PINS[7] = {13, 12, 11, 10, 9, A1, A0};
const uint8_t GLYPHS[16] = {
    0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07,
    0x7F, 0x6F, 0x58, 0x4C, 0x62, 0x69, 0x78, 0x00};

bool failed = false;
char resultLine[120];

void noteFailure(const char* step, unsigned expected, unsigned actual) {
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

void releaseBi() { pinMode(PIN_BI, INPUT_PULLUP); }

void forceBiLow() {
  digitalWrite(PIN_BI, LOW);
  pinMode(PIN_BI, OUTPUT);
}

uint8_t readSegments() {
  uint8_t value = 0;
  for (uint8_t bit = 0; bit < 7; bit++) {
    if (digitalRead(SEGMENT_PINS[bit])) value |= (uint8_t) (1u << bit);
  }
  return value;
}

void expectSegments(const char* step, uint8_t expected) {
  settle();
  const uint8_t actual = readSegments();
  if (actual != expected) noteFailure(step, expected, actual);
}

void expectRbo(const char* step, bool low) {
  settle();
  const bool actualLow = digitalRead(PIN_BI) == LOW;
  if (actualLow != low) noteFailure(step, low ? 0 : 1, actualLow ? 0 : 1);
}

void checkGlyphs() {
  releaseBi();
  digitalWrite(PIN_LT, HIGH);
  digitalWrite(PIN_RBI, HIGH);
  for (uint8_t code = 0; code <= 15; code++) {
    setCode(code);
    char step[16];
    snprintf(step, sizeof(step), "glyph %u", code);
    expectSegments(step, GLYPHS[code]);
  }
}

void checkBlankingOverridesLampTest() {
  setCode(8);
  digitalWrite(PIN_LT, LOW);
  forceBiLow();
  expectSegments("BI low", 0x00);
  releaseBi();
  digitalWrite(PIN_LT, HIGH);
}

void checkLampTest() {
  releaseBi();
  digitalWrite(PIN_RBI, HIGH);
  setCode(4);
  digitalWrite(PIN_LT, LOW);
  expectSegments("LT low", 0x7F);
  expectRbo("LT RBO", false);
  digitalWrite(PIN_LT, HIGH);
}

void checkRippleBlanking() {
  releaseBi();
  digitalWrite(PIN_LT, HIGH);
  digitalWrite(PIN_RBI, LOW);
  setCode(0);
  expectSegments("RBI zero", 0x00);
  expectRbo("RBI RBO", true);

  setCode(1);
  expectSegments("RBI one", GLYPHS[1]);
  expectRbo("RBI one RBO", false);
  digitalWrite(PIN_RBI, HIGH);
}

void checkLampTestOverridesRippleBlanking() {
  releaseBi();
  digitalWrite(PIN_RBI, LOW);
  setCode(0);
  digitalWrite(PIN_LT, LOW);
  expectSegments("LT over RBI", 0x7F);
  expectRbo("LT over RBI RBO", false);
  digitalWrite(PIN_LT, HIGH);
  digitalWrite(PIN_RBI, HIGH);
}

void runChecks() {
  checkGlyphs();
  checkBlankingOverridesLampTest();
  checkLampTest();
  checkRippleBlanking();
  checkLampTestOverridesRippleBlanking();
}

void setup() {
  pinMode(PIN_A, OUTPUT);
  pinMode(PIN_B, OUTPUT);
  pinMode(PIN_C, OUTPUT);
  pinMode(PIN_D, OUTPUT);
  pinMode(PIN_LT, OUTPUT);
  pinMode(PIN_RBI, OUTPUT);
  digitalWrite(PIN_A, LOW);
  digitalWrite(PIN_B, LOW);
  digitalWrite(PIN_C, LOW);
  digitalWrite(PIN_D, LOW);
  digitalWrite(PIN_LT, HIGH);
  digitalWrite(PIN_RBI, HIGH);
  releaseBi();
  for (uint8_t bit = 0; bit < 7; bit++) pinMode(SEGMENT_PINS[bit], INPUT);

  Serial.begin(115200);
  Serial.println("74249 ready, send any character");
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
