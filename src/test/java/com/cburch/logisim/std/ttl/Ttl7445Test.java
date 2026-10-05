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

/** Functional tests for the 7445 BCD-to-decimal decoder/driver. */
class Ttl7445Test {
  private static final byte[] OUTPUTS = {
    Ttl7445.O0, Ttl7445.O1, Ttl7445.O2, Ttl7445.O3, Ttl7445.O4,
    Ttl7445.O5, Ttl7445.O6, Ttl7445.O7, Ttl7445.O8, Ttl7445.O9
  };

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var decoder = new Ttl7445();
    final var hiddenPower = createInstance(decoder, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl7445.O0, 10, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7445.O1, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7445.O2, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7445.O3, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7445.O4, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7445.O5, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7445.O6, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7445.O7, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7445.O8, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7445.O9, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7445.D, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7445.C, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7445.B, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7445.A, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(decoder, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void validBcdCodePullsOnlyTheSelectedOutputLow() {
    final var decoder = new Ttl7445();

    for (var code = 0; code <= 9; code++) {
      final var state = new TtlTestInstanceState(decoder, false);
      setCode(state, code);
      decoder.propagate(state);
      assertOutputs(state, code);
    }
  }

  @Test
  void invalidBcdCodeReleasesEveryOutput() {
    final var decoder = new Ttl7445();

    for (var code = 10; code <= 15; code++) {
      final var state = new TtlTestInstanceState(decoder, false);
      setCode(state, code);
      decoder.propagate(state);
      assertOutputs(state, -1);
    }
  }

  @Test
  void unknownInputReleasesEveryOutput() {
    final var decoder = new Ttl7445();
    final var state = new TtlTestInstanceState(decoder, false);
    setCode(state, 4);
    state.setPortValue(Ttl7445.pinNrToPortNr(Ttl7445.B), Value.UNKNOWN);
    decoder.propagate(state);

    assertOutputs(state, -1);
  }

  @Test
  void errorInputDrivesEveryOutputToError() {
    final var decoder = new Ttl7445();
    final var state = new TtlTestInstanceState(decoder, false);
    setCode(state, 1);
    state.setPortValue(Ttl7445.pinNrToPortNr(Ttl7445.A), Value.UNKNOWN);
    state.setPortValue(Ttl7445.pinNrToPortNr(Ttl7445.D), Value.ERROR);
    decoder.propagate(state);

    for (final var output : OUTPUTS) {
      assertEquals(Value.ERROR, state.getPortValue(Ttl7445.pinNrToPortNr(output)));
    }
  }

  @Test
  void invalidExposedPowerInputsReleaseTheOutputs() {
    final var decoder = new Ttl7445();
    final var state = new TtlTestInstanceState(decoder, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    setCode(state, 3);
    decoder.propagate(state);
    assertOutputs(state, 3);

    state.setPortValue(VCC_PORT, Value.FALSE);
    decoder.propagate(state);
    assertOutputs(state, -1);

    state.setPortValue(VCC_PORT, Value.TRUE);
    decoder.propagate(state);
    assertOutputs(state, 3);

    state.setPortValue(GND_PORT, Value.TRUE);
    decoder.propagate(state);
    assertOutputs(state, -1);
  }

  private static void setCode(TtlTestInstanceState state, int code) {
    setBit(state, Ttl7445.A, (code & 1) != 0);
    setBit(state, Ttl7445.B, (code & 2) != 0);
    setBit(state, Ttl7445.C, (code & 4) != 0);
    setBit(state, Ttl7445.D, (code & 8) != 0);
  }

  private static void setBit(TtlTestInstanceState state, byte dsPinNr, boolean high) {
    state.setPortValue(Ttl7445.pinNrToPortNr(dsPinNr), high ? Value.TRUE : Value.FALSE);
  }

  /**
   * Checks the open-collector outputs.
   *
   * @param selected output index that must sink, or -1 when every output is released
   */
  private static void assertOutputs(TtlTestInstanceState state, int selected) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      final var expected = index == selected ? Value.FALSE : Value.UNKNOWN;
      assertEquals(
          expected, state.getPortValue(Ttl7445.pinNrToPortNr(OUTPUTS[index])), "output " + index);
    }
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl7445.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
