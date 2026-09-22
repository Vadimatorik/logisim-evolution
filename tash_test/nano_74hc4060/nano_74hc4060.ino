/*
 * Self-check for a 74HC4060 wired to an Arduino Nano as described in ../wiring.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 */

const uint8_t PIN_RS = 2;
const uint8_t PIN_MR = 3;
const uint16_t STAGE_MASK = 0x3FFF;
const uint16_t FULL_CYCLE = 16384;

struct OutputPin {
  const char* name;
  uint8_t pin;
  uint8_t stageBit;
};

const OutputPin OUTPUTS[] = {
    {"Q4", 4, 3},  {"Q5", 5, 4},  {"Q6", 6, 5},   {"Q7", 7, 6},  {"Q8", 8, 7},
    {"Q9", 9, 8},  {"Q10", 10, 9}, {"Q12", 11, 11}, {"Q13", 12, 12}, {"Q14", A0, 13},
};
const uint8_t OUTPUT_COUNT = sizeof(OUTPUTS) / sizeof(OUTPUTS[0]);

bool failed = false;
char resultLine[160];
uint16_t firstHigh[OUTPUT_COUNT];
bool seenHigh[OUTPUT_COUNT];

void noteFailure(unsigned count, const char* pin, int expected, int actual, const char* note) {
  if (failed) return;
  failed = true;
  if (note == nullptr) {
    snprintf(resultLine, sizeof(resultLine),
             "RESULT FAIL count=%u pin=%s expected=%d actual=%d", count, pin, expected, actual);
  } else {
    snprintf(resultLine, sizeof(resultLine),
             "RESULT FAIL count=%u pin=%s expected=%d actual=%d note=%s", count, pin, expected,
             actual, note);
  }
}

void pulseClock() {
  digitalWrite(PIN_RS, HIGH);
  digitalWrite(PIN_RS, LOW);
}

void resetCounter() {
  digitalWrite(PIN_RS, LOW);
  digitalWrite(PIN_MR, HIGH);
  delayMicroseconds(10);
  digitalWrite(PIN_MR, LOW);
  delayMicroseconds(10);
}

int readStage(uint8_t index) { return digitalRead(OUTPUTS[index].pin) == HIGH ? 1 : 0; }

void expectVisible(unsigned pulseNumber, uint16_t expectedCount) {
  for (uint8_t i = 0; i < OUTPUT_COUNT; i++) {
    const int expected = (expectedCount >> OUTPUTS[i].stageBit) & 1;
    const int actual = readStage(i);
    if (actual == 1 && !seenHigh[i]) {
      seenHigh[i] = true;
      firstHigh[i] = pulseNumber;
    }
    if (expected != actual) {
      noteFailure(pulseNumber, OUTPUTS[i].name, expected, actual, nullptr);
    }
  }
}

void clearFirstHigh() {
  for (uint8_t i = 0; i < OUTPUT_COUNT; i++) {
    seenHigh[i] = false;
    firstHigh[i] = 0;
  }
}

void checkPin7Threshold() {
  resetCounter();
  clearFirstHigh();
  for (uint8_t i = 0; i < 4; i++) pulseClock();
  const int after4 = digitalRead(4) == HIGH ? 1 : 0;
  Serial.print("PIN7 after 4 clocks: ");
  Serial.println(after4);
  if (after4 != 0) {
    noteFailure(4, "Q4", 0, after4, "pin7 divides by 8, Nexperia Q3 naming");
  }

  for (uint8_t i = 0; i < 4; i++) pulseClock();
  const int after8 = digitalRead(4) == HIGH ? 1 : 0;
  Serial.print("PIN7 after 8 clocks: ");
  Serial.println(after8);
  if (after8 != 1) {
    noteFailure(8, "Q4", 1, after8, nullptr);
  }
}

void checkRisingEdgeDoesNotCount() {
  resetCounter();
  for (uint8_t i = 0; i < 7; i++) pulseClock();
  digitalWrite(PIN_RS, HIGH);
  if (digitalRead(4) != LOW) {
    noteFailure(7, "Q4", 0, 1, "rising edge advanced the counter");
  }
  digitalWrite(PIN_RS, LOW);
  if (digitalRead(4) != HIGH) {
    noteFailure(8, "Q4", 1, 0, "falling edge did not advance the counter");
  }
}

void checkResetOverridesClock() {
  resetCounter();
  for (uint8_t i = 0; i < 8; i++) pulseClock();
  digitalWrite(PIN_RS, HIGH);
  digitalWrite(PIN_MR, HIGH);
  digitalWrite(PIN_RS, LOW);
  expectVisible(0, 0);

  pulseClock();
  expectVisible(0, 0);
  digitalWrite(PIN_MR, LOW);
  expectVisible(0, 0);

  for (uint8_t i = 0; i < 8; i++) pulseClock();
  expectVisible(8, 8);
}

void walkFullCycle() {
  resetCounter();
  clearFirstHigh();
  for (uint16_t count = 1; count <= FULL_CYCLE; count++) {
    pulseClock();
    expectVisible(count, count & STAGE_MASK);
  }
}

void printFirstHighs() {
  for (uint8_t i = 0; i < OUTPUT_COUNT; i++) {
    Serial.print("FIRST ");
    Serial.print(OUTPUTS[i].name);
    Serial.print("=");
    if (seenHigh[i]) {
      Serial.println(firstHigh[i]);
    } else {
      Serial.println("never");
    }
  }
}

void runChecks() {
  failed = false;
  resultLine[0] = '\0';
  checkPin7Threshold();
  checkRisingEdgeDoesNotCount();
  checkResetOverridesClock();
  walkFullCycle();
  printFirstHighs();
  if (!failed) {
    snprintf(resultLine, sizeof(resultLine), "RESULT PASS");
  }
  Serial.println(resultLine);
}

void setup() {
  pinMode(PIN_RS, OUTPUT);
  pinMode(PIN_MR, OUTPUT);
  digitalWrite(PIN_RS, LOW);
  digitalWrite(PIN_MR, HIGH);
  for (uint8_t i = 0; i < OUTPUT_COUNT; i++) {
    pinMode(OUTPUTS[i].pin, INPUT);
  }

  Serial.begin(115200);
  Serial.println("74HC4060 bench. Send any character to start.");
  while (Serial.read() < 0) {
    delay(10);
  }
  while (Serial.read() >= 0) {
  }
  runChecks();
}

void loop() {
  delay(2000);
  if (resultLine[0] != '\0') {
    Serial.println(resultLine);
  }
}
