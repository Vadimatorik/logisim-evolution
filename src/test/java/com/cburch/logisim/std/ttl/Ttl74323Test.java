/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.std.ttl.TtlTestInstanceState.createInstance;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC323 8-bit universal shift register. */
class Ttl74323Test {
  private static final int GND_PORT = 18;
  private static final int VCC_PORT = 19;
  private static final byte[] IO = {
    Ttl74323.IO0,
    Ttl74323.IO1,
    Ttl74323.IO2,
    Ttl74323.IO3,
    Ttl74323.IO4,
    Ttl74323.IO5,
    Ttl74323.IO6,
    Ttl74323.IO7
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74323();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(18, hiddenPower.getPorts().size());
    assertPort(hiddenPower, port(Ttl74323.S0), 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, port(Ttl74323.nOE1), 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, port(Ttl74323.nOE2), 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, port(Ttl74323.IO6), 70, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, port(Ttl74323.IO4), 90, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, port(Ttl74323.IO2), 110, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, port(Ttl74323.IO0), 130, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, port(Ttl74323.Q0), 150, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, port(Ttl74323.nCLR), 170, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, port(Ttl74323.SR), 190, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, port(Ttl74323.CP), 170, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, port(Ttl74323.IO1), 150, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, port(Ttl74323.IO3), 130, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, port(Ttl74323.IO5), 110, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, port(Ttl74323.IO7), 90, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, port(Ttl74323.Q7), 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, port(Ttl74323.SL), 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, port(Ttl74323.S1), 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(20, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 190, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void synchronousClearWaitsForTheClockAndOverridesLoad() {
    final var gate = new Ttl74323();
    final var state = new TtlTestInstanceState(gate, false);
    load(gate, state, 0x81);
    assertRegister(state, "10000001");

    state.setPortValue(port(Ttl74323.nCLR), Value.FALSE);
    gate.propagate(state);
    assertRegister(state, "10000001");

    clock(gate, state);
    assertRegister(state, "00000000");

    load(gate, state, 0x81);
    state.setPortValue(port(Ttl74323.nCLR), Value.FALSE);
    state.setPortValue(port(Ttl74323.S1), Value.TRUE);
    state.setPortValue(port(Ttl74323.S0), Value.TRUE);
    driveBus(state, 0xFF);
    gate.propagate(state);
    assertSerial(state, "10000001");
    assertBus(state, "UUUUUUUU");

    driveBus(state, 0xFF);
    clock(gate, state);
    assertSerial(state, "00000000");
    assertBus(state, "UUUUUUUU");
    present(gate, state);
    assertRegister(state, "00000000");
  }

  @Test
  void parallelLoadStoresTheBus() {
    final var gate = new Ttl74323();
    final var state = new TtlTestInstanceState(gate, false);

    load(gate, state, 0x00);
    assertRegister(state, "00000000");
    load(gate, state, 0xFF);
    assertRegister(state, "11111111");
    load(gate, state, 0xA5);
    assertRegister(state, "10100101");
    load(gate, state, 0x5A);
    assertRegister(state, "01011010");
  }

  @Test
  void shiftsWalkABitAndHoldIgnoresSerialInputs() {
    final var gate = new Ttl74323();
    final var state = new TtlTestInstanceState(gate, false);

    load(gate, state, 0x01);
    state.setPortValue(port(Ttl74323.SR), Value.FALSE);
    select(state, false, true);
    for (var bit = 0; bit < 8; bit++) {
      assertRegister(state, toBinary(1 << bit));
      clock(gate, state);
    }
    assertRegister(state, "00000000");

    load(gate, state, 0x80);
    state.setPortValue(port(Ttl74323.SL), Value.FALSE);
    select(state, true, false);
    for (var bit = 7; bit >= 0; bit--) {
      assertRegister(state, toBinary(1 << bit));
      clock(gate, state);
    }
    assertRegister(state, "00000000");

    load(gate, state, 0x01);
    state.setPortValue(port(Ttl74323.SL), Value.TRUE);
    select(state, true, false);
    clock(gate, state);
    assertRegister(state, "10000000");

    load(gate, state, 0x80);
    state.setPortValue(port(Ttl74323.SR), Value.TRUE);
    select(state, false, true);
    clock(gate, state);
    assertRegister(state, "00000001");

    load(gate, state, 0xA5);
    state.setPortValue(port(Ttl74323.SR), Value.TRUE);
    state.setPortValue(port(Ttl74323.SL), Value.TRUE);
    select(state, false, false);
    clock(gate, state);
    clock(gate, state);
    clock(gate, state);
    assertRegister(state, "10100101");
  }

  @Test
  void outputEnableReleasesTheBusButSerialOutputsStay() {
    final var gate = new Ttl74323();
    final var state = new TtlTestInstanceState(gate, false);
    load(gate, state, 0x01);

    state.setPortValue(port(Ttl74323.nOE1), Value.TRUE);
    gate.propagate(state);
    assertSerial(state, "00000001");
    assertBus(state, "UUUUUUUU");

    state.setPortValue(port(Ttl74323.nOE1), Value.FALSE);
    state.setPortValue(port(Ttl74323.nOE2), Value.TRUE);
    gate.propagate(state);
    assertSerial(state, "00000001");
    assertBus(state, "UUUUUUUU");

    state.setPortValue(port(Ttl74323.nOE2), Value.FALSE);
    state.setPortValue(port(Ttl74323.S1), Value.TRUE);
    state.setPortValue(port(Ttl74323.S0), Value.TRUE);
    gate.propagate(state);
    assertSerial(state, "00000001");
    assertBus(state, "UUUUUUUU");

    present(gate, state);
    assertRegister(state, "00000001");
  }

  @Test
  void unknownControlsAffectOnlyDisagreements() {
    final var gate = new Ttl74323();
    final var state = new TtlTestInstanceState(gate, false);
    load(gate, state, 0x81);

    state.setPortValue(port(Ttl74323.S0), Value.UNKNOWN);
    state.setPortValue(port(Ttl74323.S1), Value.FALSE);
    state.setPortValue(port(Ttl74323.SR), Value.FALSE);
    clock(gate, state);
    assertRegister(state, "U00000UU");

    load(gate, state, 0x00);
    state.setPortValue(port(Ttl74323.nCLR), Value.UNKNOWN);
    select(state, false, false);
    clock(gate, state);
    assertRegister(state, "00000000");

    load(gate, state, 0x81);
    state.setPortValue(port(Ttl74323.nCLR), Value.UNKNOWN);
    select(state, false, false);
    clock(gate, state);
    assertRegister(state, "U000000U");

    load(gate, state, 0x01);
    state.setPortValue(port(Ttl74323.CP), Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(port(Ttl74323.CP), Value.FALSE);
    gate.propagate(state);
    select(state, false, true);
    state.setPortValue(port(Ttl74323.SR), Value.FALSE);
    state.setPortValue(port(Ttl74323.CP), Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(port(Ttl74323.CP), Value.TRUE);
    gate.propagate(state);
    assertRegister(state, "00000001");
    state.setPortValue(port(Ttl74323.CP), Value.FALSE);
    gate.propagate(state);
    clock(gate, state);
    assertRegister(state, "00000010");

    load(gate, state, 0x00);
    state.setPortValue(port(Ttl74323.SR), Value.ERROR);
    select(state, false, true);
    clock(gate, state);
    assertRegister(state, "0000000E");

    load(gate, state, 0x81);
    state.setPortValue(port(Ttl74323.S0), Value.ERROR);
    state.setPortValue(port(Ttl74323.S1), Value.FALSE);
    state.setPortValue(port(Ttl74323.SR), Value.FALSE);
    clock(gate, state);
    assertRegister(state, "E00000EE");

    load(gate, state, 0x01);
    state.setPortValue(port(Ttl74323.nOE1), Value.ERROR);
    gate.propagate(state);
    assertSerial(state, "00000001");
    assertBus(state, "EEEEEEEE");

    state.setPortValue(port(Ttl74323.nOE1), Value.UNKNOWN);
    gate.propagate(state);
    assertSerial(state, "00000001");
    assertBus(state, "UUUUUUUU");
  }

  @Test
  void invalidExposedPowerMakesSerialOutputsUnknown() {
    final var gate = new Ttl74323();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    load(gate, state, 0x81);
    assertRegister(state, "10000001");

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(port(Ttl74323.Q0)));
    assertEquals(Value.UNKNOWN, state.getPortValue(port(Ttl74323.Q7)));

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertRegister(state, "10000001");

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(port(Ttl74323.Q0)));
    assertEquals(Value.UNKNOWN, state.getPortValue(port(Ttl74323.Q7)));
  }

  @Test
  void clockMetadataNamesTheRisingEdgePin() {
    final var gate = new Ttl74323();
    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(new int[] {port(Ttl74323.CP)}, gate.clockPinIndex(null));
  }

  private static int port(byte pin) {
    return Ttl74323.pinNrToPortNr(pin);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  /** Leaves CP low, both enables low, and the mode inputs at hold. */
  private static void present(Ttl74323 gate, TtlTestInstanceState state) {
    state.setPortValue(port(Ttl74323.nCLR), Value.TRUE);
    state.setPortValue(port(Ttl74323.nOE1), Value.FALSE);
    state.setPortValue(port(Ttl74323.nOE2), Value.FALSE);
    state.setPortValue(port(Ttl74323.CP), Value.FALSE);
    select(state, false, false);
    gate.propagate(state);
  }

  private static void select(TtlTestInstanceState state, boolean s1, boolean s0) {
    state.setPortValue(port(Ttl74323.S1), s1 ? Value.TRUE : Value.FALSE);
    state.setPortValue(port(Ttl74323.S0), s0 ? Value.TRUE : Value.FALSE);
  }

  private static void load(Ttl74323 gate, TtlTestInstanceState state, int data) {
    state.setPortValue(port(Ttl74323.CP), Value.FALSE);
    state.setPortValue(port(Ttl74323.nCLR), Value.TRUE);
    state.setPortValue(port(Ttl74323.nOE1), Value.FALSE);
    state.setPortValue(port(Ttl74323.nOE2), Value.FALSE);
    select(state, true, true);
    driveBus(state, data);
    gate.propagate(state);
    driveBus(state, data);
    clock(gate, state);
    present(gate, state);
  }

  private static void clock(Ttl74323 gate, TtlTestInstanceState state) {
    state.setPortValue(port(Ttl74323.CP), Value.TRUE);
    gate.propagate(state);
    state.setPortValue(port(Ttl74323.CP), Value.FALSE);
    gate.propagate(state);
  }

  private static void driveBus(TtlTestInstanceState state, int data) {
    for (var bit = 0; bit < 8; bit++) {
      final var pin = IO[bit];
      state.setPortValue(port(pin), ((data >> bit) & 1) == 0 ? Value.FALSE : Value.TRUE);
    }
  }

  /** {@code pattern} is {@code Q7} down to {@code Q0}. {@code U} and {@code E} are unknown and error. */
  private static void assertRegister(TtlTestInstanceState state, String pattern) {
    assertSerial(state, pattern);
    assertBus(state, pattern);
  }

  private static void assertSerial(TtlTestInstanceState state, String pattern) {
    assertEquals(bit(pattern, 0), state.getPortValue(port(Ttl74323.Q0)), pattern);
    assertEquals(bit(pattern, 7), state.getPortValue(port(Ttl74323.Q7)), pattern);
  }

  private static void assertBus(TtlTestInstanceState state, String pattern) {
    for (var index = 0; index < 8; index++) {
      assertEquals(bit(pattern, index), state.getPortValue(port(IO[index])), pattern);
    }
  }

  /** Bit 0 of the pattern string's right-hand end is {@code Q0}. */
  private static Value bit(String q7ToQ0, int bit) {
    return switch (q7ToQ0.charAt(7 - bit)) {
      case '1' -> Value.TRUE;
      case '0' -> Value.FALSE;
      case 'E' -> Value.ERROR;
      default -> Value.UNKNOWN;
    };
  }

  /** Eight characters, {@code Q7} at the left. */
  private static String toBinary(int data) {
    return String.format("%8s", Integer.toBinaryString(data & 0xFF)).replace(' ', '0');
  }
}
