/*
 * Self-check for a 74HC4514 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * LE is active high. While it is high, the address A3 A2 A1 A0 selects the
 * output, and A0 is the least significant bit. Taking LE low stores that
 * address. E is active low: while it is low, exactly one output goes high.
 * A high E forces every output low and does not change the latch.
 * Q12/Q13 share A6 and Q14/Q15 share A7 through 10k and 30k resistors, so
 * those four outputs are read as voltages.
 */

const uint8_t PIN_A0 = 2;
const uint8_t PIN_A1 = 3;
const uint8_t PIN_A2 = 4;
const uint8_t PIN_A3 = 5;
const uint8_t PIN_LE = 6;
const uint8_t PIN_nE = 7;
const uint8_t PIN_Q[12] = {8, 9, 10, 11, 12, 13, A0, A1, A2, A3, A4, A5};

const uint8_t PAIR_BOTH_LOW = 0;
const uint8_t PAIR_HIGH_30K = 1;
const uint8_t PAIR_HIGH_10K = 2;
const uint8_t PAIR_BOTH_HIGH = 3;

const uint8_t LATCH_SAMPLES[] = {0, 5, 11, 12, 13, 14, 15};

bool failed = false;
char resultLine[200];

void noteFailure(const char* detail) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s", detail);
}

void apply(uint8_t address, bool leHigh, bool nEHigh) {
  digitalWrite(PIN_A0, (address & 1) ? HIGH : LOW);
  digitalWrite(PIN_A1, (address & 2) ? HIGH : LOW);
  digitalWrite(PIN_A2, (address & 4) ? HIGH : LOW);
  digitalWrite(PIN_A3, (address & 8) ? HIGH : LOW);
  digitalWrite(PIN_LE, leHigh ? HIGH : LOW);
  digitalWrite(PIN_nE, nEHigh ? HIGH : LOW);
}

int readAdc(uint8_t pin) {
  analogRead(pin);
  delayMicroseconds(300);
  long sum = 0;
  for (uint8_t sample = 0; sample < 4; sample++) {
    sum += analogRead(pin);
  }
  return (int)(sum / 4);
}

uint8_t classify(int adc) {
  if (adc < 128) return PAIR_BOTH_LOW;
  if (adc < 512) return PAIR_HIGH_30K;
  if (adc < 896) return PAIR_HIGH_10K;
  return PAIR_BOTH_HIGH;
}

uint16_t readDirect() {
  uint16_t value = 0;
  for (uint8_t bit = 0; bit < 12; bit++) {
    if (digitalRead(PIN_Q[bit]) == HIGH) value |= (uint16_t)1 << bit;
  }
  return value;
}

uint8_t expectedPair(int selected, uint8_t high10kAddress) {
  if (selected < 0) return PAIR_BOTH_LOW;
  if (selected == high10kAddress) return PAIR_HIGH_10K;
  if (selected == (int)(high10kAddress + 1)) return PAIR_HIGH_30K;
  return PAIR_BOTH_LOW;
}

void checkCase(const char* phase, uint8_t driven, bool leHigh, bool nEHigh, int selected) {
  apply(driven, leHigh, nEHigh);
  delay(1);

  uint16_t expectedY = 0;
  if (selected >= 0 && selected < 12) expectedY |= (uint16_t)1 << selected;
  const uint8_t expected6 = expectedPair(selected, 12);
  const uint8_t expected7 = expectedPair(selected, 14);

  const uint16_t actualY = readDirect();
  const int adc6 = readAdc(A6);
  const int adc7 = readAdc(A7);
  const uint8_t pair6 = classify(adc6);
  const uint8_t pair7 = classify(adc7);
  const bool pass = actualY == expectedY && pair6 == expected6 && pair7 == expected7;

  Serial.print(phase);
  Serial.print(" drv=");
  Serial.print(driven);
  Serial.print(" LE=");
  Serial.print(leHigh ? 1 : 0);
  Serial.print(" nE=");
  Serial.print(nEHigh ? 1 : 0);
  Serial.print(" sel=");
  if (selected < 0) Serial.print("none");
  else Serial.print(selected);
  Serial.print(" Y=0x");
  Serial.print(actualY, HEX);
  Serial.print(" adc6=");
  Serial.print(adc6);
  Serial.print(" adc7=");
  Serial.print(adc7);
  Serial.println(pass ? " PASS" : " FAIL");

  if (pass) return;
  char detail[180];
  snprintf(detail, sizeof(detail),
           "%s drv=%u LE=%u nE=%u sel=%d Y expected=0x%03X got=0x%03X pair6 expected=%u got=%u adc6=%d pair7 expected=%u got=%u adc7=%d",
           phase, driven, leHigh ? 1 : 0, nEHigh ? 1 : 0, selected, expectedY, actualY, expected6,
           pair6, adc6, expected7, pair7, adc7);
  noteFailure(detail);
}

void runTransparent() {
  for (uint8_t address = 0; address < 16; address++) {
    checkCase("open-en", address, true, false, address);
    checkCase("open-dis", address, true, true, -1);
  }
}

void runLatch() {
  for (uint8_t index = 0; index < sizeof(LATCH_SAMPLES); index++) {
    const uint8_t stored = LATCH_SAMPLES[index];
    const uint8_t other = (uint8_t)((stored + 7) & 15);

    checkCase("capture", stored, true, false, stored);
    checkCase("hold", other, false, false, stored);
    checkCase("blank", other, false, true, -1);
    checkCase("restore", other, false, false, stored);

    checkCase("store-dis", other, true, true, -1);
    checkCase("held-dis", stored, false, false, other);
  }
}

void setup() {
  pinMode(PIN_A0, OUTPUT);
  pinMode(PIN_A1, OUTPUT);
  pinMode(PIN_A2, OUTPUT);
  pinMode(PIN_A3, OUTPUT);
  pinMode(PIN_LE, OUTPUT);
  pinMode(PIN_nE, OUTPUT);
  digitalWrite(PIN_LE, LOW);
  digitalWrite(PIN_nE, HIGH);
  apply(0, false, true);
  for (uint8_t bit = 0; bit < 12; bit++) pinMode(PIN_Q[bit], INPUT);

  Serial.begin(115200);
  Serial.println("Send any character to test the 74HC4514");
  while (Serial.available() == 0) {
  }
  while (Serial.available() > 0) Serial.read();

  runTransparent();
  runLatch();
  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
