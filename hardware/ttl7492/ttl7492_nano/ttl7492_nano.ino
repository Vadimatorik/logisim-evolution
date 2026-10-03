/*
 * Self-check for a 74x92 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * CP0 clocks Q0 and CP1 clocks Q1..Q3, both on the falling edge. The stages
 * are separate: this sketch copies Q0 onto CP1 when it checks divide-by-twelve.
 * Do not jumper those pins. Both MR pins high clear every output. Outputs are
 * read with analogRead so a 74LS high near 2.7 V still counts as high.
 * Both MR pins stay high until the check starts.
 */

const uint8_t PIN_CP1 = 2;
const uint8_t PIN_MR1 = 3;
const uint8_t PIN_MR2 = 4;
const uint8_t PIN_CP0 = 13;
const uint8_t PIN_Q0 = A0;
const uint8_t PIN_Q1 = A1;
const uint8_t PIN_Q2 = A2;
const uint8_t PIN_Q3 = A3;
const uint8_t PIN_Q[4] = {PIN_Q0, PIN_Q1, PIN_Q2, PIN_Q3};

// About 2.5 V on a 5 V Nano. Above a 74LS minimum high, below a solid low.
const int HIGH_THRESHOLD = 512;

const uint8_t DIVIDE_BY_SIX[] = {0x2, 0x4, 0x8, 0xA, 0xC, 0x0};
const uint8_t DIVIDE_BY_TWELVE[] = {1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13, 0};

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

bool pinHigh(uint8_t pin) { return analogRead(pin) > HIGH_THRESHOLD; }

uint8_t readOutputs() {
  uint8_t value = 0;
  for (uint8_t bit = 0; bit < 4; bit++) {
    if (pinHigh(PIN_Q[bit])) value |= static_cast<uint8_t>(1u << bit);
  }
  return value;
}

void expect(uint8_t expected, const char* step) {
  settle();
  const uint8_t actual = readOutputs();
  if (actual != expected) noteFailure(step, expected, actual);
}

void pulse(uint8_t clockPin) {
  digitalWrite(clockPin, HIGH);
  settle();
  digitalWrite(clockPin, LOW);
  settle();
}

void followQ0() { digitalWrite(PIN_CP1, pinHigh(PIN_Q0) ? HIGH : LOW); }

void cascadePulse() {
  followQ0();
  digitalWrite(PIN_CP0, HIGH);
  settle();
  followQ0();
  digitalWrite(PIN_CP0, LOW);
  settle();
  followQ0();
  settle();
}

void runChecks() {
  for (uint8_t step = 0; step < 4; step++) {
    pulse(PIN_CP0);
    pulse(PIN_CP1);
    expect(0, "reset-hold");
  }

  digitalWrite(PIN_MR1, LOW);
  digitalWrite(PIN_MR2, LOW);
  expect(0, "release");

  digitalWrite(PIN_CP0, HIGH);
  expect(0, "rising");
  digitalWrite(PIN_CP0, LOW);
  expect(1, "div2-high");
  pulse(PIN_CP0);
  expect(0, "div2-low");
  pulse(PIN_CP0);
  expect(1, "div2-high-again");
  pulse(PIN_CP0);
  expect(0, "div2-low-again");

  for (uint8_t step = 0; step < sizeof(DIVIDE_BY_SIX); step++) {
    char label[16];
    snprintf(label, sizeof(label), "div6 %u", step + 1);
    pulse(PIN_CP1);
    expect(DIVIDE_BY_SIX[step], label);
  }

  for (uint8_t step = 0; step < sizeof(DIVIDE_BY_TWELVE); step++) {
    char label[16];
    snprintf(label, sizeof(label), "div12 %u", step + 1);
    cascadePulse();
    expect(DIVIDE_BY_TWELVE[step], label);
  }

  pulse(PIN_CP0);
  pulse(PIN_CP1);
  expect(3, "before-partial-reset");
  digitalWrite(PIN_MR1, HIGH);
  expect(3, "mr1-only");
  digitalWrite(PIN_MR1, LOW);
  digitalWrite(PIN_MR2, HIGH);
  expect(3, "mr2-only");
  digitalWrite(PIN_MR1, HIGH);
  expect(0, "both-mr");
  pulse(PIN_CP0);
  pulse(PIN_CP1);
  expect(0, "clock-during-reset");

  digitalWrite(PIN_MR1, LOW);
  digitalWrite(PIN_MR2, LOW);
  settle();
  pulse(PIN_CP0);
  expect(1, "count-after-reset");
}

void setup() {
  digitalWrite(PIN_CP0, LOW);
  digitalWrite(PIN_CP1, LOW);
  digitalWrite(PIN_MR1, HIGH);
  digitalWrite(PIN_MR2, HIGH);
  pinMode(PIN_CP0, OUTPUT);
  pinMode(PIN_CP1, OUTPUT);
  pinMode(PIN_MR1, OUTPUT);
  pinMode(PIN_MR2, OUTPUT);
  pinMode(PIN_Q0, INPUT);
  pinMode(PIN_Q1, INPUT);
  pinMode(PIN_Q2, INPUT);
  pinMode(PIN_Q3, INPUT);

  Serial.begin(115200);
  Serial.println("Send any character to test the 74x92");
  while (Serial.available() == 0) {
  }
  while (Serial.available() > 0) Serial.read();
  runChecks();
  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
