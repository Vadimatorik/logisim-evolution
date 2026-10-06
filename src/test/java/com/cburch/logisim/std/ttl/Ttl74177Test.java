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

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74177 presettable binary counter/latch. */
class Ttl74177Test {
  private static final int GND_PORT = 12;
  private static final int VCC_PORT = 13;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74177();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74177.LOAD, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74177.QC, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74177.C, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74177.A, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74177.QA, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74177.CLK2, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74177.CLK1, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74177.QB, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74177.B, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74177.D, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74177.QD, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74177.CLR, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertEquals(Location.create(130, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void bothClocksAreReported() {
    final var gate = new Ttl74177();
    assertArrayEquals(
        new int[] {Ttl74177.pinNrToPortNr(Ttl74177.CLK1), Ttl74177.pinNrToPortNr(Ttl74177.CLK2)},
        gate.clockPinIndex(null));
  }

  @Test
  void clearOverridesLoadAndClocks() {
    final var gate = new Ttl74177();
    final var state = hold(gate);
    load(gate, state, 0xA);
    assertEquals(0xA, code(state));

    set(state, Ttl74177.LOAD, Value.FALSE);
    setData(state, 0xF);
    set(state, Ttl74177.CLR, Value.FALSE);
    gate.propagate(state);
    assertEquals(0, code(state));

    fall(gate, state, Ttl74177.CLK1);
    fall(gate, state, Ttl74177.CLK2);
    assertEquals(0, code(state));
  }

  @Test
  void loadIsTransparentAndThenHolds() {
    final var gate = new Ttl74177();
    final var state = hold(gate);

    for (var code = 0; code < 16; code++) {
      set(state, Ttl74177.LOAD, Value.FALSE);
      setData(state, code);
      gate.propagate(state);
      assertEquals(code, code(state));
    }

    set(state, Ttl74177.LOAD, Value.TRUE);
    setData(state, 0);
    gate.propagate(state);
    assertEquals(15, code(state));
  }

  @Test
  void loadOverridesAClockEdge() {
    final var gate = new Ttl74177();
    final var state = hold(gate);
    load(gate, state, 0);
    set(state, Ttl74177.LOAD, Value.FALSE);
    setData(state, 5);
    fall(gate, state, Ttl74177.CLK1);
    fall(gate, state, Ttl74177.CLK2);
    assertEquals(5, code(state));
  }

  @Test
  void fallingClk1TogglesOnlyQa() {
    final var gate = new Ttl74177();
    final var state = hold(gate);
    load(gate, state, 0x2);

    fall(gate, state, Ttl74177.CLK1);
    assertEquals(0x3, code(state));
    rise(gate, state, Ttl74177.CLK1);
    assertEquals(0x3, code(state));
    fall(gate, state, Ttl74177.CLK1);
    assertEquals(0x2, code(state));
  }

  @Test
  void divideByEightCyclesThroughEveryCode() {
    final var gate = new Ttl74177();
    final var state = hold(gate);
    load(gate, state, 0);

    final var cycle = new int[] {0x2, 0x4, 0x6, 0x8, 0xA, 0xC, 0xE, 0x0};
    for (final var expected : cycle) {
      fall(gate, state, Ttl74177.CLK2);
      assertEquals(expected, code(state));
      rise(gate, state, Ttl74177.CLK2);
      assertEquals(expected, code(state));
    }
  }

  @Test
  void bothSectionsAdvanceFromTheSameStoredWord() {
    final var gate = new Ttl74177();
    final var state = hold(gate);
    load(gate, state, 0);
    set(state, Ttl74177.CLK1, Value.FALSE);
    set(state, Ttl74177.CLK2, Value.FALSE);
    gate.propagate(state);
    assertEquals(0x3, code(state));
  }

  @Test
  void externalQaToClk2CountsBinary() {
    final var gate = new Ttl74177();
    final var state = hold(gate);
    loadWithClk2Low(gate, state);
    for (var code = 0; code < 16; code++) {
      assertEquals(code, code(state));
      binaryStep(gate, state);
    }
    assertEquals(0, code(state));
  }

  @Test
  void unknownClockLevelIsNotAnEdge() {
    final var gate = new Ttl74177();
    final var state = hold(gate);
    load(gate, state, 1);
    set(state, Ttl74177.CLK1, Value.UNKNOWN);
    gate.propagate(state);
    set(state, Ttl74177.CLK1, Value.FALSE);
    gate.propagate(state);
    assertEquals(1, code(state));
  }

  @Test
  void unknownAndErrorInputsAffectOnlyDisagreements() {
    final var gate = new Ttl74177();
    final var state = hold(gate);
    load(gate, state, 0xA);
    set(state, Ttl74177.CLR, Value.UNKNOWN);
    gate.propagate(state);
    assertBit(state, Ttl74177.QA, Value.FALSE);
    assertBit(state, Ttl74177.QB, Value.UNKNOWN);
    assertBit(state, Ttl74177.QC, Value.FALSE);
    assertBit(state, Ttl74177.QD, Value.UNKNOWN);

    clear(gate, state);
    set(state, Ttl74177.LOAD, Value.FALSE);
    set(state, Ttl74177.CLR, Value.ERROR);
    setData(state, 0xA);
    gate.propagate(state);
    assertBit(state, Ttl74177.QA, Value.FALSE);
    assertBit(state, Ttl74177.QB, Value.ERROR);
    assertBit(state, Ttl74177.QC, Value.FALSE);
    assertBit(state, Ttl74177.QD, Value.ERROR);

    clear(gate, state);
    set(state, Ttl74177.LOAD, Value.UNKNOWN);
    setData(state, 0x1);
    gate.propagate(state);
    assertBit(state, Ttl74177.QA, Value.UNKNOWN);
    assertBit(state, Ttl74177.QB, Value.FALSE);
    assertBit(state, Ttl74177.QC, Value.FALSE);
    assertBit(state, Ttl74177.QD, Value.FALSE);

    set(state, Ttl74177.LOAD, Value.FALSE);
    set(state, Ttl74177.A, Value.UNKNOWN);
    set(state, Ttl74177.B, Value.FALSE);
    set(state, Ttl74177.C, Value.TRUE);
    set(state, Ttl74177.D, Value.FALSE);
    gate.propagate(state);
    assertBit(state, Ttl74177.QA, Value.UNKNOWN);
    assertBit(state, Ttl74177.QB, Value.FALSE);
    assertBit(state, Ttl74177.QC, Value.TRUE);
    assertBit(state, Ttl74177.QD, Value.FALSE);

    set(state, Ttl74177.A, Value.ERROR);
    gate.propagate(state);
    assertBit(state, Ttl74177.QA, Value.ERROR);
    assertBit(state, Ttl74177.QC, Value.TRUE);

    clear(gate, state);
    load(gate, state, 0);
    set(state, Ttl74177.C, Value.UNKNOWN);
    fall(gate, state, Ttl74177.CLK2);
    assertEquals(0x2, code(state));
  }

  @Test
  void undefinedSectionBecomesUnknownOrErrorOnItsClock() {
    final var gate = new Ttl74177();
    final var state = hold(gate);
    set(state, Ttl74177.LOAD, Value.FALSE);
    set(state, Ttl74177.A, Value.FALSE);
    set(state, Ttl74177.B, Value.FALSE);
    set(state, Ttl74177.C, Value.UNKNOWN);
    set(state, Ttl74177.D, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl74177.LOAD, Value.TRUE);
    gate.propagate(state);
    fall(gate, state, Ttl74177.CLK2);
    assertBit(state, Ttl74177.QA, Value.FALSE);
    assertBit(state, Ttl74177.QB, Value.TRUE);
    assertBit(state, Ttl74177.QC, Value.UNKNOWN);
    assertBit(state, Ttl74177.QD, Value.FALSE);

    clear(gate, state);
    set(state, Ttl74177.LOAD, Value.FALSE);
    set(state, Ttl74177.A, Value.FALSE);
    set(state, Ttl74177.B, Value.FALSE);
    set(state, Ttl74177.C, Value.ERROR);
    set(state, Ttl74177.D, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl74177.LOAD, Value.TRUE);
    gate.propagate(state);
    fall(gate, state, Ttl74177.CLK2);
    assertBit(state, Ttl74177.QA, Value.FALSE);
    assertBit(state, Ttl74177.QB, Value.TRUE);
    assertBit(state, Ttl74177.QC, Value.ERROR);
    assertBit(state, Ttl74177.QD, Value.FALSE);
  }

  @Test
  void missingPowerLeavesOutputsUnknown() {
    final var gate = new Ttl74177();
    final var state = new TtlTestInstanceState(gate, true);
    set(state, Ttl74177.CLR, Value.FALSE);
    set(state, Ttl74177.LOAD, Value.TRUE);
    set(state, Ttl74177.CLK1, Value.TRUE);
    set(state, Ttl74177.CLK2, Value.TRUE);
    gate.propagate(state);
    assertBit(state, Ttl74177.QA, Value.UNKNOWN);
    assertBit(state, Ttl74177.QD, Value.UNKNOWN);

    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(0, code(state));
  }

  private static TtlTestInstanceState hold(Ttl74177 gate) {
    final var state = new TtlTestInstanceState(gate, false);
    set(state, Ttl74177.CLR, Value.TRUE);
    set(state, Ttl74177.LOAD, Value.TRUE);
    set(state, Ttl74177.CLK1, Value.TRUE);
    set(state, Ttl74177.CLK2, Value.TRUE);
    setData(state, 0);
    gate.propagate(state);
    return state;
  }

  private static void clear(Ttl74177 gate, TtlTestInstanceState state) {
    set(state, Ttl74177.CLR, Value.FALSE);
    set(state, Ttl74177.LOAD, Value.TRUE);
    set(state, Ttl74177.CLK1, Value.TRUE);
    set(state, Ttl74177.CLK2, Value.TRUE);
    gate.propagate(state);
    set(state, Ttl74177.CLR, Value.TRUE);
    gate.propagate(state);
  }

  /** Loads 0 while {@code CLK2} is already low, so a later binary step does not see a stale edge. */
  private static void loadWithClk2Low(Ttl74177 gate, TtlTestInstanceState state) {
    set(state, Ttl74177.CLR, Value.TRUE);
    set(state, Ttl74177.LOAD, Value.FALSE);
    set(state, Ttl74177.CLK1, Value.TRUE);
    set(state, Ttl74177.CLK2, Value.FALSE);
    setData(state, 0);
    gate.propagate(state);
    set(state, Ttl74177.LOAD, Value.TRUE);
    gate.propagate(state);
  }

  private static void load(Ttl74177 gate, TtlTestInstanceState state, int code) {
    set(state, Ttl74177.CLR, Value.TRUE);
    set(state, Ttl74177.LOAD, Value.FALSE);
    set(state, Ttl74177.CLK1, Value.TRUE);
    set(state, Ttl74177.CLK2, Value.TRUE);
    setData(state, code);
    gate.propagate(state);
    set(state, Ttl74177.LOAD, Value.TRUE);
    gate.propagate(state);
  }

  private static void fall(Ttl74177 gate, TtlTestInstanceState state, byte clock) {
    set(state, clock, Value.TRUE);
    gate.propagate(state);
    set(state, clock, Value.FALSE);
    gate.propagate(state);
  }

  private static void rise(Ttl74177 gate, TtlTestInstanceState state, byte clock) {
    set(state, clock, Value.FALSE);
    gate.propagate(state);
    set(state, clock, Value.TRUE);
    gate.propagate(state);
  }

  /** One binary step: {@code CLK2} follows {@code QA}, and the clock is {@code CLK1}. */
  private static void binaryStep(Ttl74177 gate, TtlTestInstanceState state) {
    set(state, Ttl74177.CLK2, state.getPortValue(Ttl74177.pinNrToPortNr(Ttl74177.QA)));
    set(state, Ttl74177.CLK1, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl74177.CLK2, state.getPortValue(Ttl74177.pinNrToPortNr(Ttl74177.QA)));
    gate.propagate(state);
    set(state, Ttl74177.CLK1, Value.TRUE);
    gate.propagate(state);
  }

  private static void setData(TtlTestInstanceState state, int code) {
    set(state, Ttl74177.A, bit(code, 0));
    set(state, Ttl74177.B, bit(code, 1));
    set(state, Ttl74177.C, bit(code, 2));
    set(state, Ttl74177.D, bit(code, 3));
  }

  private static Value bit(int code, int shift) {
    return ((code >> shift) & 1) == 1 ? Value.TRUE : Value.FALSE;
  }

  private static int code(TtlTestInstanceState state) {
    return (level(state, Ttl74177.QD) << 3)
        + (level(state, Ttl74177.QC) << 2)
        + (level(state, Ttl74177.QB) << 1)
        + level(state, Ttl74177.QA);
  }

  private static int level(TtlTestInstanceState state, byte pin) {
    final var value = state.getPortValue(Ttl74177.pinNrToPortNr(pin));
    if (value == Value.TRUE) {
      return 1;
    }
    if (value == Value.FALSE) {
      return 0;
    }
    throw new AssertionError(value);
  }

  private static void assertBit(TtlTestInstanceState state, byte pin, Value expected) {
    assertEquals(expected, state.getPortValue(Ttl74177.pinNrToPortNr(pin)));
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74177.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void set(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl74177.pinNrToPortNr(pin), value);
  }
}
