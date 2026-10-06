/*
 * Self-check for a 74HC150 wired to an Arduino Nano and one 74HC595,
 * as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * A is the least significant select bit and D is the most significant.
 * Address 0 selects E0 and address 15 selects E15. A low strobe makes W
 * the complement of the selected data input. A high strobe forces W high.
 * E8..E15 come from the shift register: Q0 is E8 and Q7 is E15.
 * The strobe stays high until the check starts.
 */

const uint8_t PIN_SER = 2;
const uint8_t PIN_SRCLK = 3;
const uint8_t PIN_RCLK = 4;
const uint8_t PIN_E0 = 5;
const uint8_t PIN_E1 = 6;
const uint8_t PIN_E2 = 7;
const uint8_t PIN_E3 = 8;
const uint8_t PIN_E4 = 9;
const uint8_t PIN_E5 = 10;
const uint8_t PIN_E6 = 11;
const uint8_t PIN_E7 = 12;
const uint8_t PIN_G = 13;
const uint8_t PIN_A = A0;
const uint8_t PIN_B = A1;
const uint8_t PIN_C = A2;
const uint8_t PIN_D = A3;
const uint8_t PIN_W = A4;

const uint8_t LOW_DATA_PINS[] = {
  PIN_E0, PIN_E1, PIN_E2, PIN_E3, PIN_E4, PIN_E5, PIN_E6, PIN_E7
};

bool failed = false;
char resultLine[160];

void noteFailure(const char* detail) {
  if (failed) {
    return;
  }
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s", detail);
}

void pulse(uint8_t pin) {
  digitalWrite(pin, HIGH);
  digitalWrite(pin, LOW);
}

void settle() {
  delayMicroseconds(100);
}

void writeUpper(uint8_t value) {
  for (int8_t bit = 7; bit >= 0; bit--) {
    digitalWrite(PIN_SER, ((value >> bit) & 1) ? HIGH : LOW);
    pulse(PIN_SRCLK);
  }
  pulse(PIN_RCLK);
}

void writeLower(uint8_t value) {
  for (uint8_t bit = 0; bit < 8; bit++) {
    digitalWrite(LOW_DATA_PINS[bit], ((value >> bit) & 1) ? HIGH : LOW);
  }
}

void writeData(uint16_t data) {
  writeLower(data & 0xFF);
  writeUpper(data >> 8);
}

void writeAddress(uint8_t address) {
  digitalWrite(PIN_A, (address & 1) ? HIGH : LOW);
  digitalWrite(PIN_B, (address & 2) ? HIGH : LOW);
  digitalWrite(PIN_C, (address & 4) ? HIGH : LOW);
  digitalWrite(PIN_D, (address & 8) ? HIGH : LOW);
}

bool expectedHigh(bool strobeHigh, uint8_t address, uint16_t data) {
  if (strobeHigh) {
    return true;
  }
  return ((data >> address) & 1) == 0;
}

void apply(uint8_t address, uint16_t data, bool strobeHigh) {
  writeAddress(address);
  writeData(data);
  digitalWrite(PIN_G, strobeHigh ? HIGH : LOW);
  settle();

  const bool actualHigh = digitalRead(PIN_W) == HIGH;
  const bool wantHigh = expectedHigh(strobeHigh, address, data);
  if (actualHigh == wantHigh) {
    return;
  }
  char detail[140];
  snprintf(
      detail,
      sizeof(detail),
      "addr=%u data=%04X strobe=%u expected %u got %u",
      address,
      data,
      strobeHigh ? 1 : 0,
      wantHigh ? 1 : 0,
      actualHigh ? 1 : 0);
  noteFailure(detail);
}

void setup() {
  digitalWrite(PIN_G, HIGH);
  digitalWrite(PIN_SER, LOW);
  digitalWrite(PIN_SRCLK, LOW);
  digitalWrite(PIN_RCLK, LOW);
  digitalWrite(PIN_A, LOW);
  digitalWrite(PIN_B, LOW);
  digitalWrite(PIN_C, LOW);
  digitalWrite(PIN_D, LOW);
  for (uint8_t bit = 0; bit < 8; bit++) {
    digitalWrite(LOW_DATA_PINS[bit], LOW);
  }

  pinMode(PIN_SER, OUTPUT);
  pinMode(PIN_SRCLK, OUTPUT);
  pinMode(PIN_RCLK, OUTPUT);
  pinMode(PIN_G, OUTPUT);
  pinMode(PIN_A, OUTPUT);
  pinMode(PIN_B, OUTPUT);
  pinMode(PIN_C, OUTPUT);
  pinMode(PIN_D, OUTPUT);
  pinMode(PIN_W, INPUT);
  for (uint8_t bit = 0; bit < 8; bit++) {
    pinMode(LOW_DATA_PINS[bit], OUTPUT);
  }
  writeUpper(0);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  Serial.println("checking 48 patterns");
  for (uint8_t address = 0; address < 16; address++) {
    const uint16_t selected = (uint16_t)1 << address;
    const uint16_t othersHigh = (uint16_t)(~selected);
    apply(address, othersHigh, false);
    apply(address, selected, false);
    apply(address, selected, true);
  }

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
