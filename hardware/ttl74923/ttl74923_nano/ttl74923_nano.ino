/*
 * Self-check for a 74HC923 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * The sketch closes the key matrix itself: a row is driven low only while its
 * column reads low. OSC is a slow clock from D7. KBM is left open, so debounce
 * is defeated. Columns and data pins use the Nano pull-ups; a high-Z data pin
 * therefore reads high. OE is active low. Code bit A is the LSB. Key number is
 * row * 4 + column, Y1X1 = 0 through Y5X4 = 19.
 */

const uint8_t ROW_PINS[5] = {2, 3, 4, 5, 6};
const uint8_t COL_PINS[4] = {11, 10, 9, 8};
const uint8_t PIN_OSC = 7;
const uint8_t PIN_DAV = 12;
const uint8_t PIN_OE = 13;
const uint8_t PIN_A = A4;
const uint8_t PIN_B = A3;
const uint8_t PIN_C = A2;
const uint8_t PIN_D = A1;
const uint8_t PIN_E = A0;

const unsigned long WAIT_MS = 300;
const uint8_t DATA_PINS[5] = {PIN_A, PIN_B, PIN_C, PIN_D, PIN_E};

bool failed = false;
char resultLine[96];

void noteFailure(const char* reason) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s", reason);
}

void releaseRows() {
  for (uint8_t row = 0; row < 5; row++) {
    pinMode(ROW_PINS[row], INPUT_PULLUP);
  }
}

void driveMatrix(const bool pressed[20]) {
  bool pullRow[5] = {false, false, false, false, false};
  for (uint8_t key = 0; key < 20; key++) {
    if (!pressed[key]) continue;
    const uint8_t row = key / 4;
    const uint8_t col = key % 4;
    if (digitalRead(COL_PINS[col]) == LOW) pullRow[row] = true;
  }
  for (uint8_t row = 0; row < 5; row++) {
    if (pullRow[row]) {
      pinMode(ROW_PINS[row], OUTPUT);
      digitalWrite(ROW_PINS[row], LOW);
    } else {
      pinMode(ROW_PINS[row], INPUT_PULLUP);
    }
  }
}

void clockRise() {
  digitalWrite(PIN_OSC, LOW);
  delay(2);
  digitalWrite(PIN_OSC, HIGH);
  delay(1);
}

bool waitDav(bool level, const bool pressed[20], unsigned long timeoutMs) {
  const unsigned long start = millis();
  while ((unsigned long)(millis() - start) < timeoutMs) {
    clockRise();
    driveMatrix(pressed);
    delay(2);
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
  if (digitalRead(PIN_E) == HIGH) code |= 16;
  return code;
}

void clearKeys(bool pressed[20]) {
  for (uint8_t key = 0; key < 20; key++) pressed[key] = false;
}

bool expectCode(uint8_t expected, const bool pressed[20]) {
  driveMatrix(pressed);
  delay(1);
  const uint8_t actual = readCode();
  if (actual == expected) return true;
  char reason[48];
  snprintf(reason, sizeof(reason), "code expected %u got %u", expected, actual);
  noteFailure(reason);
  return false;
}

bool checkScan() {
  bool pressed[20];
  clearKeys(pressed);
  bool seen[4] = {false, false, false, false};
  int previous = -1;
  for (uint8_t step = 0; step < 8; step++) {
    clockRise();
    driveMatrix(pressed);
    delay(1);
    int lows = 0;
    int column = -1;
    for (uint8_t col = 0; col < 4; col++) {
      if (digitalRead(COL_PINS[col]) == LOW) {
        lows++;
        column = col;
      }
    }
    if (lows != 1) {
      noteFailure("scan did not drive one column");
      return false;
    }
    seen[column] = true;
    if (previous >= 0 && column != (previous + 1) % 4) {
      noteFailure("column order was not X1 X2 X3 X4");
      return false;
    }
    previous = column;
  }
  for (uint8_t col = 0; col < 4; col++) {
    if (!seen[col]) {
      noteFailure("scan missed a column");
      return false;
    }
  }
  return true;
}

bool pressAndCheck(uint8_t key) {
  bool pressed[20];
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
  delay(5);
  return true;
}

bool checkSecondKeyIgnored() {
  bool pressed[20];
  clearKeys(pressed);
  pressed[0] = true;
  if (!waitDav(true, pressed, WAIT_MS)) {
    noteFailure("key 0 DAV did not rise");
    return false;
  }
  if (!expectCode(0, pressed)) return false;

  pressed[5] = true;
  const unsigned long start = millis();
  while ((unsigned long)(millis() - start) < 40) {
    clockRise();
    driveMatrix(pressed);
    delay(1);
  }
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
  delay(5);
  return true;
}

bool checkOutputEnable() {
  bool pressed[20];
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
  for (uint8_t bit = 0; bit < 5; bit++) {
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
  delay(5);
  return true;
}

bool checkDavStaysDriven() {
  bool pressed[20];
  clearKeys(pressed);
  digitalWrite(PIN_OE, HIGH);
  if (!waitDav(false, pressed, WAIT_MS)) {
    noteFailure("DAV was not low with no key");
    return false;
  }
  pinMode(PIN_DAV, INPUT_PULLUP);
  delay(2);
  driveMatrix(pressed);
  const bool drivenLow = digitalRead(PIN_DAV) == LOW;
  pinMode(PIN_DAV, INPUT);
  digitalWrite(PIN_OE, LOW);
  if (!drivenLow) {
    noteFailure("DAV floated while no key was pressed");
    return false;
  }
  return true;
}

void runChecks() {
  failed = false;
  digitalWrite(PIN_OE, LOW);
  releaseRows();
  delay(5);

  if (!checkScan()) {
    Serial.println(resultLine);
    return;
  }
  for (uint8_t key = 0; key < 20; key++) {
    if (!pressAndCheck(key)) {
      Serial.println(resultLine);
      return;
    }
  }
  if (!checkSecondKeyIgnored() || !checkOutputEnable() || !checkDavStaysDriven()) {
    Serial.println(resultLine);
    return;
  }
  Serial.println("RESULT PASS");
}

void setup() {
  Serial.begin(115200);
  for (uint8_t row = 0; row < 5; row++) pinMode(ROW_PINS[row], INPUT_PULLUP);
  for (uint8_t col = 0; col < 4; col++) pinMode(COL_PINS[col], INPUT_PULLUP);
  pinMode(PIN_OSC, OUTPUT);
  digitalWrite(PIN_OSC, LOW);
  pinMode(PIN_DAV, INPUT);
  pinMode(PIN_OE, OUTPUT);
  digitalWrite(PIN_OE, HIGH);
  for (uint8_t bit = 0; bit < 5; bit++) pinMode(DATA_PINS[bit], INPUT_PULLUP);
  Serial.println("Send any character to start");
}

void loop() {
  if (Serial.available() > 0) {
    while (Serial.available() > 0) Serial.read();
    runChecks();
  }
}
