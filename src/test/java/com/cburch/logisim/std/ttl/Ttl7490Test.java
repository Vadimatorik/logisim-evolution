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

class Ttl7490Test {
  private static final int[] BI_QUINARY = {0, 2, 4, 6, 8, 1, 3, 5, 7, 9};
  private static final BitWidth WIDTH = BitWidth.create(4);

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl7490();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(10, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl7490.PORT_INDEX_CKB, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7490.PORT_INDEX_R0_1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7490.PORT_INDEX_R0_2, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7490.PORT_INDEX_R9_1, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7490.PORT_INDEX_R9_2, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7490.PORT_INDEX_QC, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7490.PORT_INDEX_QB, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7490.PORT_INDEX_QD, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7490.PORT_INDEX_QA, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7490.PORT_INDEX_CKA, 10, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(12, shownPower.getPorts().size());
    assertPort(shownPower, 10, 90, -30, EndData.INPUT_ONLY); // Physical pin 10: GND
    assertPort(shownPower, 11, 90, 30, EndData.INPUT_ONLY); // Physical pin 5: VCC
  }

  @Test
  void ckaFallingEdgeTogglesOnlyQa() {
    final var gate = new Ttl7490();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    rise(gate, state, Ttl7490.PORT_INDEX_CKA);
    assertEquals(0, outputValue(state));

    fall(gate, state, Ttl7490.PORT_INDEX_CKA);
    assertEquals(1, outputValue(state));

    fallAfterRise(gate, state, Ttl7490.PORT_INDEX_CKA);
    assertEquals(0, outputValue(state));
  }

  @Test
  void ckbFallingEdgeCyclesTheDivideByFiveSection() {
    final var gate = new Ttl7490();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    fallAfterRise(gate, state, Ttl7490.PORT_INDEX_CKA);
    assertEquals(1, outputValue(state));

    final var expected = new int[] {3, 5, 7, 9, 1};
    for (var i = 0; i < expected.length; i++) {
      fallAfterRise(gate, state, Ttl7490.PORT_INDEX_CKB);
      assertEquals(expected[i], outputValue(state));
    }
  }

  @Test
  void qaToCkbCascadeCountsBcdFromZeroThroughNine() {
    final var gate = new Ttl7490();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    for (var expected = 1; expected <= 10; expected++) {
      cascadeQaToCkb(gate, state);
      assertEquals(expected % 10, outputValue(state));
    }
  }

  @Test
  void qdToCkaCascadeFollowsTheBiQuinarySequence() {
    final var gate = new Ttl7490();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    for (var step = 1; step <= BI_QUINARY.length; step++) {
      cascadeQdToCka(gate, state);
      assertEquals(BI_QUINARY[step % BI_QUINARY.length], outputValue(state));
    }
  }

  @Test
  void setToNineOverridesResetAndClocks() {
    final var gate = new Ttl7490();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    fallAfterRise(gate, state, Ttl7490.PORT_INDEX_CKA);
    assertEquals(1, outputValue(state));

    state.setPortValue(Ttl7490.PORT_INDEX_R0_1, Value.TRUE);
    state.setPortValue(Ttl7490.PORT_INDEX_R0_2, Value.TRUE);
    state.setPortValue(Ttl7490.PORT_INDEX_R9_1, Value.TRUE);
    state.setPortValue(Ttl7490.PORT_INDEX_R9_2, Value.TRUE);
    state.setPortValue(Ttl7490.PORT_INDEX_CKA, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl7490.PORT_INDEX_CKA, Value.FALSE);
    gate.propagate(state);
    assertEquals(9, outputValue(state));

    state.setPortValue(Ttl7490.PORT_INDEX_R9_1, Value.FALSE);
    state.setPortValue(Ttl7490.PORT_INDEX_R9_2, Value.FALSE);
    gate.propagate(state);
    assertEquals(0, outputValue(state));
  }

