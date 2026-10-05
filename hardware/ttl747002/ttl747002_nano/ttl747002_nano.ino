/*
 * Self-check for a Philips 74HC7002 wired to an Arduino Nano as described in
 * ../README.md. Open Serial Monitor at 115200 baud and send any character to
 * start. The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each output is the NOR of its two inputs. Outputs are push-pull, so they
 * are read directly. All inputs stay low until the check starts, which holds
 * every output high. This sketch does not measure Schmitt-trigger hysteresis.
 * Do not use it with an SN74HC7002: that part has a different pinout.
 */

const uint8_t PIN_1A = 2;
const uint8_t PIN_1B = 3;
const uint8_t PIN_2A = 4;
const uint8_t PIN_2B = 5;
const uint8_t PIN_3A = 6;
const uint8_t PIN_3B = 7;
const uint8_t PIN_4A = 8;
const uint8_t PIN_4B = 9;
const uint8_t PIN_1Y = 10;
const uint8_t PIN_2Y = 11;
const uint8_t PIN_3Y = 12;
const uint8_t PIN_4Y = A0;

struct Gate {
  uint8_t a;
  uint8_t b;
  uint8_t y;
};

const Gate GATES[] = {
    {PIN_1A, PIN_1B, PIN_1Y},
    {PIN_2A, PIN_2B, PIN_2Y},
    {PIN_3A, PIN_3B, PIN_3Y},
    {PIN_4A, PIN_4B, PIN_4Y},
};

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, uint8_t expected, uint8_t actual) {
  if (failed) return;
  failed = true;
  snprintf(
      resultLine,
      sizeof(resultLine),
      "RESULT FAIL %s expected %u got %u",
      step,
      expected,
      actual);
}

void settle() { delay(1); }

void drive(uint8_t pin, bool high) { digitalWrite(pin, high ? HIGH : LOW); }

void setInputsLow() {
  for (uint8_t index = 0; index < 4; index++) {
    drive(GATES[index].a, false);
    drive(GATES[index].b, false);
  }
}

void expectLevel(uint8_t pin, bool high, const char* step) {
  settle();
  const uint8_t actual = digitalRead(pin) ? 1 : 0;
  const uint8_t expected = high ? 1 : 0;
  if (actual != expected) noteFailure(step, expected, actual);
}

void expectAll(bool y1, bool y2, bool y3, bool y4, const char* step) {
  expectLevel(PIN_1Y, y1, step);
  expectLevel(PIN_2Y, y2, step);
  expectLevel(PIN_3Y, y3, step);
  expectLevel(PIN_4Y, y4, step);
}

void checkTruthTable() {
  setInputsLow();
  expectAll(true, true, true, true, "all low");

  for (uint8_t index = 0; index < 4; index++) {
    const bool rows[4][2] = {{false, false}, {false, true}, {true, false}, {true, true}};
    for (uint8_t row = 0; row < 4; row++) {
      setInputsLow();
      drive(GATES[index].a, rows[row][0]);
      drive(GATES[index].b, rows[row][1]);
      const bool high = !(rows[row][0] || rows[row][1]);
      expectLevel(GATES[index].y, high, "truth");
      for (uint8_t other = 0; other < 4; other++) {
        if (other == index) continue;
        expectLevel(GATES[other].y, true, "other gate");
      }
    }
  }

  for (uint8_t index = 0; index < 4; index++) {
    drive(GATES[index].a, true);
    drive(GATES[index].b, true);
  }
  expectAll(false, false, false, false, "all high");
}

void setup() {
  const uint8_t inputs[] = {
      PIN_1A, PIN_1B, PIN_2A, PIN_2B, PIN_3A, PIN_3B, PIN_4A, PIN_4B};
  for (uint8_t index = 0; index < 8; index++) {
    pinMode(inputs[index], OUTPUT);
    digitalWrite(inputs[index], LOW);
  }
  pinMode(PIN_1Y, INPUT);
  pinMode(PIN_2Y, INPUT);
  pinMode(PIN_3Y, INPUT);
  pinMode(PIN_4Y, INPUT);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  checkTruthTable();

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
