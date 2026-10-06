/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.fpga.hdlgenerator.HdlText.containsIgnoringCase;
import static com.cburch.logisim.std.ttl.TtlTestInstanceState.createInstance;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.prefs.AppPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC112 dual negative-edge J-K flip-flop. */
class Ttl74112Test {
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
    Ttl74112.PORT_INDEX_1CP,
    Ttl74112.PORT_INDEX_1J,
    Ttl74112.PORT_INDEX_1K,
    Ttl74112.PORT_INDEX_1SD,
    Ttl74112.PORT_INDEX_1RD,
    Ttl74112.PORT_INDEX_1Q,
    Ttl74112.PORT_INDEX_1NQ
  };
  private static final int[] SECOND = {
    Ttl74112.PORT_INDEX_2CP,
    Ttl74112.PORT_INDEX_2J,
    Ttl74112.PORT_INDEX_2K,
    Ttl74112.PORT_INDEX_2SD,
    Ttl74112.PORT_INDEX_2RD,
    Ttl74112.PORT_INDEX_2Q,
    Ttl74112.PORT_INDEX_2NQ
  };

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74112();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74112.PORT_INDEX_1CP, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74112.PORT_INDEX_1K, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74112.PORT_INDEX_1J, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74112.PORT_INDEX_1SD, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74112.PORT_INDEX_1Q, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74112.PORT_INDEX_1NQ, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74112.PORT_INDEX_2NQ, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74112.PORT_INDEX_2Q, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74112.PORT_INDEX_2SD, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74112.PORT_INDEX_2J, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74112.PORT_INDEX_2K, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74112.PORT_INDEX_2CP, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74112.PORT_INDEX_2RD, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74112.PORT_INDEX_1RD, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 150, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void asynchronousSetAndClearOverrideTheClock() {
    final var gate = new Ttl74112();
    final var state = new TtlTestInstanceState(gate, false);
    idle(gate, state);

    for (final var half : new int[][] {FIRST, SECOND}) {
      final var other = half == FIRST ? SECOND : FIRST;
      level(state, half[SET], Value.FALSE);
      level(state, half[CLEAR], Value.TRUE);
      level(state, half[J], Value.FALSE);
      level(state, half[K], Value.TRUE);
      gate.propagate(state);
      fall(gate, state, half[CP]);
      assertHalf(state, half, Value.TRUE, Value.FALSE);
      assertHalf(state, other, Value.FALSE, Value.TRUE);

      level(state, half[SET], Value.TRUE);
      level(state, half[CLEAR], Value.FALSE);
      level(state, half[J], Value.TRUE);
      level(state, half[K], Value.FALSE);
      level(state, half[CP], Value.TRUE);
      gate.propagate(state);
      fall(gate, state, half[CP]);
      assertHalf(state, half, Value.FALSE, Value.TRUE);
      assertHalf(state, other, Value.FALSE, Value.TRUE);
      level(state, half[CLEAR], Value.TRUE);
      level(state, half[J], Value.FALSE);
      level(state, half[K], Value.FALSE);
      gate.propagate(state);
    }
  }

  @Test
  void bothAssertedOutputsAreHighUntilOneInputRemains() {
    final var gate = new Ttl74112();
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
    final var gate = new Ttl74112();
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
  void fallingEdgeAppliesTheJkFunctionTable() {
    final var gate = new Ttl74112();
    final var state = new TtlTestInstanceState(gate, false);
    idle(gate, state);

    for (final var half : new int[][] {FIRST, SECOND}) {
      clearHalf(gate, state, half);
      capture(gate, state, half, Value.FALSE, Value.FALSE);
      assertHalf(state, half, Value.FALSE, Value.TRUE);
      capture(gate, state, half, Value.TRUE, Value.FALSE);
      assertHalf(state, half, Value.TRUE, Value.FALSE);
      capture(gate, state, half, Value.FALSE, Value.FALSE);
      assertHalf(state, half, Value.TRUE, Value.FALSE);
      capture(gate, state, half, Value.FALSE, Value.TRUE);
      assertHalf(state, half, Value.FALSE, Value.TRUE);
      capture(gate, state, half, Value.TRUE, Value.TRUE);
      assertHalf(state, half, Value.TRUE, Value.FALSE);
      capture(gate, state, half, Value.TRUE, Value.TRUE);
      assertHalf(state, half, Value.FALSE, Value.TRUE);
      level(state, half[J], Value.FALSE);
      level(state, half[K], Value.FALSE);
    }
  }

  @Test
  void risingEdgeAndLevelChangesDoNotCapture() {
    final var gate = new Ttl74112();
    final var state = new TtlTestInstanceState(gate, false);
    idle(gate, state);
    clearHalf(gate, state, FIRST);

    level(state, FIRST[CP], Value.FALSE);
    gate.propagate(state);
    level(state, FIRST[J], Value.TRUE);
    level(state, FIRST[K], Value.FALSE);
    gate.propagate(state);
    assertHalf(state, FIRST, Value.FALSE, Value.TRUE);

    level(state, FIRST[CP], Value.TRUE);
    gate.propagate(state);
    assertHalf(state, FIRST, Value.FALSE, Value.TRUE);

    level(state, FIRST[CP], Value.FALSE);
    gate.propagate(state);
    assertHalf(state, FIRST, Value.TRUE, Value.FALSE);

    level(state, FIRST[J], Value.FALSE);
    level(state, FIRST[K], Value.TRUE);
    gate.propagate(state);
    assertHalf(state, FIRST, Value.TRUE, Value.FALSE);
  }

  @Test
  void unknownClockDoesNotCaptureAndUnknownDataFollowsTheForcedCases() {
    final var gate = new Ttl74112();
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
    capture(gate, state, FIRST, Value.TRUE, Value.FALSE);
    capture(gate, state, FIRST, Value.UNKNOWN, Value.FALSE);
    assertHalf(state, FIRST, Value.TRUE, Value.FALSE);

    capture(gate, state, FIRST, Value.ERROR, Value.FALSE);
    assertHalf(state, FIRST, Value.ERROR, Value.ERROR);
    capture(gate, state, FIRST, Value.TRUE, Value.FALSE);
    assertHalf(state, FIRST, Value.TRUE, Value.FALSE);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74112();
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
  void clockPinsAreTheTwoFallingEdgeClocks() {
    final var gate = new Ttl74112();
    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(
        new int[] {Ttl74112.PORT_INDEX_1CP, Ttl74112.PORT_INDEX_2CP}, gate.clockPinIndex(null));
  }

  @Test
  void vhdlClocksOnTheFallingEdgeWithClearBeforePreset() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "next1 <= (J1 AND NOT(state1)) OR (NOT(K1) AND state1);"));
    assertTrue(containsIgnoringCase(hdl, "next2 <= (J2 AND NOT(state2)) OR (NOT(K2) AND state2);"));
    assertTrue(containsIgnoringCase(hdl, "IF (nRD1 = '0') THEN state1 <= '0';"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (nSD1 = '0') THEN state1 <= '1';"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (falling_edge(clock)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (nRD2 = '0') THEN state2 <= '0';"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (falling_edge(clock2)) THEN"));
  }

  @Test
  void verilogClocksOnTheFallingEdgeWithClearBeforePreset() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign next1 = (J1 & ~state1) | (~K1 & state1);"));
    assertTrue(hdl.contains("assign next2 = (J2 & ~state2) | (~K2 & state2);"));
    assertTrue(hdl.contains("always @(negedge clock or negedge nRD1 or negedge nSD1)"));
    assertTrue(hdl.contains("if (nRD1 == 0) state1 <= 0;"));
    assertTrue(hdl.contains("else if (nSD1 == 0) state1 <= 1;"));
    assertTrue(hdl.contains("always @(negedge clock2 or negedge nRD2 or negedge nSD2)"));
    assertTrue(hdl.contains("if (nRD2 == 0) state2 <= 0;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74112HdlGenerator();
    final var attrs = new Ttl74112().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static void idle(Ttl74112 gate, TtlTestInstanceState state) {
    for (final var half : new int[][] {FIRST, SECOND}) {
      level(state, half[CP], Value.TRUE);
      level(state, half[J], Value.FALSE);
      level(state, half[K], Value.FALSE);
      level(state, half[SET], Value.TRUE);
      level(state, half[CLEAR], Value.TRUE);
    }
    gate.propagate(state);
    clearBoth(gate, state);
  }

  private static void clearBoth(Ttl74112 gate, TtlTestInstanceState state) {
    clearHalf(gate, state, FIRST);
    clearHalf(gate, state, SECOND);
  }

  private static void clearHalf(Ttl74112 gate, TtlTestInstanceState state, int[] half) {
    level(state, half[SET], Value.TRUE);
    level(state, half[CLEAR], Value.FALSE);
    gate.propagate(state);
    level(state, half[CLEAR], Value.TRUE);
    gate.propagate(state);
    assertHalf(state, half, Value.FALSE, Value.TRUE);
  }

  private static void capture(
      Ttl74112 gate, TtlTestInstanceState state, int[] half, Value j, Value k) {
    level(state, half[CP], Value.TRUE);
    level(state, half[J], j);
    level(state, half[K], k);
    gate.propagate(state);
    fall(gate, state, half[CP]);
  }

  private static void fall(Ttl74112 gate, TtlTestInstanceState state, int clock) {
    level(state, clock, Value.FALSE);
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

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74112().createAttributeSet();
    return String.join("\n", new Ttl74112HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
