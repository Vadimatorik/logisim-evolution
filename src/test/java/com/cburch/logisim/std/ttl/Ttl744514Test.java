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

class Ttl744514Test {
  /** Outputs ordered by the address that selects them. */
  private static final byte[] OUTPUTS = {
    Ttl744514.Q0, Ttl744514.Q1, Ttl744514.Q2, Ttl744514.Q3,
    Ttl744514.Q4, Ttl744514.Q5, Ttl744514.Q6, Ttl744514.Q7,
    Ttl744514.Q8, Ttl744514.Q9, Ttl744514.Q10, Ttl744514.Q11,
    Ttl744514.Q12, Ttl744514.Q13, Ttl744514.Q14, Ttl744514.Q15
  };

  private static final int GND_PORT = 22;
  private static final int VCC_PORT = 23;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var decoder = new Ttl744514();
    final var hiddenPower = createInstance(decoder, false);

    assertEquals(22, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744514.LE, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.A0, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.A1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.Q7, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.Q6, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.Q5, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.Q4, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.Q3, 150, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.Q1, 170, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.Q2, 190, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.Q0, 210, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.Q13, 230, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.Q12, 210, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.Q15, 190, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.Q14, 170, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.Q9, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.Q8, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.Q11, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.Q10, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.A2, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.A3, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744514.NE, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(decoder, true);
    assertEquals(24, shownPower.getPorts().size());
    assertEquals(Location.create(230, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void eachAddressSelectsOneActiveHighOutputWhileTheLatchIsOpen() {
    final var decoder = new Ttl744514();

    for (var address = 0; address < OUTPUTS.length; address++) {
      final var state = transparent(decoder);
      setAddress(state, address);
      decoder.propagate(state);
      assertSelected(state, address);
    }
  }

  @Test
  void openLatchFollowsAChangingAddress() {
    final var decoder = new Ttl744514();
    final var state = transparent(decoder);

    setAddress(state, 2);
    decoder.propagate(state);
    assertSelected(state, 2);

    setAddress(state, 14);
    decoder.propagate(state);
    assertSelected(state, 14);
  }

  @Test
  void fallingLatchEnableHoldsTheAddress() {
    final var decoder = new Ttl744514();
    final var state = transparent(decoder);
    setAddress(state, 6);
    decoder.propagate(state);

    setLow(state, Ttl744514.LE);
    setAddress(state, 1);
    decoder.propagate(state);
    assertSelected(state, 6);
  }

  @Test
  void enableDoesNotChangeTheLatchedAddress() {
    final var decoder = new Ttl744514();
    final var state = transparent(decoder);
    setAddress(state, 9);
    decoder.propagate(state);
    setLow(state, Ttl744514.LE);

    setHigh(state, Ttl744514.NE);
    setAddress(state, 3);
    decoder.propagate(state);
    assertSelected(state, -1);

    setLow(state, Ttl744514.NE);
    decoder.propagate(state);
    assertSelected(state, 9);
  }

  @Test
  void highEnableWhileOpenStillStoresTheAddress() {
    final var decoder = new Ttl744514();
    final var state = transparent(decoder);
    setHigh(state, Ttl744514.NE);
    setAddress(state, 11);
    decoder.propagate(state);
    assertSelected(state, -1);

    setLow(state, Ttl744514.LE);
    setAddress(state, 0);
    setLow(state, Ttl744514.NE);
    decoder.propagate(state);
    assertSelected(state, 11);
  }

  @Test
  void disabledDeviceIgnoresTheAddress() {
    final var decoder = new Ttl744514();

    for (var address = 0; address < OUTPUTS.length; address++) {
      final var state = transparent(decoder);
      setHigh(state, Ttl744514.NE);
      setAddress(state, address);
      decoder.propagate(state);
      assertSelected(state, -1);
    }

    final var unknownAddress = transparent(decoder);
    setHigh(unknownAddress, Ttl744514.NE);
    unknownAddress.setPortValue(Ttl744514.pinNrToPortNr(Ttl744514.A3), Value.UNKNOWN);
    decoder.propagate(unknownAddress);
    assertSelected(unknownAddress, -1);
  }

  @Test
  void unknownOrErrorLatchEnableDoesNotCorruptTheStoredAddress() {
    final var decoder = new Ttl744514();
    final var state = transparent(decoder);
    setAddress(state, 4);
    decoder.propagate(state);
    setAddress(state, 12);

    state.setPortValue(Ttl744514.pinNrToPortNr(Ttl744514.LE), Value.UNKNOWN);
    decoder.propagate(state);
    assertPattern(state, "0000U0000000U000");

    setLow(state, Ttl744514.LE);
    decoder.propagate(state);
    assertSelected(state, 4);

    state.setPortValue(Ttl744514.pinNrToPortNr(Ttl744514.LE), Value.ERROR);
    setAddress(state, 0);
    decoder.propagate(state);
    assertPattern(state, "E000E00000000000");

    setLow(state, Ttl744514.LE);
    decoder.propagate(state);
    assertSelected(state, 4);
  }

  @Test
  void unknownEnableMakesOnlyTheSelectedOutputUnknown() {
    final var decoder = new Ttl744514();
    final var state = transparent(decoder);
    setAddress(state, 5);
    decoder.propagate(state);

    state.setPortValue(Ttl744514.pinNrToPortNr(Ttl744514.NE), Value.UNKNOWN);
    decoder.propagate(state);
    assertPattern(state, "00000U0000000000");

    setLow(state, Ttl744514.NE);
    decoder.propagate(state);
    assertSelected(state, 5);
  }

  @Test
  void errorEnableMakesOnlyTheSelectedOutputAnError() {
    final var decoder = new Ttl744514();
    final var state = transparent(decoder);
    setAddress(state, 5);
    decoder.propagate(state);

    state.setPortValue(Ttl744514.pinNrToPortNr(Ttl744514.NE), Value.ERROR);
    decoder.propagate(state);
    assertPattern(state, "00000E0000000000");

    setLow(state, Ttl744514.NE);
    decoder.propagate(state);
    assertSelected(state, 5);
  }

  @Test
  void unknownAddressBitLeavesOnlyTheCandidatesUnknown() {
    final var decoder = new Ttl744514();
    final var state = transparent(decoder);
    setAddress(state, 0);
    state.setPortValue(Ttl744514.pinNrToPortNr(Ttl744514.A1), Value.UNKNOWN);
    decoder.propagate(state);
    assertPattern(state, "U0U0000000000000");
  }

  @Test
  void errorAddressBitLeavesOnlyTheCandidatesInError() {
    final var decoder = new Ttl744514();
    final var state = transparent(decoder);
    setAddress(state, 0);
    state.setPortValue(Ttl744514.pinNrToPortNr(Ttl744514.A0), Value.ERROR);
    decoder.propagate(state);
    assertPattern(state, "EE00000000000000");
  }

  @Test
  void unknownAddressBitIsStored() {
    final var decoder = new Ttl744514();
    final var state = transparent(decoder);
    setHigh(state, Ttl744514.NE);
    setAddress(state, 0);
    state.setPortValue(Ttl744514.pinNrToPortNr(Ttl744514.A0), Value.UNKNOWN);
    decoder.propagate(state);
    assertSelected(state, -1);

    setLow(state, Ttl744514.LE);
    setLow(state, Ttl744514.NE);
    setAddress(state, 0);
    decoder.propagate(state);
    assertPattern(state, "UU00000000000000");
  }

  @Test
  void unknownStartupAddressStaysUnknownUntilTheLatchOpens() {
    final var previous = AppPreferences.Memory_Startup_Unknown.get();
    AppPreferences.Memory_Startup_Unknown.set(true);
    try {
      final var decoder = new Ttl744514();
      final var state = new TtlTestInstanceState(decoder, false);
      setLow(state, Ttl744514.LE);
      setLow(state, Ttl744514.NE);
      setAddress(state, 7);
      decoder.propagate(state);
      assertPattern(state, "UUUUUUUUUUUUUUUU");

      setHigh(state, Ttl744514.NE);
      decoder.propagate(state);
      assertSelected(state, -1);

      setLow(state, Ttl744514.NE);
      decoder.propagate(state);
      assertPattern(state, "UUUUUUUUUUUUUUUU");

      setHigh(state, Ttl744514.LE);
      decoder.propagate(state);
      assertSelected(state, 7);
    } finally {
      AppPreferences.Memory_Startup_Unknown.set(previous);
    }
  }

  @Test
  void knownStartupAddressSelectsOutputZeroUntilOverwritten() {
    final var previous = AppPreferences.Memory_Startup_Unknown.get();
    AppPreferences.Memory_Startup_Unknown.set(false);
    try {
      final var decoder = new Ttl744514();
      final var state = new TtlTestInstanceState(decoder, false);
      setLow(state, Ttl744514.LE);
      setLow(state, Ttl744514.NE);
      setAddress(state, 7);
      decoder.propagate(state);
      assertSelected(state, 0);

      setHigh(state, Ttl744514.LE);
      decoder.propagate(state);
      assertSelected(state, 7);
    } finally {
      AppPreferences.Memory_Startup_Unknown.set(previous);
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var decoder = new Ttl744514();
    final var state = new TtlTestInstanceState(decoder, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    setHigh(state, Ttl744514.LE);
    setLow(state, Ttl744514.NE);
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

  /** Creates a device whose latch is transparent and whose outputs are enabled. */
  private static TtlTestInstanceState transparent(Ttl744514 decoder) {
    final var state = new TtlTestInstanceState(decoder, false);
    setHigh(state, Ttl744514.LE);
    setLow(state, Ttl744514.NE);
    return state;
  }

  private static void setAddress(TtlTestInstanceState state, int address) {
    setLevel(state, Ttl744514.A0, (address & 1) != 0);
    setLevel(state, Ttl744514.A1, (address & 2) != 0);
    setLevel(state, Ttl744514.A2, (address & 4) != 0);
    setLevel(state, Ttl744514.A3, (address & 8) != 0);
  }

  /** Checks that {@code selected} is the only high output. {@code -1} requires every output low. */
  private static void assertSelected(TtlTestInstanceState state, int selected) {
    final var pattern = new StringBuilder("0000000000000000");
    if (selected >= 0) pattern.setCharAt(selected, '1');
    assertPattern(state, pattern.toString());
  }

  /**
   * Checks each output against one character of {@code pattern}: {@code 1} high, {@code 0} low,
   * {@code U} unknown, {@code E} error. Index 0 is {@code Q0}.
   */
  private static void assertPattern(TtlTestInstanceState state, String pattern) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      assertEquals(
          patternValue(pattern.charAt(index)),
          state.getPortValue(Ttl744514.pinNrToPortNr(OUTPUTS[index])),
          "Q" + index);
    }
  }

  private static Value patternValue(char symbol) {
    return switch (symbol) {
      case '1' -> Value.TRUE;
      case '0' -> Value.FALSE;
      case 'U' -> Value.UNKNOWN;
      case 'E' -> Value.ERROR;
      default -> throw new IllegalArgumentException("Unknown pattern symbol");
    };
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    assertPattern(state, "UUUUUUUUUUUUUUUU");
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl744514.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, boolean high) {
    state.setPortValue(Ttl744514.pinNrToPortNr(dsPinNr), high ? Value.TRUE : Value.FALSE);
  }

  private static void setLow(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, false);
  }

  private static void setHigh(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, true);
  }
}
