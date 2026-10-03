/*
 * Self-check for a 74HC259 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * LE (TI G) and MR (TI CLR) are active low. MR low and LE high clears every
 * output. Both low: the addressed output follows D and the others are forced
 * low. MR high and LE low: only the addressed output follows D. Both high:
 * the outputs hold. Outputs are push-pull, so the Q pins are read directly.
 * MR stays low and LE stays high until the check starts.
 */

const uint8_t PIN_A0 = 2;
const uint8_t PIN_A1 = 3;
const uint8_t PIN_A2 = 4;
const uint8_t PIN_Q[8] = {5, 6, 7, 8, 9, 10, 11, 12};
const uint8_t PIN_D = 13;
const uint8_t PIN_LE = A0;
const uint8_t PIN_MR = A1;

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

void setAddress(uint8_t address) {
  digitalWrite(PIN_A0, (address & 1) ? HIGH : LOW);
  digitalWrite(PIN_A1, (address & 2) ? HIGH : LOW);
  digitalWrite(PIN_A2, (address & 4) ? HIGH : LOW);
}

void setData(bool high) { digitalWrite(PIN_D, high ? HIGH : LOW); }

uint8_t readOutputs() {
  uint8_t value = 0;
  for (uint8_t bit = 0; bit < 8; bit++) {
    if (digitalRead(PIN_Q[bit])) value |= static_cast<uint8_t>(1u << bit);
  }
  return value;
}

void expect(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readOutputs();
  if (actual != expected) noteFailure(step, expected, actual);
}

void enterReset() {
  digitalWrite(PIN_LE, HIGH);
  digitalWrite(PIN_MR, LOW);
  settle();
}

void enterDemux() {
  digitalWrite(PIN_MR, LOW);
  digitalWrite(PIN_LE, LOW);
  settle();
}

void enterLatch() {
  digitalWrite(PIN_MR, HIGH);
  settle();
  digitalWrite(PIN_LE, LOW);
  settle();
}

void enterMemory() {
  digitalWrite(PIN_LE, HIGH);
  settle();
}

void runChecks() {
  enterReset();
  for (uint8_t address = 0; address < 8; address++) {
    setAddress(address);
    setData(true);
    expect(0, "reset");
  }

  enterDemux();
  for (uint8_t address = 0; address < 8; address++) {
    setAddress(address);
    setData(true);
    expect(static_cast<uint8_t>(1u << address), "demux-high");
    setData(false);
    expect(0, "demux-low");
  }

  setAddress(0);
  setData(true);
  expect(0x01, "demux-q0");
  setAddress(1);
  expect(0x02, "demux-q1");

  enterReset();
  expect(0, "reset-before-latch");
  setAddress(0);
  setData(true);
  enterLatch();
  expect(0x01, "latch-q0");
  setAddress(1);
  expect(0x03, "latch-q0q1");
  setData(false);
  expect(0x01, "latch-clear-q1");

  enterMemory();
  setAddress(4);
  setData(true);
  expect(0x01, "memory-hold");

  digitalWrite(PIN_LE, LOW);
  expect(0x11, "latch-q4");

  setData(true);
  for (uint8_t address = 0; address < 8; address++) {
    setAddress(address);
    expect(static_cast<uint8_t>(0x11 | (1u << address)), "latch-fill");
  }
  expect(0xFF, "latch-all");

  setAddress(3);
  setData(true);
  enterDemux();
  expect(0x08, "demux-after-ones");

  digitalWrite(PIN_MR, HIGH);
  expect(0x08, "latch-after-demux");
  setData(false);
  expect(0x00, "latch-clear-addressed");

  setAddress(0);
  setData(true);
  expect(0x01, "latch-q0-again");
  enterDemux();
  setAddress(2);
  expect(0x04, "demux-q2");
  digitalWrite(PIN_MR, HIGH);
  settle();
  digitalWrite(PIN_LE, HIGH);
  setAddress(7);
  setData(false);
  expect(0x04, "memory-after-demux");
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_A0, OUTPUT);
  pinMode(PIN_A1, OUTPUT);
  pinMode(PIN_A2, OUTPUT);
  pinMode(PIN_D, OUTPUT);
  pinMode(PIN_LE, OUTPUT);
  pinMode(PIN_MR, OUTPUT);
  for (uint8_t bit = 0; bit < 8; bit++) pinMode(PIN_Q[bit], INPUT);
  setAddress(0);
  setData(false);
  digitalWrite(PIN_LE, HIGH);
  digitalWrite(PIN_MR, LOW);
  Serial.println("74HC259 ready. Send any character to start.");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  Serial.println(failed ? resultLine : "RESULT PASS");
}
