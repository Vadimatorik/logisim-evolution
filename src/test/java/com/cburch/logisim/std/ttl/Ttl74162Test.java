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

/** Functional tests for the 74HC162 synchronous presettable BCD decade counter. */
class Ttl74162Test {
  /** Decade cycle, then the illegal codes 10 to 15 rejoining it within two clocks. */
  private static final int[] NEXT_COUNT = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 11, 6, 13, 4, 15, 2};

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74162();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74162.MR, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74162.CP, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74162.D0, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74162.D1, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74162.D2, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74162.D3, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74162.CEP, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74162.PE, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74162.CET, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74162.Q3, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74162.Q2, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74162.Q1, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74162.Q0, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74162.TC, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void clockIsTheSecondLogicalPort() {
    final var gate = new Ttl74162();
    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(new int[] {Ttl74162.pinNrToPortNr(Ttl74162.CP)}, gate.clockPinIndex(null));
  }

  @Test
  void resetIsSynchronousAndOverridesLoad() {
    final var gate = new Ttl74162();
    final var state = hold(gate);
    load(gate, state, 7);

    set(state, Ttl74162.MR, Value.FALSE);
    set(state, Ttl74162.PE, Value.FALSE);
    setData(state, 12);
    gate.propagate(state);
    assertCount(state, 7);
    assertBit(state, Ttl74162.TC, Value.FALSE);

    rise(gate, state);
    assertCount(state, 0);
    assertBit(state, Ttl74162.TC, Value.FALSE);
  }

  @Test
  void parallelLoadIgnoresTheCountEnables() {
    final var gate = new Ttl74162();
    final var state = hold(gate);

    for (var code = 0; code < 16; code++) {
      load(gate, state, code);
      assertCount(state, code);
    }
  }

  @Test
  void countingFollowsTheDecadeAndIllegalStateDiagram() {
    final var gate = new Ttl74162();
    final var state = hold(gate);
    enableCount(state);

    for (var code = 0; code < 16; code++) {
      load(gate, state, code);
      enableCount(state);
      rise(gate, state);
      assertCount(state, NEXT_COUNT[code]);
      fall(gate, state);
    }
  }

  @Test
  void decadeCycleWrapsAndTerminalCountTracksCodeNine() {
    final var gate = new Ttl74162();
    final var state = hold(gate);
    load(gate, state, 0);
    enableCount(state);

    for (var code = 0; code < 10; code++) {
      assertCount(state, code);
      assertBit(state, Ttl74162.TC, code == 9 ? Value.TRUE : Value.FALSE);
      rise(gate, state);
      fall(gate, state);
    }
    assertCount(state, 0);
    assertBit(state, Ttl74162.TC, Value.FALSE);
  }

  @Test
  void eitherCountEnableHoldsAndCarryEnableGatesTerminalCount() {
    final var gate = new Ttl74162();
    final var state = hold(gate);
    load(gate, state, 4);
    set(state, Ttl74162.CEP, Value.FALSE);
    set(state, Ttl74162.CET, Value.TRUE);
    rise(gate, state);
    fall(gate, state);
    assertCount(state, 4);
    assertBit(state, Ttl74162.TC, Value.FALSE);

    load(gate, state, 9);
    set(state, Ttl74162.CEP, Value.FALSE);
    set(state, Ttl74162.CET, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 9);
    assertBit(state, Ttl74162.TC, Value.TRUE);
    rise(gate, state);
    fall(gate, state);
    assertCount(state, 9);

    set(state, Ttl74162.CET, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 9);
    assertBit(state, Ttl74162.TC, Value.FALSE);

    set(state, Ttl74162.CEP, Value.TRUE);
    rise(gate, state);
    fall(gate, state);
    assertCount(state, 9);
    assertBit(state, Ttl74162.TC, Value.FALSE);
  }

  @Test
  void fallingAndUnknownClocksDoNotCount() {
    final var gate = new Ttl74162();
    final var state = hold(gate);
    load(gate, state, 2);
    enableCount(state);
    set(state, Ttl74162.CP, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 3);
    set(state, Ttl74162.CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 3);

    set(state, Ttl74162.CP, Value.UNKNOWN);
    gate.propagate(state);
    set(state, Ttl74162.CP, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 3);
  }

  @Test
  void unknownResetAndLoadConflictOnlyOnBitsThatDiffer() {
    final var gate = new Ttl74162();
    final var state = hold(gate);
    load(gate, state, 5);
    enableCount(state);
    set(state, Ttl74162.MR, Value.UNKNOWN);
    rise(gate, state);
    assertBit(state, Ttl74162.Q0, Value.FALSE);
    assertBit(state, Ttl74162.Q1, Value.UNKNOWN);
    assertBit(state, Ttl74162.Q2, Value.UNKNOWN);
    assertBit(state, Ttl74162.Q3, Value.FALSE);

    load(gate, state, 5);
    set(state, Ttl74162.CEP, Value.FALSE);
    set(state, Ttl74162.CET, Value.FALSE);
    set(state, Ttl74162.PE, Value.UNKNOWN);
    setData(state, 7);
    rise(gate, state);
    assertBit(state, Ttl74162.Q0, Value.TRUE);
    assertBit(state, Ttl74162.Q1, Value.UNKNOWN);
    assertBit(state, Ttl74162.Q2, Value.TRUE);
    assertBit(state, Ttl74162.Q3, Value.FALSE);
  }

  @Test
  void errorOnACountEnableIsVisibleOnlyWhenTheNextCodeDiffers() {
    final var gate = new Ttl74162();
    final var state = hold(gate);
    load(gate, state, 0);
    set(state, Ttl74162.CEP, Value.ERROR);
    set(state, Ttl74162.CET, Value.TRUE);
    rise(gate, state);
    assertBit(state, Ttl74162.Q0, Value.ERROR);
    assertBit(state, Ttl74162.Q1, Value.FALSE);
    assertBit(state, Ttl74162.Q2, Value.FALSE);
    assertBit(state, Ttl74162.Q3, Value.FALSE);

    load(gate, state, 9);
    set(state, Ttl74162.CET, Value.UNKNOWN);
    gate.propagate(state);
    assertCount(state, 9);
    assertBit(state, Ttl74162.TC, Value.UNKNOWN);

    set(state, Ttl74162.CET, Value.ERROR);
    gate.propagate(state);
    assertBit(state, Ttl74162.TC, Value.ERROR);

    load(gate, state, 3);
    set(state, Ttl74162.CET, Value.UNKNOWN);
    gate.propagate(state);
    assertBit(state, Ttl74162.TC, Value.FALSE);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74162();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    set(state, Ttl74162.CP, Value.FALSE);
    set(state, Ttl74162.MR, Value.TRUE);
    set(state, Ttl74162.PE, Value.TRUE);
    set(state, Ttl74162.CEP, Value.FALSE);
    set(state, Ttl74162.CET, Value.FALSE);
    gate.propagate(state);
    load(gate, state, 4);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertBit(state, Ttl74162.Q0, Value.UNKNOWN);
    assertBit(state, Ttl74162.TC, Value.UNKNOWN);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 4);
  }

  private static TtlTestInstanceState hold(Ttl74162 gate) {
    final var state = new TtlTestInstanceState(gate, false);
    set(state, Ttl74162.CP, Value.FALSE);
    set(state, Ttl74162.MR, Value.TRUE);
    set(state, Ttl74162.PE, Value.TRUE);
    set(state, Ttl74162.CEP, Value.FALSE);
    set(state, Ttl74162.CET, Value.FALSE);
    setData(state, 0);
    gate.propagate(state);
    return state;
  }

  private static void enableCount(TtlTestInstanceState state) {
    set(state, Ttl74162.MR, Value.TRUE);
    set(state, Ttl74162.PE, Value.TRUE);
    set(state, Ttl74162.CEP, Value.TRUE);
    set(state, Ttl74162.CET, Value.TRUE);
  }

  private static void load(Ttl74162 gate, TtlTestInstanceState state, int code) {
    set(state, Ttl74162.CP, Value.FALSE);
    set(state, Ttl74162.MR, Value.TRUE);
    set(state, Ttl74162.CEP, Value.FALSE);
    set(state, Ttl74162.CET, Value.FALSE);
    set(state, Ttl74162.PE, Value.FALSE);
    setData(state, code);
    gate.propagate(state);
    set(state, Ttl74162.CP, Value.TRUE);
    gate.propagate(state);
    set(state, Ttl74162.PE, Value.TRUE);
    set(state, Ttl74162.CP, Value.FALSE);
    gate.propagate(state);
  }

  private static void rise(Ttl74162 gate, TtlTestInstanceState state) {
    set(state, Ttl74162.CP, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl74162.CP, Value.TRUE);
    gate.propagate(state);
  }

  private static void fall(Ttl74162 gate, TtlTestInstanceState state) {
    set(state, Ttl74162.CP, Value.FALSE);
    gate.propagate(state);
  }

  private static void setData(TtlTestInstanceState state, int code) {
    set(state, Ttl74162.D0, bit(code, 0));
    set(state, Ttl74162.D1, bit(code, 1));
    set(state, Ttl74162.D2, bit(code, 2));
    set(state, Ttl74162.D3, bit(code, 3));
  }

  private static Value bit(int code, int index) {
    return ((code >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
  }

  private static void set(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl74162.pinNrToPortNr(pin), value);
  }

  private static void assertCount(TtlTestInstanceState state, int code) {
    assertBit(state, Ttl74162.Q0, bit(code, 0));
    assertBit(state, Ttl74162.Q1, bit(code, 1));
    assertBit(state, Ttl74162.Q2, bit(code, 2));
    assertBit(state, Ttl74162.Q3, bit(code, 3));
  }

  private static void assertBit(TtlTestInstanceState state, byte pin, Value expected) {
    assertEquals(expected, state.getPortValue(Ttl74162.pinNrToPortNr(pin)), "pin " + pin);
  }

  private static void assertPort(Instance instance, byte pin, int x, int y, int type) {
    final var port = Ttl74162.pinNrToPortNr(pin);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
