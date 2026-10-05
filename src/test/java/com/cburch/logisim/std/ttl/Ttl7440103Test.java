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
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC40103 8-bit synchronous binary down counter. */
class Ttl7440103Test {
  private static final BitWidth WIDTH = BitWidth.create(8);
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl7440103();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl7440103.CP, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440103.MR, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440103.TE, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440103.P0, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440103.P1, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440103.P2, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440103.P3, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440103.PL, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440103.P4, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440103.P5, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440103.P6, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440103.P7, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440103.TC, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7440103.PE, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void clockIsTheFirstLogicalPort() {
    final var gate = new Ttl7440103();
    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(new int[] {Ttl7440103.pinNrToPortNr(Ttl7440103.CP)}, gate.clockPinIndex(null));
  }

  @Test
  void masterResetForcesMaximumAndOverridesPreset() {
    final var gate = new Ttl7440103();
    final var state = idle(gate);
    asyncLoad(gate, state, 9);
    assertCount(state, 9);

    set(state, Ttl7440103.MR, Value.FALSE);
    set(state, Ttl7440103.PL, Value.FALSE);
    set(state, Ttl7440103.PE, Value.FALSE);
    set(state, Ttl7440103.TE, Value.FALSE);
    setPreset(state, 0);
    rise(gate, state);
    assertCount(state, 255);
    assertTc(state, Value.TRUE);

    set(state, Ttl7440103.MR, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
    assertTc(state, Value.FALSE);

    set(state, Ttl7440103.PL, Value.TRUE);
    setPreset(state, 7);
    gate.propagate(state);
    assertCount(state, 0);
  }

  @Test
  void asynchronousPresetLoadsWithoutAClock() {
    final var gate = new Ttl7440103();
    final var state = idle(gate);
    masterReset(gate, state);
    assertCount(state, 255);
    assertTc(state, Value.TRUE);

    set(state, Ttl7440103.TE, Value.FALSE);
    set(state, Ttl7440103.PL, Value.FALSE);
    setPreset(state, 0);
    gate.propagate(state);
    assertCount(state, 0);
    assertTc(state, Value.FALSE);

    set(state, Ttl7440103.TE, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
    assertTc(state, Value.TRUE);

    set(state, Ttl7440103.TE, Value.FALSE);
    gate.propagate(state);
    assertTc(state, Value.FALSE);

    set(state, Ttl7440103.PL, Value.TRUE);
    setPreset(state, 7);
    gate.propagate(state);
    assertCount(state, 0);
  }

  @Test
  void synchronousPresetWaitsForTheRisingEdgeAndIgnoresTe() {
    final var gate = new Ttl7440103();
    final var state = idle(gate);
    masterReset(gate, state);

    set(state, Ttl7440103.TE, Value.FALSE);
    set(state, Ttl7440103.PE, Value.FALSE);
    setPreset(state, 0);
    gate.propagate(state);
    assertCount(state, 255);
    assertTc(state, Value.TRUE);

    rise(gate, state);
    assertCount(state, 0);
    assertTc(state, Value.FALSE);
    set(state, Ttl7440103.PE, Value.TRUE);

    set(state, Ttl7440103.TE, Value.TRUE);
    set(state, Ttl7440103.PE, Value.FALSE);
    setPreset(state, 0);
    rise(gate, state);
    assertCount(state, 0);
    assertTc(state, Value.TRUE);

    set(state, Ttl7440103.PE, Value.TRUE);
    set(state, Ttl7440103.TE, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 0);
    assertTc(state, Value.FALSE);
  }

  @Test
  void countingDownWrapsAndAssertsTerminalCountOnlyAtZero() {
    final var gate = new Ttl7440103();
    final var state = idle(gate);
    asyncLoad(gate, state, 5);
    enableCount(state);
    assertTc(state, Value.TRUE);

    for (var expected = 4; expected >= 0; expected--) {
      rise(gate, state);
      assertCount(state, expected);
      assertTc(state, expected == 0 ? Value.FALSE : Value.TRUE);
    }

    rise(gate, state);
    assertCount(state, 255);
    assertTc(state, Value.TRUE);

    for (var step = 0; step < 254; step++) {
      rise(gate, state);
      assertTc(state, Value.TRUE);
    }
    assertCount(state, 1);
    rise(gate, state);
    assertCount(state, 0);
    assertTc(state, Value.FALSE);
  }

  @Test
  void terminalEnableHighHoldsTheCountAndReleasesTerminalCount() {
    final var gate = new Ttl7440103();
    final var state = idle(gate);
    asyncLoad(gate, state, 1);
    set(state, Ttl7440103.TE, Value.TRUE);
    rise(gate, state);
    rise(gate, state);
    assertCount(state, 1);
    assertTc(state, Value.TRUE);

    asyncLoad(gate, state, 0);
    set(state, Ttl7440103.TE, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
    assertTc(state, Value.TRUE);
    rise(gate, state);
    assertCount(state, 0);
    assertTc(state, Value.TRUE);

    set(state, Ttl7440103.TE, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 0);
    assertTc(state, Value.FALSE);
    rise(gate, state);
    assertCount(state, 255);
    assertTc(state, Value.TRUE);
  }

  @Test
  void fallingAndUnknownClocksDoNotCount() {
    final var gate = new Ttl7440103();
    final var state = idle(gate);
    set(state, Ttl7440103.CP, Value.TRUE);
    gate.propagate(state);
    asyncLoad(gate, state, 2);
    set(state, Ttl7440103.TE, Value.FALSE);

    set(state, Ttl7440103.CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 2);
    assertTc(state, Value.TRUE);

    set(state, Ttl7440103.CP, Value.UNKNOWN);
    gate.propagate(state);
    set(state, Ttl7440103.CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 2);

    set(state, Ttl7440103.CP, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 1);
  }

  @Test
  void unknownControlsChangeOnlyResultsThatDisagree() {
    final var gate = new Ttl7440103();
    final var state = idle(gate);
    asyncLoad(gate, state, 4);
    enableCount(state);
    set(state, Ttl7440103.TE, Value.UNKNOWN);
    set(state, Ttl7440103.CP, Value.TRUE);
    gate.propagate(state);
    assertBits(state, Value.UNKNOWN, Value.UNKNOWN, Value.UNKNOWN, Value.FALSE);
    assertTc(state, Value.TRUE);
    set(state, Ttl7440103.CP, Value.FALSE);
    gate.propagate(state);

    asyncLoad(gate, state, 0);
    set(state, Ttl7440103.TE, Value.UNKNOWN);
    gate.propagate(state);
    assertCount(state, 0);
    assertTc(state, Value.UNKNOWN);

    asyncLoad(gate, state, 1);
    set(state, Ttl7440103.TE, Value.UNKNOWN);
    rise(gate, state);
    assertBit(state, 0, Value.UNKNOWN);
    assertBit(state, 1, Value.FALSE);
    assertTc(state, Value.UNKNOWN);

    masterReset(gate, state);
    set(state, Ttl7440103.TE, Value.FALSE);
    set(state, Ttl7440103.PL, Value.UNKNOWN);
    setPreset(state, 0);
    gate.propagate(state);
    assertCountUnknown(state);
    assertTc(state, Value.UNKNOWN);

    asyncLoad(gate, state, 3);
    enableCount(state);
    set(state, Ttl7440103.PE, Value.UNKNOWN);
    setPreset(state, 0);
    rise(gate, state);
    assertTc(state, Value.UNKNOWN);
  }

  @Test
  void errorOnMasterResetIsVisibleOnlyWhereTheCountDisagrees() {
    final var gate = new Ttl7440103();
    final var state = idle(gate);
    asyncLoad(gate, state, 5);
    set(state, Ttl7440103.TE, Value.TRUE);
    set(state, Ttl7440103.MR, Value.ERROR);
    gate.propagate(state);

    assertBit(state, 0, Value.TRUE);
    assertBit(state, 1, Value.ERROR);
    assertBit(state, 2, Value.TRUE);
    for (var bit = 3; bit < 8; bit++) {
      assertBit(state, bit, Value.ERROR);
    }
    assertTc(state, Value.TRUE);

    set(state, Ttl7440103.MR, Value.TRUE);
    asyncLoad(gate, state, 1);
    enableCount(state);
    set(state, Ttl7440103.TE, Value.ERROR);
    rise(gate, state);
    assertBit(state, 0, Value.ERROR);
    assertBit(state, 1, Value.FALSE);
    assertTc(state, Value.ERROR);
  }

  @Test
  void invalidExposedPowerInputsMakeTerminalCountUnknown() {
    final var gate = new Ttl7440103();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    set(state, Ttl7440103.CP, Value.FALSE);
    set(state, Ttl7440103.MR, Value.TRUE);
    set(state, Ttl7440103.PL, Value.TRUE);
    set(state, Ttl7440103.PE, Value.TRUE);
    set(state, Ttl7440103.TE, Value.TRUE);
    setPreset(state, 0);
    gate.propagate(state);
    asyncLoad(gate, state, 4);
    set(state, Ttl7440103.TE, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 4);
    assertTc(state, Value.TRUE);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertTc(state, Value.UNKNOWN);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 4);
    assertTc(state, Value.TRUE);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertTc(state, Value.UNKNOWN);
  }

  private static void assertPort(Instance instance, byte pin, int x, int y, int type) {
    final var port = Ttl7440103.pinNrToPortNr(pin);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static TtlTestInstanceState idle(Ttl7440103 gate) {
    final var state = new TtlTestInstanceState(gate, false);
    set(state, Ttl7440103.CP, Value.FALSE);
    set(state, Ttl7440103.MR, Value.TRUE);
    set(state, Ttl7440103.PL, Value.TRUE);
    set(state, Ttl7440103.PE, Value.TRUE);
    set(state, Ttl7440103.TE, Value.TRUE);
    setPreset(state, 0);
    gate.propagate(state);
    return state;
  }

  private static void masterReset(Ttl7440103 gate, TtlTestInstanceState state) {
    set(state, Ttl7440103.MR, Value.FALSE);
    set(state, Ttl7440103.PL, Value.TRUE);
    set(state, Ttl7440103.PE, Value.TRUE);
    gate.propagate(state);
    set(state, Ttl7440103.MR, Value.TRUE);
    gate.propagate(state);
  }

  private static void asyncLoad(Ttl7440103 gate, TtlTestInstanceState state, int code) {
    set(state, Ttl7440103.MR, Value.TRUE);
    set(state, Ttl7440103.PL, Value.FALSE);
    set(state, Ttl7440103.PE, Value.TRUE);
    setPreset(state, code);
    gate.propagate(state);
    set(state, Ttl7440103.PL, Value.TRUE);
    gate.propagate(state);
  }

  private static void enableCount(TtlTestInstanceState state) {
    set(state, Ttl7440103.MR, Value.TRUE);
    set(state, Ttl7440103.PL, Value.TRUE);
    set(state, Ttl7440103.PE, Value.TRUE);
    set(state, Ttl7440103.TE, Value.FALSE);
  }

  private static void rise(Ttl7440103 gate, TtlTestInstanceState state) {
    set(state, Ttl7440103.CP, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl7440103.CP, Value.TRUE);
    gate.propagate(state);
    set(state, Ttl7440103.CP, Value.FALSE);
    gate.propagate(state);
  }

  private static void set(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl7440103.pinNrToPortNr(pin), value);
  }

  private static void setPreset(TtlTestInstanceState state, int code) {
    final byte[] pins = {
      Ttl7440103.P0,
      Ttl7440103.P1,
      Ttl7440103.P2,
      Ttl7440103.P3,
      Ttl7440103.P4,
      Ttl7440103.P5,
      Ttl7440103.P6,
      Ttl7440103.P7
    };
    for (var bit = 0; bit < pins.length; bit++) {
      set(state, pins[bit], ((code >> bit) & 1) == 0 ? Value.FALSE : Value.TRUE);
    }
  }

  private static void assertCount(TtlTestInstanceState state, int expected) {
    assertEquals(Value.createKnown(WIDTH, expected), stored(state));
  }

  private static void assertCountUnknown(TtlTestInstanceState state) {
    final var value = stored(state);
    for (var bit = 0; bit < 8; bit++) {
      assertEquals(Value.UNKNOWN, value.get(bit));
    }
  }

  private static void assertBits(
      TtlTestInstanceState state, Value bit0, Value bit1, Value bit2, Value bit3) {
    assertBit(state, 0, bit0);
    assertBit(state, 1, bit1);
    assertBit(state, 2, bit2);
    assertBit(state, 3, bit3);
    for (var bit = 4; bit < 8; bit++) {
      assertBit(state, bit, Value.FALSE);
    }
  }

  private static void assertBit(TtlTestInstanceState state, int bit, Value expected) {
    assertEquals(expected, stored(state).get(bit));
  }

  private static void assertTc(TtlTestInstanceState state, Value expected) {
    assertEquals(expected, state.getPortValue(Ttl7440103.pinNrToPortNr(Ttl7440103.TC)));
  }

  private static Value stored(TtlTestInstanceState state) {
    return ((TtlRegisterData) state.getData()).getValue();
  }
}
