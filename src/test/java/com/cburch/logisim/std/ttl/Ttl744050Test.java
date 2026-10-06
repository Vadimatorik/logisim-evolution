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

/** Functional tests for the 744050 hex non-inverting buffer. */
class Ttl744050Test {
  private static final int[] INPUTS = {
    Ttl744050.PORT_INDEX_A1,
    Ttl744050.PORT_INDEX_A2,
    Ttl744050.PORT_INDEX_A3,
    Ttl744050.PORT_INDEX_A4,
    Ttl744050.PORT_INDEX_A5,
    Ttl744050.PORT_INDEX_A6
  };
  private static final int[] OUTPUTS = {
    Ttl744050.PORT_INDEX_Y1,
    Ttl744050.PORT_INDEX_Y2,
    Ttl744050.PORT_INDEX_Y3,
    Ttl744050.PORT_INDEX_Y4,
    Ttl744050.PORT_INDEX_Y5,
    Ttl744050.PORT_INDEX_Y6
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var buffer = new Ttl744050();
    final var hiddenPower = createInstance(buffer, false);

    assertEquals(Ttl744050.SIGNAL_PORT_COUNT, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744050.PORT_INDEX_Y1, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744050.PORT_INDEX_A1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744050.PORT_INDEX_Y2, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744050.PORT_INDEX_A2, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744050.PORT_INDEX_Y3, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744050.PORT_INDEX_A3, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744050.PORT_INDEX_A4, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744050.PORT_INDEX_Y4, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744050.PORT_INDEX_A5, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744050.PORT_INDEX_Y5, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744050.PORT_INDEX_A6, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744050.PORT_INDEX_Y6, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(buffer, true);
    assertEquals(Ttl744050.SIGNAL_PORT_COUNT + 2, shownPower.getPorts().size());
    assertPort(shownPower, Ttl744050.PORT_INDEX_GND, 150, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, Ttl744050.PORT_INDEX_VCC, 10, 30, EndData.INPUT_ONLY);
  }

  @Test
  void everyInputCombinationIsCopiedToTheMatchingOutput() {
    final var buffer = new Ttl744050();

    for (var pattern = 0; pattern < 64; pattern++) {
      final var state = new TtlTestInstanceState(buffer, false);
      for (var channel = 0; channel < INPUTS.length; channel++) {
        state.setPortValue(INPUTS[channel], bit(pattern, channel));
      }
      buffer.propagate(state);

      for (var channel = 0; channel < OUTPUTS.length; channel++) {
        assertEquals(bit(pattern, channel), state.getPortValue(OUTPUTS[channel]));
      }
    }
  }

  @Test
  void unknownAndErrorInputsStayOnTheirOwnChannel() {
    final var buffer = new Ttl744050();
    final var state = new TtlTestInstanceState(buffer, false);
    for (final var input : INPUTS) {
      state.setPortValue(input, Value.FALSE);
    }
    state.setPortValue(Ttl744050.PORT_INDEX_A1, Value.UNKNOWN);
    state.setPortValue(Ttl744050.PORT_INDEX_A4, Value.ERROR);
    state.setPortValue(Ttl744050.PORT_INDEX_A6, Value.TRUE);
    buffer.propagate(state);

    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl744050.PORT_INDEX_Y1));
    assertEquals(Value.FALSE, state.getPortValue(Ttl744050.PORT_INDEX_Y2));
    assertEquals(Value.FALSE, state.getPortValue(Ttl744050.PORT_INDEX_Y3));
    assertEquals(Value.ERROR, state.getPortValue(Ttl744050.PORT_INDEX_Y4));
    assertEquals(Value.FALSE, state.getPortValue(Ttl744050.PORT_INDEX_Y5));
    assertEquals(Value.TRUE, state.getPortValue(Ttl744050.PORT_INDEX_Y6));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var buffer = new Ttl744050();
    final var state = new TtlTestInstanceState(buffer, true);
    state.setPortValue(Ttl744050.PORT_INDEX_GND, Value.FALSE);
    state.setPortValue(Ttl744050.PORT_INDEX_VCC, Value.TRUE);
    state.setPortValue(Ttl744050.PORT_INDEX_A2, Value.TRUE);
    buffer.propagate(state);
    assertEquals(Value.TRUE, state.getPortValue(Ttl744050.PORT_INDEX_Y2));

    state.setPortValue(Ttl744050.PORT_INDEX_VCC, Value.FALSE);
    buffer.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(Ttl744050.PORT_INDEX_VCC, Value.TRUE);
    buffer.propagate(state);
    assertEquals(Value.TRUE, state.getPortValue(Ttl744050.PORT_INDEX_Y2));

    state.setPortValue(Ttl744050.PORT_INDEX_GND, Value.TRUE);
    buffer.propagate(state);
    assertUnknownOutputs(state);
  }

  private static Value bit(int pattern, int channel) {
    return ((pattern >> channel) & 1) == 1 ? Value.TRUE : Value.FALSE;
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var output : OUTPUTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(output));
    }
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }
}
