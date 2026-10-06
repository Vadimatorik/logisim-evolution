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

/** Tests for the TTL 7412 triple open-drain NAND gate. */
class Ttl7412Test {
  private static final int[][] GATES = {
    {Ttl7412.PORT_INDEX_1A, Ttl7412.PORT_INDEX_1B, Ttl7412.PORT_INDEX_1C, Ttl7412.PORT_INDEX_1Y},
    {Ttl7412.PORT_INDEX_2A, Ttl7412.PORT_INDEX_2B, Ttl7412.PORT_INDEX_2C, Ttl7412.PORT_INDEX_2Y},
    {Ttl7412.PORT_INDEX_3A, Ttl7412.PORT_INDEX_3B, Ttl7412.PORT_INDEX_3C, Ttl7412.PORT_INDEX_3Y}
  };

  private static final int[] OUTPUTS = {
    Ttl7412.PORT_INDEX_1Y, Ttl7412.PORT_INDEX_2Y, Ttl7412.PORT_INDEX_3Y
  };

  @Test
  void usesPhysicalPinoutAndKeepsPowerPortsLast() {
    final var gate = new Ttl7412();
    final var hiddenPower = TtlTestInstanceState.createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl7412.PORT_INDEX_1A, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7412.PORT_INDEX_1B, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7412.PORT_INDEX_2A, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7412.PORT_INDEX_2B, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7412.PORT_INDEX_2C, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7412.PORT_INDEX_2Y, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7412.PORT_INDEX_3Y, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7412.PORT_INDEX_3A, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7412.PORT_INDEX_3B, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7412.PORT_INDEX_3C, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7412.PORT_INDEX_1Y, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7412.PORT_INDEX_1C, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = TtlTestInstanceState.createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertPort(shownPower, 12, 130, 30, EndData.INPUT_ONLY); // Physical pin 7: GND
    assertPort(shownPower, 13, 10, -30, EndData.INPUT_ONLY); // Physical pin 14: VCC
  }

  @Test
  void eachGateReleasesUnlessAllInputsAreHigh() {
    final var gate = new Ttl7412();
    final var state = new TtlTestInstanceState(gate, false);
    final var levels = new Value[] {Value.FALSE, Value.TRUE};

    for (final var pins : GATES) {
      for (final var inputA : levels) {
        for (final var inputB : levels) {
          for (final var inputC : levels) {
            releaseAll(state);
            state.setPortValue(pins[0], inputA);
            state.setPortValue(pins[1], inputB);
            state.setPortValue(pins[2], inputC);
            gate.propagate(state);

            final var expected =
                inputA == Value.TRUE && inputB == Value.TRUE && inputC == Value.TRUE
                    ? Value.FALSE
                    : Value.UNKNOWN;
            assertEquals(expected, state.getPortValue(pins[3]));
            for (final var output : OUTPUTS) {
              if (output != pins[3]) {
                assertEquals(Value.UNKNOWN, state.getPortValue(output));
              }
            }
          }
        }
      }
    }
  }

  @Test
  void lowInputReleasesOutputWhenOtherInputsAreNotDetermined() {
    final var gate = new Ttl7412();
    final var state = new TtlTestInstanceState(gate, false);

    for (final var unsettled : new Value[] {Value.UNKNOWN, Value.ERROR}) {
      releaseAll(state);
      state.setPortValue(Ttl7412.PORT_INDEX_1A, unsettled);
      state.setPortValue(Ttl7412.PORT_INDEX_1B, unsettled);
      state.setPortValue(Ttl7412.PORT_INDEX_1C, Value.FALSE);
      gate.propagate(state);
      assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7412.PORT_INDEX_1Y));

      state.setPortValue(Ttl7412.PORT_INDEX_1C, Value.TRUE);
      gate.propagate(state);
      assertEquals(Value.ERROR, state.getPortValue(Ttl7412.PORT_INDEX_1Y));
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl7412();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(12, Value.FALSE);
    state.setPortValue(13, Value.TRUE);
    driveLow(state, gate);
    assertEquals(Value.FALSE, state.getPortValue(Ttl7412.PORT_INDEX_1Y));

    state.setPortValue(13, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(13, Value.TRUE);
    driveLow(state, gate);
    assertEquals(Value.FALSE, state.getPortValue(Ttl7412.PORT_INDEX_1Y));

    state.setPortValue(12, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void releaseAll(TtlTestInstanceState state) {
    for (final var pins : GATES) {
      state.setPortValue(pins[0], Value.FALSE);
      state.setPortValue(pins[1], Value.FALSE);
      state.setPortValue(pins[2], Value.FALSE);
    }
  }

  private static void driveLow(TtlTestInstanceState state, Ttl7412 gate) {
    releaseAll(state);
    state.setPortValue(Ttl7412.PORT_INDEX_1A, Value.TRUE);
    state.setPortValue(Ttl7412.PORT_INDEX_1B, Value.TRUE);
    state.setPortValue(Ttl7412.PORT_INDEX_1C, Value.TRUE);
    gate.propagate(state);
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var output : OUTPUTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(output));
    }
  }
}
