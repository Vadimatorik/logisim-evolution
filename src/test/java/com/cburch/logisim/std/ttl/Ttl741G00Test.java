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

/** Functional tests for the 74HC1G00 single 2-input NAND gate. */
class Ttl741G00Test {
  private static final int GND_PORT = 3;
  private static final int VCC_PORT = 4;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl741G00();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(3, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl741G00.PORT_INDEX_B, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl741G00.PORT_INDEX_A, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl741G00.PORT_INDEX_Y, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(5, shownPower.getPorts().size());
    assertPort(shownPower, Ttl741G00.PORT_INDEX_B, 10, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, Ttl741G00.PORT_INDEX_A, 30, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, Ttl741G00.PORT_INDEX_Y, 30, -30, EndData.OUTPUT_ONLY);
    assertPort(shownPower, GND_PORT, 50, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void outputFollowsTheNandTruthTable() {
    assertNand(Value.FALSE, Value.FALSE, Value.TRUE);
    assertNand(Value.FALSE, Value.TRUE, Value.TRUE);
    assertNand(Value.TRUE, Value.FALSE, Value.TRUE);
    assertNand(Value.TRUE, Value.TRUE, Value.FALSE);
  }

  @Test
  void lowInputForcesHighWhenTheOtherInputIsUndefined() {
    assertNand(Value.FALSE, Value.UNKNOWN, Value.TRUE);
    assertNand(Value.UNKNOWN, Value.FALSE, Value.TRUE);
    assertNand(Value.FALSE, Value.ERROR, Value.TRUE);
    assertNand(Value.ERROR, Value.FALSE, Value.TRUE);
  }

  @Test
  void undefinedInputBecomesErrorWhenNeitherInputIsLow() {
    assertNand(Value.TRUE, Value.UNKNOWN, Value.ERROR);
    assertNand(Value.UNKNOWN, Value.TRUE, Value.ERROR);
    assertNand(Value.UNKNOWN, Value.UNKNOWN, Value.ERROR);
    assertNand(Value.TRUE, Value.ERROR, Value.ERROR);
    assertNand(Value.ERROR, Value.ERROR, Value.ERROR);
  }

  @Test
  void exposedPowerPinsMustBeValid() {
    final var gate = new Ttl741G00();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(Ttl741G00.PORT_INDEX_A, Value.TRUE);
    state.setPortValue(Ttl741G00.PORT_INDEX_B, Value.TRUE);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.FALSE, state.getPortValue(Ttl741G00.PORT_INDEX_Y));

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl741G00.PORT_INDEX_Y));

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.FALSE, state.getPortValue(Ttl741G00.PORT_INDEX_Y));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl741G00.PORT_INDEX_Y));
  }

  private static void assertNand(Value a, Value b, Value expected) {
    final var gate = new Ttl741G00();
    final var state = new TtlTestInstanceState(gate, false);
    state.setPortValue(Ttl741G00.PORT_INDEX_A, a);
    state.setPortValue(Ttl741G00.PORT_INDEX_B, b);
    gate.propagate(state);
    assertEquals(expected, state.getPortValue(Ttl741G00.PORT_INDEX_Y));
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }
}
