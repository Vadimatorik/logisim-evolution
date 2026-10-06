/*
 * Self-check for a 74HC592 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * Only RCO is visible. A is the least significant bit. CCLR, CLOAD and CCKEN
 * are active low. CCLR wins over CLOAD. RCO is high only at code 255.
 * CCLR stays low, both clocks stay low, and CCKEN and CLOAD stay high until
 * the check starts, so the counter is held clear.
 */

const uint8_t PIN_A = 2;
const uint8_t PIN_B = 3;
const uint8_t PIN_C = 4;
const uint8_t PIN_D = 5;
const uint8_t PIN_E = 6;
const uint8_t PIN_F = 7;
const uint8_t PIN_G = 8;
const uint8_t PIN_RCO = 9;
const uint8_t PIN_CCLR = 10;
const uint8_t PIN_CCK = 11;
const uint8_t PIN_CCKEN = 12;
const uint8_t PIN_RCK = 13;
const uint8_t PIN_CLOAD = A0;
const uint8_t PIN_H = A1;

const uint8_t DATA_PINS[] = {PIN_A, PIN_B, PIN_C, PIN_D, PIN_E, PIN_F, PIN_G, PIN_H};

bool failed = false;
char resultLine[96];

void noteFailure(const char* step, int expected, int actual) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s expected %d got %d", step, expected,
           actual);
}

void settle() { delay(1); }

void setData(uint8_t value) {
  for (uint8_t bit = 0; bit < 8; bit++) {
    digitalWrite(DATA_PINS[bit], (value & (1 << bit)) ? HIGH : LOW);
  }
}

void expectRco(bool high, const char* step) {
  settle();
  const int actual = digitalRead(PIN_RCO) == HIGH ? 1 : 0;
  const int expected = high ? 1 : 0;
  if (actual != expected) noteFailure(step, expected, actual);
}

void pulsePin(uint8_t pin) {
  digitalWrite(pin, LOW);
  settle();
  digitalWrite(pin, HIGH);
  settle();
  digitalWrite(pin, LOW);
  settle();
}

void holdControls() {
  digitalWrite(PIN_CCLR, HIGH);
  digitalWrite(PIN_CLOAD, HIGH);
  digitalWrite(PIN_CCKEN, HIGH);
}

void loadCounter(uint8_t value) {
  holdControls();
  setData(value);
  pulsePin(PIN_RCK);
  digitalWrite(PIN_CLOAD, LOW);
  settle();
  digitalWrite(PIN_CLOAD, HIGH);
  settle();
}

void enableCount() {
  digitalWrite(PIN_CCLR, HIGH);
  digitalWrite(PIN_CLOAD, HIGH);
  digitalWrite(PIN_CCKEN, LOW);
}

void checkClearLeavesTheRegister() {
  loadCounter(0xFF);
  expectRco(true, "loaded ones");
  digitalWrite(PIN_CCLR, LOW);
  expectRco(false, "clear");
  digitalWrite(PIN_CCLR, HIGH);
  digitalWrite(PIN_CLOAD, HIGH);
  expectRco(false, "clear released");
  digitalWrite(PIN_CLOAD, LOW);
  expectRco(true, "register survived clear");
  digitalWrite(PIN_CLOAD, HIGH);
}

void checkClearWinsOverLoad() {
  loadCounter(0xFF);
  digitalWrite(PIN_CLOAD, LOW);
  digitalWrite(PIN_CCLR, LOW);
  expectRco(false, "clear wins over load");
  digitalWrite(PIN_CCLR, HIGH);
  expectRco(true, "load resumes");
  digitalWrite(PIN_CLOAD, HIGH);
}

void checkLeastSignificantBit() {
  loadCounter(0x01);
  enableCount();
  uint16_t clocks = 0;
  while (clocks < 300 && digitalRead(PIN_RCO) == LOW) {
    pulsePin(PIN_CCK);
    clocks++;
    if (failed) return;
  }
  if (clocks != 254) noteFailure("clocks from 1 to carry", 254, clocks);
}

