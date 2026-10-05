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
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

class Ttl74290Test {
  private static final int[] BI_QUINARY = {0, 2, 4, 6, 8, 1, 3, 5, 7, 9};
  private static final BitWidth WIDTH = BitWidth.create(4);

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74290();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(10, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74290.PORT_INDEX_CP1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74290.PORT_INDEX_MR1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74290.PORT_INDEX_MR2, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74290.PORT_INDEX_MS1, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74290.PORT_INDEX_MS2, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74290.PORT_INDEX_Q2, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74290.PORT_INDEX_Q1, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74290.PORT_INDEX_Q3, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74290.PORT_INDEX_Q0, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74290.PORT_INDEX_CP0, 10, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(12, shownPower.getPorts().size());
    assertPort(shownPower, 10, 90, -30, EndData.INPUT_ONLY); // Physical pin 10: GND
    assertPort(shownPower, 11, 90, 30, EndData.INPUT_ONLY); // Physical pin 5: VCC
  }

  @Test
  void cp0FallingEdgeTogglesOnlyQ0() {
    final var gate = new Ttl74290();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    rise(gate, state, Ttl74290.PORT_INDEX_CP0);
    assertEquals(0, outputValue(state));

    fall(gate, state, Ttl74290.PORT_INDEX_CP0);
    assertEquals(1, outputValue(state));

    fallAfterRise(gate, state, Ttl74290.PORT_INDEX_CP0);
    assertEquals(0, outputValue(state));
  }

  @Test
  void cp1FallingEdgeCyclesTheDivideByFiveSection() {
    final var gate = new Ttl74290();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    fallAfterRise(gate, state, Ttl74290.PORT_INDEX_CP0);
    assertEquals(1, outputValue(state));

    final var expected = new int[] {3, 5, 7, 9, 1};
    for (var i = 0; i < expected.length; i++) {
      fallAfterRise(gate, state, Ttl74290.PORT_INDEX_CP1);
      assertEquals(expected[i], outputValue(state));
    }
  }

  @Test
  void q0ToCp1CascadeCountsBcdFromZeroThroughNine() {
    final var gate = new Ttl74290();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    for (var expected = 1; expected <= 10; expected++) {
      cascadeQ0ToCp1(gate, state);
      assertEquals(expected % 10, outputValue(state));
    }
  }

  @Test
  void q3ToCp0CascadeFollowsTheBiQuinarySequence() {
    final var gate = new Ttl74290();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    for (var step = 1; step <= BI_QUINARY.length; step++) {
      cascadeQ3ToCp0(gate, state);
      assertEquals(BI_QUINARY[step % BI_QUINARY.length], outputValue(state));
    }
  }

  @Test
  void setToNineOverridesResetAndClocks() {
    final var gate = new Ttl74290();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    fallAfterRise(gate, state, Ttl74290.PORT_INDEX_CP0);
    assertEquals(1, outputValue(state));

    state.setPortValue(Ttl74290.PORT_INDEX_MR1, Value.TRUE);
    state.setPortValue(Ttl74290.PORT_INDEX_MR2, Value.TRUE);
    state.setPortValue(Ttl74290.PORT_INDEX_MS1, Value.TRUE);
    state.setPortValue(Ttl74290.PORT_INDEX_MS2, Value.TRUE);
    state.setPortValue(Ttl74290.PORT_INDEX_CP0, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74290.PORT_INDEX_CP0, Value.FALSE);
    gate.propagate(state);
    assertEquals(9, outputValue(state));

    state.setPortValue(Ttl74290.PORT_INDEX_MS1, Value.FALSE);
    state.setPortValue(Ttl74290.PORT_INDEX_MS2, Value.FALSE);
    gate.propagate(state);
    assertEquals(0, outputValue(state));
  }

  @Test
  void oneResetOrSetInputDoesNotChangeTheMode() {
    final var gate = new Ttl74290();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    fallAfterRise(gate, state, Ttl74290.PORT_INDEX_CP1);
    assertEquals(2, outputValue(state));

    state.setPortValue(Ttl74290.PORT_INDEX_MR1, Value.TRUE);
    gate.propagate(state);
    assertEquals(2, outputValue(state));

    state.setPortValue(Ttl74290.PORT_INDEX_MS1, Value.TRUE);
    gate.propagate(state);
    fallAfterRise(gate, state, Ttl74290.PORT_INDEX_CP0);
    assertEquals(3, outputValue(state));

    state.setPortValue(Ttl74290.PORT_INDEX_MR1, Value.FALSE);
    state.setPortValue(Ttl74290.PORT_INDEX_MR2, Value.TRUE);
    state.setPortValue(Ttl74290.PORT_INDEX_MS2, Value.TRUE);
    gate.propagate(state);
    assertEquals(9, outputValue(state));
  }

