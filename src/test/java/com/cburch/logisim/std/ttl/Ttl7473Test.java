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

/** Functional tests for the 74HC73 dual negative-edge J-K flip-flop. */
class Ttl7473Test {
  private static final Half FF1 =
      new Half(Ttl7473.CP1, Ttl7473.R1, Ttl7473.J1, Ttl7473.K1, Ttl7473.Q1, Ttl7473.NQ1);
  private static final Half FF2 =
      new Half(Ttl7473.CP2, Ttl7473.R2, Ttl7473.J2, Ttl7473.K2, Ttl7473.Q2, Ttl7473.NQ2);
  private static final int GND_PORT = 12;
  private static final int VCC_PORT = 13;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl7473();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl7473.CP1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7473.R1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7473.K1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7473.CP2, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7473.R2, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7473.J2, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7473.NQ2, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7473.Q2, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7473.K2, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7473.Q1, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7473.NQ1, 30, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7473.J1, 10, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 70, -30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 70, 30, EndData.INPUT_ONLY);
  }

  @Test
  void bothClocksAreGated() {
    final var gate = new Ttl7473();
    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(new int[] {Ttl7473.CP1, Ttl7473.CP2}, gate.clockPinIndex(null));
  }

  @Test
  void eachHalfFollowsTheJkFunctionTable() {
    final var gate = new Ttl7473();
    for (var half : new Half[] {FF1, FF2}) {
      final var other = half == FF1 ? FF2 : FF1;
      final var state = new TtlTestInstanceState(gate, false);
      reset(gate, state);

      drive(state, half, Value.TRUE, Value.FALSE, Value.TRUE);
      fall(gate, state, half);
      assertHalf(state, half, Value.TRUE);
      assertHalf(state, other, Value.FALSE);

      drive(state, half, Value.FALSE, Value.FALSE, Value.TRUE);
      fall(gate, state, half);
      assertHalf(state, half, Value.TRUE);

      drive(state, half, Value.TRUE, Value.TRUE, Value.TRUE);
      fall(gate, state, half);
      assertHalf(state, half, Value.FALSE);
      fall(gate, state, half);
      assertHalf(state, half, Value.TRUE);

      drive(state, half, Value.FALSE, Value.TRUE, Value.TRUE);
      fall(gate, state, half);
      assertHalf(state, half, Value.FALSE);
      assertHalf(state, other, Value.FALSE);
    }
  }

  @Test
  void risingEdgeAndLevelChangesDoNotCapture() {
    final var gate = new Ttl7473();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    drive(state, FF1, Value.TRUE, Value.FALSE, Value.TRUE);
    state.setPortValue(FF1.clock, Value.TRUE);
    gate.propagate(state);
    assertHalf(state, FF1, Value.FALSE);

    drive(state, FF1, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertHalf(state, FF1, Value.FALSE);

    state.setPortValue(FF1.clock, Value.FALSE);
    gate.propagate(state);
    assertHalf(state, FF1, Value.FALSE);
  }

  @Test
  void fallingEdgeCapturesTheLevelsPresentAtTheEdge() {
    final var gate = new Ttl7473();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    drive(state, FF1, Value.FALSE, Value.FALSE, Value.TRUE);
    state.setPortValue(FF1.clock, Value.TRUE);
    gate.propagate(state);
    drive(state, FF1, Value.TRUE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertHalf(state, FF1, Value.FALSE);

    state.setPortValue(FF1.clock, Value.FALSE);
    gate.propagate(state);
    assertHalf(state, FF1, Value.TRUE);
  }

  @Test
  void resetOverridesClockAndDataUntilItIsReleased() {
    final var gate = new Ttl7473();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    drive(state, FF1, Value.TRUE, Value.FALSE, Value.TRUE);
    fall(gate, state, FF1);
    assertHalf(state, FF1, Value.TRUE);

    state.setPortValue(FF1.clock, Value.TRUE);
    drive(state, FF1, Value.TRUE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertHalf(state, FF1, Value.FALSE);

    state.setPortValue(FF1.clock, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(FF1.reset, Value.TRUE);
    gate.propagate(state);
    assertHalf(state, FF1, Value.FALSE);

    fall(gate, state, FF1);
    assertHalf(state, FF1, Value.TRUE);
  }

  @Test
  void clockEdgeDuringResetIsNotAppliedAfterRelease() {
    final var gate = new Ttl7473();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    drive(state, FF1, Value.TRUE, Value.FALSE, Value.FALSE);
    state.setPortValue(FF1.clock, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(FF1.clock, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(FF1.reset, Value.TRUE);
    gate.propagate(state);
    assertHalf(state, FF1, Value.FALSE);

    fall(gate, state, FF1);
    assertHalf(state, FF1, Value.TRUE);
  }

  @Test
  void oneResetLeavesTheOtherHalfAlone() {
    final var gate = new Ttl7473();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    drive(state, FF1, Value.TRUE, Value.FALSE, Value.TRUE);
    drive(state, FF2, Value.TRUE, Value.FALSE, Value.TRUE);
    fall(gate, state, FF1);
    fall(gate, state, FF2);
    assertHalf(state, FF1, Value.TRUE);
    assertHalf(state, FF2, Value.TRUE);

    drive(state, FF1, Value.TRUE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertHalf(state, FF1, Value.FALSE);
    assertHalf(state, FF2, Value.TRUE);
  }

  @Test
  void unknownResetMakesTheStoredStateUnknown() {
    final var gate = new Ttl7473();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    drive(state, FF1, Value.TRUE, Value.FALSE, Value.TRUE);
    fall(gate, state, FF1);

    drive(state, FF1, Value.TRUE, Value.FALSE, Value.UNKNOWN);
    gate.propagate(state);
    assertHalf(state, FF1, Value.UNKNOWN);

    drive(state, FF1, Value.TRUE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertHalf(state, FF1, Value.UNKNOWN);

    drive(state, FF1, Value.FALSE, Value.FALSE, Value.ERROR);
    gate.propagate(state);
    assertHalf(state, FF1, Value.UNKNOWN);
  }

  @Test
  void partialJkInputsKeepOnlyAnAgreedNextState() {
    final var gate = new Ttl7473();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    drive(state, FF1, Value.UNKNOWN, Value.FALSE, Value.TRUE);
    fall(gate, state, FF1);
    assertHalf(state, FF1, Value.UNKNOWN);

    reset(gate, state);
    drive(state, FF1, Value.TRUE, Value.UNKNOWN, Value.TRUE);
    fall(gate, state, FF1);
    assertHalf(state, FF1, Value.TRUE);

    reset(gate, state);
    drive(state, FF1, Value.FALSE, Value.UNKNOWN, Value.TRUE);
    fall(gate, state, FF1);
    assertHalf(state, FF1, Value.FALSE);

    drive(state, FF1, Value.TRUE, Value.FALSE, Value.TRUE);
    fall(gate, state, FF1);
    drive(state, FF1, Value.UNKNOWN, Value.FALSE, Value.TRUE);
    fall(gate, state, FF1);
    assertHalf(state, FF1, Value.TRUE);

    drive(state, FF1, Value.TRUE, Value.UNKNOWN, Value.TRUE);
    fall(gate, state, FF1);
    assertHalf(state, FF1, Value.UNKNOWN);

    reset(gate, state);
    drive(state, FF1, Value.TRUE, Value.FALSE, Value.TRUE);
    fall(gate, state, FF1);
    drive(state, FF1, Value.UNKNOWN, Value.TRUE, Value.TRUE);
    fall(gate, state, FF1);
    assertHalf(state, FF1, Value.FALSE);

    reset(gate, state);
    drive(state, FF1, Value.UNKNOWN, Value.UNKNOWN, Value.TRUE);
    fall(gate, state, FF1);
    assertHalf(state, FF1, Value.UNKNOWN);
    assertHalf(state, FF2, Value.FALSE);
  }

  @Test
  void unknownClockDoesNotLookLikeAFallingEdge() {
    final var gate = new Ttl7473();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    drive(state, FF1, Value.TRUE, Value.FALSE, Value.TRUE);
    fall(gate, state, FF1);

    drive(state, FF1, Value.FALSE, Value.TRUE, Value.TRUE);
    state.setPortValue(FF1.clock, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(FF1.clock, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(FF1.clock, Value.FALSE);
    gate.propagate(state);
    assertHalf(state, FF1, Value.TRUE);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl7473();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    reset(gate, state);
    drive(state, FF1, Value.TRUE, Value.FALSE, Value.TRUE);
    fall(gate, state, FF1);
    assertHalf(state, FF1, Value.TRUE);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7473.Q1));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7473.NQ1));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7473.Q2));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7473.NQ2));

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertHalf(state, FF1, Value.TRUE);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7473.Q1));

    state.setPortValue(GND_PORT, Value.FALSE);
    gate.propagate(state);
    assertHalf(state, FF1, Value.TRUE);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void reset(Ttl7473 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl7473.CP1, Value.FALSE);
    state.setPortValue(Ttl7473.CP2, Value.FALSE);
    state.setPortValue(Ttl7473.J1, Value.FALSE);
    state.setPortValue(Ttl7473.K1, Value.FALSE);
    state.setPortValue(Ttl7473.J2, Value.FALSE);
    state.setPortValue(Ttl7473.K2, Value.FALSE);
    state.setPortValue(Ttl7473.R1, Value.FALSE);
    state.setPortValue(Ttl7473.R2, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl7473.R1, Value.TRUE);
    state.setPortValue(Ttl7473.R2, Value.TRUE);
    gate.propagate(state);
  }

  private static void drive(TtlTestInstanceState state, Half half, Value j, Value k, Value reset) {
    state.setPortValue(half.j, j);
    state.setPortValue(half.k, k);
    state.setPortValue(half.reset, reset);
  }

  private static void fall(Ttl7473 gate, TtlTestInstanceState state, Half half) {
    state.setPortValue(half.clock, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(half.clock, Value.FALSE);
    gate.propagate(state);
  }

  private static void assertHalf(TtlTestInstanceState state, Half half, Value q) {
    assertEquals(q, state.getPortValue(half.q));
    assertEquals(q.not(), state.getPortValue(half.nq));
  }

  private static final class Half {
    private final int clock;
    private final int reset;
    private final int j;
    private final int k;
    private final int q;
    private final int nq;

    private Half(int clock, int reset, int j, int k, int q, int nq) {
      this.clock = clock;
      this.reset = reset;
      this.j = j;
      this.k = k;
      this.q = q;
      this.nq = nq;
    }
  }
}
