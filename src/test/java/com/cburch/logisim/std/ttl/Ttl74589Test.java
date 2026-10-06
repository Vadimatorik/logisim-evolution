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

/** Functional tests for the 74HC589 shift register with an input latch. */
class Ttl74589Test {
  private static final byte[] PARALLEL = {
    Ttl74589.A, Ttl74589.B, Ttl74589.C, Ttl74589.D,
    Ttl74589.E, Ttl74589.F, Ttl74589.G, Ttl74589.H
  };
  /** Bits of 0xA5 in shift-out order after H: G, F, E, D, C, B, A. */
  private static final boolean[] AFTER_H_OF_0XA5 = {false, true, false, false, true, false, true};
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74589();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74589.B, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74589.C, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74589.D, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74589.E, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74589.F, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74589.G, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74589.H, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74589.QH, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74589.OE, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74589.SCK, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74589.RCK, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74589.SLOAD, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74589.SA, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74589.A, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void shiftAndLatchClocksAreEdgeClocks() {
    final var gate = new Ttl74589();

    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(
        new int[] {Ttl74589.pinNrToPortNr(Ttl74589.SCK), Ttl74589.pinNrToPortNr(Ttl74589.RCK)},
        gate.clockPinIndex(null));
  }

  @Test
  void latchClockDoesNotEnterTheShiftRegister() {
    final var gate = new Ttl74589();
    final var state = fresh(gate);
    load(gate, state, 0x80);

    setParallel(state, 0x00);
    pulse(gate, state, Ttl74589.RCK);

    assertQh(state, Value.TRUE);
  }

  @Test
  void lowShiftLoadCopiesTheLatchWithoutAShiftClock() {
    final var gate = new Ttl74589();
    final var state = fresh(gate);
    load(gate, state, 0x80);
    setParallel(state, 0x00);
    pulse(gate, state, Ttl74589.RCK);

    set(state, Ttl74589.SLOAD, Value.FALSE);
    gate.propagate(state);

    assertQh(state, Value.FALSE);
  }

  @Test
  void shiftClockIsIgnoredWhileParallelLoadIsActive() {
    final var gate = new Ttl74589();
    final var state = fresh(gate);
    load(gate, state, 0x01);
    set(state, Ttl74589.SLOAD, Value.FALSE);
    set(state, Ttl74589.SA, Value.TRUE);
    gate.propagate(state);

    for (var clock = 0; clock < 8; clock++) {
      pulse(gate, state, Ttl74589.SCK);
      assertQh(state, Value.FALSE);
    }

    set(state, Ttl74589.SLOAD, Value.TRUE);
    set(state, Ttl74589.SA, Value.FALSE);
    gate.propagate(state);
    for (var clock = 0; clock < 6; clock++) {
      pulse(gate, state, Ttl74589.SCK);
      assertQh(state, Value.FALSE);
    }
    pulse(gate, state, Ttl74589.SCK);
    assertQh(state, Value.TRUE);
  }

  @Test
  void serialDataShiftsOutFromHTowardA() {
    final var gate = new Ttl74589();
    final var state = fresh(gate);
    load(gate, state, 0xA5);
    assertQh(state, Value.TRUE);

    set(state, Ttl74589.SA, Value.FALSE);
    for (final var high : AFTER_H_OF_0XA5) {
      pulse(gate, state, Ttl74589.SCK);
      assertQh(state, high ? Value.TRUE : Value.FALSE);
    }
    pulse(gate, state, Ttl74589.SCK);
    assertQh(state, Value.FALSE);

    set(state, Ttl74589.SA, Value.TRUE);
    gate.propagate(state);
    for (var clock = 0; clock < 7; clock++) {
      pulse(gate, state, Ttl74589.SCK);
      assertQh(state, Value.FALSE);
    }
    pulse(gate, state, Ttl74589.SCK);
    assertQh(state, Value.TRUE);
  }

  @Test
  void highShiftClockDoesNotShiftAgain() {
    final var gate = new Ttl74589();
    final var state = fresh(gate);
    load(gate, state, 0xA5);

    set(state, Ttl74589.SCK, Value.TRUE);
    gate.propagate(state);
    assertQh(state, Value.FALSE);
    gate.propagate(state);
    assertQh(state, Value.FALSE);

    set(state, Ttl74589.SCK, Value.FALSE);
    gate.propagate(state);
    assertQh(state, Value.FALSE);
  }

  @Test
  void simultaneousClocksCaptureParallelDataAndShiftSerialData() {
    final var gate = new Ttl74589();
    final var state = fresh(gate);
    load(gate, state, 0x00);
    setParallel(state, 0xA5);
    set(state, Ttl74589.SA, Value.TRUE);
    set(state, Ttl74589.SCK, Value.FALSE);
    set(state, Ttl74589.RCK, Value.FALSE);
    gate.propagate(state);

    set(state, Ttl74589.SCK, Value.TRUE);
    set(state, Ttl74589.RCK, Value.TRUE);
    gate.propagate(state);
    assertQh(state, Value.FALSE);

    set(state, Ttl74589.SCK, Value.FALSE);
    set(state, Ttl74589.RCK, Value.FALSE);
    set(state, Ttl74589.SLOAD, Value.FALSE);
    gate.propagate(state);
    assertQh(state, Value.TRUE);

    set(state, Ttl74589.SLOAD, Value.TRUE);
    set(state, Ttl74589.SA, Value.FALSE);
    gate.propagate(state);
    for (final var high : AFTER_H_OF_0XA5) {
      pulse(gate, state, Ttl74589.SCK);
      assertQh(state, high ? Value.TRUE : Value.FALSE);
    }
  }

