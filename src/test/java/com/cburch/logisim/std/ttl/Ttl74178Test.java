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
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

class Ttl74178Test {
  private static final int GND_PORT = 12;
  private static final int VCC_PORT = 13;
  private static final byte[] OUTPUTS = {Ttl74178.QA, Ttl74178.QB, Ttl74178.QC, Ttl74178.QD};

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var register = new Ttl74178();
    final var hiddenPower = createInstance(register, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74178.B, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74178.A, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74178.SER, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74178.QA, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74178.CLK, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74178.QB, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74178.QC, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74178.LOAD, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74178.QD, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74178.SHIFT, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74178.D, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74178.C, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(register, true);
    assertEquals(14, shownPower.getPorts().size());
    assertEquals(Location.create(130, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void parallelLoadStoresEveryCodeOnTheFallingEdge() {
    final var register = new Ttl74178();
    final var state = new TtlTestInstanceState(register, false);

    for (var code = 0; code < 16; code++) {
      load(register, state, code);
      assertWord(state, code);
    }
  }

  @Test
  void parallelLoadDoesNotHappenWithoutAFallingEdge() {
    final var register = new Ttl74178();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0x5);

    set(state, Ttl74178.SHIFT, Value.FALSE);
    set(state, Ttl74178.LOAD, Value.TRUE);
    setData(state, 0xA);
    register.propagate(state);
    assertWord(state, 0x5);

    set(state, Ttl74178.CLK, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0x5);

    set(state, Ttl74178.CLK, Value.FALSE);
    register.propagate(state);
    assertWord(state, 0xA);
  }

  @Test
  void holdKeepsTheWordWhileTheClockRuns() {
    final var register = new Ttl74178();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0x9);

    set(state, Ttl74178.SHIFT, Value.FALSE);
    set(state, Ttl74178.LOAD, Value.FALSE);
    set(state, Ttl74178.SER, Value.TRUE);
    setData(state, 0x6);
    fall(register, state);
    fall(register, state);
    assertWord(state, 0x9);
  }

  @Test
  void shiftMovesBitsTowardQdAndIgnoresLoad() {
    final var register = new Ttl74178();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0b1010);

    shift(register, state, Value.TRUE, Value.TRUE, 0);
    assertWord(state, 0b0101);

    load(register, state, 0b0001);
    shift(register, state, Value.FALSE, Value.FALSE, 0xF);
    assertWord(state, 0b0010);
  }

  @Test
  void risingAndUnknownClocksDoNotShift() {
    final var register = new Ttl74178();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0xA);
    set(state, Ttl74178.SHIFT, Value.TRUE);
    set(state, Ttl74178.LOAD, Value.FALSE);
    set(state, Ttl74178.SER, Value.TRUE);

    set(state, Ttl74178.CLK, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0xA);

    set(state, Ttl74178.CLK, Value.UNKNOWN);
    register.propagate(state);
    assertWord(state, 0xA);

    set(state, Ttl74178.CLK, Value.FALSE);
    register.propagate(state);
    assertWord(state, 0xA);

