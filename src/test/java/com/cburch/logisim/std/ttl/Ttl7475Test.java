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
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC75 quad bistable transparent latch. */
class Ttl7475Test {
  private static final byte[] DATA = {Ttl7475.D1, Ttl7475.D2, Ttl7475.D3, Ttl7475.D4};
  private static final byte[] OUTPUT = {Ttl7475.Q1, Ttl7475.Q2, Ttl7475.Q3, Ttl7475.Q4};
  private static final byte[] COMPLEMENT = {Ttl7475.Q1N, Ttl7475.Q2N, Ttl7475.Q3N, Ttl7475.Q4N};
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl7475();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl7475.Q1N, 10, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7475.D1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7475.D2, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7475.LE34, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7475.D3, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7475.D4, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7475.Q4N, 150, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7475.Q4, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7475.Q3, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7475.Q3N, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7475.LE12, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7475.Q2N, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7475.Q2, 30, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7475.Q1, 10, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(90, -30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(90, 30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void latchEnableIsNotAnEdgeClock() {
    assertFalse(new Ttl7475().checkForGatedClocks(null));
  }

  @Test
  void transparentLatchesFollowEveryDataPattern() {
    final var gate = new Ttl7475();
    final var state = new TtlTestInstanceState(gate, false);

    for (var pattern = 0; pattern < 16; pattern++) {
      drive(state, pattern, Value.TRUE, Value.TRUE);
      gate.propagate(state);
      assertPattern(state, pattern);
    }
  }

  @Test
  void highToLowEnableCapturesTheLastData() {
    final var gate = new Ttl7475();
    final var state = new TtlTestInstanceState(gate, false);
    drive(state, 0b1010, Value.TRUE, Value.TRUE);
    gate.propagate(state);

    drive(state, 0b0101, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertPattern(state, 0b1010);
  }

  @Test
  void pairsAreIndependent() {
    final var gate = new Ttl7475();
    final var state = new TtlTestInstanceState(gate, false);
    drive(state, 0b1111, Value.TRUE, Value.TRUE);
    gate.propagate(state);

    drive(state, 0b0000, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertPattern(state, 0b0011);

    drive(state, 0b1111, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    assertPattern(state, 0b0011);

    drive(state, 0b0000, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    assertPattern(state, 0b0000);

    drive(state, 0b1111, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertPattern(state, 0b1100);
  }

  @Test
  void unknownDataWhileTransparentStaysUnknownOnBothOutputs() {
    final var gate = new Ttl7475();
    final var state = new TtlTestInstanceState(gate, false);
    drive(state, 0b0000, Value.TRUE, Value.TRUE);
    gate.propagate(state);

    set(state, Ttl7475.D1, Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, level(state, Ttl7475.Q1));
    assertEquals(Value.UNKNOWN, level(state, Ttl7475.Q1N));

    set(state, Ttl7475.LE12, Value.FALSE);
    set(state, Ttl7475.D1, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, level(state, Ttl7475.Q1));
    assertEquals(Value.UNKNOWN, level(state, Ttl7475.Q1N));
    assertEquals(Value.FALSE, level(state, Ttl7475.Q2));
  }

  @Test
  void undefinedEnablePublishesTheConflictAndKeepsTheStoredBit() {
    final var gate = new Ttl7475();
    final var state = new TtlTestInstanceState(gate, false);
    drive(state, 0b0001, Value.TRUE, Value.TRUE);
    gate.propagate(state);

    drive(state, 0b0000, Value.UNKNOWN, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.ERROR, level(state, Ttl7475.Q1));
    assertEquals(Value.ERROR, level(state, Ttl7475.Q1N));
    assertEquals(Value.FALSE, level(state, Ttl7475.Q2));
    assertEquals(Value.FALSE, level(state, Ttl7475.Q3));
    assertEquals(Value.FALSE, level(state, Ttl7475.Q4));

    drive(state, 0b0000, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertPattern(state, 0b0001);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl7475();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    drive(state, 0b1010, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertPattern(state, 0b1010);

    drive(state, 0b0101, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertPattern(state, 0b1010);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertPattern(state, 0b1010);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(GND_PORT, Value.FALSE);
    gate.propagate(state);
    assertPattern(state, 0b1010);
  }

  private static void drive(TtlTestInstanceState state, int pattern, Value le12, Value le34) {
    for (var index = 0; index < DATA.length; index++) {
      set(state, DATA[index], bit(pattern, index));
    }
    set(state, Ttl7475.LE12, le12);
    set(state, Ttl7475.LE34, le34);
  }

  private static void assertPattern(TtlTestInstanceState state, int pattern) {
    for (var index = 0; index < OUTPUT.length; index++) {
      final var expected = bit(pattern, index);
      assertEquals(expected, level(state, OUTPUT[index]));
      assertEquals(inverted(expected), level(state, COMPLEMENT[index]));
    }
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var pin : OUTPUT) {
      assertEquals(Value.UNKNOWN, level(state, pin));
    }
    for (final var pin : COMPLEMENT) {
      assertEquals(Value.UNKNOWN, level(state, pin));
    }
  }

  private static Value bit(int pattern, int index) {
    return ((pattern >> index) & 1) == 1 ? Value.TRUE : Value.FALSE;
  }

  private static Value inverted(Value value) {
    return value == Value.TRUE ? Value.FALSE : Value.TRUE;
  }

  private static Value level(TtlTestInstanceState state, byte pin) {
    return state.getPortValue(Ttl7475.pinNrToPortNr(pin));
  }

  private static void set(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl7475.pinNrToPortNr(pin), value);
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl7475.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
