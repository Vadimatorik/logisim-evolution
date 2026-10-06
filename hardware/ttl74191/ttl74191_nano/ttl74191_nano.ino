/*
 * Self-check for a 74HC191 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * A low PL loads D into Q immediately. With PL high, a rising CP counts when CE
 * is low: low U/D counts up and high U/D counts down, modulo 16. TC is high at
 * terminal count. RC is low only while TC is high, CE is low and CP is low.
 * After the initial load, CE and U/D change only while CP is high.
 * Bit 0 is D0/Q0. Q, TC and RC are push-pull, so no pulldown is required.
 */

const uint8_t PIN_D[4] = {A1, 2, 10, 9};
const uint8_t PIN_Q[4] = {4, 3, 7, 8};
const uint8_t PIN_CE = 5;
const uint8_t PIN_UD = 6;
const uint8_t PIN_PL = 11;
const uint8_t PIN_TC = 12;
const uint8_t PIN_RC = A0;
const uint8_t PIN_CP = 13;

bool failed = false;
char resultLine[96];
uint8_t presented = 0;
uint8_t count = 0;
bool plHigh = false;
bool ceHigh = true;
bool down = false;
bool cpHigh = false;

void noteFailure(const char* step, const char* detail) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s %s", step, detail);
}

void settle() { delay(1); }

void writeData(uint8_t value) {
  presented = value & 0x0F;
  for (uint8_t bit = 0; bit < 4; bit++) {
    digitalWrite(PIN_D[bit], (presented >> bit) & 1 ? HIGH : LOW);
  }
  settle();
  if (!plHigh) count = presented;
}

void drivePl(bool high) {
  plHigh = high;
  digitalWrite(PIN_PL, high ? HIGH : LOW);
  settle();
  if (!plHigh) count = presented;
}

void driveCe(bool high) {
  ceHigh = high;
  digitalWrite(PIN_CE, high ? HIGH : LOW);
  settle();
}

void driveUd(bool countDown) {
  down = countDown;
  digitalWrite(PIN_UD, countDown ? HIGH : LOW);
  settle();
}

void driveCp(bool high) {
  const bool rising = !cpHigh && high;
  cpHigh = high;
  digitalWrite(PIN_CP, high ? HIGH : LOW);
  settle();
  if (rising && plHigh && !ceHigh) {
    count = down ? (count - 1) & 0x0F : (count + 1) & 0x0F;
  }
}

bool level(uint8_t pin) { return digitalRead(pin) == HIGH; }

void expect(const char* step) {
  if (failed) return;
  for (uint8_t bit = 0; bit < 4; bit++) {
    const bool actual = level(PIN_Q[bit]);
    const bool expected = ((count >> bit) & 1) != 0;
    if (actual == expected) continue;
    char detail[48];
    snprintf(detail, sizeof(detail), "Q%u expected %u got %u", bit, expected, actual);
    noteFailure(step, detail);
    return;
  }
  const bool terminal = down ? count == 0 : count == 15;
  const bool rcLow = terminal && !ceHigh && !cpHigh;
  const bool tcHigh = level(PIN_TC);
  const bool rcHigh = level(PIN_RC);
  if (tcHigh != terminal) {
    char detail[48];
    snprintf(detail, sizeof(detail), "TC expected %u got %u", terminal, tcHigh);
    noteFailure(step, detail);
    return;
  }
  if (rcHigh == rcLow) {
    char detail[48];
    snprintf(detail, sizeof(detail), "RC expected %u got %u", !rcLow, rcHigh);
    noteFailure(step, detail);
  }
}

void load(uint8_t value) {
  writeData(value);
  drivePl(false);
}

void runChecks() {
  expect("power-up-load-0");
  drivePl(true);
  expect("release-load");

  const uint8_t patterns[] = {0x0, 0xF, 0x5, 0xA, 0x1, 0x2, 0x4, 0x8};
  for (uint8_t index = 0; index < sizeof(patterns); index++) {
    char label[16];
    snprintf(label, sizeof(label), "load-%u", patterns[index]);
    load(patterns[index]);
    expect(label);
  }

  drivePl(true);
  writeData(0x3);
  expect("hold-data");
  driveCp(true);
  expect("hold-rise-while-inhibited");
  driveCp(false);
  expect("hold-fall");

  load(0);
  expect("load-0-before-up");
  drivePl(true);
  driveCp(true);
  driveCe(false);
  driveCp(false);
  for (uint8_t step = 0; step < 16; step++) {
    driveCp(true);
    char label[16];
    snprintf(label, sizeof(label), "up-%u", count);
    expect(label);
    driveCp(false);
  }

  driveCp(true);
  driveCe(true);
  driveCp(false);
  driveCp(true);
  expect("inhibit");

  driveCe(false);
  driveCp(false);
  load(0x3);
  driveCp(true);
  expect("load-overrides-clock");
  drivePl(true);
  expect("hold-after-overriding-load");
  driveCp(false);
  driveCp(true);
  expect("count-after-load");

  driveCp(true);
  driveCe(true);
  driveUd(false);
  driveCp(false);
  load(15);
  expect("tc-up-15");
  drivePl(true);
  driveCp(true);
  driveUd(true);
  expect("tc-follows-direction");
  driveUd(false);
  expect("tc-up-again");
  driveCe(false);
  expect("rc-high-while-clock-high");
  driveCp(false);
  expect("rc-pulse");
  driveCp(true);
  expect("wrap-up-clears-tc");

  driveCp(true);
  driveUd(true);
  driveCp(false);
  load(0);
  expect("rc-pulse-down");
  drivePl(true);
  driveCp(true);
  expect("wrap-down");
  driveCp(false);
  driveCp(true);
  expect("down-14");

  driveCp(true);
  driveUd(false);
  driveCp(false);
  load(7);
  expect("rc-idle-off-terminal");
}

void setup() {
  digitalWrite(PIN_PL, LOW);
  digitalWrite(PIN_CE, HIGH);
  digitalWrite(PIN_UD, LOW);
  digitalWrite(PIN_CP, LOW);
  for (uint8_t bit = 0; bit < 4; bit++) digitalWrite(PIN_D[bit], LOW);
  pinMode(PIN_PL, OUTPUT);
  pinMode(PIN_CE, OUTPUT);
  pinMode(PIN_UD, OUTPUT);
  pinMode(PIN_CP, OUTPUT);
  for (uint8_t bit = 0; bit < 4; bit++) {
    pinMode(PIN_D[bit], OUTPUT);
    pinMode(PIN_Q[bit], INPUT);
  }
  pinMode(PIN_TC, INPUT);
  pinMode(PIN_RC, INPUT);
  settle();

  Serial.begin(115200);
  Serial.println("Send any character to test the 74HC191");
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
