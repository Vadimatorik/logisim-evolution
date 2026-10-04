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

/** Functional tests for the 74LVC1G74 single D flip-flop. */
class Ttl741G74Test {

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl741G74();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(6, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl741G74.PORT_CP, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl741G74.PORT_D, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl741G74.PORT_NQ, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl741G74.PORT_Q, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl741G74.PORT_RD, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl741G74.PORT_SD, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(8, shownPower.getPorts().size());
    assertPort(shownPower, Ttl741G74.PORT_GND, 70, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, Ttl741G74.PORT_VCC, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void clockIsTheFirstLogicalPort() {
    final var gate = new Ttl741G74();
    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(new int[] {Ttl741G74.PORT_CP}, gate.clockPinIndex(null));
  }

  @Test
  void clearIsAsynchronousAndIgnoresClockAndData() {
    final var gate = new Ttl741G74();
    final var state = idle(gate);
    rise(gate, state, Value.TRUE);
    assertOutputs(state, Value.TRUE, Value.FALSE);

    set(state, Ttl741G74.PORT_D, Value.TRUE);
    set(state, Ttl741G74.PORT_CP, Value.TRUE);
    set(state, Ttl741G74.PORT_RD, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, Value.FALSE, Value.TRUE);
  }

  @Test
  void presetIsAsynchronousAndIgnoresClockAndData() {
    final var gate = new Ttl741G74();
    final var state = idle(gate);
    clear(gate, state);

    set(state, Ttl741G74.PORT_D, Value.FALSE);
    set(state, Ttl741G74.PORT_CP, Value.TRUE);
    set(state, Ttl741G74.PORT_RD, Value.TRUE);
    set(state, Ttl741G74.PORT_SD, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, Value.TRUE, Value.FALSE);
  }

  @Test
  void bothAssertedDriveBothOutputsHigh() {
    final var gate = new Ttl741G74();
    final var state = idle(gate);
    clear(gate, state);

    set(state, Ttl741G74.PORT_SD, Value.FALSE);
    set(state, Ttl741G74.PORT_RD, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, Value.TRUE, Value.TRUE);
  }

  @Test
  void releasingOneControlAppliesTheOtherWithoutAClock() {
    final var gate = new Ttl741G74();
    final var state = idle(gate);
    assertBothHigh(gate, state);

    set(state, Ttl741G74.PORT_SD, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, Value.FALSE, Value.TRUE);

    assertBothHigh(gate, state);
    set(state, Ttl741G74.PORT_RD, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, Value.TRUE, Value.FALSE);
  }

  @Test
  void bothHighPairIsHeldUntilTheNextRisingEdge() {
    final var gate = new Ttl741G74();
    final var state = idle(gate);
    assertBothHigh(gate, state);

    set(state, Ttl741G74.PORT_SD, Value.TRUE);
    set(state, Ttl741G74.PORT_RD, Value.TRUE);
    set(state, Ttl741G74.PORT_D, Value.FALSE);
    set(state, Ttl741G74.PORT_CP, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, Value.TRUE, Value.TRUE);

    fall(gate, state);
    assertOutputs(state, Value.TRUE, Value.TRUE);

    rise(gate, state, Value.FALSE);
    assertOutputs(state, Value.FALSE, Value.TRUE);
  }

  @Test
  void risingEdgeStoresDataAndLevelOrFallingEdgeHolds() {
    final var gate = new Ttl741G74();
    final var state = idle(gate);
    clear(gate, state);

    rise(gate, state, Value.TRUE);
    assertOutputs(state, Value.TRUE, Value.FALSE);

    set(state, Ttl741G74.PORT_D, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, Value.TRUE, Value.FALSE);

    fall(gate, state);
    assertOutputs(state, Value.TRUE, Value.FALSE);

    set(state, Ttl741G74.PORT_D, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, Value.TRUE, Value.FALSE);

    rise(gate, state, Value.FALSE);
    assertOutputs(state, Value.FALSE, Value.TRUE);
  }

  @Test
  void asynchronousClearOverridesARisingEdge() {
    final var gate = new Ttl741G74();
    final var state = idle(gate);
    preset(gate, state);

    set(state, Ttl741G74.PORT_D, Value.TRUE);
    set(state, Ttl741G74.PORT_RD, Value.FALSE);
    set(state, Ttl741G74.PORT_CP, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, Value.FALSE, Value.TRUE);
  }

  @Test
  void unknownDataOnARisingEdgeMakesBothOutputsUnknown() {
    final var gate = new Ttl741G74();
    final var state = idle(gate);
    clear(gate, state);

    rise(gate, state, Value.UNKNOWN);
    assertOutputs(state, Value.UNKNOWN, Value.UNKNOWN);

    clear(gate, state);
    assertOutputs(state, Value.FALSE, Value.TRUE);
  }

  @Test
  void unknownResetLeavesAClearStateAndDisturbsASetState() {
    final var gate = new Ttl741G74();
    final var state = idle(gate);
    clear(gate, state);
    set(state, Ttl741G74.PORT_RD, Value.UNKNOWN);
    gate.propagate(state);
    assertOutputs(state, Value.FALSE, Value.TRUE);

    preset(gate, state);
    set(state, Ttl741G74.PORT_RD, Value.UNKNOWN);
    gate.propagate(state);
    assertOutputs(state, Value.UNKNOWN, Value.UNKNOWN);
  }

  @Test
  void errorOnResetMakesADisagreementAnError() {
    final var gate = new Ttl741G74();
    final var state = idle(gate);
    clear(gate, state);
    set(state, Ttl741G74.PORT_RD, Value.ERROR);
    gate.propagate(state);
    assertOutputs(state, Value.FALSE, Value.TRUE);

    preset(gate, state);
    set(state, Ttl741G74.PORT_RD, Value.ERROR);
    gate.propagate(state);
    assertOutputs(state, Value.ERROR, Value.ERROR);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl741G74();
    final var state = new TtlTestInstanceState(gate, true);
    set(state, Ttl741G74.PORT_GND, Value.FALSE);
    set(state, Ttl741G74.PORT_VCC, Value.TRUE);
    set(state, Ttl741G74.PORT_CP, Value.FALSE);
    set(state, Ttl741G74.PORT_D, Value.TRUE);
    set(state, Ttl741G74.PORT_SD, Value.TRUE);
    set(state, Ttl741G74.PORT_RD, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, Value.FALSE, Value.TRUE);

    set(state, Ttl741G74.PORT_VCC, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, Value.UNKNOWN, Value.UNKNOWN);

    set(state, Ttl741G74.PORT_VCC, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, Value.FALSE, Value.TRUE);

    set(state, Ttl741G74.PORT_GND, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, Value.UNKNOWN, Value.UNKNOWN);
  }

  private static TtlTestInstanceState idle(Ttl741G74 gate) {
    final var state = new TtlTestInstanceState(gate, false);
    set(state, Ttl741G74.PORT_CP, Value.FALSE);
    set(state, Ttl741G74.PORT_D, Value.FALSE);
    set(state, Ttl741G74.PORT_SD, Value.TRUE);
    set(state, Ttl741G74.PORT_RD, Value.TRUE);
    return state;
  }

  private static void clear(Ttl741G74 gate, TtlTestInstanceState state) {
    set(state, Ttl741G74.PORT_SD, Value.TRUE);
    set(state, Ttl741G74.PORT_RD, Value.FALSE);
    set(state, Ttl741G74.PORT_CP, Value.FALSE);
    gate.propagate(state);
  }

  private static void preset(Ttl741G74 gate, TtlTestInstanceState state) {
    set(state, Ttl741G74.PORT_RD, Value.TRUE);
    set(state, Ttl741G74.PORT_SD, Value.FALSE);
    set(state, Ttl741G74.PORT_CP, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl741G74.PORT_SD, Value.TRUE);
    gate.propagate(state);
  }

  private static void assertBothHigh(Ttl741G74 gate, TtlTestInstanceState state) {
    set(state, Ttl741G74.PORT_SD, Value.FALSE);
    set(state, Ttl741G74.PORT_RD, Value.FALSE);
    set(state, Ttl741G74.PORT_CP, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, Value.TRUE, Value.TRUE);
  }

  private static void rise(Ttl741G74 gate, TtlTestInstanceState state, Value data) {
    set(state, Ttl741G74.PORT_SD, Value.TRUE);
    set(state, Ttl741G74.PORT_RD, Value.TRUE);
    set(state, Ttl741G74.PORT_D, data);
    set(state, Ttl741G74.PORT_CP, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl741G74.PORT_CP, Value.TRUE);
    gate.propagate(state);
  }

  private static void fall(Ttl741G74 gate, TtlTestInstanceState state) {
    set(state, Ttl741G74.PORT_CP, Value.FALSE);
    gate.propagate(state);
  }

  private static void assertOutputs(TtlTestInstanceState state, Value q, Value nq) {
    assertEquals(q, state.getPortValue(Ttl741G74.PORT_Q));
    assertEquals(nq, state.getPortValue(Ttl741G74.PORT_NQ));
  }

  private static void assertPort(Instance instance, int port, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void set(TtlTestInstanceState state, int port, Value value) {
    state.setPortValue(port, value);
  }
}
