/*
 * Self-check for a 74HC199 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * MR is an asynchronous active-low clear. Load and shift are sampled on the
 * rising edge of CP, and only while CE is low. A high CE holds the register.
 * Parallel inputs D0-D7 come from a 74HC595. MR stays low, CP stays low and
 * CE stays high until the check starts, so the outputs should already be zero.
 */

const uint8_t PIN_J = 2;
const uint8_t PIN_K = 3;
const uint8_t PIN_Q0 = 4;
const uint8_t PIN_Q1 = 5;
const uint8_t PIN_Q2 = 6;
const uint8_t PIN_Q3 = 7;
const uint8_t PIN_CE = 8;
const uint8_t PIN_CP = 9;
const uint8_t PIN_MR = 10;
const uint8_t PIN_Q4 = 11;
const uint8_t PIN_Q5 = 12;
const uint8_t PIN_Q6 = A0;
const uint8_t PIN_Q7 = A1;
const uint8_t PIN_PE = A2;
const uint8_t PIN_SER = A3;
const uint8_t PIN_SRCLK = A4;
const uint8_t PIN_RCLK = A5;

const uint8_t Q_PINS[8] = {PIN_Q0, PIN_Q1, PIN_Q2, PIN_Q3, PIN_Q4, PIN_Q5, PIN_Q6, PIN_Q7};

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

void setData(uint8_t code) {
  for (int8_t bit = 7; bit >= 0; bit--) {
    digitalWrite(PIN_SER, (code & (1 << bit)) ? HIGH : LOW);
    digitalWrite(PIN_SRCLK, HIGH);
    settle();
    digitalWrite(PIN_SRCLK, LOW);
    settle();
  }
  digitalWrite(PIN_RCLK, HIGH);
  settle();
  digitalWrite(PIN_RCLK, LOW);
  settle();
}

uint8_t readWord() {
  uint8_t value = 0;
  for (uint8_t bit = 0; bit < 8; bit++) {
    if (digitalRead(Q_PINS[bit])) value |= (1 << bit);
  }
  return value;
}

void expectWord(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readWord();
  if (actual != expected) noteFailure(step, expected, actual);
}

void clockRise() {
  digitalWrite(PIN_CP, HIGH);
  settle();
}

void clockFall() {
  digitalWrite(PIN_CP, LOW);
  settle();
}

void clockPulse() {
  clockFall();
  clockRise();
  clockFall();
}

void load(uint8_t code) {
  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_CE, LOW);
  digitalWrite(PIN_PE, LOW);
  digitalWrite(PIN_J, LOW);
  digitalWrite(PIN_K, LOW);
  setData(code);
  clockPulse();
}

void shift(bool jHigh, bool kHigh) {
  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_CE, LOW);
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_J, jHigh ? HIGH : LOW);
  digitalWrite(PIN_K, kHigh ? HIGH : LOW);
  clockPulse();
}

void checkResetOverridesLoad() {
  expectWord(0, "reset before clock");

  digitalWrite(PIN_CE, LOW);
  digitalWrite(PIN_PE, LOW);
  setData(0xA5);
  expectWord(0, "reset ignores data");
  clockPulse();
  expectWord(0, "reset overrides clock");

  digitalWrite(PIN_MR, HIGH);
  expectWord(0, "load waits for edge");
  clockPulse();
  expectWord(0xA5, "load on rising edge");
}

void checkLoads() {
  const uint8_t codes[] = {0x00, 0xFF, 0xA5, 0x5A, 0x01, 0x80};
  for (uint8_t index = 0; index < sizeof(codes); index++) {
    load(codes[index]);
    expectWord(codes[index], "parallel load");
  }
}

void checkLoadIgnoresJk() {
  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_CE, LOW);
  digitalWrite(PIN_PE, LOW);
  digitalWrite(PIN_J, HIGH);
  digitalWrite(PIN_K, LOW);
  setData(0x00);
  clockPulse();
  expectWord(0x00, "load ignores JK");
}

void checkClockEnableHolds() {
  load(0xA5);
  digitalWrite(PIN_CE, HIGH);
  digitalWrite(PIN_PE, LOW);
  setData(0x5A);
  clockPulse();
  expectWord(0xA5, "CE holds load");

  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_J, HIGH);
  digitalWrite(PIN_K, HIGH);
  clockPulse();
  expectWord(0xA5, "CE holds shift");
}

void checkJkModes() {
  load(0x01);
  shift(true, true);
  expectWord(0x03, "JK set");

  load(0x01);
  shift(false, false);
  expectWord(0x02, "JK reset");

  load(0x01);
  shift(true, false);
  expectWord(0x02, "JK toggle");

  load(0x01);
  shift(false, true);
  expectWord(0x03, "JK retain");
}

void checkTiedJkIsData() {
  load(0x00);
  const uint8_t serial[] = {1, 1, 0, 1};
  uint8_t word = 0;
  for (uint8_t index = 0; index < sizeof(serial); index++) {
    const bool high = serial[index] != 0;
    shift(high, high);
    word = (uint8_t)((word << 1) | serial[index]);
    expectWord(word, "JK tied as D");
  }
}

void checkShiftTowardQ7() {
  load(0x01);
  uint8_t word = 0x01;
  for (uint8_t step = 0; step < 8; step++) {
    shift(false, false);
    word = (uint8_t)((word << 1) & 0xFF);
    expectWord(word, "shift toward Q7");
  }
}

void checkFallingEdge() {
  load(0x0A);
  digitalWrite(PIN_CE, LOW);
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_J, LOW);
  digitalWrite(PIN_K, LOW);
  settle();
  expectWord(0x0A, "shift waits for edge");
  clockFall();
  expectWord(0x0A, "falling edge");
  clockRise();
  expectWord(0x14, "shift on next rise");
}

void setup() {
  pinMode(PIN_J, OUTPUT);
  pinMode(PIN_K, OUTPUT);
  pinMode(PIN_CE, OUTPUT);
  pinMode(PIN_CP, OUTPUT);
  pinMode(PIN_MR, OUTPUT);
  pinMode(PIN_PE, OUTPUT);
  pinMode(PIN_SER, OUTPUT);
  pinMode(PIN_SRCLK, OUTPUT);
  pinMode(PIN_RCLK, OUTPUT);
  for (uint8_t bit = 0; bit < 8; bit++) pinMode(Q_PINS[bit], INPUT);

  digitalWrite(PIN_MR, LOW);
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_CE, HIGH);
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_J, LOW);
  digitalWrite(PIN_K, LOW);
  digitalWrite(PIN_SER, LOW);
  digitalWrite(PIN_SRCLK, LOW);
  digitalWrite(PIN_RCLK, LOW);
  setData(0);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  checkResetOverridesLoad();
  checkLoads();
  checkLoadIgnoresJk();
  checkClockEnableHolds();
  checkJkModes();
  checkTiedJkIsData();
  checkShiftTowardQ7();
  checkFallingEdge();

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
