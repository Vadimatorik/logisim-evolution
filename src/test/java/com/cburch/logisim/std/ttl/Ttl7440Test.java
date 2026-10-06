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

class Ttl7440Test {
  /** Gate 1 inputs, then gate 2 inputs. Bit 0 is 1A. */
  private static final byte[] INPUTS = {
    Ttl7440.A1, Ttl7440.B1, Ttl7440.C1, Ttl7440.D1,
    Ttl7440.A2, Ttl7440.B2, Ttl7440.C2, Ttl7440.D2
  };

  private static final int GND_PORT = 10;
  private static final int VCC_PORT = 11;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl7440();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(10, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl7440.A1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440.B1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440.C1, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440.D1, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440.Y1, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7440.Y2, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7440.A2, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440.B2, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440.C2, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7440.D2, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(12, shownPower.getPorts().size());
    assertEquals(Location.create(130, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void outputIsLowOnlyWhenEveryInputIsHigh() {
    final var gate = new Ttl7440();

    for (var pattern = 0; pattern < 256; pattern++) {
      final var state = new TtlTestInstanceState(gate, false);
      setInputs(state, pattern);
      gate.propagate(state);

      assertEquals(nandNibble(pattern & 0x0F), output(state, Ttl7440.Y1), "gate 1 " + pattern);
      assertEquals(nandNibble(pattern >>> 4), output(state, Ttl7440.Y2), "gate 2 " + pattern);
    }
  }

  @Test
  void lowInputOverridesUnknownAndErrorOnTheSameGate() {
    final var gate = new Ttl7440();
    final var state = new TtlTestInstanceState(gate, false);
    setHigh(state, Ttl7440.A2);
    setHigh(state, Ttl7440.B2);
    setHigh(state, Ttl7440.C2);
    setHigh(state, Ttl7440.D2);

    setLow(state, Ttl7440.A1);
    state.setPortValue(Ttl7440.pinNrToPortNr(Ttl7440.B1), Value.UNKNOWN);
    state.setPortValue(Ttl7440.pinNrToPortNr(Ttl7440.C1), Value.ERROR);
    setHigh(state, Ttl7440.D1);
    gate.propagate(state);

    assertEquals(Value.TRUE, output(state, Ttl7440.Y1));
    assertEquals(Value.FALSE, output(state, Ttl7440.Y2));
  }

  @Test
  void unknownOrErrorStaysOnItsOwnGateWhenNoInputIsLow() {
    final var gate = new Ttl7440();
    final var unknown = new TtlTestInstanceState(gate, false);
    setAllHigh(unknown);
    unknown.setPortValue(Ttl7440.pinNrToPortNr(Ttl7440.C1), Value.UNKNOWN);
    gate.propagate(unknown);
    assertEquals(Value.UNKNOWN, output(unknown, Ttl7440.Y1));
    assertEquals(Value.FALSE, output(unknown, Ttl7440.Y2));

    final var error = new TtlTestInstanceState(gate, false);
    setAllHigh(error);
    error.setPortValue(Ttl7440.pinNrToPortNr(Ttl7440.B2), Value.ERROR);
    error.setPortValue(Ttl7440.pinNrToPortNr(Ttl7440.C2), Value.UNKNOWN);
    gate.propagate(error);
    assertEquals(Value.FALSE, output(error, Ttl7440.Y1));
    assertEquals(Value.ERROR, output(error, Ttl7440.Y2));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl7440();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    setAllHigh(state);
    gate.propagate(state);
    assertEquals(Value.FALSE, output(state, Ttl7440.Y1));
    assertEquals(Value.FALSE, output(state, Ttl7440.Y2));

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl7440.Y1));
    assertEquals(Value.UNKNOWN, output(state, Ttl7440.Y2));

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.FALSE, output(state, Ttl7440.Y1));
    assertEquals(Value.FALSE, output(state, Ttl7440.Y2));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl7440.Y1));
    assertEquals(Value.UNKNOWN, output(state, Ttl7440.Y2));
  }

  private static Value nandNibble(int nibble) {
    return nibble == 0x0F ? Value.FALSE : Value.TRUE;
  }

  private static void setInputs(TtlTestInstanceState state, int pattern) {
    for (var bit = 0; bit < INPUTS.length; bit++) {
      state.setPortValue(
          Ttl7440.pinNrToPortNr(INPUTS[bit]),
          ((pattern >>> bit) & 1) == 1 ? Value.TRUE : Value.FALSE);
    }
  }

  private static void setAllHigh(TtlTestInstanceState state) {
    for (final var pin : INPUTS) {
      setHigh(state, pin);
    }
  }

  private static Value output(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl7440.pinNrToPortNr(dsPinNr));
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl7440.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLow(TtlTestInstanceState state, byte dsPinNr) {
    state.setPortValue(Ttl7440.pinNrToPortNr(dsPinNr), Value.FALSE);
  }

  private static void setHigh(TtlTestInstanceState state, byte dsPinNr) {
    state.setPortValue(Ttl7440.pinNrToPortNr(dsPinNr), Value.TRUE);
  }
}
