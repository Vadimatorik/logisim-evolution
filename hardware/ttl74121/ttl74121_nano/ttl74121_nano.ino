/*
 * Self-check for a 74HC121 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * One non-retriggerable monostable. With 10 kOhm from 5V to pin 11 and 100 nF
 * between pins 10 and 11, Q is high for about 700 us and nQ is the complement.
 * There is no reset. Falling A1 triggers when A2 and B are high. Falling A2
 * triggers when A1 and B are high. Both A inputs falling triggers when B is
 * high. Rising B triggers when A1 or A2 is low. Another edge during the pulse
 * does not extend it. A1 and A2 stay high and B stays low until the check starts.
 */

const uint8_t PIN_A1 = 2;
const uint8_t PIN_A2 = 3;
const uint8_t PIN_B = 4;
const uint8_t PIN_Q = 5;
const uint8_t PIN_NQ = 6;

const unsigned long WIDTH_MIN_US = 250;
const unsigned long WIDTH_MAX_US = 1400;
const unsigned long EDGE_TIMEOUT_US = 2000;
const unsigned long PULSE_TIMEOUT_US = 5000;

enum Kind { KIND_B_A1_LOW, KIND_B_A2_LOW, KIND_A1, KIND_A2, KIND_BOTH };

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, const char* detail) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s %s", step, detail);
}

void settle() { delayMicroseconds(20); }

void drive(bool a1, bool a2, bool b) {
  digitalWrite(PIN_A1, a1 ? HIGH : LOW);
  digitalWrite(PIN_A2, a2 ? HIGH : LOW);
  digitalWrite(PIN_B, b ? HIGH : LOW);
}

void expectLevels(bool qHigh, const char* step) {
  settle();
  const bool q = digitalRead(PIN_Q) == HIGH;
  const bool nq = digitalRead(PIN_NQ) == HIGH;
  if (q != qHigh || nq == qHigh) {
    char detail[24];
    snprintf(detail, sizeof(detail), "Q=%d nQ=%d", q ? 1 : 0, nq ? 1 : 0);
    noteFailure(step, detail);
  }
}

void arm(Kind kind, const char* step) {
  // B falls first. A edges while B is low do not trigger, and rising B with
  // both A inputs high does not trigger either. There is no reset pin, so a
  // pulse already in progress has to finish before the next edge.
  digitalWrite(PIN_B, LOW);
  if (kind == KIND_B_A1_LOW) {
    digitalWrite(PIN_A1, LOW);
    digitalWrite(PIN_A2, HIGH);
  } else if (kind == KIND_B_A2_LOW) {
    digitalWrite(PIN_A1, HIGH);
    digitalWrite(PIN_A2, LOW);
  } else {
    digitalWrite(PIN_A1, HIGH);
    digitalWrite(PIN_A2, HIGH);
    digitalWrite(PIN_B, HIGH);
  }
  settle();
  if (digitalRead(PIN_Q) == HIGH && waitUntil(micros(), PULSE_TIMEOUT_US, false) == 0) {
    noteFailure(step, "Q stayed high");
    return;
  }
  expectLevels(false, step);
}

void fire(Kind kind) {
  if (kind == KIND_B_A1_LOW || kind == KIND_B_A2_LOW) digitalWrite(PIN_B, HIGH);
  else if (kind == KIND_A1) digitalWrite(PIN_A1, LOW);
  else if (kind == KIND_A2) digitalWrite(PIN_A2, LOW);
  else {
    // D2 and D3 are PORTD bits 2 and 3, so both A inputs fall in one write.
    PORTD &= (uint8_t) ~((1 << PD2) | (1 << PD3));
  }
}

unsigned long waitUntil(unsigned long start, unsigned long limit, bool qHigh) {
  while ((unsigned long)(micros() - start) < limit) {
    if ((digitalRead(PIN_Q) == HIGH) == qHigh) return micros();
  }
  return 0;
}

