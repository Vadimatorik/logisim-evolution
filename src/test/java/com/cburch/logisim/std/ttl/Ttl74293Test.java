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

class Ttl74293Test {
  @Test
  void usesPhysicalPinoutAndKeepsCornerPowerPortsLast() {
    final var gate = new Ttl74293();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(8, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74293.PORT_INDEX_QC, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74293.PORT_INDEX_QB, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74293.PORT_INDEX_QD, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74293.PORT_INDEX_QA, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74293.PORT_INDEX_A, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74293.PORT_INDEX_B, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74293.PORT_INDEX_R0_1, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74293.PORT_INDEX_R0_2, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(10, shownPower.getPorts().size());
    assertPort(shownPower, 8, 130, 30, EndData.INPUT_ONLY); // Physical pin 7: GND
    assertPort(shownPower, 9, 10, -30, EndData.INPUT_ONLY); // Physical pin 14: VCC
  }

  @Test
  void reportsBothClockPorts() {
    final var gate = new Ttl74293();

    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(
        new int[] {Ttl74293.PORT_INDEX_B, Ttl74293.PORT_INDEX_A}, gate.clockPinIndex(null));
  }

  @Test
  void fallingEdgesCountAndOneResetInputDoesNotClear() {
    final var gate = new Ttl74293();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    rise(gate, state, Ttl74293.PORT_INDEX_A);
    assertEquals(0, outputValue(state));
    fall(gate, state, Ttl74293.PORT_INDEX_A);
    assertEquals(1, outputValue(state));

    rise(gate, state, Ttl74293.PORT_INDEX_B);
    assertEquals(1, outputValue(state));
    fall(gate, state, Ttl74293.PORT_INDEX_B);
    assertEquals(3, outputValue(state));

    state.setPortValue(Ttl74293.PORT_INDEX_R0_1, Value.TRUE);
    gate.propagate(state);
    assertEquals(3, outputValue(state));

    state.setPortValue(Ttl74293.PORT_INDEX_R0_1, Value.FALSE);
    state.setPortValue(Ttl74293.PORT_INDEX_R0_2, Value.TRUE);
    gate.propagate(state);
    assertEquals(3, outputValue(state));

    state.setPortValue(Ttl74293.PORT_INDEX_R0_1, Value.TRUE);
    gate.propagate(state);
    assertEquals(0, outputValue(state));
  }

  @Test
  void clockBAdvancesOnlyTheUpperSection() {
    final var gate = new Ttl74293();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    for (var step = 1; step <= 8; step++) {
      pulse(gate, state, Ttl74293.PORT_INDEX_B);
      assertEquals((step & 7) << 1, outputValue(state));
    }
  }

  @Test
  void bothResetInputsOverrideTheClocks() {
    final var gate = new Ttl74293();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    pulse(gate, state, Ttl74293.PORT_INDEX_A);

    state.setPortValue(Ttl74293.PORT_INDEX_A, Value.TRUE);
    state.setPortValue(Ttl74293.PORT_INDEX_B, Value.TRUE);
    state.setPortValue(Ttl74293.PORT_INDEX_R0_1, Value.TRUE);
    state.setPortValue(Ttl74293.PORT_INDEX_R0_2, Value.TRUE);
    gate.propagate(state);
    assertEquals(0, outputValue(state));

    state.setPortValue(Ttl74293.PORT_INDEX_A, Value.FALSE);
    state.setPortValue(Ttl74293.PORT_INDEX_B, Value.FALSE);
    gate.propagate(state);
    assertEquals(0, outputValue(state));
  }

  @Test
  void qaToBCascadeCountsFromZeroThroughFifteen() {
    final var gate = new Ttl74293();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    for (var expected = 1; expected <= 16; expected++) {
      cascadeClockA(gate, state);
      assertEquals(expected & 0xF, outputValue(state));
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74293();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(8, Value.FALSE);
    state.setPortValue(9, Value.TRUE);
    reset(gate, state);
    pulse(gate, state, Ttl74293.PORT_INDEX_A);
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

  private static void reset(Ttl74293 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl74293.PORT_INDEX_A, Value.FALSE);
    state.setPortValue(Ttl74293.PORT_INDEX_B, Value.FALSE);
    state.setPortValue(Ttl74293.PORT_INDEX_R0_1, Value.TRUE);
    state.setPortValue(Ttl74293.PORT_INDEX_R0_2, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74293.PORT_INDEX_R0_1, Value.FALSE);
    state.setPortValue(Ttl74293.PORT_INDEX_R0_2, Value.FALSE);
    gate.propagate(state);
  }

  private static void rise(Ttl74293 gate, TtlTestInstanceState state, int clockPort) {
    state.setPortValue(clockPort, Value.TRUE);
    gate.propagate(state);
  }

  private static void fall(Ttl74293 gate, TtlTestInstanceState state, int clockPort) {
    state.setPortValue(clockPort, Value.FALSE);
    gate.propagate(state);
  }

  private static void pulse(Ttl74293 gate, TtlTestInstanceState state, int clockPort) {
    rise(gate, state, clockPort);
    fall(gate, state, clockPort);
  }

  private static void cascadeClockA(Ttl74293 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl74293.PORT_INDEX_B, state.getPortValue(Ttl74293.PORT_INDEX_QA));
    state.setPortValue(Ttl74293.PORT_INDEX_A, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74293.PORT_INDEX_B, state.getPortValue(Ttl74293.PORT_INDEX_QA));
    state.setPortValue(Ttl74293.PORT_INDEX_A, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74293.PORT_INDEX_B, state.getPortValue(Ttl74293.PORT_INDEX_QA));
    gate.propagate(state);
  }

  private static int outputValue(TtlTestInstanceState state) {
    return (int)
        (state.getPortValue(Ttl74293.PORT_INDEX_QA).toLongValue()
            | state.getPortValue(Ttl74293.PORT_INDEX_QB).toLongValue() << 1
            | state.getPortValue(Ttl74293.PORT_INDEX_QC).toLongValue() << 2
            | state.getPortValue(Ttl74293.PORT_INDEX_QD).toLongValue() << 3);
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74293.PORT_INDEX_QA));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74293.PORT_INDEX_QB));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74293.PORT_INDEX_QC));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74293.PORT_INDEX_QD));
  }
}
