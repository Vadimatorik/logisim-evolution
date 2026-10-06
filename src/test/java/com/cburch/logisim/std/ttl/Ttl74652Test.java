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

/** Functional tests for the 74HC652 octal bus transceiver and registers. */
class Ttl74652Test {
  private static final int[] BUS_A = {
    Ttl74652.PORT_INDEX_A1,
    Ttl74652.PORT_INDEX_A2,
    Ttl74652.PORT_INDEX_A3,
    Ttl74652.PORT_INDEX_A4,
    Ttl74652.PORT_INDEX_A5,
    Ttl74652.PORT_INDEX_A6,
    Ttl74652.PORT_INDEX_A7,
    Ttl74652.PORT_INDEX_A8
  };
  private static final int[] BUS_B = {
    Ttl74652.PORT_INDEX_B1,
    Ttl74652.PORT_INDEX_B2,
    Ttl74652.PORT_INDEX_B3,
    Ttl74652.PORT_INDEX_B4,
    Ttl74652.PORT_INDEX_B5,
    Ttl74652.PORT_INDEX_B6,
    Ttl74652.PORT_INDEX_B7,
    Ttl74652.PORT_INDEX_B8
  };
  private static final int[] PATTERNS = {0x00, 0xFF, 0x55, 0xAA, 0x01, 0x80, 0x0F, 0xF0};

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74652();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(22, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_CLKAB, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_SAB, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_OEAB, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_A1, 70, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_A2, 90, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_A3, 110, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_A4, 130, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_A5, 150, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_A6, 170, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_A7, 190, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_A8, 210, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_B8, 230, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_B7, 210, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_B6, 190, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_B5, 170, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_B4, 150, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_B3, 130, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_B2, 110, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_B1, 90, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_OEBA, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_SBA, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74652.PORT_INDEX_CLKBA, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(24, shownPower.getPorts().size());
    assertPort(shownPower, Ttl74652.PORT_INDEX_GND, 230, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, Ttl74652.PORT_INDEX_VCC, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void clocksAreTheTwoRegisterClocks() {
    final var gate = new Ttl74652();

    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(
        new int[] {Ttl74652.PORT_INDEX_CLKAB, Ttl74652.PORT_INDEX_CLKBA}, gate.clockPinIndex(null));
  }

  @Test
  void highOeabAndLowSelectCopyAToB() {
    final var gate = new Ttl74652();

    for (final var pattern : PATTERNS) {
      final var state = new TtlTestInstanceState(gate, false);
      present(state, pattern, 0x00, Value.TRUE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
      gate.propagate(state);
      assertBus(state, BUS_B, pattern);
      assertBusUnknown(state, BUS_A);
    }
  }

  @Test
  void lowOebaAndLowSelectCopyBToA() {
    final var gate = new Ttl74652();

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
    final var gate = new Ttl74652();

    for (var bit = 0; bit < 8; bit++) {
      final var towardB = new TtlTestInstanceState(gate, false);
      present(towardB, 1 << bit, 0x00, Value.TRUE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
      gate.propagate(towardB);
      assertBus(towardB, BUS_B, 1 << bit);

      final var towardA = new TtlTestInstanceState(gate, false);
      present(towardA, 0x00, 1 << bit, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
      gate.propagate(towardA);
      assertBus(towardA, BUS_A, 1 << bit);
    }
  }

  @Test
  void isolationReleasesBothBuses() {
    final var gate = new Ttl74652();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0x55, 0x0F, Value.FALSE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertBusUnknown(state, BUS_A);
    assertBusUnknown(state, BUS_B);
  }

  @Test
  void bothDirectionsCopyTheOppositeBusAtOnce() {
    final var gate = new Ttl74652();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0x55, 0xAA, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_A, 0xAA);
    assertBus(state, BUS_B, 0x55);
  }

  @Test
  void risingClkabStoresAAndLevelOrFallingEdgeDoesNot() {
    final var gate = new Ttl74652();
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
  void risingClkbaStoresBIndependently() {
    final var gate = new Ttl74652();
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
  void storedDataCanDriveBothBusesTogether() {
    final var gate = new Ttl74652();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0xA5, 0x5A, Value.FALSE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    present(state, 0xA5, 0x5A, Value.FALSE, Value.TRUE, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    present(state, 0x00, 0x00, Value.TRUE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0xA5);
    assertBus(state, BUS_A, 0x5A);
  }

  @Test
  void clockingTheOutputSideCapturesTheOppositeBus() {
    final var gate = new Ttl74652();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0x00, 0x3C, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_A, 0x3C);
    present(state, 0x00, 0x3C, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    present(state, 0x00, 0x00, Value.TRUE, Value.TRUE, Value.TRUE, Value.FALSE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0x3C);
    assertBusUnknown(state, BUS_A);
  }

  @Test
  void realTimePathStoresAInBothRegisters() {
    final var gate = new Ttl74652();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0x3C, 0x00, Value.TRUE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0x3C);
    present(state, 0x3C, 0x00, Value.TRUE, Value.TRUE, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    present(state, 0x00, 0x00, Value.TRUE, Value.TRUE, Value.TRUE, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0x3C);
    present(state, 0x00, 0x00, Value.FALSE, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertBus(state, BUS_A, 0x3C);
  }

  @Test
  void simultaneousClocksSampleTheValueThatWasAlreadyOnThePin() {
    final var gate = new Ttl74652();
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

    present(state, 0x00, 0x00, Value.TRUE, Value.TRUE, Value.TRUE, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0x0F);
  }

  @Test
  void unknownControlsReleaseOnlyTheirOwnDirection() {
    final var gate = new Ttl74652();
    final var controls = new Value[] {Value.UNKNOWN, Value.ERROR};

    for (final var control : controls) {
      final var unknownOeab = new TtlTestInstanceState(gate, false);
      present(unknownOeab, 0x55, 0x0F, control, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
      gate.propagate(unknownOeab);
      assertBus(unknownOeab, BUS_A, 0x0F);
      assertBusUnknown(unknownOeab, BUS_B);

      final var unknownOeba = new TtlTestInstanceState(gate, false);
      present(unknownOeba, 0x55, 0x0F, Value.TRUE, control, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
      gate.propagate(unknownOeba);
      assertBus(unknownOeba, BUS_B, 0x55);
      assertBusUnknown(unknownOeba, BUS_A);

      final var unknownSelectAb = new TtlTestInstanceState(gate, false);
      present(unknownSelectAb, 0x55, 0x0F, Value.TRUE, Value.FALSE, control, Value.FALSE, Value.FALSE, Value.FALSE);
      gate.propagate(unknownSelectAb);
      assertBus(unknownSelectAb, BUS_A, 0x0F);
      assertBusUnknown(unknownSelectAb, BUS_B);

      final var unknownSelectBa = new TtlTestInstanceState(gate, false);
      present(unknownSelectBa, 0x55, 0x0F, Value.TRUE, Value.FALSE, Value.FALSE, control, Value.FALSE, Value.FALSE);
      gate.propagate(unknownSelectBa);
      assertBus(unknownSelectBa, BUS_B, 0x55);
      assertBusUnknown(unknownSelectBa, BUS_A);
    }
  }

  @Test
  void unknownSourceBitBecomesAnErrorOnTheDrivenBus() {
    final var gate = new Ttl74652();
    final var towardB = new TtlTestInstanceState(gate, false);
    present(towardB, 0x00, 0x00, Value.TRUE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
    towardB.setPortValue(Ttl74652.PORT_INDEX_A1, Value.UNKNOWN);
    gate.propagate(towardB);

    assertEquals(Value.ERROR, towardB.getPortValue(Ttl74652.PORT_INDEX_B1));
    for (var bit = 1; bit < 8; bit++) {
      assertEquals(Value.FALSE, towardB.getPortValue(BUS_B[bit]), "B" + (bit + 1));
    }

    final var towardA = new TtlTestInstanceState(gate, false);
    present(towardA, 0x00, 0x00, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
    towardA.setPortValue(Ttl74652.PORT_INDEX_B1, Value.UNKNOWN);
    gate.propagate(towardA);

    assertEquals(Value.ERROR, towardA.getPortValue(Ttl74652.PORT_INDEX_A1));
    for (var bit = 1; bit < 8; bit++) {
      assertEquals(Value.FALSE, towardA.getPortValue(BUS_A[bit]), "A" + (bit + 1));
    }
  }

  @Test
  void invalidExposedPowerInputsReleaseBothBuses() {
    final var gate = new Ttl74652();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(Ttl74652.PORT_INDEX_GND, Value.FALSE);
    state.setPortValue(Ttl74652.PORT_INDEX_VCC, Value.TRUE);
    present(state, 0x55, 0x00, Value.TRUE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0x55);

    state.setPortValue(Ttl74652.PORT_INDEX_VCC, Value.FALSE);
    gate.propagate(state);
    assertBusUnknown(state, BUS_A);
    assertBusUnknown(state, BUS_B);

    state.setPortValue(Ttl74652.PORT_INDEX_VCC, Value.TRUE);
    present(state, 0x55, 0x00, Value.TRUE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0x55);

    state.setPortValue(Ttl74652.PORT_INDEX_GND, Value.TRUE);
    gate.propagate(state);
    assertBusUnknown(state, BUS_A);
    assertBusUnknown(state, BUS_B);
  }

  /** Shows register A on B without a new clock edge. The clocks stay at the given levels. */
  private static void showStoredA(
      Ttl74652 gate, TtlTestInstanceState state, Value clockAb, Value clockBa) {
    present(state, 0x00, 0x00, Value.TRUE, Value.TRUE, Value.TRUE, Value.FALSE, clockAb, clockBa);
    gate.propagate(state);
  }

  /** Shows register B on A without a new clock edge. The clocks stay at the given levels. */
  private static void showStoredB(
      Ttl74652 gate, TtlTestInstanceState state, Value clockAb, Value clockBa) {
    present(state, 0x00, 0x00, Value.FALSE, Value.FALSE, Value.FALSE, Value.TRUE, clockAb, clockBa);
    gate.propagate(state);
  }

  private static void present(
      TtlTestInstanceState state,
      int busA,
      int busB,
      Value outputEnableAb,
      Value outputEnableBa,
      Value selectAb,
      Value selectBa,
      Value clockAb,
      Value clockBa) {
    state.setPortValue(Ttl74652.PORT_INDEX_OEAB, outputEnableAb);
    state.setPortValue(Ttl74652.PORT_INDEX_OEBA, outputEnableBa);
    state.setPortValue(Ttl74652.PORT_INDEX_SAB, selectAb);
    state.setPortValue(Ttl74652.PORT_INDEX_SBA, selectBa);
    state.setPortValue(Ttl74652.PORT_INDEX_CLKAB, clockAb);
    state.setPortValue(Ttl74652.PORT_INDEX_CLKBA, clockBa);
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