  @Test
  void oneResetOrSetInputDoesNotChangeTheMode() {
    final var gate = new Ttl7490();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    fallAfterRise(gate, state, Ttl7490.PORT_INDEX_CKB);
    assertEquals(2, outputValue(state));

    state.setPortValue(Ttl7490.PORT_INDEX_R0_1, Value.TRUE);
    gate.propagate(state);
    assertEquals(2, outputValue(state));

    state.setPortValue(Ttl7490.PORT_INDEX_R9_1, Value.TRUE);
    gate.propagate(state);
    fallAfterRise(gate, state, Ttl7490.PORT_INDEX_CKA);
    assertEquals(3, outputValue(state));

    state.setPortValue(Ttl7490.PORT_INDEX_R0_1, Value.FALSE);
    state.setPortValue(Ttl7490.PORT_INDEX_R0_2, Value.TRUE);
    state.setPortValue(Ttl7490.PORT_INDEX_R9_2, Value.TRUE);
    gate.propagate(state);
    assertEquals(9, outputValue(state));
  }

  @Test
  void risingAndUnknownClocksDoNotCount() {
    final var gate = new Ttl7490();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    state.setPortValue(Ttl7490.PORT_INDEX_CKA, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl7490.PORT_INDEX_CKA, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl7490.PORT_INDEX_CKA, Value.FALSE);
    gate.propagate(state);
    assertEquals(0, outputValue(state));

    state.setPortValue(Ttl7490.PORT_INDEX_CKB, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl7490.PORT_INDEX_CKB, Value.FALSE);
    gate.propagate(state);
    assertEquals(0, outputValue(state));
  }

  @Test
  void unknownResetOrSetDoesNotForceACode() {
    final var gate = new Ttl7490();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    fallAfterRise(gate, state, Ttl7490.PORT_INDEX_CKA);
    assertEquals(1, outputValue(state));

    state.setPortValue(Ttl7490.PORT_INDEX_R0_1, Value.TRUE);
    state.setPortValue(Ttl7490.PORT_INDEX_R0_2, Value.UNKNOWN);
    state.setPortValue(Ttl7490.PORT_INDEX_R9_1, Value.TRUE);
    state.setPortValue(Ttl7490.PORT_INDEX_R9_2, Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(1, outputValue(state));

    fallAfterRise(gate, state, Ttl7490.PORT_INDEX_CKA);
    assertEquals(0, outputValue(state));
  }

  @Test
  void undefinedSectionBecomesUnknownOnItsClock() {
    final var gate = new Ttl7490();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    final var bits = Value.createKnown(WIDTH, 0).getAll();
    bits[0] = Value.UNKNOWN;
    forceBits(state, bits);

    fallAfterRise(gate, state, Ttl7490.PORT_INDEX_CKA);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7490.PORT_INDEX_QA));
    assertEquals(Value.FALSE, state.getPortValue(Ttl7490.PORT_INDEX_QB));
    assertEquals(Value.FALSE, state.getPortValue(Ttl7490.PORT_INDEX_QC));
    assertEquals(Value.FALSE, state.getPortValue(Ttl7490.PORT_INDEX_QD));