  @Test
  void risingAndUnknownClocksDoNotCount() {
    final var gate = new Ttl74290();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    state.setPortValue(Ttl74290.PORT_INDEX_CP0, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74290.PORT_INDEX_CP0, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl74290.PORT_INDEX_CP0, Value.FALSE);
    gate.propagate(state);
    assertEquals(0, outputValue(state));

    state.setPortValue(Ttl74290.PORT_INDEX_CP1, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl74290.PORT_INDEX_CP1, Value.FALSE);
    gate.propagate(state);
    assertEquals(0, outputValue(state));
  }

  @Test
  void unknownResetOrSetDoesNotForceACode() {
    final var gate = new Ttl74290();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    fallAfterRise(gate, state, Ttl74290.PORT_INDEX_CP0);
    assertEquals(1, outputValue(state));

    state.setPortValue(Ttl74290.PORT_INDEX_MR1, Value.TRUE);
    state.setPortValue(Ttl74290.PORT_INDEX_MR2, Value.UNKNOWN);
    state.setPortValue(Ttl74290.PORT_INDEX_MS1, Value.TRUE);
    state.setPortValue(Ttl74290.PORT_INDEX_MS2, Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(1, outputValue(state));

    fallAfterRise(gate, state, Ttl74290.PORT_INDEX_CP0);
    assertEquals(0, outputValue(state));
  }

  @Test
  void undefinedSectionBecomesUnknownOnItsClock() {
    final var gate = new Ttl74290();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    final var bits = Value.createKnown(WIDTH, 0).getAll();
    bits[0] = Value.UNKNOWN;
    forceBits(state, bits);

    fallAfterRise(gate, state, Ttl74290.PORT_INDEX_CP0);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74290.PORT_INDEX_Q0));
    assertEquals(Value.FALSE, state.getPortValue(Ttl74290.PORT_INDEX_Q1));
    assertEquals(Value.FALSE, state.getPortValue(Ttl74290.PORT_INDEX_Q2));
    assertEquals(Value.FALSE, state.getPortValue(Ttl74290.PORT_INDEX_Q3));

