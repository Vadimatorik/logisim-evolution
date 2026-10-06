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

/** Functional tests for the 74HC4510 presettable BCD up/down counter. */
class Ttl744510Test {
  /** Decade cycle, then the illegal codes 10 to 15 from Philips Fig. 14, counting up. */
  private static final int[] NEXT_UP = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 11, 6, 13, 4, 15, 2};

  /** Decade cycle, then the illegal codes 10 to 15 from Philips Fig. 14, counting down. */
  private static final int[] NEXT_DOWN = {9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 13, 10, 3, 12, 1, 14};

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl744510();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744510.PL, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744510.Q3, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744510.D3, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744510.D0, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744510.CE, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744510.Q0, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744510.TC, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744510.MR, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744510.UPDN, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744510.Q1, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744510.D1, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744510.D2, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744510.Q2, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744510.CP, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void clockIsTheLastLogicalPort() {
    final var gate = new Ttl744510();
    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(new int[] {Ttl744510.pinNrToPortNr(Ttl744510.CP)}, gate.clockPinIndex(null));
  }

  @Test
  void masterResetClearsWithoutAClockAndOverridesLoad() {
    final var gate = new Ttl744510();
    final var state = hold(gate);
    load(gate, state, 12);

    set(state, Ttl744510.PL, Value.TRUE);
    setData(state, 9);
    set(state, Ttl744510.MR, Value.TRUE);
    set(state, Ttl744510.CP, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
    assertBit(state, Ttl744510.TC, Value.TRUE);

    set(state, Ttl744510.UPDN, Value.FALSE);
    set(state, Ttl744510.CE, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 0);
    assertBit(state, Ttl744510.TC, Value.FALSE);

    set(state, Ttl744510.MR, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 9);
    assertBit(state, Ttl744510.TC, Value.TRUE);
  }

  @Test
  void parallelLoadIsTransparentWhilePresetStaysHigh() {
    final var gate = new Ttl744510();
    final var state = hold(gate);

    for (var code = 0; code < 16; code++) {
      load(gate, state, code);
      assertCount(state, code);
    }

    set(state, Ttl744510.PL, Value.TRUE);
    set(state, Ttl744510.CE, Value.FALSE);
    set(state, Ttl744510.UPDN, Value.TRUE);
    setData(state, 6);
    gate.propagate(state);
    assertCount(state, 6);
    setData(state, 10);
    gate.propagate(state);
    assertCount(state, 10);
    set(state, Ttl744510.PL, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 10);
  }

  @Test
  void countingUpWrapsAtNineAndTerminalCountFollowsQ0AndQ3() {
    final var gate = new Ttl744510();
    final var state = hold(gate);
    load(gate, state, 0);
    enableCount(state, true);

    for (var code = 0; code < 10; code++) {
      assertCount(state, code);
      assertBit(state, Ttl744510.TC, terminalUp(code));
      rise(gate, state);
      fall(gate, state);
    }
    assertCount(state, 0);
    assertBit(state, Ttl744510.TC, Value.TRUE);
  }

  @Test
  void countingDownWrapsAtZeroAndTerminalCountIsOnlyCodeZero() {
    final var gate = new Ttl744510();
    final var state = hold(gate);
    load(gate, state, 0);
    enableCount(state, false);
    gate.propagate(state);
    assertCount(state, 0);
    assertBit(state, Ttl744510.TC, Value.FALSE);

    for (var code = 9; code >= 0; code--) {
      rise(gate, state);
      fall(gate, state);
      assertCount(state, code);
      assertBit(state, Ttl744510.TC, code == 0 ? Value.FALSE : Value.TRUE);
    }
  }

  @Test
  void illegalCodesFollowTheStateDiagramInBothDirections() {
    final var gate = new Ttl744510();
    final var state = hold(gate);

    for (var code = 0; code < 16; code++) {
      load(gate, state, code);
      enableCount(state, true);
      rise(gate, state);
      assertCount(state, NEXT_UP[code]);
      fall(gate, state);
    }

    for (var code = 0; code < 16; code++) {
      load(gate, state, code);
      enableCount(state, false);
      rise(gate, state);
      assertCount(state, NEXT_DOWN[code]);
      fall(gate, state);
    }
  }

  @Test
  void terminalCountTracksIllegalUpCodesWithoutAClock() {
    final var gate = new Ttl744510();
    final var state = hold(gate);
    enableCount(state, true);

    for (var code = 10; code < 16; code++) {
      load(gate, state, code);
      enableCount(state, true);
      gate.propagate(state);
      assertCount(state, code);
      assertBit(state, Ttl744510.TC, terminalUp(code));
    }

    load(gate, state, 9);
    enableCount(state, false);
    gate.propagate(state);
    assertCount(state, 9);
    assertBit(state, Ttl744510.TC, Value.TRUE);
  }

  @Test
  void countEnableAndFallingClockHoldAndDirectionChangesTerminalCount() {
    final var gate = new Ttl744510();
    final var state = hold(gate);
    load(gate, state, 9);
    set(state, Ttl744510.CE, Value.TRUE);
    set(state, Ttl744510.UPDN, Value.TRUE);
    gate.propagate(state);
    assertBit(state, Ttl744510.TC, Value.TRUE);
    rise(gate, state);
    fall(gate, state);
    assertCount(state, 9);

    set(state, Ttl744510.CP, Value.TRUE);
    gate.propagate(state);
    set(state, Ttl744510.CE, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 9);
    assertBit(state, Ttl744510.TC, Value.FALSE);
    set(state, Ttl744510.UPDN, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 9);
    assertBit(state, Ttl744510.TC, Value.TRUE);

    set(state, Ttl744510.CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 9);
  }

  @Test
  void releasingResetWhileTheClockIsHighDoesNotCount() {
    final var gate = new Ttl744510();
    final var state = hold(gate);
    load(gate, state, 3);
    enableCount(state, true);
    set(state, Ttl744510.CP, Value.FALSE);
    set(state, Ttl744510.MR, Value.TRUE);
    gate.propagate(state);
    set(state, Ttl744510.CP, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
    set(state, Ttl744510.MR, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 0);
    fall(gate, state);
    rise(gate, state);
    assertCount(state, 1);
  }

  @Test
  void unknownAndFallingClocksDoNotCount() {
    final var gate = new Ttl744510();
    final var state = hold(gate);
    load(gate, state, 4);
    enableCount(state, true);
    set(state, Ttl744510.CP, Value.UNKNOWN);
    gate.propagate(state);
    set(state, Ttl744510.CP, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 4);
    set(state, Ttl744510.CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 4);
  }

  @Test
  void unknownResetAndLoadConflictOnlyOnBitsThatDiffer() {
    final var gate = new Ttl744510();
    final var state = hold(gate);
    load(gate, state, 5);
    set(state, Ttl744510.CE, Value.TRUE);
    set(state, Ttl744510.MR, Value.UNKNOWN);
    gate.propagate(state);
    assertBit(state, Ttl744510.Q0, Value.UNKNOWN);
    assertBit(state, Ttl744510.Q1, Value.FALSE);
    assertBit(state, Ttl744510.Q2, Value.UNKNOWN);
    assertBit(state, Ttl744510.Q3, Value.FALSE);
    assertBit(state, Ttl744510.TC, Value.TRUE);

    load(gate, state, 5);
    set(state, Ttl744510.CE, Value.TRUE);
    set(state, Ttl744510.PL, Value.UNKNOWN);
    setData(state, 7);
    gate.propagate(state);
    assertBit(state, Ttl744510.Q0, Value.TRUE);
    assertBit(state, Ttl744510.Q1, Value.UNKNOWN);
    assertBit(state, Ttl744510.Q2, Value.TRUE);
    assertBit(state, Ttl744510.Q3, Value.FALSE);

    load(gate, state, 9);
    enableCount(state, true);
    set(state, Ttl744510.UPDN, Value.UNKNOWN);
    gate.propagate(state);
    assertCount(state, 9);
    assertBit(state, Ttl744510.TC, Value.UNKNOWN);
  }

  @Test
  void errorOnTheCountEnableIsVisibleOnlyWhenTheNextCodeDiffers() {
    final var gate = new Ttl744510();
    final var state = hold(gate);
    load(gate, state, 0);
    enableCount(state, true);
    set(state, Ttl744510.CE, Value.ERROR);
    rise(gate, state);
    assertBit(state, Ttl744510.Q0, Value.ERROR);
    assertBit(state, Ttl744510.Q1, Value.FALSE);
    assertBit(state, Ttl744510.Q2, Value.FALSE);
    assertBit(state, Ttl744510.Q3, Value.FALSE);

    load(gate, state, 9);
    set(state, Ttl744510.UPDN, Value.TRUE);
    set(state, Ttl744510.CE, Value.ERROR);
    gate.propagate(state);
    assertCount(state, 9);
    assertBit(state, Ttl744510.TC, Value.ERROR);

    load(gate, state, 3);
    set(state, Ttl744510.CE, Value.UNKNOWN);
    gate.propagate(state);
    assertBit(state, Ttl744510.TC, Value.TRUE);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl744510();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    set(state, Ttl744510.CP, Value.FALSE);
    set(state, Ttl744510.MR, Value.FALSE);
    set(state, Ttl744510.PL, Value.FALSE);
    set(state, Ttl744510.CE, Value.TRUE);
    set(state, Ttl744510.UPDN, Value.TRUE);
    gate.propagate(state);
    load(gate, state, 4);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertBit(state, Ttl744510.Q0, Value.UNKNOWN);
    assertBit(state, Ttl744510.TC, Value.UNKNOWN);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 4);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertBit(state, Ttl744510.Q2, Value.UNKNOWN);
    assertBit(state, Ttl744510.TC, Value.UNKNOWN);
  }

  private static Value terminalUp(int code) {
    final var q0AndQ3 = (code & 0x9) == 0x9;
    return q0AndQ3 ? Value.FALSE : Value.TRUE;
  }

  private static TtlTestInstanceState hold(Ttl744510 gate) {
    final var state = new TtlTestInstanceState(gate, false);
    set(state, Ttl744510.CP, Value.FALSE);
    set(state, Ttl744510.MR, Value.FALSE);
    set(state, Ttl744510.PL, Value.FALSE);
    set(state, Ttl744510.CE, Value.TRUE);
    set(state, Ttl744510.UPDN, Value.TRUE);
    setData(state, 0);
    gate.propagate(state);
    return state;
  }

  private static void enableCount(TtlTestInstanceState state, boolean up) {
    set(state, Ttl744510.MR, Value.FALSE);
    set(state, Ttl744510.PL, Value.FALSE);
    set(state, Ttl744510.CE, Value.FALSE);
    set(state, Ttl744510.UPDN, up ? Value.TRUE : Value.FALSE);
  }

  private static void load(Ttl744510 gate, TtlTestInstanceState state, int code) {
    set(state, Ttl744510.MR, Value.FALSE);
    set(state, Ttl744510.CE, Value.TRUE);
    set(state, Ttl744510.PL, Value.TRUE);
    setData(state, code);
    gate.propagate(state);
    set(state, Ttl744510.PL, Value.FALSE);
    gate.propagate(state);
  }

  private static void rise(Ttl744510 gate, TtlTestInstanceState state) {
    set(state, Ttl744510.CP, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl744510.CP, Value.TRUE);
    gate.propagate(state);
  }

  private static void fall(Ttl744510 gate, TtlTestInstanceState state) {
    set(state, Ttl744510.CP, Value.FALSE);
    gate.propagate(state);
  }

  private static void setData(TtlTestInstanceState state, int code) {
    set(state, Ttl744510.D0, bit(code, 0));
    set(state, Ttl744510.D1, bit(code, 1));
    set(state, Ttl744510.D2, bit(code, 2));
    set(state, Ttl744510.D3, bit(code, 3));
  }

  private static Value bit(int code, int index) {
    return ((code >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
  }

  private static void set(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl744510.pinNrToPortNr(pin), value);
  }

  private static void assertCount(TtlTestInstanceState state, int code) {
    assertBit(state, Ttl744510.Q0, bit(code, 0));
    assertBit(state, Ttl744510.Q1, bit(code, 1));
    assertBit(state, Ttl744510.Q2, bit(code, 2));
    assertBit(state, Ttl744510.Q3, bit(code, 3));
  }

  private static void assertBit(TtlTestInstanceState state, byte pin, Value expected) {
    assertEquals(expected, state.getPortValue(Ttl744510.pinNrToPortNr(pin)), "pin " + pin);
  }

  private static void assertPort(Instance instance, byte pin, int x, int y, int type) {
    final var port = Ttl744510.pinNrToPortNr(pin);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
