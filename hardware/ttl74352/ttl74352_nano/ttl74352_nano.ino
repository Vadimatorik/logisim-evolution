/*
 * Self-check for a 74HC352 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * S0 is the least significant select bit. A high strobe forces that section's
 * output high. A low strobe makes the output the complement of the selected
 * data input. Outputs are push-pull, so n1Y and n2Y are read directly.
 * Both strobes stay high until the check starts.
 */

const uint8_t PIN_N1E = 2;
const uint8_t PIN_S1 = 3;
const uint8_t PIN_1D3 = 4;
const uint8_t PIN_1D2 = 5;
const uint8_t PIN_1D1 = 6;
const uint8_t PIN_1D0 = 7;
const uint8_t PIN_N1Y = 8;
const uint8_t PIN_N2Y = 9;
const uint8_t PIN_2D0 = 10;
const uint8_t PIN_2D1 = 11;
const uint8_t PIN_2D2 = 12;
const uint8_t PIN_2D3 = 13;
const uint8_t PIN_S0 = A0;
const uint8_t PIN_N2E = A1;

bool failed = false;
char resultLine[160];

void noteFailure(const char* detail) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s", detail);
}

void settle() { delayMicroseconds(100); }

bool expectedLevel(bool inhibit, bool s1, bool s0, bool d0, bool d1, bool d2, bool d3) {
  if (inhibit) return HIGH;
  const bool data = !s1 && !s0 ? d0 : !s1 && s0 ? d1 : s1 && !s0 ? d2 : d3;
  return !data;
}

void setup() {
  digitalWrite(PIN_N1E, HIGH);
  digitalWrite(PIN_N2E, HIGH);
  digitalWrite(PIN_S1, LOW);
  digitalWrite(PIN_S0, LOW);
  digitalWrite(PIN_1D0, LOW);
  digitalWrite(PIN_1D1, LOW);
  digitalWrite(PIN_1D2, LOW);
  digitalWrite(PIN_1D3, LOW);
  digitalWrite(PIN_2D0, LOW);
  digitalWrite(PIN_2D1, LOW);
  digitalWrite(PIN_2D2, LOW);
  digitalWrite(PIN_2D3, LOW);

  pinMode(PIN_N1E, OUTPUT);
  pinMode(PIN_S1, OUTPUT);
  pinMode(PIN_1D3, OUTPUT);
  pinMode(PIN_1D2, OUTPUT);
  pinMode(PIN_1D1, OUTPUT);
  pinMode(PIN_1D0, OUTPUT);
  pinMode(PIN_N1Y, INPUT);
  pinMode(PIN_N2Y, INPUT);
  pinMode(PIN_2D0, OUTPUT);
  pinMode(PIN_2D1, OUTPUT);
  pinMode(PIN_2D2, OUTPUT);
  pinMode(PIN_2D3, OUTPUT);
  pinMode(PIN_S0, OUTPUT);
  pinMode(PIN_N2E, OUTPUT);

  Serial.begin(115200);
  while (!Serial.available()) {
  }
  while (Serial.available()) {
    Serial.read();
  }

  Serial.println("checking 4096 patterns");
  for (uint16_t code = 0; code < 4096; code++) {
    const bool n1e = code & 1;
    const bool s1 = code & 2;
    const bool d13 = code & 4;
    const bool d12 = code & 8;
    const bool d11 = code & 16;
    const bool d10 = code & 32;
    const bool d20 = code & 64;
    const bool d21 = code & 128;
    const bool d22 = code & 256;
    const bool d23 = code & 512;
    const bool s0 = code & 1024;
    const bool n2e = code & 2048;

    digitalWrite(PIN_N1E, n1e ? HIGH : LOW);
    digitalWrite(PIN_S1, s1 ? HIGH : LOW);
    digitalWrite(PIN_1D3, d13 ? HIGH : LOW);
    digitalWrite(PIN_1D2, d12 ? HIGH : LOW);
    digitalWrite(PIN_1D1, d11 ? HIGH : LOW);
    digitalWrite(PIN_1D0, d10 ? HIGH : LOW);
    digitalWrite(PIN_2D0, d20 ? HIGH : LOW);
    digitalWrite(PIN_2D1, d21 ? HIGH : LOW);
    digitalWrite(PIN_2D2, d22 ? HIGH : LOW);
    digitalWrite(PIN_2D3, d23 ? HIGH : LOW);
    digitalWrite(PIN_S0, s0 ? HIGH : LOW);
    digitalWrite(PIN_N2E, n2e ? HIGH : LOW);
    settle();

    const bool y1 = digitalRead(PIN_N1Y) == HIGH;
    const bool y2 = digitalRead(PIN_N2Y) == HIGH;
    const bool expect1 = expectedLevel(n1e, s1, s0, d10, d11, d12, d13) == HIGH;
    const bool expect2 = expectedLevel(n2e, s1, s0, d20, d21, d22, d23) == HIGH;
    if (y1 != expect1 || y2 != expect2) {
      char detail[140];
      snprintf(
          detail,
          sizeof(detail),
          "n1E=%u S1=%u S0=%u 1D=%u%u%u%u n2E=%u 2D=%u%u%u%u y1 expected %u got %u y2 expected %u got %u",
          n1e,
          s1,
          s0,
          d13,
          d12,
          d11,
          d10,
          n2e,
          d23,
          d22,
          d21,
          d20,
          expect1,
          y1,
          expect2,
          y2);
      noteFailure(detail);
    }
  }

  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void loop() {}
