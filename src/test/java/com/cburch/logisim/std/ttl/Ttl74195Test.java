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

class Ttl74195Test {
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;
  private static final byte[] OUTPUTS = {
    Ttl74195.Q0, Ttl74195.Q1, Ttl74195.Q2, Ttl74195.Q3, Ttl74195.NQ3
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var register = new Ttl74195();
    final var hiddenPower = createInstance(register, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74195.MR, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74195.J, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74195.K, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74195.D0, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74195.D1, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74195.D2, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74195.D3, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74195.PE, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74195.CP, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74195.NQ3, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74195.Q3, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74195.Q2, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74195.Q1, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74195.Q0, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(register, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void masterResetClearsEveryStageAndReleasesToAHold() {
    final var register = new Ttl74195();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0xA);

    set(state, Ttl74195.MR, Value.FALSE);
    register.propagate(state);
    assertWord(state, 0);

    set(state, Ttl74195.MR, Value.TRUE);
    set(state, Ttl74195.PE, Value.FALSE);
    setData(state, 0xF);
    register.propagate(state);
    assertWord(state, 0);
  }

  @Test
  void parallelLoadStoresEveryCodeOnTheRisingEdge() {
    final var register = new Ttl74195();
    final var state = new TtlTestInstanceState(register, false);

    for (var code = 0; code < 16; code++) {
      load(register, state, code);
      assertWord(state, code);
    }
  }

  @Test
  void parallelLoadDoesNotHappenWithoutARisingEdge() {
    final var register = new Ttl74195();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0x5);

    set(state, Ttl74195.PE, Value.FALSE);
    setData(state, 0xA);
    register.propagate(state);
    assertWord(state, 0x5);

    set(state, Ttl74195.CP, Value.FALSE);
    register.propagate(state);
    assertWord(state, 0x5);

    set(state, Ttl74195.CP, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0xA);
  }

  @Test
  void shiftMovesBitsTowardQ3() {
    final var register = new Ttl74195();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0b1010);

    shift(register, state, Value.TRUE, Value.TRUE);
    assertWord(state, 0b0101);
  }

  @Test
  void firstStageFollowsTheJkModes() {
    final var register = new Ttl74195();
    final var state = new TtlTestInstanceState(register, false);

    load(register, state, 0b0001);
    shift(register, state, Value.TRUE, Value.TRUE);
    assertWord(state, 0b0011);

    load(register, state, 0b0001);
    shift(register, state, Value.FALSE, Value.FALSE);
    assertWord(state, 0b0010);

    load(register, state, 0b0001);
    shift(register, state, Value.TRUE, Value.FALSE);
    assertWord(state, 0b0010);

    load(register, state, 0b0001);
    shift(register, state, Value.FALSE, Value.TRUE);
    assertWord(state, 0b0011);
  }

  @Test
  void tiedJAndKActAsADataInput() {
    final var register = new Ttl74195();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0);