    bits[1] = Value.UNKNOWN;
    forceBits(state, bits);
    fallAfterRise(gate, state, Ttl7490.PORT_INDEX_CKB);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7490.PORT_INDEX_QA));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7490.PORT_INDEX_QB));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7490.PORT_INDEX_QC));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7490.PORT_INDEX_QD));

    state.setPortValue(Ttl7490.PORT_INDEX_R0_1, Value.TRUE);
    state.setPortValue(Ttl7490.PORT_INDEX_R0_2, Value.TRUE);
    gate.propagate(state);
    assertEquals(0, outputValue(state));
  }

  @Test
  void errorInASectionBecomesErrorOnItsClock() {
    final var gate = new Ttl7490();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    final var bits = Value.createKnown(WIDTH, 1).getAll();
    bits[1] = Value.ERROR;
    forceBits(state, bits);

    fallAfterRise(gate, state, Ttl7490.PORT_INDEX_CKB);
    assertEquals(Value.TRUE, state.getPortValue(Ttl7490.PORT_INDEX_QA));
    assertEquals(Value.ERROR, state.getPortValue(Ttl7490.PORT_INDEX_QB));
    assertEquals(Value.ERROR, state.getPortValue(Ttl7490.PORT_INDEX_QC));
    assertEquals(Value.ERROR, state.getPortValue(Ttl7490.PORT_INDEX_QD));

    fallAfterRise(gate, state, Ttl7490.PORT_INDEX_CKA);
    assertEquals(Value.FALSE, state.getPortValue(Ttl7490.PORT_INDEX_QA));
    assertEquals(Value.ERROR, state.getPortValue(Ttl7490.PORT_INDEX_QB));

    state.setPortValue(Ttl7490.PORT_INDEX_R9_1, Value.TRUE);
    state.setPortValue(Ttl7490.PORT_INDEX_R9_2, Value.TRUE);
    gate.propagate(state);
    assertEquals(9, outputValue(state));
  }

  @Test
  void codesOutsideTheDivideByFiveSequenceReturnToZero() {
    final var gate = new Ttl7490();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    for (var code = 5; code <= 7; code++) {
      for (var qa = 0; qa <= 1; qa++) {
        forceValue(state, qa + 2 * code);
        fallAfterRise(gate, state, Ttl7490.PORT_INDEX_CKB);
        assertEquals(qa, outputValue(state));
      }
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl7490();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(10, Value.FALSE);
    state.setPortValue(11, Value.TRUE);
    reset(gate, state);
    fallAfterRise(gate, state, Ttl7490.PORT_INDEX_CKA);
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

  private static void reset(Ttl7490 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl7490.PORT_INDEX_CKA, Value.FALSE);
    state.setPortValue(Ttl7490.PORT_INDEX_CKB, Value.FALSE);
    state.setPortValue(Ttl7490.PORT_INDEX_R0_1, Value.TRUE);
    state.setPortValue(Ttl7490.PORT_INDEX_R0_2, Value.TRUE);
    state.setPortValue(Ttl7490.PORT_INDEX_R9_1, Value.FALSE);
    state.setPortValue(Ttl7490.PORT_INDEX_R9_2, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl7490.PORT_INDEX_R0_1, Value.FALSE);
    state.setPortValue(Ttl7490.PORT_INDEX_R0_2, Value.FALSE);
    gate.propagate(state);
  }

  private static void rise(Ttl7490 gate, TtlTestInstanceState state, int clockPort) {
    state.setPortValue(clockPort, Value.TRUE);
    gate.propagate(state);
  }

  private static void fall(Ttl7490 gate, TtlTestInstanceState state, int clockPort) {
    state.setPortValue(clockPort, Value.FALSE);
    gate.propagate(state);
  }

  private static void fallAfterRise(Ttl7490 gate, TtlTestInstanceState state, int clockPort) {
    rise(gate, state, clockPort);
    fall(gate, state, clockPort);
  }

  private static void cascadeQaToCkb(Ttl7490 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl7490.PORT_INDEX_CKB, state.getPortValue(Ttl7490.PORT_INDEX_QA));
    rise(gate, state, Ttl7490.PORT_INDEX_CKA);
    state.setPortValue(Ttl7490.PORT_INDEX_CKB, state.getPortValue(Ttl7490.PORT_INDEX_QA));
    fall(gate, state, Ttl7490.PORT_INDEX_CKA);
    state.setPortValue(Ttl7490.PORT_INDEX_CKB, state.getPortValue(Ttl7490.PORT_INDEX_QA));
    gate.propagate(state);
  }

  private static void cascadeQdToCka(Ttl7490 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl7490.PORT_INDEX_CKA, state.getPortValue(Ttl7490.PORT_INDEX_QD));
    rise(gate, state, Ttl7490.PORT_INDEX_CKB);
    state.setPortValue(Ttl7490.PORT_INDEX_CKA, state.getPortValue(Ttl7490.PORT_INDEX_QD));
    fall(gate, state, Ttl7490.PORT_INDEX_CKB);
    state.setPortValue(Ttl7490.PORT_INDEX_CKA, state.getPortValue(Ttl7490.PORT_INDEX_QD));
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
        (state.getPortValue(Ttl7490.PORT_INDEX_QA).toLongValue()
            | state.getPortValue(Ttl7490.PORT_INDEX_QB).toLongValue() << 1
            | state.getPortValue(Ttl7490.PORT_INDEX_QC).toLongValue() << 2
            | state.getPortValue(Ttl7490.PORT_INDEX_QD).toLongValue() << 3);
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7490.PORT_INDEX_QA));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7490.PORT_INDEX_QB));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7490.PORT_INDEX_QC));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7490.PORT_INDEX_QD));
  }
}
