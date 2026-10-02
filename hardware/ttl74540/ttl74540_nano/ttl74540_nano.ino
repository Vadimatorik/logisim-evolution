/*
 * Self-check for a 74HC540 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Both nOE low: Yn is the complement of An. Either nOE high releases every output.
 * Hi-Z is detected by precharging the output pins. No external bias resistor is used.
 */

const uint8_t PIN_OE1 = 2;
const uint8_t PIN_A[8] = {3, 4, 5, 6, 7, 8, 9, 10};
// Y1..Y8. Y1 is the output opposite A1, which is package pin 18.
const uint8_t PIN_Y[8] = {A4, A3, A2, A1, A0, 13, 12, 11};
const uint8_t PIN_OE2 = A5;

bool failed = false;
char resultLine[120];

void noteFailure(const char* step, const char* detail) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s %s", step, detail);
}

void drive(uint8_t inputs, bool oe1High, bool oe2High) {
  for (uint8_t channel = 0; channel < 8; channel++) {
    digitalWrite(PIN_A[channel], (inputs & (1 << channel)) ? HIGH : LOW);
  }
  digitalWrite(PIN_OE1, oe1High ? HIGH : LOW);
  digitalWrite(PIN_OE2, oe2High ? HIGH : LOW);
}

uint8_t readOutputs() {
  uint8_t value = 0;
  for (uint8_t channel = 0; channel < 8; channel++) {
    pinMode(PIN_Y[channel], INPUT);
    if (digitalRead(PIN_Y[channel]) == HIGH) value |= (1 << channel);
  }
  return value;
}

void expectDriven(const char* step, uint8_t expected) {
  delay(1);
  const uint8_t actual = readOutputs();
  Serial.print(step);
  Serial.print(" expected=0x");
  Serial.print(expected, HEX);
  Serial.print(" actual=0x");
  Serial.print(actual, HEX);
  const bool pass = expected == actual;
  Serial.println(pass ? " PASS" : " FAIL");
  if (!pass) {
    char detail[40];
    snprintf(detail, sizeof(detail), "expected=0x%02X actual=0x%02X", expected, actual);
    noteFailure(step, detail);
  }
}

bool pinIsReleased(uint8_t pin) {
  pinMode(pin, OUTPUT);
  digitalWrite(pin, LOW);
  delayMicroseconds(20);
  pinMode(pin, INPUT);
  delayMicroseconds(5);
  const bool heldLow = digitalRead(pin) == LOW;

  pinMode(pin, OUTPUT);
  digitalWrite(pin, HIGH);
  delayMicroseconds(20);
  pinMode(pin, INPUT);
  delayMicroseconds(5);
  const bool heldHigh = digitalRead(pin) == HIGH;
  return heldLow && heldHigh;
}

void expectReleased(const char* step) {
  delay(1);
  int stuck = -1;
  for (uint8_t channel = 0; channel < 8; channel++) {
    if (!pinIsReleased(PIN_Y[channel])) {
      stuck = channel;
      break;
    }
  }
  Serial.print(step);
  Serial.println(stuck < 0 ? " released PASS" : " released FAIL");
  if (stuck >= 0) {
    char detail[24];
    snprintf(detail, sizeof(detail), "Y%d still driven", stuck + 1);
    noteFailure(step, detail);
  }
}

void run() {
  drive(0x00, false, false);
  expectDriven("all inputs low", 0xFF);

  drive(0xFF, false, false);
  expectDriven("all inputs high", 0x00);

  for (uint8_t channel = 0; channel < 8; channel++) {
    char step[24];
    snprintf(step, sizeof(step), "only A%d high", channel + 1);
    drive((uint8_t) (1 << channel), false, false);
    expectDriven(step, (uint8_t) ~(1 << channel));
  }

  drive(0xFF, true, false);
  expectReleased("nOE1 high");

  drive(0x00, true, false);
  expectReleased("nOE1 high inputs changed");

  drive(0xFF, false, true);
  expectReleased("nOE2 high");

  drive(0x00, false, false);
  expectDriven("enabled again", 0xFF);
}

void setup() {
  Serial.begin(115200);
  for (uint8_t channel = 0; channel < 8; channel++) {
    pinMode(PIN_A[channel], OUTPUT);
    pinMode(PIN_Y[channel], INPUT);
  }
  pinMode(PIN_OE1, OUTPUT);
  pinMode(PIN_OE2, OUTPUT);
  digitalWrite(PIN_OE1, HIGH);
  digitalWrite(PIN_OE2, HIGH);

  Serial.println("74HC540 bench. Send any character to start.");
  while (Serial.available() == 0) {
  }
  while (Serial.available() > 0) {
    Serial.read();
  }
  run();
  Serial.println(failed ? resultLine : "RESULT PASS");
}

void loop() {
}
