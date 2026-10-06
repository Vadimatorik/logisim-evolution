/*
 * Self-check for a 74HC257 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * nOE high releases every Y output. nOE low passes I1 when S is high and I0
 * when S is low. Each Y pin is read through 470 ohms: pull-up then a short
 * discharge distinguishes driven high from high-Z.
 */

const uint8_t PIN_S = 2;
const uint8_t PIN_I0[4] = {3, 5, 7, 9};
const uint8_t PIN_I1[4] = {4, 6, 8, 10};
const uint8_t PIN_OE = 11;
const uint8_t PIN_Y[4] = {12, 13, A0, A1};

const uint8_t LEVEL_LOW = 0;
const uint8_t LEVEL_HIGH = 1;
const uint8_t LEVEL_Z = 2;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s", step);
}

void settle() { delayMicroseconds(20); }

uint8_t readLevel(uint8_t pin) {
  pinMode(pin, INPUT_PULLUP);
  delayMicroseconds(50);
  const int pulledUp = digitalRead(pin);

  pinMode(pin, OUTPUT);
  digitalWrite(pin, LOW);
  delayMicroseconds(5);
  pinMode(pin, INPUT);
  delayMicroseconds(50);
  const int released = digitalRead(pin);

  if (pulledUp == LOW && released == LOW) return LEVEL_LOW;
  if (pulledUp == HIGH && released == HIGH) return LEVEL_HIGH;
  if (pulledUp == HIGH && released == LOW) return LEVEL_Z;
  return 3;
}

char levelChar(uint8_t level) {
  if (level == LEVEL_LOW) return '0';
  if (level == LEVEL_HIGH) return '1';
  if (level == LEVEL_Z) return 'Z';
  return '?';
}

void setSources(uint8_t source0, uint8_t source1) {
  for (uint8_t channel = 0; channel < 4; channel++) {
    digitalWrite(PIN_I0[channel], (source0 & (1 << channel)) ? HIGH : LOW);
    digitalWrite(PIN_I1[channel], (source1 & (1 << channel)) ? HIGH : LOW);
  }
}

void expectOutputs(const char* step, const uint8_t expected[4]) {
  settle();
  uint8_t actual[4];
  for (uint8_t channel = 0; channel < 4; channel++) {
    actual[channel] = readLevel(PIN_Y[channel]);
  }

  Serial.print(step);
  Serial.print(" Y=");
  for (uint8_t channel = 0; channel < 4; channel++) {
    Serial.print(levelChar(actual[channel]));
  }
  Serial.println();

  for (uint8_t channel = 0; channel < 4; channel++) {
    if (actual[channel] != expected[channel]) noteFailure(step);
  }
}

void checkReleased() {
  char step[40];
  const uint8_t released[4] = {LEVEL_Z, LEVEL_Z, LEVEL_Z, LEVEL_Z};
  digitalWrite(PIN_OE, HIGH);
  for (uint8_t pattern = 0; pattern < 4; pattern++) {
    digitalWrite(PIN_S, (pattern & 1) ? HIGH : LOW);
    setSources(pattern == 2 ? 0x0 : 0xF, pattern == 3 ? 0x0 : 0xF);
    snprintf(step, sizeof(step), "release %u", pattern);
    expectOutputs(step, released);
  }
}

void checkSelected(bool selectHigh) {
  char step[48];
  digitalWrite(PIN_OE, LOW);
  digitalWrite(PIN_S, selectHigh ? HIGH : LOW);
  for (uint8_t pattern = 0; pattern < 16; pattern++) {
    const uint8_t source0 = selectHigh ? (uint8_t)~pattern : pattern;
    const uint8_t source1 = selectHigh ? pattern : (uint8_t)~pattern;
    setSources(source0, source1);
    uint8_t expected[4];
    for (uint8_t channel = 0; channel < 4; channel++) {
      expected[channel] = (pattern & (1 << channel)) ? LEVEL_HIGH : LEVEL_LOW;
    }
    snprintf(step, sizeof(step), "select %u pattern %u", selectHigh ? 1 : 0, pattern);
    expectOutputs(step, expected);
  }
}

void runChecks() {
  digitalWrite(PIN_OE, HIGH);
  digitalWrite(PIN_S, LOW);
  setSources(0, 0);
  settle();

  checkReleased();
  checkSelected(false);
  checkSelected(true);
}

void setup() {
  pinMode(PIN_S, OUTPUT);
  pinMode(PIN_OE, OUTPUT);
  for (uint8_t channel = 0; channel < 4; channel++) {
    pinMode(PIN_I0[channel], OUTPUT);
    pinMode(PIN_I1[channel], OUTPUT);
    pinMode(PIN_Y[channel], INPUT);
  }

  digitalWrite(PIN_OE, HIGH);
  digitalWrite(PIN_S, LOW);
  setSources(0, 0);

  Serial.begin(115200);
  Serial.println("READY 74HC257, send a character");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  failed = false;
  runChecks();
  if (!failed) Serial.println("RESULT PASS");
  else Serial.println(resultLine);
}
