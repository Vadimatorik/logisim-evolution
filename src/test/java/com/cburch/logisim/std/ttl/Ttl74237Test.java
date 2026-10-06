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
import com.cburch.logisim.prefs.AppPreferences;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74x237 decoder with address latches. */
class Ttl74237Test {
  private static final byte[] OUTPUTS = {
    Ttl74237.Y0, Ttl74237.Y1, Ttl74237.Y2, Ttl74237.Y3,
    Ttl74237.Y4, Ttl74237.Y5, Ttl74237.Y6, Ttl74237.Y7
  };

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var decoder = new Ttl74237();
    final var hiddenPower = createInstance(decoder, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74237.A0, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74237.A1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74237.A2, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74237.LE, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74237.E1, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74237.E2, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74237.Y7, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74237.Y6, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74237.Y5, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74237.Y4, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74237.Y3, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74237.Y2, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74237.Y1, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74237.Y0, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(decoder, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void eachAddressSelectsItsActiveHighOutputWhileTheLatchIsTransparent() {
    final var decoder = new Ttl74237();

    for (var address = 0; address < OUTPUTS.length; address++) {
      final var state = enabled(decoder);
      setAddress(state, address);
      decoder.propagate(state);
      assertSelected(state, address);
    }
  }

  @Test
  void eitherInactiveEnableForcesEveryOutputLow() {
    final var decoder = new Ttl74237();

    for (var address = 0; address < OUTPUTS.length; address++) {
      final var e1High = enabled(decoder);
      setAddress(e1High, address);
      setHigh(e1High, Ttl74237.E1);
      decoder.propagate(e1High);
      assertLevel(e1High, Value.FALSE);

      final var e2Low = enabled(decoder);
      setAddress(e2Low, address);
      setLow(e2Low, Ttl74237.E2);
      decoder.propagate(e2Low);
      assertLevel(e2Low, Value.FALSE);
    }
  }

  @Test
  void risingLatchEnableKeepsTheAddressPresentBeforeTheEdge() {
    final var decoder = new Ttl74237();
    final var state = enabled(decoder);
    setAddress(state, 5);
    decoder.propagate(state);
    assertSelected(state, 5);

    setHigh(state, Ttl74237.LE);
    setAddress(state, 2);
    decoder.propagate(state);
    assertSelected(state, 5);

    setLow(state, Ttl74237.LE);
    decoder.propagate(state);
    assertSelected(state, 2);
  }

  @Test
  void enablesBlankAndRestoreTheStoredLine() {
    final var decoder = new Ttl74237();
    final var state = enabled(decoder);
    setAddress(state, 4);
    decoder.propagate(state);
    setHigh(state, Ttl74237.LE);
    setAddress(state, 1);
    decoder.propagate(state);
    assertSelected(state, 4);

    setHigh(state, Ttl74237.E1);
    decoder.propagate(state);
    assertLevel(state, Value.FALSE);

    setLow(state, Ttl74237.E1);
    decoder.propagate(state);
    assertSelected(state, 4);

    setLow(state, Ttl74237.E2);
    decoder.propagate(state);
    assertLevel(state, Value.FALSE);

    setHigh(state, Ttl74237.E2);
    decoder.propagate(state);
    assertSelected(state, 4);
  }

  @Test
  void addressIsStoredWhileTheOutputsAreDisabled() {
    final var decoder = new Ttl74237();
    final var state = enabled(decoder);
    setHigh(state, Ttl74237.E1);
    setAddress(state, 1);
    decoder.propagate(state);
    assertLevel(state, Value.FALSE);

    setHigh(state, Ttl74237.LE);
    setAddress(state, 0);
    setLow(state, Ttl74237.E1);
    decoder.propagate(state);
    assertSelected(state, 1);
  }

  @Test
  void unknownAddressDrivesEnabledOutputsToError() {
    final var decoder = new Ttl74237();
    final var state = enabled(decoder);
    setAddress(state, 3);
    state.setPortValue(Ttl74237.pinNrToPortNr(Ttl74237.A1), Value.UNKNOWN);
    decoder.propagate(state);
    assertLevel(state, Value.ERROR);

    setAddress(state, 3);
    decoder.propagate(state);
    assertSelected(state, 3);
  }

  @Test
  void unknownEnableLeavesTheOutputsLowAndStillLetsTheLatchSample() {
    final var decoder = new Ttl74237();
    final var state = enabled(decoder);
    state.setPortValue(Ttl74237.pinNrToPortNr(Ttl74237.E2), Value.UNKNOWN);
    setAddress(state, 6);
    decoder.propagate(state);
    assertLevel(state, Value.FALSE);

    setHigh(state, Ttl74237.LE);
    setAddress(state, 0);
    setHigh(state, Ttl74237.E2);
    decoder.propagate(state);
    assertSelected(state, 6);
  }

  @Test
  void unknownLatchEnableDoesNotFollowANewAddress() {
    final var decoder = new Ttl74237();
    final var state = enabled(decoder);
    setAddress(state, 6);
    decoder.propagate(state);

    state.setPortValue(Ttl74237.pinNrToPortNr(Ttl74237.LE), Value.UNKNOWN);
    setAddress(state, 0);
    decoder.propagate(state);
    assertSelected(state, 6);
  }

  @Test
  void unconnectedDeviceKeepsEveryOutputLow() {
    final var decoder = new Ttl74237();
    final var state = new TtlTestInstanceState(decoder, false);
    decoder.propagate(state);
    assertLevel(state, Value.FALSE);
  }

  @Test
  void latchDoesNotSampleWhileItsEnableIsAlreadyHigh() {
    final var decoder = new Ttl74237();
    final var state = enabled(decoder);
    setHigh(state, Ttl74237.LE);
    setAddress(state, 7);
    decoder.propagate(state);

    if (AppPreferences.Memory_Startup_Unknown.get()) {
      assertLevel(state, Value.ERROR);
    } else {
      assertSelected(state, 0);
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var decoder = new Ttl74237();
    final var state = new TtlTestInstanceState(decoder, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    setLow(state, Ttl74237.LE);
    setLow(state, Ttl74237.E1);
    setHigh(state, Ttl74237.E2);
    setAddress(state, 3);
    decoder.propagate(state);
    assertSelected(state, 3);

    state.setPortValue(VCC_PORT, Value.FALSE);
    decoder.propagate(state);
    assertLevel(state, Value.UNKNOWN);

    state.setPortValue(VCC_PORT, Value.TRUE);
    decoder.propagate(state);
    assertSelected(state, 3);

    state.setPortValue(GND_PORT, Value.TRUE);
    decoder.propagate(state);
    assertLevel(state, Value.UNKNOWN);
  }

  /** Creates an enabled device whose latch is transparent and whose address is low. */
  private static TtlTestInstanceState enabled(Ttl74237 decoder) {
    final var state = new TtlTestInstanceState(decoder, false);
    setLow(state, Ttl74237.LE);
    setLow(state, Ttl74237.E1);
    setHigh(state, Ttl74237.E2);
    setAddress(state, 0);
    return state;
  }

  private static void setAddress(TtlTestInstanceState state, int address) {
    setLevel(state, Ttl74237.A0, (address & 1) != 0);
    setLevel(state, Ttl74237.A1, (address & 2) != 0);
    setLevel(state, Ttl74237.A2, (address & 4) != 0);
  }

  private static void assertSelected(TtlTestInstanceState state, int address) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      assertEquals(index == address ? Value.TRUE : Value.FALSE, level(state, OUTPUTS[index]));
    }
  }

  private static void assertLevel(TtlTestInstanceState state, Value level) {
    for (final var output : OUTPUTS) {
      assertEquals(level, level(state, output));
    }
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74237.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, boolean high) {
    state.setPortValue(Ttl74237.pinNrToPortNr(dsPinNr), high ? Value.TRUE : Value.FALSE);
  }

  private static void setLow(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, false);
  }

  private static void setHigh(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, true);
  }

  private static Value level(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74237.pinNrToPortNr(dsPinNr));
  }
}
