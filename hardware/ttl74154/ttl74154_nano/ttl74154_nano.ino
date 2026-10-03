/*
 * Self-check for a 74HC154 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Both enables are active low. With both low, exactly one output goes low: its
 * number is the address A3 A2 A1 A0, and A0 is the least significant bit.
 * A high enable forces every output high. Y12/Y13 share A6 and Y14/Y15 share
 * A7 through 10k and 30k resistors, so those four outputs are read as voltages.
 */

const uint8_t PIN_A0 = 2;
const uint8_t PIN_A1 = 3;
const uint8_t PIN_A2 = 4;
const uint8_t PIN_A3 = 5;
const uint8_t PIN_nE1 = 6;
const uint8_t PIN_nE2 = 7;
const uint8_t PIN_Y[12] = {8, 9, 10, 11, 12, 13, A0, A1, A2, A3, A4, A5};

const uint8_t PAIR_BOTH_LOW = 0;
const uint8_t PAIR_LOW_10K = 1;
const uint8_t PAIR_LOW_30K = 2;
const uint8_t PAIR_BOTH_HIGH = 3;

bool failed = false;
char resultLine[180];

void noteFailure(const char* detail) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s", detail);
}

void apply(uint8_t address, bool nE1High, bool nE2High) {
  digitalWrite(PIN_A0, (address & 1) ? HIGH : LOW);
  digitalWrite(PIN_A1, (address & 2) ? HIGH : LOW);
  digitalWrite(PIN_A2, (address & 4) ? HIGH : LOW);
  digitalWrite(PIN_A3, (address & 8) ? HIGH : LOW);
  digitalWrite(PIN_nE1, nE1High ? HIGH : LOW);
  digitalWrite(PIN_nE2, nE2High ? HIGH : LOW);
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
  if (adc < 512) return PAIR_LOW_10K;
  if (adc < 896) return PAIR_LOW_30K;
  return PAIR_BOTH_HIGH;
}

uint16_t readDirect() {
  uint16_t value = 0;
  for (uint8_t bit = 0; bit < 12; bit++) {
    if (digitalRead(PIN_Y[bit]) == HIGH) value |= (uint16_t)1 << bit;
  }
  return value;
}

uint8_t expectedPair(bool enabled, uint8_t address, uint8_t low10kAddress) {
  if (!enabled) return PAIR_BOTH_HIGH;
  if (address == low10kAddress) return PAIR_LOW_10K;
  if (address == (uint8_t)(low10kAddress + 1)) return PAIR_LOW_30K;
  return PAIR_BOTH_HIGH;
}

void checkCase(uint8_t address, bool nE1High, bool nE2High) {
  apply(address, nE1High, nE2High);
  delay(1);

  const bool enabled = !nE1High && !nE2High;
  uint16_t expectedY = 0x0FFF;
  if (enabled && address < 12) expectedY &= ~((uint16_t)1 << address);
  const uint8_t expected6 = expectedPair(enabled, address, 12);
  const uint8_t expected7 = expectedPair(enabled, address, 14);

  const uint16_t actualY = readDirect();
  const int adc6 = readAdc(A6);
  const int adc7 = readAdc(A7);
  const uint8_t pair6 = classify(adc6);
  const uint8_t pair7 = classify(adc7);
  const bool pass = actualY == expectedY && pair6 == expected6 && pair7 == expected7;

  Serial.print(enabled ? "en" : "dis");
  Serial.print(" addr=");
  Serial.print(address);
  Serial.print(" nE1=");
  Serial.print(nE1High ? 1 : 0);
  Serial.print(" nE2=");
  Serial.print(nE2High ? 1 : 0);
  Serial.print(" Y=0x");
  Serial.print(actualY, HEX);
  Serial.print(" adc6=");
  Serial.print(adc6);
  Serial.print(" adc7=");
  Serial.print(adc7);
  Serial.println(pass ? " PASS" : " FAIL");

  if (pass) return;
  char detail[160];
  snprintf(detail, sizeof(detail),
           "addr=%u nE1=%u nE2=%u Y expected=0x%03X got=0x%03X pair6 expected=%u got=%u adc6=%d pair7 expected=%u got=%u adc7=%d",
           address, nE1High ? 1 : 0, nE2High ? 1 : 0, expectedY, actualY, expected6, pair6, adc6,
           expected7, pair7, adc7);
  noteFailure(detail);
}

void runChecks() {
  for (uint8_t enables = 0; enables < 4; enables++) {
    const bool nE1High = enables & 1;
    const bool nE2High = enables & 2;
    for (uint8_t address = 0; address < 16; address++) {
      checkCase(address, nE1High, nE2High);
    }
  }
}

void setup() {
  pinMode(PIN_A0, OUTPUT);
  pinMode(PIN_A1, OUTPUT);
  pinMode(PIN_A2, OUTPUT);
  pinMode(PIN_A3, OUTPUT);
  pinMode(PIN_nE1, OUTPUT);
  pinMode(PIN_nE2, OUTPUT);
  digitalWrite(PIN_nE1, HIGH);
  digitalWrite(PIN_nE2, HIGH);
  apply(0, true, true);
  for (uint8_t bit = 0; bit < 12; bit++) pinMode(PIN_Y[bit], INPUT);

  Serial.begin(115200);
  Serial.println("Send any character to test the 74HC154");
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
