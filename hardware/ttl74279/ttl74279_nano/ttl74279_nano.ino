/*
 * Self-check for a 74HC279 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Set and reset are active low. Latches 1 and 3 set when either set input is
 * low. While both set and reset are low, Q is high. Releasing one input first
 * follows the input that stays low. Releasing both together is only printed:
 * latches 1-3 are released by one port write, latch 4 by two writes with 4S
 * first. Outputs are push-pull, so the Q pins are read directly. Every S and
 * R input stays high until the check starts.
 */

const uint8_t PIN_1R = 2;
const uint8_t PIN_1S1 = 3;
const uint8_t PIN_1S2 = 4;
const uint8_t PIN_1Q = 5;
const uint8_t PIN_2R = 6;
const uint8_t PIN_2S = 7;
const uint8_t PIN_2Q = 8;
const uint8_t PIN_3Q = 9;
const uint8_t PIN_3R = 10;
const uint8_t PIN_3S1 = 11;
const uint8_t PIN_3S2 = 12;
const uint8_t PIN_4S = 13;
const uint8_t PIN_4R = A0;
const uint8_t PIN_4Q = A1;

const uint8_t PIN_Q[4] = {PIN_1Q, PIN_2Q, PIN_3Q, PIN_4Q};

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, uint8_t latch, bool expectedHigh, bool actualHigh) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s latch %u expected %u got %u",
      step,
      latch + 1,
      expectedHigh ? 1 : 0,
      actualHigh ? 1 : 0);
}

void settle() { delay(1); }

void setLevel(uint8_t pin, bool high) { digitalWrite(pin, high ? HIGH : LOW); }

void drive(uint8_t latch, bool setAHigh, bool setBHigh, bool resetHigh) {
  switch (latch) {
    case 0:
      setLevel(PIN_1S1, setAHigh);
      setLevel(PIN_1S2, setBHigh);
      setLevel(PIN_1R, resetHigh);
      break;
    case 1:
      setLevel(PIN_2S, setAHigh);
      setLevel(PIN_2R, resetHigh);
      break;
    case 2:
      setLevel(PIN_3S1, setAHigh);
      setLevel(PIN_3S2, setBHigh);
      setLevel(PIN_3R, resetHigh);
      break;
    default:
      setLevel(PIN_4S, setAHigh);
      setLevel(PIN_4R, resetHigh);
      break;
  }
}

void hold(uint8_t latch) { drive(latch, true, true, true); }

void holdAll() {
  for (uint8_t latch = 0; latch < 4; latch++) hold(latch);
}

bool readLatch(uint8_t latch) { return digitalRead(PIN_Q[latch]) == HIGH; }

void expectLatch(uint8_t latch, bool high, const char* step) {
  settle();
  const bool actual = readLatch(latch);
  if (actual != high) noteFailure(step, latch, high, actual);
}

void expectAll(bool q1, bool q2, bool q3, bool q4, const char* step) {
  const bool expected[4] = {q1, q2, q3, q4};
  settle();
  for (uint8_t latch = 0; latch < 4; latch++) {
    const bool actual = readLatch(latch);
    if (actual != expected[latch]) noteFailure(step, latch, expected[latch], actual);
  }
}

void resetLatch(uint8_t latch) {
  drive(latch, true, true, false);
  expectLatch(latch, false, "reset");
  hold(latch);
  expectLatch(latch, false, "reset-hold");
}

void checkSetInput(uint8_t latch, bool useSecondSet, const char* step) {
  resetLatch(latch);
  if (useSecondSet) drive(latch, true, false, true);
  else drive(latch, false, true, true);
  expectLatch(latch, true, step);
  hold(latch);
  expectLatch(latch, true, "set-hold");
}

void checkPartialSetRelease(uint8_t latch) {
  resetLatch(latch);
  drive(latch, false, false, false);
  expectLatch(latch, true, "dual-both-low");
  drive(latch, true, false, false);
  expectLatch(latch, true, "one-set-still-low");
  drive(latch, true, true, false);
  expectLatch(latch, false, "both-sets-released");
  hold(latch);
  expectLatch(latch, false, "hold-after-partial-release");
}

void checkOrderedRelease(uint8_t latch) {
  resetLatch(latch);
  drive(latch, false, false, false);
  expectLatch(latch, true, "both-low");

  drive(latch, false, false, true);
  expectLatch(latch, true, "release-reset-first");
  hold(latch);
  expectLatch(latch, true, "hold-after-set");

  drive(latch, false, false, false);
  expectLatch(latch, true, "both-low-again");
  drive(latch, true, true, false);
  expectLatch(latch, false, "release-set-first");
  hold(latch);
  expectLatch(latch, false, "hold-after-reset");
}

void releaseTogether(uint8_t latch) {
  holdAll();
  drive(latch, false, false, false);
  settle();
  noInterrupts();
  switch (latch) {
    case 0:
      PORTD |= _BV(PIN_1R) | _BV(PIN_1S1) | _BV(PIN_1S2);
      break;
    case 1:
      PORTD |= _BV(PIN_2R) | _BV(PIN_2S);
      break;
    case 2:
      PORTB |= _BV(PIN_3R - 8) | _BV(PIN_3S1 - 8) | _BV(PIN_3S2 - 8);
      break;
    default:
      PORTB |= _BV(PIN_4S - 8);
      PORTC |= _BV(PIN_4R - A0);
      break;
  }
  interrupts();
  settle();
}

void observeSimultaneousRelease() {
  releaseTogether(0);
  releaseTogether(1);
  releaseTogether(2);
  releaseTogether(3);
  Serial.print("OBSERVE simultaneous-release");
  for (uint8_t latch = 0; latch < 4; latch++) {
    Serial.print(" ");
    Serial.print(latch + 1);
    Serial.print("Q=");
    Serial.print(readLatch(latch) ? "1" : "0");
  }
  Serial.println();
}

void runChecks() {
  holdAll();
  for (uint8_t latch = 0; latch < 4; latch++) resetLatch(latch);
  expectAll(false, false, false, false, "all-reset");

  checkSetInput(0, false, "set-1s1");
  checkSetInput(0, true, "set-1s2");
  checkSetInput(1, false, "set-2s");
  checkSetInput(2, false, "set-3s1");
  checkSetInput(2, true, "set-3s2");
  checkSetInput(3, false, "set-4s");

  for (uint8_t latch = 0; latch < 4; latch++) resetLatch(latch);
  drive(0, false, true, true);
  drive(2, true, false, true);
  expectAll(true, false, true, false, "independent-set");
  drive(1, false, true, true);
  expectAll(true, true, true, false, "independent-neighbor");
  holdAll();
  expectAll(true, true, true, false, "independent-hold");

  checkPartialSetRelease(0);
  checkPartialSetRelease(2);
  checkOrderedRelease(0);
  checkOrderedRelease(1);
  checkOrderedRelease(2);
  checkOrderedRelease(3);

  observeSimultaneousRelease();
}

void setup() {
  Serial.begin(115200);
  const uint8_t outputs[] = {
      PIN_1R, PIN_1S1, PIN_1S2, PIN_2R, PIN_2S, PIN_3R, PIN_3S1, PIN_3S2, PIN_4R, PIN_4S};
  for (uint8_t index = 0; index < sizeof(outputs); index++) {
    digitalWrite(outputs[index], HIGH);
    pinMode(outputs[index], OUTPUT);
  }
  for (uint8_t latch = 0; latch < 4; latch++) pinMode(PIN_Q[latch], INPUT);
  Serial.println("74HC279 ready. Send any character to start.");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  Serial.println(failed ? resultLine : "RESULT PASS");
}
