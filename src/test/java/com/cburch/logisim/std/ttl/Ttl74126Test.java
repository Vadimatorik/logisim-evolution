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

/** Functional tests for the 74HC126 quad bus buffer with active-high three-state outputs. */
class Ttl74126Test {
  private static final int GND_PORT = 12;
  private static final int VCC_PORT = 13;
  private static final int OE = 0;
  private static final int A = 1;
  private static final int Y = 2;
  private static final int[][] CHANNELS = {
    {Ttl74126.PORT_INDEX_1OE, Ttl74126.PORT_INDEX_1A, Ttl74126.PORT_INDEX_1Y},
    {Ttl74126.PORT_INDEX_2OE, Ttl74126.PORT_INDEX_2A, Ttl74126.PORT_INDEX_2Y},
    {Ttl74126.PORT_INDEX_3OE, Ttl74126.PORT_INDEX_3A, Ttl74126.PORT_INDEX_3Y},
    {Ttl74126.PORT_INDEX_4OE, Ttl74126.PORT_INDEX_4A, Ttl74126.PORT_INDEX_4Y},
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74126();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74126.PORT_INDEX_1OE, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74126.PORT_INDEX_1A, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74126.PORT_INDEX_1Y, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74126.PORT_INDEX_2OE, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74126.PORT_INDEX_2A, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74126.PORT_INDEX_2Y, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74126.PORT_INDEX_3Y, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74126.PORT_INDEX_3A, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74126.PORT_INDEX_3OE, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74126.PORT_INDEX_4Y, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74126.PORT_INDEX_4A, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74126.PORT_INDEX_4OE, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertPort(shownPower, Ttl74126.PORT_INDEX_1OE, 10, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, Ttl74126.PORT_INDEX_4OE, 30, -30, EndData.INPUT_ONLY);
    assertPort(shownPower, GND_PORT, 130, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void enabledOutputFollowsTheInputOnEveryChannel() {
    final var gate = new Ttl74126();
    final var state = new TtlTestInstanceState(gate, false);

    for (final var channel : CHANNELS) {
      disableAll(state);
      state.setPortValue(channel[OE], Value.TRUE);
      state.setPortValue(channel[A], Value.FALSE);
      gate.propagate(state);
      assertEquals(Value.FALSE, state.getPortValue(channel[Y]));

      state.setPortValue(channel[A], Value.TRUE);
      gate.propagate(state);
      assertEquals(Value.TRUE, state.getPortValue(channel[Y]));

      state.setPortValue(channel[A], Value.UNKNOWN);
      gate.propagate(state);
      assertEquals(Value.UNKNOWN, state.getPortValue(channel[Y]));

      state.setPortValue(channel[A], Value.ERROR);
      gate.propagate(state);
      assertEquals(Value.ERROR, state.getPortValue(channel[Y]));
    }
  }

  @Test
  void disabledOutputStaysUnknownRegardlessOfTheInput() {
    final var gate = new Ttl74126();
    final var state = new TtlTestInstanceState(gate, false);

    for (final var input : new Value[] {Value.FALSE, Value.TRUE, Value.ERROR, Value.UNKNOWN}) {
      for (final var channel : CHANNELS) {
        disableAll(state);
        state.setPortValue(channel[OE], Value.FALSE);
        state.setPortValue(channel[A], input);
        gate.propagate(state);
        assertEquals(Value.UNKNOWN, state.getPortValue(channel[Y]));
      }
    }
  }

  @Test
  void uncertainEnableLeavesTheOutputUnknown() {
    final var gate = new Ttl74126();
    final var state = new TtlTestInstanceState(gate, false);

    for (final var enable : new Value[] {Value.UNKNOWN, Value.ERROR}) {
      for (final var channel : CHANNELS) {
        disableAll(state);
        state.setPortValue(channel[OE], enable);
        state.setPortValue(channel[A], Value.TRUE);
        gate.propagate(state);
        assertEquals(Value.UNKNOWN, state.getPortValue(channel[Y]));
      }
    }
  }

  @Test
  void channelsStayIndependent() {
    final var gate = new Ttl74126();
    final var state = new TtlTestInstanceState(gate, false);
    final var inputs = new Value[] {Value.TRUE, Value.FALSE, Value.TRUE, Value.FALSE};
    final var enables = new Value[] {Value.TRUE, Value.FALSE, Value.TRUE, Value.FALSE};

    for (var index = 0; index < CHANNELS.length; index++) {
      state.setPortValue(CHANNELS[index][OE], enables[index]);
      state.setPortValue(CHANNELS[index][A], inputs[index]);
    }
    gate.propagate(state);

    assertEquals(Value.TRUE, state.getPortValue(CHANNELS[0][Y]));
    assertEquals(Value.UNKNOWN, state.getPortValue(CHANNELS[1][Y]));
    assertEquals(Value.TRUE, state.getPortValue(CHANNELS[2][Y]));
    assertEquals(Value.UNKNOWN, state.getPortValue(CHANNELS[3][Y]));

    state.setPortValue(CHANNELS[1][A], Value.TRUE);
    state.setPortValue(CHANNELS[3][A], Value.ERROR);
    gate.propagate(state);
    assertEquals(Value.TRUE, state.getPortValue(CHANNELS[0][Y]));
    assertEquals(Value.UNKNOWN, state.getPortValue(CHANNELS[1][Y]));
    assertEquals(Value.TRUE, state.getPortValue(CHANNELS[2][Y]));
    assertEquals(Value.UNKNOWN, state.getPortValue(CHANNELS[3][Y]));
  }

  private static void disableAll(TtlTestInstanceState state) {
    for (final var channel : CHANNELS) {
      state.setPortValue(channel[OE], Value.FALSE);
      state.setPortValue(channel[A], Value.FALSE);
    }
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }
}
