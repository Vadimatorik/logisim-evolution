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

/** Functional tests for the 7416 hex open-collector inverter. */
class Ttl7416Test {
  private static final int[] INPUTS = {
    Ttl7416.PORT_1A,
    Ttl7416.PORT_2A,
    Ttl7416.PORT_3A,
    Ttl7416.PORT_4A,
    Ttl7416.PORT_5A,
    Ttl7416.PORT_6A
  };

  private static final int[] OUTPUTS = {
    Ttl7416.PORT_1Y,
    Ttl7416.PORT_2Y,
    Ttl7416.PORT_3Y,
    Ttl7416.PORT_4Y,
    Ttl7416.PORT_5Y,
    Ttl7416.PORT_6Y
  };

  @Test
  void usesPhysicalPinoutAndKeepsPowerPortsLast() {
    final var gate = new Ttl7416();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl7416.PORT_1A, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7416.PORT_1Y, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7416.PORT_2A, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7416.PORT_2Y, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7416.PORT_3A, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7416.PORT_3Y, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7416.PORT_4Y, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7416.PORT_4A, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7416.PORT_5Y, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7416.PORT_5A, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7416.PORT_6Y, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7416.PORT_6A, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertPort(shownPower, 12, 130, 30, EndData.INPUT_ONLY); // Physical pin 7: GND
    assertPort(shownPower, 13, 10, -30, EndData.INPUT_ONLY); // Physical pin 14: VCC
  }

  @Test
  void everyInputCombinationSinksOnHighAndReleasesOnLow() {
    final var gate = new Ttl7416();
    final var state = new TtlTestInstanceState(gate, false);

    for (var mask = 0; mask < 64; mask++) {
      for (var channel = 0; channel < INPUTS.length; channel++) {
        final var high = (mask & (1 << channel)) != 0;
        state.setPortValue(INPUTS[channel], high ? Value.TRUE : Value.FALSE);
      }
      gate.propagate(state);
      for (var channel = 0; channel < OUTPUTS.length; channel++) {
        final var high = (mask & (1 << channel)) != 0;
        assertEquals(high ? Value.FALSE : Value.UNKNOWN, state.getPortValue(OUTPUTS[channel]));
      }
    }
  }

  @Test
  void unknownInputReleasesAndErrorInputPropagates() {
    final var gate = new Ttl7416();
    final var state = new TtlTestInstanceState(gate, false);
    drive(state, Value.TRUE);
    state.setPortValue(Ttl7416.PORT_1A, Value.UNKNOWN);
    state.setPortValue(Ttl7416.PORT_4A, Value.ERROR);
    gate.propagate(state);

    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7416.PORT_1Y));
    assertEquals(Value.ERROR, state.getPortValue(Ttl7416.PORT_4Y));
    assertEquals(Value.FALSE, state.getPortValue(Ttl7416.PORT_2Y));
    assertEquals(Value.FALSE, state.getPortValue(Ttl7416.PORT_3Y));
    assertEquals(Value.FALSE, state.getPortValue(Ttl7416.PORT_5Y));
    assertEquals(Value.FALSE, state.getPortValue(Ttl7416.PORT_6Y));
  }

  @Test
  void invalidExposedPowerInputsReleaseEveryOutput() {
    final var gate = new Ttl7416();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(12, Value.FALSE);
    state.setPortValue(13, Value.TRUE);
    drive(state, Value.TRUE);
    gate.propagate(state);
    assertAll(state, Value.FALSE);

    state.setPortValue(13, Value.FALSE);
    gate.propagate(state);
    assertAll(state, Value.UNKNOWN);

    state.setPortValue(13, Value.TRUE);
    gate.propagate(state);
    assertAll(state, Value.FALSE);

    state.setPortValue(12, Value.TRUE);
    gate.propagate(state);
    assertAll(state, Value.UNKNOWN);
  }

  private static void drive(TtlTestInstanceState state, Value value) {
    for (final var port : INPUTS) {
      state.setPortValue(port, value);
    }
  }

  private static void assertAll(TtlTestInstanceState state, Value expected) {
    for (final var port : OUTPUTS) {
      assertEquals(expected, state.getPortValue(port));
    }
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }
}
