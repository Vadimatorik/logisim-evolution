/*
 * Self-check for a 74LVC1G74 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * RD and SD are active low and override CP. Both low forces Q and nQ high.
 * Releasing one of them applies the input that stays low, without a clock.
 * With both high, D is stored on the rising edge of CP and nQ is its complement.
 * Outputs are push-pull, so Q and nQ are read directly.
 * RD stays low, SD stays high and both CP and D stay low until the check starts.
 * The clear is asynchronous, so the outputs are already defined before the first clock.
 *
 * A failure code packs Q in bit 0 and nQ in bit 1.
 */

const uint8_t PIN_CP = 2;
const uint8_t PIN_D = 3;
const uint8_t PIN_RD = 4;
const uint8_t PIN_SD = 5;
const uint8_t PIN_Q = 6;
const uint8_t PIN_NQ = 7;

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

uint8_t readOutputs() {
  uint8_t value = 0;
  if (digitalRead(PIN_Q)) value |= 1;
  if (digitalRead(PIN_NQ)) value |= 2;
  return value;
}

void expectOutputs(bool q, bool nq, const char* step) {
  settle();
  const uint8_t actual = readOutputs();
  const uint8_t expected = (q ? 1 : 0) | (nq ? 2 : 0);
  if (actual != expected) noteFailure(step, expected, actual);
}

void rise(bool data) {
  digitalWrite(PIN_D, data ? HIGH : LOW);
  digitalWrite(PIN_CP, LOW);
  settle();
  digitalWrite(PIN_CP, HIGH);
  settle();
}

void fall() {
  digitalWrite(PIN_CP, LOW);
  settle();
}

void checkAsynchronousControls() {
  digitalWrite(PIN_SD, HIGH);
  digitalWrite(PIN_RD, LOW);
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_D, HIGH);
  expectOutputs(false, true, "async clear");

  rise(true);
  expectOutputs(false, true, "clear overrides rising edge");

  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_D, LOW);
  digitalWrite(PIN_RD, HIGH);
  digitalWrite(PIN_SD, LOW);
  expectOutputs(true, false, "async preset");

  rise(false);
  expectOutputs(true, false, "preset overrides rising edge");
}

void checkBothAsserted() {
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_SD, LOW);
  digitalWrite(PIN_RD, LOW);
  expectOutputs(true, true, "both asserted");

  digitalWrite(PIN_SD, HIGH);
  expectOutputs(false, true, "release set leaves reset");

  digitalWrite(PIN_SD, LOW);
  digitalWrite(PIN_RD, LOW);
  expectOutputs(true, true, "both asserted again");

  digitalWrite(PIN_RD, HIGH);
  expectOutputs(true, false, "release reset leaves set");
}

void checkClockAndHold() {
  digitalWrite(PIN_SD, HIGH);
  digitalWrite(PIN_RD, HIGH);
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_D, LOW);
  expectOutputs(true, false, "inactive controls hold preset");

  rise(false);
  expectOutputs(false, true, "rising edge stores 0");

  digitalWrite(PIN_D, HIGH);
  expectOutputs(false, true, "data change while clock high holds");
  fall();
  expectOutputs(false, true, "falling edge holds");

  digitalWrite(PIN_D, HIGH);
  expectOutputs(false, true, "data change while clock low holds");
  rise(true);
  expectOutputs(true, false, "rising edge stores 1");

  digitalWrite(PIN_SD, LOW);
  digitalWrite(PIN_RD, LOW);
  digitalWrite(PIN_CP, LOW);
  expectOutputs(true, true, "both asserted before hold");

  digitalWrite(PIN_SD, HIGH);
  digitalWrite(PIN_RD, HIGH);
  digitalWrite(PIN_D, LOW);
  expectOutputs(true, true, "both-high pair held");
  fall();
  expectOutputs(true, true, "falling edge keeps both high");
  rise(false);
  expectOutputs(false, true, "next rising edge leaves both high");
}

void setup() {
  pinMode(PIN_CP, OUTPUT);
  pinMode(PIN_D, OUTPUT);
  pinMode(PIN_RD, OUTPUT);
  pinMode(PIN_SD, OUTPUT);
  pinMode(PIN_Q, INPUT);
  pinMode(PIN_NQ, INPUT);

  digitalWrite(PIN_RD, LOW);
  digitalWrite(PIN_SD, HIGH);
  digitalWrite(PIN_CP, LOW);
  digitalWrite(PIN_D, LOW);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  checkAsynchronousControls();
  checkBothAsserted();
  checkClockAndHold();

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
