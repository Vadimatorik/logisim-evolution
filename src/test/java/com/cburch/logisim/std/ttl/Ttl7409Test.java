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

/** Functional tests for the 74x09 quad 2-input open-drain AND gate. */
class Ttl7409Test {
  private static final int GND_PORT = 12;
  private static final int VCC_PORT = 13;

  private static final byte[][] GATES = {
    {Ttl7409.A1, Ttl7409.B1, Ttl7409.Y1},
    {Ttl7409.A2, Ttl7409.B2, Ttl7409.Y2},
    {Ttl7409.A3, Ttl7409.B3, Ttl7409.Y3},
    {Ttl7409.A4, Ttl7409.B4, Ttl7409.Y4}
  };

  private static final byte[] INPUTS = {
    Ttl7409.A1, Ttl7409.B1, Ttl7409.A2, Ttl7409.B2,
    Ttl7409.A3, Ttl7409.B3, Ttl7409.A4, Ttl7409.B4
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl7409();
    assertEquals("7409", gate.getName());

    final var hiddenPower = createInstance(gate, false);
    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl7409.A1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7409.B1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7409.Y1, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7409.A2, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7409.B2, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7409.Y2, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7409.Y3, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7409.A3, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7409.B3, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7409.Y4, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7409.A4, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7409.B4, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertEquals(Location.create(130, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void eachGateReleasesOnlyWhenBothInputsAreHigh() {
    final var gate = new Ttl7409();
    final var state = new TtlTestInstanceState(gate, false);
    final var inputs = new Value[] {Value.FALSE, Value.TRUE};
    final var outputs =
        new Value[][] {
          {Value.FALSE, Value.FALSE},
          {Value.FALSE, Value.UNKNOWN}
        };

    for (var gateIndex = 0; gateIndex < GATES.length; gateIndex++) {
      for (var aIndex = 0; aIndex < inputs.length; aIndex++) {
        for (var bIndex = 0; bIndex < inputs.length; bIndex++) {
          setAllLow(state);
          setPin(state, GATES[gateIndex][0], inputs[aIndex]);
          setPin(state, GATES[gateIndex][1], inputs[bIndex]);
          gate.propagate(state);
          assertPin(state, GATES[gateIndex][2], outputs[aIndex][bIndex]);
          for (var other = 0; other < GATES.length; other++) {
            if (other != gateIndex) assertPin(state, GATES[other][2], Value.FALSE);
          }
        }
      }
    }
  }

  @Test
  void gatesDoNotAffectEachOther() {
    final var gate = new Ttl7409();
    final var state = new TtlTestInstanceState(gate, false);
    setAllLow(state);
    setPin(state, Ttl7409.A1, Value.TRUE);
    setPin(state, Ttl7409.B1, Value.TRUE);
    setPin(state, Ttl7409.A2, Value.FALSE);
    setPin(state, Ttl7409.B2, Value.TRUE);
    setPin(state, Ttl7409.A3, Value.TRUE);
    setPin(state, Ttl7409.B3, Value.FALSE);
    setPin(state, Ttl7409.A4, Value.TRUE);
    setPin(state, Ttl7409.B4, Value.TRUE);
    gate.propagate(state);

    assertPin(state, Ttl7409.Y1, Value.UNKNOWN);
    assertPin(state, Ttl7409.Y2, Value.FALSE);
    assertPin(state, Ttl7409.Y3, Value.FALSE);
    assertPin(state, Ttl7409.Y4, Value.UNKNOWN);
  }

  @Test
  void lowInputForcesTheOutputLowAndOtherUnknownsAreErrors() {
    final var gate = new Ttl7409();
    final var state = new TtlTestInstanceState(gate, false);
    final var rows =
        new Value[][] {
          {Value.FALSE, Value.UNKNOWN, Value.FALSE},
          {Value.UNKNOWN, Value.FALSE, Value.FALSE},
          {Value.FALSE, Value.ERROR, Value.FALSE},
          {Value.ERROR, Value.FALSE, Value.FALSE},
          {Value.TRUE, Value.UNKNOWN, Value.ERROR},
          {Value.UNKNOWN, Value.TRUE, Value.ERROR},
          {Value.UNKNOWN, Value.UNKNOWN, Value.ERROR},
          {Value.TRUE, Value.ERROR, Value.ERROR},
          {Value.ERROR, Value.TRUE, Value.ERROR},
          {Value.ERROR, Value.ERROR, Value.ERROR},
          {Value.ERROR, Value.UNKNOWN, Value.ERROR},
          {Value.UNKNOWN, Value.ERROR, Value.ERROR}
        };

    for (var pins : GATES) {
      for (var row : rows) {
        setAllLow(state);
        setPin(state, pins[0], row[0]);
        setPin(state, pins[1], row[1]);
        gate.propagate(state);
        assertPin(state, pins[2], row[2]);
      }
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl7409();
    final var state = new TtlTestInstanceState(gate, true);
    setAllLow(state);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertPin(state, Ttl7409.Y1, Value.FALSE);
    assertPin(state, Ttl7409.Y4, Value.FALSE);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertOutputsUnknown(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertPin(state, Ttl7409.Y1, Value.FALSE);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertOutputsUnknown(state);

    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.UNKNOWN);
    gate.propagate(state);
    assertOutputsUnknown(state);

    state.setPortValue(VCC_PORT, Value.ERROR);
    gate.propagate(state);
    assertOutputsUnknown(state);
  }

  private static void assertPort(Instance instance, byte pin, int x, int y, int type) {
    final int index = Ttl7409.pinNrToPortNr(pin);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void setAllLow(TtlTestInstanceState state) {
    for (var pin : INPUTS) setPin(state, pin, Value.FALSE);
  }

  private static void setPin(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl7409.pinNrToPortNr(pin), value);
  }

  private static void assertPin(TtlTestInstanceState state, byte pin, Value expected) {
    assertEquals(expected, state.getPortValue(Ttl7409.pinNrToPortNr(pin)));
  }

  private static void assertOutputsUnknown(TtlTestInstanceState state) {
    assertPin(state, Ttl7409.Y1, Value.UNKNOWN);
    assertPin(state, Ttl7409.Y2, Value.UNKNOWN);
    assertPin(state, Ttl7409.Y3, Value.UNKNOWN);
    assertPin(state, Ttl7409.Y4, Value.UNKNOWN);
  }
}