unsigned long measurePulse(Kind kind, const char* step) {
  arm(kind, step);
  const unsigned long started = micros();
  fire(kind);
  const unsigned long rose = waitUntil(started, EDGE_TIMEOUT_US, true);
  if (rose == 0) {
    noteFailure(step, "Q stayed low");
    return 0;
  }
  expectLevels(true, step);
  const unsigned long fell = waitUntil(rose, PULSE_TIMEOUT_US, false);
  if (fell == 0) {
    noteFailure(step, "Q stayed high");
    return 0;
  }
  expectLevels(false, step);
  return fell - rose;
}

unsigned long checkWidth(Kind kind, const char* step) {
  const unsigned long width = measurePulse(kind, step);
  if (width == 0) return 0;
  Serial.print(step);
  Serial.print(' ');
  Serial.print(width);
  Serial.println(" us");
  if (width < WIDTH_MIN_US || width > WIDTH_MAX_US) {
    char detail[24];
    snprintf(detail, sizeof(detail), "%lu us", width);
    noteFailure(step, detail);
    return 0;
  }
  return width;
}

void checkOtherADoesNotTrigger(unsigned long width) {
  if (width == 0) return;
  const char* step = "a1-while-a2-low";
  digitalWrite(PIN_A1, LOW);
  delayMicroseconds(1500);
  expectLevels(false, step);
}

void checkNoRetrigger(unsigned long width) {
  if (width == 0) return;
  const char* step = "no-retrigger";
  arm(KIND_B_A1_LOW, step);
  const unsigned long started = micros();
  fire(KIND_B_A1_LOW);
  const unsigned long rose = waitUntil(started, EDGE_TIMEOUT_US, true);
  if (rose == 0) {
    noteFailure(step, "Q stayed low");
    return;
  }
  const unsigned long halfway = rose + width / 2;
  while ((long)(halfway - micros()) > 0) {
  }
  if (digitalRead(PIN_Q) == LOW) {
    noteFailure(step, "ended early");
    return;
  }
  digitalWrite(PIN_B, LOW);
  delayMicroseconds(8);
  digitalWrite(PIN_B, HIGH);
  const unsigned long afterOriginal = rose + width + 80;
  while ((long)(afterOriginal - micros()) > 0) {
  }
  expectLevels(false, step);
}

void checkBlocked() {
  const char* bothA = "block-both-a";
  drive(true, true, false);
  settle();
  digitalWrite(PIN_B, HIGH);
  delayMicroseconds(1500);
  expectLevels(false, bothA);

  const char* lowB = "block-b-low";
  drive(true, true, false);
  settle();
  digitalWrite(PIN_A1, LOW);
  delayMicroseconds(1500);
  expectLevels(false, lowB);
}

void runCheck() {
  drive(true, true, false);
  settle();
  expectLevels(false, "idle");
  const unsigned long width = checkWidth(KIND_B_A1_LOW, "rise-b-a1-low");
  checkWidth(KIND_B_A2_LOW, "rise-b-a2-low");
  checkWidth(KIND_A1, "fall-a1");
  const unsigned long fallA2 = checkWidth(KIND_A2, "fall-a2");
  checkOtherADoesNotTrigger(fallA2);
  checkWidth(KIND_BOTH, "fall-both-a");
  checkNoRetrigger(width);
  checkBlocked();
  drive(true, true, false);
}

void setup() {
  digitalWrite(PIN_A1, HIGH);
  digitalWrite(PIN_A2, HIGH);
  digitalWrite(PIN_B, LOW);
  pinMode(PIN_A1, OUTPUT);
  pinMode(PIN_A2, OUTPUT);
  pinMode(PIN_B, OUTPUT);
  pinMode(PIN_Q, INPUT);
  pinMode(PIN_NQ, INPUT);
  Serial.begin(115200);
  Serial.println("74HC121 bench. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  failed = false;
  resultLine[0] = '\0';
  runCheck();
  if (!failed) Serial.println("RESULT PASS");
  else Serial.println(resultLine);
}
