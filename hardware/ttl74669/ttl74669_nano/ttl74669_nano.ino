/*
 * Self-check for a 74HC669 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * LOAD, ENP, ENT and RCO are active low. A high U/D counts up. CK counts or
 * loads on the rising edge. A low LOAD loads A-D even when both count enables
 * are low. RCO is low only when ENT is low and the code is terminal for the
 * direction: 15 while counting up, 0 while counting down. Outputs are
 * push-pull. CK stays low and LOAD, ENP and ENT stay high until the check
 * starts. D13 follows QB, so the board LED tracks that bit.
 */

const uint8_t PIN_UD = 2;
const uint8_t PIN_CK = 3;
const uint8_t PIN_A = 4;
const uint8_t PIN_B = 5;
const uint8_t PIN_C = 6;
const uint8_t PIN_D = 7;
const uint8_t PIN_ENP = 8;
const uint8_t PIN_LOAD = 9;
const uint8_t PIN_ENT = 10;
const uint8_t PIN_QD = 11;
const uint8_t PIN_QC = 12;
const uint8_t PIN_QB = 13;
const uint8_t PIN_QA = A0;
const uint8_t PIN_RCO = A1;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, uint8_t expectedQ, bool expectedRcoLow, uint8_t actualQ,
                 bool actualRcoLow) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine),
           "RESULT FAIL %s expected Q=%02X RCO=%c got Q=%02X RCO=%c", step, expectedQ,
           expectedRcoLow ? 'L' : 'H', actualQ, actualRcoLow ? 'L' : 'H');
}

void settle() { delay(1); }

void setData(uint8_t value) {
  digitalWrite(PIN_A, (value & 1) ? HIGH : LOW);
  digitalWrite(PIN_B, (value & 2) ? HIGH : LOW);
  digitalWrite(PIN_C, (value & 4) ? HIGH : LOW);
  digitalWrite(PIN_D, (value & 8) ? HIGH : LOW);
}

uint8_t readQ() {
  uint8_t value = 0;
  if (digitalRead(PIN_QA)) value |= 1;
  if (digitalRead(PIN_QB)) value |= 2;
  if (digitalRead(PIN_QC)) value |= 4;
  if (digitalRead(PIN_QD)) value |= 8;
  return value;
}

void expectState(uint8_t expectedQ, bool expectedRcoLow, const char* step) {
  settle();
  const uint8_t actualQ = readQ();
  const bool actualRcoLow = digitalRead(PIN_RCO) == LOW;
  if (actualQ != expectedQ || actualRcoLow != expectedRcoLow) {
    noteFailure(step, expectedQ, expectedRcoLow, actualQ, actualRcoLow);
  }
}

void pulse() {
  digitalWrite(PIN_CK, LOW);
  settle();
  digitalWrite(PIN_CK, HIGH);
  settle();
}

void load(uint8_t value) {
  setData(value);
  digitalWrite(PIN_UD, HIGH);
  digitalWrite(PIN_ENP, HIGH);
  digitalWrite(PIN_ENT, HIGH);
  digitalWrite(PIN_LOAD, LOW);
  pulse();
  digitalWrite(PIN_LOAD, HIGH);
}

void armCount(bool up) {
  digitalWrite(PIN_UD, up ? HIGH : LOW);
  digitalWrite(PIN_ENP, LOW);
  digitalWrite(PIN_ENT, LOW);
  digitalWrite(PIN_LOAD, HIGH);
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
  digitalWrite(PIN_ENP, LOW);
  digitalWrite(PIN_ENT, LOW);
  digitalWrite(PIN_LOAD, LOW);
  pulse();
  digitalWrite(PIN_LOAD, HIGH);
  expectState(5, false, "load-over-count");

  load(0);
  armCount(true);
  expectState(0, false, "up-0");
  for (uint8_t count = 1; count <= 15; count++) {
    pulse();
    expectState(count, count == 15, count == 15 ? "up-15" : "up");
  }
  pulse();
  expectState(0, false, "up-wrap");

  load(15);
  digitalWrite(PIN_UD, HIGH);
  digitalWrite(PIN_ENP, HIGH);
  digitalWrite(PIN_ENT, LOW);
  digitalWrite(PIN_LOAD, HIGH);
  settle();
  expectState(15, true, "enp-hold-rco");
  pulse();
  expectState(15, true, "enp-hold");

  digitalWrite(PIN_ENT, HIGH);
  settle();
  expectState(15, false, "ent-high-rco");
  pulse();
  expectState(15, false, "ent-hold");

  load(0);
  armCount(true);
  expectState(0, false, "dir-up-at-0");
  digitalWrite(PIN_UD, LOW);
  settle();
  expectState(0, true, "dir-down-at-0");

  for (uint8_t count = 15;; count--) {
    pulse();
    expectState(count, count == 0, count == 0 ? "down-0" : "down");
    if (count == 0) break;
  }

  digitalWrite(PIN_UD, HIGH);
  settle();
  expectState(0, false, "before-fall");
  digitalWrite(PIN_CK, LOW);
  settle();
  expectState(0, false, "falling-edge");
  pulse();
  expectState(1, false, "rising-after-fall");
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_UD, OUTPUT);
  pinMode(PIN_CK, OUTPUT);
  pinMode(PIN_A, OUTPUT);
  pinMode(PIN_B, OUTPUT);
  pinMode(PIN_C, OUTPUT);
  pinMode(PIN_D, OUTPUT);
  pinMode(PIN_ENP, OUTPUT);
  pinMode(PIN_LOAD, OUTPUT);
  pinMode(PIN_ENT, OUTPUT);
  pinMode(PIN_QA, INPUT);
  pinMode(PIN_QB, INPUT);
  pinMode(PIN_QC, INPUT);
  pinMode(PIN_QD, INPUT);
  pinMode(PIN_RCO, INPUT);
  digitalWrite(PIN_CK, LOW);
  digitalWrite(PIN_UD, HIGH);
  digitalWrite(PIN_LOAD, HIGH);
  digitalWrite(PIN_ENP, HIGH);
  digitalWrite(PIN_ENT, HIGH);
  setData(0);
  Serial.println("74HC669 ready. Send any character to start.");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  Serial.println(failed ? resultLine : "RESULT PASS");
}
