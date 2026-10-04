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

/** Functional tests for the 74HC1G14 single inverting Schmitt trigger. */
class Ttl741G14Test {
  private static final int GND_PORT = 2;
  private static final int VCC_PORT = 3;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl741G14();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(2, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl741G14.PORT_INDEX_A, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl741G14.PORT_INDEX_Y, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(4, shownPower.getPorts().size());
    assertPort(shownPower, Ttl741G14.PORT_INDEX_A, 30, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, Ttl741G14.PORT_INDEX_Y, 30, -30, EndData.OUTPUT_ONLY);
    assertPort(shownPower, GND_PORT, 50, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void outputIsTheComplementOfTheInput() {
    final var gate = new Ttl741G14();
    final var state = new TtlTestInstanceState(gate, false);

    state.setPortValue(Ttl741G14.PORT_INDEX_A, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.TRUE, state.getPortValue(Ttl741G14.PORT_INDEX_Y));

    state.setPortValue(Ttl741G14.PORT_INDEX_A, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.FALSE, state.getPortValue(Ttl741G14.PORT_INDEX_Y));
  }

  @Test
  void unknownAndErrorInputsBecomeError() {
    final var gate = new Ttl741G14();
    final var state = new TtlTestInstanceState(gate, false);

    state.setPortValue(Ttl741G14.PORT_INDEX_A, Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(Value.ERROR, state.getPortValue(Ttl741G14.PORT_INDEX_Y));

    state.setPortValue(Ttl741G14.PORT_INDEX_A, Value.ERROR);
    gate.propagate(state);
    assertEquals(Value.ERROR, state.getPortValue(Ttl741G14.PORT_INDEX_Y));
  }

  @Test
  void exposedPowerPinsMustBeValid() {
    final var gate = new Ttl741G14();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(Ttl741G14.PORT_INDEX_A, Value.FALSE);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.TRUE, state.getPortValue(Ttl741G14.PORT_INDEX_Y));

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl741G14.PORT_INDEX_Y));

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.TRUE, state.getPortValue(Ttl741G14.PORT_INDEX_Y));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl741G14.PORT_INDEX_Y));
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }
}
