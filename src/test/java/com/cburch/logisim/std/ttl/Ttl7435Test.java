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

/** Functional tests for the 7435 hex noninverting buffer with open-collector outputs. */
class Ttl7435Test {
  private static final byte[][] CHANNELS = {
    {Ttl7435.A1, Ttl7435.Y1},
    {Ttl7435.A2, Ttl7435.Y2},
    {Ttl7435.A3, Ttl7435.Y3},
    {Ttl7435.A4, Ttl7435.Y4},
    {Ttl7435.A5, Ttl7435.Y5},
    {Ttl7435.A6, Ttl7435.Y6}
  };

  private static final int GND_PORT = 12;
  private static final int VCC_PORT = 13;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var buffer = new Ttl7435();
    final var hiddenPower = createInstance(buffer, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl7435.A1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7435.Y1, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7435.A2, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7435.Y2, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7435.A3, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7435.Y3, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7435.Y4, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7435.A4, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7435.Y5, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7435.A5, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7435.Y6, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7435.A6, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(buffer, true);
    assertEquals(14, shownPower.getPorts().size());
    assertEquals(Location.create(130, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void eachBufferSinksALowInputAndReleasesAHighInput() {
    final var buffer = new Ttl7435();

    for (var pattern = 0; pattern < 64; pattern++) {
      final var state = new TtlTestInstanceState(buffer, false);
      for (var channel = 0; channel < CHANNELS.length; channel++) {
        setInput(state, CHANNELS[channel][0], (pattern & (1 << channel)) != 0);
      }
      buffer.propagate(state);

      for (var channel = 0; channel < CHANNELS.length; channel++) {
        final var released = (pattern & (1 << channel)) != 0;
        assertEquals(
            released ? Value.UNKNOWN : Value.FALSE,
            output(state, CHANNELS[channel][1]),
            "channel " + (channel + 1) + " pattern " + pattern);
      }
    }
  }

  @Test
  void unknownAndErrorInputsAreNotForcedLow() {
    final var buffer = new Ttl7435();
    final var state = new TtlTestInstanceState(buffer, false);
    state.setPortValue(Ttl7435.pinNrToPortNr(Ttl7435.A1), Value.UNKNOWN);
    state.setPortValue(Ttl7435.pinNrToPortNr(Ttl7435.A2), Value.ERROR);
    setInput(state, Ttl7435.A3, false);
    buffer.propagate(state);

    assertEquals(Value.UNKNOWN, output(state, Ttl7435.Y1));
    assertEquals(Value.ERROR, output(state, Ttl7435.Y2));
    assertEquals(Value.FALSE, output(state, Ttl7435.Y3));
    assertEquals(Value.UNKNOWN, output(state, Ttl7435.Y4));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var buffer = new Ttl7435();
    final var state = new TtlTestInstanceState(buffer, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    setInput(state, Ttl7435.A1, false);
    setInput(state, Ttl7435.A6, true);
    buffer.propagate(state);
    assertEquals(Value.FALSE, output(state, Ttl7435.Y1));
    assertEquals(Value.UNKNOWN, output(state, Ttl7435.Y6));

    state.setPortValue(VCC_PORT, Value.FALSE);
    buffer.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    buffer.propagate(state);
    assertEquals(Value.FALSE, output(state, Ttl7435.Y1));

    state.setPortValue(GND_PORT, Value.TRUE);
    buffer.propagate(state);
    assertUnknownOutputs(state);
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var channel : CHANNELS) {
      assertEquals(Value.UNKNOWN, output(state, channel[1]));
    }
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl7435.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setInput(TtlTestInstanceState state, byte dsPinNr, boolean high) {
    state.setPortValue(Ttl7435.pinNrToPortNr(dsPinNr), high ? Value.TRUE : Value.FALSE);
  }

  private static Value output(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl7435.pinNrToPortNr(dsPinNr));
  }
}