    set(state, Ttl74178.CLK, Value.TRUE);
    register.propagate(state);
    set(state, Ttl74178.CLK, Value.FALSE);
    register.propagate(state);
    assertWord(state, 0x5);
  }

  @Test
  void unknownShiftConflictsOnlyWhereShiftAndHoldDiffer() {
    final var register = new Ttl74178();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0);
    set(state, Ttl74178.SHIFT, Value.UNKNOWN);
    set(state, Ttl74178.LOAD, Value.FALSE);
    set(state, Ttl74178.SER, Value.TRUE);
    fall(register, state);

    assertBit(state, Ttl74178.QA, Value.UNKNOWN);
    assertBit(state, Ttl74178.QB, Value.FALSE);
    assertBit(state, Ttl74178.QC, Value.FALSE);
    assertBit(state, Ttl74178.QD, Value.FALSE);

    load(register, state, 0);
    set(state, Ttl74178.SHIFT, Value.ERROR);
    set(state, Ttl74178.LOAD, Value.FALSE);
    set(state, Ttl74178.SER, Value.TRUE);
    fall(register, state);
    assertBit(state, Ttl74178.QA, Value.ERROR);
    assertBit(state, Ttl74178.QB, Value.FALSE);
  }

  @Test
  void unknownLoadConflictsOnlyWhereLoadAndHoldDiffer() {
    final var register = new Ttl74178();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0);
    set(state, Ttl74178.SHIFT, Value.FALSE);
    set(state, Ttl74178.LOAD, Value.UNKNOWN);
    setData(state, 0x1);
    fall(register, state);

    assertBit(state, Ttl74178.QA, Value.UNKNOWN);
    assertBit(state, Ttl74178.QB, Value.FALSE);
    assertBit(state, Ttl74178.QC, Value.FALSE);
    assertBit(state, Ttl74178.QD, Value.FALSE);

    load(register, state, 0);
    set(state, Ttl74178.SHIFT, Value.FALSE);
    set(state, Ttl74178.LOAD, Value.ERROR);
    setData(state, 0x1);
    fall(register, state);
    assertBit(state, Ttl74178.QA, Value.ERROR);
    assertBit(state, Ttl74178.QB, Value.FALSE);
  }

  @Test
  void unknownLoadedBitShiftsTowardQd() {
    final var register = new Ttl74178();
    final var state = new TtlTestInstanceState(register, false);
    set(state, Ttl74178.SHIFT, Value.FALSE);
    set(state, Ttl74178.LOAD, Value.TRUE);
    set(state, Ttl74178.A, Value.TRUE);
    set(state, Ttl74178.B, Value.UNKNOWN);
    set(state, Ttl74178.C, Value.FALSE);
    set(state, Ttl74178.D, Value.FALSE);
    fall(register, state);
    assertBit(state, Ttl74178.QA, Value.TRUE);
    assertBit(state, Ttl74178.QB, Value.UNKNOWN);
    assertBit(state, Ttl74178.QC, Value.FALSE);
    assertBit(state, Ttl74178.QD, Value.FALSE);

    shift(register, state, Value.FALSE, Value.FALSE, 0);
    assertBit(state, Ttl74178.QA, Value.FALSE);
    assertBit(state, Ttl74178.QB, Value.TRUE);
    assertBit(state, Ttl74178.QC, Value.UNKNOWN);
    assertBit(state, Ttl74178.QD, Value.FALSE);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var register = new Ttl74178();
    final var state = new TtlTestInstanceState(register, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    load(register, state, 0x6);
    assertWord(state, 0x6);

    state.setPortValue(VCC_PORT, Value.FALSE);
    register.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0x6);

    state.setPortValue(GND_PORT, Value.TRUE);
    register.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(GND_PORT, Value.FALSE);
    register.propagate(state);
    assertWord(state, 0x6);
  }

  private static void load(Ttl74178 register, TtlTestInstanceState state, int code) {
    set(state, Ttl74178.SHIFT, Value.FALSE);
    set(state, Ttl74178.LOAD, Value.TRUE);
    set(state, Ttl74178.SER, Value.FALSE);
    setData(state, code);
    fall(register, state);
  }

  private static void shift(
      Ttl74178 register, TtlTestInstanceState state, Value serial, Value load, int parallel) {
    set(state, Ttl74178.SHIFT, Value.TRUE);
    set(state, Ttl74178.LOAD, load);
    set(state, Ttl74178.SER, serial);
    setData(state, parallel);
    fall(register, state);
  }

  private static void fall(Ttl74178 register, TtlTestInstanceState state) {
    set(state, Ttl74178.CLK, Value.TRUE);
    register.propagate(state);
    set(state, Ttl74178.CLK, Value.FALSE);
    register.propagate(state);
  }

  private static void setData(TtlTestInstanceState state, int code) {
    set(state, Ttl74178.A, bit(code, 0));
    set(state, Ttl74178.B, bit(code, 1));
    set(state, Ttl74178.C, bit(code, 2));
    set(state, Ttl74178.D, bit(code, 3));
  }

  private static Value bit(int code, int index) {
    return ((code >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
  }

  private static void set(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl74178.pinNrToPortNr(pin), value);
  }

  private static void assertWord(TtlTestInstanceState state, int code) {
    assertBit(state, Ttl74178.QA, bit(code, 0));
    assertBit(state, Ttl74178.QB, bit(code, 1));
    assertBit(state, Ttl74178.QC, bit(code, 2));
    assertBit(state, Ttl74178.QD, bit(code, 3));
  }

  private static void assertBit(TtlTestInstanceState state, byte pin, Value expected) {
    assertEquals(expected, state.getPortValue(Ttl74178.pinNrToPortNr(pin)));
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var output : OUTPUTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74178.pinNrToPortNr(output)));
    }
  }

  private static void assertPort(Instance instance, byte pin, int x, int y, int type) {
    final var port = Ttl74178.pinNrToPortNr(pin);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
