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

/** Functional tests for the 74HC4520 dual synchronous 4-bit binary counter. */
class Ttl744520Test {
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;
  private static final BitWidth WIDTH = BitWidth.create(4);
  private static final int[] COUNTER1 = {
    Ttl744520.PORT_INDEX_1Q0,
    Ttl744520.PORT_INDEX_1Q1,
    Ttl744520.PORT_INDEX_1Q2,
    Ttl744520.PORT_INDEX_1Q3
  };
  private static final int[] COUNTER2 = {
    Ttl744520.PORT_INDEX_2Q0,
    Ttl744520.PORT_INDEX_2Q1,
    Ttl744520.PORT_INDEX_2Q2,
    Ttl744520.PORT_INDEX_2Q3
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl744520();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744520.PORT_INDEX_1CP0, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744520.PORT_INDEX_1CP1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744520.PORT_INDEX_1Q0, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744520.PORT_INDEX_1Q1, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744520.PORT_INDEX_1Q2, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744520.PORT_INDEX_1Q3, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744520.PORT_INDEX_1MR, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744520.PORT_INDEX_2CP0, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744520.PORT_INDEX_2CP1, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744520.PORT_INDEX_2Q0, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744520.PORT_INDEX_2Q1, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744520.PORT_INDEX_2Q2, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744520.PORT_INDEX_2Q3, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744520.PORT_INDEX_2MR, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 150, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void clockPinsAreTheFourEdges() {
    final var gate = new Ttl744520();

    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(
        new int[] {
          Ttl744520.PORT_INDEX_1CP0,
          Ttl744520.PORT_INDEX_1CP1,
          Ttl744520.PORT_INDEX_2CP0,
          Ttl744520.PORT_INDEX_2CP1
        },
        gate.clockPinIndex(null));
  }

  @Test
  void risingCp0CountsWhileCp1IsHighAndWraps() {
    final var gate = new Ttl744520();
    final var state = reset(gate);

    for (var count = 1; count <= 15; count++) {
      pulseCp0(gate, state, Ttl744520.PORT_INDEX_1CP0);
      assertCount(state, COUNTER1, count);
    }
    pulseCp0(gate, state, Ttl744520.PORT_INDEX_1CP0);
    assertCount(state, COUNTER1, 0);
    assertCount(state, COUNTER2, 0);
  }

  @Test
  void fallingCp1CountsWhileCp0IsLowAndWraps() {
    final var gate = new Ttl744520();
    final var state = reset(gate);

    for (var count = 1; count <= 15; count++) {
      fall(gate, state, Ttl744520.PORT_INDEX_1CP1);
      assertCount(state, COUNTER1, count);
      rise(gate, state, Ttl744520.PORT_INDEX_1CP1);
      assertCount(state, COUNTER1, count);
    }
    fall(gate, state, Ttl744520.PORT_INDEX_1CP1);
    assertCount(state, COUNTER1, 0);
  }

  @Test
  void disabledAndOppositeEdgesDoNotCount() {
    final var gate = new Ttl744520();
    final var state = reset(gate);

    rise(gate, state, Ttl744520.PORT_INDEX_1CP0);
    fall(gate, state, Ttl744520.PORT_INDEX_1CP0);
    assertCount(state, COUNTER1, 1);

    rise(gate, state, Ttl744520.PORT_INDEX_1CP0);
    fall(gate, state, Ttl744520.PORT_INDEX_1CP1);
    fall(gate, state, Ttl744520.PORT_INDEX_1CP0);
    rise(gate, state, Ttl744520.PORT_INDEX_1CP0);
    fall(gate, state, Ttl744520.PORT_INDEX_1CP0);
    assertCount(state, COUNTER1, 2);

    rise(gate, state, Ttl744520.PORT_INDEX_1CP0);
    rise(gate, state, Ttl744520.PORT_INDEX_1CP1);
    fall(gate, state, Ttl744520.PORT_INDEX_1CP1);
    assertCount(state, COUNTER1, 2);
  }

  @Test
  void bothInputsChangingTogetherCountOnce() {
    final var gate = new Ttl744520();
    final var state = reset(gate);

    state.setPortValue(Ttl744520.PORT_INDEX_1CP0, Value.TRUE);
    state.setPortValue(Ttl744520.PORT_INDEX_1CP1, Value.FALSE);
    gate.propagate(state);
    assertCount(state, COUNTER1, 1);

    state.setPortValue(Ttl744520.PORT_INDEX_1CP0, Value.FALSE);
    state.setPortValue(Ttl744520.PORT_INDEX_1CP1, Value.TRUE);
    gate.propagate(state);
    assertCount(state, COUNTER1, 1);

    state.setPortValue(Ttl744520.PORT_INDEX_1CP0, Value.TRUE);
    state.setPortValue(Ttl744520.PORT_INDEX_1CP1, Value.FALSE);
    gate.propagate(state);
    assertCount(state, COUNTER1, 2);
  }

  @Test
  void masterResetClearsAndOverridesTheClock() {
    final var gate = new Ttl744520();
    final var state = reset(gate);
    clockCp0(gate, state, Ttl744520.PORT_INDEX_1CP0, 4);
    assertCount(state, COUNTER1, 4);

    state.setPortValue(Ttl744520.PORT_INDEX_1MR, Value.TRUE);
    state.setPortValue(Ttl744520.PORT_INDEX_1CP0, Value.TRUE);
    gate.propagate(state);
    assertCount(state, COUNTER1, 0);

    fall(gate, state, Ttl744520.PORT_INDEX_1CP0);
    rise(gate, state, Ttl744520.PORT_INDEX_1CP0);
    fall(gate, state, Ttl744520.PORT_INDEX_1CP0);
    assertCount(state, COUNTER1, 0);

    state.setPortValue(Ttl744520.PORT_INDEX_1MR, Value.FALSE);
    gate.propagate(state);
    assertCount(state, COUNTER1, 0);
    pulseCp0(gate, state, Ttl744520.PORT_INDEX_1CP0);
    assertCount(state, COUNTER1, 1);
  }

  @Test
  void halvesAreIndependent() {
    final var gate = new Ttl744520();
    final var state = reset(gate);

    clockCp0(gate, state, Ttl744520.PORT_INDEX_1CP0, 3);
    fall(gate, state, Ttl744520.PORT_INDEX_2CP1);
    rise(gate, state, Ttl744520.PORT_INDEX_2CP1);
    fall(gate, state, Ttl744520.PORT_INDEX_2CP1);
    assertCount(state, COUNTER1, 3);
    assertCount(state, COUNTER2, 2);

    state.setPortValue(Ttl744520.PORT_INDEX_2MR, Value.TRUE);
    gate.propagate(state);
    assertCount(state, COUNTER1, 3);
    assertCount(state, COUNTER2, 0);
  }

  @Test
  void unknownClockAndResetDoNotChangeTheCount() {
    final var gate = new Ttl744520();
    final var state = reset(gate);
    clockCp0(gate, state, Ttl744520.PORT_INDEX_1CP0, 2);

    state.setPortValue(Ttl744520.PORT_INDEX_1CP0, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl744520.PORT_INDEX_1CP0, Value.TRUE);
    gate.propagate(state);
    assertCount(state, COUNTER1, 2);

    state.setPortValue(Ttl744520.PORT_INDEX_1CP0, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl744520.PORT_INDEX_1CP1, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl744520.PORT_INDEX_1CP1, Value.FALSE);
    gate.propagate(state);
    assertCount(state, COUNTER1, 2);

    state.setPortValue(Ttl744520.PORT_INDEX_1MR, Value.UNKNOWN);
    gate.propagate(state);
    assertCount(state, COUNTER1, 2);

    state.setPortValue(Ttl744520.PORT_INDEX_1CP1, Value.TRUE);
    gate.propagate(state);
    pulseCp0(gate, state, Ttl744520.PORT_INDEX_1CP0);
    assertCount(state, COUNTER1, 3);
  }

  @Test
  void undefinedCountBecomesUnknownOnTheNextClock() {
    final var gate = new Ttl744520();
    final var state = reset(gate);
    final var data = new TtlRegisterData(WIDTH, 2);
    data.setValue(0, Value.createUnknown(WIDTH));
    data.setValue(1, Value.createKnown(WIDTH, 5));
    state.setData(data);

    pulseCp0(gate, state, Ttl744520.PORT_INDEX_1CP0);
    assertUnknown(state, COUNTER1);
    assertCount(state, COUNTER2, 5);

    state.setPortValue(Ttl744520.PORT_INDEX_1MR, Value.TRUE);
    gate.propagate(state);
    assertCount(state, COUNTER1, 0);
    assertCount(state, COUNTER2, 5);
  }

  @Test
  void errorInTheCountBecomesErrorOnTheNextClock() {
    final var gate = new Ttl744520();
    final var state = reset(gate);
    final var data = new TtlRegisterData(WIDTH, 2);
    final var bits = Value.createKnown(WIDTH, 0).getAll();
    bits[0] = Value.ERROR;
    data.setValue(0, Value.create(bits));
    data.setValue(1, Value.createKnown(WIDTH, 5));
    state.setData(data);

    pulseCp0(gate, state, Ttl744520.PORT_INDEX_1CP0);
    assertError(state, COUNTER1);
    assertCount(state, COUNTER2, 5);

    state.setPortValue(Ttl744520.PORT_INDEX_1MR, Value.TRUE);
    gate.propagate(state);
    assertCount(state, COUNTER1, 0);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl744520();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    reset(gate, state);
    clockCp0(gate, state, Ttl744520.PORT_INDEX_1CP0, 4);
    clockCp0(gate, state, Ttl744520.PORT_INDEX_2CP0, 6);
    assertCount(state, COUNTER1, 4);
    assertCount(state, COUNTER2, 6);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertUnknown(state, COUNTER1);
    assertUnknown(state, COUNTER2);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertCount(state, COUNTER1, 4);
    assertCount(state, COUNTER2, 6);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertUnknown(state, COUNTER1);
    assertUnknown(state, COUNTER2);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static TtlTestInstanceState reset(Ttl744520 gate) {
    final var state = new TtlTestInstanceState(gate, false);
    return reset(gate, state);
  }

  private static TtlTestInstanceState reset(Ttl744520 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl744520.PORT_INDEX_1CP0, Value.FALSE);
    state.setPortValue(Ttl744520.PORT_INDEX_1CP1, Value.TRUE);
    state.setPortValue(Ttl744520.PORT_INDEX_1MR, Value.TRUE);
    state.setPortValue(Ttl744520.PORT_INDEX_2CP0, Value.FALSE);
    state.setPortValue(Ttl744520.PORT_INDEX_2CP1, Value.TRUE);
    state.setPortValue(Ttl744520.PORT_INDEX_2MR, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl744520.PORT_INDEX_1MR, Value.FALSE);
    state.setPortValue(Ttl744520.PORT_INDEX_2MR, Value.FALSE);
    gate.propagate(state);
    return state;
  }

  private static void rise(Ttl744520 gate, TtlTestInstanceState state, int port) {
    state.setPortValue(port, Value.TRUE);
    gate.propagate(state);
  }

  private static void fall(Ttl744520 gate, TtlTestInstanceState state, int port) {
    state.setPortValue(port, Value.FALSE);
    gate.propagate(state);
  }

  private static void pulseCp0(Ttl744520 gate, TtlTestInstanceState state, int cp0) {
    rise(gate, state, cp0);
    fall(gate, state, cp0);
  }

  private static void clockCp0(Ttl744520 gate, TtlTestInstanceState state, int cp0, int pulses) {
    for (var i = 0; i < pulses; i++) {
      pulseCp0(gate, state, cp0);
    }
  }

  private static void assertCount(TtlTestInstanceState state, int[] ports, int count) {
    for (var bit = 0; bit < ports.length; bit++) {
      final var expected = ((count >> bit) & 1) == 0 ? Value.FALSE : Value.TRUE;
      assertEquals(expected, state.getPortValue(ports[bit]), "count " + count);
    }
  }

  private static void assertUnknown(TtlTestInstanceState state, int[] ports) {
    for (final var port : ports) {
      assertEquals(Value.UNKNOWN, state.getPortValue(port));
    }
  }

  private static void assertError(TtlTestInstanceState state, int[] ports) {
    for (final var port : ports) {
      assertEquals(Value.ERROR, state.getPortValue(port));
    }
  }
}
