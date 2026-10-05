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

class Ttl74197Test {
  private static final BitWidth WIDTH = BitWidth.create(4);
  private static final int GND_PORT = 12;
  private static final int VCC_PORT = 13;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74197();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74197.PORT_INDEX_PL, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74197.PORT_INDEX_Q2, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74197.PORT_INDEX_P2, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74197.PORT_INDEX_P0, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74197.PORT_INDEX_Q0, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74197.PORT_INDEX_CP1, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74197.PORT_INDEX_CP0, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74197.PORT_INDEX_Q1, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74197.PORT_INDEX_P1, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74197.PORT_INDEX_P3, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74197.PORT_INDEX_Q3, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74197.PORT_INDEX_MR, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 130, 30, EndData.INPUT_ONLY); // Physical pin 7: GND
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY); // Physical pin 14: VCC
  }

  @Test
  void masterResetClearsAndOverridesLoadAndClocks() {
    final var gate = new Ttl74197();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    fallAfterRise(gate, state, Ttl74197.PORT_INDEX_CP0);
    assertEquals(1, outputValue(state));

    setData(state, 0xA);
    state.setPortValue(Ttl74197.PORT_INDEX_PL, Value.FALSE);
    state.setPortValue(Ttl74197.PORT_INDEX_MR, Value.FALSE);
    gate.propagate(state);
    assertEquals(0, outputValue(state));

    fallAfterRise(gate, state, Ttl74197.PORT_INDEX_CP0);
    fallAfterRise(gate, state, Ttl74197.PORT_INDEX_CP1);
    assertEquals(0, outputValue(state));

    state.setPortValue(Ttl74197.PORT_INDEX_MR, Value.TRUE);
    gate.propagate(state);
    assertEquals(0xA, outputValue(state));
  }

  @Test
  void parallelLoadIsTransparentAndThenHolds() {
    final var gate = new Ttl74197();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    state.setPortValue(Ttl74197.PORT_INDEX_PL, Value.FALSE);
    for (var code = 0; code <= 15; code++) {
      setData(state, code);
      gate.propagate(state);
      assertEquals(code, outputValue(state));
    }

    state.setPortValue(Ttl74197.PORT_INDEX_PL, Value.TRUE);
    setData(state, 0);
    gate.propagate(state);
    assertEquals(15, outputValue(state));
  }

  @Test
  void cp0FallingEdgeTogglesOnlyQ0() {
    final var gate = new Ttl74197();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    rise(gate, state, Ttl74197.PORT_INDEX_CP0);
    assertEquals(0, outputValue(state));

    fall(gate, state, Ttl74197.PORT_INDEX_CP0);
    assertEquals(1, outputValue(state));

    fallAfterRise(gate, state, Ttl74197.PORT_INDEX_CP0);
    assertEquals(0, outputValue(state));

    fallAfterRise(gate, state, Ttl74197.PORT_INDEX_CP1);
    assertEquals(2, outputValue(state));
  }

  @Test
  void cp1FallingEdgeCountsTheDivideByEightSection() {
    final var gate = new Ttl74197();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    fallAfterRise(gate, state, Ttl74197.PORT_INDEX_CP0);
    assertEquals(1, outputValue(state));

    final var expected = new int[] {3, 5, 7, 9, 11, 13, 15, 1};
    for (var code : expected) {
      fallAfterRise(gate, state, Ttl74197.PORT_INDEX_CP1);
      assertEquals(code, outputValue(state));
    }
  }

  @Test
  void q0ToCp1CascadeCountsFromZeroThroughFifteen() {
    final var gate = new Ttl74197();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    for (var expected = 1; expected <= 16; expected++) {
      cascadeQ0ToCp1(gate, state);
      assertEquals(expected & 0xF, outputValue(state));
    }
  }

  @Test
  void fallingEdgeDuringResetOrLoadIsConsumed() {
    final var gate = new Ttl74197();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    fallAfterRise(gate, state, Ttl74197.PORT_INDEX_CP0);
    assertEquals(1, outputValue(state));

    state.setPortValue(Ttl74197.PORT_INDEX_MR, Value.FALSE);
    gate.propagate(state);
    fallAfterRise(gate, state, Ttl74197.PORT_INDEX_CP0);
    assertEquals(0, outputValue(state));

    state.setPortValue(Ttl74197.PORT_INDEX_MR, Value.TRUE);
    gate.propagate(state);
    assertEquals(0, outputValue(state));

    setData(state, 5);
    state.setPortValue(Ttl74197.PORT_INDEX_PL, Value.FALSE);
    gate.propagate(state);
    fallAfterRise(gate, state, Ttl74197.PORT_INDEX_CP0);
    assertEquals(5, outputValue(state));

    state.setPortValue(Ttl74197.PORT_INDEX_PL, Value.TRUE);
    gate.propagate(state);
    fall(gate, state, Ttl74197.PORT_INDEX_CP0);
    assertEquals(5, outputValue(state));

    fallAfterRise(gate, state, Ttl74197.PORT_INDEX_CP0);
    assertEquals(4, outputValue(state));
  }

  @Test
  void unknownResetLoadAndClocksDoNotForceAChange() {
    final var gate = new Ttl74197();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    fallAfterRise(gate, state, Ttl74197.PORT_INDEX_CP0);
    assertEquals(1, outputValue(state));

    state.setPortValue(Ttl74197.PORT_INDEX_MR, Value.UNKNOWN);
    setData(state, 7);
    gate.propagate(state);
    assertEquals(1, outputValue(state));

    state.setPortValue(Ttl74197.PORT_INDEX_MR, Value.TRUE);
    state.setPortValue(Ttl74197.PORT_INDEX_PL, Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(1, outputValue(state));

    state.setPortValue(Ttl74197.PORT_INDEX_CP0, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74197.PORT_INDEX_CP0, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl74197.PORT_INDEX_CP0, Value.FALSE);
    gate.propagate(state);
    assertEquals(1, outputValue(state));

    state.setPortValue(Ttl74197.PORT_INDEX_CP1, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl74197.PORT_INDEX_CP1, Value.FALSE);
    gate.propagate(state);
    assertEquals(1, outputValue(state));
  }

  @Test
  void loadCopiesUnknownAndErrorDataBits() {
    final var gate = new Ttl74197();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    state.setPortValue(Ttl74197.PORT_INDEX_PL, Value.FALSE);
    state.setPortValue(Ttl74197.PORT_INDEX_P0, Value.UNKNOWN);
    state.setPortValue(Ttl74197.PORT_INDEX_P1, Value.TRUE);
    state.setPortValue(Ttl74197.PORT_INDEX_P2, Value.ERROR);
    state.setPortValue(Ttl74197.PORT_INDEX_P3, Value.FALSE);
    gate.propagate(state);

    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74197.PORT_INDEX_Q0));
    assertEquals(Value.TRUE, state.getPortValue(Ttl74197.PORT_INDEX_Q1));
    assertEquals(Value.ERROR, state.getPortValue(Ttl74197.PORT_INDEX_Q2));
    assertEquals(Value.FALSE, state.getPortValue(Ttl74197.PORT_INDEX_Q3));
  }

  @Test
  void undefinedAndErrorBitsStayInTheirSection() {
    final var gate = new Ttl74197();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);
    final var unknown = Value.createKnown(WIDTH, 1).getAll();
    unknown[1] = Value.UNKNOWN;
    forceBits(state, unknown);

    fallAfterRise(gate, state, Ttl74197.PORT_INDEX_CP0);
    assertEquals(Value.FALSE, state.getPortValue(Ttl74197.PORT_INDEX_Q0));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74197.PORT_INDEX_Q1));
    assertEquals(Value.FALSE, state.getPortValue(Ttl74197.PORT_INDEX_Q2));
    assertEquals(Value.FALSE, state.getPortValue(Ttl74197.PORT_INDEX_Q3));

    fallAfterRise(gate, state, Ttl74197.PORT_INDEX_CP1);
    assertEquals(Value.FALSE, state.getPortValue(Ttl74197.PORT_INDEX_Q0));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74197.PORT_INDEX_Q1));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74197.PORT_INDEX_Q2));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74197.PORT_INDEX_Q3));

    final var error = Value.createKnown(WIDTH, 1).getAll();
    error[2] = Value.ERROR;
    forceBits(state, error);
    fallAfterRise(gate, state, Ttl74197.PORT_INDEX_CP1);
    assertEquals(Value.TRUE, state.getPortValue(Ttl74197.PORT_INDEX_Q0));
    assertEquals(Value.ERROR, state.getPortValue(Ttl74197.PORT_INDEX_Q1));
    assertEquals(Value.ERROR, state.getPortValue(Ttl74197.PORT_INDEX_Q2));
    assertEquals(Value.ERROR, state.getPortValue(Ttl74197.PORT_INDEX_Q3));

    fallAfterRise(gate, state, Ttl74197.PORT_INDEX_CP0);
    assertEquals(Value.FALSE, state.getPortValue(Ttl74197.PORT_INDEX_Q0));
    assertEquals(Value.ERROR, state.getPortValue(Ttl74197.PORT_INDEX_Q1));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74197();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    reset(gate, state);
    fallAfterRise(gate, state, Ttl74197.PORT_INDEX_CP0);
    assertEquals(1, outputValue(state));

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(1, outputValue(state));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void reset(Ttl74197 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl74197.PORT_INDEX_CP0, Value.FALSE);
    state.setPortValue(Ttl74197.PORT_INDEX_CP1, Value.FALSE);
    state.setPortValue(Ttl74197.PORT_INDEX_PL, Value.TRUE);
    state.setPortValue(Ttl74197.PORT_INDEX_MR, Value.FALSE);
    setData(state, 0);
    gate.propagate(state);
    state.setPortValue(Ttl74197.PORT_INDEX_MR, Value.TRUE);
    gate.propagate(state);
  }

  private static void setData(TtlTestInstanceState state, int code) {
    state.setPortValue(Ttl74197.PORT_INDEX_P0, bit(code, 0));
    state.setPortValue(Ttl74197.PORT_INDEX_P1, bit(code, 1));
    state.setPortValue(Ttl74197.PORT_INDEX_P2, bit(code, 2));
    state.setPortValue(Ttl74197.PORT_INDEX_P3, bit(code, 3));
  }

  private static Value bit(int code, int shift) {
    return ((code >> shift) & 1) == 1 ? Value.TRUE : Value.FALSE;
  }

  private static void rise(Ttl74197 gate, TtlTestInstanceState state, int clockPort) {
    state.setPortValue(clockPort, Value.TRUE);
    gate.propagate(state);
  }

  private static void fall(Ttl74197 gate, TtlTestInstanceState state, int clockPort) {
    state.setPortValue(clockPort, Value.FALSE);
    gate.propagate(state);
  }

  private static void fallAfterRise(Ttl74197 gate, TtlTestInstanceState state, int clockPort) {
    rise(gate, state, clockPort);
    fall(gate, state, clockPort);
  }

  private static void cascadeQ0ToCp1(Ttl74197 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl74197.PORT_INDEX_CP1, state.getPortValue(Ttl74197.PORT_INDEX_Q0));
    rise(gate, state, Ttl74197.PORT_INDEX_CP0);
    state.setPortValue(Ttl74197.PORT_INDEX_CP1, state.getPortValue(Ttl74197.PORT_INDEX_Q0));
    fall(gate, state, Ttl74197.PORT_INDEX_CP0);
    state.setPortValue(Ttl74197.PORT_INDEX_CP1, state.getPortValue(Ttl74197.PORT_INDEX_Q0));
    gate.propagate(state);
  }

  private static void forceBits(TtlTestInstanceState state, Value[] bits) {
    final var data = new TtlRegisterData(WIDTH);
    data.setValue(Value.create(bits));
    state.setData(data);
  }

  private static int outputValue(TtlTestInstanceState state) {
    return (int)
        (state.getPortValue(Ttl74197.PORT_INDEX_Q0).toLongValue()
            | state.getPortValue(Ttl74197.PORT_INDEX_Q1).toLongValue() << 1
            | state.getPortValue(Ttl74197.PORT_INDEX_Q2).toLongValue() << 2
            | state.getPortValue(Ttl74197.PORT_INDEX_Q3).toLongValue() << 3);
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74197.PORT_INDEX_Q0));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74197.PORT_INDEX_Q1));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74197.PORT_INDEX_Q2));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74197.PORT_INDEX_Q3));
  }
}
