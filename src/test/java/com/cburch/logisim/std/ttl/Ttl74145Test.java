/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Tests for the TTL 74145 BCD-to-decimal decoder/driver. */
class Ttl74145Test {
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var decoder = new Ttl74145();
    final var hiddenPower = TtlTestInstanceState.createInstance(decoder, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74145.Q0, 10, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74145.Q1, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74145.Q2, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74145.Q3, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74145.Q4, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74145.Q5, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74145.Q6, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74145.Q7, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74145.Q8, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74145.Q9, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74145.D, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74145.C, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74145.B, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74145.A, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = TtlTestInstanceState.createInstance(decoder, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void eachValidCodeSinksOnlyItsOwnOutput() {
    final var decoder = new Ttl74145();

    for (var code = 0; code <= 9; code++) {
      final var state = driven(decoder, code);
      decoder.propagate(state);
      assertOutputs(state, Value.UNKNOWN, code);
    }
  }

  @Test
  void invalidCodesReleaseEveryOutput() {
    final var decoder = new Ttl74145();

    for (var code = 10; code <= 15; code++) {
      final var state = driven(decoder, code);
      decoder.propagate(state);
      assertOutputs(state, Value.UNKNOWN, -1);
    }
  }

  @Test
  void unsettledInputMakesEveryOutputError() {
    final var decoder = new Ttl74145();

    for (final var pin : new byte[] {Ttl74145.A, Ttl74145.B, Ttl74145.C, Ttl74145.D}) {
      for (final var unsettled : new Value[] {Value.UNKNOWN, Value.ERROR}) {
        final var state = driven(decoder, 5);
        state.setPortValue(Ttl74145.pinNrToPortNr(pin), unsettled);
        decoder.propagate(state);
        assertOutputs(state, Value.ERROR, -1);
      }
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var decoder = new Ttl74145();
    final var state = new TtlTestInstanceState(decoder, true);
    setLevel(state, Ttl74145.A, false);
    setLevel(state, Ttl74145.B, false);
    setLevel(state, Ttl74145.C, true);
    setLevel(state, Ttl74145.D, false);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    decoder.propagate(state);
    assertOutputs(state, Value.UNKNOWN, 4);

    state.setPortValue(VCC_PORT, Value.FALSE);
    decoder.propagate(state);
    assertOutputs(state, Value.UNKNOWN, -1);

    state.setPortValue(VCC_PORT, Value.TRUE);
    decoder.propagate(state);
    assertOutputs(state, Value.UNKNOWN, 4);

    state.setPortValue(GND_PORT, Value.TRUE);
    decoder.propagate(state);
    assertOutputs(state, Value.UNKNOWN, -1);
  }

  private static TtlTestInstanceState driven(Ttl74145 decoder, int code) {
    final var state = new TtlTestInstanceState(decoder, false);
    setLevel(state, Ttl74145.A, (code & 1) != 0);
    setLevel(state, Ttl74145.B, (code & 2) != 0);
    setLevel(state, Ttl74145.C, (code & 4) != 0);
    setLevel(state, Ttl74145.D, (code & 8) != 0);
    return state;
  }

  private static void assertOutputs(TtlTestInstanceState state, Value released, int sinkingCode) {
    final var outputs = new byte[] {
      Ttl74145.Q0, Ttl74145.Q1, Ttl74145.Q2, Ttl74145.Q3, Ttl74145.Q4,
      Ttl74145.Q5, Ttl74145.Q6, Ttl74145.Q7, Ttl74145.Q8, Ttl74145.Q9
    };
    for (var index = 0; index < outputs.length; index++) {
      final var expected = index == sinkingCode ? Value.FALSE : released;
      assertEquals(expected, state.getPortValue(Ttl74145.pinNrToPortNr(outputs[index])));
    }
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74145.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, boolean high) {
    state.setPortValue(Ttl74145.pinNrToPortNr(dsPinNr), high ? Value.TRUE : Value.FALSE);
  }
}
