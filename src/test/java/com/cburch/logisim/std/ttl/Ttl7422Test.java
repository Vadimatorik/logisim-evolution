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
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Tests for the TTL 7422 dual 4-input open-collector NAND gate. */
class Ttl7422Test {
  private static final int[][] GATES = {
    {
      Ttl7422.PORT_INDEX_1A,
      Ttl7422.PORT_INDEX_1B,
      Ttl7422.PORT_INDEX_1C,
      Ttl7422.PORT_INDEX_1D,
      Ttl7422.PORT_INDEX_1Y
    },
    {
      Ttl7422.PORT_INDEX_2A,
      Ttl7422.PORT_INDEX_2B,
      Ttl7422.PORT_INDEX_2C,
      Ttl7422.PORT_INDEX_2D,
      Ttl7422.PORT_INDEX_2Y
    }
  };

  private static final int GND_PORT = 10;
  private static final int VCC_PORT = 11;

  @Test
  void usesPhysicalPinoutAndKeepsPowerPortsLast() {
    final var gate = new Ttl7422();
    final var hiddenPower = TtlTestInstanceState.createInstance(gate, false);

    assertEquals(10, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl7422.PORT_INDEX_1A, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7422.PORT_INDEX_1B, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7422.PORT_INDEX_1C, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7422.PORT_INDEX_1D, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7422.PORT_INDEX_1Y, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7422.PORT_INDEX_2Y, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7422.PORT_INDEX_2A, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7422.PORT_INDEX_2B, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7422.PORT_INDEX_2C, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7422.PORT_INDEX_2D, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = TtlTestInstanceState.createInstance(gate, true);
    assertEquals(12, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 130, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void eachGateDrivesLowOnlyWhenEveryInputIsHigh() {
    final var gate = new Ttl7422();
    final var state = new TtlTestInstanceState(gate, false);

    for (var gateIndex = 0; gateIndex < GATES.length; gateIndex++) {
      for (var mask = 0; mask < 16; mask++) {
        driveBothLow(state);
        final var pins = GATES[gateIndex];
        for (var bit = 0; bit < 4; bit++) {
          state.setPortValue(pins[bit], ((mask >> bit) & 1) == 1 ? Value.TRUE : Value.FALSE);
        }
        gate.propagate(state);
        assertEquals(mask == 0b1111 ? Value.FALSE : Value.UNKNOWN, state.getPortValue(pins[4]));
        assertEquals(Value.UNKNOWN, state.getPortValue(GATES[1 - gateIndex][4]));
      }
    }
  }

  @Test
  void lowInputReleasesOutputEvenWhenOtherInputsAreUnsettled() {
    final var gate = new Ttl7422();
    final var state = new TtlTestInstanceState(gate, false);

    for (final var unsettled : new Value[] {Value.UNKNOWN, Value.ERROR}) {
      driveBothLow(state);
      state.setPortValue(Ttl7422.PORT_INDEX_1A, Value.FALSE);
      state.setPortValue(Ttl7422.PORT_INDEX_1B, unsettled);
      state.setPortValue(Ttl7422.PORT_INDEX_1C, Value.TRUE);
      state.setPortValue(Ttl7422.PORT_INDEX_1D, Value.TRUE);
      gate.propagate(state);
      assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7422.PORT_INDEX_1Y));
      assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7422.PORT_INDEX_2Y));
    }
  }

  @Test
  void unsettledInputsMakeOutputErrorWhenNoneIsLow() {
    final var gate = new Ttl7422();
    final var state = new TtlTestInstanceState(gate, false);

    for (final var unsettled : new Value[] {Value.UNKNOWN, Value.ERROR}) {
      driveBothLow(state);
      state.setPortValue(Ttl7422.PORT_INDEX_1A, unsettled);
      state.setPortValue(Ttl7422.PORT_INDEX_1B, Value.TRUE);
      state.setPortValue(Ttl7422.PORT_INDEX_1C, Value.TRUE);
      state.setPortValue(Ttl7422.PORT_INDEX_1D, Value.TRUE);
      gate.propagate(state);
      assertEquals(Value.ERROR, state.getPortValue(Ttl7422.PORT_INDEX_1Y));
      assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7422.PORT_INDEX_2Y));
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl7422();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    driveBothHigh(state);
    gate.propagate(state);
    assertEquals(Value.FALSE, state.getPortValue(Ttl7422.PORT_INDEX_1Y));
    assertEquals(Value.FALSE, state.getPortValue(Ttl7422.PORT_INDEX_2Y));

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    driveBothHigh(state);
    gate.propagate(state);
    assertEquals(Value.FALSE, state.getPortValue(Ttl7422.PORT_INDEX_1Y));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void driveBothLow(TtlTestInstanceState state) {
    for (final var pins : GATES) {
      for (var input = 0; input < 4; input++) {
        state.setPortValue(pins[input], Value.FALSE);
      }
    }
  }

  private static void driveBothHigh(TtlTestInstanceState state) {
    for (final var pins : GATES) {
      for (var input = 0; input < 4; input++) {
        state.setPortValue(pins[input], Value.TRUE);
      }
    }
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7422.PORT_INDEX_1Y));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7422.PORT_INDEX_2Y));
  }
}
