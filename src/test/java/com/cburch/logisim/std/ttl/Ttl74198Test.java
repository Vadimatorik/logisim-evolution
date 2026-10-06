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

class Ttl74198Test {
  private static final int GND_PORT = 22;
  private static final int VCC_PORT = 23;
  private static final byte[] OUTPUTS = {
    Ttl74198.Q0, Ttl74198.Q1, Ttl74198.Q2, Ttl74198.Q3,
    Ttl74198.Q4, Ttl74198.Q5, Ttl74198.Q6, Ttl74198.Q7
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var register = new Ttl74198();
    final var hiddenPower = createInstance(register, false);

    assertEquals(22, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74198.S0, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.DSR, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.D0, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.Q0, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.D1, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.Q1, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.D2, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.Q2, 150, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.D3, 170, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.Q3, 190, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.CP, 210, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.MR, 230, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.DSL, 210, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.D4, 190, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.Q4, 170, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.D5, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.Q5, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.D6, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.Q6, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.D7, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.Q7, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74198.S1, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(register, true);
    assertEquals(24, shownPower.getPorts().size());
    assertEquals(Location.create(230, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void masterResetClearsEveryStageAndReleasesToAHold() {
    final var register = new Ttl74198();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0xA5);

    set(state, Ttl74198.MR, Value.FALSE);
    register.propagate(state);
    assertWord(state, 0);

    set(state, Ttl74198.MR, Value.TRUE);
    set(state, Ttl74198.S0, Value.TRUE);
    set(state, Ttl74198.S1, Value.TRUE);
    setData(state, 0xFF);
    register.propagate(state);
    assertWord(state, 0);
  }

  @Test
  void parallelLoadStoresTheWordOnTheRisingEdge() {
    final var register = new Ttl74198();
    final var state = new TtlTestInstanceState(register, false);
    final var codes = new int[] {0x00, 0xFF, 0xA5, 0x5A, 0x01, 0x80};

    for (final var code : codes) {
      load(register, state, code);
      assertWord(state, code);
    }
  }

  @Test
  void parallelLoadDoesNotHappenWithoutARisingEdge() {
    final var register = new Ttl74198();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0x3C);

    set(state, Ttl74198.S0, Value.TRUE);
    set(state, Ttl74198.S1, Value.TRUE);
    setData(state, 0xC3);
    register.propagate(state);
    assertWord(state, 0x3C);

    set(state, Ttl74198.CP, Value.FALSE);
    register.propagate(state);
    assertWord(state, 0x3C);

    set(state, Ttl74198.CP, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0xC3);
  }

  @Test
  void holdIgnoresTheClockAndTheDataInputs() {
    final var register = new Ttl74198();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0x96);

    set(state, Ttl74198.S0, Value.FALSE);
    set(state, Ttl74198.S1, Value.FALSE);
    set(state, Ttl74198.DSR, Value.TRUE);
    set(state, Ttl74198.DSL, Value.TRUE);
    setData(state, 0x00);
    rise(register, state);
    assertWord(state, 0x96);
  }

  @Test
  void shiftRightWalksABitFromQ0TowardQ7() {
    final var register = new Ttl74198();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0);

    shiftRight(register, state, Value.TRUE);
    assertWord(state, 0x01);
    for (var step = 1; step < 8; step++) {
      shiftRight(register, state, Value.FALSE);
      assertWord(state, 1 << step);
    }
    shiftRight(register, state, Value.FALSE);
    assertWord(state, 0);
  }

  @Test
  void shiftLeftWalksABitFromQ7TowardQ0() {
    final var register = new Ttl74198();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0);

