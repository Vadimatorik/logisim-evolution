/*
 * Self-check for a 74HC126 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each Y pin has a 10k resistor to BIAS. A driven HC output overrides that
 * resistor. A high-impedance output follows BIAS, which is how Z is observed.
 */

struct Channel {
  const char* name;
  uint8_t oe;
  uint8_t a;
  uint8_t y;
};

const Channel CHANNELS[] = {
    {"1", 2, 3, 4},
    {"2", 5, 6, 7},
    {"3", 10, 9, 8},
    {"4", A1, A0, 11},
};
const uint8_t CHANNEL_COUNT = sizeof(CHANNELS) / sizeof(CHANNELS[0]);
const uint8_t BIAS = A2;

bool failed = false;
char resultLine[120];

void noteFailure(const char* name, const char* step, int expected, int actual) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine),
           "RESULT FAIL channel=%s step=%s expected=%d actual=%d", name, step, expected, actual);
}

void driveIdle() {
  digitalWrite(BIAS, LOW);
  for (uint8_t i = 0; i < CHANNEL_COUNT; i++) {
    digitalWrite(CHANNELS[i].oe, LOW);
    digitalWrite(CHANNELS[i].a, LOW);
  }
}

int level(uint8_t pin) {
  return digitalRead(pin) == HIGH ? 1 : 0;
}

void checkPin(const char* name, const char* step, uint8_t pin, int expected) {
  delay(1);
  const int actual = level(pin);
  const bool pass = actual == expected;
  Serial.print("channel ");
  Serial.print(name);
  Serial.print(" ");
  Serial.print(step);
  Serial.print(" expected=");
  Serial.print(expected);
  Serial.print(" actual=");
  Serial.print(actual);
  Serial.println(pass ? " PASS" : " FAIL");
  if (!pass) noteFailure(name, step, expected, actual);
}

void checkOthers(const Channel& active, const char* step, int expected) {
  for (uint8_t i = 0; i < CHANNEL_COUNT; i++) {
    if (CHANNELS[i].y == active.y) continue;
    checkPin(CHANNELS[i].name, step, CHANNELS[i].y, expected);
  }
}

void checkDriven(const Channel& channel, int inputLevel, int biasLevel) {
  digitalWrite(BIAS, biasLevel);
  digitalWrite(channel.oe, HIGH);
  digitalWrite(channel.a, inputLevel);
  checkPin(channel.name, inputLevel ? "driven-high" : "driven-low", channel.y, inputLevel);
  checkOthers(channel, "other-hiz", biasLevel);
  digitalWrite(channel.oe, LOW);
  digitalWrite(channel.a, LOW);
}

void checkReleased(const Channel& channel, int biasLevel) {
  digitalWrite(BIAS, biasLevel);
  digitalWrite(channel.oe, LOW);
  digitalWrite(channel.a, LOW);
  checkPin(channel.name, biasLevel ? "hiz-pullup" : "hiz-pulldown", channel.y, biasLevel);

  digitalWrite(channel.a, HIGH);
  checkPin(channel.name, biasLevel ? "hiz-pullup-input-changed" : "hiz-pulldown-input-changed",
           channel.y, biasLevel);
  checkOthers(channel, "other-hiz", biasLevel);
  digitalWrite(channel.a, LOW);
}

void runTests() {
  driveIdle();
  for (uint8_t i = 0; i < CHANNEL_COUNT; i++) {
    checkDriven(CHANNELS[i], LOW, HIGH);
    checkDriven(CHANNELS[i], HIGH, LOW);
    checkReleased(CHANNELS[i], HIGH);
    checkReleased(CHANNELS[i], LOW);
    driveIdle();
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(BIAS, OUTPUT);
  for (uint8_t i = 0; i < CHANNEL_COUNT; i++) {
    pinMode(CHANNELS[i].oe, OUTPUT);
    pinMode(CHANNELS[i].a, OUTPUT);
    pinMode(CHANNELS[i].y, INPUT);
  }
  driveIdle();

  Serial.println("74HC126 bench. Send any character to start.");
  while (Serial.available() == 0) {
  }
  while (Serial.available() > 0) {
    Serial.read();
  }

  runTests();
  Serial.println(failed ? resultLine : "RESULT PASS");
}

void loop() {}
