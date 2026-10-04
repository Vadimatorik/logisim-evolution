/*
 * Self-check for a 74HC1G125 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * OE is active low. A low OE copies A to Y. A high OE releases Y.
 * Two 100k resistors, from Y to 5V and from Y to GND, hold a released
 * output near mid-scale. analogRead below 200 is low, above 800 is high,
 * and 400 to 600 is high impedance. OE stays high and A stays low until
 * the check starts.
 */

const uint8_t PIN_OE = 2;
const uint8_t PIN_A = 3;
const uint8_t PIN_Y = A0;

const int LOW_MAX = 200;
const int HIGH_MIN = 800;
const int Z_MIN = 400;
const int Z_MAX = 600;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, const char* expected, int actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s expected %s got %d",
      step,
      expected,
      actual);
}

int readY() {
  delay(2);
  analogRead(PIN_Y);
  delay(1);
  return analogRead(PIN_Y);
}

void expectLevel(const char* expected, bool matches, int reading, const char* step) {
  if (!matches) noteFailure(step, expected, reading);
}

void expectLow(const char* step) {
  const int reading = readY();
  expectLevel("LOW", reading < LOW_MAX, reading, step);
}

void expectHigh(const char* step) {
  const int reading = readY();
  expectLevel("HIGH", reading > HIGH_MIN, reading, step);
}

void expectZ(const char* step) {
  const int reading = readY();
  expectLevel("Z", reading >= Z_MIN && reading <= Z_MAX, reading, step);
}

void drive(bool oeHigh, bool aHigh, const char* step) {
  digitalWrite(PIN_OE, oeHigh ? HIGH : LOW);
  digitalWrite(PIN_A, aHigh ? HIGH : LOW);
  if (oeHigh) expectZ(step);
  else if (aHigh) expectHigh(step);
  else expectLow(step);
}

void checkTable(const char* prefix) {
  char step[48];
  snprintf(step, sizeof(step), "%s OE low A low", prefix);
  drive(false, false, step);
  snprintf(step, sizeof(step), "%s OE low A high", prefix);
  drive(false, true, step);
  snprintf(step, sizeof(step), "%s OE high A low", prefix);
  drive(true, false, step);
  snprintf(step, sizeof(step), "%s OE high A high", prefix);
  drive(true, true, step);
}

void setup() {
  pinMode(PIN_OE, OUTPUT);
  pinMode(PIN_A, OUTPUT);
  pinMode(PIN_Y, INPUT);
  digitalWrite(PIN_OE, HIGH);
  digitalWrite(PIN_A, LOW);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  checkTable("table");
  for (uint8_t index = 0; index < 8; index++) {
    checkTable("repeat");
  }

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
