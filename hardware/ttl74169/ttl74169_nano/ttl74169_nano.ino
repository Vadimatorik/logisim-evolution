/*
 * Self-check for a 74HC169 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * PE, CEP and CET are active low. U/D high counts up. CP counts on the rising
 * edge. TC is active low and follows the current count without a clock.
 * Outputs are push-pull, so Q and TC are read directly. Until the check
 * starts, CP stays low and PE, CEP and CET stay high.
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

bool failed = false;
char resultLine[96];

void settle() { delay(1); }

void noteFailure(const char* step, uint8_t expectedQ, bool expectedTcHigh, uint8_t actualQ,
                 bool actualTcHigh) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s expected Q %X TC %c got Q %X TC %c",
           step, expectedQ, expectedTcHigh ? 'H' : 'L', actualQ, actualTcHigh ? 'H' : 'L');
}

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

void expect(uint8_t q, bool tcHigh, const char* step) {
  if (failed) return;
  settle();
  const uint8_t actualQ = readQ();
  const bool actualTcHigh = digitalRead(PIN_TC) == HIGH;
  if (actualQ != q || actualTcHigh != tcHigh) {
    noteFailure(step, q, tcHigh, actualQ, actualTcHigh);
  }
}

void rising() {
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
  rising();
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
  const uint8_t loads[] = {0, 1, 7, 15, 10};
  for (uint8_t index = 0; index < sizeof(loads); index++) {
    load(loads[index]);
    expect(loads[index], true, "load");
  }
  if (failed) return;

  load(0);
  armCount(true);
  expect(0, true, "up-start");
  for (uint8_t count = 1; count <= 15; count++) {
    rising();
    expect(count, count != 15, "up");
  }
  rising();
  expect(0, true, "up-wrap");
  if (failed) return;

  digitalWrite(PIN_UD, LOW);
  settle();
  expect(0, false, "down-at-zero");
  rising();
  expect(15, true, "down-from-zero");
  for (uint8_t count = 14; count > 0; count--) {
    rising();
    expect(count, true, "down");
  }
  rising();
  expect(0, false, "down-wrap");
  if (failed) return;

  load(15);
  armCount(true);
  expect(15, false, "cet-low-at-15");
  digitalWrite(PIN_CET, HIGH);
  settle();
  expect(15, true, "cet-high-at-15");
  rising();
  expect(15, true, "cet-hold");

  digitalWrite(PIN_CET, LOW);
  digitalWrite(PIN_CEP, HIGH);
  settle();
  expect(15, false, "cep-high-tc");
  rising();
  expect(15, false, "cep-hold");
  if (failed) return;

  setData(3);
  digitalWrite(PIN_CEP, LOW);
  digitalWrite(PIN_CET, LOW);
  digitalWrite(PIN_PE, LOW);
  digitalWrite(PIN_UD, HIGH);
  rising();
  expect(3, true, "load-priority");
  digitalWrite(PIN_PE, HIGH);

  digitalWrite(PIN_CP, LOW);
  settle();
  expect(3, true, "falling-edge");
  digitalWrite(PIN_CP, HIGH);
  settle();
  expect(4, true, "rising-after-fall");
  if (failed) return;

  load(0);
  armCount(true);
  digitalWrite(PIN_CEP, HIGH);
  settle();
  expect(0, true, "direction-up");
  digitalWrite(PIN_UD, LOW);
  settle();
  expect(0, false, "direction-down");
  digitalWrite(PIN_UD, HIGH);
  settle();
  expect(0, true, "direction-up-again");
}

void setup() {
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
  digitalWrite(PIN_PE, HIGH);
  digitalWrite(PIN_CEP, HIGH);
  digitalWrite(PIN_CET, HIGH);
  digitalWrite(PIN_UD, HIGH);
  setData(0);

  Serial.begin(115200);
  Serial.println("74HC169 bench. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  failed = false;
  Serial.println("RUN");
  runChecks();
  if (!failed) Serial.println("RESULT PASS");
  else Serial.println(resultLine);
}
