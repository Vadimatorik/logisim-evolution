/*
 * Self-check for a 74HC176 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * CLR low clears every output asynchronously and overrides LOAD and both clocks.
 * LOAD low, with CLR high, copies A..D to QA..QD without a clock. Counting is on
 * the falling edge: CLK1 toggles QA, CLK2 advances QB QC QD through 0..4.
 * Do not jumper QA to CLK2 or QD to CLK1. The sketch drives those links itself
 * when it checks the BCD and bi-quinary cycles.
 * Clocks stay high, CLR stays low and LOAD stays high until the check starts.
 */

const uint8_t PIN_LOAD = 2;
const uint8_t PIN_CLR = 3;
const uint8_t PIN_A = 4;
const uint8_t PIN_B = 5;
const uint8_t PIN_C = 6;
const uint8_t PIN_D = 7;
const uint8_t PIN_CLK1 = 8;
const uint8_t PIN_CLK2 = 9;
const uint8_t PIN_QA = 10;
const uint8_t PIN_QB = 11;
const uint8_t PIN_QC = 12;
const uint8_t PIN_QD = 13;

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

void setData(uint8_t code) {
  digitalWrite(PIN_A, (code & 1) ? HIGH : LOW);
  digitalWrite(PIN_B, (code & 2) ? HIGH : LOW);
  digitalWrite(PIN_C, (code & 4) ? HIGH : LOW);
  digitalWrite(PIN_D, (code & 8) ? HIGH : LOW);
}

uint8_t readNibble() {
  uint8_t value = 0;
  if (digitalRead(PIN_QA)) value |= 1;
  if (digitalRead(PIN_QB)) value |= 2;
  if (digitalRead(PIN_QC)) value |= 4;
  if (digitalRead(PIN_QD)) value |= 8;
  return value;
}

void expectNibble(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readNibble();
  if (actual != expected) noteFailure(step, expected, actual);
}

void rearmClocks() {
  digitalWrite(PIN_CLK1, HIGH);
  digitalWrite(PIN_CLK2, HIGH);
  settle();
}

void fall(uint8_t pin) {
  digitalWrite(pin, HIGH);
  settle();
  digitalWrite(pin, LOW);
  settle();
}

void rise(uint8_t pin) {
  digitalWrite(pin, LOW);
  settle();
  digitalWrite(pin, HIGH);
  settle();
}

void loadCode(uint8_t code, bool clk1High, bool clk2High) {
  digitalWrite(PIN_CLR, HIGH);
  digitalWrite(PIN_LOAD, LOW);
  digitalWrite(PIN_CLK1, clk1High ? HIGH : LOW);
  digitalWrite(PIN_CLK2, clk2High ? HIGH : LOW);
  setData(code);
  settle();
  expectNibble(code, "load");
  digitalWrite(PIN_LOAD, HIGH);
  settle();
  expectNibble(code, "hold after load");
}

void checkClearOverridesLoad() {
  loadCode(0xA, true, true);
  digitalWrite(PIN_LOAD, LOW);
  setData(0xF);
  digitalWrite(PIN_CLR, LOW);
  settle();
  expectNibble(0, "clear overrides load");
  digitalWrite(PIN_CLK1, LOW);
  digitalWrite(PIN_CLK2, LOW);
  settle();
  expectNibble(0, "clear overrides clocks");
  digitalWrite(PIN_CLR, HIGH);
  digitalWrite(PIN_LOAD, HIGH);
  rearmClocks();
  expectNibble(0, "hold after clear");
}

void checkTransparentLoad() {
  for (uint8_t code = 0; code < 16; code++) {
    loadCode(code, true, true);
  }
  setData(0);
  settle();
  expectNibble(15, "data ignored while load high");
}

void checkDivideByTwo() {
  loadCode(0x2, true, true);
  fall(PIN_CLK1);
  expectNibble(0x3, "clk1 toggles qa");
  rise(PIN_CLK1);
  expectNibble(0x3, "clk1 rise ignored");
  fall(PIN_CLK1);
  expectNibble(0x2, "clk1 toggles qa back");
  rise(PIN_CLK1);
}

void checkDivideByFive() {
  loadCode(0, true, true);
  const uint8_t cycle[] = {0x2, 0x4, 0x6, 0x8, 0x0};
  for (uint8_t index = 0; index < 5; index++) {
    fall(PIN_CLK2);
    expectNibble(cycle[index], "divide by five");
    rise(PIN_CLK2);
    expectNibble(cycle[index], "clk2 rise ignored");
  }
  const uint8_t illegal[] = {0xA, 0xC, 0xE};
  for (uint8_t index = 0; index < 3; index++) {
    loadCode(illegal[index], true, true);
    fall(PIN_CLK2);
    expectNibble(0, "illegal divide by five");
    rise(PIN_CLK2);
  }
}

void checkBothEdges() {
  loadCode(0, true, true);
  digitalWrite(PIN_CLK1, LOW);
  digitalWrite(PIN_CLK2, LOW);
  settle();
  expectNibble(0x3, "both clocks fall");
  rearmClocks();
}

void bcdStep() {
  digitalWrite(PIN_CLK2, digitalRead(PIN_QA) ? HIGH : LOW);
  settle();
  digitalWrite(PIN_CLK1, LOW);
  settle();
  digitalWrite(PIN_CLK2, digitalRead(PIN_QA) ? HIGH : LOW);
  settle();
  digitalWrite(PIN_CLK1, HIGH);
  settle();
}

void checkBcd() {
  loadCode(0, true, false);
  for (uint8_t code = 0; code < 10; code++) {
    expectNibble(code, "bcd");
    bcdStep();
  }
  expectNibble(0, "bcd wrap");
}

void biQuinaryStep() {
  digitalWrite(PIN_CLK1, digitalRead(PIN_QD) ? HIGH : LOW);
  settle();
  digitalWrite(PIN_CLK2, LOW);
  settle();
  digitalWrite(PIN_CLK1, digitalRead(PIN_QD) ? HIGH : LOW);
  settle();
  digitalWrite(PIN_CLK2, HIGH);
  settle();
}

void checkBiQuinary() {
  loadCode(0, false, true);
  const uint8_t cycle[] = {0x2, 0x4, 0x6, 0x8, 0x1, 0x3, 0x5, 0x7, 0x9, 0x0};
  for (uint8_t index = 0; index < 10; index++) {
    biQuinaryStep();
    expectNibble(cycle[index], "bi-quinary");
  }
}

void setup() {
  pinMode(PIN_LOAD, OUTPUT);
  pinMode(PIN_CLR, OUTPUT);
  pinMode(PIN_A, OUTPUT);
  pinMode(PIN_B, OUTPUT);
  pinMode(PIN_C, OUTPUT);
  pinMode(PIN_D, OUTPUT);
  pinMode(PIN_CLK1, OUTPUT);
  pinMode(PIN_CLK2, OUTPUT);
  pinMode(PIN_QA, INPUT);
  pinMode(PIN_QB, INPUT);
  pinMode(PIN_QC, INPUT);
  pinMode(PIN_QD, INPUT);

  digitalWrite(PIN_CLK1, HIGH);
  digitalWrite(PIN_CLK2, HIGH);
  digitalWrite(PIN_CLR, LOW);
  digitalWrite(PIN_LOAD, HIGH);
  setData(0);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  expectNibble(0, "initial clear");
  checkClearOverridesLoad();
  checkTransparentLoad();
  checkDivideByTwo();
  checkDivideByFive();
  checkBothEdges();
  checkBcd();
  checkBiQuinary();

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