    bits[1] = Value.UNKNOWN;
    forceBits(state, bits);
    fallAfterRise(gate, state, Ttl74290.PORT_INDEX_CP1);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74290.PORT_INDEX_Q0));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74290.PORT_INDEX_Q1));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74290.PORT_INDEX_Q2));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74290.PORT_INDEX_Q3));

    state.setPortValue(Ttl74290.PORT_INDEX_MR1, Value.TRUE);
    state.setPortValue(Ttl74290.PORT_INDEX_MR2, Value.TRUE);
    gate.propagate(state);
    assertEquals(0, outputValue(state));
  }

  @Test
  void errorInASectionBecomesErrorOnItsClock() {
    final var gate = new Ttl74290();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    final var bits = Value.createKnown(WIDTH, 1).getAll();
    bits[1] = Value.ERROR;
    forceBits(state, bits);

    fallAfterRise(gate, state, Ttl74290.PORT_INDEX_CP1);
    assertEquals(Value.TRUE, state.getPortValue(Ttl74290.PORT_INDEX_Q0));
    assertEquals(Value.ERROR, state.getPortValue(Ttl74290.PORT_INDEX_Q1));
    assertEquals(Value.ERROR, state.getPortValue(Ttl74290.PORT_INDEX_Q2));
    assertEquals(Value.ERROR, state.getPortValue(Ttl74290.PORT_INDEX_Q3));

    fallAfterRise(gate, state, Ttl74290.PORT_INDEX_CP0);
    assertEquals(Value.FALSE, state.getPortValue(Ttl74290.PORT_INDEX_Q0));
    assertEquals(Value.ERROR, state.getPortValue(Ttl74290.PORT_INDEX_Q1));

    state.setPortValue(Ttl74290.PORT_INDEX_MS1, Value.TRUE);
    state.setPortValue(Ttl74290.PORT_INDEX_MS2, Value.TRUE);
    gate.propagate(state);
    assertEquals(9, outputValue(state));
  }

  @Test
  void codesOutsideTheDivideByFiveSequenceReturnToZero() {
    final var gate = new Ttl74290();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    for (var code = 5; code <= 7; code++) {
      for (var q0 = 0; q0 <= 1; q0++) {
        forceValue(state, q0 + 2 * code);
        fallAfterRise(gate, state, Ttl74290.PORT_INDEX_CP1);
        assertEquals(q0, outputValue(state));
      }
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74290();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(10, Value.FALSE);
    state.setPortValue(11, Value.TRUE);
    reset(gate, state);
    fallAfterRise(gate, state, Ttl74290.PORT_INDEX_CP0);
    assertEquals(1, outputValue(state));

    state.setPortValue(11, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(11, Value.TRUE);
    gate.propagate(state);
    assertEquals(1, outputValue(state));

    state.setPortValue(10, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void reset(Ttl74290 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl74290.PORT_INDEX_CP0, Value.FALSE);
    state.setPortValue(Ttl74290.PORT_INDEX_CP1, Value.FALSE);
    state.setPortValue(Ttl74290.PORT_INDEX_MR1, Value.TRUE);
    state.setPortValue(Ttl74290.PORT_INDEX_MR2, Value.TRUE);
    state.setPortValue(Ttl74290.PORT_INDEX_MS1, Value.FALSE);
    state.setPortValue(Ttl74290.PORT_INDEX_MS2, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74290.PORT_INDEX_MR1, Value.FALSE);
    state.setPortValue(Ttl74290.PORT_INDEX_MR2, Value.FALSE);
    gate.propagate(state);
  }

  private static void rise(Ttl74290 gate, TtlTestInstanceState state, int clockPort) {
    state.setPortValue(clockPort, Value.TRUE);
    gate.propagate(state);
  }

  private static void fall(Ttl74290 gate, TtlTestInstanceState state, int clockPort) {
    state.setPortValue(clockPort, Value.FALSE);
    gate.propagate(state);
  }

  private static void fallAfterRise(Ttl74290 gate, TtlTestInstanceState state, int clockPort) {
    rise(gate, state, clockPort);
    fall(gate, state, clockPort);
  }

  private static void cascadeQ0ToCp1(Ttl74290 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl74290.PORT_INDEX_CP1, state.getPortValue(Ttl74290.PORT_INDEX_Q0));
    rise(gate, state, Ttl74290.PORT_INDEX_CP0);
    state.setPortValue(Ttl74290.PORT_INDEX_CP1, state.getPortValue(Ttl74290.PORT_INDEX_Q0));
    fall(gate, state, Ttl74290.PORT_INDEX_CP0);
    state.setPortValue(Ttl74290.PORT_INDEX_CP1, state.getPortValue(Ttl74290.PORT_INDEX_Q0));
    gate.propagate(state);
  }

  private static void cascadeQ3ToCp0(Ttl74290 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl74290.PORT_INDEX_CP0, state.getPortValue(Ttl74290.PORT_INDEX_Q3));
    rise(gate, state, Ttl74290.PORT_INDEX_CP1);
    state.setPortValue(Ttl74290.PORT_INDEX_CP0, state.getPortValue(Ttl74290.PORT_INDEX_Q3));
    fall(gate, state, Ttl74290.PORT_INDEX_CP1);
    state.setPortValue(Ttl74290.PORT_INDEX_CP0, state.getPortValue(Ttl74290.PORT_INDEX_Q3));
    gate.propagate(state);
  }

  private static void forceValue(TtlTestInstanceState state, int nibble) {
    final var data = new TtlRegisterData(WIDTH);
    data.setValue(Value.createKnown(WIDTH, nibble));
    state.setData(data);
  }

  private static void forceBits(TtlTestInstanceState state, Value[] bits) {
    final var data = new TtlRegisterData(WIDTH);
    data.setValue(Value.create(bits));
    state.setData(data);
  }

  private static int outputValue(TtlTestInstanceState state) {
    return (int)
        (state.getPortValue(Ttl74290.PORT_INDEX_Q0).toLongValue()
            | state.getPortValue(Ttl74290.PORT_INDEX_Q1).toLongValue() << 1
            | state.getPortValue(Ttl74290.PORT_INDEX_Q2).toLongValue() << 2
            | state.getPortValue(Ttl74290.PORT_INDEX_Q3).toLongValue() << 3);
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74290.PORT_INDEX_Q0));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74290.PORT_INDEX_Q1));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74290.PORT_INDEX_Q2));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74290.PORT_INDEX_Q3));
  }
}
