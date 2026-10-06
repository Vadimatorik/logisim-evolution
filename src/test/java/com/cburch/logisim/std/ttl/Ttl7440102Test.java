/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC40102 8-bit synchronous BCD down counter. */
class Ttl7440102Test {
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl7440102();
    final var hiddenPower = TtlTestInstanceState.createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl7440102.CP, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440102.MR, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440102.TE, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440102.P0, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440102.P1, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440102.P2, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440102.P3, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440102.PL, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440102.P4, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440102.P5, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440102.P6, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440102.P7, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440102.TC, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7440102.PE, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = TtlTestInstanceState.createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void clockIsTheFirstLogicalPort() {
    final var gate = new Ttl7440102();

    assertEquals(Ttl7440102.pinNrToPortNr(Ttl7440102.CP), gate.clockPinIndex(null)[0]);
    assertTrue(gate.checkForGatedClocks(null));
  }

  @Test
  void masterResetLoadsNinetyNineAndOverridesPreset() {
    final var gate = new Ttl7440102();
    final var state = new TtlTestInstanceState(gate, false);
    idle(state);
    set(state, Ttl7440102.TE, Value.FALSE);
    asyncLoad(gate, state, 0x00);
    assertCode(state, 0x00);

    set(state, Ttl7440102.PL, Value.FALSE);
    setJam(state, 0x00);
    set(state, Ttl7440102.MR, Value.FALSE);
    gate.propagate(state);
    assertCode(state, 0x99);
    assertTc(state, Value.TRUE);

    rise(gate, state);
    fall(gate, state);
    assertCode(state, 0x99);

    set(state, Ttl7440102.MR, Value.TRUE);
    gate.propagate(state);
    assertCode(state, 0x00);
    assertTc(state, Value.FALSE);
  }

  @Test
  void asynchronousPresetIsTransparentAndIgnoresTheClock() {
    final var gate = new Ttl7440102();
    final var state = new TtlTestInstanceState(gate, false);
    idle(state);
    asyncLoad(gate, state, 0x10);
    assertCode(state, 0x10);

    set(state, Ttl7440102.CP, Value.TRUE);
    gate.propagate(state);
    assertCode(state, 0x10);
    set(state, Ttl7440102.CP, Value.FALSE);
    gate.propagate(state);

    setJam(state, 0x25);
    gate.propagate(state);
    assertCode(state, 0x25);
    set(state, Ttl7440102.PL, Value.TRUE);
    gate.propagate(state);
    assertCode(state, 0x25);
  }

  @Test
  void synchronousPresetBeatsCountingAndEnableHolds() {
    final var gate = new Ttl7440102();
    final var state = new TtlTestInstanceState(gate, false);
    idle(state);
    asyncLoad(gate, state, 0x05);
    set(state, Ttl7440102.PL, Value.TRUE);
    set(state, Ttl7440102.TE, Value.FALSE);
    set(state, Ttl7440102.PE, Value.FALSE);
    setJam(state, 0x02);
    rise(gate, state);
    assertCode(state, 0x02);
    fall(gate, state);

    set(state, Ttl7440102.PE, Value.TRUE);
    set(state, Ttl7440102.TE, Value.TRUE);
    rise(gate, state);
    fall(gate, state);
    assertCode(state, 0x02);
    assertTc(state, Value.TRUE);

    set(state, Ttl7440102.TE, Value.FALSE);
    rise(gate, state);
    fall(gate, state);
    assertCode(state, 0x01);
  }

  @Test
  void countingBorrowsAcrossDecadesAndWrapsThroughNinetyNine() {
    final var gate = new Ttl7440102();
    final var state = new TtlTestInstanceState(gate, false);
    idle(state);
    asyncLoad(gate, state, 0x10);
    count(gate, state);
    assertCode(state, 0x09);
    assertTc(state, Value.TRUE);

    asyncLoad(gate, state, 0x20);
    assertClocksUntilZero(gate, state, 20);

    asyncLoad(gate, state, 0x00);
    set(state, Ttl7440102.TE, Value.FALSE);
    gate.propagate(state);
    assertTc(state, Value.FALSE);
    set(state, Ttl7440102.CP, Value.TRUE);
    gate.propagate(state);
    assertCode(state, 0x00);
    assertTc(state, Value.FALSE);
    set(state, Ttl7440102.CP, Value.FALSE);
    gate.propagate(state);

    count(gate, state);
    assertCode(state, 0x99);
    assertTc(state, Value.TRUE);
    assertClocksUntilZero(gate, state, 99);
    count(gate, state);
    assertCode(state, 0x99);
    assertTc(state, Value.TRUE);
  }

