/*
 * Self-check for a 74HC520 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * A low G drives P=Q low only when P and Q match. Any difference, and a high G,
 * drive P=Q high. The output is push-pull. Bit 0 is P0/Q0. G stays high until
 * the pins are set.
 */

const uint8_t PIN_G = 2;
const uint8_t PIN_PQ = A5;
const uint8_t PIN_P[8] = {3, 5, 7, 9, 11, 13, A1, A3};
const uint8_t PIN_Q[8] = {4, 6, 8, 10, 12, A0, A2, A4};

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, const char* detail) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s %s", step, detail);
}

void settle() { delayMicroseconds(20); }

void writeWords(uint8_t wordP, uint8_t wordQ) {
  for (uint8_t bit = 0; bit < 8; bit++) {
    digitalWrite(PIN_P[bit], (wordP >> bit) & 1 ? HIGH : LOW);
    digitalWrite(PIN_Q[bit], (wordQ >> bit) & 1 ? HIGH : LOW);
  }
  settle();
}

void setEnable(bool enabled) {
  digitalWrite(PIN_G, enabled ? LOW : HIGH);
  settle();
}

void expectLevel(const char* step, bool low) {
  const bool actual = digitalRead(PIN_PQ);
  const bool expectedLow = low;
  if ((actual == LOW) == expectedLow) return;
  char detail[48];
  snprintf(detail, sizeof(detail), "P=Q expected %s got %s", low ? "L" : "H", actual == LOW ? "L" : "H");
  noteFailure(step, detail);
}

void expectEqual(uint8_t value, const char* step) {
  writeWords(value, value);
  expectLevel(step, true);
}

void expectHigh(uint8_t wordP, uint8_t wordQ, const char* step) {
  writeWords(wordP, wordQ);
  expectLevel(step, false);
}

void runChecks() {
  setEnable(true);
  expectEqual(0x00, "eq-00");
  expectEqual(0xFF, "eq-ff");
  expectEqual(0x55, "eq-55");
  expectEqual(0xAA, "eq-aa");
  for (uint8_t bit = 0; bit < 8; bit++) {
    char label[16];
    snprintf(label, sizeof(label), "walk-%u", bit);
    expectEqual(static_cast<uint8_t>(1u << bit), label);
  }

  for (uint8_t bit = 0; bit < 8; bit++) {
    char label[16];
    snprintf(label, sizeof(label), "diff-%u", bit);
    expectHigh(0xA5, static_cast<uint8_t>(0xA5 ^ (1u << bit)), label);
  }
  expectHigh(0xF0, 0x0F, "gt");
  expectHigh(0x0F, 0xF0, "lt");

  setEnable(false);
  expectHigh(0x55, 0x55, "dis-eq");
  expectHigh(0x55, 0xAA, "dis-ne");
}

void setup() {
  pinMode(PIN_G, OUTPUT);
  digitalWrite(PIN_G, HIGH);
  for (uint8_t bit = 0; bit < 8; bit++) {
    pinMode(PIN_P[bit], OUTPUT);
    pinMode(PIN_Q[bit], OUTPUT);
    digitalWrite(PIN_P[bit], LOW);
    digitalWrite(PIN_Q[bit], LOW);
  }
  pinMode(PIN_PQ, INPUT);

  Serial.begin(115200);
  Serial.println("Send any character to test the 74HC520");
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
