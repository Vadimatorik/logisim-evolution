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

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC592 input register and 8-bit counter. */
class Ttl74592Test {
  private static final BitWidth WIDTH = BitWidth.create(Ttl74592.WIDTH);
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74592();
    final var hiddenPower = TtlTestInstanceState.createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74592.A, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74592.B, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74592.C, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74592.D, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74592.E, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74592.F, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74592.G, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74592.RCO, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74592.CCLR, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74592.CCK, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74592.CCKEN, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74592.RCK, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74592.CLOAD, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74592.H, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = TtlTestInstanceState.createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void registerStoresInputsOnlyOnTheRisingRegisterClock() {
    final var gate = new Ttl74592();
    final var state = knownState(gate, false);

    setData(state, 0xA5);
    gate.propagate(state);
    assertRegister(state, 0);
    assertCounter(state, 0);

    state.setPortValue(port(Ttl74592.RCK), Value.FALSE);
    gate.propagate(state);
    assertRegister(state, 0);

    rise(gate, state, Ttl74592.RCK);
    assertRegister(state, 0xA5);
    assertCounter(state, 0);
    assertEquals(Value.FALSE, carry(state));

    fall(gate, state, Ttl74592.RCK);
    assertRegister(state, 0xA5);
  }

  @Test
  void counterLoadIsTransparentAndClearLeavesTheRegister() {
    final var gate = new Ttl74592();
    final var state = knownState(gate, false);

    loadRegister(gate, state, 0x3C);
    assertCounter(state, 0);
    state.setPortValue(port(Ttl74592.CLOAD), Value.FALSE);
    gate.propagate(state);
    assertCounter(state, 0x3C);
    assertEquals(Value.FALSE, carry(state));

    setData(state, 0xFF);
    rise(gate, state, Ttl74592.RCK);
    assertRegister(state, 0xFF);
    assertCounter(state, 0xFF);
    assertEquals(Value.TRUE, carry(state));
    fall(gate, state, Ttl74592.RCK);

    state.setPortValue(port(Ttl74592.CLOAD), Value.TRUE);
    gate.propagate(state);
    setData(state, 0x01);
    rise(gate, state, Ttl74592.RCK);
    assertRegister(state, 0x01);
    assertCounter(state, 0xFF);

    state.setPortValue(port(Ttl74592.CCLR), Value.FALSE);
    gate.propagate(state);
    assertCounter(state, 0);
    assertRegister(state, 0x01);
    assertEquals(Value.FALSE, carry(state));

    state.setPortValue(port(Ttl74592.CCLR), Value.TRUE);
    gate.propagate(state);
    assertCounter(state, 0);
    state.setPortValue(port(Ttl74592.CLOAD), Value.FALSE);
    gate.propagate(state);
    assertCounter(state, 0x01);
  }

  @Test
  void clearOverridesLoadAndTheCounterClock() {
    final var gate = new Ttl74592();
    final var state = knownState(gate, false);
    loadCounter(gate, state, 0xFF);
    assertEquals(Value.TRUE, carry(state));

    state.setPortValue(port(Ttl74592.CLOAD), Value.FALSE);
    state.setPortValue(port(Ttl74592.CCLR), Value.FALSE);
    state.setPortValue(port(Ttl74592.CCKEN), Value.FALSE);
    rise(gate, state, Ttl74592.CCK);
    assertCounter(state, 0);
    assertRegister(state, 0xFF);
    assertEquals(Value.FALSE, carry(state));

    fall(gate, state, Ttl74592.CCK);
    state.setPortValue(port(Ttl74592.CCLR), Value.TRUE);
    gate.propagate(state);
    assertCounter(state, 0xFF);
    assertEquals(Value.TRUE, carry(state));
  }

