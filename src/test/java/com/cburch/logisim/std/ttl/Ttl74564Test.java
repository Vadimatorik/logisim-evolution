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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC564 octal inverting positive-edge D flip-flop. */
class Ttl74564Test {
  private static final int[] DATA = {
    Ttl74564.PORT_INDEX_D0,
    Ttl74564.PORT_INDEX_D1,
    Ttl74564.PORT_INDEX_D2,
    Ttl74564.PORT_INDEX_D3,
    Ttl74564.PORT_INDEX_D4,
    Ttl74564.PORT_INDEX_D5,
    Ttl74564.PORT_INDEX_D6,
    Ttl74564.PORT_INDEX_D7
  };
  private static final int[] OUTPUTS = {
    Ttl74564.PORT_INDEX_nQ0,
    Ttl74564.PORT_INDEX_nQ1,
    Ttl74564.PORT_INDEX_nQ2,
    Ttl74564.PORT_INDEX_nQ3,
    Ttl74564.PORT_INDEX_nQ4,
    Ttl74564.PORT_INDEX_nQ5,
    Ttl74564.PORT_INDEX_nQ6,
    Ttl74564.PORT_INDEX_nQ7
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74564();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(18, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74564.PORT_INDEX_nOE, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74564.PORT_INDEX_D0, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74564.PORT_INDEX_D1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74564.PORT_INDEX_D2, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74564.PORT_INDEX_D3, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74564.PORT_INDEX_D4, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74564.PORT_INDEX_D5, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74564.PORT_INDEX_D6, 150, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74564.PORT_INDEX_D7, 170, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74564.PORT_INDEX_CP, 190, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74564.PORT_INDEX_nQ7, 170, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74564.PORT_INDEX_nQ6, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74564.PORT_INDEX_nQ5, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74564.PORT_INDEX_nQ4, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74564.PORT_INDEX_nQ3, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74564.PORT_INDEX_nQ2, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74564.PORT_INDEX_nQ1, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74564.PORT_INDEX_nQ0, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(20, shownPower.getPorts().size());
    assertEquals(
        Location.create(190, 30, false), shownPower.getPortLocation(Ttl74564.PORT_INDEX_GND));
    assertEquals(
        Location.create(10, -30, false), shownPower.getPortLocation(Ttl74564.PORT_INDEX_VCC));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(Ttl74564.PORT_INDEX_GND).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(Ttl74564.PORT_INDEX_VCC).getType());
  }

  @Test
  void clockPinIsTheRisingEdgeClock() {
    final var gate = new Ttl74564();

    assertTrue(gate.checkForGatedClocks(null));
    assertEquals(Ttl74564.PORT_INDEX_CP, gate.clockPinIndex(null)[0]);
    assertEquals(1, gate.clockPinIndex(null).length);
  }

  @Test
  void risingEdgeDrivesTheComplementOfEveryByte() {
    final var gate = new Ttl74564();

    for (var pattern = 0; pattern < 256; pattern++) {
      final var state = new TtlTestInstanceState(gate, false);
      enable(state);
      setData(state, pattern);
      rise(gate, state);
      assertOutputs(state, pattern);
    }
  }

  @Test
  void levelAndFallingEdgeHoldTheRegister() {
    final var gate = new Ttl74564();
    final var state = new TtlTestInstanceState(gate, false);
    enable(state);
    setData(state, 0x55);
    rise(gate, state);
    assertOutputs(state, 0x55);

    setData(state, 0xAA);
    gate.propagate(state);
    assertOutputs(state, 0x55);

    state.setPortValue(Ttl74564.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, 0x55);
  }

  @Test
  void unknownClockDoesNotLoad() {
    final var gate = new Ttl74564();
    final var state = new TtlTestInstanceState(gate, false);
    enable(state);
    setData(state, 0x0F);
    rise(gate, state);

    setData(state, 0xF0);
    state.setPortValue(Ttl74564.PORT_INDEX_CP, Value.UNKNOWN);
    gate.propagate(state);
    assertOutputs(state, 0x0F);

    state.setPortValue(Ttl74564.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, 0x0F);

    state.setPortValue(Ttl74564.PORT_INDEX_CP, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, 0xF0);
  }

  @Test
  void outputEnableReleasesAndRevealsWithoutChangingTheRegister() {
    final var gate = new Ttl74564();
    final var state = new TtlTestInstanceState(gate, false);
    enable(state);
    setData(state, 0x55);
    rise(gate, state);

    state.setPortValue(Ttl74564.PORT_INDEX_nOE, Value.TRUE);
    gate.propagate(state);
    assertOutputsUnknown(state);

    state.setPortValue(Ttl74564.PORT_INDEX_nOE, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, 0x55);
  }

