/*
 * Self-check for a 74HC4520 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each half counts on the rising edge of nCP0 while nCP1 is high, and on the
 * falling edge of nCP1 while nCP0 is low. A high nMR clears that half and
 * overrides the clock. The count wraps from 15 to 0.
 */

const uint8_t PIN_1CP0 = 2;
const uint8_t PIN_1CP1 = 3;
const uint8_t PIN_1MR = 4;
const uint8_t PIN_2CP0 = 5;
const uint8_t PIN_2CP1 = 6;
const uint8_t PIN_2MR = 7;
const uint8_t PIN_1Q0 = 13;
const uint8_t PIN_1Q1 = 8;
const uint8_t PIN_1Q2 = 9;
const uint8_t PIN_1Q3 = 10;
const uint8_t PIN_2Q0 = 11;
const uint8_t PIN_2Q1 = 12;
const uint8_t PIN_2Q2 = A0;
const uint8_t PIN_2Q3 = A1;

struct Half {
  uint8_t pinCp0;
  uint8_t pinCp1;
  uint8_t pinMr;
  uint8_t pinQ0;
  uint8_t pinQ1;
  uint8_t pinQ2;
  uint8_t pinQ3;
  bool cp0;
  bool cp1;
  bool mr;
  uint8_t count;
};

bool failed = false;
char resultLine[96];
Half counter1;
Half counter2;

void noteFailure(const char* step, uint8_t expected, uint8_t actual) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s expected %u got %u", step, expected, actual);
}

void settle() { delayMicroseconds(5); }

bool derived(bool cp0, bool cp1) { return cp0 || !cp1; }

uint8_t readNibble(const Half& half) {
  uint8_t value = 0;
  if (digitalRead(half.pinQ0)) value |= 1;
  if (digitalRead(half.pinQ1)) value |= 2;
  if (digitalRead(half.pinQ2)) value |= 4;
  if (digitalRead(half.pinQ3)) value |= 8;
  return value;
}

void check(const Half& half, const char* step) {
  const uint8_t actual = readNibble(half);
  if (actual != half.count) noteFailure(step, half.count, actual);
}

void setCp0(Half& half, bool high, const char* step) {
  const bool previous = derived(half.cp0, half.cp1);
  half.cp0 = high;
  digitalWrite(half.pinCp0, high ? HIGH : LOW);
  settle();
  if (half.mr) half.count = 0;
  else if (!previous && derived(half.cp0, half.cp1)) half.count = (half.count + 1) & 0x0F;
  check(half, step);
}

void setCp1(Half& half, bool high, const char* step) {
  const bool previous = derived(half.cp0, half.cp1);
  half.cp1 = high;
  digitalWrite(half.pinCp1, high ? HIGH : LOW);
  settle();
  if (half.mr) half.count = 0;
  else if (!previous && derived(half.cp0, half.cp1)) half.count = (half.count + 1) & 0x0F;
  check(half, step);
}

void setMr(Half& half, bool high, const char* step) {
  half.mr = high;
  digitalWrite(half.pinMr, high ? HIGH : LOW);
  settle();
  if (high) half.count = 0;
  check(half, step);
}

void pulseCp0(Half& half, const char* step) {
  setCp0(half, true, step);
  setCp0(half, false, step);
}

void prepare(Half& half, uint8_t cp0, uint8_t cp1, uint8_t mr, uint8_t q0, uint8_t q1, uint8_t q2, uint8_t q3) {
  half.pinCp0 = cp0;
  half.pinCp1 = cp1;
  half.pinMr = mr;
  half.pinQ0 = q0;
  half.pinQ1 = q1;
  half.pinQ2 = q2;
  half.pinQ3 = q3;
  half.cp0 = false;
  half.cp1 = true;
  half.mr = true;
  half.count = 0;
  pinMode(cp0, OUTPUT);
  pinMode(cp1, OUTPUT);
  pinMode(mr, OUTPUT);
  pinMode(q0, INPUT);
  pinMode(q1, INPUT);
  pinMode(q2, INPUT);
  pinMode(q3, INPUT);
  digitalWrite(cp0, LOW);
  digitalWrite(cp1, HIGH);
  digitalWrite(mr, HIGH);
}

void runChecks() {
  delay(1);
  check(counter1, "reset-1");
  check(counter2, "reset-2");
  setMr(counter1, false, "release-mr-1");
  setMr(counter2, false, "release-mr-2");

  for (uint8_t step = 0; step < 16; step++) {
    char label[24];
    snprintf(label, sizeof(label), "count-1 %u", step + 1);
    pulseCp0(counter1, label);
  }
  check(counter2, "counter-2 idle");

  setCp0(counter1, true, "cp0-rise");
  setCp1(counter1, false, "cp1-fall-cp0-high");
  setCp0(counter1, false, "cp0-fall");
  setCp0(counter1, true, "cp0-rise-cp1-low");
  setCp0(counter1, false, "cp0-fall-cp1-low");
  setCp0(counter1, true, "cp0-high");
  setCp1(counter1, true, "cp1-rise-cp0-high");
  setCp1(counter1, false, "cp1-fall-cp0-high-2");
  setCp0(counter1, false, "cp0-low");
  setCp1(counter1, true, "cp1-rise-cp0-low");
  setCp1(counter1, false, "cp1-fall-counts");

  setMr(counter1, true, "mr-1");
  setCp1(counter1, true, "cp1-high-during-mr");
  pulseCp0(counter1, "clock-during-mr");
  setMr(counter1, false, "release-mr-again");
  pulseCp0(counter1, "count-after-mr");

  pulseCp0(counter2, "count-2 a");
  pulseCp0(counter2, "count-2 b");
  pulseCp0(counter2, "count-2 c");
  setCp1(counter2, false, "cp1-2-fall");
  check(counter1, "counter-1 held");

  setMr(counter2, true, "mr-2");
  check(counter1, "counter-1 after mr-2");
}

void setup() {
  prepare(counter1, PIN_1CP0, PIN_1CP1, PIN_1MR, PIN_1Q0, PIN_1Q1, PIN_1Q2, PIN_1Q3);
  prepare(counter2, PIN_2CP0, PIN_2CP1, PIN_2MR, PIN_2Q0, PIN_2Q1, PIN_2Q2, PIN_2Q3);
  Serial.begin(115200);
  Serial.println("Send any character to test the 74HC4520");
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
