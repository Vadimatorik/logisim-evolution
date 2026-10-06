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

/** Functional tests for the 74F199 8-bit parallel-access shift register. */
class Ttl74199Test {
  private static final int GND_PORT = 22;
  private static final int VCC_PORT = 23;
  private static final byte[] OUTPUTS = {
    Ttl74199.Q0, Ttl74199.Q1, Ttl74199.Q2, Ttl74199.Q3,
    Ttl74199.Q4, Ttl74199.Q5, Ttl74199.Q6, Ttl74199.Q7
  };
  private static final byte[] DATA = {
    Ttl74199.D0, Ttl74199.D1, Ttl74199.D2, Ttl74199.D3,
    Ttl74199.D4, Ttl74199.D5, Ttl74199.D6, Ttl74199.D7
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var register = new Ttl74199();
    final var hiddenPower = createInstance(register, false);

    assertEquals(22, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74199.J, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.K, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.D0, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.Q0, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.D1, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.Q1, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.D2, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.Q2, 150, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.D3, 170, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.Q3, 190, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.CE, 210, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.CP, 230, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.MR, 210, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.Q4, 190, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.D4, 170, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.Q5, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.D5, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.Q6, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.D6, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.Q7, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.D7, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74199.PE, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(register, true);
    assertEquals(24, shownPower.getPorts().size());
    assertEquals(Location.create(230, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void clockIsTheCpPort() {
    final var register = new Ttl74199();
    assertTrue(register.checkForGatedClocks(null));
    assertArrayEquals(new int[] {Ttl74199.pinNrToPortNr(Ttl74199.CP)}, register.clockPinIndex(null));
  }

  @Test
  void masterResetClearsEveryStageAndReleasesToAHold() {
    final var register = new Ttl74199();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0xA5);

    set(state, Ttl74199.MR, Value.FALSE);
    register.propagate(state);
    assertWord(state, 0);

    set(state, Ttl74199.MR, Value.TRUE);
    set(state, Ttl74199.CE, Value.FALSE);
    set(state, Ttl74199.PE, Value.FALSE);
    setData(state, 0xFF);
    register.propagate(state);
    assertWord(state, 0);
  }

  @Test
  void masterResetOverridesARisingEdge() {
    final var register = new Ttl74199();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0xFF);

    set(state, Ttl74199.MR, Value.FALSE);
    set(state, Ttl74199.CE, Value.FALSE);
    set(state, Ttl74199.PE, Value.FALSE);
    setData(state, 0xA5);
    set(state, Ttl74199.CP, Value.FALSE);
    register.propagate(state);
    set(state, Ttl74199.CP, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0);

    set(state, Ttl74199.MR, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0);
  }

  @Test
  void parallelLoadStoresTheWordOnTheRisingEdge() {
    final var register = new Ttl74199();
    final var state = new TtlTestInstanceState(register, false);
    final var codes = new int[] {0x00, 0xFF, 0xA5, 0x5A, 0x01, 0x80};

    for (final var code : codes) {
      load(register, state, code);
      assertWord(state, code);
    }
  }

  @Test
  void parallelLoadIgnoresJAndK() {
    final var register = new Ttl74199();
    final var state = new TtlTestInstanceState(register, false);
    set(state, Ttl74199.MR, Value.TRUE);
    set(state, Ttl74199.CE, Value.FALSE);
    set(state, Ttl74199.PE, Value.FALSE);
    set(state, Ttl74199.J, Value.TRUE);
    set(state, Ttl74199.K, Value.FALSE);
    setData(state, 0x00);
    rise(register, state);
    assertWord(state, 0);
  }

  @Test
  void parallelLoadDoesNotHappenWithoutARisingEdge() {
    final var register = new Ttl74199();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0x3C);

    set(state, Ttl74199.CE, Value.FALSE);
    set(state, Ttl74199.PE, Value.FALSE);
    setData(state, 0xC3);
    register.propagate(state);
    assertWord(state, 0x3C);

    set(state, Ttl74199.CP, Value.FALSE);
    register.propagate(state);
    assertWord(state, 0x3C);

    set(state, Ttl74199.CP, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0xC3);
  }

  @Test
  void shiftMovesBitsTowardQ7() {
    final var register = new Ttl74199();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0x01);

    var word = 0x01;
    for (var step = 0; step < 8; step++) {
      shift(register, state, Value.FALSE, Value.FALSE);
      word = (word << 1) & 0xFF;
      assertWord(state, word);
    }
  }

