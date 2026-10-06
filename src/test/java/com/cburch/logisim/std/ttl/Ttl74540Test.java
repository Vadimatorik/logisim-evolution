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

/** Functional tests for the 74HC540 octal inverting buffer with three-state outputs. */
class Ttl74540Test {
  private static final byte[] INPUTS = {
    Ttl74540.A1, Ttl74540.A2, Ttl74540.A3, Ttl74540.A4,
    Ttl74540.A5, Ttl74540.A6, Ttl74540.A7, Ttl74540.A8
  };
  private static final byte[] OUTPUTS = {
    Ttl74540.Y1, Ttl74540.Y2, Ttl74540.Y3, Ttl74540.Y4,
    Ttl74540.Y5, Ttl74540.Y6, Ttl74540.Y7, Ttl74540.Y8
  };
  private static final int GND_PORT = 18;
  private static final int VCC_PORT = 19;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74540();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(18, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74540.OE1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74540.A1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74540.A2, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74540.A3, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74540.A4, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74540.A5, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74540.A6, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74540.A7, 150, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74540.A8, 170, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74540.Y8, 190, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74540.Y7, 170, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74540.Y6, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74540.Y5, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74540.Y4, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74540.Y3, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74540.Y2, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74540.Y1, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74540.OE2, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(20, shownPower.getPorts().size());
    assertEquals(Location.create(190, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void eachInputIsInvertedOnItsOwnOutput() {
    final var gate = new Ttl74540();
    final var state = enabled(gate);

    gate.propagate(state);
    assertOutputs(state, Value.TRUE);

    for (final var input : INPUTS) {
      setLevel(state, input, Value.TRUE);
    }
    gate.propagate(state);
    assertOutputs(state, Value.FALSE);

    for (final var input : INPUTS) {
      setLevel(state, input, Value.FALSE);
    }
    for (var channel = 0; channel < INPUTS.length; channel++) {
      setLevel(state, INPUTS[channel], Value.TRUE);
      gate.propagate(state);
      for (var output = 0; output < OUTPUTS.length; output++) {
        assertEquals(
            output == channel ? Value.FALSE : Value.TRUE,
            level(state, OUTPUTS[output]));
      }
      setLevel(state, INPUTS[channel], Value.FALSE);
    }
  }

  @Test
  void eitherOutputEnableReleasesEveryOutput() {
    final var gate = new Ttl74540();
    final var state = enabled(gate);
    setLevel(state, Ttl74540.A1, Value.TRUE);
    setLevel(state, Ttl74540.A8, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.FALSE, level(state, Ttl74540.Y1));
    assertEquals(Value.TRUE, level(state, Ttl74540.Y2));
    assertEquals(Value.FALSE, level(state, Ttl74540.Y8));

    setLevel(state, Ttl74540.OE1, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, Value.UNKNOWN);

    setLevel(state, Ttl74540.OE1, Value.FALSE);
    setLevel(state, Ttl74540.OE2, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, Value.UNKNOWN);

    setLevel(state, Ttl74540.OE2, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.FALSE, level(state, Ttl74540.Y1));
    assertEquals(Value.TRUE, level(state, Ttl74540.Y2));
    assertEquals(Value.FALSE, level(state, Ttl74540.Y8));
  }

  @Test
  void unknownEnableReleasesEveryOutput() {
    final var gate = new Ttl74540();
    final var state = enabled(gate);
    setLevel(state, Ttl74540.A1, Value.TRUE);
    gate.propagate(state);

    setLevel(state, Ttl74540.OE1, Value.UNKNOWN);
    gate.propagate(state);
    assertOutputs(state, Value.UNKNOWN);

    setLevel(state, Ttl74540.OE1, Value.FALSE);
    setLevel(state, Ttl74540.OE2, Value.ERROR);
    gate.propagate(state);
    assertOutputs(state, Value.UNKNOWN);
  }

  @Test
  void unknownInputStaysUnknownAndErrorStaysError() {
    final var gate = new Ttl74540();
    final var state = enabled(gate);

    setLevel(state, Ttl74540.A1, Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, level(state, Ttl74540.Y1));
    assertEquals(Value.TRUE, level(state, Ttl74540.Y2));

    setLevel(state, Ttl74540.A1, Value.ERROR);
    gate.propagate(state);
    assertEquals(Value.ERROR, level(state, Ttl74540.Y1));
    assertEquals(Value.TRUE, level(state, Ttl74540.Y2));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74540();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    setLevel(state, Ttl74540.OE1, Value.FALSE);
    setLevel(state, Ttl74540.OE2, Value.FALSE);
    for (final var input : INPUTS) {
      setLevel(state, input, Value.FALSE);
    }
    setLevel(state, Ttl74540.A1, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.FALSE, level(state, Ttl74540.Y1));

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, Value.UNKNOWN);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.FALSE, level(state, Ttl74540.Y1));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, Value.UNKNOWN);
  }

  /** Creates an enabled device whose data inputs are all held low. */
  private static TtlTestInstanceState enabled(Ttl74540 gate) {
    final var state = new TtlTestInstanceState(gate, false);
    setLevel(state, Ttl74540.OE1, Value.FALSE);
    setLevel(state, Ttl74540.OE2, Value.FALSE);
    for (final var input : INPUTS) {
      setLevel(state, input, Value.FALSE);
    }
    return state;
  }

  private static void assertOutputs(TtlTestInstanceState state, Value expected) {
    for (final var output : OUTPUTS) {
      assertEquals(expected, level(state, output));
    }
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74540.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl74540.pinNrToPortNr(dsPinNr), value);
  }

  private static Value level(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74540.pinNrToPortNr(dsPinNr));
  }
}
