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

/** Functional tests for the 74HC243 quad non-inverting bus transceiver. */
class Ttl74243Test {
  private static final int[] BUS_A = {
    Ttl74243.PORT_INDEX_A0,
    Ttl74243.PORT_INDEX_A1,
    Ttl74243.PORT_INDEX_A2,
    Ttl74243.PORT_INDEX_A3
  };
  private static final int[] BUS_B = {
    Ttl74243.PORT_INDEX_B0,
    Ttl74243.PORT_INDEX_B1,
    Ttl74243.PORT_INDEX_B2,
    Ttl74243.PORT_INDEX_B3
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74243();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(10, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74243.PORT_INDEX_OEA, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74243.PORT_INDEX_A0, 50, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74243.PORT_INDEX_A1, 70, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74243.PORT_INDEX_A2, 90, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74243.PORT_INDEX_A3, 110, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74243.PORT_INDEX_B3, 130, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74243.PORT_INDEX_B2, 110, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74243.PORT_INDEX_B1, 90, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74243.PORT_INDEX_B0, 70, -30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74243.PORT_INDEX_OEB, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(12, shownPower.getPorts().size());
    assertPort(shownPower, Ttl74243.PORT_INDEX_GND, 130, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, Ttl74243.PORT_INDEX_VCC, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void enablesAreNotClocks() {
    assertFalse(new Ttl74243().checkForGatedClocks(null));
  }

  @Test
  void bothEnablesLowCopyAOntoB() {
    final var gate = new Ttl74243();
    final var patterns = new int[] {0x0, 0xF, 0x5, 0xA, 0x1, 0x8, 0x3, 0xC};

    for (final var pattern : patterns) {
      final var state = new TtlTestInstanceState(gate, false);
      present(state, pattern, 0x0, Value.FALSE, Value.FALSE);
      gate.propagate(state);
      assertBus(state, BUS_B, pattern);
      assertBusUnknown(state, BUS_A);
    }
  }

  @Test
  void bothEnablesHighCopyBOntoA() {
    final var gate = new Ttl74243();
    final var patterns = new int[] {0x0, 0xF, 0x5, 0xA, 0x1, 0x8, 0x3, 0xC};

    for (final var pattern : patterns) {
      final var state = new TtlTestInstanceState(gate, false);
      present(state, 0x0, pattern, Value.TRUE, Value.TRUE);
      gate.propagate(state);
      assertBus(state, BUS_A, pattern);
      assertBusUnknown(state, BUS_B);
    }
  }

  @Test
  void eachBitCopiesOntoItsOwnPair() {
    final var gate = new Ttl74243();

    for (var bit = 0; bit < 4; bit++) {
      final var towardB = new TtlTestInstanceState(gate, false);
      present(towardB, 1 << bit, 0xF, Value.FALSE, Value.FALSE);
      gate.propagate(towardB);
      assertBus(towardB, BUS_B, 1 << bit);

      final var towardA = new TtlTestInstanceState(gate, false);
      present(towardA, 0xF, 1 << bit, Value.TRUE, Value.TRUE);
      gate.propagate(towardA);
      assertBus(towardA, BUS_A, 1 << bit);
    }
  }

  @Test
  void differingEnablesReleaseBothBuses() {
    final var gate = new Ttl74243();
    final var enables =
        new Value[][] {
          {Value.TRUE, Value.FALSE},
          {Value.FALSE, Value.TRUE}
        };

    for (final var enable : enables) {
      final var state = new TtlTestInstanceState(gate, false);
      present(state, 0x5, 0x3, enable[0], enable[1]);
      gate.propagate(state);
      assertBusUnknown(state, BUS_A);
      assertBusUnknown(state, BUS_B);
    }
  }

  @Test
  void unknownControlsReleaseBothBuses() {
    final var gate = new Ttl74243();
    final var controls = new Value[] {Value.UNKNOWN, Value.ERROR};

    for (final var control : controls) {
      final var unknownOea = new TtlTestInstanceState(gate, false);
      present(unknownOea, 0x5, 0x3, control, Value.FALSE);
      gate.propagate(unknownOea);
      assertBusUnknown(unknownOea, BUS_A);
      assertBusUnknown(unknownOea, BUS_B);

      final var unknownOeb = new TtlTestInstanceState(gate, false);
      present(unknownOeb, 0x5, 0x3, Value.TRUE, control);
      gate.propagate(unknownOeb);
      assertBusUnknown(unknownOeb, BUS_A);
      assertBusUnknown(unknownOeb, BUS_B);
    }
  }

  @Test
  void unknownSourceBitBecomesAnErrorOnTheDrivenBus() {
    final var gate = new Ttl74243();
    final var towardB = new TtlTestInstanceState(gate, false);
    present(towardB, 0x0, 0x0, Value.FALSE, Value.FALSE);
    towardB.setPortValue(Ttl74243.PORT_INDEX_A0, Value.UNKNOWN);
    gate.propagate(towardB);

    assertEquals(Value.ERROR, towardB.getPortValue(Ttl74243.PORT_INDEX_B0));
    for (var bit = 1; bit < 4; bit++) {
      assertEquals(Value.FALSE, towardB.getPortValue(BUS_B[bit]), "B" + bit);
    }

    final var towardA = new TtlTestInstanceState(gate, false);
    present(towardA, 0x0, 0x0, Value.TRUE, Value.TRUE);
    towardA.setPortValue(Ttl74243.PORT_INDEX_B0, Value.UNKNOWN);
    gate.propagate(towardA);

    assertEquals(Value.ERROR, towardA.getPortValue(Ttl74243.PORT_INDEX_A0));
    for (var bit = 1; bit < 4; bit++) {
      assertEquals(Value.FALSE, towardA.getPortValue(BUS_A[bit]), "A" + bit);
    }
  }

  @Test
  void invalidExposedPowerInputsReleaseBothBuses() {
    final var gate = new Ttl74243();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(Ttl74243.PORT_INDEX_GND, Value.FALSE);
    state.setPortValue(Ttl74243.PORT_INDEX_VCC, Value.TRUE);
    present(state, 0x5, 0x0, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0x5);

    state.setPortValue(Ttl74243.PORT_INDEX_VCC, Value.FALSE);
    gate.propagate(state);
    assertBusUnknown(state, BUS_A);
    assertBusUnknown(state, BUS_B);

    state.setPortValue(Ttl74243.PORT_INDEX_VCC, Value.TRUE);
    present(state, 0x5, 0x0, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertBus(state, BUS_B, 0x5);

    state.setPortValue(Ttl74243.PORT_INDEX_GND, Value.TRUE);
    gate.propagate(state);
    assertBusUnknown(state, BUS_A);
    assertBusUnknown(state, BUS_B);
  }

  private static void present(
      TtlTestInstanceState state, int busA, int busB, Value outputEnableA, Value outputEnableB) {
    state.setPortValue(Ttl74243.PORT_INDEX_OEA, outputEnableA);
    state.setPortValue(Ttl74243.PORT_INDEX_OEB, outputEnableB);
    for (var bit = 0; bit < 4; bit++) {
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
