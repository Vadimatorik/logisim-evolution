/*
 * Self-check for a 74HC922 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * The sketch closes the key matrix itself: a row is driven low only while its
 * column reads low. OSC is 100 nF and KBM is 1 uF, so a real press is debounced
 * for about 10 ms. Columns need external 10 kOhm pull-ups. Data pins use the
 * Nano pull-ups so a high-Z output reads high. OE is active low. Code bit A is
 * the LSB. Key number is row * 4 + column, Y1X1 = 0 through Y4X4 = 15.
 */

const uint8_t ROW_PINS[4] = {2, 3, 4, 5};
const uint8_t COL_PINS[4] = {9, 8, 7, 6};
const uint8_t PIN_DAV = 10;
const uint8_t PIN_OE = 11;
const uint8_t PIN_A = A0;
const uint8_t PIN_B = A1;
const uint8_t PIN_C = A2;
const uint8_t PIN_D = A3;

const unsigned long WAIT_MS = 200;
const uint8_t DATA_PINS[4] = {PIN_A, PIN_B, PIN_C, PIN_D};

bool failed = false;
char resultLine[96];

void noteFailure(const char* reason) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s", reason);
}

void releaseRows() {
  for (uint8_t row = 0; row < 4; row++) {
    pinMode(ROW_PINS[row], INPUT_PULLUP);
  }
}

void driveMatrix(const bool pressed[16]) {
  bool pullRow[4] = {false, false, false, false};
  for (uint8_t key = 0; key < 16; key++) {
    if (!pressed[key]) continue;
    const uint8_t row = key / 4;
    const uint8_t col = key % 4;
    if (digitalRead(COL_PINS[col]) == LOW) pullRow[row] = true;
  }
  for (uint8_t row = 0; row < 4; row++) {
    if (pullRow[row]) {
      pinMode(ROW_PINS[row], OUTPUT);
      digitalWrite(ROW_PINS[row], LOW);
    } else {
      pinMode(ROW_PINS[row], INPUT_PULLUP);
    }
  }
}

bool waitDav(bool level, const bool pressed[16], unsigned long timeoutMs) {
  const unsigned long start = millis();
  while ((unsigned long)(millis() - start) < timeoutMs) {
    driveMatrix(pressed);
    if (digitalRead(PIN_DAV) == (level ? HIGH : LOW)) {
      driveMatrix(pressed);
      return true;
    }
  }
  driveMatrix(pressed);
  return digitalRead(PIN_DAV) == (level ? HIGH : LOW);
}

uint8_t readCode() {
  uint8_t code = 0;
  if (digitalRead(PIN_A) == HIGH) code |= 1;
  if (digitalRead(PIN_B) == HIGH) code |= 2;
  if (digitalRead(PIN_C) == HIGH) code |= 4;
  if (digitalRead(PIN_D) == HIGH) code |= 8;
  return code;
}

void clearKeys(bool pressed[16]) {
  for (uint8_t key = 0; key < 16; key++) pressed[key] = false;
}

bool expectCode(uint8_t expected, const bool pressed[16]) {
  driveMatrix(pressed);
  delay(1);
  const uint8_t actual = readCode();
  if (actual == expected) return true;
  char reason[48];
  snprintf(reason, sizeof(reason), "code expected %u got %u", expected, actual);
  noteFailure(reason);
  return false;
}

bool pressAndCheck(uint8_t key) {
  bool pressed[16];
  clearKeys(pressed);
  pressed[key] = true;
  if (!waitDav(true, pressed, WAIT_MS)) {
    char reason[48];
    snprintf(reason, sizeof(reason), "key %u DAV did not rise", key);
    noteFailure(reason);
    return false;
  }
  if (!expectCode(key, pressed)) return false;

  clearKeys(pressed);
  if (!waitDav(false, pressed, WAIT_MS)) {
    char reason[48];
    snprintf(reason, sizeof(reason), "key %u DAV did not fall", key);
    noteFailure(reason);
    return false;
  }
  if (!expectCode(key, pressed)) return false;
  delay(30);
  return true;
}

