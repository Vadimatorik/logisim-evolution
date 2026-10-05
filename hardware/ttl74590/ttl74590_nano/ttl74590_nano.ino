/*
 * Self-check for a 74HC590 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * MRC is an active-low asynchronous counter clear and does not clear the
 * register. The counter advances on the rising edge of CPC while CE is low.
 * A rising CPR stores the counter. CPC and CPR are raised together with one
 * PORTD write, and the register then stays one count behind. RCO is low only
 * while the counter holds 255. OE high releases Q; those pins are then read
 * with the internal pull-ups.
 * MRC stays low, both clocks stay low, and CE and OE stay high until the
 * check starts.
 */

const uint8_t PIN_MRC = 2;
const uint8_t PIN_CPC = 3;
const uint8_t PIN_CE = 4;
const uint8_t PIN_CPR = 5;
const uint8_t PIN_OE = 6;
const uint8_t PIN_Q1 = 7;
const uint8_t PIN_Q2 = 8;
const uint8_t PIN_Q3 = 9;
const uint8_t PIN_Q4 = 10;
const uint8_t PIN_Q5 = 11;
const uint8_t PIN_Q6 = 12;
const uint8_t PIN_Q7 = A0;
const uint8_t PIN_RCO = A1;
const uint8_t PIN_Q0 = A2;

const uint8_t Q_PINS[8] = {PIN_Q0, PIN_Q1, PIN_Q2, PIN_Q3, PIN_Q4, PIN_Q5, PIN_Q6, PIN_Q7};
// D3 (CPC) and D5 (CPR) are both PORTD, so one write raises them together.
const uint8_t TIED_MASK = (1 << PD3) | (1 << PD5);

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

void qMode(uint8_t mode) {
  for (uint8_t bit = 0; bit < 8; bit++) pinMode(Q_PINS[bit], mode);
}

uint8_t readQ() {
  uint8_t value = 0;
  for (uint8_t bit = 0; bit < 8; bit++) {
    if (digitalRead(Q_PINS[bit])) value |= (uint8_t) (1 << bit);
  }
  return value;
}

void expectQ(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readQ();
  if (actual != expected) noteFailure(step, expected, actual);
}

void expectRco(bool high, const char* step) {
  settle();
  const uint8_t actual = digitalRead(PIN_RCO) ? 1 : 0;
  const uint8_t expected = high ? 1 : 0;
  if (actual != expected) noteFailure(step, expected, actual);
}

void pulse(uint8_t pin) {
  digitalWrite(pin, LOW);
  settle();
  digitalWrite(pin, HIGH);
  settle();
  digitalWrite(pin, LOW);
  settle();
}

void count(uint16_t pulses) {
  digitalWrite(PIN_CE, LOW);
  for (uint16_t i = 0; i < pulses; i++) pulse(PIN_CPC);
}

void capture() { pulse(PIN_CPR); }

void clearCounter() {
  digitalWrite(PIN_MRC, LOW);
  settle();
  digitalWrite(PIN_MRC, HIGH);
  settle();
}

void riseTied() {
  PORTD &= ~TIED_MASK;
  settle();
  PORTD |= TIED_MASK;
  settle();
}

void checkResetLeavesTheRegister() {
  digitalWrite(PIN_OE, LOW);
  clearCounter();
  capture();
  expectQ(0, "reset-capture");
  expectRco(true, "reset-rco");

  count(5);
  expectQ(0, "count-hidden");
  capture();
  expectQ(5, "count-stored");

  digitalWrite(PIN_MRC, LOW);
  settle();
  expectQ(5, "reset-keeps-register");
  expectRco(true, "reset-rco-high");
  pulse(PIN_CPC);
  digitalWrite(PIN_MRC, HIGH);
  settle();
  capture();
  expectQ(0, "reset-cleared-counter");
}

