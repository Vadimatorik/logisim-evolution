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
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC543 octal registered transceiver. */
class Ttl74543Test {
  private static final int[] BUS_A = {
    Ttl74543.PORT_INDEX_A0,
    Ttl74543.PORT_INDEX_A1,
    Ttl74543.PORT_INDEX_A2,
    Ttl74543.PORT_INDEX_A3,
    Ttl74543.PORT_INDEX_A4,
    Ttl74543.PORT_INDEX_A5,
    Ttl74543.PORT_INDEX_A6,
    Ttl74543.PORT_INDEX_A7
  };
  private static final int[] BUS_B = {
    Ttl74543.PORT_INDEX_B0,
    Ttl74543.PORT_INDEX_B1,
    Ttl74543.PORT_INDEX_B2,
    Ttl74543.PORT_INDEX_B3,
    Ttl74543.PORT_INDEX_B4,
    Ttl74543.PORT_INDEX_B5,
    Ttl74543.PORT_INDEX_B6,
    Ttl74543.PORT_INDEX_B7
  };
  private static final int[] PATTERNS = {0x00, 0xFF, 0x55, 0xAA, 0x01, 0x80, 0x0F, 0xF0};

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74543();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(22, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_nLEBA, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_nOEBA, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_A0, 50, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_A1, 70, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_A2, 90, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_A3, 110, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_A4, 130, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_A5, 150, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_A6, 170, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_A7, 190, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_nCEAB, 210, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_nOEAB, 230, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_nLEAB, 210, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_B7, 190, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_B6, 170, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_B5, 150, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_B4, 130, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_B3, 110, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_B2, 90, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_B1, 70, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_B0, 50, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74543.PORT_INDEX_nCEBA, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(24, shownPower.getPorts().size());
    assertPort(shownPower, Ttl74543.PORT_INDEX_GND, 230, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, Ttl74543.PORT_INDEX_VCC, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void latchEnablesAreNotClocks() {
    assertFalse(new Ttl74543().checkForGatedClocks(null));
  }

  @Test
  void transparentPathCopiesAToB() {
    final var gate = new Ttl74543();

    for (final var pattern : PATTERNS) {
      final var state = new TtlTestInstanceState(gate, false);
      present(state, pattern, 0x00, Value.FALSE, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
      gate.propagate(state);
      assertBus(state, BUS_B, pattern);
      assertBusUnknown(state, BUS_A);
    }
  }

  @Test
  void transparentPathCopiesBToA() {
    final var gate = new Ttl74543();

    for (final var pattern : PATTERNS) {
      final var state = new TtlTestInstanceState(gate, false);
      present(state, 0x00, pattern, Value.TRUE, Value.TRUE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE);
      gate.propagate(state);
      assertBus(state, BUS_A, pattern);
      assertBusUnknown(state, BUS_B);
    }
  }

  @Test
  void eachBitLandsOnItsOwnPair() {
    final var gate = new Ttl74543();

    for (var bit = 0; bit < 8; bit++) {
      final var towardB = new TtlTestInstanceState(gate, false);
      present(towardB, 1 << bit, 0x00, Value.FALSE, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
      gate.propagate(towardB);
      assertBus(towardB, BUS_B, 1 << bit);

      final var towardA = new TtlTestInstanceState(gate, false);
      present(towardA, 0x00, 1 << bit, Value.TRUE, Value.TRUE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE);
      gate.propagate(towardA);
      assertBus(towardA, BUS_A, 1 << bit);
    }
  }

  @Test
  void highOutputEnableOrChipEnableReleasesTheBus() {
    final var gate = new Ttl74543();
    final var outputDisabled = new TtlTestInstanceState(gate, false);
    present(outputDisabled, 0x55, 0x00, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(outputDisabled);
    assertBusUnknown(outputDisabled, BUS_A);
    assertBusUnknown(outputDisabled, BUS_B);

    final var chipDisabled = new TtlTestInstanceState(gate, false);
    present(chipDisabled, 0x55, 0x00, Value.TRUE, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(chipDisabled);
    assertBusUnknown(chipDisabled, BUS_A);
    assertBusUnknown(chipDisabled, BUS_B);
  }

  @Test
  void risingLatchEnableHoldsTheWord() {
    final var gate = new Ttl74543();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0xA5, 0x00, Value.FALSE, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    present(state, 0x00, 0x00, Value.FALSE, Value.TRUE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0xA5);

    present(state, 0x00, 0x5A, Value.TRUE, Value.TRUE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    present(state, 0x00, 0x00, Value.TRUE, Value.TRUE, Value.TRUE, Value.FALSE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_A, 0x5A);
  }

  @Test
  void risingChipEnableLatchesAndReleasesUntilItReturns() {
    final var gate = new Ttl74543();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0xA5, 0x00, Value.FALSE, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    present(state, 0x11, 0x00, Value.TRUE, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertBusUnknown(state, BUS_B);

    present(state, 0x11, 0x00, Value.FALSE, Value.TRUE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0xA5);
  }

  @Test
  void outputEnableDoesNotBlockTheLatch() {
    final var gate = new Ttl74543();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0x5A, 0x00, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertBusUnknown(state, BUS_B);
    present(state, 0x00, 0x00, Value.FALSE, Value.TRUE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0x5A);
  }

  @Test
  void theTwoLatchesKeepIndependentWords() {
    final var gate = new Ttl74543();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0x0F, 0x00, Value.FALSE, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    present(state, 0x00, 0xF0, Value.TRUE, Value.TRUE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    present(state, 0x00, 0x00, Value.FALSE, Value.TRUE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0x0F);
    present(state, 0x00, 0x00, Value.TRUE, Value.TRUE, Value.TRUE, Value.FALSE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_A, 0xF0);
  }

  @Test
  void openLatchFollowsTheWordDrivenOntoItsBus() {
    final var gate = new Ttl74543();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0x00, 0x3C, Value.FALSE, Value.FALSE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_A, 0x3C);
    present(state, 0x00, 0x00, Value.FALSE, Value.TRUE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0x3C);
  }

  @Test
  void bothTransparentDriversDoNotSwapTheLatches() {
    final var gate = new Ttl74543();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0x0F, 0x00, Value.FALSE, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    present(state, 0x00, 0xF0, Value.TRUE, Value.TRUE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    present(state, 0x00, 0x00, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    gate.propagate(state);
    present(state, 0x00, 0x00, Value.FALSE, Value.TRUE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0x0F);
    present(state, 0x00, 0x00, Value.TRUE, Value.TRUE, Value.TRUE, Value.FALSE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_A, 0xF0);
  }

  @Test
  void unknownControlsReleaseOnlyThatDirection() {
    final var gate = new Ttl74543();
    final var controls = new Value[] {Value.UNKNOWN, Value.ERROR};

    for (final var control : controls) {
      final var unknownLatch = new TtlTestInstanceState(gate, false);
      present(unknownLatch, 0x55, 0x00, Value.FALSE, control, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
      gate.propagate(unknownLatch);
      assertBusUnknown(unknownLatch, BUS_B);

      final var unknownOutput = new TtlTestInstanceState(gate, false);
      present(unknownOutput, 0x55, 0x00, Value.FALSE, Value.FALSE, control, Value.TRUE, Value.TRUE, Value.TRUE);
      gate.propagate(unknownOutput);
      assertBusUnknown(unknownOutput, BUS_B);

      final var unknownChip = new TtlTestInstanceState(gate, false);
      present(unknownChip, 0x55, 0x00, control, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
      gate.propagate(unknownChip);
      assertBusUnknown(unknownChip, BUS_B);

      final var otherDirection = new TtlTestInstanceState(gate, false);
      present(otherDirection, 0x00, 0x3C, control, control, control, Value.FALSE, Value.FALSE, Value.FALSE);
      gate.propagate(otherDirection);
      assertBus(otherDirection, BUS_A, 0x3C);
    }
  }

  @Test
  void unknownSourceBitBecomesAnErrorOnTheDrivenBus() {
    final var gate = new Ttl74543();
    final var towardB = new TtlTestInstanceState(gate, false);
    present(towardB, 0x00, 0x00, Value.FALSE, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
    towardB.setPortValue(Ttl74543.PORT_INDEX_A0, Value.UNKNOWN);
    gate.propagate(towardB);

    assertEquals(Value.ERROR, towardB.getPortValue(Ttl74543.PORT_INDEX_B0));
    for (var bit = 1; bit < 8; bit++) {
      assertEquals(Value.FALSE, towardB.getPortValue(BUS_B[bit]), "B" + bit);
    }

    final var towardA = new TtlTestInstanceState(gate, false);
    present(towardA, 0x00, 0x00, Value.TRUE, Value.TRUE, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE);
    towardA.setPortValue(Ttl74543.PORT_INDEX_B0, Value.UNKNOWN);
    gate.propagate(towardA);

    assertEquals(Value.ERROR, towardA.getPortValue(Ttl74543.PORT_INDEX_A0));
    for (var bit = 1; bit < 8; bit++) {
      assertEquals(Value.FALSE, towardA.getPortValue(BUS_A[bit]), "A" + bit);
    }
  }

  @Test
  void invalidExposedPowerInputsReleaseBothBuses() {
    final var gate = new Ttl74543();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(Ttl74543.PORT_INDEX_GND, Value.FALSE);
    state.setPortValue(Ttl74543.PORT_INDEX_VCC, Value.TRUE);
    present(state, 0x55, 0x00, Value.FALSE, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0x55);

    state.setPortValue(Ttl74543.PORT_INDEX_VCC, Value.FALSE);
    gate.propagate(state);
    assertBusUnknown(state, BUS_A);
    assertBusUnknown(state, BUS_B);

    state.setPortValue(Ttl74543.PORT_INDEX_VCC, Value.TRUE);
    present(state, 0x55, 0x00, Value.FALSE, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0x55);

    state.setPortValue(Ttl74543.PORT_INDEX_GND, Value.TRUE);
    gate.propagate(state);
    assertBusUnknown(state, BUS_A);
    assertBusUnknown(state, BUS_B);
  }

  private static void present(
      TtlTestInstanceState state,
      int busA,
      int busB,
      Value nCeAb,
      Value nLeAb,
      Value nOeAb,
      Value nCeBa,
      Value nLeBa,
      Value nOeBa) {
    state.setPortValue(Ttl74543.PORT_INDEX_nCEAB, nCeAb);
    state.setPortValue(Ttl74543.PORT_INDEX_nLEAB, nLeAb);
    state.setPortValue(Ttl74543.PORT_INDEX_nOEAB, nOeAb);
    state.setPortValue(Ttl74543.PORT_INDEX_nCEBA, nCeBa);
    state.setPortValue(Ttl74543.PORT_INDEX_nLEBA, nLeBa);
    state.setPortValue(Ttl74543.PORT_INDEX_nOEBA, nOeBa);
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
