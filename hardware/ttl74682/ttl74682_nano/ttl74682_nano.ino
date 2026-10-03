/*
 * Self-check for a 74HC682 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * P=Q is low only when the unsigned words match. P>Q is low only when P is
 * greater. Both outputs are high when P is less, and both are push-pull.
 * Bit 0 is P0/Q0.
 */

const uint8_t PIN_PGTQ = 2;
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

void expectRelation(const char* step, uint8_t wordP, uint8_t wordQ) {
  writeWords(wordP, wordQ);
  const bool equal = wordP == wordQ;
  const bool greater = wordP > wordQ;
  const bool pqLow = digitalRead(PIN_PQ) == LOW;
  const bool pgtqLow = digitalRead(PIN_PGTQ) == LOW;
  if (pqLow == equal && pgtqLow == greater) return;
  char detail[72];
  snprintf(
      detail,
      sizeof(detail),
      "P=%02X Q=%02X P=Q %s/%s P>Q %s/%s",
      wordP,
      wordQ,
      equal ? "L" : "H",
      pqLow ? "L" : "H",
      greater ? "L" : "H",
      pgtqLow ? "L" : "H");
  noteFailure(step, detail);
}

void runChecks() {
  const uint8_t equals[] = {0x00, 0xFF, 0x55, 0xAA};
  for (uint8_t index = 0; index < sizeof(equals); index++) {
    char label[16];
    snprintf(label, sizeof(label), "eq-%02X", equals[index]);
    expectRelation(label, equals[index], equals[index]);
  }
  for (uint8_t bit = 0; bit < 8; bit++) {
    char label[16];
    const uint8_t pattern = static_cast<uint8_t>(1u << bit);
    snprintf(label, sizeof(label), "walk-%u", bit);
    expectRelation(label, pattern, pattern);
    snprintf(label, sizeof(label), "gt-bit-%u", bit);
    expectRelation(label, pattern, 0x00);
    snprintf(label, sizeof(label), "lt-bit-%u", bit);
    expectRelation(label, 0x00, pattern);
  }
  expectRelation("gt-80-7f", 0x80, 0x7F);
  expectRelation("lt-7f-80", 0x7F, 0x80);
  expectRelation("gt-01-00", 0x01, 0x00);
  expectRelation("lt-00-01", 0x00, 0x01);
  expectRelation("gt-ff-fe", 0xFF, 0xFE);
  expectRelation("lt-fe-ff", 0xFE, 0xFF);
}

void setup() {
  for (uint8_t bit = 0; bit < 8; bit++) {
    pinMode(PIN_P[bit], OUTPUT);
    pinMode(PIN_Q[bit], OUTPUT);
    digitalWrite(PIN_P[bit], LOW);
    digitalWrite(PIN_Q[bit], LOW);
  }
  pinMode(PIN_PGTQ, INPUT);
  pinMode(PIN_PQ, INPUT);

  Serial.begin(115200);
  Serial.println("Send any character to test the 74HC682");
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
