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

class Ttl7428Test {
  private static final int GND_PORT = 12;
  private static final int VCC_PORT = 13;

  /** Each row is input A, input B, output Y for one gate. */
  private static final int[][] GATES = {
    {Ttl7428.PORT_1A, Ttl7428.PORT_1B, Ttl7428.PORT_1Y},
    {Ttl7428.PORT_2A, Ttl7428.PORT_2B, Ttl7428.PORT_2Y},
    {Ttl7428.PORT_3A, Ttl7428.PORT_3B, Ttl7428.PORT_3Y},
    {Ttl7428.PORT_4A, Ttl7428.PORT_4B, Ttl7428.PORT_4Y}
  };

  private static final Value[][] TRUTH_TABLE = {
    {Value.FALSE, Value.FALSE, Value.TRUE},
    {Value.FALSE, Value.TRUE, Value.FALSE},
    {Value.TRUE, Value.FALSE, Value.FALSE},
    {Value.TRUE, Value.TRUE, Value.FALSE}
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl7428();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl7428.PORT_1Y, 10, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7428.PORT_1A, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7428.PORT_1B, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7428.PORT_2Y, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7428.PORT_2A, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7428.PORT_2B, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7428.PORT_3A, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7428.PORT_3B, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7428.PORT_3Y, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7428.PORT_4A, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7428.PORT_4B, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7428.PORT_4Y, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertEquals(Location.create(130, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void eachGateFollowsTheNorTruthTable() {
    final var gate = new Ttl7428();

    for (final var pins : GATES) {
      for (final var row : TRUTH_TABLE) {
        final var state = inputsLow(gate, false);
        state.setPortValue(pins[0], row[0]);
        state.setPortValue(pins[1], row[1]);
        gate.propagate(state);

        assertEquals(row[2], state.getPortValue(pins[2]));
        for (final var other : GATES) {
          if (other == pins) continue;
          assertEquals(Value.TRUE, state.getPortValue(other[2]));
        }
      }
    }
  }

  @Test
  void gatesComputeIndependentNorFunctions() {
    final var gate = new Ttl7428();
    final var state = inputsLow(gate, false);
    for (var index = 0; index < GATES.length; index++) {
      state.setPortValue(GATES[index][0], TRUTH_TABLE[index][0]);
      state.setPortValue(GATES[index][1], TRUTH_TABLE[index][1]);
    }
    gate.propagate(state);

    for (var index = 0; index < GATES.length; index++) {
      assertEquals(TRUTH_TABLE[index][2], state.getPortValue(GATES[index][2]));
    }
  }

  @Test
  void unknownInputsFollowTheOneBitNorAlgebra() {
    final var gate = new Ttl7428();

    final var highUnknown = inputsLow(gate, false);
    highUnknown.setPortValue(Ttl7428.PORT_1A, Value.TRUE);
    highUnknown.setPortValue(Ttl7428.PORT_1B, Value.UNKNOWN);
    gate.propagate(highUnknown);
    assertEquals(Value.FALSE, highUnknown.getPortValue(Ttl7428.PORT_1Y));

    // A 1-bit OR of FALSE and UNKNOWN is ERROR, so the NOR output is ERROR as well.
    final var lowUnknown = inputsLow(gate, false);
    lowUnknown.setPortValue(Ttl7428.PORT_2A, Value.FALSE);
    lowUnknown.setPortValue(Ttl7428.PORT_2B, Value.UNKNOWN);
    gate.propagate(lowUnknown);
    assertEquals(Value.ERROR, lowUnknown.getPortValue(Ttl7428.PORT_2Y));
  }

  @Test
  void exposedPowerPinsMustBeValidBeforeTheOutputsAreDriven() {
    final var gate = new Ttl7428();
    final var state = inputsLow(gate, true);
    state.setPortValue(Ttl7428.PORT_4A, Value.TRUE);
    state.setPortValue(Ttl7428.PORT_4B, Value.TRUE);

    state.setPortValue(GND_PORT, Value.TRUE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertOutputsUnknown(state);

    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertOutputsUnknown(state);

    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.TRUE, state.getPortValue(Ttl7428.PORT_1Y));
    assertEquals(Value.FALSE, state.getPortValue(Ttl7428.PORT_4Y));
  }

  private static TtlTestInstanceState inputsLow(Ttl7428 gate, boolean showPower) {
    final var state = new TtlTestInstanceState(gate, showPower);
    for (final var pins : GATES) {
      state.setPortValue(pins[0], Value.FALSE);
      state.setPortValue(pins[1], Value.FALSE);
    }
    if (showPower) {
      state.setPortValue(GND_PORT, Value.FALSE);
      state.setPortValue(VCC_PORT, Value.TRUE);
    }
    return state;
  }

  private static void assertOutputsUnknown(TtlTestInstanceState state) {
    for (final var pins : GATES) {
      assertEquals(Value.UNKNOWN, state.getPortValue(pins[2]));
    }
  }

  private static void assertPort(Instance instance, int port, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
