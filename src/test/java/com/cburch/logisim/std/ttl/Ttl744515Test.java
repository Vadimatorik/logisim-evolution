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

class Ttl744515Test {
  /** Outputs ordered by the address that selects them. */
  private static final byte[] OUTPUTS = {
    Ttl744515.Q0, Ttl744515.Q1, Ttl744515.Q2, Ttl744515.Q3,
    Ttl744515.Q4, Ttl744515.Q5, Ttl744515.Q6, Ttl744515.Q7,
    Ttl744515.Q8, Ttl744515.Q9, Ttl744515.Q10, Ttl744515.Q11,
    Ttl744515.Q12, Ttl744515.Q13, Ttl744515.Q14, Ttl744515.Q15
  };

  private static final int GND_PORT = 22;
  private static final int VCC_PORT = 23;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var decoder = new Ttl744515();
    final var hiddenPower = createInstance(decoder, false);

    assertEquals(22, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744515.LE, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.A0, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.A1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.Q7, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.Q6, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.Q5, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.Q4, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.Q3, 150, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.Q1, 170, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.Q2, 190, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.Q0, 210, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.Q13, 230, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.Q12, 210, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.Q15, 190, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.Q14, 170, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.Q9, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.Q8, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.Q11, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.Q10, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.A2, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.A3, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744515.NE, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(decoder, true);
    assertEquals(24, shownPower.getPorts().size());
    assertEquals(Location.create(230, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void eachAddressSelectsOneActiveLowOutputWhileTheLatchIsOpen() {
    final var decoder = new Ttl744515();

    for (var address = 0; address < OUTPUTS.length; address++) {
      final var state = transparent(decoder);
      setAddress(state, address);
      decoder.propagate(state);
      assertSelected(state, address);
    }
  }

  @Test
  void openLatchFollowsAChangingAddress() {
    final var decoder = new Ttl744515();
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
    final var decoder = new Ttl744515();
    final var state = transparent(decoder);
    setAddress(state, 6);
    decoder.propagate(state);

    setLow(state, Ttl744515.LE);
    setAddress(state, 1);
    decoder.propagate(state);
    assertSelected(state, 6);
  }

  @Test
  void enableDoesNotChangeTheLatchedAddress() {
    final var decoder = new Ttl744515();
    final var state = transparent(decoder);
    setAddress(state, 9);
    decoder.propagate(state);
    setLow(state, Ttl744515.LE);

    setHigh(state, Ttl744515.NE);
    setAddress(state, 3);
    decoder.propagate(state);
    assertSelected(state, -1);

    setLow(state, Ttl744515.NE);
    decoder.propagate(state);
    assertSelected(state, 9);
  }

  @Test
  void highEnableWhileOpenStillStoresTheAddress() {
    final var decoder = new Ttl744515();
    final var state = transparent(decoder);
    setHigh(state, Ttl744515.NE);
    setAddress(state, 11);
    decoder.propagate(state);
    assertSelected(state, -1);

    setLow(state, Ttl744515.LE);
    setAddress(state, 0);
    setLow(state, Ttl744515.NE);
    decoder.propagate(state);
    assertSelected(state, 11);
  }

  @Test
  void disabledDeviceIgnoresTheAddress() {
    final var decoder = new Ttl744515();

    for (var address = 0; address < OUTPUTS.length; address++) {
      final var state = transparent(decoder);
      setHigh(state, Ttl744515.NE);
      setAddress(state, address);
      decoder.propagate(state);
      assertSelected(state, -1);
    }

    final var unknownAddress = transparent(decoder);
    setHigh(unknownAddress, Ttl744515.NE);
    unknownAddress.setPortValue(Ttl744515.pinNrToPortNr(Ttl744515.A3), Value.UNKNOWN);
    decoder.propagate(unknownAddress);
    assertSelected(unknownAddress, -1);
  }

  @Test
  void unknownOrErrorLatchEnableHoldsTheAddress() {
    final var decoder = new Ttl744515();
    final var state = transparent(decoder);
    setAddress(state, 4);
    decoder.propagate(state);
    setAddress(state, 12);

    state.setPortValue(Ttl744515.pinNrToPortNr(Ttl744515.LE), Value.UNKNOWN);
    decoder.propagate(state);
    assertSelected(state, 4);

    state.setPortValue(Ttl744515.pinNrToPortNr(Ttl744515.LE), Value.ERROR);
    setAddress(state, 0);
    decoder.propagate(state);
    assertSelected(state, 4);
  }

  @Test
  void unknownOrErrorEnableMakesOutputsUnknownWithoutLosingTheAddress() {
    final var decoder = new Ttl744515();
    final var state = transparent(decoder);
    setAddress(state, 3);
    decoder.propagate(state);

    state.setPortValue(Ttl744515.pinNrToPortNr(Ttl744515.NE), Value.UNKNOWN);
    decoder.propagate(state);
    assertUnknownOutputs(state);

    setLow(state, Ttl744515.NE);
    decoder.propagate(state);
    assertSelected(state, 3);

    state.setPortValue(Ttl744515.pinNrToPortNr(Ttl744515.NE), Value.ERROR);
    decoder.propagate(state);
    assertUnknownOutputs(state);

    setLow(state, Ttl744515.NE);
    decoder.propagate(state);
    assertSelected(state, 3);
  }

  @Test
  void unknownOrErrorAddressWhileEnabledMakesOutputsUnknown() {
    final var decoder = new Ttl744515();
    final var unknown = transparent(decoder);
    setAddress(unknown, 0);
    unknown.setPortValue(Ttl744515.pinNrToPortNr(Ttl744515.A1), Value.UNKNOWN);
    decoder.propagate(unknown);
    assertUnknownOutputs(unknown);

    final var error = transparent(decoder);
    setAddress(error, 1);
    error.setPortValue(Ttl744515.pinNrToPortNr(Ttl744515.A0), Value.ERROR);
    decoder.propagate(error);
    assertUnknownOutputs(error);
  }

  @Test
  void unknownAddressBitIsStored() {
    final var decoder = new Ttl744515();
    final var state = transparent(decoder);
    setHigh(state, Ttl744515.NE);
    setAddress(state, 0);
    state.setPortValue(Ttl744515.pinNrToPortNr(Ttl744515.A0), Value.UNKNOWN);
    decoder.propagate(state);
    assertSelected(state, -1);

    setLow(state, Ttl744515.LE);
    setLow(state, Ttl744515.NE);
    setAddress(state, 0);
    decoder.propagate(state);
    assertUnknownOutputs(state);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var decoder = new Ttl744515();
    final var state = new TtlTestInstanceState(decoder, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    setHigh(state, Ttl744515.LE);
    setLow(state, Ttl744515.NE);
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
  private static TtlTestInstanceState transparent(Ttl744515 decoder) {
    final var state = new TtlTestInstanceState(decoder, false);
    setHigh(state, Ttl744515.LE);
    setLow(state, Ttl744515.NE);
    return state;
  }

  private static void setAddress(TtlTestInstanceState state, int address) {
    setLevel(state, Ttl744515.A0, (address & 1) != 0);
    setLevel(state, Ttl744515.A1, (address & 2) != 0);
    setLevel(state, Ttl744515.A2, (address & 4) != 0);
    setLevel(state, Ttl744515.A3, (address & 8) != 0);
  }

  /** Checks that {@code selected} is the only low output. {@code -1} requires every output high. */
  private static void assertSelected(TtlTestInstanceState state, int selected) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      final var expected = index == selected ? Value.FALSE : Value.TRUE;
      assertEquals(
          expected,
          state.getPortValue(Ttl744515.pinNrToPortNr(OUTPUTS[index])),
          "Q" + index);
    }
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var output : OUTPUTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(Ttl744515.pinNrToPortNr(output)));
    }
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl744515.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, boolean high) {
    state.setPortValue(Ttl744515.pinNrToPortNr(dsPinNr), high ? Value.TRUE : Value.FALSE);
  }

  private static void setLow(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, false);
  }

  private static void setHigh(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, true);
  }
}