  @Test
  void terminalEnableGatesTerminalCountWithoutAClock() {
    final var gate = new Ttl7440102();
    final var state = new TtlTestInstanceState(gate, false);
    idle(state);
    asyncLoad(gate, state, 0x00);
    set(state, Ttl7440102.PL, Value.TRUE);
    set(state, Ttl7440102.TE, Value.TRUE);
    gate.propagate(state);
    assertTc(state, Value.TRUE);

    set(state, Ttl7440102.TE, Value.FALSE);
    gate.propagate(state);
    assertTc(state, Value.FALSE);
    assertCode(state, 0x00);
  }

  @Test
  void illegalNibblesDecrementUntilTheyWrapToNine() {
    final var gate = new Ttl7440102();
    final var state = new TtlTestInstanceState(gate, false);
    idle(state);

    asyncLoad(gate, state, 0x0A);
    count(gate, state);
    assertCode(state, 0x09);
    assertClocksUntilZero(gate, state, 9);

    asyncLoad(gate, state, 0x0F);
    count(gate, state);
    assertCode(state, 0x0E);
    assertClocksUntilZero(gate, state, 14);

    asyncLoad(gate, state, 0xA0);
    count(gate, state);
    assertCode(state, 0x99);
    assertClocksUntilZero(gate, state, 99);

    asyncLoad(gate, state, 0xFF);
    count(gate, state);
    assertCode(state, 0xFE);
    assertClocksUntilZero(gate, state, 164);
  }

  @Test
  void fallingAndUnknownClocksDoNotCount() {
    final var gate = new Ttl7440102();
    final var state = new TtlTestInstanceState(gate, false);
    idle(state);
    asyncLoad(gate, state, 0x04);
    count(gate, state);
    assertCode(state, 0x03);

    set(state, Ttl7440102.CP, Value.UNKNOWN);
    gate.propagate(state);
    set(state, Ttl7440102.CP, Value.TRUE);
    gate.propagate(state);
    assertCode(state, 0x03);
    set(state, Ttl7440102.CP, Value.FALSE);
    gate.propagate(state);
    count(gate, state);
    assertCode(state, 0x02);
  }

  @Test
  void unknownResetConflictsOnlyOnBitsThatDiffer() {
    final var gate = new Ttl7440102();
    final var state = new TtlTestInstanceState(gate, false);
    idle(state);
    asyncLoad(gate, state, 0x00);
    set(state, Ttl7440102.PL, Value.TRUE);
    set(state, Ttl7440102.TE, Value.TRUE);
    set(state, Ttl7440102.MR, Value.UNKNOWN);
    gate.propagate(state);

    assertPattern(stored(state), "X00XX00X");
    assertTc(state, Value.TRUE);

    set(state, Ttl7440102.TE, Value.FALSE);
    gate.propagate(state);
    assertTc(state, Value.UNKNOWN);

    set(state, Ttl7440102.MR, Value.ERROR);
    set(state, Ttl7440102.TE, Value.TRUE);
    gate.propagate(state);
    assertPattern(stored(state), "E00EE00E");
    assertTc(state, Value.TRUE);
  }

  @Test
  void unknownPresetAndJamFollowTheSameSubstitutionRule() {
    final var gate = new Ttl7440102();
    final var state = new TtlTestInstanceState(gate, false);
    idle(state);
    asyncLoad(gate, state, 0x01);
    set(state, Ttl7440102.PL, Value.UNKNOWN);
    setJam(state, 0x00);
    gate.propagate(state);
    assertPattern(stored(state), "0000000X");

    set(state, Ttl7440102.PL, Value.FALSE);
    set(state, Ttl7440102.P0, Value.UNKNOWN);
    set(state, Ttl7440102.P1, Value.TRUE);
    gate.propagate(state);
    assertPattern(stored(state), "0000001X");
    set(state, Ttl7440102.TE, Value.FALSE);
    gate.propagate(state);
    assertTc(state, Value.TRUE);
  }

  @Test
  void invalidExposedPowerInputsMakeTerminalCountUnknown() {
    final var gate = new Ttl7440102();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    idle(state);
    asyncLoad(gate, state, 0x08);
    assertCode(state, 0x08);
    assertTc(state, Value.TRUE);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertTc(state, Value.UNKNOWN);
    assertEquals(Value.createKnown(Ttl7440102.CODE_WIDTH, 0x08), stored(state));

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertCode(state, 0x08);
    assertTc(state, Value.TRUE);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertTc(state, Value.UNKNOWN);
  }

