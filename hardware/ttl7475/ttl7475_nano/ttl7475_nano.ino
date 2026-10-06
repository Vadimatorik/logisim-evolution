/*
 * Self-check for a 74HC75 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * LE12 and LE34 are active high. A high enable makes that pair follow its data
 * inputs, and the complementary outputs are the inverse. A low enable holds the
 * pair. The two pairs are independent. Outputs are push-pull, so the Q pins are
 * read directly. Both enables stay low until the check starts.
 *
 * This is the 74HC75 pinout: VCC is pin 5 and GND is pin 12. Do not use a
 * 7475 or 74LS75 on this wiring.
 */

const uint8_t PIN_Q1N = 2;
const uint8_t PIN_D1 = 3;
const uint8_t PIN_D2 = 4;
const uint8_t PIN_LE34 = 5;
const uint8_t PIN_D3 = 6;
const uint8_t PIN_D4 = 7;
const uint8_t PIN_Q4N = 8;
const uint8_t PIN_Q4 = 9;
const uint8_t PIN_Q3 = 10;
const uint8_t PIN_Q3N = 11;
const uint8_t PIN_LE12 = 12;
const uint8_t PIN_Q2N = 13;
const uint8_t PIN_Q2 = A0;
const uint8_t PIN_Q1 = A1;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, uint8_t expectedQ, uint8_t actualQ, uint8_t actualQn) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s expected Q %02X Qn %02X got Q %02X Qn %02X",
      step,
      expectedQ,
      static_cast<uint8_t>(~expectedQ & 0x0F),
      actualQ,
      actualQn);
}

void settle() { delay(1); }

void setData(uint8_t pattern) {
  digitalWrite(PIN_D1, (pattern & 1) ? HIGH : LOW);
  digitalWrite(PIN_D2, (pattern & 2) ? HIGH : LOW);
  digitalWrite(PIN_D3, (pattern & 4) ? HIGH : LOW);
  digitalWrite(PIN_D4, (pattern & 8) ? HIGH : LOW);
}

void setEnable(bool le12, bool le34) {
  digitalWrite(PIN_LE12, le12 ? HIGH : LOW);
  digitalWrite(PIN_LE34, le34 ? HIGH : LOW);
}

uint8_t readQ() {
  uint8_t value = 0;
  if (digitalRead(PIN_Q1)) value |= 1;
  if (digitalRead(PIN_Q2)) value |= 2;
  if (digitalRead(PIN_Q3)) value |= 4;
  if (digitalRead(PIN_Q4)) value |= 8;
  return value;
}

uint8_t readQn() {
  uint8_t value = 0;
  if (digitalRead(PIN_Q1N)) value |= 1;
  if (digitalRead(PIN_Q2N)) value |= 2;
  if (digitalRead(PIN_Q3N)) value |= 4;
  if (digitalRead(PIN_Q4N)) value |= 8;
  return value;
}

void expect(uint8_t expected, const char* step) {
  settle();
  const uint8_t actualQ = readQ();
  const uint8_t actualQn = readQn();
  const uint8_t expectedQn = static_cast<uint8_t>(~expected & 0x0F);
  if (actualQ != expected || actualQn != expectedQn) {
    noteFailure(step, expected, actualQ, actualQn);
  }
}

void runChecks() {
  setEnable(true, true);
  for (uint8_t pattern = 0; pattern < 16; pattern++) {
    setData(pattern);
    expect(pattern, "transparent");
  }

  setData(0x0A);
  expect(0x0A, "transparent-before-hold");
  setEnable(false, false);
  setData(0x05);
  expect(0x0A, "hold");

  setEnable(true, true);
  setData(0x0F);
  expect(0x0F, "all-ones");
  setEnable(false, true);
  setData(0x00);
  expect(0x03, "pair34-follows");
  setEnable(true, false);
  setData(0x0F);
  expect(0x03, "pair12-follows");
  setData(0x00);
  expect(0x00, "pair12-cleared");
  setEnable(false, true);
  setData(0x0F);
  expect(0x0C, "pair34-set");

  setEnable(true, true);
  setData(0x05);
  expect(0x05, "capture-load");
  setEnable(false, false);
  setData(0x0A);
  expect(0x05, "capture-hold");
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_D1, OUTPUT);
  pinMode(PIN_D2, OUTPUT);
  pinMode(PIN_D3, OUTPUT);
  pinMode(PIN_D4, OUTPUT);
  pinMode(PIN_LE12, OUTPUT);
  pinMode(PIN_LE34, OUTPUT);
  pinMode(PIN_Q1, INPUT);
  pinMode(PIN_Q1N, INPUT);
  pinMode(PIN_Q2, INPUT);
  pinMode(PIN_Q2N, INPUT);
  pinMode(PIN_Q3, INPUT);
  pinMode(PIN_Q3N, INPUT);
  pinMode(PIN_Q4, INPUT);
  pinMode(PIN_Q4N, INPUT);
  setData(0);
  setEnable(false, false);
  Serial.println("74HC75 ready. Send any character to start.");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  setEnable(false, false);
  Serial.println(failed ? resultLine : "RESULT PASS");
}
