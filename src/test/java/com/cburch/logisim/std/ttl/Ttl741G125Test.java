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

/** Functional tests for the 74HC1G125 single bus buffer with a 3-state output. */
class Ttl741G125Test {
  private static final int GND_PORT = 3;
  private static final int VCC_PORT = 4;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl741G125();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(3, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl741G125.PORT_INDEX_OE, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl741G125.PORT_INDEX_A, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl741G125.PORT_INDEX_Y, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(5, shownPower.getPorts().size());
    assertPort(shownPower, Ttl741G125.PORT_INDEX_OE, 10, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, Ttl741G125.PORT_INDEX_A, 30, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, Ttl741G125.PORT_INDEX_Y, 30, -30, EndData.OUTPUT_ONLY);
    assertPort(shownPower, GND_PORT, 50, -30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void outputFollowsTheBufferFunctionTable() {
    assertBuffer(Value.FALSE, Value.FALSE, Value.FALSE);
    assertBuffer(Value.FALSE, Value.TRUE, Value.TRUE);
    assertBuffer(Value.TRUE, Value.FALSE, Value.UNKNOWN);
    assertBuffer(Value.TRUE, Value.TRUE, Value.UNKNOWN);
  }

  @Test
  void enabledOutputPassesUnknownAndError() {
    assertBuffer(Value.FALSE, Value.UNKNOWN, Value.UNKNOWN);
    assertBuffer(Value.FALSE, Value.ERROR, Value.ERROR);
  }

  @Test
  void undefinedEnableChangesTheOutputOnlyWhenTheResultsDisagree() {
    assertBuffer(Value.UNKNOWN, Value.FALSE, Value.UNKNOWN);
    assertBuffer(Value.UNKNOWN, Value.TRUE, Value.UNKNOWN);
    assertBuffer(Value.UNKNOWN, Value.UNKNOWN, Value.UNKNOWN);
    assertBuffer(Value.ERROR, Value.FALSE, Value.ERROR);
    assertBuffer(Value.ERROR, Value.TRUE, Value.ERROR);
    assertBuffer(Value.ERROR, Value.UNKNOWN, Value.UNKNOWN);
    assertBuffer(Value.ERROR, Value.ERROR, Value.ERROR);
    assertBuffer(Value.TRUE, Value.ERROR, Value.UNKNOWN);
  }

  @Test
  void exposedPowerPinsMustBeValid() {
    final var gate = new Ttl741G125();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(Ttl741G125.PORT_INDEX_OE, Value.FALSE);
    state.setPortValue(Ttl741G125.PORT_INDEX_A, Value.TRUE);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.TRUE, state.getPortValue(Ttl741G125.PORT_INDEX_Y));

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl741G125.PORT_INDEX_Y));

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.TRUE, state.getPortValue(Ttl741G125.PORT_INDEX_Y));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl741G125.PORT_INDEX_Y));
  }

  @Test
  void threeStateOutputIsNotAnHdlTarget() {
    final var gate = new Ttl741G125();
    final var attrs = gate.createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertFalse(gate.isHDLSupportedComponent(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(gate.isHDLSupportedComponent(attrs));
  }

  private static void assertBuffer(Value outputEnable, Value data, Value expected) {
    final var gate = new Ttl741G125();
    final var state = new TtlTestInstanceState(gate, false);
    state.setPortValue(Ttl741G125.PORT_INDEX_OE, outputEnable);
    state.setPortValue(Ttl741G125.PORT_INDEX_A, data);
    gate.propagate(state);
    assertEquals(expected, state.getPortValue(Ttl741G125.PORT_INDEX_Y));
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }
}
