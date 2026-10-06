/*
 * Self-check for a 74HC107 wired to an Arduino Nano as described in ../README.md.
 * Open Serial Monitor at 115200 baud and send any character to start.
 * The last line is "RESULT PASS" or "RESULT FAIL ...".
 *
 * nR low forces Q low and nQ high. A falling CP applies J and K.
 * A rising CP does not. The two halves do not share a clock.
 */

const uint8_t PIN_1J = 2;
const uint8_t PIN_1K = 3;
const uint8_t PIN_1CP = 4;
const uint8_t PIN_1R = 5;
const uint8_t PIN_1Q = 6;
const uint8_t PIN_N1Q = 7;
const uint8_t PIN_2J = 8;
const uint8_t PIN_2K = 9;
const uint8_t PIN_2CP = 10;
const uint8_t PIN_2R = 11;
const uint8_t PIN_2Q = 12;
const uint8_t PIN_N2Q = 13;

bool failed = false;
char resultLine[96];

void noteFailure(const char* step) {
  if (failed) return;
  failed = true;
  snprintf(resultLine, sizeof(resultLine), "RESULT FAIL %s", step);
}

void drive(uint8_t pinJ, uint8_t pinK, uint8_t pinCp, uint8_t pinR, bool j, bool k, bool reset) {
  digitalWrite(pinJ, j ? HIGH : LOW);
  digitalWrite(pinK, k ? HIGH : LOW);
  digitalWrite(pinCp, LOW);
  digitalWrite(pinR, reset ? LOW : HIGH);
}

void idle() {
  drive(PIN_1J, PIN_1K, PIN_1CP, PIN_1R, false, false, true);
  drive(PIN_2J, PIN_2K, PIN_2CP, PIN_2R, false, false, true);
}

void expectHalf(const char* step, uint8_t pinQ, uint8_t pinNq, bool qHigh) {
  delay(1);
  const bool q = digitalRead(pinQ) == HIGH;
  const bool nq = digitalRead(pinNq) == HIGH;
  Serial.print(step);
  Serial.print(" Q=");
  Serial.print(q ? "1" : "0");
  Serial.print(" nQ=");
  Serial.println(nq ? "1" : "0");
  if (q != qHigh || nq == qHigh) noteFailure(step);
}

void rise(uint8_t pinCp) {
  digitalWrite(pinCp, HIGH);
  delayMicroseconds(2);
}

void fall(uint8_t pinCp) {
  digitalWrite(pinCp, LOW);
  delayMicroseconds(2);
}

// Rising edge must not transfer. The following fall applies j and k.
void fallClock(uint8_t pinJ, uint8_t pinK, uint8_t pinCp, bool j, bool k) {
  digitalWrite(pinJ, j ? HIGH : LOW);
  digitalWrite(pinK, k ? HIGH : LOW);
  delayMicroseconds(2);
  rise(pinCp);
  fall(pinCp);
}

void checkHalf(
    const char* name,
    uint8_t pinJ,
    uint8_t pinK,
    uint8_t pinCp,
    uint8_t pinR,
    uint8_t pinQ,
    uint8_t pinNq,
    uint8_t otherQ,
    uint8_t otherNq) {
  char step[48];

  digitalWrite(pinR, HIGH);
  digitalWrite(pinJ, HIGH);
  digitalWrite(pinK, LOW);
  rise(pinCp);
  snprintf(step, sizeof(step), "%s rising set", name);
  expectHalf(step, pinQ, pinNq, false);

  fall(pinCp);
  snprintf(step, sizeof(step), "%s set", name);
  expectHalf(step, pinQ, pinNq, true);
  snprintf(step, sizeof(step), "%s other holds after set", name);
  expectHalf(step, otherQ, otherNq, false);

  digitalWrite(pinJ, LOW);
  digitalWrite(pinK, HIGH);
  delayMicroseconds(2);
  snprintf(step, sizeof(step), "%s j/k without clock", name);
  expectHalf(step, pinQ, pinNq, true);

  rise(pinCp);
  snprintf(step, sizeof(step), "%s rising clear", name);
  expectHalf(step, pinQ, pinNq, true);
  fall(pinCp);
  snprintf(step, sizeof(step), "%s clear", name);
  expectHalf(step, pinQ, pinNq, false);

  fallClock(pinJ, pinK, pinCp, true, true);
  snprintf(step, sizeof(step), "%s toggle 1", name);
  expectHalf(step, pinQ, pinNq, true);
  fallClock(pinJ, pinK, pinCp, true, true);
  snprintf(step, sizeof(step), "%s toggle 0", name);
  expectHalf(step, pinQ, pinNq, false);
  snprintf(step, sizeof(step), "%s other holds after toggle", name);
  expectHalf(step, otherQ, otherNq, false);

  digitalWrite(pinJ, LOW);
  digitalWrite(pinK, LOW);
  digitalWrite(pinR, LOW);
  delayMicroseconds(2);
  snprintf(step, sizeof(step), "%s async reset", name);
  expectHalf(step, pinQ, pinNq, false);

  digitalWrite(pinJ, HIGH);
  rise(pinCp);
  fall(pinCp);
  snprintf(step, sizeof(step), "%s clock while reset", name);
  expectHalf(step, pinQ, pinNq, false);

  digitalWrite(pinR, HIGH);
  delayMicroseconds(2);
  snprintf(step, sizeof(step), "%s release reset", name);
  expectHalf(step, pinQ, pinNq, false);
  digitalWrite(pinJ, LOW);
  digitalWrite(pinCp, LOW);
}

void runChecks() {
  idle();
  delay(1);
  expectHalf("reset 1", PIN_1Q, PIN_N1Q, false);
  expectHalf("reset 2", PIN_2Q, PIN_N2Q, false);

  digitalWrite(PIN_1R, HIGH);
  digitalWrite(PIN_2R, HIGH);
  digitalWrite(PIN_1J, HIGH);
  digitalWrite(PIN_2J, HIGH);
  delay(1);
  expectHalf("release 1", PIN_1Q, PIN_N1Q, false);
  expectHalf("release 2", PIN_2Q, PIN_N2Q, false);
  digitalWrite(PIN_1J, LOW);
  digitalWrite(PIN_2J, LOW);

  checkHalf("ff1", PIN_1J, PIN_1K, PIN_1CP, PIN_1R, PIN_1Q, PIN_N1Q, PIN_2Q, PIN_N2Q);
  checkHalf("ff2", PIN_2J, PIN_2K, PIN_2CP, PIN_2R, PIN_2Q, PIN_N2Q, PIN_1Q, PIN_N1Q);
}

void setup() {
  Serial.begin(115200);
  const uint8_t outputs[] = {
      PIN_1J, PIN_1K, PIN_1CP, PIN_1R, PIN_2J, PIN_2K, PIN_2CP, PIN_2R};
  for (uint8_t index = 0; index < sizeof(outputs); index++) pinMode(outputs[index], OUTPUT);
  pinMode(PIN_1Q, INPUT);
  pinMode(PIN_N1Q, INPUT);
  pinMode(PIN_2Q, INPUT);
  pinMode(PIN_N2Q, INPUT);
  idle();
  Serial.println("74HC107 bench. Send any character to start.");
}

void loop() {
  if (!Serial.available()) return;
  while (Serial.available()) Serial.read();
  failed = false;
  runChecks();
  Serial.println(failed ? resultLine : "RESULT PASS");
}
