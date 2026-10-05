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

/** Functional tests for the 74HC4516 presettable binary up/down counter. */
class Ttl744516Test {
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl744516();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744516.PL, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744516.Q3, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744516.D3, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744516.D0, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744516.CE, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744516.Q0, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744516.TC, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744516.MR, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744516.UPDN, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744516.Q1, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744516.D1, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744516.D2, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744516.Q2, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744516.CP, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void clockIsTheLastLogicalPort() {
    final var gate = new Ttl744516();
    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(new int[] {Ttl744516.pinNrToPortNr(Ttl744516.CP)}, gate.clockPinIndex(null));
  }

  @Test
  void masterResetClearsWithoutAClockAndOverridesLoad() {
    final var gate = new Ttl744516();
    final var state = hold(gate);
    load(gate, state, 12);

    set(state, Ttl744516.PL, Value.TRUE);
    setData(state, 9);
    set(state, Ttl744516.MR, Value.TRUE);
    set(state, Ttl744516.CP, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
    assertBit(state, Ttl744516.TC, Value.TRUE);

    set(state, Ttl744516.UPDN, Value.FALSE);
    set(state, Ttl744516.CE, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 0);
    assertBit(state, Ttl744516.TC, Value.FALSE);

    set(state, Ttl744516.MR, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 9);
  }

  @Test
  void parallelLoadIsTransparentWhilePresetStaysHigh() {
    final var gate = new Ttl744516();
    final var state = hold(gate);

    for (var code = 0; code < 16; code++) {
      load(gate, state, code);
      assertCount(state, code);
    }

    set(state, Ttl744516.PL, Value.TRUE);
    setData(state, 6);
    gate.propagate(state);
    assertCount(state, 6);
    setData(state, 10);
    gate.propagate(state);
    assertCount(state, 10);
    set(state, Ttl744516.PL, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 10);
  }

  @Test
  void countingUpAndDownWrapsAndTerminalCountFollowsTheEnds() {
    final var gate = new Ttl744516();
    final var state = hold(gate);
    load(gate, state, 14);
    enableCount(state, true);
    gate.propagate(state);

    assertCount(state, 14);
    assertBit(state, Ttl744516.TC, Value.TRUE);
    rise(gate, state);
    assertCount(state, 15);
    assertBit(state, Ttl744516.TC, Value.FALSE);
    fall(gate, state);
    assertCount(state, 15);
    rise(gate, state);
    fall(gate, state);
    assertCount(state, 0);
    assertBit(state, Ttl744516.TC, Value.TRUE);

    enableCount(state, false);
    gate.propagate(state);
    assertBit(state, Ttl744516.TC, Value.FALSE);
    rise(gate, state);
    fall(gate, state);
    assertCount(state, 15);
    assertBit(state, Ttl744516.TC, Value.TRUE);
    rise(gate, state);
    fall(gate, state);
    assertCount(state, 14);
    assertBit(state, Ttl744516.TC, Value.TRUE);
  }

  @Test
  void countEnableAndFallingClockHoldAndDirectionChangesTerminalCount() {
    final var gate = new Ttl744516();
    final var state = hold(gate);
    load(gate, state, 15);
    set(state, Ttl744516.CE, Value.TRUE);
    set(state, Ttl744516.UPDN, Value.TRUE);
    gate.propagate(state);
    assertBit(state, Ttl744516.TC, Value.TRUE);
    rise(gate, state);
    fall(gate, state);
    assertCount(state, 15);

    set(state, Ttl744516.CP, Value.TRUE);
    gate.propagate(state);
    set(state, Ttl744516.CE, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 15);
    assertBit(state, Ttl744516.TC, Value.FALSE);
    set(state, Ttl744516.UPDN, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 15);
    assertBit(state, Ttl744516.TC, Value.TRUE);

    set(state, Ttl744516.CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 15);
  }

  @Test
  void releasingResetWhileTheClockIsHighDoesNotCount() {
    final var gate = new Ttl744516();
    final var state = hold(gate);
    load(gate, state, 3);
    enableCount(state, true);
    set(state, Ttl744516.CP, Value.FALSE);
    set(state, Ttl744516.MR, Value.TRUE);
    gate.propagate(state);
    set(state, Ttl744516.CP, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
    set(state, Ttl744516.MR, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 0);
    fall(gate, state);
    rise(gate, state);
    assertCount(state, 1);
  }

  @Test
  void unknownAndFallingClocksDoNotCount() {
    final var gate = new Ttl744516();
    final var state = hold(gate);
    load(gate, state, 4);
    enableCount(state, true);
    set(state, Ttl744516.CP, Value.UNKNOWN);
    gate.propagate(state);
    set(state, Ttl744516.CP, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 4);
    set(state, Ttl744516.CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 4);
  }

  @Test
  void unknownResetAndEnableConflictOnlyOnBitsThatDiffer() {
    final var gate = new Ttl744516();
    final var state = hold(gate);
    load(gate, state, 5);
    set(state, Ttl744516.CE, Value.TRUE);
    set(state, Ttl744516.MR, Value.UNKNOWN);
    gate.propagate(state);
    assertBit(state, Ttl744516.Q0, Value.UNKNOWN);
    assertBit(state, Ttl744516.Q1, Value.FALSE);
    assertBit(state, Ttl744516.Q2, Value.UNKNOWN);
    assertBit(state, Ttl744516.Q3, Value.FALSE);
    assertBit(state, Ttl744516.TC, Value.TRUE);

    load(gate, state, 0);
    enableCount(state, false);
    set(state, Ttl744516.UPDN, Value.UNKNOWN);
    gate.propagate(state);
    assertCount(state, 0);
    assertBit(state, Ttl744516.TC, Value.UNKNOWN);

    load(gate, state, 0);
    enableCount(state, true);
    set(state, Ttl744516.CE, Value.UNKNOWN);
    rise(gate, state);
    assertBit(state, Ttl744516.Q0, Value.UNKNOWN);
    assertBit(state, Ttl744516.Q1, Value.FALSE);
    assertBit(state, Ttl744516.Q2, Value.FALSE);
    assertBit(state, Ttl744516.Q3, Value.FALSE);
  }

  @Test
  void errorOnTheCountEnableIsVisibleOnlyWhenTheNextCodeDiffers() {
    final var gate = new Ttl744516();
    final var state = hold(gate);
    load(gate, state, 0);
    enableCount(state, true);
    set(state, Ttl744516.CE, Value.ERROR);
    rise(gate, state);
    assertBit(state, Ttl744516.Q0, Value.ERROR);
    assertBit(state, Ttl744516.Q1, Value.FALSE);
    assertBit(state, Ttl744516.Q2, Value.FALSE);
    assertBit(state, Ttl744516.Q3, Value.FALSE);

    load(gate, state, 15);
    set(state, Ttl744516.UPDN, Value.TRUE);
    set(state, Ttl744516.CE, Value.ERROR);
    gate.propagate(state);
    assertCount(state, 15);
    assertBit(state, Ttl744516.TC, Value.ERROR);

    load(gate, state, 3);
    set(state, Ttl744516.CE, Value.UNKNOWN);
    gate.propagate(state);
    assertBit(state, Ttl744516.TC, Value.TRUE);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl744516();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    set(state, Ttl744516.CP, Value.FALSE);
    set(state, Ttl744516.MR, Value.FALSE);
    set(state, Ttl744516.PL, Value.FALSE);
    set(state, Ttl744516.CE, Value.TRUE);
    set(state, Ttl744516.UPDN, Value.TRUE);
    gate.propagate(state);
    load(gate, state, 4);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertBit(state, Ttl744516.Q0, Value.UNKNOWN);
    assertBit(state, Ttl744516.TC, Value.UNKNOWN);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 4);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertBit(state, Ttl744516.Q2, Value.UNKNOWN);
    assertBit(state, Ttl744516.TC, Value.UNKNOWN);
  }

  private static TtlTestInstanceState hold(Ttl744516 gate) {
    final var state = new TtlTestInstanceState(gate, false);
    set(state, Ttl744516.CP, Value.FALSE);
    set(state, Ttl744516.MR, Value.FALSE);
    set(state, Ttl744516.PL, Value.FALSE);
    set(state, Ttl744516.CE, Value.TRUE);
    set(state, Ttl744516.UPDN, Value.TRUE);
    setData(state, 0);
    gate.propagate(state);
    return state;
  }

  private static void enableCount(TtlTestInstanceState state, boolean up) {
    set(state, Ttl744516.MR, Value.FALSE);
    set(state, Ttl744516.PL, Value.FALSE);
    set(state, Ttl744516.CE, Value.FALSE);
    set(state, Ttl744516.UPDN, up ? Value.TRUE : Value.FALSE);
  }

  private static void load(Ttl744516 gate, TtlTestInstanceState state, int code) {
    set(state, Ttl744516.MR, Value.FALSE);
    set(state, Ttl744516.CE, Value.TRUE);
    set(state, Ttl744516.PL, Value.TRUE);
    setData(state, code);
    gate.propagate(state);
    set(state, Ttl744516.PL, Value.FALSE);
    gate.propagate(state);
  }

  private static void rise(Ttl744516 gate, TtlTestInstanceState state) {
    set(state, Ttl744516.CP, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl744516.CP, Value.TRUE);
    gate.propagate(state);
  }

  private static void fall(Ttl744516 gate, TtlTestInstanceState state) {
    set(state, Ttl744516.CP, Value.FALSE);
    gate.propagate(state);
  }

  private static void setData(TtlTestInstanceState state, int code) {
    set(state, Ttl744516.D0, bit(code, 0));
    set(state, Ttl744516.D1, bit(code, 1));
    set(state, Ttl744516.D2, bit(code, 2));
    set(state, Ttl744516.D3, bit(code, 3));
  }

  private static Value bit(int code, int index) {
    return ((code >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
  }

  private static void set(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl744516.pinNrToPortNr(pin), value);
  }

  private static void assertCount(TtlTestInstanceState state, int code) {
    assertBit(state, Ttl744516.Q0, bit(code, 0));
    assertBit(state, Ttl744516.Q1, bit(code, 1));
    assertBit(state, Ttl744516.Q2, bit(code, 2));
    assertBit(state, Ttl744516.Q3, bit(code, 3));
  }

  private static void assertBit(TtlTestInstanceState state, byte pin, Value expected) {
    assertEquals(expected, state.getPortValue(Ttl744516.pinNrToPortNr(pin)), "pin " + pin);
  }

  private static void assertPort(Instance instance, byte pin, int x, int y, int type) {
    final var port = Ttl744516.pinNrToPortNr(pin);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
