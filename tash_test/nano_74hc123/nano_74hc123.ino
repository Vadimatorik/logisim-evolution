/*
 * Self-check for a 74HC123 or 74HCT123 wired to an Arduino Nano as described
 * in ../wiring_74hc123.md. Open Serial Monitor at 115200 baud and send any
 * character to start. The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * R_KOHM and C_PF must match the parts on the board. The expected width is
 * the same 5 V formula as the simulator: tW(ns) = 0.45 * R(kOhm) * C(pF).
 */

const unsigned long R_KOHM = 10;
const unsigned long C_PF = 100000UL;
const uint8_t TOLERANCE_PERCENT = 50;

struct Half {
  const char* name;
  uint8_t a;
  uint8_t b;
  uint8_t rd;
  uint8_t q;
  uint8_t qbar;
};

const Half HALF1 = {"1", 2, 3, 4, 11, 8};
const Half HALF2 = {"2", 5, 6, 7, 9, 10};

bool failed = false;
char resultLine[180];
unsigned long referenceUs = 0;

unsigned long expectedUs() { return (45ULL * R_KOHM * C_PF) / 100000ULL; }

void noteFailure(const char* half, const char* note) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL half=%s note=%s", half, note);
}

void noteFailureValue(const char* half, const char* note, unsigned long actual,
                      unsigned long expected) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine),
           "RESULT FAIL half=%s note=%s actual=%lu expected=%lu", half, note, actual, expected);
}

bool within(unsigned long measured, unsigned long reference) {
  if (reference == 0 || measured == 0) return false;
  const unsigned long delta = reference * TOLERANCE_PERCENT / 100UL;
  const unsigned long low = reference > delta ? reference - delta : 0;
  return measured >= low && measured <= reference + delta;
}

int readBit(uint8_t pin) { return digitalRead(pin) == HIGH ? 1 : 0; }

void expectIdle(const Half& half, const char* note) {
  if (readBit(half.q) != 0 || readBit(half.qbar) != 1) {
    noteFailure(half.name, note);
  }
}

void expectActive(const Half& half, const char* note) {
  if (readBit(half.q) != 1 || readBit(half.qbar) != 0) {
    noteFailure(half.name, note);
  }
}

void holdReset(const Half& half) {
  digitalWrite(half.a, HIGH);
  digitalWrite(half.b, LOW);
  digitalWrite(half.rd, LOW);
}

void releaseReset(const Half& half) {
  digitalWrite(half.a, HIGH);
  digitalWrite(half.b, LOW);
  delayMicroseconds(5);
  digitalWrite(half.rd, HIGH);
  delayMicroseconds(5);
}

void settle(const Half& half) {
  const unsigned long timeout = expectedUs() * 4UL + 1000UL;
  const unsigned long start = micros();
  while (digitalRead(half.q) == HIGH) {
    if (micros() - start > timeout) break;
  }
  delayMicroseconds(50);
}

unsigned long awaitLevel(uint8_t pin, int level, unsigned long timeoutUs) {
  const unsigned long start = micros();
  while (digitalRead(pin) != level) {
    if (micros() - start > timeoutUs) return 0;
  }
  return micros();
}

unsigned long highTimeAfter(uint8_t qPin) {
  const unsigned long timeout = expectedUs() * 4UL + 2000UL;
  if (awaitLevel(qPin, HIGH, timeout) == 0) return 0;
  const unsigned long rose = micros();
  if (awaitLevel(qPin, LOW, timeout) == 0) return 0;
  return micros() - rose;
}

void triggerRisingB(const Half& half) {
  digitalWrite(half.rd, HIGH);
  digitalWrite(half.a, LOW);
  digitalWrite(half.b, LOW);
  delayMicroseconds(5);
  digitalWrite(half.b, HIGH);
}

void triggerFallingA(const Half& half) {
  digitalWrite(half.rd, HIGH);
  digitalWrite(half.b, HIGH);
  digitalWrite(half.a, HIGH);
  delayMicroseconds(5);
  digitalWrite(half.a, LOW);
}

void triggerRisingRd(const Half& half) {
  digitalWrite(half.a, LOW);
  digitalWrite(half.b, HIGH);
  digitalWrite(half.rd, LOW);
  delayMicroseconds(5);
  digitalWrite(half.rd, HIGH);
}

void reportWidth(const Half& half, const char* edge, unsigned long measured) {
  Serial.print("WIDTH half=");
  Serial.print(half.name);
  Serial.print(" edge=");
  Serial.print(edge);
  Serial.print(" us=");
  Serial.print(measured);
  Serial.print(" expected=");
  Serial.println(expectedUs());
}

unsigned long measureEdge(const Half& half, const char* edge, uint8_t mode) {
  holdReset(half);
  delayMicroseconds(20);
  releaseReset(half);
  if (mode == 0) triggerRisingB(half);
  else if (mode == 1) triggerFallingA(half);
  else triggerRisingRd(half);
  const unsigned long measured = highTimeAfter(half.q);
  reportWidth(half, edge, measured);
  settle(half);
  return measured;
}

