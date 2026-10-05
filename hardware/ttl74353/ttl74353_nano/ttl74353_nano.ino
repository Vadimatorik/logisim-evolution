/*
 * Self-check for a 74HC353 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * 1G and 2G are independent. A high strobe releases that channel. A low strobe
 * drives the inverted source chosen by B (MSB) and A (LSB). Each Y pin is read
 * through 470 ohms: pull-up then a short discharge distinguishes driven high
 * from high-Z.
 */

const uint8_t PIN_G1 = 2;
const uint8_t PIN_B = 3;
const uint8_t PIN_C1[4] = {7, 6, 5, 4};
const uint8_t PIN_Y1 = 12;
const uint8_t PIN_Y2 = 13;
const uint8_t PIN_C2[4] = {8, 9, 10, 11};
const uint8_t PIN_A = A0;
const uint8_t PIN_G2 = A1;

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

void setData(const uint8_t pins[4], uint8_t nibble) {
  for (uint8_t index = 0; index < 4; index++) {
    digitalWrite(pins[index], (nibble & (1 << index)) ? HIGH : LOW);
  }
}

void setSelect(uint8_t select) {
  digitalWrite(PIN_A, (select & 1) ? HIGH : LOW);
  digitalWrite(PIN_B, (select & 2) ? HIGH : LOW);
}

uint8_t oppositeNibble(uint8_t select, bool selectedHigh) {
  uint8_t nibble = selectedHigh ? 0x00 : 0x0F;
  if (selectedHigh) nibble |= (uint8_t)(1u << select);
  else nibble &= (uint8_t)~(1u << select);
  return nibble;
}

uint8_t invertedLevel(bool selectedHigh) {
  return selectedHigh ? LEVEL_LOW : LEVEL_HIGH;
}

void expectOutputs(const char* step, uint8_t expected1, uint8_t expected2) {
  settle();
  const uint8_t actual1 = readLevel(PIN_Y1);
  const uint8_t actual2 = readLevel(PIN_Y2);

  Serial.print(step);
  Serial.print(" Y=");
  Serial.print(levelChar(actual1));
  Serial.println(levelChar(actual2));

  if (actual1 != expected1 || actual2 != expected2) noteFailure(step);
}

void checkBothReleased() {
  char step[40];
  digitalWrite(PIN_G1, HIGH);
  digitalWrite(PIN_G2, HIGH);
  for (uint8_t select = 0; select < 4; select++) {
    setSelect(select);
    setData(PIN_C1, (select & 1) ? 0x00 : 0x0F);
    setData(PIN_C2, (select & 2) ? 0x00 : 0x0F);
    snprintf(step, sizeof(step), "both release %u", select);
    expectOutputs(step, LEVEL_Z, LEVEL_Z);
  }
}

void checkOneReleased(bool releaseFirst) {
  char step[48];
  digitalWrite(PIN_G1, releaseFirst ? HIGH : LOW);
  digitalWrite(PIN_G2, releaseFirst ? LOW : HIGH);
  for (uint8_t select = 0; select < 4; select++) {
    for (uint8_t pattern = 0; pattern < 4; pattern++) {
      const bool high1 = pattern & 1;
      const bool high2 = pattern & 2;
      setSelect(select);
      setData(PIN_C1, oppositeNibble(select, high1));
      setData(PIN_C2, oppositeNibble(select, high2));
      snprintf(step, sizeof(step), "strobe %u select %u pattern %u",
               releaseFirst ? 1 : 2, select, pattern);
      if (releaseFirst) expectOutputs(step, LEVEL_Z, invertedLevel(high2));
      else expectOutputs(step, invertedLevel(high1), LEVEL_Z);
    }
  }
}

void checkSelected() {
  char step[48];
  digitalWrite(PIN_G1, LOW);
  digitalWrite(PIN_G2, LOW);
  for (uint8_t select = 0; select < 4; select++) {
    for (uint8_t pattern = 0; pattern < 4; pattern++) {
      const bool high1 = pattern & 1;
      const bool high2 = pattern & 2;
      setSelect(select);
      setData(PIN_C1, oppositeNibble(select, high1));
      setData(PIN_C2, oppositeNibble(select, high2));
      snprintf(step, sizeof(step), "select %u pattern %u", select, pattern);
      expectOutputs(step, invertedLevel(high1), invertedLevel(high2));
    }
  }
}

void runChecks() {
  digitalWrite(PIN_G1, HIGH);
  digitalWrite(PIN_G2, HIGH);
  setSelect(0);
  setData(PIN_C1, 0);
  setData(PIN_C2, 0);
  settle();

  checkBothReleased();
  checkOneReleased(true);
  checkOneReleased(false);
  checkSelected();
}

void setup() {
  pinMode(PIN_G1, OUTPUT);
  pinMode(PIN_G2, OUTPUT);
  pinMode(PIN_A, OUTPUT);
  pinMode(PIN_B, OUTPUT);
  pinMode(PIN_Y1, INPUT);
  pinMode(PIN_Y2, INPUT);
  for (uint8_t index = 0; index < 4; index++) {
    pinMode(PIN_C1[index], OUTPUT);
    pinMode(PIN_C2[index], OUTPUT);
  }

  digitalWrite(PIN_G1, HIGH);
  digitalWrite(PIN_G2, HIGH);
  setSelect(0);
  setData(PIN_C1, 0);
  setData(PIN_C2, 0);

  Serial.begin(115200);
  Serial.println("READY 74HC353, send a character");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  failed = false;
  runChecks();
  if (!failed) Serial.println("RESULT PASS");
  else Serial.println(resultLine);
}