bool checkSecondKeyIgnored() {
  bool pressed[16];
  clearKeys(pressed);
  pressed[0] = true;
  if (!waitDav(true, pressed, WAIT_MS)) {
    noteFailure("key 0 DAV did not rise");
    return false;
  }
  if (!expectCode(0, pressed)) return false;

  pressed[5] = true;
  const unsigned long start = millis();
  while ((unsigned long)(millis() - start) < 40) driveMatrix(pressed);
  if (digitalRead(PIN_DAV) != HIGH || readCode() != 0) {
    noteFailure("second key changed the held code");
    return false;
  }

  pressed[0] = false;
  if (!waitDav(false, pressed, WAIT_MS)) {
    noteFailure("DAV stayed high after the first key");
    return false;
  }
  if (readCode() != 0) {
    noteFailure("code changed before the second key was accepted");
    return false;
  }
  if (!waitDav(true, pressed, WAIT_MS)) {
    noteFailure("second key DAV did not rise");
    return false;
  }
  if (!expectCode(5, pressed)) return false;

  clearKeys(pressed);
  if (!waitDav(false, pressed, WAIT_MS)) {
    noteFailure("DAV stayed high after the second key");
    return false;
  }
  delay(30);
  return true;
}

bool checkOutputEnable() {
  bool pressed[16];
  clearKeys(pressed);
  pressed[0] = true;
  digitalWrite(PIN_OE, LOW);
  if (!waitDav(true, pressed, WAIT_MS)) {
    noteFailure("OE check DAV did not rise");
    return false;
  }
  if (!expectCode(0, pressed)) return false;

  digitalWrite(PIN_OE, HIGH);
  delay(1);
  driveMatrix(pressed);
  for (uint8_t bit = 0; bit < 4; bit++) {
    if (digitalRead(DATA_PINS[bit]) != HIGH) {
      noteFailure("OE high did not release the data pins");
      return false;
    }
  }
  if (digitalRead(PIN_DAV) != HIGH) {
    noteFailure("OE high cleared DAV");
    return false;
  }

  digitalWrite(PIN_OE, LOW);
  delay(1);
  if (!expectCode(0, pressed)) return false;
  clearKeys(pressed);
  if (!waitDav(false, pressed, WAIT_MS)) {
    noteFailure("DAV stayed high after OE check");
    return false;
  }
  delay(30);
  return true;
}

bool checkBounce() {
  bool pressed[16];
  clearKeys(pressed);
  const unsigned long start = millis();
  while ((unsigned long)(millis() - start) < 40) {
    if (digitalRead(COL_PINS[0]) == LOW) {
      pinMode(ROW_PINS[0], OUTPUT);
      digitalWrite(ROW_PINS[0], LOW);
      delayMicroseconds(200);
      pinMode(ROW_PINS[0], INPUT_PULLUP);
      delay(1);
    }
  }
  releaseRows();
  if (digitalRead(PIN_DAV) == HIGH) {
    noteFailure("short bounce raised DAV");
    return false;
  }
  return pressAndCheck(0);
}

void runChecks() {
  failed = false;
  digitalWrite(PIN_OE, LOW);
  releaseRows();
  delay(30);

  for (uint8_t key = 0; key < 16; key++) {
    if (!pressAndCheck(key)) {
      Serial.println(resultLine);
      return;
    }
  }
  if (!checkSecondKeyIgnored() || !checkOutputEnable() || !checkBounce()) {
    Serial.println(resultLine);
    return;
  }
  Serial.println("RESULT PASS");
}

void setup() {
  Serial.begin(115200);
  for (uint8_t row = 0; row < 4; row++) pinMode(ROW_PINS[row], INPUT_PULLUP);
  for (uint8_t col = 0; col < 4; col++) pinMode(COL_PINS[col], INPUT);
  pinMode(PIN_DAV, INPUT);
  pinMode(PIN_OE, OUTPUT);
  digitalWrite(PIN_OE, HIGH);
  for (uint8_t bit = 0; bit < 4; bit++) pinMode(DATA_PINS[bit], INPUT_PULLUP);
  Serial.println("Send any character to start");
}

void loop() {
  if (Serial.available() > 0) {
    while (Serial.available() > 0) Serial.read();
    runChecks();
  }
}
