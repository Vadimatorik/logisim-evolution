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

class Ttl74150Test {
  /** Data inputs ordered by the address that selects them. */
  private static final byte[] DATA = {
    Ttl74150.E0, Ttl74150.E1, Ttl74150.E2, Ttl74150.E3,
    Ttl74150.E4, Ttl74150.E5, Ttl74150.E6, Ttl74150.E7,
    Ttl74150.E8, Ttl74150.E9, Ttl74150.E10, Ttl74150.E11,
    Ttl74150.E12, Ttl74150.E13, Ttl74150.E14, Ttl74150.E15
  };

  private static final int GND_PORT = 22;
  private static final int VCC_PORT = 23;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var selector = new Ttl74150();
    final var hiddenPower = createInstance(selector, false);

    assertEquals(22, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74150.E7, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.E6, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.E5, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.E4, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.E3, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.E2, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.E1, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.E0, 150, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.G, 170, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.W, 190, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.D, 210, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.C, 230, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.B, 210, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.A, 190, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.E15, 170, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.E14, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.E13, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.E12, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.E11, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.E10, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.E9, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74150.E8, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(selector, true);
    assertEquals(24, shownPower.getPorts().size());
    assertEquals(Location.create(230, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void enabledDeviceInvertsTheSelectedInput() {
    final var selector = new Ttl74150();

    for (var address = 0; address < DATA.length; address++) {
      for (var selectedHigh : new boolean[] {false, true}) {
        final var state = enabled(selector, address, selectedHigh);
        selector.propagate(state);

        assertEquals(selectedHigh ? Value.FALSE : Value.TRUE, output(state));
      }
    }
  }

  @Test
  void strobeHighForcesTheOutputHigh() {
    final var selector = new Ttl74150();

    for (var address = 0; address < DATA.length; address++) {
      final var state = enabled(selector, address, true);
      setHigh(state, Ttl74150.G);
      selector.propagate(state);

      assertEquals(Value.TRUE, output(state));
    }

    final var unknownAddress = enabled(selector, 0, true);
    setHigh(unknownAddress, Ttl74150.G);
    unknownAddress.setPortValue(Ttl74150.pinNrToPortNr(Ttl74150.A), Value.UNKNOWN);
    selector.propagate(unknownAddress);
    assertEquals(Value.TRUE, output(unknownAddress));
  }

  @Test
  void selectedInputPassesUnknownAndErrorToTheInvertedOutput() {
    final var selector = new Ttl74150();

    final var unknown = enabled(selector, 5, false);
    unknown.setPortValue(Ttl74150.pinNrToPortNr(Ttl74150.E5), Value.UNKNOWN);
    selector.propagate(unknown);
    assertEquals(Value.UNKNOWN, output(unknown));

    final var error = enabled(selector, 5, false);
    error.setPortValue(Ttl74150.pinNrToPortNr(Ttl74150.E5), Value.ERROR);
    selector.propagate(error);
    assertEquals(Value.ERROR, output(error));

    final var unselectedError = enabled(selector, 5, false);
    unselectedError.setPortValue(Ttl74150.pinNrToPortNr(Ttl74150.E4), Value.ERROR);
    selector.propagate(unselectedError);
    assertEquals(Value.TRUE, output(unselectedError));
  }

  @Test
  void undefinedAddressPropagatesUnknownOrError() {
    final var selector = new Ttl74150();

    final var unknown = enabled(selector, 0, false);
    unknown.setPortValue(Ttl74150.pinNrToPortNr(Ttl74150.D), Value.UNKNOWN);
    selector.propagate(unknown);
    assertEquals(Value.UNKNOWN, output(unknown));

    final var error = enabled(selector, 0, false);
    error.setPortValue(Ttl74150.pinNrToPortNr(Ttl74150.B), Value.ERROR);
    selector.propagate(error);
    assertEquals(Value.ERROR, output(error));
  }

  @Test
  void undefinedStrobePropagatesUnknownOrError() {
    final var selector = new Ttl74150();

    final var unknown = enabled(selector, 9, true);
    unknown.setPortValue(Ttl74150.pinNrToPortNr(Ttl74150.G), Value.UNKNOWN);
    selector.propagate(unknown);
    assertEquals(Value.UNKNOWN, output(unknown));

    final var error = enabled(selector, 9, true);
    error.setPortValue(Ttl74150.pinNrToPortNr(Ttl74150.G), Value.ERROR);
    selector.propagate(error);
    assertEquals(Value.ERROR, output(error));

    final var unknownStrobeWithErrorData = enabled(selector, 9, true);
    unknownStrobeWithErrorData.setPortValue(Ttl74150.pinNrToPortNr(Ttl74150.G), Value.UNKNOWN);
    unknownStrobeWithErrorData.setPortValue(Ttl74150.pinNrToPortNr(Ttl74150.E0), Value.ERROR);
    selector.propagate(unknownStrobeWithErrorData);
    assertEquals(Value.ERROR, output(unknownStrobeWithErrorData));
  }

  @Test
  void invalidExposedPowerInputsMakeTheOutputUnknown() {
    final var selector = new Ttl74150();
    final var state = new TtlTestInstanceState(selector, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    setAddress(state, 3);
    setData(state, 3, false);
    setLow(state, Ttl74150.G);
    selector.propagate(state);
    assertEquals(Value.TRUE, output(state));

    state.setPortValue(VCC_PORT, Value.FALSE);
    selector.propagate(state);
    assertEquals(Value.UNKNOWN, output(state));

    state.setPortValue(VCC_PORT, Value.TRUE);
    selector.propagate(state);
    assertEquals(Value.TRUE, output(state));

    state.setPortValue(GND_PORT, Value.TRUE);
    selector.propagate(state);
    assertEquals(Value.UNKNOWN, output(state));
  }

  /** Creates an enabled device that selects one input and drives every other input to the opposite level. */
  private static TtlTestInstanceState enabled(Ttl74150 selector, int address, boolean selectedHigh) {
    final var state = new TtlTestInstanceState(selector, false);
    setLow(state, Ttl74150.G);
    setAddress(state, address);
    setData(state, address, selectedHigh);
    return state;
  }

  private static void setAddress(TtlTestInstanceState state, int address) {
    setLevel(state, Ttl74150.A, (address & 1) != 0);
    setLevel(state, Ttl74150.B, (address & 2) != 0);
    setLevel(state, Ttl74150.C, (address & 4) != 0);
    setLevel(state, Ttl74150.D, (address & 8) != 0);
  }

  private static void setData(TtlTestInstanceState state, int selected, boolean selectedHigh) {
    for (var index = 0; index < DATA.length; index++) {
      setLevel(state, DATA[index], index == selected ? selectedHigh : !selectedHigh);
    }
  }

  private static Value output(TtlTestInstanceState state) {
    return state.getPortValue(Ttl74150.pinNrToPortNr(Ttl74150.W));
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74150.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, boolean high) {
    state.setPortValue(Ttl74150.pinNrToPortNr(dsPinNr), high ? Value.TRUE : Value.FALSE);
  }

  private static void setLow(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, false);
  }

  private static void setHigh(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, true);
  }
}
