/*
 * Self-check for a 74HC375 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * G12 and G34 are active high. A high enable makes that pair follow its data
 * inputs, and each nQ pin is the complement of its Q pin. A low enable holds
 * the pair. The two pairs are independent. Outputs are push-pull, so Q and nQ
 * are read directly. Both enables stay low until the check starts.
 *
 * This is the 74HC375 pinout: VCC is pin 16 and GND is pin 8. Do not use a
 * 7475 or 74HC75 on this wiring.
 */

const uint8_t PIN_D1 = 2;
const uint8_t PIN_D2 = 3;
const uint8_t PIN_D3 = 4;
const uint8_t PIN_D4 = 5;
const uint8_t PIN_G12 = 6;
const uint8_t PIN_G34 = 7;
const uint8_t PIN_Q1 = 8;
const uint8_t PIN_NQ1 = 9;
const uint8_t PIN_Q2 = 10;
const uint8_t PIN_NQ2 = 11;
const uint8_t PIN_Q3 = 12;
const uint8_t PIN_NQ3 = A0;
const uint8_t PIN_Q4 = A1;
const uint8_t PIN_NQ4 = A2;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, const char* which, uint8_t expected, uint8_t actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s %s expected %02X got %02X",
      step,
      which,
      expected,
      actual);
}

void settle() { delay(1); }

void setData(uint8_t pattern) {
  digitalWrite(PIN_D1, (pattern & 1) ? HIGH : LOW);
  digitalWrite(PIN_D2, (pattern & 2) ? HIGH : LOW);
  digitalWrite(PIN_D3, (pattern & 4) ? HIGH : LOW);
  digitalWrite(PIN_D4, (pattern & 8) ? HIGH : LOW);
}

void setEnable(bool g12, bool g34) {
  digitalWrite(PIN_G12, g12 ? HIGH : LOW);
  digitalWrite(PIN_G34, g34 ? HIGH : LOW);
}

uint8_t readNibble(uint8_t bit0, uint8_t bit1, uint8_t bit2, uint8_t bit3) {
  uint8_t value = 0;
  if (digitalRead(bit0)) value |= 1;
  if (digitalRead(bit1)) value |= 2;
  if (digitalRead(bit2)) value |= 4;
  if (digitalRead(bit3)) value |= 8;
  return value;
}

uint8_t readQ() { return readNibble(PIN_Q1, PIN_Q2, PIN_Q3, PIN_Q4); }

uint8_t readNQ() { return readNibble(PIN_NQ1, PIN_NQ2, PIN_NQ3, PIN_NQ4); }

void expect(uint8_t expectedQ, const char* step) {
  settle();
  const uint8_t actualQ = readQ();
  const uint8_t actualNQ = readNQ();
  const uint8_t expectedNQ = (~expectedQ) & 0x0F;
  if (actualQ != expectedQ) noteFailure(step, "Q", expectedQ, actualQ);
  else if (actualNQ != expectedNQ) noteFailure(step, "nQ", expectedNQ, actualNQ);
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
  pinMode(PIN_G12, OUTPUT);
  pinMode(PIN_G34, OUTPUT);
  pinMode(PIN_Q1, INPUT);
  pinMode(PIN_NQ1, INPUT);
  pinMode(PIN_Q2, INPUT);
  pinMode(PIN_NQ2, INPUT);
  pinMode(PIN_Q3, INPUT);
  pinMode(PIN_NQ3, INPUT);
  pinMode(PIN_Q4, INPUT);
  pinMode(PIN_NQ4, INPUT);
  setData(0);
  setEnable(false, false);
  Serial.println("74HC375 ready. Send any character to start.");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  setEnable(false, false);
  Serial.println(failed ? resultLine : "RESULT PASS");
}