void checkEnableAndRollover() {
  loadCounter(0xFE);
  holdControls();
  pulsePin(PIN_CCK);
  expectRco(false, "count enable high");

  enableCount();
  digitalWrite(PIN_CCK, HIGH);
  expectRco(true, "count to 255");
  digitalWrite(PIN_CCK, LOW);
  expectRco(true, "falling clock holds 255");
  pulsePin(PIN_CCK);
  expectRco(false, "roll to zero");
}

void checkEnableIsSampledOnTheRisingClock() {
  loadCounter(0xFE);
  digitalWrite(PIN_CCKEN, HIGH);
  digitalWrite(PIN_CCK, LOW);
  settle();
  digitalWrite(PIN_CCK, HIGH);
  expectRco(false, "rising while disabled");
  digitalWrite(PIN_CCKEN, LOW);
  expectRco(false, "enable while clock high");
  digitalWrite(PIN_CCK, LOW);
  expectRco(false, "fall after late enable");
  digitalWrite(PIN_CCK, HIGH);
  expectRco(true, "next rising counts");
  digitalWrite(PIN_CCK, LOW);
}

void checkDataNeedsARegisterClock() {
  loadCounter(0x00);
  expectRco(false, "loaded zero");
  setData(0xFF);
  digitalWrite(PIN_CLOAD, LOW);
  expectRco(false, "data without register clock");
  pulsePin(PIN_RCK);
  expectRco(true, "register clock while load held");
  digitalWrite(PIN_CLOAD, HIGH);
}

void checkTransparentLoad() {
  digitalWrite(PIN_CCLR, HIGH);
  digitalWrite(PIN_CCKEN, HIGH);
  digitalWrite(PIN_CLOAD, LOW);
  setData(0x00);
  pulsePin(PIN_RCK);
  expectRco(false, "transparent zero");
  setData(0xFF);
  pulsePin(PIN_RCK);
  expectRco(true, "transparent ones");
  digitalWrite(PIN_CLOAD, HIGH);
  setData(0x00);
  pulsePin(PIN_RCK);
  expectRco(true, "register changes while load high");
}

void checkFullCycle() {
  loadCounter(0x00);
  enableCount();
  expectRco(false, "cycle start");
  for (uint16_t count = 0; count < 255; count++) {
    if (failed) return;
    char step[40];
    snprintf(step, sizeof(step), "cycle at %u", count);
    expectRco(false, step);
    pulsePin(PIN_CCK);
  }
  expectRco(true, "cycle at 255");
  pulsePin(PIN_CCK);
  expectRco(false, "cycle wrapped");
}

void driveIdle() {
  digitalWrite(PIN_CCLR, LOW);
  digitalWrite(PIN_CCK, LOW);
  digitalWrite(PIN_RCK, LOW);
  digitalWrite(PIN_CCKEN, HIGH);
  digitalWrite(PIN_CLOAD, HIGH);
  setData(0x00);
}

void setup() {
  Serial.begin(115200);
  for (uint8_t bit = 0; bit < 8; bit++) pinMode(DATA_PINS[bit], OUTPUT);
  pinMode(PIN_RCO, INPUT);
  pinMode(PIN_CCLR, OUTPUT);
  pinMode(PIN_CCK, OUTPUT);
  pinMode(PIN_CCKEN, OUTPUT);
  pinMode(PIN_RCK, OUTPUT);
  pinMode(PIN_CLOAD, OUTPUT);
  driveIdle();
  Serial.println("74HC592 ready. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available()) Serial.read();
  failed = false;
  resultLine[0] = '\0';
  driveIdle();
  expectRco(false, "held clear");
  checkClearLeavesTheRegister();
  checkClearWinsOverLoad();
  checkLeastSignificantBit();
  checkEnableAndRollover();
  checkEnableIsSampledOnTheRisingClock();
  checkDataNeedsARegisterClock();
  checkTransparentLoad();
  checkFullCycle();
  if (!failed) Serial.println("RESULT PASS");
  else Serial.println(resultLine);
}
