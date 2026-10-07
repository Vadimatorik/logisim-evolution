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

/** Functional tests for the 74HC544 octal inverting registered transceiver. */
class Ttl74544Test {
  private static final int[] BUS_A = {
    Ttl74544.PORT_INDEX_A0,
    Ttl74544.PORT_INDEX_A1,
    Ttl74544.PORT_INDEX_A2,
    Ttl74544.PORT_INDEX_A3,
    Ttl74544.PORT_INDEX_A4,
    Ttl74544.PORT_INDEX_A5,
    Ttl74544.PORT_INDEX_A6,
    Ttl74544.PORT_INDEX_A7
  };
  private static final int[] BUS_B = {
    Ttl74544.PORT_INDEX_B0,
    Ttl74544.PORT_INDEX_B1,
    Ttl74544.PORT_INDEX_B2,
    Ttl74544.PORT_INDEX_B3,
    Ttl74544.PORT_INDEX_B4,
    Ttl74544.PORT_INDEX_B5,
    Ttl74544.PORT_INDEX_B6,
    Ttl74544.PORT_INDEX_B7
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74544();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(22, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_nLEBA, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_nOEBA, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_A0, 50, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_A1, 70, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_A2, 90, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_A3, 110, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_A4, 130, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_A5, 150, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_A6, 170, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_A7, 190, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_nEAB, 210, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_nOEAB, 230, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_nLEAB, 210, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_B7, 190, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_B6, 170, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_B5, 150, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_B4, 130, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_B3, 110, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_B2, 90, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_B1, 70, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_B0, 50, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74544.PORT_INDEX_nEBA, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(24, shownPower.getPorts().size());
    assertPort(shownPower, Ttl74544.PORT_INDEX_GND, 230, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, Ttl74544.PORT_INDEX_VCC, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void latchEnablesAreNotClocks() {
    assertFalse(new Ttl74544().checkForGatedClocks(null));
  }

  @Test
  void transparentAtoBDrivesBWithTheComplementOfA() {
    final var gate = new Ttl74544();
    final var patterns = new int[] {0x00, 0xFF, 0x55, 0xAA, 0x01, 0x80, 0x0F, 0xF0};

    for (final var pattern : patterns) {
      final var state = new TtlTestInstanceState(gate, false);
      present(state, pattern, 0x00, false, false, false, true, true, true);
      gate.propagate(state);
      assertBus(state, BUS_B, pattern ^ 0xFF);
      assertBusUnknown(state, BUS_A);
    }
  }

  @Test
  void transparentBtoADrivesAWithTheComplementOfB() {
    final var gate = new Ttl74544();
    final var patterns = new int[] {0x00, 0xFF, 0x55, 0xAA, 0x01, 0x80, 0x0F, 0xF0};

    for (final var pattern : patterns) {
      final var state = new TtlTestInstanceState(gate, false);
      present(state, 0x00, pattern, true, true, true, false, false, false);
      gate.propagate(state);
      assertBus(state, BUS_A, pattern ^ 0xFF);
      assertBusUnknown(state, BUS_B);
    }
  }

  @Test
  void eachBitInvertsOntoItsOwnPair() {
    final var gate = new Ttl74544();

    for (var bit = 0; bit < 8; bit++) {
      final var towardB = new TtlTestInstanceState(gate, false);
      present(towardB, 1 << bit, 0x00, false, false, false, true, true, true);
      gate.propagate(towardB);
      assertBus(towardB, BUS_B, (~(1 << bit)) & 0xFF);

      final var towardA = new TtlTestInstanceState(gate, false);
      present(towardA, 0x00, 1 << bit, true, true, true, false, false, false);
      gate.propagate(towardA);
      assertBus(towardA, BUS_A, (~(1 << bit)) & 0xFF);
    }
  }

  @Test
  void risingLatchEnableStoresAtoBAndHoldsIt() {
    final var gate = new Ttl74544();
    final var state = new TtlTestInstanceState(gate, false);
    present(state, 0x55, 0x00, false, false, false, true, true, true);
    gate.propagate(state);
    assertBus(state, BUS_B, 0xAA);

    present(state, 0xAA, 0x00, false, true, false, true, true, true);
    gate.propagate(state);
    assertBus(state, BUS_B, 0x55);

    present(state, 0x0F, 0x00, false, true, false, true, true, true);
    gate.propagate(state);
    assertBus(state, BUS_B, 0x55);
  }

  @Test
  void risingPathEnableStoresAndReleasesTheOutput() {
    final var gate = new Ttl74544();
    final var state = new TtlTestInstanceState(gate, false);
    present(state, 0x00, 0x81, true, true, true, false, false, false);
    gate.propagate(state);
    assertBus(state, BUS_A, 0x7E);

    present(state, 0x00, 0x18, true, true, true, true, false, false);
    gate.propagate(state);
    assertBusUnknown(state, BUS_A);

    present(state, 0x00, 0x18, true, true, true, false, true, false);
    gate.propagate(state);
    assertBus(state, BUS_A, 0xE7);
  }

  @Test
  void highPathEnableBlocksTheInputEvenWhileLatchEnableIsLow() {
    final var gate = new Ttl74544();
    final var state = new TtlTestInstanceState(gate, false);
    present(state, 0x01, 0x00, false, false, false, true, true, true);
    gate.propagate(state);

    present(state, 0x01, 0x00, true, false, false, true, true, true);
    gate.propagate(state);
    present(state, 0x80, 0x00, true, false, false, true, true, true);
    gate.propagate(state);
    present(state, 0x80, 0x00, true, true, false, true, true, true);
    gate.propagate(state);
    present(state, 0x80, 0x00, false, true, false, true, true, true);
    gate.propagate(state);
    assertBus(state, BUS_B, 0xFE);
  }

  @Test
  void outputEnableReleasesTheBusWithoutChangingTheLatch() {
    final var gate = new Ttl74544();
    final var state = new TtlTestInstanceState(gate, false);
    present(state, 0x0F, 0x00, false, false, false, true, true, true);
    gate.propagate(state);
    present(state, 0x0F, 0x00, false, true, false, true, true, true);
    gate.propagate(state);

    present(state, 0xF0, 0x00, false, true, true, true, true, true);
    gate.propagate(state);
    assertBusUnknown(state, BUS_A);
    assertBusUnknown(state, BUS_B);

    present(state, 0xF0, 0x00, false, true, false, true, true, true);
    gate.propagate(state);
    assertBus(state, BUS_B, 0xF0);
  }

  @Test
  void transparentModeStillStoresWhileTheOutputIsReleased() {
    final var gate = new Ttl74544();
    final var state = new TtlTestInstanceState(gate, false);
    present(state, 0x3C, 0x00, false, false, true, true, true, true);
    gate.propagate(state);
    assertBusUnknown(state, BUS_B);

    present(state, 0x3C, 0x00, false, true, true, true, true, true);
    gate.propagate(state);
    present(state, 0x00, 0x00, false, true, true, true, true, true);
    gate.propagate(state);
    present(state, 0x00, 0x00, false, true, false, true, true, true);
    gate.propagate(state);
    assertBus(state, BUS_B, 0xC3);
  }

  @Test
  void unknownControlsReleaseTheBusAndDoNotOpenTheLatch() {
    final var gate = new Ttl74544();
    final var controls = new Value[] {Value.UNKNOWN, Value.ERROR};

    for (final var control : controls) {
      final var state = new TtlTestInstanceState(gate, false);
      present(state, 0x55, 0x00, false, false, false, true, true, true);
      gate.propagate(state);
      present(state, 0xAA, 0x00, false, true, false, true, true, true);
      gate.propagate(state);

      state.setPortValue(Ttl74544.PORT_INDEX_nLEAB, control);
      state.setPortValue(Ttl74544.PORT_INDEX_A0, Value.FALSE);
      gate.propagate(state);
      assertBus(state, BUS_B, 0x55);

      state.setPortValue(Ttl74544.PORT_INDEX_nEAB, control);
      gate.propagate(state);
      assertBusUnknown(state, BUS_B);

      present(state, 0xAA, 0x00, false, true, false, true, true, true);
      gate.propagate(state);
      assertBus(state, BUS_B, 0x55);
    }
  }

  @Test
  void unknownSourceBitBecomesAnErrorOnTheDrivenBus() {
    final var gate = new Ttl74544();
    final var state = new TtlTestInstanceState(gate, false);
    present(state, 0x00, 0x00, false, false, false, true, true, true);
    state.setPortValue(Ttl74544.PORT_INDEX_A0, Value.UNKNOWN);
    gate.propagate(state);

    assertEquals(Value.ERROR, state.getPortValue(Ttl74544.PORT_INDEX_B0));
    for (var bit = 1; bit < 8; bit++) {
      assertEquals(Value.TRUE, state.getPortValue(BUS_B[bit]), "B" + bit);
    }
  }

  @Test
  void bothDirectionsDriveTheirStoredWordsWithoutFeedback() {
    final var gate = new Ttl74544();
    final var state = new TtlTestInstanceState(gate, false);
    present(state, 0x55, 0x00, false, false, true, true, true, true);
    gate.propagate(state);
    present(state, 0x55, 0x00, false, true, true, true, true, true);
    gate.propagate(state);
    present(state, 0x00, 0x0F, true, true, true, false, false, true);
    gate.propagate(state);
    present(state, 0x00, 0x0F, true, true, true, false, true, true);
    gate.propagate(state);

    present(state, 0x00, 0x00, false, true, false, false, true, false);
    gate.propagate(state);
    assertBus(state, BUS_B, 0xAA);
    assertBus(state, BUS_A, 0xF0);

    gate.propagate(state);
    assertBus(state, BUS_B, 0xAA);
    assertBus(state, BUS_A, 0xF0);
  }

  @Test
  void invalidExposedPowerInputsReleaseBothBuses() {
    final var gate = new Ttl74544();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(Ttl74544.PORT_INDEX_GND, Value.FALSE);
    state.setPortValue(Ttl74544.PORT_INDEX_VCC, Value.TRUE);
    present(state, 0x55, 0x00, false, false, false, true, true, true);
    gate.propagate(state);
    present(state, 0x55, 0x00, false, true, false, true, true, true);
    gate.propagate(state);
    assertBus(state, BUS_B, 0xAA);

    state.setPortValue(Ttl74544.PORT_INDEX_VCC, Value.FALSE);
    present(state, 0x00, 0x00, false, true, false, true, true, true);
    gate.propagate(state);
    assertBusUnknown(state, BUS_A);
    assertBusUnknown(state, BUS_B);

    state.setPortValue(Ttl74544.PORT_INDEX_VCC, Value.TRUE);
    present(state, 0x00, 0x00, false, true, false, true, true, true);
    gate.propagate(state);
    assertBus(state, BUS_B, 0xAA);

    state.setPortValue(Ttl74544.PORT_INDEX_GND, Value.TRUE);
    gate.propagate(state);
    assertBusUnknown(state, BUS_A);
    assertBusUnknown(state, BUS_B);
  }

  private static void present(
      TtlTestInstanceState state,
      int busA,
      int busB,
      boolean nEab,
      boolean nLeab,
      boolean nOeab,
      boolean nEba,
      boolean nLeba,
      boolean nOeba) {
    state.setPortValue(Ttl74544.PORT_INDEX_nEAB, level(nEab));
    state.setPortValue(Ttl74544.PORT_INDEX_nLEAB, level(nLeab));
    state.setPortValue(Ttl74544.PORT_INDEX_nOEAB, level(nOeab));
    state.setPortValue(Ttl74544.PORT_INDEX_nEBA, level(nEba));
    state.setPortValue(Ttl74544.PORT_INDEX_nLEBA, level(nLeba));
    state.setPortValue(Ttl74544.PORT_INDEX_nOEBA, level(nOeba));
    for (var bit = 0; bit < 8; bit++) {
      state.setPortValue(BUS_A[bit], bitValue(busA, bit));
      state.setPortValue(BUS_B[bit], bitValue(busB, bit));
    }
  }

  private static Value level(boolean high) {
    return high ? Value.TRUE : Value.FALSE;
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
