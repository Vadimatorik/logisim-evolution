/*
 * Self-check for a 74HC137 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * LE low makes the address transparent. A rising LE keeps that address.
 * Outputs stay high unless E1 is low and E2 is high. The selected Y is low.
 */

const uint8_t PIN_A0 = 2;
const uint8_t PIN_A1 = 3;
const uint8_t PIN_A2 = 4;
const uint8_t PIN_LE = 5;
const uint8_t PIN_E1 = 6;
const uint8_t PIN_E2 = 7;
const uint8_t PIN_Y0 = 8;
const uint8_t PIN_Y1 = 9;
const uint8_t PIN_Y2 = 10;
const uint8_t PIN_Y3 = 11;
const uint8_t PIN_Y4 = 12;
const uint8_t PIN_Y5 = 13;
const uint8_t PIN_Y6 = A0;
const uint8_t PIN_Y7 = A1;

const uint8_t OUTPUTS[] = {
    PIN_Y0, PIN_Y1, PIN_Y2, PIN_Y3, PIN_Y4, PIN_Y5, PIN_Y6, PIN_Y7};

bool failed = false;
char resultLine[96];

void noteFailure(const char* step) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s", step);
}

void setAddress(uint8_t code) {
  digitalWrite(PIN_A0, (code & 1) ? HIGH : LOW);
  digitalWrite(PIN_A1, (code & 2) ? HIGH : LOW);
  digitalWrite(PIN_A2, (code & 4) ? HIGH : LOW);
  delayMicroseconds(2);
}

void expectPattern(const char* step, int selected) {
  delay(1);
  char bits[9];
  bool ok = true;
  for (uint8_t index = 0; index < 8; index++) {
    const bool high = digitalRead(OUTPUTS[index]) == HIGH;
    bits[index] = high ? '1' : '0';
    const bool wantHigh = selected < 0 || selected != index;
    if (high != wantHigh) ok = false;
  }
  bits[8] = 0;
  Serial.print(step);
  Serial.print(" Y=");
  Serial.println(bits);
  if (!ok) noteFailure(step);
}

void runChecks() {
  char step[48];

  digitalWrite(PIN_LE, LOW);
  digitalWrite(PIN_E1, HIGH);
  digitalWrite(PIN_E2, HIGH);
  for (uint8_t code = 0; code < 8; code++) {
    setAddress(code);
    snprintf(step, sizeof(step), "E1 high addr %u", code);
    expectPattern(step, -1);
  }

  digitalWrite(PIN_E1, LOW);
  digitalWrite(PIN_E2, LOW);
  setAddress(3);
  expectPattern("E2 low", -1);

  digitalWrite(PIN_E2, HIGH);
  for (uint8_t code = 0; code < 8; code++) {
    setAddress(code);
    snprintf(step, sizeof(step), "transparent %u", code);
    expectPattern(step, code);
  }

  setAddress(3);
  digitalWrite(PIN_LE, HIGH);
  delayMicroseconds(2);
  expectPattern("latched 3", 3);

  setAddress(0);
  expectPattern("address ignored", 3);

  digitalWrite(PIN_E1, HIGH);
  expectPattern("E1 hides latch", -1);
  digitalWrite(PIN_E1, LOW);
  delayMicroseconds(2);
  expectPattern("E1 restore", 3);

  digitalWrite(PIN_E2, LOW);
  expectPattern("E2 hides latch", -1);
  digitalWrite(PIN_E2, HIGH);
  delayMicroseconds(2);
  expectPattern("E2 restore", 3);

  digitalWrite(PIN_LE, LOW);
  delayMicroseconds(2);
  expectPattern("transparent again", 0);

  setAddress(6);
  digitalWrite(PIN_LE, HIGH);
  delayMicroseconds(2);
  setAddress(1);
  expectPattern("latched 6", 6);

  digitalWrite(PIN_E1, HIGH);
  digitalWrite(PIN_E2, LOW);
}

void setup() {
  Serial.begin(115200);
  const uint8_t inputs[] = {PIN_A0, PIN_A1, PIN_A2, PIN_LE, PIN_E1, PIN_E2};
  for (uint8_t index = 0; index < sizeof(inputs); index++) pinMode(inputs[index], OUTPUT);
  for (uint8_t index = 0; index < sizeof(OUTPUTS); index++) pinMode(OUTPUTS[index], INPUT);
  digitalWrite(PIN_LE, HIGH);
  digitalWrite(PIN_E1, HIGH);
  digitalWrite(PIN_E2, LOW);
  setAddress(0);
  Serial.println("74HC137 bench. Send any character to start.");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  Serial.println(failed ? resultLine : "RESULT PASS");
}
