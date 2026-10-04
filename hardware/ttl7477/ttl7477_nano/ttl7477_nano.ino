/*
 * Self-check for a 74HC77 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * LE12 and LE34 are active high. A high enable makes that pair follow its data
 * inputs. A low enable holds the pair. The two pairs are independent. There
 * are no complementary outputs. Outputs are push-pull, so the Q pins are read
 * directly. Both enables stay low until the check starts.
 *
 * This is the 74HC77 pinout: VCC is pin 4 and GND is pin 11. Pins 7 and 10
 * stay open. Do not use a 7475 or 74HC75 on this wiring.
 */

const uint8_t PIN_D1 = 2;
const uint8_t PIN_D2 = 3;
const uint8_t PIN_LE34 = 4;
const uint8_t PIN_D3 = 5;
const uint8_t PIN_D4 = 6;
const uint8_t PIN_LE12 = 7;
const uint8_t PIN_Q4 = 8;
const uint8_t PIN_Q3 = 9;
const uint8_t PIN_Q2 = 10;
const uint8_t PIN_Q1 = 11;

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

void expect(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readQ();
  if (actual != expected) noteFailure(step, expected, actual);
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
  pinMode(PIN_Q2, INPUT);
  pinMode(PIN_Q3, INPUT);
  pinMode(PIN_Q4, INPUT);
  setData(0);
  setEnable(false, false);
  Serial.println("74HC77 ready. Send any character to start.");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  setEnable(false, false);
  Serial.println(failed ? resultLine : "RESULT PASS");
}