    final var serial = new int[] {1, 1, 0, 1};
    var word = 0;
    for (final var bit : serial) {
      final var level = bit == 1 ? Value.TRUE : Value.FALSE;
      shift(register, state, level, level);
      word = ((word & 0x7) << 1) | bit;
      assertWord(state, word);
    }
    assertWord(state, 0b1101);
  }

  @Test
  void masterResetOverridesARisingEdge() {
    final var register = new Ttl74195();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0xF);

    set(state, Ttl74195.MR, Value.FALSE);
    set(state, Ttl74195.PE, Value.FALSE);
    setData(state, 0xA);
    set(state, Ttl74195.CP, Value.FALSE);
    register.propagate(state);
    set(state, Ttl74195.CP, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0);

    set(state, Ttl74195.MR, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0);
  }

  @Test
  void fallingAndUnknownClocksDoNotShift() {
    final var register = new Ttl74195();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0xA);
    set(state, Ttl74195.PE, Value.TRUE);
    set(state, Ttl74195.J, Value.TRUE);
    set(state, Ttl74195.K, Value.TRUE);

    set(state, Ttl74195.CP, Value.FALSE);
    register.propagate(state);
    assertWord(state, 0xA);

    set(state, Ttl74195.CP, Value.UNKNOWN);
    register.propagate(state);
    assertWord(state, 0xA);

    set(state, Ttl74195.CP, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0xA);

    set(state, Ttl74195.CP, Value.FALSE);
    register.propagate(state);
    set(state, Ttl74195.CP, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0x5);
  }

  @Test
  void unknownParallelEnableConflictsOnlyWhereLoadAndShiftDiffer() {
    final var register = new Ttl74195();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0);
    set(state, Ttl74195.J, Value.FALSE);
    set(state, Ttl74195.K, Value.FALSE);
    setData(state, 0x1);

    set(state, Ttl74195.PE, Value.UNKNOWN);
    rise(register, state);
    assertBit(state, Ttl74195.Q0, Value.UNKNOWN);
    assertBit(state, Ttl74195.Q1, Value.FALSE);
    assertBit(state, Ttl74195.Q2, Value.FALSE);
    assertBit(state, Ttl74195.Q3, Value.FALSE);
    assertBit(state, Ttl74195.NQ3, Value.TRUE);

    load(register, state, 0);
    set(state, Ttl74195.J, Value.FALSE);
    set(state, Ttl74195.K, Value.FALSE);
    setData(state, 0x1);
    set(state, Ttl74195.PE, Value.ERROR);
    rise(register, state);
    assertBit(state, Ttl74195.Q0, Value.ERROR);
    assertBit(state, Ttl74195.Q1, Value.FALSE);
    assertBit(state, Ttl74195.NQ3, Value.TRUE);
  }

  @Test
  void unknownJConflictsOnlyTheFirstStage() {
    final var register = new Ttl74195();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0);
    set(state, Ttl74195.PE, Value.TRUE);
    set(state, Ttl74195.K, Value.TRUE);
    set(state, Ttl74195.J, Value.UNKNOWN);
    rise(register, state);

    assertBit(state, Ttl74195.Q0, Value.UNKNOWN);
    assertBit(state, Ttl74195.Q1, Value.FALSE);
    assertBit(state, Ttl74195.Q2, Value.FALSE);
    assertBit(state, Ttl74195.Q3, Value.FALSE);
    assertBit(state, Ttl74195.NQ3, Value.TRUE);

    load(register, state, 0);
    set(state, Ttl74195.PE, Value.TRUE);
    set(state, Ttl74195.K, Value.TRUE);
    set(state, Ttl74195.J, Value.ERROR);
    rise(register, state);
    assertBit(state, Ttl74195.Q0, Value.ERROR);
    assertBit(state, Ttl74195.Q1, Value.FALSE);
  }

  @Test
  void unknownLoadedBitShiftsTowardQ3() {
    final var register = new Ttl74195();
    final var state = new TtlTestInstanceState(register, false);
    set(state, Ttl74195.MR, Value.TRUE);
    set(state, Ttl74195.PE, Value.FALSE);
    set(state, Ttl74195.D0, Value.TRUE);
    set(state, Ttl74195.D1, Value.UNKNOWN);
    set(state, Ttl74195.D2, Value.FALSE);
    set(state, Ttl74195.D3, Value.FALSE);
    rise(register, state);
    assertBit(state, Ttl74195.Q0, Value.TRUE);
    assertBit(state, Ttl74195.Q1, Value.UNKNOWN);
    assertBit(state, Ttl74195.Q3, Value.FALSE);
    assertBit(state, Ttl74195.NQ3, Value.TRUE);

    shift(register, state, Value.FALSE, Value.FALSE);
    assertBit(state, Ttl74195.Q0, Value.FALSE);
    assertBit(state, Ttl74195.Q1, Value.TRUE);
    assertBit(state, Ttl74195.Q2, Value.UNKNOWN);
    assertBit(state, Ttl74195.Q3, Value.FALSE);
    assertBit(state, Ttl74195.NQ3, Value.TRUE);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var register = new Ttl74195();
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

  private static void load(Ttl74195 register, TtlTestInstanceState state, int code) {
    set(state, Ttl74195.MR, Value.TRUE);
    set(state, Ttl74195.PE, Value.FALSE);
    set(state, Ttl74195.J, Value.FALSE);
    set(state, Ttl74195.K, Value.FALSE);
    setData(state, code);
    rise(register, state);
  }

  private static void shift(
      Ttl74195 register, TtlTestInstanceState state, Value jPin, Value kPin) {
    set(state, Ttl74195.MR, Value.TRUE);
    set(state, Ttl74195.PE, Value.TRUE);
    set(state, Ttl74195.J, jPin);
    set(state, Ttl74195.K, kPin);
    rise(register, state);
  }

  private static void rise(Ttl74195 register, TtlTestInstanceState state) {
    set(state, Ttl74195.CP, Value.FALSE);
    register.propagate(state);
    set(state, Ttl74195.CP, Value.TRUE);
    register.propagate(state);
  }

  private static void setData(TtlTestInstanceState state, int code) {
    set(state, Ttl74195.D0, bit(code, 0));
    set(state, Ttl74195.D1, bit(code, 1));
    set(state, Ttl74195.D2, bit(code, 2));
    set(state, Ttl74195.D3, bit(code, 3));
  }

  private static Value bit(int code, int index) {
    return ((code >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
  }

  private static void set(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl74195.pinNrToPortNr(pin), value);
  }

  private static void assertWord(TtlTestInstanceState state, int code) {
    assertBit(state, Ttl74195.Q0, bit(code, 0));
    assertBit(state, Ttl74195.Q1, bit(code, 1));
    assertBit(state, Ttl74195.Q2, bit(code, 2));
    assertBit(state, Ttl74195.Q3, bit(code, 3));
    assertBit(state, Ttl74195.NQ3, bit(code, 3).not());
  }

  private static void assertBit(TtlTestInstanceState state, byte pin, Value expected) {
    assertEquals(expected, state.getPortValue(Ttl74195.pinNrToPortNr(pin)));
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var output : OUTPUTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74195.pinNrToPortNr(output)));
    }
  }

  private static void assertPort(Instance instance, byte pin, int x, int y, int type) {
    final var port = Ttl74195.pinNrToPortNr(pin);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
