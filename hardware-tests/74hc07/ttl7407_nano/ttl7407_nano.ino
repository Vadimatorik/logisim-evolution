/*
 * Self-check for a 74HC07 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * WIRED_AND stays 0 for the six independent buffers. Set it to 1 only after
 * removing the pull-up on pin 4 and jumpering pin 2 to pin 4.
 */

const uint8_t WIRED_AND = 0;

struct Buffer {
  const char* name;
  uint8_t pinA;
  uint8_t pinY;
};

const Buffer BUFFERS[] = {
    {"1", 2, 8},
    {"2", 3, 9},
    {"3", 4, 10},
    {"4", 5, 11},
    {"5", 6, 12},
    {"6", 7, A0},
};
const uint8_t BUFFER_COUNT = sizeof(BUFFERS) / sizeof(BUFFERS[0]);

bool failed = false;
bool wiredFailed = false;
char resultLine[120];

void noteFailure(const char* name, int inputA, int expected, int actual) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL buffer=%s A=%d expected=%d actual=%d", name,
           inputA, expected, actual);
}

void driveAllHigh() {
  for (uint8_t i = 0; i < BUFFER_COUNT; i++) {
    digitalWrite(BUFFERS[i].pinA, HIGH);
  }
}

void checkLevel(const Buffer& buffer, int inputA) {
  driveAllHigh();
  digitalWrite(buffer.pinA, inputA ? HIGH : LOW);
  delay(1);
  const int actual = digitalRead(buffer.pinY) == HIGH ? 1 : 0;
  Serial.print("buffer ");
  Serial.print(buffer.name);
  Serial.print(" A=");
  Serial.print(inputA);
  Serial.print(" expected=");
  Serial.print(inputA);
  Serial.print(" actual=");
  Serial.print(actual);
  Serial.println(inputA == actual ? " PASS" : " FAIL");
  if (inputA != actual) noteFailure(buffer.name, inputA, inputA, actual);
}

void checkBuffers(uint8_t first) {
  for (uint8_t i = first; i < BUFFER_COUNT; i++) {
    checkLevel(BUFFERS[i], 0);
    checkLevel(BUFFERS[i], 1);
  }
}

void checkWiredPair(int inputA, int inputB, int expected) {
  driveAllHigh();
  digitalWrite(BUFFERS[0].pinA, inputA ? HIGH : LOW);
  digitalWrite(BUFFERS[1].pinA, inputB ? HIGH : LOW);
  delay(1);
  const int actual = digitalRead(BUFFERS[0].pinY) == HIGH ? 1 : 0;
  Serial.print("wired A=");
  Serial.print(inputA);
  Serial.print(" B=");
  Serial.print(inputB);
  Serial.print(" expected=");
  Serial.print(expected);
  Serial.print(" actual=");
  Serial.print(actual);
  Serial.println(expected == actual ? " PASS" : " FAIL");
  if (expected != actual) {
    wiredFailed = true;
    noteFailure("wired", inputA, expected, actual);
  }
}

void checkWiredAnd() {
  wiredFailed = false;
  checkWiredPair(0, 0, 0);
  checkWiredPair(1, 0, 0);
  checkWiredPair(0, 1, 0);
  checkWiredPair(1, 1, 1);
  Serial.println(wiredFailed ? "wired-AND FAIL" : "wired-AND PASS");
}

void runTest() {
  failed = false;
  resultLine[0] = '\0';
  Serial.println(WIRED_AND ? "74HC07 test, wired-AND on" : "74HC07 test");
  checkBuffers(WIRED_AND ? 2 : 0);
  if (WIRED_AND) checkWiredAnd();
  driveAllHigh();
  Serial.println(failed ? resultLine : "RESULT PASS");
}

void setup() {
  Serial.begin(115200);
  for (uint8_t i = 0; i < BUFFER_COUNT; i++) {
    pinMode(BUFFERS[i].pinA, OUTPUT);
    pinMode(BUFFERS[i].pinY, INPUT);
    digitalWrite(BUFFERS[i].pinA, HIGH);
  }
  Serial.println("74HC07 ready. Send any character to start.");
}

void loop() {
  if (Serial.available() == 0) return;
  while (Serial.available() > 0) Serial.read();
  runTest();
}
