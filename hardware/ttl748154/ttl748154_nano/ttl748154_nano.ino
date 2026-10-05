/*
 * Временная проверка SN74LV8154 на Arduino Nano.
 * Каталог hardware/ttl748154 удаляется перед merge request.
 *
 * Serial 115200. После сброса скетч ждёт любой символ.
 * Успех: последняя строка RESULT PASS.
 * Первое расхождение: RESULT FAIL, эта строка не затирается.
 */

static const uint8_t PIN_CCLR = 2;
static const uint8_t PIN_CLKA = 3;
static const uint8_t PIN_CLKB = 4;
static const uint8_t PIN_RCLK = 5;
static const uint8_t PIN_CLKBEN = 6;
static const uint8_t PIN_GAL = 7;
static const uint8_t PIN_GAU = 8;
static const uint8_t PIN_GBL = 9;
static const uint8_t PIN_GBU = 10;
static const uint8_t PIN_RCOA = 11;

static const uint8_t Y_PINS[8] = {12, 13, A0, A1, A2, A3, A4, A5};

static const uint8_t GATE_A_LOW = 0;
static const uint8_t GATE_A_HIGH = 1;
static const uint8_t GATE_B_LOW = 2;
static const uint8_t GATE_B_HIGH = 3;

static bool failed = false;

void expect(bool ok, const __FlashStringHelper* message) {
  if (ok || failed) {
    return;
  }
  Serial.print(F("RESULT FAIL "));
  Serial.println(message);
  failed = true;
}

void yMode(uint8_t mode) {
  for (uint8_t bit = 0; bit < 8; bit++) {
    pinMode(Y_PINS[bit], mode);
  }
}

void idle() {
  // D0 и D1 остаются за Serial. CCLR и такты низкие, CLKBEN и GAL высокие.
  PORTD = (PORTD & 0x03) | _BV(6) | _BV(7);
  PORTB |= _BV(0) | _BV(1) | _BV(2);
}

void gatesHigh() {
  PORTD |= _BV(7);
  PORTB |= _BV(0) | _BV(1) | _BV(2);
}

void selectGate(uint8_t gate) {
  gatesHigh();
  switch (gate) {
    case GATE_A_LOW:
      PORTD &= ~_BV(7);
      break;
    case GATE_A_HIGH:
      PORTB &= ~_BV(0);
      break;
    case GATE_B_LOW:
      PORTB &= ~_BV(1);
      break;
    case GATE_B_HIGH:
      PORTB &= ~_BV(2);
      break;
    default:
      break;
  }
  delayMicroseconds(5);
}

uint8_t readY() {
  uint8_t value = 0;
  for (uint8_t bit = 0; bit < 8; bit++) {
    if (digitalRead(Y_PINS[bit]) == HIGH) {
      value |= (uint8_t)(1U << bit);
    }
  }
  return value;
}

uint8_t readByte(uint8_t gate) {
  selectGate(gate);
  const uint8_t value = readY();
  gatesHigh();
  return value;
}

uint16_t readCounter(bool counterA) {
  const uint8_t lowGate = counterA ? GATE_A_LOW : GATE_B_LOW;
  const uint8_t highGate = counterA ? GATE_A_HIGH : GATE_B_HIGH;
  return (uint16_t)readByte(lowGate) | ((uint16_t)readByte(highGate) << 8);
}

void pulsePortD(uint8_t bit, uint32_t count) {
  const uint8_t mask = _BV(bit);
  for (uint32_t i = 0; i < count; i++) {
    PORTD |= mask;
    PORTD &= (uint8_t)~mask;
  }
}

void latch() {
  PORTD |= _BV(5);
  delayMicroseconds(2);
  PORTD &= ~_BV(5);
  delayMicroseconds(2);
}

void clearCounters() {
  PORTD &= ~_BV(2);
  delayMicroseconds(2);
  PORTD |= _BV(2);
  delayMicroseconds(2);
}

void pulseCascade() {
  if ((PINB & _BV(3)) == 0) {
    PORTD &= ~_BV(6);
  } else {
    PORTD |= _BV(6);
  }
  PORTD |= _BV(3) | _BV(4);
  PORTD &= (uint8_t)~(_BV(3) | _BV(4));
}

