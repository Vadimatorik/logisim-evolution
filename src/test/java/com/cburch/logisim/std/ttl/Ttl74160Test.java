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

/** Functional tests for the 74HC160 synchronous BCD decade counter. */
class Ttl74160Test {
  private static final int[] NEXT_COUNT = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 11, 6, 13, 4, 15, 2};
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74160();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74160.PORT_INDEX_MR, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74160.PORT_INDEX_CP, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74160.PORT_INDEX_D0, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74160.PORT_INDEX_D1, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74160.PORT_INDEX_D2, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74160.PORT_INDEX_D3, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74160.PORT_INDEX_CEP, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74160.PORT_INDEX_PE, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74160.PORT_INDEX_CET, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74160.PORT_INDEX_Q3, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74160.PORT_INDEX_Q2, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74160.PORT_INDEX_Q1, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74160.PORT_INDEX_Q0, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74160.PORT_INDEX_TC, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void clockIsTheRisingEdgeOfCp() {
    final var gate = new Ttl74160();
    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(new int[] {Ttl74160.PORT_INDEX_CP}, gate.clockPinIndex(null));
  }

  @Test
  void masterResetClearsWithoutAClockAndWinsOverLoad() {
    final var gate = new Ttl74160();
    final var state = new TtlTestInstanceState(gate, false);
    load(gate, state, 9);

    state.setPortValue(Ttl74160.PORT_INDEX_CP, Value.TRUE);
    state.setPortValue(Ttl74160.PORT_INDEX_PE, Value.FALSE);
    setData(state, 7);
    state.setPortValue(Ttl74160.PORT_INDEX_MR, Value.FALSE);
    gate.propagate(state);

    assertWord(state, 0, Value.FALSE);
  }

  @Test
  void parallelLoadStoresEveryCodeOnTheRisingEdge() {
    final var gate = new Ttl74160();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    for (var code = 0; code < 16; code++) {
      load(gate, state, code);
      assertWord(state, code, code == 9 ? Value.TRUE : Value.FALSE);
    }
  }

  @Test
  void loadOverridesCountEnables() {
    final var gate = new Ttl74160();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    enableCount(state);
    state.setPortValue(Ttl74160.PORT_INDEX_PE, Value.FALSE);
    setData(state, 7);
    pulse(gate, state);

    assertWord(state, 7, Value.FALSE);
  }

  @Test
  void eachCodeAdvancesOnceUnderTheDecadeEquations() {
    final var gate = new Ttl74160();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    for (var code = 0; code < NEXT_COUNT.length; code++) {
      load(gate, state, code);
      enableCount(state);
      pulse(gate, state);
      final var next = NEXT_COUNT[code];
      assertWord(state, next, next == 9 ? Value.TRUE : Value.FALSE);
    }
  }

  @Test
  void terminalCountFollowsCetWithoutAClock() {
    final var gate = new Ttl74160();
    final var state = new TtlTestInstanceState(gate, false);
    load(gate, state, 9);

    state.setPortValue(Ttl74160.PORT_INDEX_CET, Value.FALSE);
    gate.propagate(state);
    assertWord(state, 9, Value.FALSE);

    state.setPortValue(Ttl74160.PORT_INDEX_CET, Value.TRUE);
    gate.propagate(state);
    assertWord(state, 9, Value.TRUE);

    state.setPortValue(Ttl74160.PORT_INDEX_CET, Value.UNKNOWN);
    gate.propagate(state);
    assertWord(state, 9, Value.UNKNOWN);

    state.setPortValue(Ttl74160.PORT_INDEX_CET, Value.ERROR);
    gate.propagate(state);
    assertWord(state, 9, Value.ERROR);

    load(gate, state, 8);
    state.setPortValue(Ttl74160.PORT_INDEX_CET, Value.UNKNOWN);
    gate.propagate(state);
    assertWord(state, 8, Value.FALSE);

    state.setPortValue(Ttl74160.PORT_INDEX_CET, Value.ERROR);
    gate.propagate(state);
    assertWord(state, 8, Value.FALSE);
  }

  @Test
  void eitherLowCountEnableHoldsAndCetLowClearsTerminalCount() {
    final var gate = new Ttl74160();
    final var state = new TtlTestInstanceState(gate, false);
    load(gate, state, 9);
    enableCount(state);
    state.setPortValue(Ttl74160.PORT_INDEX_CEP, Value.FALSE);
    setData(state, 1);
    pulse(gate, state);
    assertWord(state, 9, Value.TRUE);

    state.setPortValue(Ttl74160.PORT_INDEX_CEP, Value.TRUE);
    state.setPortValue(Ttl74160.PORT_INDEX_CET, Value.FALSE);
    pulse(gate, state);
    assertWord(state, 9, Value.FALSE);
  }

  @Test
  void fallingEdgeDoesNotCount() {
    final var gate = new Ttl74160();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    enableCount(state);
    state.setPortValue(Ttl74160.PORT_INDEX_CP, Value.TRUE);
    gate.propagate(state);
    assertWord(state, 1, Value.FALSE);

    state.setPortValue(Ttl74160.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    assertWord(state, 1, Value.FALSE);
  }

  @Test
  void undefinedMasterResetDoesNotClear() {
    final var gate = new Ttl74160();
    final var state = new TtlTestInstanceState(gate, false);
    load(gate, state, 5);

    state.setPortValue(Ttl74160.PORT_INDEX_MR, Value.UNKNOWN);
    gate.propagate(state);
    assertWord(state, 5, Value.FALSE);

    enableCount(state);
    state.setPortValue(Ttl74160.PORT_INDEX_MR, Value.UNKNOWN);
    pulse(gate, state);
    assertWord(state, 6, Value.FALSE);

    load(gate, state, 4);
    state.setPortValue(Ttl74160.PORT_INDEX_MR, Value.ERROR);
    gate.propagate(state);
    assertWord(state, 4, Value.FALSE);
  }

  @Test
  void unknownDataBitsAreLoadedAndBlockTheFollowingCount() {
    final var gate = new Ttl74160();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    state.setPortValue(Ttl74160.PORT_INDEX_PE, Value.FALSE);
    state.setPortValue(Ttl74160.PORT_INDEX_D0, Value.UNKNOWN);
    state.setPortValue(Ttl74160.PORT_INDEX_D1, Value.FALSE);
    state.setPortValue(Ttl74160.PORT_INDEX_D2, Value.FALSE);
    state.setPortValue(Ttl74160.PORT_INDEX_D3, Value.TRUE);
    state.setPortValue(Ttl74160.PORT_INDEX_CET, Value.TRUE);
    pulse(gate, state);

    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74160.PORT_INDEX_Q0));
    assertEquals(Value.FALSE, state.getPortValue(Ttl74160.PORT_INDEX_Q1));
    assertEquals(Value.FALSE, state.getPortValue(Ttl74160.PORT_INDEX_Q2));
    assertEquals(Value.TRUE, state.getPortValue(Ttl74160.PORT_INDEX_Q3));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74160.PORT_INDEX_TC));

    enableCount(state);
    pulse(gate, state);
    assertUnknownWord(state);
  }

  @Test
  void countingAnErrorWordBecomesError() {
    final var gate = new Ttl74160();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    state.setPortValue(Ttl74160.PORT_INDEX_PE, Value.FALSE);
    state.setPortValue(Ttl74160.PORT_INDEX_D0, Value.ERROR);
    state.setPortValue(Ttl74160.PORT_INDEX_D1, Value.FALSE);
    state.setPortValue(Ttl74160.PORT_INDEX_D2, Value.FALSE);
    state.setPortValue(Ttl74160.PORT_INDEX_D3, Value.FALSE);
    state.setPortValue(Ttl74160.PORT_INDEX_CET, Value.FALSE);
    pulse(gate, state);
    assertEquals(Value.ERROR, state.getPortValue(Ttl74160.PORT_INDEX_Q0));
    assertEquals(Value.FALSE, state.getPortValue(Ttl74160.PORT_INDEX_TC));

    enableCount(state);
    pulse(gate, state);
    assertErrorWord(state);
  }

  @Test
  void ambiguousEnableBecomesUnknownUnlessCountIsBlocked() {
    final var gate = new Ttl74160();
    final var state = new TtlTestInstanceState(gate, false);
    load(gate, state, 3);
    enableCount(state);
    state.setPortValue(Ttl74160.PORT_INDEX_CEP, Value.UNKNOWN);
    state.setPortValue(Ttl74160.PORT_INDEX_CET, Value.FALSE);
    pulse(gate, state);
    assertWord(state, 3, Value.FALSE);

    state.setPortValue(Ttl74160.PORT_INDEX_CET, Value.TRUE);
    pulse(gate, state);
    assertUnknownWord(state);
  }

  @Test
  void invalidPowerPinsForceOutputsUnknown() {
    final var gate = new Ttl74160();
    final var state = new TtlTestInstanceState(gate, true);
    drivePower(state, Value.FALSE, Value.TRUE);
    reset(gate, state);
    load(gate, state, 6);

    drivePower(state, Value.FALSE, Value.UNKNOWN);
    gate.propagate(state);
    assertUnknownWord(state);

    drivePower(state, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertWord(state, 6, Value.FALSE);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void drivePower(TtlTestInstanceState state, Value ground, Value vcc) {
    state.setPortValue(GND_PORT, ground);
    state.setPortValue(VCC_PORT, vcc);
  }

  private static void reset(Ttl74160 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl74160.PORT_INDEX_CP, Value.FALSE);
    state.setPortValue(Ttl74160.PORT_INDEX_MR, Value.FALSE);
    state.setPortValue(Ttl74160.PORT_INDEX_CET, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74160.PORT_INDEX_MR, Value.TRUE);
    gate.propagate(state);
  }

  private static void enableCount(TtlTestInstanceState state) {
    state.setPortValue(Ttl74160.PORT_INDEX_MR, Value.TRUE);
    state.setPortValue(Ttl74160.PORT_INDEX_PE, Value.TRUE);
    state.setPortValue(Ttl74160.PORT_INDEX_CEP, Value.TRUE);
    state.setPortValue(Ttl74160.PORT_INDEX_CET, Value.TRUE);
  }

  private static void setData(TtlTestInstanceState state, int code) {
    state.setPortValue(Ttl74160.PORT_INDEX_D0, bit(code, 0));
    state.setPortValue(Ttl74160.PORT_INDEX_D1, bit(code, 1));
    state.setPortValue(Ttl74160.PORT_INDEX_D2, bit(code, 2));
    state.setPortValue(Ttl74160.PORT_INDEX_D3, bit(code, 3));
  }

  private static void load(Ttl74160 gate, TtlTestInstanceState state, int code) {
    state.setPortValue(Ttl74160.PORT_INDEX_MR, Value.TRUE);
    state.setPortValue(Ttl74160.PORT_INDEX_PE, Value.FALSE);
    state.setPortValue(Ttl74160.PORT_INDEX_CET, Value.TRUE);
    setData(state, code);
    pulse(gate, state);
  }

  private static void pulse(Ttl74160 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl74160.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74160.PORT_INDEX_CP, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74160.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
  }

  private static Value bit(int code, int index) {
    return ((code >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
  }

  private static void assertWord(TtlTestInstanceState state, int code, Value terminalCount) {
    assertEquals(bit(code, 0), state.getPortValue(Ttl74160.PORT_INDEX_Q0), "Q0 of " + code);
    assertEquals(bit(code, 1), state.getPortValue(Ttl74160.PORT_INDEX_Q1), "Q1 of " + code);
    assertEquals(bit(code, 2), state.getPortValue(Ttl74160.PORT_INDEX_Q2), "Q2 of " + code);
    assertEquals(bit(code, 3), state.getPortValue(Ttl74160.PORT_INDEX_Q3), "Q3 of " + code);
    assertEquals(terminalCount, state.getPortValue(Ttl74160.PORT_INDEX_TC), "TC of " + code);
  }

  private static void assertUnknownWord(TtlTestInstanceState state) {
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74160.PORT_INDEX_Q0));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74160.PORT_INDEX_Q1));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74160.PORT_INDEX_Q2));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74160.PORT_INDEX_Q3));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74160.PORT_INDEX_TC));
  }

  private static void assertErrorWord(TtlTestInstanceState state) {
    assertEquals(Value.ERROR, state.getPortValue(Ttl74160.PORT_INDEX_Q0));
    assertEquals(Value.ERROR, state.getPortValue(Ttl74160.PORT_INDEX_Q1));
    assertEquals(Value.ERROR, state.getPortValue(Ttl74160.PORT_INDEX_Q2));
    assertEquals(Value.ERROR, state.getPortValue(Ttl74160.PORT_INDEX_Q3));
    assertEquals(Value.ERROR, state.getPortValue(Ttl74160.PORT_INDEX_TC));
  }
}
