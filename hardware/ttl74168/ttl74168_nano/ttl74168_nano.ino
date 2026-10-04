/*
 * Self-check for a 74HC168 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * CEP, CET, PE and TC are active low. A high U/D counts up. CP counts or loads
 * on the rising edge. A low PE loads D0-D3 even when both count enables are
 * low. TC is low only when CET is low and the code is terminal for the
 * direction: 9, 11, 13 or 15 while counting up, and 0 while counting down.
 * Outputs are push-pull. CP stays low and PE, CEP and CET stay high until the
 * check starts. D13 follows Q1, so the board LED tracks that bit.
 *
 * The expected next codes match Ttl74168. Counting up, illegal codes follow
 * 10→11→6, 12→13→4 and 14→15→2. Counting down they are 10→1, 11→10, 12→3,
 * 13→12, 14→4 and 15→14.
 */

const uint8_t PIN_UD = 2;
const uint8_t PIN_CP = 3;
const uint8_t PIN_D0 = 4;
const uint8_t PIN_D1 = 5;
const uint8_t PIN_D2 = 6;
const uint8_t PIN_D3 = 7;
const uint8_t PIN_CEP = 8;
const uint8_t PIN_PE = 9;
const uint8_t PIN_CET = 10;
const uint8_t PIN_Q3 = 11;
const uint8_t PIN_Q2 = 12;
const uint8_t PIN_Q1 = 13;
const uint8_t PIN_Q0 = A0;
const uint8_t PIN_TC = A1;

const uint8_t NEXT_UP[16] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 11, 6, 13, 4, 15, 2};
const uint8_t NEXT_DOWN[16] = {9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 1, 10, 3, 12, 4, 14};

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, uint8_t expectedQ, bool expectedTcLow, uint8_t actualQ,
                 bool actualTcLow) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine),
           "RESULT FAIL %s expected Q=%02X TC=%c got Q=%02X TC=%c", step, expectedQ,
           expectedTcLow ? 'L' : 'H', actualQ, actualTcLow ? 'L' : 'H');
}

void settle() { delay(1); }

void setData(uint8_t value) {
  digitalWrite(PIN_D0, (value & 1) ? HIGH : LOW);
  digitalWrite(PIN_D1, (value & 2) ? HIGH : LOW);
  digitalWrite(PIN_D2, (value & 4) ? HIGH : LOW);
  digitalWrite(PIN_D3, (value & 8) ? HIGH : LOW);
}

uint8_t readQ() {
  uint8_t value = 0;
  if (digitalRead(PIN_Q0)) value |= 1;
  if (digitalRead(PIN_Q1)) value |= 2;
  if (digitalRead(PIN_Q2)) value |= 4;
  if (digitalRead(PIN_Q3)) value |= 8;
  return value;
}

bool upTerminal(uint8_t code) { return (code & 0x9) == 0x9; }

void expectState(uint8_t expectedQ, bool expectedTcLow, const char* step) {
  settle();
  const uint8_t actualQ = readQ();
  const bool actualTcLow = digitalRead(PIN_TC) == LOW;
  if (actualQ != expectedQ || actualTcLow != expectedTcLow) {
    noteFailure(step, expectedQ, expectedTcLow, actualQ, actualTcLow);
  }
}

void pulse() {
  digitalWrite(PIN_CP, LOW);
  settle();
  digitalWrite(PIN_CP, HIGH);
  settle();
}

void load(uint8_t value) {
  setData(value);
  digitalWrite(PIN_UD, HIGH);
  digitalWrite(PIN_CEP, HIGH);
  digitalWrite(PIN_CET, HIGH);
  digitalWrite(PIN_PE, LOW);
  pulse();
  digitalWrite(PIN_PE, HIGH);
}

void armCount(bool up) {
  digitalWrite(PIN_UD, up ? HIGH : LOW);
  digitalWrite(PIN_CEP, LOW);
  digitalWrite(PIN_CET, LOW);
  digitalWrite(PIN_PE, HIGH);
  settle();
}

void runChecks() {
  const uint8_t codes[] = {0, 1, 7, 10, 15};
  for (uint8_t index = 0; index < 5; index++) {
    load(codes[index]);
    expectState(codes[index], false, "load");
  }

  setData(5);
  digitalWrite(PIN_UD, HIGH);
  digitalWrite(PIN_CEP, LOW);
  digitalWrite(PIN_CET, LOW);
  digitalWrite(PIN_PE, LOW);
  pulse();
  digitalWrite(PIN_PE, HIGH);
  expectState(5, false, "load-over-count");

  load(0);
  armCount(true);
  expectState(0, false, "up-0");
  for (uint8_t count = 1; count <= 9; count++) {
    pulse();
    expectState(count, count == 9, count == 9 ? "up-9" : "up");
  }
  pulse();
  expectState(0, false, "up-wrap");

  armCount(false);
  expectState(0, true, "dir-down-at-0");
  for (uint8_t count = 9;; count--) {
    pulse();
    expectState(count, count == 0, count == 0 ? "down-0" : "down");
    if (count == 0) break;
  }

  load(9);
  digitalWrite(PIN_UD, HIGH);
  digitalWrite(PIN_CEP, HIGH);
  digitalWrite(PIN_CET, LOW);
  digitalWrite(PIN_PE, HIGH);
  settle();
  expectState(9, true, "cep-hold-tc");
  pulse();
  expectState(9, true, "cep-hold");

  digitalWrite(PIN_CET, HIGH);
  settle();
  expectState(9, false, "cet-high-tc");
  pulse();
  expectState(9, false, "cet-hold");

  load(11);
  armCount(true);
  expectState(11, true, "tc-11");
  load(13);
  armCount(true);
  expectState(13, true, "tc-13");
  load(15);
  armCount(true);
  expectState(15, true, "tc-15");

  for (uint8_t code = 0; code < 16; code++) {
    load(code);
    armCount(true);
    expectState(code, upTerminal(code), "before-up");
    pulse();
    const uint8_t upNext = NEXT_UP[code];
    expectState(upNext, upTerminal(upNext), "step-up");

    load(code);
    armCount(false);
    expectState(code, code == 0, "before-down");
    pulse();
    const uint8_t downNext = NEXT_DOWN[code];
    expectState(downNext, downNext == 0, "step-down");
  }

  load(0);
  armCount(true);
  expectState(0, false, "before-fall");
  digitalWrite(PIN_CP, LOW);
  settle();
  expectState(0, false, "falling-edge");
  pulse();
  expectState(1, false, "rising-after-fall");
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_UD, OUTPUT);
  pinMode(PIN_CP, OUTPUT);
  pinMode(PIN_D0, OUTPUT);
  pinMode(PIN_D1, OUTPUT);
  pinMode(PIN_D2, OUTPUT);
  pinMode(PIN_D3, OUTPUT);
  pinMode(PIN_CEP, OUTPUT);
  pinMode(PIN_PE, OUTPUT);
  pinMode(PIN_CET, OUTPUT);
  pinMode(PIN_Q0, INPUT);
  pinMode(PIN_Q1, INPUT);
  pinMode(PIN_Q2, INPUT);
  pinMode(PIN_Q3, INPUT);
  pinMode(PIN_TC, INPUT);
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_UD, HIGH);
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_CEP, HIGH);
  digitalWrite(PIN_CET, HIGH);
  setData(0);
  Serial.println("74HC168 ready. Send any character to start.");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  Serial.println(failed ? resultLine : "RESULT PASS");
}
