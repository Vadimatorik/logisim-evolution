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

/** Functional tests for the 7492 divide-by-twelve counter. */
class Ttl7492Test {
  private static final int[] DIVIDE_BY_TWELVE = {1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13, 0};
  private static final int[] DIVIDE_BY_SIX = {2, 4, 8, 10, 12, 0};

  @Test
  void usesPhysicalPinoutAndKeepsPowerPortsLast() {
    final var gate = new Ttl7492();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(8, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl7492.PORT_INDEX_CKB, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7492.PORT_INDEX_R0_1, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7492.PORT_INDEX_R0_2, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7492.PORT_INDEX_QD, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7492.PORT_INDEX_QC, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7492.PORT_INDEX_QB, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7492.PORT_INDEX_QA, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7492.PORT_INDEX_CKA, 10, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(10, shownPower.getPorts().size());
    assertPort(shownPower, 8, 90, -30, EndData.INPUT_ONLY); // Physical pin 10: GND
    assertPort(shownPower, 9, 90, 30, EndData.INPUT_ONLY); // Physical pin 5: VCC
  }

  @Test
  void risingEdgeDoesNotCountAndClockATogglesOnlyQa() {
    final var gate = new Ttl7492();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    state.setPortValue(Ttl7492.PORT_INDEX_CKA, Value.TRUE);
    gate.propagate(state);
    assertEquals(0, outputValue(state));

    state.setPortValue(Ttl7492.PORT_INDEX_CKA, Value.FALSE);
    gate.propagate(state);
    assertEquals(1, outputValue(state));

    pulse(gate, state, Ttl7492.PORT_INDEX_CKA);
    assertEquals(0, outputValue(state));
    pulse(gate, state, Ttl7492.PORT_INDEX_CKA);
    assertEquals(1, outputValue(state));
    pulse(gate, state, Ttl7492.PORT_INDEX_CKA);
    assertEquals(0, outputValue(state));
  }

  @Test
  void clockBCountsDivideBySixWithoutChangingQa() {
    final var gate = new Ttl7492();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    for (final var expected : DIVIDE_BY_SIX) {
      pulse(gate, state, Ttl7492.PORT_INDEX_CKB);
      assertEquals(expected, outputValue(state));
      assertEquals(Value.FALSE, state.getPortValue(Ttl7492.PORT_INDEX_QA));
    }
  }

  @Test
  void qaToCkbCascadeCountsDivideByTwelve() {
    final var gate = new Ttl7492();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    for (var step = 0; step < DIVIDE_BY_TWELVE.length; step++) {
      cascadeClockA(gate, state);
      assertEquals(DIVIDE_BY_TWELVE[step], outputValue(state));
      final var qdHigh = step >= 5 && step <= 10;
      assertEquals(qdHigh ? Value.TRUE : Value.FALSE, state.getPortValue(Ttl7492.PORT_INDEX_QD));
    }
  }

  @Test
  void resetRequiresBothInputsAndOverridesTheClocks() {
    final var gate = new Ttl7492();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    pulse(gate, state, Ttl7492.PORT_INDEX_CKA);
    pulse(gate, state, Ttl7492.PORT_INDEX_CKB);
    assertEquals(3, outputValue(state));

    state.setPortValue(Ttl7492.PORT_INDEX_R0_1, Value.TRUE);
    gate.propagate(state);
    assertEquals(3, outputValue(state));
    state.setPortValue(Ttl7492.PORT_INDEX_R0_1, Value.FALSE);
    state.setPortValue(Ttl7492.PORT_INDEX_R0_2, Value.TRUE);
    gate.propagate(state);
    assertEquals(3, outputValue(state));

    state.setPortValue(Ttl7492.PORT_INDEX_R0_1, Value.TRUE);
    gate.propagate(state);
    assertEquals(0, outputValue(state));

    pulse(gate, state, Ttl7492.PORT_INDEX_CKA);
    pulse(gate, state, Ttl7492.PORT_INDEX_CKB);
    assertEquals(0, outputValue(state));

    state.setPortValue(Ttl7492.PORT_INDEX_R0_1, Value.FALSE);
    state.setPortValue(Ttl7492.PORT_INDEX_R0_2, Value.FALSE);
    gate.propagate(state);
    pulse(gate, state, Ttl7492.PORT_INDEX_CKA);
    assertEquals(1, outputValue(state));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl7492();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(8, Value.FALSE);
    state.setPortValue(9, Value.TRUE);
    reset(gate, state);
    pulse(gate, state, Ttl7492.PORT_INDEX_CKA);
    assertEquals(1, outputValue(state));

    state.setPortValue(9, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(9, Value.TRUE);
    gate.propagate(state);
    assertEquals(1, outputValue(state));

    state.setPortValue(8, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void reset(Ttl7492 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl7492.PORT_INDEX_CKA, Value.FALSE);
    state.setPortValue(Ttl7492.PORT_INDEX_CKB, Value.FALSE);
    state.setPortValue(Ttl7492.PORT_INDEX_R0_1, Value.TRUE);
    state.setPortValue(Ttl7492.PORT_INDEX_R0_2, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl7492.PORT_INDEX_R0_1, Value.FALSE);
    state.setPortValue(Ttl7492.PORT_INDEX_R0_2, Value.FALSE);
    gate.propagate(state);
  }

  private static void pulse(Ttl7492 gate, TtlTestInstanceState state, int clockPort) {
    state.setPortValue(clockPort, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(clockPort, Value.FALSE);
    gate.propagate(state);
  }

  private static void cascadeClockA(Ttl7492 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl7492.PORT_INDEX_CKB, state.getPortValue(Ttl7492.PORT_INDEX_QA));
    state.setPortValue(Ttl7492.PORT_INDEX_CKA, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl7492.PORT_INDEX_CKB, state.getPortValue(Ttl7492.PORT_INDEX_QA));
    state.setPortValue(Ttl7492.PORT_INDEX_CKA, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl7492.PORT_INDEX_CKB, state.getPortValue(Ttl7492.PORT_INDEX_QA));
    gate.propagate(state);
  }

  private static int outputValue(TtlTestInstanceState state) {
    return (int)
        (state.getPortValue(Ttl7492.PORT_INDEX_QA).toLongValue()
            | state.getPortValue(Ttl7492.PORT_INDEX_QB).toLongValue() << 1
            | state.getPortValue(Ttl7492.PORT_INDEX_QC).toLongValue() << 2
            | state.getPortValue(Ttl7492.PORT_INDEX_QD).toLongValue() << 3);
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7492.PORT_INDEX_QA));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7492.PORT_INDEX_QB));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7492.PORT_INDEX_QC));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7492.PORT_INDEX_QD));
  }
}
