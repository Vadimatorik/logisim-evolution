/*
 * Self-check for a 74HC174 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * MR is active low and overrides the clock. While MR is high, Dn is stored on
 * the rising edge of CP and held on a falling edge or while the clock is steady.
 */

const uint8_t PIN_MR = 2;
const uint8_t PIN_CP = 6;
const uint8_t DATA_PINS[6] = {3, 4, 5, 7, 8, 9};
const uint8_t Q_PINS[6] = {10, 11, 12, A0, A1, A2};

bool failed = false;
char resultLine[160];

void noteFailure(const char* step, int expected, int actual) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s expected=0x%02X actual=0x%02X", step,
           expected, actual);
}

void setData(int pattern) {
  for (uint8_t bit = 0; bit < 6; bit++) {
    digitalWrite(DATA_PINS[bit], (pattern >> bit) & 1 ? HIGH : LOW);
  }
}

int readQ() {
  int value = 0;
  for (uint8_t bit = 0; bit < 6; bit++) {
    if (digitalRead(Q_PINS[bit]) == HIGH) value |= 1 << bit;
  }
  return value;
}

void expectQ(const char* step, int expected) {
  delay(1);
  const int actual = readQ();
  Serial.print(step);
  Serial.print(" expected=0x");
  Serial.print(expected, HEX);
  Serial.print(" actual=0x");
  Serial.print(actual, HEX);
  Serial.println(expected == actual ? " PASS" : " FAIL");
  if (expected != actual) noteFailure(step, expected, actual);
}

void risingEdge() {
  digitalWrite(PIN_CP, LOW);
  delay(1);
  digitalWrite(PIN_CP, HIGH);
}

void checkClearOverridesTheClock() {
  setData(0x3F);
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_MR, LOW);
  expectQ("clear", 0);

  risingEdge();
  expectQ("clear during rising edge", 0);

  digitalWrite(PIN_MR, HIGH);
  expectQ("clear release without edge", 0);

  digitalWrite(PIN_CP, LOW);
  expectQ("falling edge after clear", 0);

  risingEdge();
  expectQ("load after clear", 0x3F);
}

void checkEveryPattern() {
  for (int pattern = 0; pattern < 64; pattern++) {
    setData(pattern);
    risingEdge();
    char step[24];
    snprintf(step, sizeof(step), "load 0x%02X", pattern);
    expectQ(step, pattern);
  }
}

void checkHold() {
  setData(0x15);
  risingEdge();
  expectQ("load 0x15", 0x15);

  setData(0x2A);
  expectQ("hold while clock high", 0x15);

  digitalWrite(PIN_CP, LOW);
  expectQ("falling edge", 0x15);

  setData(0x38);
  expectQ("hold while clock low", 0x15);

  risingEdge();
  expectQ("load 0x38", 0x38);
}

void runTest() {
  failed = false;
  resultLine[0] = '\0';
  Serial.println("74HC174 test");
  checkClearOverridesTheClock();
  checkEveryPattern();
  checkHold();
  digitalWrite(PIN_MR, LOW);
  digitalWrite(PIN_CP, LOW);
  setData(0);
  Serial.println(failed ? resultLine : "RESULT PASS");
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_MR, OUTPUT);
  pinMode(PIN_CP, OUTPUT);
  digitalWrite(PIN_MR, LOW);
  digitalWrite(PIN_CP, LOW);
  for (uint8_t bit = 0; bit < 6; bit++) {
    pinMode(DATA_PINS[bit], OUTPUT);
    pinMode(Q_PINS[bit], INPUT);
  }
  setData(0);
  Serial.println("74HC174 ready. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  runTest();
}
