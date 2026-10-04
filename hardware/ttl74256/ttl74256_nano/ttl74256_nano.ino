/*
 * Self-check for a 74LS256 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * E and CL (74F256 MR) are active low. E high and CL low clears both sections.
 * Both low: the addressed output of each section follows its own D and the
 * other three outputs of that section are forced low. E low and CL high: only
 * the addressed output follows D. Both high: the outputs hold. Address changes
 * are made only while E is high. Outputs are push-pull, so the Q pins are read
 * directly. E stays high and CL stays low until the check starts.
 *
 * The byte layout is Q3b Q2b Q1b Q0b Q3a Q2a Q1a Q0a.
 */

const uint8_t PIN_A0 = 2;
const uint8_t PIN_A1 = 3;
const uint8_t PIN_DA = 4;
const uint8_t PIN_DB = 5;
const uint8_t PIN_E = 6;
const uint8_t PIN_CL = 7;
const uint8_t PIN_Q0A = 8;
const uint8_t PIN_Q1A = 9;
const uint8_t PIN_Q2A = 10;
const uint8_t PIN_Q3A = 11;
const uint8_t PIN_Q0B = 12;
const uint8_t PIN_Q1B = 13;
const uint8_t PIN_Q2B = A0;
const uint8_t PIN_Q3B = A1;

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
}

void setData(bool dataA, bool dataB) {
  digitalWrite(PIN_DA, dataA ? HIGH : LOW);
  digitalWrite(PIN_DB, dataB ? HIGH : LOW);
}

uint8_t readOutputs() {
  uint8_t value = 0;
  if (digitalRead(PIN_Q0A)) value |= 0x01;
  if (digitalRead(PIN_Q1A)) value |= 0x02;
  if (digitalRead(PIN_Q2A)) value |= 0x04;
  if (digitalRead(PIN_Q3A)) value |= 0x08;
  if (digitalRead(PIN_Q0B)) value |= 0x10;
  if (digitalRead(PIN_Q1B)) value |= 0x20;
  if (digitalRead(PIN_Q2B)) value |= 0x40;
  if (digitalRead(PIN_Q3B)) value |= 0x80;
  return value;
}

void expect(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readOutputs();
  if (actual != expected) noteFailure(step, expected, actual);
}

void enterReset() {
  digitalWrite(PIN_E, HIGH);
  digitalWrite(PIN_CL, LOW);
  settle();
}

void enterDemux() {
  digitalWrite(PIN_E, LOW);
  settle();
}

void enterMemory() {
  // CL rises before E. Raising E first while CL is low would clear the latches.
  digitalWrite(PIN_CL, HIGH);
  settle();
  digitalWrite(PIN_E, HIGH);
  settle();
}

void enterLatch() {
  digitalWrite(PIN_CL, HIGH);
  settle();
  digitalWrite(PIN_E, LOW);
  settle();
}

void runChecks() {
  enterReset();
  for (uint8_t address = 0; address < 4; address++) {
    setAddress(address);
    setData(true, true);
    expect(0x00, "reset");
  }

  for (uint8_t address = 0; address < 4; address++) {
    enterReset();
    setAddress(address);
    setData(true, false);
    enterDemux();
    expect(static_cast<uint8_t>(1u << address), "demux-a");

    enterReset();
    setData(false, true);
    enterDemux();
    expect(static_cast<uint8_t>(0x10u << address), "demux-b");
  }

  enterReset();
  setAddress(0);
  setData(true, false);
  enterLatch();
  expect(0x01, "latch-a0");

  enterMemory();
  setAddress(1);
  setData(false, true);
  enterLatch();
  expect(0x21, "latch-a0-b1");

  enterMemory();
  setAddress(1);
  setData(true, false);
  enterLatch();
  expect(0x03, "latch-a0a1");

  enterMemory();
  setAddress(3);
  setData(true, true);
  expect(0x03, "memory-hold");

  setAddress(2);
  setData(true, false);
  expect(0x03, "memory-before-demux");
  enterLatch();
  expect(0x07, "latch-before-demux");

  digitalWrite(PIN_CL, LOW);
  settle();
  expect(0x04, "demux-after-ones");

  digitalWrite(PIN_CL, HIGH);
  settle();
  enterMemory();
  setAddress(0);
  setData(false, true);
  expect(0x04, "memory-after-demux");

  enterLatch();
  expect(0x14, "latch-b0-keeps-a2");
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_A0, OUTPUT);
  pinMode(PIN_A1, OUTPUT);
  pinMode(PIN_DA, OUTPUT);
  pinMode(PIN_DB, OUTPUT);
  pinMode(PIN_E, OUTPUT);
  pinMode(PIN_CL, OUTPUT);
  pinMode(PIN_Q0A, INPUT);
  pinMode(PIN_Q1A, INPUT);
  pinMode(PIN_Q2A, INPUT);
  pinMode(PIN_Q3A, INPUT);
  pinMode(PIN_Q0B, INPUT);
  pinMode(PIN_Q1B, INPUT);
  pinMode(PIN_Q2B, INPUT);
  pinMode(PIN_Q3B, INPUT);
  setAddress(0);
  setData(false, false);
  digitalWrite(PIN_E, HIGH);
  digitalWrite(PIN_CL, LOW);
  Serial.println("74256 ready. Send any character to start.");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  Serial.println(failed ? resultLine : "RESULT PASS");
}
