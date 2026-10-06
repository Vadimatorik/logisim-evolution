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

/** Tests for the TTL 74136 quad open-drain XOR gate. */
class Ttl74136Test {
  private static final int[][] GATES = {
    {Ttl74136.PORT_INDEX_1A, Ttl74136.PORT_INDEX_1B, Ttl74136.PORT_INDEX_1Y},
    {Ttl74136.PORT_INDEX_2A, Ttl74136.PORT_INDEX_2B, Ttl74136.PORT_INDEX_2Y},
    {Ttl74136.PORT_INDEX_3A, Ttl74136.PORT_INDEX_3B, Ttl74136.PORT_INDEX_3Y},
    {Ttl74136.PORT_INDEX_4A, Ttl74136.PORT_INDEX_4B, Ttl74136.PORT_INDEX_4Y}
  };

  private static final int[] OUTPUTS = {
    Ttl74136.PORT_INDEX_1Y,
    Ttl74136.PORT_INDEX_2Y,
    Ttl74136.PORT_INDEX_3Y,
    Ttl74136.PORT_INDEX_4Y
  };

  @Test
  void usesPhysicalPinoutAndKeepsPowerPortsLast() {
    final var gate = new Ttl74136();
    final var hiddenPower = TtlTestInstanceState.createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74136.PORT_INDEX_1A, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74136.PORT_INDEX_1B, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74136.PORT_INDEX_1Y, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74136.PORT_INDEX_2A, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74136.PORT_INDEX_2B, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74136.PORT_INDEX_2Y, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74136.PORT_INDEX_3Y, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74136.PORT_INDEX_3A, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74136.PORT_INDEX_3B, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74136.PORT_INDEX_4Y, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74136.PORT_INDEX_4A, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74136.PORT_INDEX_4B, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = TtlTestInstanceState.createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertPort(shownPower, 12, 130, 30, EndData.INPUT_ONLY); // Physical pin 7: GND
    assertPort(shownPower, 13, 10, -30, EndData.INPUT_ONLY); // Physical pin 14: VCC
  }

  @Test
  void eachGateDrivesLowOnlyWhenItsInputsAreEqual() {
    final var gate = new Ttl74136();
    final var state = new TtlTestInstanceState(gate, false);
    final var levels = new Value[] {Value.FALSE, Value.TRUE};

    for (final var pins : GATES) {
      for (final var inputA : levels) {
        for (final var inputB : levels) {
          releaseAll(state);
          state.setPortValue(pins[0], inputA);
          state.setPortValue(pins[1], inputB);
          gate.propagate(state);

          final var expected = inputA == inputB ? Value.FALSE : Value.UNKNOWN;
          assertEquals(expected, state.getPortValue(pins[2]));
          for (final var output : OUTPUTS) {
            if (output != pins[2]) {
              assertEquals(Value.UNKNOWN, state.getPortValue(output));
            }
          }
        }
      }
    }
  }

  @Test
  void unsettledInputDoesNotDriveTheOutputLow() {
    final var gate = new Ttl74136();
    final var state = new TtlTestInstanceState(gate, false);

    for (final var unsettled : new Value[] {Value.UNKNOWN, Value.ERROR}) {
      releaseAll(state);
      state.setPortValue(Ttl74136.PORT_INDEX_1A, unsettled);
      state.setPortValue(Ttl74136.PORT_INDEX_1B, Value.FALSE);
      gate.propagate(state);
      assertEquals(Value.ERROR, state.getPortValue(Ttl74136.PORT_INDEX_1Y));

      state.setPortValue(Ttl74136.PORT_INDEX_1B, Value.TRUE);
      gate.propagate(state);
      assertEquals(Value.ERROR, state.getPortValue(Ttl74136.PORT_INDEX_1Y));
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74136();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(12, Value.FALSE);
    state.setPortValue(13, Value.TRUE);
    driveLow(state, gate);
    assertEquals(Value.FALSE, state.getPortValue(Ttl74136.PORT_INDEX_1Y));

    state.setPortValue(13, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(13, Value.TRUE);
    driveLow(state, gate);
    assertEquals(Value.FALSE, state.getPortValue(Ttl74136.PORT_INDEX_1Y));

    state.setPortValue(12, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  /** Leaves every gate released: unequal inputs make an open-drain XOR float. */
  private static void releaseAll(TtlTestInstanceState state) {
    for (final var pins : GATES) {
      state.setPortValue(pins[0], Value.FALSE);
      state.setPortValue(pins[1], Value.TRUE);
    }
  }

  private static void driveLow(TtlTestInstanceState state, Ttl74136 gate) {
    releaseAll(state);
    state.setPortValue(Ttl74136.PORT_INDEX_1A, Value.FALSE);
    state.setPortValue(Ttl74136.PORT_INDEX_1B, Value.FALSE);
    gate.propagate(state);
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var output : OUTPUTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(output));
    }
  }
}