  @Test
  void clockWhileDisabledStillLoads() {
    final var gate = new Ttl74564();
    final var state = new TtlTestInstanceState(gate, false);
    enable(state);
    setData(state, 0x55);
    rise(gate, state);

    state.setPortValue(Ttl74564.PORT_INDEX_nOE, Value.TRUE);
    setData(state, 0xAA);
    rise(gate, state);
    assertOutputsUnknown(state);

    state.setPortValue(Ttl74564.PORT_INDEX_nOE, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, 0xAA);
  }

  @Test
  void unknownOrErrorOutputEnableReleasesTheOutputs() {
    final var gate = new Ttl74564();
    final var state = new TtlTestInstanceState(gate, false);
    enable(state);
    setData(state, 0x3C);
    rise(gate, state);

    state.setPortValue(Ttl74564.PORT_INDEX_nOE, Value.UNKNOWN);
    gate.propagate(state);
    assertOutputsUnknown(state);

    state.setPortValue(Ttl74564.PORT_INDEX_nOE, Value.ERROR);
    gate.propagate(state);
    assertOutputsUnknown(state);

    state.setPortValue(Ttl74564.PORT_INDEX_nOE, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, 0x3C);
  }

  @Test
  void unknownOrErrorDataBitIsStoredAndInverted() {
    final var gate = new Ttl74564();
    final var state = new TtlTestInstanceState(gate, false);
    enable(state);
    setData(state, 0x00);
    state.setPortValue(Ttl74564.PORT_INDEX_D3, Value.UNKNOWN);
    state.setPortValue(Ttl74564.PORT_INDEX_D4, Value.ERROR);
    rise(gate, state);

    for (var bit = 0; bit < OUTPUTS.length; bit++) {
      final Value expected;
      if (bit == 3) expected = Value.UNKNOWN;
      else if (bit == 4) expected = Value.ERROR;
      else expected = Value.TRUE;
      assertEquals(expected, state.getPortValue(OUTPUTS[bit]), "nQ" + bit);
    }
  }

  @Test
  void invalidExposedPowerInputsReleaseTheOutputsWithoutClearingTheRegister() {
    final var gate = new Ttl74564();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(Ttl74564.PORT_INDEX_GND, Value.FALSE);
    state.setPortValue(Ttl74564.PORT_INDEX_VCC, Value.TRUE);
    enable(state);
    setData(state, 0xA5);
    rise(gate, state);
    assertOutputs(state, 0xA5);

    state.setPortValue(Ttl74564.PORT_INDEX_VCC, Value.FALSE);
    setData(state, 0x5A);
    state.setPortValue(Ttl74564.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74564.PORT_INDEX_CP, Value.TRUE);
    gate.propagate(state);
    assertOutputsUnknown(state);

    state.setPortValue(Ttl74564.PORT_INDEX_VCC, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, 0xA5);

    state.setPortValue(Ttl74564.PORT_INDEX_GND, Value.TRUE);
    gate.propagate(state);
    assertOutputsUnknown(state);

    state.setPortValue(Ttl74564.PORT_INDEX_GND, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, 0xA5);
  }

  private static void enable(TtlTestInstanceState state) {
    state.setPortValue(Ttl74564.PORT_INDEX_nOE, Value.FALSE);
  }

  private static void setData(TtlTestInstanceState state, int pattern) {
    for (var bit = 0; bit < DATA.length; bit++) {
      final var value = ((pattern >> bit) & 1) == 1 ? Value.TRUE : Value.FALSE;
      state.setPortValue(DATA[bit], value);
    }
  }

  private static void rise(Ttl74564 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl74564.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74564.PORT_INDEX_CP, Value.TRUE);
    gate.propagate(state);
  }

  /** The enabled outputs are the complement of the byte that was stored. */
  private static void assertOutputs(TtlTestInstanceState state, int pattern) {
    for (var bit = 0; bit < OUTPUTS.length; bit++) {
      final var expected = ((pattern >> bit) & 1) == 1 ? Value.FALSE : Value.TRUE;
      assertEquals(expected, state.getPortValue(OUTPUTS[bit]), "nQ" + bit);
    }
  }

  private static void assertOutputsUnknown(TtlTestInstanceState state) {
    for (final var output : OUTPUTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(output));
    }
  }

  private static void assertPort(Instance instance, int port, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