  @Test
  void counterAdvancesOnlyWhileTheEnableIsLowAtTheRisingClock() {
    final var gate = new Ttl74592();
    final var state = knownState(gate, false);
    loadCounter(gate, state, 0x01);
    hold(state);
    gate.propagate(state);

    rise(gate, state, Ttl74592.CCK);
    assertCounter(state, 0x01);
    state.setPortValue(port(Ttl74592.CCKEN), Value.FALSE);
    gate.propagate(state);
    assertCounter(state, 0x01);
    fall(gate, state, Ttl74592.CCK);

    enableCount(state);
    for (var expected = 2; expected <= 8; expected++) {
      pulseCount(gate, state);
      assertCounter(state, expected);
    }

    state.setPortValue(port(Ttl74592.CCK), Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(port(Ttl74592.CCK), Value.FALSE);
    gate.propagate(state);
    assertCounter(state, 8);

    loadCounter(gate, state, 0xFE);
    enableCount(state);
    pulseCount(gate, state);
    assertCounter(state, 0xFF);
    assertEquals(Value.TRUE, carry(state));
    pulseCount(gate, state);
    assertCounter(state, 0);
    assertEquals(Value.FALSE, carry(state));
  }

  @Test
  void rippleCarryIsHighOnlyAtTerminalCount() {
    final var gate = new Ttl74592();
    final var state = knownState(gate, false);
    loadCounter(gate, state, 0);
    enableCount(state);
    assertEquals(Value.FALSE, carry(state));

    for (var count = 0; count < 255; count++) {
      assertEquals(Value.FALSE, carry(state), "carry at " + count);
      pulseCount(gate, state);
    }
    assertCounter(state, 255);
    assertEquals(Value.TRUE, carry(state));
    pulseCount(gate, state);
    assertCounter(state, 0);
    assertEquals(Value.FALSE, carry(state));
  }

  @Test
  void inputAIsTheLeastSignificantBit() {
    final var gate = new Ttl74592();
    final var state = knownState(gate, false);
    loadCounter(gate, state, 0x01);
    enableCount(state);

    for (var clocks = 0; clocks < 254; clocks++) {
      assertEquals(Value.FALSE, carry(state));
      pulseCount(gate, state);
    }
    assertCounter(state, 255);
    assertEquals(Value.TRUE, carry(state));
  }

  @Test
  void unknownAndErrorControlsChangeOnlyDisagreements() {
    final var gate = new Ttl74592();
    final var state = knownState(gate, false);
    loadCounter(gate, state, 0);
    hold(state);

    state.setPortValue(port(Ttl74592.CCLR), Value.UNKNOWN);
    gate.propagate(state);
    assertCounter(state, 0);
    assertEquals(Value.FALSE, carry(state));

    loadCounter(gate, state, 0x01);
    hold(state);
    state.setPortValue(port(Ttl74592.CCLR), Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, counter(state).get(0));
    assertEquals(Value.FALSE, counter(state).get(1));
    assertEquals(Value.FALSE, carry(state));

    loadCounter(gate, state, 0xFF);
    hold(state);
    state.setPortValue(port(Ttl74592.CCLR), Value.ERROR);
    gate.propagate(state);
    assertEquals(Value.ERROR, counter(state).get(0));
    assertEquals(Value.ERROR, counter(state).get(7));
    assertEquals(Value.ERROR, carry(state));

    loadCounter(gate, state, 0);
    hold(state);
    state.setPortValue(port(Ttl74592.CCLR), Value.ERROR);
    gate.propagate(state);
    assertCounter(state, 0);

    loadCounter(gate, state, 0xFE);
    enableCount(state);
    state.setPortValue(port(Ttl74592.CCKEN), Value.UNKNOWN);
    rise(gate, state, Ttl74592.CCK);
    assertEquals(Value.UNKNOWN, counter(state).get(0));
    assertEquals(Value.TRUE, counter(state).get(1));
    assertEquals(Value.UNKNOWN, carry(state));

    loadCounter(gate, state, 0x11);
    hold(state);
    state.setPortValue(port(Ttl74592.CLOAD), Value.UNKNOWN);
    gate.propagate(state);
    assertCounter(state, 0x11);

    setData(state, 0x00);
    rise(gate, state, Ttl74592.RCK);
    state.setPortValue(port(Ttl74592.CLOAD), Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, counter(state).get(0));
    assertEquals(Value.UNKNOWN, counter(state).get(4));
  }

  @Test
  void unknownDataIsStoredOnlyForTheBitsThatAreNotKnown() {
    final var gate = new Ttl74592();
    final var state = knownState(gate, false);
    setData(state, 0xFF);
    state.setPortValue(port(Ttl74592.A), Value.UNKNOWN);
    state.setPortValue(port(Ttl74592.H), Value.ERROR);
    rise(gate, state, Ttl74592.RCK);
    assertEquals(Value.UNKNOWN, register(state).get(0));
    assertEquals(Value.TRUE, register(state).get(1));
    assertEquals(Value.TRUE, register(state).get(6));
    assertEquals(Value.ERROR, register(state).get(7));

    state.setPortValue(port(Ttl74592.CLOAD), Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, counter(state).get(0));
    assertEquals(Value.ERROR, counter(state).get(7));
    assertEquals(Value.ERROR, carry(state));
  }

  @Test
  void invalidExposedPowerInputsMakeTheCarryUnknown() {
    final var gate = new Ttl74592();
    final var state = knownState(gate, true);
    loadCounter(gate, state, 0xFF);
    assertEquals(Value.TRUE, carry(state));

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, carry(state));

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.TRUE, carry(state));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, carry(state));
  }

  private static void assertPort(Instance instance, byte pin, int x, int y, int type) {
    final var index = Ttl74592.pinNrToPortNr(pin);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static TtlTestInstanceState knownState(Ttl74592 gate, boolean showPower) {
    final var state = new TtlTestInstanceState(gate, showPower);
    if (showPower) {
      state.setPortValue(GND_PORT, Value.FALSE);
      state.setPortValue(VCC_PORT, Value.TRUE);
    }
    setData(state, 0);
    hold(state);
    state.setPortValue(port(Ttl74592.CCK), Value.FALSE);
    state.setPortValue(port(Ttl74592.RCK), Value.FALSE);
    gate.propagate(state);
    state.setPortValue(port(Ttl74592.CCLR), Value.FALSE);
    gate.propagate(state);
    state.setPortValue(port(Ttl74592.CCLR), Value.TRUE);
    gate.propagate(state);
    pulseRegister(gate, state);
    return state;
  }

  private static void hold(TtlTestInstanceState state) {
    state.setPortValue(port(Ttl74592.CCLR), Value.TRUE);
    state.setPortValue(port(Ttl74592.CLOAD), Value.TRUE);
    state.setPortValue(port(Ttl74592.CCKEN), Value.TRUE);
  }

  private static void enableCount(TtlTestInstanceState state) {
    state.setPortValue(port(Ttl74592.CCLR), Value.TRUE);
    state.setPortValue(port(Ttl74592.CLOAD), Value.TRUE);
    state.setPortValue(port(Ttl74592.CCKEN), Value.FALSE);
  }

  private static void setData(TtlTestInstanceState state, int code) {
    final byte[] pins = {
      Ttl74592.A, Ttl74592.B, Ttl74592.C, Ttl74592.D,
      Ttl74592.E, Ttl74592.F, Ttl74592.G, Ttl74592.H
    };
    for (var bit = 0; bit < pins.length; bit++) {
      final var high = ((code >> bit) & 1) == 1;
      state.setPortValue(port(pins[bit]), high ? Value.TRUE : Value.FALSE);
    }
  }

  private static void loadRegister(Ttl74592 gate, TtlTestInstanceState state, int code) {
    hold(state);
    setData(state, code);
    pulseRegister(gate, state);
  }

  private static void loadCounter(Ttl74592 gate, TtlTestInstanceState state, int code) {
    loadRegister(gate, state, code);
    state.setPortValue(port(Ttl74592.CLOAD), Value.FALSE);
    gate.propagate(state);
    state.setPortValue(port(Ttl74592.CLOAD), Value.TRUE);
    gate.propagate(state);
  }

  private static void pulseRegister(Ttl74592 gate, TtlTestInstanceState state) {
    rise(gate, state, Ttl74592.RCK);
    fall(gate, state, Ttl74592.RCK);
  }

  private static void pulseCount(Ttl74592 gate, TtlTestInstanceState state) {
    rise(gate, state, Ttl74592.CCK);
    fall(gate, state, Ttl74592.CCK);
  }

  private static void rise(Ttl74592 gate, TtlTestInstanceState state, byte clock) {
    state.setPortValue(port(clock), Value.TRUE);
    gate.propagate(state);
  }

  private static void fall(Ttl74592 gate, TtlTestInstanceState state, byte clock) {
    state.setPortValue(port(clock), Value.FALSE);
    gate.propagate(state);
  }

  private static int port(byte pin) {
    return Ttl74592.pinNrToPortNr(pin);
  }

  private static Value carry(TtlTestInstanceState state) {
    return state.getPortValue(port(Ttl74592.RCO));
  }

  private static Value register(TtlTestInstanceState state) {
    return ((TtlRegisterData) state.getData()).getValue(Ttl74592.REGISTER_WORD);
  }

  private static Value counter(TtlTestInstanceState state) {
    return ((TtlRegisterData) state.getData()).getValue(Ttl74592.COUNTER_WORD);
  }

  private static void assertRegister(TtlTestInstanceState state, int code) {
    assertEquals(Value.createKnown(WIDTH, code), register(state));
  }

  private static void assertCounter(TtlTestInstanceState state, int code) {
    assertEquals(Value.createKnown(WIDTH, code), counter(state));
  }
}