    shiftLeft(register, state, Value.TRUE);
    assertWord(state, 0x80);
    for (var step = 1; step < 8; step++) {
      shiftLeft(register, state, Value.FALSE);
      assertWord(state, 0x80 >> step);
    }
    shiftLeft(register, state, Value.FALSE);
    assertWord(state, 0);
  }

  @Test
  void masterResetOverridesARisingEdge() {
    final var register = new Ttl74198();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0xFF);

    set(state, Ttl74198.MR, Value.FALSE);
    set(state, Ttl74198.S0, Value.TRUE);
    set(state, Ttl74198.S1, Value.TRUE);
    setData(state, 0xA5);
    set(state, Ttl74198.CP, Value.FALSE);
    register.propagate(state);
    set(state, Ttl74198.CP, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0);

    set(state, Ttl74198.MR, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0);
  }

  @Test
  void fallingAndUnknownClocksDoNotShift() {
    final var register = new Ttl74198();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0xA5);
    set(state, Ttl74198.S1, Value.FALSE);
    set(state, Ttl74198.S0, Value.TRUE);
    set(state, Ttl74198.DSR, Value.TRUE);

    set(state, Ttl74198.CP, Value.FALSE);
    register.propagate(state);
    assertWord(state, 0xA5);

    set(state, Ttl74198.CP, Value.UNKNOWN);
    register.propagate(state);
    assertWord(state, 0xA5);

    set(state, Ttl74198.CP, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0xA5);

    set(state, Ttl74198.CP, Value.FALSE);
    register.propagate(state);
    set(state, Ttl74198.CP, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0x4B);
  }

  @Test
  void unknownModeConflictsOnlyWhereTheAlternativesDiffer() {
    final var register = new Ttl74198();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0);
    set(state, Ttl74198.S1, Value.FALSE);
    set(state, Ttl74198.S0, Value.UNKNOWN);
    set(state, Ttl74198.DSR, Value.TRUE);
    rise(register, state);

    assertBit(state, Ttl74198.Q0, Value.UNKNOWN);
    assertBit(state, Ttl74198.Q1, Value.FALSE);
    assertBit(state, Ttl74198.Q7, Value.FALSE);

    load(register, state, 0);
    set(state, Ttl74198.S1, Value.FALSE);
    set(state, Ttl74198.S0, Value.ERROR);
    set(state, Ttl74198.DSR, Value.TRUE);
    rise(register, state);
    assertBit(state, Ttl74198.Q0, Value.ERROR);
    assertBit(state, Ttl74198.Q1, Value.FALSE);
  }

  @Test
  void unknownSerialInputConflictsOnlyTheEntryStage() {
    final var register = new Ttl74198();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0x0F);

    shiftRight(register, state, Value.UNKNOWN);
    assertBit(state, Ttl74198.Q0, Value.UNKNOWN);
    assertBit(state, Ttl74198.Q1, Value.TRUE);
    assertBit(state, Ttl74198.Q4, Value.TRUE);
    assertBit(state, Ttl74198.Q5, Value.FALSE);

    load(register, state, 0xF0);
    shiftLeft(register, state, Value.ERROR);
    assertBit(state, Ttl74198.Q7, Value.ERROR);
    assertBit(state, Ttl74198.Q6, Value.TRUE);
    assertBit(state, Ttl74198.Q3, Value.TRUE);
    assertBit(state, Ttl74198.Q0, Value.FALSE);
  }

  @Test
  void unknownLoadedBitShiftsTowardQ7() {
    final var register = new Ttl74198();
    final var state = new TtlTestInstanceState(register, false);
    set(state, Ttl74198.MR, Value.TRUE);
    set(state, Ttl74198.S0, Value.TRUE);
    set(state, Ttl74198.S1, Value.TRUE);
    setData(state, 0);
    set(state, Ttl74198.D0, Value.TRUE);
    set(state, Ttl74198.D1, Value.UNKNOWN);
    rise(register, state);
    assertBit(state, Ttl74198.Q0, Value.TRUE);
    assertBit(state, Ttl74198.Q1, Value.UNKNOWN);
    assertBit(state, Ttl74198.Q2, Value.FALSE);

    shiftRight(register, state, Value.FALSE);
    assertBit(state, Ttl74198.Q0, Value.FALSE);
    assertBit(state, Ttl74198.Q1, Value.TRUE);
    assertBit(state, Ttl74198.Q2, Value.UNKNOWN);
    assertBit(state, Ttl74198.Q3, Value.FALSE);
  }

  @Test
  void unknownResetConflictsOnlyWhereTheStoredBitIsHigh() {
    final var register = new Ttl74198();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0x81);

    set(state, Ttl74198.MR, Value.UNKNOWN);
    register.propagate(state);
    assertBit(state, Ttl74198.Q0, Value.UNKNOWN);
    assertBit(state, Ttl74198.Q1, Value.FALSE);
    assertBit(state, Ttl74198.Q7, Value.UNKNOWN);

    load(register, state, 0x81);
    set(state, Ttl74198.MR, Value.ERROR);
    register.propagate(state);
    assertBit(state, Ttl74198.Q0, Value.ERROR);
    assertBit(state, Ttl74198.Q1, Value.FALSE);
    assertBit(state, Ttl74198.Q7, Value.ERROR);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var register = new Ttl74198();
    final var state = new TtlTestInstanceState(register, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    load(register, state, 0x66);
    assertWord(state, 0x66);

    state.setPortValue(VCC_PORT, Value.FALSE);
    register.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0x66);

    state.setPortValue(GND_PORT, Value.TRUE);
    register.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(GND_PORT, Value.FALSE);
    register.propagate(state);
    assertWord(state, 0x66);
  }

  private static void load(Ttl74198 register, TtlTestInstanceState state, int code) {
    set(state, Ttl74198.MR, Value.TRUE);
    set(state, Ttl74198.S0, Value.TRUE);
    set(state, Ttl74198.S1, Value.TRUE);
    set(state, Ttl74198.DSR, Value.FALSE);
    set(state, Ttl74198.DSL, Value.FALSE);
    setData(state, code);
    rise(register, state);
  }

  private static void shiftRight(Ttl74198 register, TtlTestInstanceState state, Value serial) {
    set(state, Ttl74198.MR, Value.TRUE);
    set(state, Ttl74198.S1, Value.FALSE);
    set(state, Ttl74198.S0, Value.TRUE);
    set(state, Ttl74198.DSR, serial);
    rise(register, state);
  }

  private static void shiftLeft(Ttl74198 register, TtlTestInstanceState state, Value serial) {
    set(state, Ttl74198.MR, Value.TRUE);
    set(state, Ttl74198.S1, Value.TRUE);
    set(state, Ttl74198.S0, Value.FALSE);
    set(state, Ttl74198.DSL, serial);
    rise(register, state);
  }

  private static void rise(Ttl74198 register, TtlTestInstanceState state) {
    set(state, Ttl74198.CP, Value.FALSE);
    register.propagate(state);
    set(state, Ttl74198.CP, Value.TRUE);
    register.propagate(state);
  }

  private static void setData(TtlTestInstanceState state, int code) {
    set(state, Ttl74198.D0, bit(code, 0));
    set(state, Ttl74198.D1, bit(code, 1));
    set(state, Ttl74198.D2, bit(code, 2));
    set(state, Ttl74198.D3, bit(code, 3));
    set(state, Ttl74198.D4, bit(code, 4));
    set(state, Ttl74198.D5, bit(code, 5));
    set(state, Ttl74198.D6, bit(code, 6));
    set(state, Ttl74198.D7, bit(code, 7));
  }

  private static Value bit(int code, int index) {
    return ((code >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
  }

  private static void set(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl74198.pinNrToPortNr(pin), value);
  }

  private static void assertWord(TtlTestInstanceState state, int code) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      assertBit(state, OUTPUTS[index], bit(code, index));
    }
  }

  private static void assertBit(TtlTestInstanceState state, byte pin, Value expected) {
    assertEquals(expected, state.getPortValue(Ttl74198.pinNrToPortNr(pin)), pinName(pin));
  }

  private static String pinName(byte pin) {
    return "pin " + pin;
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var output : OUTPUTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74198.pinNrToPortNr(output)));
    }
  }

  private static void assertPort(Instance instance, byte pin, int x, int y, int type) {
    final var port = Ttl74198.pinNrToPortNr(pin);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
