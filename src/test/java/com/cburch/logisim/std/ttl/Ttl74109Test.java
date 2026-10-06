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
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC109 dual positive-edge J-K flip-flop. */
class Ttl74109Test {
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;
  private static final int CP = 0;
  private static final int J = 1;
  private static final int K = 2;
  private static final int SET = 3;
  private static final int CLEAR = 4;
  private static final int Q = 5;
  private static final int NQ = 6;
  private static final int[] FIRST = {
    Ttl74109.PORT_INDEX_1CP,
    Ttl74109.PORT_INDEX_1J,
    Ttl74109.PORT_INDEX_1K,
    Ttl74109.PORT_INDEX_1SD,
    Ttl74109.PORT_INDEX_1RD,
    Ttl74109.PORT_INDEX_1Q,
    Ttl74109.PORT_INDEX_1NQ
  };
  private static final int[] SECOND = {
    Ttl74109.PORT_INDEX_2CP,
    Ttl74109.PORT_INDEX_2J,
    Ttl74109.PORT_INDEX_2K,
    Ttl74109.PORT_INDEX_2SD,
    Ttl74109.PORT_INDEX_2RD,
    Ttl74109.PORT_INDEX_2Q,
    Ttl74109.PORT_INDEX_2NQ
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74109();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74109.PORT_INDEX_1RD, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74109.PORT_INDEX_1J, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74109.PORT_INDEX_1K, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74109.PORT_INDEX_1CP, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74109.PORT_INDEX_1SD, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74109.PORT_INDEX_1Q, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74109.PORT_INDEX_1NQ, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74109.PORT_INDEX_2NQ, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74109.PORT_INDEX_2Q, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74109.PORT_INDEX_2SD, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74109.PORT_INDEX_2CP, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74109.PORT_INDEX_2K, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74109.PORT_INDEX_2J, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74109.PORT_INDEX_2RD, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 150, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void asynchronousSetAndClearOverrideTheClock() {
    final var gate = new Ttl74109();
    final var state = new TtlTestInstanceState(gate, false);
    idle(gate, state);

    for (final var half : new int[][] {FIRST, SECOND}) {
      final var other = half == FIRST ? SECOND : FIRST;
      level(state, half[SET], Value.FALSE);
      level(state, half[CLEAR], Value.TRUE);
      level(state, half[J], Value.FALSE);
      level(state, half[K], Value.FALSE);
      gate.propagate(state);
      rise(gate, state, half[CP]);
      assertHalf(state, half, Value.TRUE, Value.FALSE);
      assertHalf(state, other, Value.FALSE, Value.TRUE);

      level(state, half[SET], Value.TRUE);
      level(state, half[CLEAR], Value.FALSE);
      level(state, half[J], Value.TRUE);
      level(state, half[K], Value.TRUE);
      level(state, half[CP], Value.FALSE);
      gate.propagate(state);
      rise(gate, state, half[CP]);
      assertHalf(state, half, Value.FALSE, Value.TRUE);
      assertHalf(state, other, Value.FALSE, Value.TRUE);
      level(state, half[CLEAR], Value.TRUE);
      level(state, half[J], Value.FALSE);
      level(state, half[K], Value.TRUE);
      gate.propagate(state);
    }
  }

  @Test
  void bothAssertedOutputsAreHighUntilOneInputRemains() {
    final var gate = new Ttl74109();
    final var state = new TtlTestInstanceState(gate, false);
    idle(gate, state);
    clearBoth(gate, state);

    level(state, FIRST[SET], Value.FALSE);
    level(state, FIRST[CLEAR], Value.FALSE);
    gate.propagate(state);
    assertHalf(state, FIRST, Value.TRUE, Value.TRUE);
    assertHalf(state, SECOND, Value.FALSE, Value.TRUE);

    level(state, FIRST[SET], Value.TRUE);
    gate.propagate(state);
    assertHalf(state, FIRST, Value.FALSE, Value.TRUE);

    level(state, FIRST[SET], Value.FALSE);
    level(state, FIRST[CLEAR], Value.FALSE);
    gate.propagate(state);
    level(state, FIRST[CLEAR], Value.TRUE);
    gate.propagate(state);
    assertHalf(state, FIRST, Value.TRUE, Value.FALSE);
  }

  @Test
  void releasingBothAsyncInputsTogetherMakesTheStateUnknown() {
    final var gate = new Ttl74109();
    final var state = new TtlTestInstanceState(gate, false);
    idle(gate, state);

    level(state, FIRST[SET], Value.FALSE);
    level(state, FIRST[CLEAR], Value.FALSE);
    gate.propagate(state);
    level(state, FIRST[SET], Value.TRUE);
    level(state, FIRST[CLEAR], Value.TRUE);
    gate.propagate(state);
    assertHalf(state, FIRST, Value.UNKNOWN, Value.UNKNOWN);
    assertHalf(state, SECOND, Value.FALSE, Value.TRUE);
  }

  @Test
  void risingEdgeAppliesTheJkFunctionTable() {
    final var gate = new Ttl74109();
    final var state = new TtlTestInstanceState(gate, false);
    idle(gate, state);

    for (final var half : new int[][] {FIRST, SECOND}) {
      final var other = half == FIRST ? SECOND : FIRST;
      clearBoth(gate, state);
      capture(gate, state, half, Value.FALSE, Value.TRUE);
      assertHalf(state, half, Value.FALSE, Value.TRUE);
      capture(gate, state, half, Value.FALSE, Value.FALSE);
      assertHalf(state, half, Value.FALSE, Value.TRUE);
      capture(gate, state, half, Value.TRUE, Value.TRUE);
      assertHalf(state, half, Value.TRUE, Value.FALSE);
      capture(gate, state, half, Value.FALSE, Value.TRUE);
      assertHalf(state, half, Value.TRUE, Value.FALSE);
      capture(gate, state, half, Value.TRUE, Value.FALSE);
      assertHalf(state, half, Value.FALSE, Value.TRUE);
      capture(gate, state, half, Value.TRUE, Value.FALSE);
      assertHalf(state, half, Value.TRUE, Value.FALSE);
      assertHalf(state, other, Value.FALSE, Value.TRUE);
      level(state, half[J], Value.FALSE);
      level(state, half[K], Value.TRUE);
    }
  }

  @Test
  void tiedJAndKInputsBehaveAsADflipFlop() {
    final var gate = new Ttl74109();
    final var state = new TtlTestInstanceState(gate, false);
    idle(gate, state);
    clearHalf(gate, state, FIRST);

    capture(gate, state, FIRST, Value.TRUE, Value.TRUE);
    assertHalf(state, FIRST, Value.TRUE, Value.FALSE);
    capture(gate, state, FIRST, Value.FALSE, Value.FALSE);
    assertHalf(state, FIRST, Value.FALSE, Value.TRUE);
    capture(gate, state, FIRST, Value.TRUE, Value.TRUE);
    assertHalf(state, FIRST, Value.TRUE, Value.FALSE);
    assertHalf(state, SECOND, Value.FALSE, Value.TRUE);
  }

  @Test
  void fallingEdgeAndLevelChangesDoNotCapture() {
    final var gate = new Ttl74109();
    final var state = new TtlTestInstanceState(gate, false);
    idle(gate, state);
    clearHalf(gate, state, FIRST);

    level(state, FIRST[CP], Value.TRUE);
    gate.propagate(state);
    level(state, FIRST[J], Value.TRUE);
    level(state, FIRST[K], Value.TRUE);
    gate.propagate(state);
    assertHalf(state, FIRST, Value.FALSE, Value.TRUE);

    level(state, FIRST[CP], Value.FALSE);
    gate.propagate(state);
    assertHalf(state, FIRST, Value.FALSE, Value.TRUE);

    level(state, FIRST[CP], Value.TRUE);
    gate.propagate(state);
    assertHalf(state, FIRST, Value.TRUE, Value.FALSE);

    level(state, FIRST[J], Value.FALSE);
    level(state, FIRST[K], Value.FALSE);
    gate.propagate(state);
    assertHalf(state, FIRST, Value.TRUE, Value.FALSE);

    level(state, FIRST[CP], Value.FALSE);
    gate.propagate(state);
    assertHalf(state, FIRST, Value.TRUE, Value.FALSE);
  }

  @Test
  void unknownClockDoesNotCaptureAndUnknownDataFollowsTheForcedCases() {
    final var gate = new Ttl74109();
    final var state = new TtlTestInstanceState(gate, false);
    idle(gate, state);
    clearHalf(gate, state, FIRST);

    level(state, FIRST[CP], Value.UNKNOWN);
    gate.propagate(state);
    level(state, FIRST[CP], Value.FALSE);
    gate.propagate(state);
    assertHalf(state, FIRST, Value.FALSE, Value.TRUE);

    level(state, FIRST[CP], Value.TRUE);
    capture(gate, state, FIRST, Value.UNKNOWN, Value.FALSE);
    assertHalf(state, FIRST, Value.UNKNOWN, Value.UNKNOWN);

    clearHalf(gate, state, FIRST);
    capture(gate, state, FIRST, Value.TRUE, Value.TRUE);
    capture(gate, state, FIRST, Value.UNKNOWN, Value.TRUE);
    assertHalf(state, FIRST, Value.TRUE, Value.FALSE);

    capture(gate, state, FIRST, Value.ERROR, Value.FALSE);
    assertHalf(state, FIRST, Value.ERROR, Value.ERROR);
    capture(gate, state, FIRST, Value.TRUE, Value.TRUE);
    assertHalf(state, FIRST, Value.TRUE, Value.FALSE);
    capture(gate, state, FIRST, Value.FALSE, Value.FALSE);
    assertHalf(state, FIRST, Value.FALSE, Value.TRUE);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74109();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    idle(gate, state);
    clearBoth(gate, state);
    level(state, FIRST[SET], Value.FALSE);
    gate.propagate(state);
    level(state, FIRST[SET], Value.TRUE);
    gate.propagate(state);
    assertHalf(state, FIRST, Value.TRUE, Value.FALSE);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertHalf(state, FIRST, Value.TRUE, Value.FALSE);
    assertHalf(state, SECOND, Value.FALSE, Value.TRUE);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  @Test
  void clockPinsAreTheTwoRisingEdgeClocks() {
    final var gate = new Ttl74109();
    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(
        new int[] {Ttl74109.PORT_INDEX_1CP, Ttl74109.PORT_INDEX_2CP}, gate.clockPinIndex(null));
  }

  private static void idle(Ttl74109 gate, TtlTestInstanceState state) {
    for (final var half : new int[][] {FIRST, SECOND}) {
      level(state, half[CP], Value.FALSE);
      level(state, half[J], Value.FALSE);
      level(state, half[K], Value.TRUE);
      level(state, half[SET], Value.TRUE);
      level(state, half[CLEAR], Value.TRUE);
    }
    gate.propagate(state);
    clearBoth(gate, state);
  }

  private static void clearBoth(Ttl74109 gate, TtlTestInstanceState state) {
    clearHalf(gate, state, FIRST);
    clearHalf(gate, state, SECOND);
  }

  private static void clearHalf(Ttl74109 gate, TtlTestInstanceState state, int[] half) {
    level(state, half[SET], Value.TRUE);
    level(state, half[CLEAR], Value.FALSE);
    gate.propagate(state);
    level(state, half[CLEAR], Value.TRUE);
    gate.propagate(state);
    assertHalf(state, half, Value.FALSE, Value.TRUE);
  }

  private static void capture(
      Ttl74109 gate, TtlTestInstanceState state, int[] half, Value j, Value k) {
    level(state, half[CP], Value.FALSE);
    level(state, half[J], j);
    level(state, half[K], k);
    gate.propagate(state);
    rise(gate, state, half[CP]);
  }

  private static void rise(Ttl74109 gate, TtlTestInstanceState state, int clock) {
    level(state, clock, Value.TRUE);
    gate.propagate(state);
  }

  private static void level(TtlTestInstanceState state, int port, Value value) {
    state.setPortValue(port, value);
  }

  private static void assertHalf(TtlTestInstanceState state, int[] half, Value q, Value nq) {
    assertEquals(q, state.getPortValue(half[Q]));
    assertEquals(nq, state.getPortValue(half[NQ]));
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    assertHalf(state, FIRST, Value.UNKNOWN, Value.UNKNOWN);
    assertHalf(state, SECOND, Value.UNKNOWN, Value.UNKNOWN);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }
}
