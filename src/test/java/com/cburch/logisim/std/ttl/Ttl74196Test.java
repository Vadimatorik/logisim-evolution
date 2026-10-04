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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC196 presettable decade ripple counter. */
class Ttl74196Test {
  private static final int[] BCD = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0};
  private static final int[] BI_QUINARY = {2, 4, 6, 8, 1, 3, 5, 7, 9, 0};

  @Test
  void usesPhysicalPinoutAndKeepsPowerPortsLast() {
    final var gate = new Ttl74196();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74196.PORT_INDEX_PL, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74196.PORT_INDEX_Q2, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74196.PORT_INDEX_P2, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74196.PORT_INDEX_P0, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74196.PORT_INDEX_Q0, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74196.PORT_INDEX_CP1, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74196.PORT_INDEX_CP0, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74196.PORT_INDEX_Q1, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74196.PORT_INDEX_P1, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74196.PORT_INDEX_P3, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74196.PORT_INDEX_Q3, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74196.PORT_INDEX_MR, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertPort(shownPower, 12, 130, 30, EndData.INPUT_ONLY); // Physical pin 7: GND
    assertPort(shownPower, 13, 10, -30, EndData.INPUT_ONLY); // Physical pin 14: VCC
  }

  @Test
  void masterResetOverridesLoadAndClocks() {
    final var gate = new Ttl74196();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    load(gate, state, 0xA);
    assertEquals(0xA, outputCode(state));

    state.setPortValue(Ttl74196.PORT_INDEX_MR, Value.FALSE);
    state.setPortValue(Ttl74196.PORT_INDEX_PL, Value.FALSE);
    setData(state, 0xF);
    gate.propagate(state);
    assertEquals(0, outputCode(state));

    pulse(gate, state, Ttl74196.PORT_INDEX_CP0);
    pulse(gate, state, Ttl74196.PORT_INDEX_CP1);
    state.setPortValue(Ttl74196.PORT_INDEX_MR, Value.TRUE);
    state.setPortValue(Ttl74196.PORT_INDEX_PL, Value.TRUE);
    gate.propagate(state);
    assertEquals(0, outputCode(state));
  }

  @Test
  void parallelLoadIsTransparentAndOverridesClocks() {
    final var gate = new Ttl74196();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    for (var code = 0; code < 16; code++) {
      load(gate, state, code);
      assertEquals(code, outputCode(state));
    }

    state.setPortValue(Ttl74196.PORT_INDEX_PL, Value.FALSE);
    setData(state, 0x5);
    gate.propagate(state);
    assertEquals(0x5, outputCode(state));
    setData(state, 0x9);
    gate.propagate(state);
    assertEquals(0x9, outputCode(state));

    pulse(gate, state, Ttl74196.PORT_INDEX_CP0);
    pulse(gate, state, Ttl74196.PORT_INDEX_CP1);
    assertEquals(0x9, outputCode(state));

    state.setPortValue(Ttl74196.PORT_INDEX_PL, Value.TRUE);
    gate.propagate(state);
    assertEquals(0x9, outputCode(state));
    pulse(gate, state, Ttl74196.PORT_INDEX_CP0);
    assertEquals(0x8, outputCode(state));
  }

  @Test
  void fallingEdgesAdvanceIndependentSections() {
    final var gate = new Ttl74196();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    rise(gate, state, Ttl74196.PORT_INDEX_CP0);
    rise(gate, state, Ttl74196.PORT_INDEX_CP1);
    assertEquals(0, outputCode(state));

    pulse(gate, state, Ttl74196.PORT_INDEX_CP0);
    assertEquals(1, outputCode(state));
    pulse(gate, state, Ttl74196.PORT_INDEX_CP0);
    assertEquals(0, outputCode(state));

    for (var section = 1; section <= 5; section++) {
      pulse(gate, state, Ttl74196.PORT_INDEX_CP1);
      assertEquals((section % 5) << 1, outputCode(state));
    }
  }

  @Test
  void bcdCascadeCountsZeroThroughNine() {
    final var gate = new Ttl74196();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    for (final var expected : BCD) {
      cascade(gate, state, Ttl74196.PORT_INDEX_CP0, Ttl74196.PORT_INDEX_Q0, Ttl74196.PORT_INDEX_CP1);
      assertEquals(expected, outputCode(state));
    }
  }

  @Test
  void biQuinaryCascadeMatchesTheDatasheet() {
    final var gate = new Ttl74196();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    for (final var expected : BI_QUINARY) {
      cascade(gate, state, Ttl74196.PORT_INDEX_CP1, Ttl74196.PORT_INDEX_Q3, Ttl74196.PORT_INDEX_CP0);
      assertEquals(expected, outputCode(state));
    }
  }

  @Test
  void illegalDivideByFiveCodesReturnToZero() {
    final var gate = new Ttl74196();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    for (var section = 5; section <= 7; section++) {
      for (var q0 = 0; q0 <= 1; q0++) {
        load(gate, state, (section << 1) | q0);
        state.setPortValue(Ttl74196.PORT_INDEX_PL, Value.TRUE);
        gate.propagate(state);
        pulse(gate, state, Ttl74196.PORT_INDEX_CP1);
        assertEquals(q0, outputCode(state));
      }
    }
  }

  @Test
  void unknownControlsAffectOnlyDisagreedBits() {
    final var gate = new Ttl74196();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    state.setPortValue(Ttl74196.PORT_INDEX_MR, Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(0, outputCode(state));

    state.setPortValue(Ttl74196.PORT_INDEX_MR, Value.TRUE);
    pulse(gate, state, Ttl74196.PORT_INDEX_CP0);
    state.setPortValue(Ttl74196.PORT_INDEX_MR, Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74196.PORT_INDEX_Q0));
    assertEquals(Value.FALSE, state.getPortValue(Ttl74196.PORT_INDEX_Q1));
    assertEquals(Value.FALSE, state.getPortValue(Ttl74196.PORT_INDEX_Q2));
    assertEquals(Value.FALSE, state.getPortValue(Ttl74196.PORT_INDEX_Q3));

    state.setPortValue(Ttl74196.PORT_INDEX_MR, Value.ERROR);
    gate.propagate(state);
    assertEquals(Value.ERROR, state.getPortValue(Ttl74196.PORT_INDEX_Q0));

    reset(gate, state);
    state.setPortValue(Ttl74196.PORT_INDEX_PL, Value.UNKNOWN);
    setData(state, 0);
    gate.propagate(state);
    assertEquals(0, outputCode(state));

    setData(state, 0xF);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74196.PORT_INDEX_Q0));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74196.PORT_INDEX_Q1));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74196.PORT_INDEX_Q2));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74196.PORT_INDEX_Q3));

    state.setPortValue(Ttl74196.PORT_INDEX_PL, Value.FALSE);
    state.setPortValue(Ttl74196.PORT_INDEX_P1, Value.UNKNOWN);
    state.setPortValue(Ttl74196.PORT_INDEX_P2, Value.TRUE);
    state.setPortValue(Ttl74196.PORT_INDEX_P3, Value.TRUE);
    state.setPortValue(Ttl74196.PORT_INDEX_P0, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74196.PORT_INDEX_PL, Value.TRUE);
    gate.propagate(state);
    pulse(gate, state, Ttl74196.PORT_INDEX_CP1);
    assertEquals(0, outputCode(state));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74196();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(12, Value.FALSE);
    state.setPortValue(13, Value.TRUE);
    reset(gate, state);
    pulse(gate, state, Ttl74196.PORT_INDEX_CP0);
    assertEquals(1, outputCode(state));

    state.setPortValue(13, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(13, Value.TRUE);
    gate.propagate(state);
    assertEquals(1, outputCode(state));

    state.setPortValue(12, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void reset(Ttl74196 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl74196.PORT_INDEX_CP0, Value.FALSE);
    state.setPortValue(Ttl74196.PORT_INDEX_CP1, Value.FALSE);
    state.setPortValue(Ttl74196.PORT_INDEX_PL, Value.TRUE);
    setData(state, 0);
    state.setPortValue(Ttl74196.PORT_INDEX_MR, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74196.PORT_INDEX_MR, Value.TRUE);
    gate.propagate(state);
  }

  private static void load(Ttl74196 gate, TtlTestInstanceState state, int code) {
    state.setPortValue(Ttl74196.PORT_INDEX_MR, Value.TRUE);
    state.setPortValue(Ttl74196.PORT_INDEX_PL, Value.FALSE);
    setData(state, code);
    gate.propagate(state);
  }

  private static void setData(TtlTestInstanceState state, int code) {
    state.setPortValue(Ttl74196.PORT_INDEX_P0, bit(code, 0));
    state.setPortValue(Ttl74196.PORT_INDEX_P1, bit(code, 1));
    state.setPortValue(Ttl74196.PORT_INDEX_P2, bit(code, 2));
    state.setPortValue(Ttl74196.PORT_INDEX_P3, bit(code, 3));
  }

  private static Value bit(int code, int shift) {
    return ((code >> shift) & 1) == 1 ? Value.TRUE : Value.FALSE;
  }

  private static void rise(Ttl74196 gate, TtlTestInstanceState state, int clockPort) {
    state.setPortValue(clockPort, Value.TRUE);
    gate.propagate(state);
  }

  private static void pulse(Ttl74196 gate, TtlTestInstanceState state, int clockPort) {
    rise(gate, state, clockPort);
    state.setPortValue(clockPort, Value.FALSE);
    gate.propagate(state);
  }

  private static void cascade(
      Ttl74196 gate, TtlTestInstanceState state, int clockPort, int sourcePort, int destinationPort) {
    state.setPortValue(destinationPort, state.getPortValue(sourcePort));
    state.setPortValue(clockPort, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(destinationPort, state.getPortValue(sourcePort));
    state.setPortValue(clockPort, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(destinationPort, state.getPortValue(sourcePort));
    gate.propagate(state);
  }

  private static int outputCode(TtlTestInstanceState state) {
    final var q0 = state.getPortValue(Ttl74196.PORT_INDEX_Q0);
    final var q1 = state.getPortValue(Ttl74196.PORT_INDEX_Q1);
    final var q2 = state.getPortValue(Ttl74196.PORT_INDEX_Q2);
    final var q3 = state.getPortValue(Ttl74196.PORT_INDEX_Q3);
    assertTrue(q0 == Value.TRUE || q0 == Value.FALSE);
    assertTrue(q1 == Value.TRUE || q1 == Value.FALSE);
    assertTrue(q2 == Value.TRUE || q2 == Value.FALSE);
    assertTrue(q3 == Value.TRUE || q3 == Value.FALSE);
    return (q0 == Value.TRUE ? 1 : 0)
        | (q1 == Value.TRUE ? 2 : 0)
        | (q2 == Value.TRUE ? 4 : 0)
        | (q3 == Value.TRUE ? 8 : 0);
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74196.PORT_INDEX_Q0));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74196.PORT_INDEX_Q1));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74196.PORT_INDEX_Q2));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74196.PORT_INDEX_Q3));
  }
}
