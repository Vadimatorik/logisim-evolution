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

/** Functional tests for the 74HC646 octal bus transceiver and register. */
class Ttl74646Test {
  private static final int[] BUS_A = {
    Ttl74646.PORT_INDEX_A0,
    Ttl74646.PORT_INDEX_A1,
    Ttl74646.PORT_INDEX_A2,
    Ttl74646.PORT_INDEX_A3,
    Ttl74646.PORT_INDEX_A4,
    Ttl74646.PORT_INDEX_A5,
    Ttl74646.PORT_INDEX_A6,
    Ttl74646.PORT_INDEX_A7
  };
  private static final int[] BUS_B = {
    Ttl74646.PORT_INDEX_B0,
    Ttl74646.PORT_INDEX_B1,
    Ttl74646.PORT_INDEX_B2,
    Ttl74646.PORT_INDEX_B3,
    Ttl74646.PORT_INDEX_B4,
    Ttl74646.PORT_INDEX_B5,
    Ttl74646.PORT_INDEX_B6,
    Ttl74646.PORT_INDEX_B7
  };
  private static final int[] PATTERNS = {0x00, 0xFF, 0x55, 0xAA, 0x01, 0x80, 0x0F, 0xF0};

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74646();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(22, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_CPAB, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_SAB, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_DIR, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_A0, 70, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_A1, 90, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_A2, 110, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_A3, 130, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_A4, 150, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_A5, 170, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_A6, 190, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_A7, 210, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_B7, 230, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_B6, 210, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_B5, 190, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_B4, 170, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_B3, 150, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_B2, 130, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_B1, 110, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_B0, 90, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_nOE, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_SBA, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74646.PORT_INDEX_CPBA, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(24, shownPower.getPorts().size());
    assertPort(shownPower, Ttl74646.PORT_INDEX_GND, 230, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, Ttl74646.PORT_INDEX_VCC, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void clocksAreTheTwoRegisterClocks() {
    final var gate = new Ttl74646();

    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(
        new int[] {Ttl74646.PORT_INDEX_CPAB, Ttl74646.PORT_INDEX_CPBA}, gate.clockPinIndex(null));
  }

  @Test
  void highDirectionAndLowSelectCopyAToB() {
    final var gate = new Ttl74646();

    for (final var pattern : PATTERNS) {
      final var state = new TtlTestInstanceState(gate, false);
      present(state, pattern, 0x00, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
      gate.propagate(state);
      assertBus(state, BUS_B, pattern);
      assertBusUnknown(state, BUS_A);
    }
  }

  @Test
  void lowDirectionAndLowSelectCopyBToA() {
    final var gate = new Ttl74646();

    for (final var pattern : PATTERNS) {
      final var state = new TtlTestInstanceState(gate, false);
      present(state, 0x00, pattern, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
      gate.propagate(state);
      assertBus(state, BUS_A, pattern);
      assertBusUnknown(state, BUS_B);
    }
  }

  @Test
  void eachBitLandsOnItsOwnPair() {
    final var gate = new Ttl74646();

    for (var bit = 0; bit < 8; bit++) {
      final var towardB = new TtlTestInstanceState(gate, false);
      present(towardB, 1 << bit, 0x00, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
      gate.propagate(towardB);
      assertBus(towardB, BUS_B, 1 << bit);

      final var towardA = new TtlTestInstanceState(gate, false);
      present(towardA, 0x00, 1 << bit, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
      gate.propagate(towardA);
      assertBus(towardA, BUS_A, 1 << bit);
    }
  }

  @Test
  void highOutputEnableReleasesBothBuses() {
    final var gate = new Ttl74646();
    final var directions = new Value[] {Value.TRUE, Value.FALSE};

    for (final var direction : directions) {
      final var state = new TtlTestInstanceState(gate, false);
      present(state, 0x55, 0x0F, direction, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
      gate.propagate(state);
      assertBusUnknown(state, BUS_A);
      assertBusUnknown(state, BUS_B);
    }
  }

  @Test
  void risingCpabStoresAAndLevelOrFallingEdgeDoesNot() {
    final var gate = new Ttl74646();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0xA5, 0x5A, Value.FALSE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    present(state, 0xA5, 0x5A, Value.FALSE, Value.TRUE, Value.FALSE, Value.FALSE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    present(state, 0x00, 0x5A, Value.FALSE, Value.TRUE, Value.FALSE, Value.FALSE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    showStoredA(gate, state, Value.TRUE, Value.FALSE);
    assertBus(state, BUS_B, 0xA5);

    present(state, 0x11, 0x5A, Value.FALSE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    showStoredA(gate, state, Value.FALSE, Value.FALSE);
    assertBus(state, BUS_B, 0xA5);
  }

  @Test
  void risingCpbaStoresBIndependently() {
    final var gate = new Ttl74646();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0xA5, 0x5A, Value.FALSE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    present(state, 0xA5, 0x5A, Value.FALSE, Value.TRUE, Value.FALSE, Value.FALSE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    present(state, 0xA5, 0x5A, Value.FALSE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    present(state, 0x00, 0x5A, Value.FALSE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.TRUE);
    gate.propagate(state);

    showStoredA(gate, state, Value.FALSE, Value.TRUE);
    assertBus(state, BUS_B, 0xA5);
    showStoredB(gate, state, Value.FALSE, Value.TRUE);
    assertBus(state, BUS_A, 0x5A);
  }

  @Test
  void clockingTheOutputSideCapturesTheOppositeBus() {
    final var gate = new Ttl74646();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0x00, 0x3C, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_A, 0x3C);
    present(state, 0x00, 0x3C, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    present(state, 0x00, 0x00, Value.TRUE, Value.FALSE, Value.TRUE, Value.FALSE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0x3C);
    assertBusUnknown(state, BUS_A);
  }

  @Test
  void simultaneousClocksSampleTheValueThatWasAlreadyOnThePin() {
    final var gate = new Ttl74646();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0x00, 0x0F, Value.FALSE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    present(state, 0x00, 0x0F, Value.FALSE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    present(state, 0x00, 0xF0, Value.FALSE, Value.FALSE, Value.FALSE, Value.TRUE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_A, 0x0F);
    present(state, 0x00, 0xF0, Value.FALSE, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertBus(state, BUS_A, 0xF0);
    assertBusUnknown(state, BUS_B);

    present(state, 0x00, 0x00, Value.TRUE, Value.FALSE, Value.TRUE, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0x0F);
  }

  @Test
  void unknownControlsReleaseBothBuses() {
    final var gate = new Ttl74646();
    final var controls = new Value[] {Value.UNKNOWN, Value.ERROR};

    for (final var control : controls) {
      final var unknownEnable = new TtlTestInstanceState(gate, false);
      present(unknownEnable, 0x55, 0x0F, Value.TRUE, control, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
      gate.propagate(unknownEnable);
      assertBusUnknown(unknownEnable, BUS_A);
      assertBusUnknown(unknownEnable, BUS_B);

      final var unknownDirection = new TtlTestInstanceState(gate, false);
      present(unknownDirection, 0x55, 0x0F, control, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
      gate.propagate(unknownDirection);
      assertBusUnknown(unknownDirection, BUS_A);
      assertBusUnknown(unknownDirection, BUS_B);

      final var unknownSelectAb = new TtlTestInstanceState(gate, false);
      present(unknownSelectAb, 0x55, 0x0F, Value.TRUE, Value.FALSE, control, Value.FALSE, Value.FALSE, Value.FALSE);
      gate.propagate(unknownSelectAb);
      assertBusUnknown(unknownSelectAb, BUS_A);
      assertBusUnknown(unknownSelectAb, BUS_B);

      final var unknownSelectBa = new TtlTestInstanceState(gate, false);
      present(unknownSelectBa, 0x55, 0x0F, Value.FALSE, Value.FALSE, Value.FALSE, control, Value.FALSE, Value.FALSE);
      gate.propagate(unknownSelectBa);
      assertBusUnknown(unknownSelectBa, BUS_A);
      assertBusUnknown(unknownSelectBa, BUS_B);
    }
  }

  @Test
  void unknownSourceBitBecomesAnErrorOnTheDrivenBus() {
    final var gate = new Ttl74646();
    final var towardB = new TtlTestInstanceState(gate, false);
    present(towardB, 0x00, 0x00, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
    towardB.setPortValue(Ttl74646.PORT_INDEX_A0, Value.UNKNOWN);
    gate.propagate(towardB);

    assertEquals(Value.ERROR, towardB.getPortValue(Ttl74646.PORT_INDEX_B0));
    for (var bit = 1; bit < 8; bit++) {
      assertEquals(Value.FALSE, towardB.getPortValue(BUS_B[bit]), "B" + bit);
    }

    final var towardA = new TtlTestInstanceState(gate, false);
    present(towardA, 0x00, 0x00, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
    towardA.setPortValue(Ttl74646.PORT_INDEX_B0, Value.UNKNOWN);
    gate.propagate(towardA);

    assertEquals(Value.ERROR, towardA.getPortValue(Ttl74646.PORT_INDEX_A0));
    for (var bit = 1; bit < 8; bit++) {
      assertEquals(Value.FALSE, towardA.getPortValue(BUS_A[bit]), "A" + bit);
    }
  }

  @Test
  void invalidExposedPowerInputsReleaseBothBuses() {
    final var gate = new Ttl74646();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(Ttl74646.PORT_INDEX_GND, Value.FALSE);
    state.setPortValue(Ttl74646.PORT_INDEX_VCC, Value.TRUE);
    present(state, 0x55, 0x00, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0x55);

    state.setPortValue(Ttl74646.PORT_INDEX_VCC, Value.FALSE);
    gate.propagate(state);
    assertBusUnknown(state, BUS_A);
    assertBusUnknown(state, BUS_B);

    state.setPortValue(Ttl74646.PORT_INDEX_VCC, Value.TRUE);
    present(state, 0x55, 0x00, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0x55);

    state.setPortValue(Ttl74646.PORT_INDEX_GND, Value.TRUE);
    gate.propagate(state);
    assertBusUnknown(state, BUS_A);
    assertBusUnknown(state, BUS_B);
  }

  /** Shows register A on B without a new clock edge. The clocks stay at the given levels. */
  private static void showStoredA(
      Ttl74646 gate, TtlTestInstanceState state, Value clockAb, Value clockBa) {
    present(state, 0x00, 0x00, Value.TRUE, Value.FALSE, Value.TRUE, Value.FALSE, clockAb, clockBa);
    gate.propagate(state);
  }

  /** Shows register B on A without a new clock edge. The clocks stay at the given levels. */
  private static void showStoredB(
      Ttl74646 gate, TtlTestInstanceState state, Value clockAb, Value clockBa) {
    present(state, 0x00, 0x00, Value.FALSE, Value.FALSE, Value.FALSE, Value.TRUE, clockAb, clockBa);
    gate.propagate(state);
  }

  private static void present(
      TtlTestInstanceState state,
      int busA,
      int busB,
      Value direction,
      Value outputEnable,
      Value selectAb,
      Value selectBa,
      Value clockAb,
      Value clockBa) {
    state.setPortValue(Ttl74646.PORT_INDEX_DIR, direction);
    state.setPortValue(Ttl74646.PORT_INDEX_nOE, outputEnable);
    state.setPortValue(Ttl74646.PORT_INDEX_SAB, selectAb);
    state.setPortValue(Ttl74646.PORT_INDEX_SBA, selectBa);
    state.setPortValue(Ttl74646.PORT_INDEX_CPAB, clockAb);
    state.setPortValue(Ttl74646.PORT_INDEX_CPBA, clockBa);
    for (var bit = 0; bit < 8; bit++) {
      state.setPortValue(BUS_A[bit], bitValue(busA, bit));
      state.setPortValue(BUS_B[bit], bitValue(busB, bit));
    }
  }

  private static Value bitValue(int word, int bit) {
    return ((word >> bit) & 1) == 1 ? Value.TRUE : Value.FALSE;
  }

  private static void assertBus(TtlTestInstanceState state, int[] ports, int expected) {
    for (var bit = 0; bit < ports.length; bit++) {
      assertEquals(bitValue(expected, bit), state.getPortValue(ports[bit]), "bit " + bit);
    }
  }

  private static void assertBusUnknown(TtlTestInstanceState state, int[] ports) {
    for (final var port : ports) {
      assertEquals(Value.UNKNOWN, state.getPortValue(port));
    }
  }

  private static void assertPort(Instance instance, int port, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
