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
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC4020 14-stage binary ripple counter. */
class Ttl744020Test {
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;
  private static final BitWidth WIDTH = BitWidth.create(14);
  private static final int[] VISIBLE_BITS = {0, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
  private static final int[] VISIBLE_PORTS = {
    Ttl744020.PORT_INDEX_Q1,
    Ttl744020.PORT_INDEX_Q4,
    Ttl744020.PORT_INDEX_Q5,
    Ttl744020.PORT_INDEX_Q6,
    Ttl744020.PORT_INDEX_Q7,
    Ttl744020.PORT_INDEX_Q8,
    Ttl744020.PORT_INDEX_Q9,
    Ttl744020.PORT_INDEX_Q10,
    Ttl744020.PORT_INDEX_Q11,
    Ttl744020.PORT_INDEX_Q12,
    Ttl744020.PORT_INDEX_Q13,
    Ttl744020.PORT_INDEX_Q14
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl744020();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744020.PORT_INDEX_Q12, 10, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744020.PORT_INDEX_Q13, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744020.PORT_INDEX_Q14, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744020.PORT_INDEX_Q6, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744020.PORT_INDEX_Q5, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744020.PORT_INDEX_Q7, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744020.PORT_INDEX_Q4, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744020.PORT_INDEX_Q1, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744020.PORT_INDEX_CP, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744020.PORT_INDEX_MR, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744020.PORT_INDEX_Q9, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744020.PORT_INDEX_Q8, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744020.PORT_INDEX_Q10, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744020.PORT_INDEX_Q11, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 150, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void visibleOutputsFollowBinaryStageWeights() {
    final var gate = new Ttl744020();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    assertCount(state, 0);

    for (var count = 1; count <= 8; count++) {
      pulse(gate, state);
      assertCount(state, count);
    }
    assertEquals(Value.FALSE, state.getPortValue(Ttl744020.PORT_INDEX_Q1));
    assertEquals(Value.TRUE, state.getPortValue(Ttl744020.PORT_INDEX_Q4));

    for (var count = 9; count <= 64; count++) {
      pulse(gate, state);
      assertCount(state, count);
    }

    clock(gate, state, 8192 - 64);
    assertCount(state, 8192);
    assertEquals(Value.TRUE, state.getPortValue(Ttl744020.PORT_INDEX_Q14));
    clock(gate, state, 8191);
    assertCount(state, 16383);
    pulse(gate, state);
    assertCount(state, 0);
  }

  @Test
  void masterResetClearsAndOverridesTheClock() {
    final var gate = new Ttl744020();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    clock(gate, state, 8);
    assertCount(state, 8);

    state.setPortValue(Ttl744020.PORT_INDEX_CP, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl744020.PORT_INDEX_MR, Value.TRUE);
    state.setPortValue(Ttl744020.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 0);

    pulse(gate, state);
    assertCount(state, 0);
    state.setPortValue(Ttl744020.PORT_INDEX_MR, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 0);
    clock(gate, state, 8);
    assertCount(state, 8);
  }

  @Test
  void risingAndUnknownClocksDoNotCount() {
    final var gate = new Ttl744020();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    state.setPortValue(Ttl744020.PORT_INDEX_CP, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);

    state.setPortValue(Ttl744020.PORT_INDEX_CP, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl744020.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 0);
  }

  @Test
  void unknownResetDoesNotClearAndAFallingEdgeStillCounts() {
    final var gate = new Ttl744020();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    clock(gate, state, 8);

    state.setPortValue(Ttl744020.PORT_INDEX_MR, Value.UNKNOWN);
    gate.propagate(state);
    assertCount(state, 8);

    pulse(gate, state);
    assertCount(state, 9);
  }

  @Test
  void undefinedCountBecomesUnknownOnTheNextClock() {
    final var gate = new Ttl744020();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    final var data = new TtlRegisterData(WIDTH);
    data.setValue(Value.createUnknown(WIDTH));
    state.setData(data);

    pulse(gate, state);
    assertUnknownOutputs(state);

    state.setPortValue(Ttl744020.PORT_INDEX_MR, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
  }

  @Test
  void errorInTheCountBecomesErrorOnTheNextClock() {
    final var gate = new Ttl744020();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    final var data = new TtlRegisterData(WIDTH);
    final var bits = Value.createKnown(WIDTH, 0).getAll();
    bits[0] = Value.ERROR;
    data.setValue(Value.create(bits));
    state.setData(data);

    pulse(gate, state);
    assertErrorOutputs(state);

    state.setPortValue(Ttl744020.PORT_INDEX_MR, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl744020();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    reset(gate, state);
    clock(gate, state, 8);
    assertCount(state, 8);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 8);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  @Test
  void gatedClockIsTheExternalClockPin() {
    final var gate = new Ttl744020();
    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(new int[] {Ttl744020.PORT_INDEX_CP}, gate.clockPinIndex(null));
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void reset(Ttl744020 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl744020.PORT_INDEX_CP, Value.FALSE);
    state.setPortValue(Ttl744020.PORT_INDEX_MR, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl744020.PORT_INDEX_MR, Value.FALSE);
    gate.propagate(state);
  }

  private static void pulse(Ttl744020 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl744020.PORT_INDEX_CP, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl744020.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
  }

  private static void clock(Ttl744020 gate, TtlTestInstanceState state, int pulses) {
    for (var i = 0; i < pulses; i++) {
      pulse(gate, state);
    }
  }

  private static void assertCount(TtlTestInstanceState state, int count) {
    for (var i = 0; i < VISIBLE_BITS.length; i++) {
      final var expected = ((count >> VISIBLE_BITS[i]) & 1) == 0 ? Value.FALSE : Value.TRUE;
      assertEquals(expected, state.getPortValue(VISIBLE_PORTS[i]), "count " + count);
    }
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var port : VISIBLE_PORTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(port));
    }
  }

  private static void assertErrorOutputs(TtlTestInstanceState state) {
    for (final var port : VISIBLE_PORTS) {
      assertEquals(Value.ERROR, state.getPortValue(port));
    }
  }
}
