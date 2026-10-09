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

class Ttl744028Test {
  /** Outputs ordered by the BCD code that selects them. */
  private static final byte[] OUTPUTS = {
    Ttl744028.Y0, Ttl744028.Y1, Ttl744028.Y2, Ttl744028.Y3, Ttl744028.Y4,
    Ttl744028.Y5, Ttl744028.Y6, Ttl744028.Y7, Ttl744028.Y8, Ttl744028.Y9
  };

  private static final byte[] ADDRESS = {Ttl744028.A0, Ttl744028.A1, Ttl744028.A2, Ttl744028.A3};

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var decoder = new Ttl744028();
    final var hiddenPower = createInstance(decoder, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744028.Y4, 10, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744028.Y2, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744028.Y0, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744028.Y7, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744028.Y9, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744028.Y5, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744028.Y6, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744028.Y8, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744028.A0, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744028.A3, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744028.A2, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744028.A1, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744028.Y1, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744028.Y3, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(decoder, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void eachBcdCodeSelectsOneActiveHighOutput() {
    final var decoder = new Ttl744028();

    for (var code = 0; code <= 9; code++) {
      final var state = new TtlTestInstanceState(decoder, false);
      setAddress(state, code);
      decoder.propagate(state);
      assertSelected(state, code);
    }
  }

  @Test
  void illegalCodesHoldEveryOutputLow() {
    final var decoder = new Ttl744028();

    for (var code = 10; code <= 15; code++) {
      final var state = new TtlTestInstanceState(decoder, false);
      setAddress(state, code);
      decoder.propagate(state);
      assertSelected(state, -1);
    }
  }

  @Test
  void unknownAddressMakesOutputsUnknown() {
    final var decoder = new Ttl744028();

    for (final var bit : ADDRESS) {
      final var state = new TtlTestInstanceState(decoder, false);
      setAddress(state, 5);
      state.setPortValue(Ttl744028.pinNrToPortNr(bit), Value.UNKNOWN);
      decoder.propagate(state);
      assertUnknownOutputs(state);
    }
  }

  @Test
  void errorAddressMakesOutputsUnknown() {
    final var decoder = new Ttl744028();

    for (final var bit : ADDRESS) {
      final var state = new TtlTestInstanceState(decoder, false);
      setAddress(state, 5);
      state.setPortValue(Ttl744028.pinNrToPortNr(bit), Value.ERROR);
      decoder.propagate(state);
      assertUnknownOutputs(state);
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var decoder = new Ttl744028();
    final var state = new TtlTestInstanceState(decoder, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
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

  private static void setAddress(TtlTestInstanceState state, int address) {
    setLevel(state, Ttl744028.A0, (address & 1) != 0);
    setLevel(state, Ttl744028.A1, (address & 2) != 0);
    setLevel(state, Ttl744028.A2, (address & 4) != 0);
    setLevel(state, Ttl744028.A3, (address & 8) != 0);
  }

  /** Checks that {@code selected} is the only high output. {@code -1} requires every output low. */
  private static void assertSelected(TtlTestInstanceState state, int selected) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      final var expected = index == selected ? Value.TRUE : Value.FALSE;
      assertEquals(
          expected,
          state.getPortValue(Ttl744028.pinNrToPortNr(OUTPUTS[index])),
          "Y" + index);
    }
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var output : OUTPUTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(Ttl744028.pinNrToPortNr(output)));
    }
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl744028.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, boolean high) {
    state.setPortValue(Ttl744028.pinNrToPortNr(dsPinNr), high ? Value.TRUE : Value.FALSE);
  }
}
