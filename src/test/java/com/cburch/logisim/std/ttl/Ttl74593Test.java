/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC593 8-bit counter with input register and 3-state I/O. */
class Ttl74593Test {
  private static final byte[] BUS = {
    Ttl74593.QA, Ttl74593.QB, Ttl74593.QC, Ttl74593.QD,
    Ttl74593.QE, Ttl74593.QF, Ttl74593.QG, Ttl74593.QH
  };

  private static final int GND_PORT = 18;
  private static final int VCC_PORT = 19;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74593();
    final var hiddenPower = TtlTestInstanceState.createInstance(gate, false);

    assertEquals(18, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74593.QA, 10, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74593.QB, 30, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74593.QC, 50, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74593.QD, 70, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74593.QE, 90, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74593.QF, 110, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74593.QG, 130, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74593.QH, 150, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74593.NCLOAD, 170, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74593.NRCO, 190, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74593.NCCLR, 170, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74593.CCK, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74593.NCCKEN, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74593.CCKEN, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74593.RCK, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74593.NRCKEN, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74593.NG, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74593.G, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = TtlTestInstanceState.createInstance(gate, true);
    assertEquals(20, shownPower.getPorts().size());
    assertEquals(Location.create(190, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void bothClocksAreReported() {
    final var gate = new Ttl74593();
    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(
        new int[] {port(Ttl74593.CCK), port(Ttl74593.RCK)}, gate.clockPinIndex(null));
  }

  @Test
  void clearIsAsynchronousAndLeavesTheRegister() {
    final var gate = new Ttl74593();
    final var state = idle(gate);
    loadRegister(gate, state, 0x5A);
    transfer(gate, state);
    show(gate, state);
    assertCount(state, 0x5A);

    clear(gate, state);
    show(gate, state);
    assertCount(state, 0x00);

    transfer(gate, state);
    show(gate, state);
    assertCount(state, 0x5A);
  }

  @Test
  void clearWinsWhenLoadIsAlsoLow() {
    final var gate = new Ttl74593();
    final var state = idle(gate);
    loadRegister(gate, state, 0xA5);
    set(state, Ttl74593.NCLOAD, Value.FALSE);
    set(state, Ttl74593.NCCLR, Value.FALSE);
    gate.propagate(state);

    show(gate, state);
    assertCount(state, 0x00);
  }

  @Test
  void counterLoadCopiesTheRegisterRatherThanThePins() {
    final var gate = new Ttl74593();
    final var state = idle(gate);
    loadRegister(gate, state, 0xA5);
    transfer(gate, state);

    hide(gate, state);
    setBus(state, 0x5A);
    set(state, Ttl74593.NRCKEN, Value.TRUE);
    transfer(gate, state);
    show(gate, state);
    assertCount(state, 0xA5);
  }

  @Test
  void registerAndCounterUpdateInTheSameStep() {
    final var gate = new Ttl74593();
    final var state = idle(gate);
    loadRegister(gate, state, 0x00);
    transfer(gate, state);

    hide(gate, state);
    set(state, Ttl74593.NRCKEN, Value.FALSE);
    set(state, Ttl74593.NCLOAD, Value.FALSE);
    set(state, Ttl74593.RCK, Value.FALSE);
    setBus(state, 0x77);
    gate.propagate(state);
    setBus(state, 0x77);
    set(state, Ttl74593.RCK, Value.TRUE);
    gate.propagate(state);

    show(gate, state);
    assertCount(state, 0x77);
  }

  @Test
  void fallingRegisterClockDoesNotLoad() {
    final var gate = new Ttl74593();
    final var state = idle(gate);
    hide(gate, state);
    set(state, Ttl74593.NRCKEN, Value.FALSE);
    set(state, Ttl74593.RCK, Value.FALSE);
    setBus(state, 0x11);
    gate.propagate(state);
    setBus(state, 0x11);
    set(state, Ttl74593.RCK, Value.TRUE);
    gate.propagate(state);
    setBus(state, 0x22);
    gate.propagate(state);
    set(state, Ttl74593.RCK, Value.FALSE);
    setBus(state, 0x22);
    gate.propagate(state);

    transfer(gate, state);
    show(gate, state);
    assertCount(state, 0x11);
  }

  @Test
  void countingFollowsEitherEnableAndWrapsWithActiveLowCarry() {
    final var gate = new Ttl74593();
    final var state = idle(gate);
    loadRegister(gate, state, 0x12);
    transfer(gate, state);
    armCount(state, false, true);
    show(gate, state);
    pulseCounter(gate, state);
    assertCount(state, 0x12);

    armCount(state, false, false);
    pulseCounter(gate, state);
    assertCount(state, 0x13);

    armCount(state, true, true);
    pulseCounter(gate, state);
    assertCount(state, 0x14);

    armCount(state, true, false);
    pulseCounter(gate, state);
    assertCount(state, 0x15);

    loadRegister(gate, state, 0xFE);
    transfer(gate, state);
    armCount(state, true, true);
    show(gate, state);
    assertCount(state, 0xFE);
    pulseCounter(gate, state);
    assertCount(state, 0xFF);
    assertEquals(Value.FALSE, carry(state));
    pulseCounter(gate, state);
    assertCount(state, 0x00);
    assertEquals(Value.TRUE, carry(state));
  }

  @Test
  void onlyOneOutputEnableCombinationDrivesTheBus() {
    final var gate = new Ttl74593();
    final var state = idle(gate);
    clear(gate, state);

    driveEnable(gate, state, false, false);
    assertReleased(state);
    driveEnable(gate, state, false, true);
    assertReleased(state);
    driveEnable(gate, state, true, true);
    assertReleased(state);
    driveEnable(gate, state, true, false);
    assertCount(state, 0x00);

    loadRegister(gate, state, 0xFF);
    transfer(gate, state);
    driveEnable(gate, state, true, false);
    assertCount(state, 0xFF);
    assertEquals(Value.FALSE, carry(state));
    driveEnable(gate, state, false, false);
    assertReleased(state);
    assertEquals(Value.FALSE, carry(state));
  }

  @Test
  void unknownClearAffectsOnlyTheBitsThatCouldChange() {
    final var gate = new Ttl74593();
    final var state = idle(gate);
    loadRegister(gate, state, 0x01);
    transfer(gate, state);
    show(gate, state);

    set(state, Ttl74593.NCCLR, Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, busBit(state, 0));
    assertEquals(Value.FALSE, busBit(state, 1));
    assertEquals(Value.TRUE, carry(state));
  }

  @Test
  void errorClearPoisonsOnlyDisagreements() {
    final var gate = new Ttl74593();
    final var state = idle(gate);
    clear(gate, state);
    show(gate, state);
    set(state, Ttl74593.NCCLR, Value.ERROR);
    gate.propagate(state);
    assertCount(state, 0x00);
    assertEquals(Value.TRUE, carry(state));

    set(state, Ttl74593.NCCLR, Value.TRUE);
    loadRegister(gate, state, 0x01);
    transfer(gate, state);
    show(gate, state);
    set(state, Ttl74593.NCCLR, Value.ERROR);
    gate.propagate(state);
    assertEquals(Value.ERROR, busBit(state, 0));
    assertEquals(Value.FALSE, busBit(state, 1));
    assertEquals(Value.TRUE, carry(state));
  }

  @Test
  void unknownCountEnableMakesOnlyTheToggledBitUnknown() {
    final var gate = new Ttl74593();
    final var state = idle(gate);
    clear(gate, state);
    show(gate, state);
    set(state, Ttl74593.NCCKEN, Value.TRUE);
    set(state, Ttl74593.CCKEN, Value.UNKNOWN);
    set(state, Ttl74593.CCK, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl74593.CCK, Value.TRUE);
    gate.propagate(state);

    assertEquals(Value.UNKNOWN, busBit(state, 0));
    assertEquals(Value.FALSE, busBit(state, 1));
    assertEquals(Value.TRUE, carry(state));
  }

  @Test
  void errorOnTheOutputEnableMakesADrivenBusAnError() {
    final var gate = new Ttl74593();
    final var state = idle(gate);
    clear(gate, state);
    set(state, Ttl74593.NG, Value.TRUE);
    set(state, Ttl74593.G, Value.ERROR);
    gate.propagate(state);
    assertReleased(state);

    set(state, Ttl74593.NG, Value.FALSE);
    set(state, Ttl74593.G, Value.ERROR);
    gate.propagate(state);
    for (var bit = 0; bit < BUS.length; bit++) {
      assertEquals(Value.ERROR, busBit(state, bit));
    }
    assertEquals(Value.TRUE, carry(state));
  }

  @Test
  void invalidPowerReleasesTheBusWithoutForgettingTheCount() {
    final var gate = new Ttl74593();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    hold(state);
    gate.propagate(state);
    loadRegister(gate, state, 0x3C);
    transfer(gate, state);
    show(gate, state);
    assertCount(state, 0x3C);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertReleased(state);
    assertEquals(Value.UNKNOWN, carry(state));

    state.setPortValue(VCC_PORT, Value.TRUE);
    show(gate, state);
    assertCount(state, 0x3C);
    assertEquals(Value.TRUE, carry(state));
  }

  private static void assertPort(Instance instance, byte pin, int x, int y, int type) {
    final var index = port(pin);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static TtlTestInstanceState idle(Ttl74593 gate) {
    final var state = new TtlTestInstanceState(gate, false);
    hold(state);
    gate.propagate(state);
    return state;
  }

  private static void hold(TtlTestInstanceState state) {
    set(state, Ttl74593.NCLOAD, Value.TRUE);
    set(state, Ttl74593.NCCLR, Value.TRUE);
    set(state, Ttl74593.CCK, Value.FALSE);
    set(state, Ttl74593.NCCKEN, Value.TRUE);
    set(state, Ttl74593.CCKEN, Value.FALSE);
    set(state, Ttl74593.RCK, Value.FALSE);
    set(state, Ttl74593.NRCKEN, Value.TRUE);
    set(state, Ttl74593.NG, Value.TRUE);
    set(state, Ttl74593.G, Value.FALSE);
    setBus(state, 0);
  }

  private static void hide(Ttl74593 gate, TtlTestInstanceState state) {
    set(state, Ttl74593.NG, Value.TRUE);
    set(state, Ttl74593.G, Value.FALSE);
    gate.propagate(state);
  }

  private static void show(Ttl74593 gate, TtlTestInstanceState state) {
    set(state, Ttl74593.NG, Value.FALSE);
    set(state, Ttl74593.G, Value.TRUE);
    gate.propagate(state);
  }

  private static void driveEnable(
      Ttl74593 gate, TtlTestInstanceState state, boolean g, boolean gBar) {
    set(state, Ttl74593.G, g ? Value.TRUE : Value.FALSE);
    set(state, Ttl74593.NG, gBar ? Value.TRUE : Value.FALSE);
    gate.propagate(state);
  }

  private static void clear(Ttl74593 gate, TtlTestInstanceState state) {
    set(state, Ttl74593.NCCLR, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl74593.NCCLR, Value.TRUE);
    gate.propagate(state);
  }

  private static void loadRegister(Ttl74593 gate, TtlTestInstanceState state, int code) {
    hide(gate, state);
    set(state, Ttl74593.NRCKEN, Value.FALSE);
    set(state, Ttl74593.RCK, Value.FALSE);
    setBus(state, code);
    gate.propagate(state);
    setBus(state, code);
    set(state, Ttl74593.RCK, Value.TRUE);
    gate.propagate(state);
    set(state, Ttl74593.RCK, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl74593.NRCKEN, Value.TRUE);
    gate.propagate(state);
  }

  private static void transfer(Ttl74593 gate, TtlTestInstanceState state) {
    set(state, Ttl74593.NCLOAD, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl74593.NCLOAD, Value.TRUE);
    gate.propagate(state);
  }

  private static void armCount(TtlTestInstanceState state, boolean ccken, boolean nccken) {
    set(state, Ttl74593.CCKEN, ccken ? Value.TRUE : Value.FALSE);
    set(state, Ttl74593.NCCKEN, nccken ? Value.TRUE : Value.FALSE);
  }

  private static void pulseCounter(Ttl74593 gate, TtlTestInstanceState state) {
    set(state, Ttl74593.CCK, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl74593.CCK, Value.TRUE);
    gate.propagate(state);
    set(state, Ttl74593.CCK, Value.FALSE);
    gate.propagate(state);
  }

  private static void assertCount(TtlTestInstanceState state, int code) {
    for (var bit = 0; bit < BUS.length; bit++) {
      final var expected = ((code >> bit) & 1) == 0 ? Value.FALSE : Value.TRUE;
      assertEquals(expected, busBit(state, bit), "bit " + bit + " of " + code);
    }
    assertEquals(code == 0xFF ? Value.FALSE : Value.TRUE, carry(state));
  }

  private static void assertReleased(TtlTestInstanceState state) {
    for (var bit = 0; bit < BUS.length; bit++) {
      assertEquals(Value.UNKNOWN, busBit(state, bit));
    }
  }

  private static Value busBit(TtlTestInstanceState state, int bit) {
    return state.getPortValue(port(BUS[bit]));
  }

  private static Value carry(TtlTestInstanceState state) {
    return state.getPortValue(port(Ttl74593.NRCO));
  }

  private static void setBus(TtlTestInstanceState state, int code) {
    for (var bit = 0; bit < BUS.length; bit++) {
      final var value = ((code >> bit) & 1) == 0 ? Value.FALSE : Value.TRUE;
      set(state, BUS[bit], value);
    }
  }

  private static void set(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(port(pin), value);
  }

  private static int port(byte pin) {
    return Ttl74593.pinNrToPortNr(pin);
  }
}
