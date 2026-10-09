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

class Ttl74239Test {
  /** Outputs of one half, ordered by the address that selects them. */
  private static final byte[][] OUTPUTS = {
    {Ttl74239.Y0_1, Ttl74239.Y1_1, Ttl74239.Y2_1, Ttl74239.Y3_1},
    {Ttl74239.Y0_2, Ttl74239.Y1_2, Ttl74239.Y2_2, Ttl74239.Y3_2}
  };

  private static final byte[] ENABLES = {Ttl74239.NE1, Ttl74239.NE2};
  private static final byte[] ADDRESS0 = {Ttl74239.A0_1, Ttl74239.A0_2};
  private static final byte[] ADDRESS1 = {Ttl74239.A1_1, Ttl74239.A1_2};

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var decoder = new Ttl74239();
    final var hiddenPower = createInstance(decoder, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74239.NE1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74239.A0_1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74239.A1_1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74239.Y0_1, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74239.Y1_1, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74239.Y2_1, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74239.Y3_1, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74239.Y3_2, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74239.Y2_2, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74239.Y1_2, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74239.Y0_2, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74239.A1_2, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74239.A0_2, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74239.NE2, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(decoder, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void eachAddressSelectsOneActiveHighOutput() {
    final var decoder = new Ttl74239();

    for (var half = 0; half < OUTPUTS.length; half++) {
      for (var address = 0; address < OUTPUTS[half].length; address++) {
        final var state = enabled(decoder);
        setAddress(state, half, address);
        decoder.propagate(state);
        assertSelected(state, half, address);
      }
    }
  }

  @Test
  void eachEnableDisablesItsOwnOutputs() {
    final var decoder = new Ttl74239();

    for (var half = 0; half < OUTPUTS.length; half++) {
      final var state = enabled(decoder);
      setAddress(state, 0, 1);
      setAddress(state, 1, 2);
      setHigh(state, ENABLES[half]);
      decoder.propagate(state);
      assertSelected(state, half, -1);
      assertSelected(state, 1 - half, half == 0 ? 2 : 1);
    }
  }

  @Test
  void disabledHalfIgnoresTheAddress() {
    final var decoder = new Ttl74239();

    for (var half = 0; half < OUTPUTS.length; half++) {
      for (var address = 0; address < OUTPUTS[half].length; address++) {
        final var state = enabled(decoder);
        setHigh(state, ENABLES[half]);
        setAddress(state, half, address);
        decoder.propagate(state);
        assertSelected(state, half, -1);
      }
    }

    final var unknownAddress = enabled(decoder);
    setHigh(unknownAddress, Ttl74239.NE1);
    unknownAddress.setPortValue(Ttl74239.pinNrToPortNr(Ttl74239.A1_1), Value.UNKNOWN);
    decoder.propagate(unknownAddress);
    assertSelected(unknownAddress, 0, -1);
    assertSelected(unknownAddress, 1, 0);
  }

  @Test
  void halvesStayIndependent() {
    final var decoder = new Ttl74239();
    final var state = enabled(decoder);
    setAddress(state, 0, 0);
    setAddress(state, 1, 3);
    decoder.propagate(state);
    assertSelected(state, 0, 0);
    assertSelected(state, 1, 3);
  }

  @Test
  void unknownEnableMakesThatHalfUnknown() {
    final var decoder = new Ttl74239();
    final var state = enabled(decoder);
    setAddress(state, 0, 3);
    setAddress(state, 1, 1);
    state.setPortValue(Ttl74239.pinNrToPortNr(Ttl74239.NE2), Value.UNKNOWN);
    decoder.propagate(state);
    assertSelected(state, 0, 3);
    assertUnknownOutputs(state, 1);
  }

  @Test
  void unknownAddressWhileEnabledMakesThatHalfUnknown() {
    final var decoder = new Ttl74239();
    final var state = enabled(decoder);
    setAddress(state, 0, 0);
    setAddress(state, 1, 2);
    state.setPortValue(Ttl74239.pinNrToPortNr(Ttl74239.A0_1), Value.UNKNOWN);
    decoder.propagate(state);
    assertUnknownOutputs(state, 0);
    assertSelected(state, 1, 2);
  }

  @Test
  void errorInputsMakeThatHalfUnknown() {
    final var decoder = new Ttl74239();
    final var state = enabled(decoder);
    setAddress(state, 0, 1);
    setAddress(state, 1, 1);
    state.setPortValue(Ttl74239.pinNrToPortNr(Ttl74239.NE1), Value.ERROR);
    decoder.propagate(state);
    assertUnknownOutputs(state, 0);
    assertSelected(state, 1, 1);

    state.setPortValue(Ttl74239.pinNrToPortNr(Ttl74239.NE1), Value.FALSE);
    state.setPortValue(Ttl74239.pinNrToPortNr(Ttl74239.A1_2), Value.ERROR);
    decoder.propagate(state);
    assertSelected(state, 0, 1);
    assertUnknownOutputs(state, 1);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var decoder = new Ttl74239();
    final var state = new TtlTestInstanceState(decoder, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    setLow(state, Ttl74239.NE1);
    setLow(state, Ttl74239.NE2);
    setAddress(state, 0, 2);
    setAddress(state, 1, 3);
    decoder.propagate(state);
    assertSelected(state, 0, 2);
    assertSelected(state, 1, 3);

    state.setPortValue(VCC_PORT, Value.FALSE);
    decoder.propagate(state);
    assertUnknownOutputs(state, 0);
    assertUnknownOutputs(state, 1);

    state.setPortValue(VCC_PORT, Value.TRUE);
    decoder.propagate(state);
    assertSelected(state, 0, 2);
    assertSelected(state, 1, 3);

    state.setPortValue(GND_PORT, Value.TRUE);
    decoder.propagate(state);
    assertUnknownOutputs(state, 0);
    assertUnknownOutputs(state, 1);
  }

  /** Creates a device with both halves enabled at address 0. */
  private static TtlTestInstanceState enabled(Ttl74239 decoder) {
    final var state = new TtlTestInstanceState(decoder, false);
    setLow(state, Ttl74239.NE1);
    setLow(state, Ttl74239.NE2);
    setAddress(state, 0, 0);
    setAddress(state, 1, 0);
    return state;
  }

  private static void setAddress(TtlTestInstanceState state, int half, int address) {
    setLevel(state, ADDRESS0[half], (address & 1) != 0);
    setLevel(state, ADDRESS1[half], (address & 2) != 0);
  }

  /** Checks that {@code selected} is the only high output. {@code -1} requires every output low. */
  private static void assertSelected(TtlTestInstanceState state, int half, int selected) {
    for (var index = 0; index < OUTPUTS[half].length; index++) {
      final var expected = index == selected ? Value.TRUE : Value.FALSE;
      assertEquals(
          expected, state.getPortValue(Ttl74239.pinNrToPortNr(OUTPUTS[half][index])), "half " + half);
    }
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state, int half) {
    for (final var output : OUTPUTS[half]) {
      assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74239.pinNrToPortNr(output)));
    }
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74239.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, boolean high) {
    state.setPortValue(Ttl74239.pinNrToPortNr(dsPinNr), high ? Value.TRUE : Value.FALSE);
  }

  private static void setLow(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, false);
  }

  private static void setHigh(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, true);
  }
}
