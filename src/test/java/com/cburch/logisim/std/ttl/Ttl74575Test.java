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

/** Functional tests for the SN74ALS575A octal D flip-flop. */
class Ttl74575Test {
  private static final int[] DATA = {
    Ttl74575.PORT_INDEX_D1,
    Ttl74575.PORT_INDEX_D2,
    Ttl74575.PORT_INDEX_D3,
    Ttl74575.PORT_INDEX_D4,
    Ttl74575.PORT_INDEX_D5,
    Ttl74575.PORT_INDEX_D6,
    Ttl74575.PORT_INDEX_D7,
    Ttl74575.PORT_INDEX_D8
  };
  private static final int[] OUTPUTS = {
    Ttl74575.PORT_INDEX_Q1,
    Ttl74575.PORT_INDEX_Q2,
    Ttl74575.PORT_INDEX_Q3,
    Ttl74575.PORT_INDEX_Q4,
    Ttl74575.PORT_INDEX_Q5,
    Ttl74575.PORT_INDEX_Q6,
    Ttl74575.PORT_INDEX_Q7,
    Ttl74575.PORT_INDEX_Q8
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74575();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(19, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74575.PORT_INDEX_nCLR, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74575.PORT_INDEX_nOE, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74575.PORT_INDEX_D1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74575.PORT_INDEX_D2, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74575.PORT_INDEX_D3, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74575.PORT_INDEX_D4, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74575.PORT_INDEX_D5, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74575.PORT_INDEX_D6, 150, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74575.PORT_INDEX_D7, 170, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74575.PORT_INDEX_D8, 190, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74575.PORT_INDEX_CLK, 210, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74575.PORT_INDEX_Q8, 190, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74575.PORT_INDEX_Q7, 170, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74575.PORT_INDEX_Q6, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74575.PORT_INDEX_Q5, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74575.PORT_INDEX_Q4, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74575.PORT_INDEX_Q3, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74575.PORT_INDEX_Q2, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74575.PORT_INDEX_Q1, 50, -30, EndData.OUTPUT_ONLY);
    assertNoPortAt(hiddenPower, 210, 30);
    assertNoPortAt(hiddenPower, 230, -30);
    assertNoPortAt(hiddenPower, 30, -30);

    final var shownPower = createInstance(gate, true);
    assertEquals(21, shownPower.getPorts().size());
    assertEquals(Location.create(230, 30, false), shownPower.getPortLocation(Ttl74575.PORT_INDEX_GND));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(Ttl74575.PORT_INDEX_VCC));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(Ttl74575.PORT_INDEX_GND).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(Ttl74575.PORT_INDEX_VCC).getType());
  }

  @Test
  void clockPinIsTheRisingEdgeClock() {
    final var gate = new Ttl74575();

    assertTrue(gate.checkForGatedClocks(null));
    assertEquals(Ttl74575.PORT_INDEX_CLK, gate.clockPinIndex(null)[0]);
    assertEquals(1, gate.clockPinIndex(null).length);
  }

  @Test
  void risingEdgeLoadsTheDataInputs() {
    final var gate = new Ttl74575();
    final var patterns = new int[] {0x00, 0xFF, 0xA5, 0x5A, 0x01, 0x80};

    for (final var pattern : patterns) {
      final var state = armed(gate);
      setData(state, pattern);
      rise(gate, state);
      assertOutputs(state, pattern);
    }
  }

  @Test
  void levelAndFallingEdgeHoldTheRegister() {
    final var gate = new Ttl74575();
    final var state = armed(gate);
    setData(state, 0xA5);
    rise(gate, state);
    assertOutputs(state, 0xA5);

    setData(state, 0x5A);
    gate.propagate(state);
    assertOutputs(state, 0xA5);

    state.setPortValue(Ttl74575.PORT_INDEX_CLK, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, 0xA5);
  }

  @Test
  void unknownClockDoesNotLoad() {
    final var gate = new Ttl74575();
    final var state = armed(gate);
    setData(state, 0x0F);
    rise(gate, state);

    setData(state, 0xF0);
    state.setPortValue(Ttl74575.PORT_INDEX_CLK, Value.UNKNOWN);
    gate.propagate(state);
    assertOutputs(state, 0x0F);

    state.setPortValue(Ttl74575.PORT_INDEX_CLK, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, 0x0F);

    state.setPortValue(Ttl74575.PORT_INDEX_CLK, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, 0xF0);
  }

  @Test
  void clearIsSynchronousAndWinsOverData() {
    final var gate = new Ttl74575();
    final var state = armed(gate);
    setData(state, 0xA5);
    rise(gate, state);

    state.setPortValue(Ttl74575.PORT_INDEX_nCLR, Value.FALSE);
    setData(state, 0xFF);
    gate.propagate(state);
    assertOutputs(state, 0xA5);

    state.setPortValue(Ttl74575.PORT_INDEX_CLK, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, 0xA5);

    state.setPortValue(Ttl74575.PORT_INDEX_CLK, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, 0x00);

    state.setPortValue(Ttl74575.PORT_INDEX_nCLR, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, 0x00);

    setData(state, 0x5A);
    rise(gate, state);
    assertOutputs(state, 0x5A);
  }

  @Test
  void outputEnableReleasesAndRevealsWithoutChangingTheRegister() {
    final var gate = new Ttl74575();
    final var state = armed(gate);
    setData(state, 0xA5);
    rise(gate, state);

    state.setPortValue(Ttl74575.PORT_INDEX_nOE, Value.TRUE);
    gate.propagate(state);
    assertOutputsUnknown(state);

    state.setPortValue(Ttl74575.PORT_INDEX_nOE, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, 0xA5);
  }

  @Test
  void clockWhileDisabledStillLoads() {
    final var gate = new Ttl74575();
    final var state = armed(gate);
    setData(state, 0xA5);
    rise(gate, state);

    state.setPortValue(Ttl74575.PORT_INDEX_nOE, Value.TRUE);
    setData(state, 0x5A);
    rise(gate, state);
    assertOutputsUnknown(state);

    state.setPortValue(Ttl74575.PORT_INDEX_nOE, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, 0x5A);
  }

  @Test
  void unknownClearAgreesOnlyWhenDataIsAlreadyZero() {
    final var gate = new Ttl74575();
    final var state = armed(gate);
    setData(state, 0x0F);
    state.setPortValue(Ttl74575.PORT_INDEX_nCLR, Value.UNKNOWN);
    rise(gate, state);

    for (var bit = 0; bit < OUTPUTS.length; bit++) {
      final var expected = bit < 4 ? Value.UNKNOWN : Value.FALSE;
      assertEquals(expected, state.getPortValue(OUTPUTS[bit]), "Q" + (bit + 1));
    }
  }

  @Test
  void errorClearBecomesAnErrorOnlyWhereTheSubstitutionsDisagree() {
    final var gate = new Ttl74575();
    final var state = armed(gate);
    setData(state, 0x01);
    state.setPortValue(Ttl74575.PORT_INDEX_nCLR, Value.ERROR);
    rise(gate, state);

    assertEquals(Value.ERROR, state.getPortValue(Ttl74575.PORT_INDEX_Q1));
    for (var bit = 1; bit < OUTPUTS.length; bit++) {
      assertEquals(Value.FALSE, state.getPortValue(OUTPUTS[bit]), "Q" + (bit + 1));
    }
  }

  @Test
  void unknownOrErrorOutputEnableFollowsTheStoredBit() {
    final var gate = new Ttl74575();
    final var state = armed(gate);
    setData(state, 0x00);
    rise(gate, state);

    state.setPortValue(Ttl74575.PORT_INDEX_nOE, Value.UNKNOWN);
    gate.propagate(state);
    assertOutputsUnknown(state);

    state.setPortValue(Ttl74575.PORT_INDEX_nOE, Value.ERROR);
    gate.propagate(state);
    for (final var output : OUTPUTS) {
      assertEquals(Value.ERROR, state.getPortValue(output));
    }

    state.setPortValue(Ttl74575.PORT_INDEX_nOE, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, 0x00);
  }

  @Test
  void invalidExposedPowerInputsReleaseTheOutputsWithoutClearingTheRegister() {
    final var gate = new Ttl74575();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(Ttl74575.PORT_INDEX_GND, Value.FALSE);
    state.setPortValue(Ttl74575.PORT_INDEX_VCC, Value.TRUE);
    releaseClear(state);
    enable(state);
    setData(state, 0xA5);
    rise(gate, state);
    assertOutputs(state, 0xA5);

    state.setPortValue(Ttl74575.PORT_INDEX_VCC, Value.FALSE);
    setData(state, 0x5A);
    state.setPortValue(Ttl74575.PORT_INDEX_nCLR, Value.FALSE);
    state.setPortValue(Ttl74575.PORT_INDEX_CLK, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74575.PORT_INDEX_CLK, Value.TRUE);
    gate.propagate(state);
    assertOutputsUnknown(state);

    state.setPortValue(Ttl74575.PORT_INDEX_VCC, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, 0xA5);

    state.setPortValue(Ttl74575.PORT_INDEX_GND, Value.TRUE);
    gate.propagate(state);
    assertOutputsUnknown(state);

    state.setPortValue(Ttl74575.PORT_INDEX_GND, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, 0xA5);
  }

  private static TtlTestInstanceState armed(Ttl74575 gate) {
    final var state = new TtlTestInstanceState(gate, false);
    releaseClear(state);
    enable(state);
    state.setPortValue(Ttl74575.PORT_INDEX_CLK, Value.FALSE);
    return state;
  }

  private static void releaseClear(TtlTestInstanceState state) {
    state.setPortValue(Ttl74575.PORT_INDEX_nCLR, Value.TRUE);
  }

  private static void enable(TtlTestInstanceState state) {
    state.setPortValue(Ttl74575.PORT_INDEX_nOE, Value.FALSE);
  }

  private static void setData(TtlTestInstanceState state, int pattern) {
    for (var bit = 0; bit < DATA.length; bit++) {
      final var value = ((pattern >> bit) & 1) == 1 ? Value.TRUE : Value.FALSE;
      state.setPortValue(DATA[bit], value);
    }
  }

  private static void rise(Ttl74575 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl74575.PORT_INDEX_CLK, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74575.PORT_INDEX_CLK, Value.TRUE);
    gate.propagate(state);
  }

  private static void assertOutputs(TtlTestInstanceState state, int pattern) {
    for (var bit = 0; bit < OUTPUTS.length; bit++) {
      final var expected = ((pattern >> bit) & 1) == 1 ? Value.TRUE : Value.FALSE;
      assertEquals(expected, state.getPortValue(OUTPUTS[bit]), "Q" + (bit + 1));
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

  private static void assertNoPortAt(Instance instance, int x, int y) {
    final var location = Location.create(x, y, false);
    for (var port = 0; port < instance.getPorts().size(); port++) {
      assertTrue(
          !location.equals(instance.getPortLocation(port)),
          "unexpected port at " + location);
    }
  }
}