  @Test
  void firstStageFollowsTheJkModes() {
    final var register = new Ttl74199();
    final var state = new TtlTestInstanceState(register, false);

    load(register, state, 0x01);
    shift(register, state, Value.TRUE, Value.TRUE);
    assertWord(state, 0x03);

    load(register, state, 0x01);
    shift(register, state, Value.FALSE, Value.FALSE);
    assertWord(state, 0x02);

    load(register, state, 0x01);
    shift(register, state, Value.TRUE, Value.FALSE);
    assertWord(state, 0x02);

    load(register, state, 0x01);
    shift(register, state, Value.FALSE, Value.TRUE);
    assertWord(state, 0x03);
  }

  @Test
  void tiedJAndKActAsADataInput() {
    final var register = new Ttl74199();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0);

    final var serial = new int[] {1, 1, 0, 1};
    var word = 0;
    for (final var bit : serial) {
      final var level = bit == 1 ? Value.TRUE : Value.FALSE;
      shift(register, state, level, level);
      word = ((word << 1) | bit) & 0xFF;
      assertWord(state, word);
    }
    assertWord(state, 0b1101);
  }

  @Test
  void highClockEnableHoldsThroughLoadAndShift() {
    final var register = new Ttl74199();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0xA5);

    set(state, Ttl74199.CE, Value.TRUE);
    set(state, Ttl74199.PE, Value.FALSE);
    setData(state, 0x5A);
    rise(register, state);
    assertWord(state, 0xA5);

    set(state, Ttl74199.PE, Value.TRUE);
    set(state, Ttl74199.J, Value.TRUE);
    set(state, Ttl74199.K, Value.TRUE);
    rise(register, state);
    assertWord(state, 0xA5);
  }

  @Test
  void fallingAndUnknownClocksDoNotShift() {
    final var register = new Ttl74199();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0x0A);
    set(state, Ttl74199.CE, Value.FALSE);
    set(state, Ttl74199.PE, Value.TRUE);
    set(state, Ttl74199.J, Value.FALSE);
    set(state, Ttl74199.K, Value.FALSE);

    set(state, Ttl74199.CP, Value.FALSE);
    register.propagate(state);
    assertWord(state, 0x0A);

    set(state, Ttl74199.CP, Value.UNKNOWN);
    register.propagate(state);
    assertWord(state, 0x0A);

    set(state, Ttl74199.CP, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0x0A);

    set(state, Ttl74199.CP, Value.FALSE);
    register.propagate(state);
    set(state, Ttl74199.CP, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0x14);
  }

  @Test
  void unknownParallelEnableConflictsOnlyWhereLoadAndShiftDiffer() {
    final var register = new Ttl74199();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0);
    set(state, Ttl74199.CE, Value.FALSE);
    set(state, Ttl74199.J, Value.FALSE);
    set(state, Ttl74199.K, Value.FALSE);
    setData(state, 0x01);

    set(state, Ttl74199.PE, Value.UNKNOWN);
    rise(register, state);
    assertBit(state, Ttl74199.Q0, Value.UNKNOWN);
    assertBit(state, Ttl74199.Q1, Value.FALSE);
    assertBit(state, Ttl74199.Q7, Value.FALSE);

    load(register, state, 0);
    set(state, Ttl74199.CE, Value.FALSE);
    set(state, Ttl74199.J, Value.FALSE);
    set(state, Ttl74199.K, Value.FALSE);
    setData(state, 0x01);
    set(state, Ttl74199.PE, Value.ERROR);
    rise(register, state);
    assertBit(state, Ttl74199.Q0, Value.ERROR);
    assertBit(state, Ttl74199.Q1, Value.FALSE);
    assertBit(state, Ttl74199.Q7, Value.FALSE);
  }

  @Test
  void unknownClockEnableConflictsOnlyWhereHoldAndLoadDiffer() {
    final var register = new Ttl74199();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0);
    set(state, Ttl74199.PE, Value.FALSE);
    setData(state, 0x01);

    set(state, Ttl74199.CE, Value.UNKNOWN);
    rise(register, state);
    assertBit(state, Ttl74199.Q0, Value.UNKNOWN);
    assertBit(state, Ttl74199.Q1, Value.FALSE);

    load(register, state, 0);
    set(state, Ttl74199.PE, Value.FALSE);
    setData(state, 0x01);
    set(state, Ttl74199.CE, Value.ERROR);
    rise(register, state);
    assertBit(state, Ttl74199.Q0, Value.ERROR);
    assertBit(state, Ttl74199.Q1, Value.FALSE);
  }

  @Test
  void unknownJConflictsOnlyTheFirstStage() {
    final var register = new Ttl74199();
    final var state = new TtlTestInstanceState(register, false);
    load(register, state, 0);
    set(state, Ttl74199.CE, Value.FALSE);
    set(state, Ttl74199.PE, Value.TRUE);
    set(state, Ttl74199.K, Value.TRUE);
    set(state, Ttl74199.J, Value.UNKNOWN);
    rise(register, state);

    assertBit(state, Ttl74199.Q0, Value.UNKNOWN);
    assertBit(state, Ttl74199.Q1, Value.FALSE);
    assertBit(state, Ttl74199.Q7, Value.FALSE);

    load(register, state, 0x01);
    set(state, Ttl74199.CE, Value.FALSE);
    set(state, Ttl74199.PE, Value.TRUE);
    set(state, Ttl74199.K, Value.TRUE);
    set(state, Ttl74199.J, Value.UNKNOWN);
    rise(register, state);
    assertBit(state, Ttl74199.Q0, Value.TRUE);
    assertBit(state, Ttl74199.Q1, Value.TRUE);

    load(register, state, 0);
    set(state, Ttl74199.CE, Value.FALSE);
    set(state, Ttl74199.PE, Value.TRUE);
    set(state, Ttl74199.K, Value.TRUE);
    set(state, Ttl74199.J, Value.ERROR);
    rise(register, state);
    assertBit(state, Ttl74199.Q0, Value.ERROR);
    assertBit(state, Ttl74199.Q1, Value.FALSE);
  }

  @Test
  void unknownLoadedBitShiftsTowardQ7() {
    final var register = new Ttl74199();
    final var state = new TtlTestInstanceState(register, false);
    set(state, Ttl74199.MR, Value.TRUE);
    set(state, Ttl74199.CE, Value.FALSE);
    set(state, Ttl74199.PE, Value.FALSE);
    set(state, Ttl74199.D0, Value.TRUE);
    set(state, Ttl74199.D1, Value.UNKNOWN);
    for (var bit = 2; bit < 8; bit++) {
      set(state, DATA[bit], Value.FALSE);
    }
    rise(register, state);
    assertBit(state, Ttl74199.Q0, Value.TRUE);
    assertBit(state, Ttl74199.Q1, Value.UNKNOWN);
    assertBit(state, Ttl74199.Q7, Value.FALSE);

    shift(register, state, Value.FALSE, Value.FALSE);
    assertBit(state, Ttl74199.Q0, Value.FALSE);
    assertBit(state, Ttl74199.Q1, Value.TRUE);
    assertBit(state, Ttl74199.Q2, Value.UNKNOWN);
    assertBit(state, Ttl74199.Q3, Value.FALSE);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var register = new Ttl74199();
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

  private static void load(Ttl74199 register, TtlTestInstanceState state, int code) {
    set(state, Ttl74199.MR, Value.TRUE);
    set(state, Ttl74199.CE, Value.FALSE);
    set(state, Ttl74199.PE, Value.FALSE);
    set(state, Ttl74199.J, Value.FALSE);
    set(state, Ttl74199.K, Value.FALSE);
    setData(state, code);
    rise(register, state);
  }

  private static void shift(Ttl74199 register, TtlTestInstanceState state, Value jPin, Value kPin) {
    set(state, Ttl74199.MR, Value.TRUE);
    set(state, Ttl74199.CE, Value.FALSE);
    set(state, Ttl74199.PE, Value.TRUE);
    set(state, Ttl74199.J, jPin);
    set(state, Ttl74199.K, kPin);
    rise(register, state);
  }

  private static void rise(Ttl74199 register, TtlTestInstanceState state) {
    set(state, Ttl74199.CP, Value.FALSE);
    register.propagate(state);
    set(state, Ttl74199.CP, Value.TRUE);
    register.propagate(state);
  }

  private static void setData(TtlTestInstanceState state, int code) {
    for (var bit = 0; bit < 8; bit++) {
      set(state, DATA[bit], bit(code, bit));
    }
  }

  private static Value bit(int code, int index) {
    return ((code >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
  }

  private static void set(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl74199.pinNrToPortNr(pin), value);
  }

  private static void assertWord(TtlTestInstanceState state, int code) {
    for (var bit = 0; bit < 8; bit++) {
      assertBit(state, OUTPUTS[bit], bit(code, bit));
    }
  }

  private static void assertBit(TtlTestInstanceState state, byte pin, Value expected) {
    assertEquals(expected, state.getPortValue(Ttl74199.pinNrToPortNr(pin)));
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var output : OUTPUTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74199.pinNrToPortNr(output)));
    }
  }

  private static void assertPort(Instance instance, byte pin, int x, int y, int type) {
    final var port = Ttl74199.pinNrToPortNr(pin);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