  private static void idle(TtlTestInstanceState state) {
    set(state, Ttl7440102.CP, Value.FALSE);
    set(state, Ttl7440102.MR, Value.TRUE);
    set(state, Ttl7440102.TE, Value.TRUE);
    set(state, Ttl7440102.PL, Value.TRUE);
    set(state, Ttl7440102.PE, Value.TRUE);
    setJam(state, 0x00);
  }

  private static void asyncLoad(Ttl7440102 gate, TtlTestInstanceState state, int code) {
    set(state, Ttl7440102.MR, Value.TRUE);
    set(state, Ttl7440102.PE, Value.TRUE);
    setJam(state, code);
    set(state, Ttl7440102.PL, Value.FALSE);
    gate.propagate(state);
  }

  private static void count(Ttl7440102 gate, TtlTestInstanceState state) {
    set(state, Ttl7440102.MR, Value.TRUE);
    set(state, Ttl7440102.PL, Value.TRUE);
    set(state, Ttl7440102.PE, Value.TRUE);
    set(state, Ttl7440102.TE, Value.FALSE);
    rise(gate, state);
    fall(gate, state);
  }

  private static void assertClocksUntilZero(
      Ttl7440102 gate, TtlTestInstanceState state, int clocks) {
    for (var left = clocks; left > 0; left--) {
      assertTc(state, Value.TRUE);
      count(gate, state);
    }
    assertCode(state, 0x00);
    assertTc(state, Value.FALSE);
  }

  private static void rise(Ttl7440102 gate, TtlTestInstanceState state) {
    set(state, Ttl7440102.CP, Value.TRUE);
    gate.propagate(state);
  }

  private static void fall(Ttl7440102 gate, TtlTestInstanceState state) {
    set(state, Ttl7440102.CP, Value.FALSE);
    gate.propagate(state);
  }

  private static void setJam(TtlTestInstanceState state, int code) {
    for (var bit = 0; bit < Ttl7440102.WIDTH; bit++) {
      set(state, Ttl7440102.JAM_PINS[bit], ((code >> bit) & 1) == 0 ? Value.FALSE : Value.TRUE);
    }
  }

  private static void set(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl7440102.pinNrToPortNr(pin), value);
  }

  private static Value stored(TtlTestInstanceState state) {
    return ((TtlRegisterData) state.getData()).getValue();
  }

  private static void assertCode(TtlTestInstanceState state, int code) {
    assertEquals(Value.createKnown(Ttl7440102.CODE_WIDTH, code), stored(state), hex(code));
    final var terminal =
        code == 0 && state.getPortValue(Ttl7440102.pinNrToPortNr(Ttl7440102.TE)) == Value.FALSE
            ? Value.FALSE
            : Value.TRUE;
    if (state.getPortValue(Ttl7440102.pinNrToPortNr(Ttl7440102.TE)) == Value.TRUE
        || state.getPortValue(Ttl7440102.pinNrToPortNr(Ttl7440102.TE)) == Value.FALSE) {
      assertTc(state, terminal);
    }
  }

  private static void assertTc(TtlTestInstanceState state, Value expected) {
    assertEquals(expected, state.getPortValue(Ttl7440102.pinNrToPortNr(Ttl7440102.TC)));
  }

  private static void assertPattern(Value actual, String msbFirst) {
    assertEquals(Ttl7440102.WIDTH, msbFirst.length());
    for (var bit = 0; bit < Ttl7440102.WIDTH; bit++) {
      final var symbol = msbFirst.charAt(Ttl7440102.WIDTH - 1 - bit);
      final var expected =
          switch (symbol) {
            case '0' -> Value.FALSE;
            case '1' -> Value.TRUE;
            case 'X' -> Value.UNKNOWN;
            case 'E' -> Value.ERROR;
            default -> throw new IllegalArgumentException("bad pattern symbol " + symbol);
          };
      assertEquals(expected, actual.get(bit), "bit " + bit + " of " + msbFirst);
    }
  }

  private static void assertPort(Instance instance, byte pin, int x, int y, int type) {
    final var index = Ttl7440102.pinNrToPortNr(pin);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static String hex(int code) {
    return String.format("%02X", code);
  }
}
