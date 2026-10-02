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

/** Functional tests for the 74HC174 hex D-type flip-flop with asynchronous clear. */
class Ttl74174Test {
  private static final int STAGES = 6;
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;
  private static final int[] DATA_PORTS = {
    Ttl74174.PORT_INDEX_D1,
    Ttl74174.PORT_INDEX_D2,
    Ttl74174.PORT_INDEX_D3,
    Ttl74174.PORT_INDEX_D4,
    Ttl74174.PORT_INDEX_D5,
    Ttl74174.PORT_INDEX_D6
  };
  private static final int[] OUTPUT_PORTS = {
    Ttl74174.PORT_INDEX_Q1,
    Ttl74174.PORT_INDEX_Q2,
    Ttl74174.PORT_INDEX_Q3,
    Ttl74174.PORT_INDEX_Q4,
    Ttl74174.PORT_INDEX_Q5,
    Ttl74174.PORT_INDEX_Q6
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74174();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74174.PORT_INDEX_nCLR, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74174.PORT_INDEX_Q1, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74174.PORT_INDEX_D1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74174.PORT_INDEX_D2, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74174.PORT_INDEX_Q2, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74174.PORT_INDEX_D3, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74174.PORT_INDEX_Q3, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74174.PORT_INDEX_CLK, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74174.PORT_INDEX_Q4, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74174.PORT_INDEX_D4, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74174.PORT_INDEX_Q5, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74174.PORT_INDEX_D5, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74174.PORT_INDEX_D6, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74174.PORT_INDEX_Q6, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 150, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void risingEdgeStoresEachDataPattern() {
    final var gate = new Ttl74174();
    final var state = cleared(gate);

    for (var pattern = 0; pattern < (1 << STAGES); pattern++) {
      setData(state, pattern);
      risingEdge(gate, state);
      assertPattern(state, pattern);
    }
  }

  @Test
  void outputsHoldUntilTheNextRisingEdge() {
    final var gate = new Ttl74174();
    final var state = cleared(gate);
    setData(state, 0b010101);
    risingEdge(gate, state);
    assertPattern(state, 0b010101);

    setData(state, 0b101010);
    gate.propagate(state);
    assertPattern(state, 0b010101);

    state.setPortValue(Ttl74174.PORT_INDEX_CLK, Value.FALSE);
    gate.propagate(state);
    assertPattern(state, 0b010101);

    setData(state, 0b111000);
    gate.propagate(state);
    assertPattern(state, 0b010101);

    risingEdge(gate, state);
    assertPattern(state, 0b111000);
  }

  @Test
  void clearOverridesTheClockAndHoldsZerosUntilTheNextRisingEdge() {
    final var gate = new Ttl74174();
    final var state = cleared(gate);
    setData(state, 0b111111);
    risingEdge(gate, state);
    assertPattern(state, 0b111111);

    state.setPortValue(Ttl74174.PORT_INDEX_CLK, Value.FALSE);
    state.setPortValue(Ttl74174.PORT_INDEX_nCLR, Value.FALSE);
    gate.propagate(state);
    assertPattern(state, 0);

    state.setPortValue(Ttl74174.PORT_INDEX_CLK, Value.TRUE);
    gate.propagate(state);
    assertPattern(state, 0);

    state.setPortValue(Ttl74174.PORT_INDEX_nCLR, Value.TRUE);
    gate.propagate(state);
    assertPattern(state, 0);

    state.setPortValue(Ttl74174.PORT_INDEX_CLK, Value.FALSE);
    gate.propagate(state);
    risingEdge(gate, state);
    assertPattern(state, 0b111111);
  }

  @Test
  void missingPowerForcesTheOutputsUnknown() {
    final var gate = new Ttl74174();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    state.setPortValue(Ttl74174.PORT_INDEX_nCLR, Value.FALSE);
    state.setPortValue(Ttl74174.PORT_INDEX_CLK, Value.FALSE);
    gate.propagate(state);
    assertPattern(state, 0);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertPattern(state, 0);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  private static TtlTestInstanceState cleared(Ttl74174 gate) {
    final var state = new TtlTestInstanceState(gate, false);
    state.setPortValue(Ttl74174.PORT_INDEX_nCLR, Value.TRUE);
    state.setPortValue(Ttl74174.PORT_INDEX_CLK, Value.FALSE);
    setData(state, 0);
    gate.propagate(state);
    state.setPortValue(Ttl74174.PORT_INDEX_nCLR, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74174.PORT_INDEX_nCLR, Value.TRUE);
    gate.propagate(state);
    return state;
  }

  private static void risingEdge(Ttl74174 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl74174.PORT_INDEX_CLK, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74174.PORT_INDEX_CLK, Value.TRUE);
    gate.propagate(state);
  }

  private static void setData(TtlTestInstanceState state, int pattern) {
    for (var bit = 0; bit < STAGES; bit++) {
      final var level = ((pattern >> bit) & 1) == 1 ? Value.TRUE : Value.FALSE;
      state.setPortValue(DATA_PORTS[bit], level);
    }
  }

  private static void assertPattern(TtlTestInstanceState state, int pattern) {
    for (var bit = 0; bit < STAGES; bit++) {
      final var expected = ((pattern >> bit) & 1) == 1 ? Value.TRUE : Value.FALSE;
      assertEquals(expected, state.getPortValue(OUTPUT_PORTS[bit]));
    }
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var port : OUTPUT_PORTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(port));
    }
  }

  private static void assertPort(Instance instance, int port, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
