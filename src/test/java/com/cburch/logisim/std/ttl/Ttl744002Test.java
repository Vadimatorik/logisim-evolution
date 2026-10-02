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

class Ttl744002Test {
  /** Gate 1 inputs, then gate 2 inputs. Bit 0 is 1A. */
  private static final byte[] INPUTS = {
    Ttl744002.A1, Ttl744002.B1, Ttl744002.C1, Ttl744002.D1,
    Ttl744002.A2, Ttl744002.B2, Ttl744002.C2, Ttl744002.D2
  };

  private static final int GND_PORT = 10;
  private static final int VCC_PORT = 11;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl744002();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(10, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744002.Y1, 10, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744002.A1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744002.B1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744002.C1, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744002.D1, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744002.A2, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744002.B2, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744002.C2, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744002.D2, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744002.Y2, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(12, shownPower.getPorts().size());
    assertEquals(Location.create(130, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void outputIsHighOnlyWhenEveryInputIsLow() {
    final var gate = new Ttl744002();

    for (var pattern = 0; pattern < 256; pattern++) {
      final var state = new TtlTestInstanceState(gate, false);
      setInputs(state, pattern);
      gate.propagate(state);

      assertEquals(norNibble(pattern & 0x0F), output(state, Ttl744002.Y1), "gate 1 " + pattern);
      assertEquals(norNibble(pattern >>> 4), output(state, Ttl744002.Y2), "gate 2 " + pattern);
    }
  }

  @Test
  void highInputOverridesUnknownAndErrorOnTheSameGate() {
    final var gate = new Ttl744002();
    final var state = new TtlTestInstanceState(gate, false);
    setLow(state, Ttl744002.A2);
    setLow(state, Ttl744002.B2);
    setLow(state, Ttl744002.C2);
    setLow(state, Ttl744002.D2);

    setHigh(state, Ttl744002.A1);
    state.setPortValue(Ttl744002.pinNrToPortNr(Ttl744002.B1), Value.UNKNOWN);
    state.setPortValue(Ttl744002.pinNrToPortNr(Ttl744002.C1), Value.ERROR);
    setLow(state, Ttl744002.D1);
    gate.propagate(state);

    assertEquals(Value.FALSE, output(state, Ttl744002.Y1));
    assertEquals(Value.TRUE, output(state, Ttl744002.Y2));
  }

  @Test
  void unknownOrErrorStaysOnItsOwnGateWhenNoInputIsHigh() {
    final var gate = new Ttl744002();
    final var unknown = new TtlTestInstanceState(gate, false);
    setAllLow(unknown);
    unknown.setPortValue(Ttl744002.pinNrToPortNr(Ttl744002.C1), Value.UNKNOWN);
    gate.propagate(unknown);
    assertEquals(Value.UNKNOWN, output(unknown, Ttl744002.Y1));
    assertEquals(Value.TRUE, output(unknown, Ttl744002.Y2));

    final var error = new TtlTestInstanceState(gate, false);
    setAllLow(error);
    error.setPortValue(Ttl744002.pinNrToPortNr(Ttl744002.B2), Value.ERROR);
    error.setPortValue(Ttl744002.pinNrToPortNr(Ttl744002.C2), Value.UNKNOWN);
    gate.propagate(error);
    assertEquals(Value.TRUE, output(error, Ttl744002.Y1));
    assertEquals(Value.ERROR, output(error, Ttl744002.Y2));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl744002();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    setAllLow(state);
    gate.propagate(state);
    assertEquals(Value.TRUE, output(state, Ttl744002.Y1));
    assertEquals(Value.TRUE, output(state, Ttl744002.Y2));

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl744002.Y1));
    assertEquals(Value.UNKNOWN, output(state, Ttl744002.Y2));

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.TRUE, output(state, Ttl744002.Y1));
    assertEquals(Value.TRUE, output(state, Ttl744002.Y2));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl744002.Y1));
    assertEquals(Value.UNKNOWN, output(state, Ttl744002.Y2));
  }

  private static Value norNibble(int nibble) {
    return nibble == 0 ? Value.TRUE : Value.FALSE;
  }

  private static void setInputs(TtlTestInstanceState state, int pattern) {
    for (var bit = 0; bit < INPUTS.length; bit++) {
      state.setPortValue(
          Ttl744002.pinNrToPortNr(INPUTS[bit]),
          ((pattern >>> bit) & 1) == 1 ? Value.TRUE : Value.FALSE);
    }
  }

  private static void setAllLow(TtlTestInstanceState state) {
    for (final var pin : INPUTS) {
      setLow(state, pin);
    }
  }

  private static Value output(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl744002.pinNrToPortNr(dsPinNr));
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl744002.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLow(TtlTestInstanceState state, byte dsPinNr) {
    state.setPortValue(Ttl744002.pinNrToPortNr(dsPinNr), Value.FALSE);
  }

  private static void setHigh(TtlTestInstanceState state, byte dsPinNr) {
    state.setPortValue(Ttl744002.pinNrToPortNr(dsPinNr), Value.TRUE);
  }
}