  @Test
  void outputEnableHidesQHWhileTheRegisterKeepsShifting() {
    final var gate = new Ttl74589();
    final var state = fresh(gate);
    load(gate, state, 0x80);

    set(state, Ttl74589.OE, Value.TRUE);
    gate.propagate(state);
    assertQh(state, Value.UNKNOWN);

    set(state, Ttl74589.SA, Value.FALSE);
    pulse(gate, state, Ttl74589.SCK);
    assertQh(state, Value.UNKNOWN);

    set(state, Ttl74589.OE, Value.FALSE);
    gate.propagate(state);
    assertQh(state, Value.FALSE);
  }

  @Test
  void unknownOutputEnableReleasesQH() {
    final var gate = new Ttl74589();
    final var state = fresh(gate);
    load(gate, state, 0x80);

    set(state, Ttl74589.OE, Value.UNKNOWN);
    gate.propagate(state);
    assertQh(state, Value.UNKNOWN);
    set(state, Ttl74589.OE, Value.ERROR);
    gate.propagate(state);
    assertQh(state, Value.UNKNOWN);

    set(state, Ttl74589.OE, Value.FALSE);
    gate.propagate(state);
    assertQh(state, Value.TRUE);
  }

  @Test
  void unknownShiftLoadDoesNotMoveTheShiftRegister() {
    final var gate = new Ttl74589();
    final var state = fresh(gate);
    load(gate, state, 0x80);

    set(state, Ttl74589.SLOAD, Value.UNKNOWN);
    set(state, Ttl74589.SA, Value.FALSE);
    pulse(gate, state, Ttl74589.SCK);
    assertQh(state, Value.TRUE);

    setParallel(state, 0x00);
    pulse(gate, state, Ttl74589.RCK);
    assertQh(state, Value.TRUE);

    set(state, Ttl74589.SLOAD, Value.ERROR);
    pulse(gate, state, Ttl74589.SCK);
    assertQh(state, Value.TRUE);

    set(state, Ttl74589.SLOAD, Value.FALSE);
    gate.propagate(state);
    assertQh(state, Value.FALSE);
  }

  @Test
  void unknownClocksDoNotCaptureOrShift() {
    final var gate = new Ttl74589();
    final var state = fresh(gate);
    load(gate, state, 0x80);

    set(state, Ttl74589.SCK, Value.UNKNOWN);
    gate.propagate(state);
    set(state, Ttl74589.SCK, Value.TRUE);
    gate.propagate(state);
    assertQh(state, Value.TRUE);
    set(state, Ttl74589.SCK, Value.FALSE);
    gate.propagate(state);

    setParallel(state, 0x00);
    set(state, Ttl74589.RCK, Value.ERROR);
    gate.propagate(state);
    set(state, Ttl74589.RCK, Value.TRUE);
    gate.propagate(state);
    set(state, Ttl74589.SLOAD, Value.FALSE);
    gate.propagate(state);
    assertQh(state, Value.TRUE);
  }

  @Test
  void unknownParallelDataIsStoredInTheLatch() {
    final var gate = new Ttl74589();
    final var state = fresh(gate);
    load(gate, state, 0x00);

    set(state, Ttl74589.H, Value.UNKNOWN);
    pulse(gate, state, Ttl74589.RCK);
    assertQh(state, Value.FALSE);

    set(state, Ttl74589.SLOAD, Value.FALSE);
    gate.propagate(state);
    assertQh(state, Value.UNKNOWN);
  }

  @Test
  void inputsThatChangeWithoutAClockAreHeld() {
    final var gate = new Ttl74589();
    final var state = fresh(gate);
    load(gate, state, 0xA5);

    setParallel(state, 0x00);
    set(state, Ttl74589.SA, Value.TRUE);
    gate.propagate(state);
    gate.propagate(state);
    assertQh(state, Value.TRUE);

    pulse(gate, state, Ttl74589.SCK);
    assertQh(state, Value.FALSE);
  }

  private static TtlTestInstanceState fresh(Ttl74589 gate) {
    final var state = new TtlTestInstanceState(gate, false);
    set(state, Ttl74589.SCK, Value.FALSE);
    set(state, Ttl74589.RCK, Value.FALSE);
    set(state, Ttl74589.SLOAD, Value.TRUE);
    set(state, Ttl74589.OE, Value.FALSE);
    set(state, Ttl74589.SA, Value.FALSE);
    setParallel(state, 0x00);
    gate.propagate(state);
    return state;
  }

  /** Stores {@code mask} in both registers. Bit 0 is stage A and bit 7 is stage H. */
  private static void load(Ttl74589 gate, TtlTestInstanceState state, int mask) {
    setParallel(state, mask);
    set(state, Ttl74589.SLOAD, Value.FALSE);
    gate.propagate(state);
    pulse(gate, state, Ttl74589.RCK);
    set(state, Ttl74589.SLOAD, Value.TRUE);
    gate.propagate(state);
  }

  private static void pulse(Ttl74589 gate, TtlTestInstanceState state, byte pin) {
    set(state, pin, Value.FALSE);
    gate.propagate(state);
    set(state, pin, Value.TRUE);
    gate.propagate(state);
    set(state, pin, Value.FALSE);
    gate.propagate(state);
  }

  private static void setParallel(TtlTestInstanceState state, int mask) {
    for (var stage = 0; stage < PARALLEL.length; stage++) {
      set(state, PARALLEL[stage], ((mask >> stage) & 1) == 1 ? Value.TRUE : Value.FALSE);
    }
  }

  private static void assertQh(TtlTestInstanceState state, Value expected) {
    assertEquals(expected, state.getPortValue(Ttl74589.pinNrToPortNr(Ttl74589.QH)));
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74589.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void set(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl74589.pinNrToPortNr(pin), value);
  }
}
