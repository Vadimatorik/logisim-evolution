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

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

class Ttl747002Test {
  private static final int[][] GATES = {
    {Ttl747002.PORT_1A, Ttl747002.PORT_1B, Ttl747002.PORT_1Y},
    {Ttl747002.PORT_2A, Ttl747002.PORT_2B, Ttl747002.PORT_2Y},
    {Ttl747002.PORT_3A, Ttl747002.PORT_3B, Ttl747002.PORT_3Y},
    {Ttl747002.PORT_4A, Ttl747002.PORT_4B, Ttl747002.PORT_4Y},
  };

  private static final Value[][] TRUTH = {
    {Value.FALSE, Value.FALSE, Value.TRUE},
    {Value.FALSE, Value.TRUE, Value.FALSE},
    {Value.TRUE, Value.FALSE, Value.FALSE},
    {Value.TRUE, Value.TRUE, Value.FALSE},
  };

  @Test
  void usesThe7402PinoutAndKeepsPowerPortsLast() {
    final var gate = new Ttl747002();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl747002.PORT_1Y, 10, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl747002.PORT_1A, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747002.PORT_1B, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747002.PORT_2Y, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl747002.PORT_2A, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747002.PORT_2B, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747002.PORT_3A, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747002.PORT_3B, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747002.PORT_3Y, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl747002.PORT_4A, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747002.PORT_4B, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747002.PORT_4Y, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertPort(shownPower, Ttl747002.PORT_GND, 130, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, Ttl747002.PORT_VCC, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void eachGateFollowsTheNorTruthTable() {
    final var gate = new Ttl747002();
    final var state = new TtlTestInstanceState(gate, false);

    for (var index = 0; index < GATES.length; index++) {
      for (var row : TRUTH) {
        setInputsLow(state);
        state.setPortValue(GATES[index][0], row[0]);
        state.setPortValue(GATES[index][1], row[1]);
        gate.propagate(state);
        for (var other = 0; other < GATES.length; other++) {
          final var expected = other == index ? row[2] : Value.TRUE;
          assertEquals(expected, state.getPortValue(GATES[other][2]));
        }
      }
    }
  }

  @Test
  void highInputForcesLowOutputAndUnknownOtherwiseBecomesError() {
    final var gate = new Ttl747002();
    final var state = new TtlTestInstanceState(gate, false);
    setInputsLow(state);

    state.setPortValue(Ttl747002.PORT_1A, Value.TRUE);
    state.setPortValue(Ttl747002.PORT_1B, Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(Value.FALSE, state.getPortValue(Ttl747002.PORT_1Y));

    state.setPortValue(Ttl747002.PORT_1A, Value.ERROR);
    state.setPortValue(Ttl747002.PORT_1B, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.FALSE, state.getPortValue(Ttl747002.PORT_1Y));

    // One-bit OR of a non-forcing unknown is ERROR, and NOT keeps that error.
    state.setPortValue(Ttl747002.PORT_1A, Value.FALSE);
    state.setPortValue(Ttl747002.PORT_1B, Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(Value.ERROR, state.getPortValue(Ttl747002.PORT_1Y));

    state.setPortValue(Ttl747002.PORT_2A, Value.UNKNOWN);
    state.setPortValue(Ttl747002.PORT_2B, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.ERROR, state.getPortValue(Ttl747002.PORT_2Y));
    assertEquals(Value.TRUE, state.getPortValue(Ttl747002.PORT_3Y));
    assertEquals(Value.TRUE, state.getPortValue(Ttl747002.PORT_4Y));
  }

  @Test
  void invalidExposedPowerMakesOutputsUnknownUntilPowerReturns() {
    final var gate = new Ttl747002();
    final var state = new TtlTestInstanceState(gate, true);
    setInputsLow(state);
    state.setPortValue(Ttl747002.PORT_GND, Value.FALSE);
    state.setPortValue(Ttl747002.PORT_VCC, Value.TRUE);
    gate.propagate(state);
    assertAllHigh(state);

    state.setPortValue(Ttl747002.PORT_VCC, Value.FALSE);
    gate.propagate(state);
    assertAllUnknown(state);

    state.setPortValue(Ttl747002.PORT_VCC, Value.TRUE);
    gate.propagate(state);
    assertAllHigh(state);

    state.setPortValue(Ttl747002.PORT_GND, Value.TRUE);
    gate.propagate(state);
    assertAllUnknown(state);

    state.setPortValue(Ttl747002.PORT_GND, Value.FALSE);
    gate.propagate(state);
    assertAllHigh(state);
  }

  private static void setInputsLow(TtlTestInstanceState state) {
    for (var pins : GATES) {
      state.setPortValue(pins[0], Value.FALSE);
      state.setPortValue(pins[1], Value.FALSE);
    }
  }

  private static void assertAllHigh(TtlTestInstanceState state) {
    for (var pins : GATES) {
      assertEquals(Value.TRUE, state.getPortValue(pins[2]));
    }
  }

  private static void assertAllUnknown(TtlTestInstanceState state) {
    for (var pins : GATES) {
      assertEquals(Value.UNKNOWN, state.getPortValue(pins[2]));
    }
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }
}
