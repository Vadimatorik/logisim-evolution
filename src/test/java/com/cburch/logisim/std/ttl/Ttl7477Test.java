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

/** Functional tests for the 74HC77 quad bistable transparent latch. */
class Ttl7477Test {
  private static final byte[] DATA = {Ttl7477.D1, Ttl7477.D2, Ttl7477.D3, Ttl7477.D4};
  private static final byte[] OUTPUT = {Ttl7477.Q1, Ttl7477.Q2, Ttl7477.Q3, Ttl7477.Q4};
  private static final int GND_PORT = 10;
  private static final int VCC_PORT = 11;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl7477();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(10, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl7477.D1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7477.D2, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7477.LE34, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7477.D3, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7477.D4, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7477.Q4, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7477.Q3, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7477.LE12, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7477.Q2, 30, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7477.Q1, 10, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(12, shownPower.getPorts().size());
    assertEquals(Location.create(70, -30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(70, 30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void latchEnableIsNotAnEdgeClock() {
    assertFalse(new Ttl7477().checkForGatedClocks(null));
  }

  @Test
  void transparentLatchesFollowEveryDataPattern() {
    final var gate = new Ttl7477();
    final var state = new TtlTestInstanceState(gate, false);

    for (var pattern = 0; pattern < 16; pattern++) {
      drive(state, pattern, Value.TRUE, Value.TRUE);
      gate.propagate(state);
      assertPattern(state, pattern);
    }
  }

  @Test
  void highToLowEnableCapturesTheLastData() {
    final var gate = new Ttl7477();
    final var state = new TtlTestInstanceState(gate, false);
    drive(state, 0b1010, Value.TRUE, Value.TRUE);
    gate.propagate(state);

    drive(state, 0b0101, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertPattern(state, 0b1010);
  }

  @Test
  void pairsAreIndependent() {
    final var gate = new Ttl7477();
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
  void unknownOrErrorDataWhileTransparentIsPublishedAndHeld() {
    final var gate = new Ttl7477();
    final var state = new TtlTestInstanceState(gate, false);
    drive(state, 0b0000, Value.TRUE, Value.TRUE);
    gate.propagate(state);

    set(state, Ttl7477.D1, Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, level(state, Ttl7477.Q1));

    set(state, Ttl7477.LE12, Value.FALSE);
    set(state, Ttl7477.D1, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, level(state, Ttl7477.Q1));
    assertEquals(Value.FALSE, level(state, Ttl7477.Q2));

    set(state, Ttl7477.LE12, Value.TRUE);
    set(state, Ttl7477.D1, Value.ERROR);
    gate.propagate(state);
    assertEquals(Value.ERROR, level(state, Ttl7477.Q1));

    set(state, Ttl7477.LE12, Value.FALSE);
    set(state, Ttl7477.D1, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.ERROR, level(state, Ttl7477.Q1));
  }

  @Test
  void undefinedEnablePublishesTheConflictAndKeepsTheStoredBit() {
    final var gate = new Ttl7477();
    for (final var enable : new Value[] {Value.UNKNOWN, Value.ERROR}) {
      final var state = new TtlTestInstanceState(gate, false);
      drive(state, 0b0001, Value.TRUE, Value.TRUE);
      gate.propagate(state);

      drive(state, 0b0000, enable, Value.FALSE);
      gate.propagate(state);
      assertEquals(Value.ERROR, level(state, Ttl7477.Q1));
      assertEquals(Value.FALSE, level(state, Ttl7477.Q2));
      assertEquals(Value.FALSE, level(state, Ttl7477.Q3));
      assertEquals(Value.FALSE, level(state, Ttl7477.Q4));

      drive(state, 0b0000, Value.FALSE, Value.FALSE);
      gate.propagate(state);
      assertPattern(state, 0b0001);
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl7477();
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
    set(state, Ttl7477.LE12, le12);
    set(state, Ttl7477.LE34, le34);
  }

  private static void assertPattern(TtlTestInstanceState state, int pattern) {
    for (var index = 0; index < OUTPUT.length; index++) {
      assertEquals(bit(pattern, index), level(state, OUTPUT[index]));
    }
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var pin : OUTPUT) {
      assertEquals(Value.UNKNOWN, level(state, pin));
    }
  }

  private static Value bit(int pattern, int index) {
    return ((pattern >> index) & 1) == 1 ? Value.TRUE : Value.FALSE;
  }

  private static Value level(TtlTestInstanceState state, byte pin) {
    return state.getPortValue(Ttl7477.pinNrToPortNr(pin));
  }

  private static void set(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl7477.pinNrToPortNr(pin), value);
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl7477.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