void testClearLeavesStorage() {
  clearCounters();
  latch();
  expect(readCounter(true) == 0, F("A is not 0 after clear"));
  expect(readCounter(false) == 0, F("B is not 0 after clear"));
  expect(digitalRead(PIN_RCOA) == HIGH, F("RCOA is low at count 0"));
  if (failed) {
    return;
  }

  pulsePortD(PIN_CLKA, 8);
  expect(readCounter(true) == 0, F("A storage changed before RCLK"));
  latch();
  expect(readCounter(true) == 8, F("A is not 8 after latch"));
  expect(readCounter(false) == 0, F("B changed with CLKA"));
  if (failed) {
    return;
  }

  PORTD &= ~_BV(2);
  delayMicroseconds(2);
  expect(readCounter(true) == 8, F("CCLR cleared the storage register"));
  expect(digitalRead(PIN_RCOA) == HIGH, F("RCOA stayed low during clear"));
  PORTD |= _BV(2);
  delayMicroseconds(2);
  expect(readCounter(true) == 8, F("storage changed when CCLR was released"));
  latch();
  expect(readCounter(true) == 0, F("A was not stored as 0 after clear"));
  expect(readCounter(false) == 0, F("B was not stored as 0 after clear"));
}

void testCounterBEnable() {
  clearCounters();
  latch();
  PORTD |= _BV(6);
  pulsePortD(PIN_CLKB, 4);
  latch();
  expect(readCounter(false) == 0, F("B counted while CLKBEN was high"));
  if (failed) {
    return;
  }

  PORTD &= ~_BV(6);
  pulsePortD(PIN_CLKB, 4);
  PORTD |= _BV(6);
  latch();
  expect(readCounter(false) == 4, F("B did not count while CLKBEN was low"));
  expect(readCounter(true) == 0, F("A changed with CLKB"));
}

void testCounterACarry() {
  clearCounters();
  latch();
  pulsePortD(PIN_CLKA, 65535UL);
  expect(digitalRead(PIN_RCOA) == LOW, F("RCOA is high at FFFF"));
  latch();
  expect(readCounter(true) == 0xFFFF, F("A is not FFFF"));
  expect(readCounter(false) == 0, F("B changed while filling A"));
  if (failed) {
    return;
  }

  pulsePortD(PIN_CLKA, 1);
  expect(digitalRead(PIN_RCOA) == HIGH, F("RCOA stayed low after A wrapped"));
  latch();
  expect(readCounter(true) == 0, F("A did not wrap to 0"));
}

void testCascade() {
  clearCounters();
  latch();
  PORTD |= _BV(6);
  for (uint32_t i = 0; i < 65535UL; i++) {
    pulseCascade();
  }
  expect(digitalRead(PIN_RCOA) == LOW, F("RCOA is high after 65535 cascade clocks"));
  latch();
  expect(readCounter(true) == 0xFFFF, F("cascade A is not FFFF"));
  expect(readCounter(false) == 0, F("B counted before A was full"));
  if (failed) {
    return;
  }

  pulseCascade();
  expect(digitalRead(PIN_RCOA) == HIGH, F("RCOA stayed low on the cascade wrap"));
  latch();
  expect(readCounter(true) == 0, F("cascade A did not wrap"));
  expect(readCounter(false) == 1, F("cascade B did not become 1"));
  PORTD |= _BV(6);
}

void testHighZ() {
  clearCounters();
  latch();
  selectGate(GATE_A_LOW);
  expect(readY() == 0x00, F("driven low byte is not 0"));
  gatesHigh();
  yMode(INPUT_PULLUP);
  delay(1);
  expect(readY() == 0xFF, F("released Y bus did not float high"));
  yMode(INPUT);
}

void reportOverlap() {
  clearCounters();
  pulsePortD(PIN_CLKA, 255);
  latch();
  PORTD &= ~_BV(7);
  PORTB = (PORTB | _BV(1) | _BV(2)) & ~_BV(0);
  delayMicroseconds(5);
  const uint8_t observed = readY();
  gatesHigh();
  Serial.print(F("MULTI GAL+GAU observed=0x"));
  if (observed < 0x10) {
    Serial.print('0');
  }
  Serial.println(observed, HEX);
}

void setup() {
  Serial.begin(115200);
  idle();
  pinMode(PIN_CCLR, OUTPUT);
  pinMode(PIN_CLKA, OUTPUT);
  pinMode(PIN_CLKB, OUTPUT);
  pinMode(PIN_RCLK, OUTPUT);
  pinMode(PIN_CLKBEN, OUTPUT);
  pinMode(PIN_GAL, OUTPUT);
  pinMode(PIN_GAU, OUTPUT);
  pinMode(PIN_GBL, OUTPUT);
  pinMode(PIN_GBU, OUTPUT);
  pinMode(PIN_RCOA, INPUT);
  yMode(INPUT);
  idle();

  Serial.println(F("748154 ready, send any byte"));
  while (Serial.available() == 0) {
  }
  while (Serial.available() > 0) {
    Serial.read();
  }

  testClearLeavesStorage();
  if (!failed) {
    testCounterBEnable();
  }
  if (!failed) {
    testCounterACarry();
  }
  if (!failed) {
    testCascade();
  }
  if (!failed) {
    testHighZ();
  }
  if (!failed) {
    reportOverlap();
    Serial.println(F("RESULT PASS"));
  }
}

void loop() {}