void checkCountEnableAndEdges() {
  digitalWrite(PIN_CE, HIGH);
  pulse(PIN_CPC);
  capture();
  expectQ(0, "ce-high-holds");

  // A rising edge while CE is high does not count, and the later fall does not either.
  digitalWrite(PIN_CPC, HIGH);
  settle();
  digitalWrite(PIN_CE, LOW);
  settle();
  digitalWrite(PIN_CPC, LOW);
  settle();
  capture();
  expectQ(0, "cpc-fall-holds");

  pulse(PIN_CPC);
  capture();
  expectQ(1, "cpc-rise-counts");

  count(2);
  capture();
  expectQ(3, "count-to-3");

  digitalWrite(PIN_CE, HIGH);
  pulse(PIN_CPC);
  digitalWrite(PIN_CPC, HIGH);
  settle();
  digitalWrite(PIN_CE, LOW);
  settle();
  digitalWrite(PIN_CE, HIGH);
  settle();
  digitalWrite(PIN_CPC, LOW);
  settle();
  digitalWrite(PIN_CE, LOW);
  settle();
  capture();
  expectQ(3, "ce-edge-holds");

  pulse(PIN_CPC);
  capture();
  expectQ(4, "ce-low-counts");
}

void checkRegisterClock() {
  clearCounter();
  capture();
  count(4);
  expectQ(0, "register-holds");
  digitalWrite(PIN_CPR, HIGH);
  settle();
  expectQ(4, "register-stores");

  count(3);
  expectQ(4, "register-stays");
  pulse(PIN_CPR);
  expectQ(7, "register-updates");
}

void checkTiedClocks() {
  clearCounter();
  capture();
  digitalWrite(PIN_CPC, LOW);
  digitalWrite(PIN_CPR, LOW);
  digitalWrite(PIN_CE, LOW);
  settle();

  for (uint8_t code = 0; code < 5; code++) {
    riseTied();
    char step[16];
    snprintf(step, sizeof(step), "tied-%02X", code);
    expectQ(code, step);
    PORTD &= ~TIED_MASK;
    settle();
  }
}

void checkCarry() {
  clearCounter();
  capture();
  expectRco(true, "carry-idle");
  count(254);
  expectRco(true, "carry-254");
  count(1);
  expectRco(false, "carry-255");
  capture();
  expectQ(255, "code-255");

  digitalWrite(PIN_CE, HIGH);
  settle();
  expectRco(false, "carry-holds-at-255");
  pulse(PIN_CPC);
  capture();
  expectQ(255, "ce-blocks-wrap");
  expectRco(false, "carry-still-low");

  digitalWrite(PIN_CE, LOW);
  pulse(PIN_CPC);
  expectRco(true, "carry-wrap");
  capture();
  expectQ(0, "code-wrap");
}

void checkBitWalk() {
  clearCounter();
  capture();
  uint8_t code = 0;
  for (uint8_t bit = 0; bit < 8; bit++) {
    count(1u << bit);
    code |= (uint8_t) (1 << bit);
    capture();
    char step[16];
    snprintf(step, sizeof(step), "bit-%02X", code);
    expectQ(code, step);
  }
}

void checkOutputEnable() {
  clearCounter();
  capture();
  expectQ(0, "oe-zero");
  qMode(INPUT_PULLUP);
  digitalWrite(PIN_OE, HIGH);
  expectQ(0xFF, "oe-release");
  expectRco(true, "oe-rco-stays");
  digitalWrite(PIN_OE, LOW);
  expectQ(0, "oe-drive");
  qMode(INPUT);
}

void run() {
  checkResetLeavesTheRegister();
  checkCountEnableAndEdges();
  checkRegisterClock();
  checkTiedClocks();
  checkCarry();
  checkBitWalk();
  checkOutputEnable();
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_MRC, OUTPUT);
  pinMode(PIN_CPC, OUTPUT);
  pinMode(PIN_CE, OUTPUT);
  pinMode(PIN_CPR, OUTPUT);
  pinMode(PIN_OE, OUTPUT);
  digitalWrite(PIN_MRC, LOW);
  digitalWrite(PIN_CPC, LOW);
  digitalWrite(PIN_CPR, LOW);
  digitalWrite(PIN_CE, HIGH);
  digitalWrite(PIN_OE, HIGH);
  qMode(INPUT);
  pinMode(PIN_RCO, INPUT);

  Serial.println("74HC590 ready. Send any character to start.");
  while (!Serial.available()) {
  }
  while (Serial.available()) Serial.read();
  run();
  Serial.println(failed ? resultLine : "RESULT PASS");
}

void loop() {}
