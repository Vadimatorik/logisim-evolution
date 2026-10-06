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

class Ttl74154Test {
  /** Outputs ordered by the address that selects them. */
  private static final byte[] OUTPUTS = {
    Ttl74154.Y0, Ttl74154.Y1, Ttl74154.Y2, Ttl74154.Y3,
    Ttl74154.Y4, Ttl74154.Y5, Ttl74154.Y6, Ttl74154.Y7,
    Ttl74154.Y8, Ttl74154.Y9, Ttl74154.Y10, Ttl74154.Y11,
    Ttl74154.Y12, Ttl74154.Y13, Ttl74154.Y14, Ttl74154.Y15
  };

  private static final int GND_PORT = 22;
  private static final int VCC_PORT = 23;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var decoder = new Ttl74154();
    final var hiddenPower = createInstance(decoder, false);

    assertEquals(22, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74154.Y0, 10, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.Y1, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.Y2, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.Y3, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.Y4, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.Y5, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.Y6, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.Y7, 150, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.Y8, 170, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.Y9, 190, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.Y10, 210, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.Y11, 230, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.Y12, 210, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.Y13, 190, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.Y14, 170, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.Y15, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.NE1, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.NE2, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.A3, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.A2, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.A1, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74154.A0, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(decoder, true);
    assertEquals(24, shownPower.getPorts().size());
    assertEquals(Location.create(230, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void eachAddressSelectsOneActiveLowOutput() {
    final var decoder = new Ttl74154();

    for (var address = 0; address < OUTPUTS.length; address++) {
      final var state = enabled(decoder);
      setAddress(state, address);
      decoder.propagate(state);
      assertSelected(state, address);
    }
  }

  @Test
  void eachEnableDisablesAllOutputs() {
    final var decoder = new Ttl74154();

    final var nEnable1High = enabled(decoder);
    setAddress(nEnable1High, 5);
    setHigh(nEnable1High, Ttl74154.NE1);
    decoder.propagate(nEnable1High);
    assertSelected(nEnable1High, -1);

    final var nEnable2High = enabled(decoder);
    setAddress(nEnable2High, 5);
    setHigh(nEnable2High, Ttl74154.NE2);
    decoder.propagate(nEnable2High);
    assertSelected(nEnable2High, -1);
  }

  @Test
  void disabledDeviceIgnoresTheAddress() {
    final var decoder = new Ttl74154();

    for (var address = 0; address < OUTPUTS.length; address++) {
      final var state = enabled(decoder);
      setHigh(state, Ttl74154.NE1);
      setAddress(state, address);
      decoder.propagate(state);
      assertSelected(state, -1);
    }

    final var unknownAddress = enabled(decoder);
    setHigh(unknownAddress, Ttl74154.NE2);
    unknownAddress.setPortValue(Ttl74154.pinNrToPortNr(Ttl74154.A3), Value.UNKNOWN);
    decoder.propagate(unknownAddress);
    assertSelected(unknownAddress, -1);
  }

  @Test
  void unknownEnableMakesOutputsUnknown() {
    final var decoder = new Ttl74154();
    final var state = enabled(decoder);
    setAddress(state, 3);
    state.setPortValue(Ttl74154.pinNrToPortNr(Ttl74154.NE1), Value.UNKNOWN);
    decoder.propagate(state);
    assertUnknownOutputs(state);
  }

  @Test
  void unknownAddressWhileEnabledMakesOutputsUnknown() {
    final var decoder = new Ttl74154();
    final var state = enabled(decoder);
    setAddress(state, 0);
    state.setPortValue(Ttl74154.pinNrToPortNr(Ttl74154.A1), Value.UNKNOWN);
    decoder.propagate(state);
    assertUnknownOutputs(state);
  }

  @Test
  void errorInputsMakeOutputsUnknown() {
    final var decoder = new Ttl74154();
    final var enableError = enabled(decoder);
    setAddress(enableError, 1);
    enableError.setPortValue(Ttl74154.pinNrToPortNr(Ttl74154.NE2), Value.ERROR);
    decoder.propagate(enableError);
    assertUnknownOutputs(enableError);

    final var addressError = enabled(decoder);
    setAddress(addressError, 1);
    addressError.setPortValue(Ttl74154.pinNrToPortNr(Ttl74154.A0), Value.ERROR);
    decoder.propagate(addressError);
    assertUnknownOutputs(addressError);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var decoder = new Ttl74154();
    final var state = new TtlTestInstanceState(decoder, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    setLow(state, Ttl74154.NE1);
    setLow(state, Ttl74154.NE2);
    setAddress(state, 4);
    decoder.propagate(state);
    assertSelected(state, 4);

    state.setPortValue(VCC_PORT, Value.FALSE);
    decoder.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    decoder.propagate(state);
    assertSelected(state, 4);

    state.setPortValue(GND_PORT, Value.TRUE);
    decoder.propagate(state);
    assertUnknownOutputs(state);
  }

  /** Creates an enabled device. The address is left for the caller to drive. */
  private static TtlTestInstanceState enabled(Ttl74154 decoder) {
    final var state = new TtlTestInstanceState(decoder, false);
    setLow(state, Ttl74154.NE1);
    setLow(state, Ttl74154.NE2);
    return state;
  }

  private static void setAddress(TtlTestInstanceState state, int address) {
    setLevel(state, Ttl74154.A0, (address & 1) != 0);
    setLevel(state, Ttl74154.A1, (address & 2) != 0);
    setLevel(state, Ttl74154.A2, (address & 4) != 0);
    setLevel(state, Ttl74154.A3, (address & 8) != 0);
  }

  /** Checks that {@code selected} is the only low output. {@code -1} requires every output high. */
  private static void assertSelected(TtlTestInstanceState state, int selected) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      final var expected = index == selected ? Value.FALSE : Value.TRUE;
      assertEquals(
          expected,
          state.getPortValue(Ttl74154.pinNrToPortNr(OUTPUTS[index])),
          "Y" + index);
    }
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var output : OUTPUTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74154.pinNrToPortNr(output)));
    }
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74154.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, boolean high) {
    state.setPortValue(Ttl74154.pinNrToPortNr(dsPinNr), high ? Value.TRUE : Value.FALSE);
  }

  private static void setLow(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, false);
  }

  private static void setHigh(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, true);
  }
}
