/*
 * Self-check for a 74HC393 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Each half counts on the falling edge of nCP. A high nMR clears that half and
 * overrides the clock. The count wraps from 15 to 0. nQ0 is the least
 * significant bit. The two counters are independent.
 */

const uint8_t PIN_1CP = 2;
const uint8_t PIN_1MR = 3;
const uint8_t PIN_1Q0 = 4;
const uint8_t PIN_1Q1 = 5;
const uint8_t PIN_1Q2 = 6;
const uint8_t PIN_1Q3 = 7;
const uint8_t PIN_2Q3 = 8;
const uint8_t PIN_2Q2 = 9;
const uint8_t PIN_2Q1 = 10;
const uint8_t PIN_2Q0 = 11;
const uint8_t PIN_2MR = 12;
const uint8_t PIN_2CP = 13;

struct Half {
  uint8_t pinCp;
  uint8_t pinMr;
  uint8_t pinQ[4];
  bool cp;
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

void settle() { delayMicroseconds(20); }

uint8_t readNibble(const Half& half) {
  uint8_t value = 0;
  for (uint8_t bit = 0; bit < 4; bit++) {
    if (digitalRead(half.pinQ[bit])) value |= (1 << bit);
  }
  return value;
}

void check(const Half& half, const char* step) {
  const uint8_t actual = readNibble(half);
  if (actual != half.count) noteFailure(step, half.count, actual);
}

void setCp(Half& half, bool high, const char* step) {
  const bool falling = half.cp && !high;
  half.cp = high;
  digitalWrite(half.pinCp, high ? HIGH : LOW);
  settle();
  if (half.mr) half.count = 0;
  else if (falling) half.count = (half.count + 1) & 0x0F;
  check(half, step);
}

void setMr(Half& half, bool high, const char* step) {
  half.mr = high;
  digitalWrite(half.pinMr, high ? HIGH : LOW);
  settle();
  if (high) half.count = 0;
  check(half, step);
}

void pulse(Half& half, const char* step) {
  setCp(half, true, step);
  setCp(half, false, step);
}

void resetOnFallingEdge(Half& half, const char* step) {
  half.mr = true;
  half.cp = false;
  digitalWrite(half.pinMr, HIGH);
  digitalWrite(half.pinCp, LOW);
  settle();
  half.count = 0;
  check(half, step);
}

void prepare(Half& half, uint8_t cp, uint8_t mr, uint8_t q0, uint8_t q1, uint8_t q2, uint8_t q3) {
  half.pinCp = cp;
  half.pinMr = mr;
  half.pinQ[0] = q0;
  half.pinQ[1] = q1;
  half.pinQ[2] = q2;
  half.pinQ[3] = q3;
  half.cp = false;
  half.mr = true;
  half.count = 0;
  digitalWrite(cp, LOW);
  digitalWrite(mr, HIGH);
  pinMode(cp, OUTPUT);
  pinMode(mr, OUTPUT);
  pinMode(q0, INPUT);
  pinMode(q1, INPUT);
  pinMode(q2, INPUT);
  pinMode(q3, INPUT);
}

void countWrap(Half& half, const char* name) {
  for (uint8_t step = 0; step < 16; step++) {
    char label[24];
    snprintf(label, sizeof(label), "%s %u", name, step + 1);
    pulse(half, label);
  }
}

void runChecks() {
  delay(1);
  check(counter1, "reset-1");
  check(counter2, "reset-2");
  pulse(counter1, "clock-during-reset-1");
  pulse(counter2, "clock-during-reset-2");

  setMr(counter1, false, "release-mr-1");
  setMr(counter2, false, "release-mr-2");
  setCp(counter1, true, "rising-1");
  setCp(counter1, false, "count-1 1");
  for (uint8_t step = 1; step < 16; step++) {
    char label[24];
    snprintf(label, sizeof(label), "count-1 %u", step + 1);
    pulse(counter1, label);
  }
  check(counter2, "counter-2 idle");
  countWrap(counter2, "count-2");
  check(counter1, "counter-1 idle");

  pulse(counter1, "hold-1 a");
  pulse(counter1, "hold-1 b");
  pulse(counter1, "hold-1 c");
  pulse(counter2, "hold-2 a");
  pulse(counter2, "hold-2 b");
  pulse(counter2, "hold-2 c");
  pulse(counter2, "hold-2 d");
  pulse(counter2, "hold-2 e");
  check(counter1, "counter-1 at 3");

  setMr(counter1, true, "mr-1");
  check(counter2, "counter-2 at 5");
  pulse(counter1, "clock-during-mr-1");

  setCp(counter2, true, "2cp-high");
  resetOnFallingEdge(counter2, "mr-2-on-falling-edge");
  check(counter1, "counter-1 still clear");
  pulse(counter2, "clock-during-mr-2");
  setMr(counter2, false, "release-mr-2-again");
  pulse(counter2, "count-after-mr-2");
  check(counter1, "counter-1 after mr-2");
}

void setup() {
  prepare(counter1, PIN_1CP, PIN_1MR, PIN_1Q0, PIN_1Q1, PIN_1Q2, PIN_1Q3);
  prepare(counter2, PIN_2CP, PIN_2MR, PIN_2Q0, PIN_2Q1, PIN_2Q2, PIN_2Q3);
  Serial.begin(115200);
  Serial.println("Send any character to test the 74HC393");
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
