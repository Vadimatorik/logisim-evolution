/*
 * Self-check for a 74HC196 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * MR and PL are active low. MR clears asynchronously and overrides PL and both
 * clocks. While PL is low the outputs follow P0-P3. CP0 toggles Q0 on a falling
 * edge. CP1 advances Q1 (LSB), Q2 and Q3 through 0, 1, 2, 3, 4 and back to 0.
 * Do not jumper Q0 to CP1 or Q3 to CP0: this sketch drives both clocks itself.
 * MR stays low, PL stays high and both clocks stay low until the check starts.
 */

const uint8_t PIN_MR = 2;
const uint8_t PIN_PL = 3;
const uint8_t PIN_CP0 = 4;
const uint8_t PIN_CP1 = 5;
const uint8_t PIN_P0 = 6;
const uint8_t PIN_P1 = 7;
const uint8_t PIN_P2 = 8;
const uint8_t PIN_P3 = 9;
const uint8_t PIN_Q0 = 10;
const uint8_t PIN_Q1 = 11;
const uint8_t PIN_Q2 = 12;
const uint8_t PIN_Q3 = A0;

const uint8_t BCD[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0};
const uint8_t BI_QUINARY[] = {2, 4, 6, 8, 1, 3, 5, 7, 9, 0};

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
  digitalWrite(PIN_P0, (code & 1) ? HIGH : LOW);
  digitalWrite(PIN_P1, (code & 2) ? HIGH : LOW);
  digitalWrite(PIN_P2, (code & 4) ? HIGH : LOW);
  digitalWrite(PIN_P3, (code & 8) ? HIGH : LOW);
}

uint8_t readCount() {
  uint8_t value = 0;
  if (digitalRead(PIN_Q0)) value |= 1;
  if (digitalRead(PIN_Q1)) value |= 2;
  if (digitalRead(PIN_Q2)) value |= 4;
  if (digitalRead(PIN_Q3)) value |= 8;
  return value;
}

void expectCount(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readCount();
  if (actual != expected) noteFailure(step, expected, actual);
}

void pulse(uint8_t clockPin) {
  digitalWrite(clockPin, LOW);
  settle();
  digitalWrite(clockPin, HIGH);
  settle();
  digitalWrite(clockPin, LOW);
  settle();
}

void rise(uint8_t clockPin) {
  digitalWrite(clockPin, HIGH);
  settle();
}

void clearChip() {
  digitalWrite(PIN_MR, LOW);
  settle();
  digitalWrite(PIN_CP0, LOW);
  digitalWrite(PIN_CP1, LOW);
  digitalWrite(PIN_PL, HIGH);
  setData(0);
  settle();
  digitalWrite(PIN_MR, HIGH);
  settle();
}

void load(uint8_t code) {
  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_PL, LOW);
  setData(code);
  settle();
}

void cascade(uint8_t clockPin, uint8_t sourceMask, uint8_t destPin) {
  digitalWrite(destPin, (readCount() & sourceMask) ? HIGH : LOW);
  settle();
  digitalWrite(clockPin, HIGH);
  settle();
  digitalWrite(destPin, (readCount() & sourceMask) ? HIGH : LOW);
  settle();
  digitalWrite(clockPin, LOW);
  settle();
  digitalWrite(destPin, (readCount() & sourceMask) ? HIGH : LOW);
  settle();
}

void checkResetOverridesLoad() {
  digitalWrite(PIN_MR, LOW);
  digitalWrite(PIN_PL, LOW);
  setData(0x0F);
  pulse(PIN_CP0);
  pulse(PIN_CP1);
  expectCount(0, "reset-holds");
  digitalWrite(PIN_PL, HIGH);
  setData(0);
  digitalWrite(PIN_MR, HIGH);
  expectCount(0, "reset-release");
}

void checkTransparentLoad() {
  clearChip();
  for (uint8_t code = 0; code < 16; code++) {
    load(code);
    char step[12];
    snprintf(step, sizeof(step), "load-%02X", code);
    expectCount(code, step);
  }
  pulse(PIN_CP0);
  pulse(PIN_CP1);
  expectCount(0x0F, "load-ignores-clock");
  digitalWrite(PIN_PL, HIGH);
  setData(0);
  expectCount(0x0F, "hold-after-load");
  pulse(PIN_CP0);
  expectCount(0x0E, "count-after-load");
}

void checkIndependentSections() {
  clearChip();
  rise(PIN_CP0);
  rise(PIN_CP1);
  expectCount(0, "rising-edge");
  pulse(PIN_CP0);
  expectCount(1, "cp0-1");
  pulse(PIN_CP0);
  expectCount(0, "cp0-0");
  const uint8_t divideByFive[] = {2, 4, 6, 8, 0};
  for (uint8_t index = 0; index < 5; index++) {
    pulse(PIN_CP1);
    char step[12];
    snprintf(step, sizeof(step), "cp1-%u", index);
    expectCount(divideByFive[index], step);
  }
}

void checkIllegalCodes() {
  for (uint8_t section = 5; section <= 7; section++) {
    for (uint8_t q0 = 0; q0 <= 1; q0++) {
      const uint8_t code = (section << 1) | q0;
      load(code);
      digitalWrite(PIN_PL, HIGH);
      settle();
      pulse(PIN_CP1);
      char step[16];
      snprintf(step, sizeof(step), "illegal-%02X", code);
      expectCount(q0, step);
    }
  }
}

void checkBcd() {
  clearChip();
  for (uint8_t index = 0; index < 10; index++) {
    cascade(PIN_CP0, 1, PIN_CP1);
    char step[12];
    snprintf(step, sizeof(step), "bcd-%u", index);
    expectCount(BCD[index], step);
  }
}

void checkBiQuinary() {
  clearChip();
  for (uint8_t index = 0; index < 10; index++) {
    cascade(PIN_CP1, 8, PIN_CP0);
    char step[12];
    snprintf(step, sizeof(step), "biqu-%u", index);
    expectCount(BI_QUINARY[index], step);
  }
}

void runChecks() {
  checkResetOverridesLoad();
  checkTransparentLoad();
  checkIndependentSections();
  checkIllegalCodes();
  checkBcd();
  checkBiQuinary();
  if (!failed) {
    Serial.println("RESULT PASS");
  } else {
    Serial.println(resultLine);
  }
}

void setup() {
  const uint8_t outputs[] = {
      PIN_MR, PIN_PL, PIN_CP0, PIN_CP1, PIN_P0, PIN_P1, PIN_P2, PIN_P3};
  for (uint8_t index = 0; index < sizeof(outputs); index++) {
    pinMode(outputs[index], OUTPUT);
  }
  pinMode(PIN_Q0, INPUT);
  pinMode(PIN_Q1, INPUT);
  pinMode(PIN_Q2, INPUT);
  pinMode(PIN_Q3, INPUT);
  digitalWrite(PIN_MR, LOW);
  digitalWrite(PIN_PL, HIGH);
  digitalWrite(PIN_CP0, LOW);
  digitalWrite(PIN_CP1, LOW);
  setData(0);

  Serial.begin(115200);
  Serial.println("74HC196 bench. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  failed = false;
  runChecks();
  while (Serial.available() > 0) Serial.read();
}
