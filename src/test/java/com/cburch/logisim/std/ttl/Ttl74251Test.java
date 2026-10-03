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

/** Functional tests for the 74HC251 8-line to 1-line data selector. */
class Ttl74251Test {
  private static final byte[] DATA = {
    Ttl74251.I0, Ttl74251.I1, Ttl74251.I2, Ttl74251.I3,
    Ttl74251.I4, Ttl74251.I5, Ttl74251.I6, Ttl74251.I7
  };

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var mux = new Ttl74251();
    final var hiddenPower = createInstance(mux, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74251.I3, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74251.I2, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74251.I1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74251.I0, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74251.Y, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74251.NY, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74251.NOE, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74251.S2, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74251.S1, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74251.S0, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74251.I7, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74251.I6, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74251.I5, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74251.I4, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(mux, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void enabledDeviceRoutesEveryAddressAndDataPattern() {
    final var mux = new Ttl74251();

    for (var channel = 0; channel < DATA.length; channel++) {
      for (var pattern = 0; pattern < 256; pattern++) {
        final var state = enabled(mux);
        setData(state, pattern);
        setAddress(state, channel);
        mux.propagate(state);

        final var selected = ((pattern >> channel) & 1) == 1 ? Value.TRUE : Value.FALSE;
        assertEquals(selected, output(state, Ttl74251.Y), "Y channel " + channel);
        assertEquals(complement(selected), output(state, Ttl74251.NY), "nY channel " + channel);
      }
    }
  }

  @Test
  void outputEnableReleasesBothOutputs() {
    final var mux = new Ttl74251();
    final var state = enabled(mux);
    setData(state, 0x01);
    setAddress(state, 0);
    mux.propagate(state);
    assertEquals(Value.TRUE, output(state, Ttl74251.Y));
    assertEquals(Value.FALSE, output(state, Ttl74251.NY));

    for (final var released : new Value[] {Value.TRUE, Value.UNKNOWN, Value.ERROR}) {
      setPort(state, Ttl74251.NOE, released);
      mux.propagate(state);
      assertEquals(Value.UNKNOWN, output(state, Ttl74251.Y));
      assertEquals(Value.UNKNOWN, output(state, Ttl74251.NY));
    }

    setPort(state, Ttl74251.NOE, Value.FALSE);
    mux.propagate(state);
    assertEquals(Value.TRUE, output(state, Ttl74251.Y));
    assertEquals(Value.FALSE, output(state, Ttl74251.NY));
  }

  @Test
  void unknownOrErrorSelectReleasesBothOutputs() {
    final var mux = new Ttl74251();
    final var state = enabled(mux);
    setData(state, 0xFF);
    setAddress(state, 0);

    setPort(state, Ttl74251.S1, Value.UNKNOWN);
    mux.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl74251.Y));
    assertEquals(Value.UNKNOWN, output(state, Ttl74251.NY));

    setAddress(state, 0);
    setPort(state, Ttl74251.S2, Value.ERROR);
    mux.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl74251.Y));
    assertEquals(Value.UNKNOWN, output(state, Ttl74251.NY));
  }

  @Test
  void selectedInputPassesUnknownAndErrorToBothOutputs() {
    final var mux = new Ttl74251();
    final var state = enabled(mux);
    setData(state, 0x00);
    setAddress(state, 3);

    setPort(state, Ttl74251.I3, Value.UNKNOWN);
    mux.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl74251.Y));
    assertEquals(Value.UNKNOWN, output(state, Ttl74251.NY));

    setPort(state, Ttl74251.I3, Value.ERROR);
    mux.propagate(state);
    assertEquals(Value.ERROR, output(state, Ttl74251.Y));
    assertEquals(Value.ERROR, output(state, Ttl74251.NY));
  }

  @Test
  void releasedOutputsIgnoreAddressAndDataChanges() {
    final var mux = new Ttl74251();
    final var state = enabled(mux);
    setData(state, 0x01);
    setAddress(state, 0);
    mux.propagate(state);
    assertEquals(Value.TRUE, output(state, Ttl74251.Y));

    setPort(state, Ttl74251.NOE, Value.TRUE);
    setData(state, 0x80);
    setAddress(state, 7);
    mux.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl74251.Y));
    assertEquals(Value.UNKNOWN, output(state, Ttl74251.NY));

    setPort(state, Ttl74251.NOE, Value.FALSE);
    mux.propagate(state);
    assertEquals(Value.TRUE, output(state, Ttl74251.Y));
    assertEquals(Value.FALSE, output(state, Ttl74251.NY));
  }

  @Test
  void invalidExposedPowerInputsReleaseTheOutputs() {
    final var mux = new Ttl74251();
    final var state = new TtlTestInstanceState(mux, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    setPort(state, Ttl74251.NOE, Value.FALSE);
    setData(state, 0x08);
    setAddress(state, 3);
    mux.propagate(state);
    assertEquals(Value.TRUE, output(state, Ttl74251.Y));
    assertEquals(Value.FALSE, output(state, Ttl74251.NY));

    state.setPortValue(VCC_PORT, Value.FALSE);
    mux.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl74251.Y));
    assertEquals(Value.UNKNOWN, output(state, Ttl74251.NY));

    state.setPortValue(VCC_PORT, Value.TRUE);
    mux.propagate(state);
    assertEquals(Value.TRUE, output(state, Ttl74251.Y));

    state.setPortValue(GND_PORT, Value.TRUE);
    mux.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl74251.Y));
    assertEquals(Value.UNKNOWN, output(state, Ttl74251.NY));

    state.setPortValue(GND_PORT, Value.FALSE);
    mux.propagate(state);
    assertEquals(Value.TRUE, output(state, Ttl74251.Y));
    assertEquals(Value.FALSE, output(state, Ttl74251.NY));
  }

  private static TtlTestInstanceState enabled(Ttl74251 mux) {
    final var state = new TtlTestInstanceState(mux, false);
    setPort(state, Ttl74251.NOE, Value.FALSE);
    return state;
  }

  private static void setData(TtlTestInstanceState state, int pattern) {
    for (var bit = 0; bit < DATA.length; bit++) {
      setPort(state, DATA[bit], ((pattern >> bit) & 1) == 1 ? Value.TRUE : Value.FALSE);
    }
  }

  private static void setAddress(TtlTestInstanceState state, int channel) {
    setPort(state, Ttl74251.S0, (channel & 1) != 0 ? Value.TRUE : Value.FALSE);
    setPort(state, Ttl74251.S1, (channel & 2) != 0 ? Value.TRUE : Value.FALSE);
    setPort(state, Ttl74251.S2, (channel & 4) != 0 ? Value.TRUE : Value.FALSE);
  }

  private static void setPort(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl74251.pinNrToPortNr(dsPinNr), value);
  }

  private static Value output(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74251.pinNrToPortNr(dsPinNr));
  }

  private static Value complement(Value value) {
    return value == Value.TRUE ? Value.FALSE : Value.TRUE;
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74251.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