void checkIdleAndBlockedEdges() {
  holdReset(HALF1);
  holdReset(HALF2);
  delayMicroseconds(20);
  releaseReset(HALF1);
  releaseReset(HALF2);
  expectIdle(HALF1, "idle after reset");
  expectIdle(HALF2, "idle after reset");

  digitalWrite(HALF1.a, HIGH);
  digitalWrite(HALF1.b, LOW);
  delayMicroseconds(5);
  digitalWrite(HALF1.b, HIGH);
  delayMicroseconds(20);
  expectIdle(HALF1, "rising B while A is high");

  digitalWrite(HALF1.b, LOW);
  digitalWrite(HALF1.a, HIGH);
  delayMicroseconds(5);
  digitalWrite(HALF1.a, LOW);
  delayMicroseconds(20);
  expectIdle(HALF1, "falling A while B is low");
  expectIdle(HALF2, "other half moved");
}

void checkTriggerModes() {
  referenceUs = measureEdge(HALF1, "B", 0);
  if (!within(referenceUs, expectedUs())) {
    noteFailureValue(HALF1.name, "B pulse width", referenceUs, expectedUs());
  }
  const unsigned long fallingA = measureEdge(HALF1, "A", 1);
  if (!within(fallingA, expectedUs())) {
    noteFailureValue(HALF1.name, "A pulse width", fallingA, expectedUs());
  }
  const unsigned long risingRd = measureEdge(HALF1, "RD", 2);
  if (!within(risingRd, expectedUs())) {
    noteFailureValue(HALF1.name, "RD pulse width", risingRd, expectedUs());
  }
  const unsigned long other = measureEdge(HALF2, "B", 0);
  if (!within(other, expectedUs())) {
    noteFailureValue(HALF2.name, "B pulse width", other, expectedUs());
  }
}

void checkHalvesAreIndependent() {
  holdReset(HALF1);
  holdReset(HALF2);
  releaseReset(HALF1);
  releaseReset(HALF2);
  triggerRisingB(HALF1);
  delayMicroseconds(30);
  expectActive(HALF1, "section 1 did not trigger");
  expectIdle(HALF2, "section 1 disturbed section 2");
  settle(HALF1);
  expectIdle(HALF2, "section 2 changed after section 1");
}

void checkResetAborts() {
  releaseReset(HALF1);
  triggerRisingB(HALF1);
  delayMicroseconds(referenceUs / 3UL);
  expectActive(HALF1, "pulse ended before reset");
  digitalWrite(HALF1.rd, LOW);
  delayMicroseconds(20);
  expectIdle(HALF1, "reset did not clear the pulse");
  delayMicroseconds(referenceUs);
  expectIdle(HALF1, "pulse continued after reset");
}

void checkLevelsDoNotAbort() {
  releaseReset(HALF1);
  triggerRisingB(HALF1);
  delayMicroseconds(20);
  digitalWrite(HALF1.a, HIGH);
  digitalWrite(HALF1.b, LOW);
  delayMicroseconds(referenceUs / 3UL);
  expectActive(HALF1, "A or B level aborted the pulse");
  settle(HALF1);
  expectIdle(HALF1, "pulse did not end");
}

void checkRetriggerExtends() {
  releaseReset(HALF1);
  triggerRisingB(HALF1);
  delayMicroseconds(referenceUs / 3UL);
  expectActive(HALF1, "pulse ended before retrigger");
  const unsigned long retriggeredAt = micros();
  digitalWrite(HALF1.b, LOW);
  delayMicroseconds(5);
  digitalWrite(HALF1.b, HIGH);
  if (awaitLevel(HALF1.q, LOW, expectedUs() * 4UL) == 0) {
    noteFailure(HALF1.name, "retrigger pulse did not end");
    digitalWrite(HALF1.rd, LOW);
    return;
  }
  const unsigned long tail = micros() - retriggeredAt;
  Serial.print("RETRIGGER tail_us=");
  Serial.print(tail);
  Serial.print(" reference_us=");
  Serial.println(referenceUs);
  if (!within(tail, referenceUs)) {
    noteFailureValue(HALF1.name, "retrigger did not restart the width", tail, referenceUs);
  }
  settle(HALF1);
}

void runChecks() {
  failed = false;
  resultLine[0] = '\0';
  referenceUs = 0;
  checkIdleAndBlockedEdges();
  checkTriggerModes();
  if (referenceUs == 0) {
    noteFailure(HALF1.name, "no reference pulse");
  } else {
    checkHalvesAreIndependent();
    checkResetAborts();
    checkLevelsDoNotAbort();
    checkRetriggerExtends();
  }
  holdReset(HALF1);
  holdReset(HALF2);
  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void setupOutput(uint8_t pin, uint8_t level) {
  pinMode(pin, OUTPUT);
  digitalWrite(pin, level);
}

void setup() {
  setupOutput(HALF1.a, HIGH);
  setupOutput(HALF1.b, LOW);
  setupOutput(HALF1.rd, LOW);
  setupOutput(HALF2.a, HIGH);
  setupOutput(HALF2.b, LOW);
  setupOutput(HALF2.rd, LOW);
  pinMode(HALF1.q, INPUT);
  pinMode(HALF1.qbar, INPUT);
  pinMode(HALF2.q, INPUT);
  pinMode(HALF2.qbar, INPUT);

  Serial.begin(115200);
  Serial.println("74HC123 bench. Send any character to start.");
  while (Serial.read() < 0) {
    delay(10);
  }
  while (Serial.read() >= 0) {
  }
  runChecks();
}

void loop() {
  delay(2000);
  if (resultLine[0] != '\0') {
    Serial.println(resultLine);
  }
}
