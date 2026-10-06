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

/** Functional tests for the 74x4072 dual 4-input OR gate. */
class Ttl744072Test {
  private static final int GND_PORT = 10;
  private static final int VCC_PORT = 11;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl744072();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(10, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744072.Y1, 10, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744072.A1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744072.B1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744072.C1, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744072.D1, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744072.A2, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744072.B2, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744072.C2, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744072.D2, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744072.Y2, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(12, shownPower.getPorts().size());
    assertEquals(Location.create(130, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void eachInputCombinationOrsWithinItsOwnGate() {
    final var gate = new Ttl744072();

    for (var mask = 0; mask < 16; mask++) {
      final var first = driven(gate, mask, 0);
      gate.propagate(first);
      assertEquals(orLevel(mask), first.getPortValue(Ttl744072.Y1), "gate 1 mask " + mask);
      assertEquals(Value.FALSE, first.getPortValue(Ttl744072.Y2), "gate 2 stays low");

      final var second = driven(gate, 0, mask);
      gate.propagate(second);
      assertEquals(Value.FALSE, second.getPortValue(Ttl744072.Y1), "gate 1 stays low");
      assertEquals(orLevel(mask), second.getPortValue(Ttl744072.Y2), "gate 2 mask " + mask);
    }
  }

  @Test
  void bothGatesComputeTogether() {
    final var gate = new Ttl744072();
    final var state = driven(gate, 0b0001, 0b1000);
    gate.propagate(state);

    assertEquals(Value.TRUE, state.getPortValue(Ttl744072.Y1));
    assertEquals(Value.TRUE, state.getPortValue(Ttl744072.Y2));
  }

  @Test
  void highInputOverridesUnknownAndErrorOnTheSameGate() {
    final var gate = new Ttl744072();
    final var unknown = driven(gate, 0, 0);
    unknown.setPortValue(Ttl744072.B1, Value.UNKNOWN);
    unknown.setPortValue(Ttl744072.C2, Value.UNKNOWN);
    gate.propagate(unknown);
    assertEquals(Value.ERROR, unknown.getPortValue(Ttl744072.Y1));
    assertEquals(Value.ERROR, unknown.getPortValue(Ttl744072.Y2));

    unknown.setPortValue(Ttl744072.A1, Value.TRUE);
    unknown.setPortValue(Ttl744072.A2, Value.TRUE);
    gate.propagate(unknown);
    assertEquals(Value.TRUE, unknown.getPortValue(Ttl744072.Y1));
    assertEquals(Value.TRUE, unknown.getPortValue(Ttl744072.Y2));

    final var error = driven(gate, 0, 0);
    error.setPortValue(Ttl744072.D1, Value.ERROR);
    error.setPortValue(Ttl744072.D2, Value.ERROR);
    gate.propagate(error);
    assertEquals(Value.ERROR, error.getPortValue(Ttl744072.Y1));
    assertEquals(Value.ERROR, error.getPortValue(Ttl744072.Y2));

    error.setPortValue(Ttl744072.A1, Value.TRUE);
    error.setPortValue(Ttl744072.B2, Value.TRUE);
    gate.propagate(error);
    assertEquals(Value.TRUE, error.getPortValue(Ttl744072.Y1));
    assertEquals(Value.TRUE, error.getPortValue(Ttl744072.Y2));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl744072();
    final var state = driven(gate, 0b0100, 0, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.TRUE, state.getPortValue(Ttl744072.Y1));
    assertEquals(Value.FALSE, state.getPortValue(Ttl744072.Y2));

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl744072.Y1));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl744072.Y2));

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.TRUE, state.getPortValue(Ttl744072.Y1));
    assertEquals(Value.FALSE, state.getPortValue(Ttl744072.Y2));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl744072.Y1));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl744072.Y2));
  }

  private static TtlTestInstanceState driven(Ttl744072 gate, int firstMask, int secondMask) {
    return driven(gate, firstMask, secondMask, false);
  }

  private static TtlTestInstanceState driven(
      Ttl744072 gate, int firstMask, int secondMask, boolean showPower) {
    final var state = new TtlTestInstanceState(gate, showPower);
    setNibble(state, Ttl744072.A1, firstMask);
    setNibble(state, Ttl744072.A2, secondMask);
    return state;
  }

  private static void setNibble(TtlTestInstanceState state, int firstPort, int mask) {
    for (var bit = 0; bit < 4; bit++) {
      final var level = ((mask >> bit) & 1) == 0 ? Value.FALSE : Value.TRUE;
      state.setPortValue(firstPort + bit, level);
    }
  }

  private static Value orLevel(int mask) {
    return mask == 0 ? Value.FALSE : Value.TRUE;
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }
}
