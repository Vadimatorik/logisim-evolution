/*
 * Self-check for a 74HC597 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * STCP loads D0..D7 into storage. PL low copies storage into the shift register,
 * so Q shows D7. SHCP shifts DS toward Q only while PL and MR are high.
 * MR low with PL high clears only the shift register. PL and MR both low is
 * invalid: the sketch prints the observed Q and then checks that MR can clear it.
 * Q is push-pull. MR stays low and PL stays high until the check starts.
 */

const uint8_t PIN_D[8] = {2, 3, 4, 5, 6, 7, 10, 11};
const uint8_t PIN_SHCP = 8;
const uint8_t PIN_STCP = 9;
const uint8_t PIN_DS = 12;
const uint8_t PIN_PL = A0;
const uint8_t PIN_MR = A1;
const uint8_t PIN_Q = A2;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, bool expectedHigh, bool actualHigh) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s expected %d got %d",
      step,
      expectedHigh ? 1 : 0,
      actualHigh ? 1 : 0);
}

void settle() { delay(1); }

void setParallel(uint8_t value) {
  for (uint8_t bit = 0; bit < 8; bit++) {
    digitalWrite(PIN_D[bit], (value & (1u << bit)) ? HIGH : LOW);
  }
}

void pulsePin(uint8_t pin) {
  digitalWrite(pin, HIGH);
  delayMicroseconds(2);
  digitalWrite(pin, LOW);
  delayMicroseconds(2);
}

// D8 (SHCP) and D9 (STCP) are PORTB bits 0 and 1. One write raises both edges.
void pulseBothClocks() {
  PORTB &= ~(_BV(PB0) | _BV(PB1));
  delayMicroseconds(2);
  PORTB |= (_BV(PB0) | _BV(PB1));
  delayMicroseconds(2);
  PORTB &= ~(_BV(PB0) | _BV(PB1));
}

bool readQ() {
  pinMode(PIN_Q, INPUT);
  return digitalRead(PIN_Q) == HIGH;
}

void expectQ(const char* step, bool high) {
  settle();
  const bool actual = readQ();
  Serial.print(step);
  Serial.print(" Q expected=");
  Serial.print(high ? "1" : "0");
  Serial.print(" actual=");
  Serial.print(actual ? "1" : "0");
  const bool pass = actual == high;
  Serial.println(pass ? " PASS" : " FAIL");
  if (!pass) noteFailure(step, high, actual);
}

void idle() {
  digitalWrite(PIN_MR, LOW);
  digitalWrite(PIN_PL, HIGH);
  digitalWrite(PIN_SHCP, LOW);
  digitalWrite(PIN_STCP, LOW);
  digitalWrite(PIN_DS, LOW);
  setParallel(0);
}

void releaseReset() {
  digitalWrite(PIN_MR, HIGH);
  settle();
}

void loadShift(uint8_t value) {
  setParallel(value);
  delayMicroseconds(2);
  pulsePin(PIN_STCP);
  digitalWrite(PIN_PL, LOW);
  settle();
  digitalWrite(PIN_PL, HIGH);
  settle();
}

void runChecks() {
  idle();
  expectQ("reset", false);
  releaseReset();
  expectQ("release-reset", false);

  setParallel(0x80);
  pulsePin(PIN_STCP);
  expectQ("storage-hidden", false);
  digitalWrite(PIN_PL, LOW);
  expectQ("load-d7", true);
  digitalWrite(PIN_PL, HIGH);
  setParallel(0x00);
  expectQ("hold-after-pins", true);
  digitalWrite(PIN_PL, LOW);
  expectQ("reload-stored", true);
  digitalWrite(PIN_PL, HIGH);

  // 0xA5 is D7..D0 = 1 0 1 0 0 1 0 1. Q starts at D7, then walks toward DS=0.
  loadShift(0xA5);
  expectQ("loaded-a5", true);
  digitalWrite(PIN_DS, LOW);
  const uint8_t shifted = 0x52;
  for (uint8_t bit = 0; bit < 8; bit++) {
    pulsePin(PIN_SHCP);
    char step[24];
    snprintf(step, sizeof(step), "shift %u", bit);
    expectQ(step, (shifted & (1u << bit)) != 0);
  }

  digitalWrite(PIN_MR, LOW);
  expectQ("clear-shift", false);
  releaseReset();
  setParallel(0x00);
  digitalWrite(PIN_PL, LOW);
  expectQ("storage-survived-clear", true);
  digitalWrite(PIN_PL, HIGH);

  digitalWrite(PIN_PL, LOW);
  setParallel(0x00);
  pulsePin(PIN_STCP);
  expectQ("store-while-load-low", false);
  setParallel(0x80);
  pulsePin(PIN_STCP);
  expectQ("store-d7-while-load", true);
  setParallel(0x00);
  expectQ("pins-without-clock", true);
  digitalWrite(PIN_DS, LOW);
  pulsePin(PIN_SHCP);
  expectQ("shift-ignored-while-load", true);
  digitalWrite(PIN_PL, HIGH);

  pulsePin(PIN_SHCP);
  expectQ("shift-rising", false);
  digitalWrite(PIN_SHCP, HIGH);
  expectQ("shift-held-high", false);
  digitalWrite(PIN_SHCP, LOW);
  expectQ("shift-falling", false);

  setParallel(0x00);
  pulsePin(PIN_STCP);
  digitalWrite(PIN_STCP, HIGH);
  setParallel(0xFF);
  digitalWrite(PIN_STCP, LOW);
  digitalWrite(PIN_PL, LOW);
  expectQ("storage-falling-ignored", false);
  digitalWrite(PIN_PL, HIGH);

  loadShift(0x40);
  expectQ("loaded-d6", false);
  setParallel(0x00);
  digitalWrite(PIN_DS, LOW);
  pulseBothClocks();
  expectQ("both-clocks-shift", true);
  digitalWrite(PIN_PL, LOW);
  expectQ("both-clocks-stored", false);
  digitalWrite(PIN_PL, HIGH);

  loadShift(0x80);
  digitalWrite(PIN_PL, LOW);
  digitalWrite(PIN_MR, LOW);
  settle();
  const bool illegal = readQ();
  Serial.print("illegal-both-low Q observed=");
  Serial.println(illegal ? "1" : "0");
  digitalWrite(PIN_PL, HIGH);
  digitalWrite(PIN_MR, HIGH);
  settle();
  digitalWrite(PIN_MR, LOW);
  expectQ("recover-clear", false);
}

void setup() {
  Serial.begin(115200);
  for (uint8_t bit = 0; bit < 8; bit++) pinMode(PIN_D[bit], OUTPUT);
  pinMode(PIN_SHCP, OUTPUT);
  pinMode(PIN_STCP, OUTPUT);
  pinMode(PIN_DS, OUTPUT);
  pinMode(PIN_PL, OUTPUT);
  pinMode(PIN_MR, OUTPUT);
  pinMode(PIN_Q, INPUT);
  idle();
  Serial.println("74HC597 bench. Send any character to start.");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  Serial.println(failed ? resultLine : "RESULT PASS");
}
