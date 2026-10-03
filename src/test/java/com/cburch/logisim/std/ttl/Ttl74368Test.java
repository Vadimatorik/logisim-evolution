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

class Ttl74368Test {
  /** Data inputs in channel order. */
  private static final byte[] INPUTS = {
    Ttl74368.A1, Ttl74368.A2, Ttl74368.A3, Ttl74368.A4, Ttl74368.A5, Ttl74368.A6
  };

  /** Data outputs in channel order. */
  private static final byte[] OUTPUTS = {
    Ttl74368.Y1, Ttl74368.Y2, Ttl74368.Y3, Ttl74368.Y4, Ttl74368.Y5, Ttl74368.Y6
  };

  /** Buffers controlled by n1OE. The remaining buffers belong to n2OE. */
  private static final int GROUP1_SIZE = 4;

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var buffer = new Ttl74368();
    final var hiddenPower = createInstance(buffer, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74368.OE1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74368.A1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74368.Y1, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74368.A2, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74368.Y2, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74368.A3, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74368.Y3, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74368.Y4, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74368.A4, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74368.Y5, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74368.A5, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74368.Y6, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74368.A6, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74368.OE2, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(buffer, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void enabledDeviceInvertsEveryDataPattern() {
    final var buffer = new Ttl74368();

    for (var pattern = 0; pattern < (1 << INPUTS.length); pattern++) {
      final var state = enabled(buffer);
      setData(state, pattern);
      buffer.propagate(state);
      assertInverted(state, pattern, 0, OUTPUTS.length);
    }
  }

  @Test
  void eachEnableReleasesOnlyItsOwnGroup() {
    final var buffer = new Ttl74368();

    for (var pattern = 0; pattern < (1 << INPUTS.length); pattern++) {
      final var outputEnable1High = enabled(buffer);
      setData(outputEnable1High, pattern);
      setHigh(outputEnable1High, Ttl74368.OE1);
      buffer.propagate(outputEnable1High);
      assertReleased(outputEnable1High, 0, GROUP1_SIZE);
      assertInverted(outputEnable1High, pattern, GROUP1_SIZE, OUTPUTS.length);

      final var outputEnable2High = enabled(buffer);
      setData(outputEnable2High, pattern);
      setHigh(outputEnable2High, Ttl74368.OE2);
      buffer.propagate(outputEnable2High);
      assertInverted(outputEnable2High, pattern, 0, GROUP1_SIZE);
      assertReleased(outputEnable2High, GROUP1_SIZE, OUTPUTS.length);

      final var bothHigh = enabled(buffer);
      setData(bothHigh, pattern);
      setHigh(bothHigh, Ttl74368.OE1);
      setHigh(bothHigh, Ttl74368.OE2);
      buffer.propagate(bothHigh);
      assertReleased(bothHigh, 0, OUTPUTS.length);
    }
  }

  @Test
  void unknownEnableReleasesOnlyItsGroupAndErrorEnableDrivesError() {
    final var buffer = new Ttl74368();

    final var unknownEnable = enabled(buffer);
    setData(unknownEnable, 0x15);
    unknownEnable.setPortValue(Ttl74368.pinNrToPortNr(Ttl74368.OE1), Value.UNKNOWN);
    buffer.propagate(unknownEnable);
    assertReleased(unknownEnable, 0, GROUP1_SIZE);
    assertInverted(unknownEnable, 0x15, GROUP1_SIZE, OUTPUTS.length);

    final var errorEnable = enabled(buffer);
    setData(errorEnable, 0x2A);
    errorEnable.setPortValue(Ttl74368.pinNrToPortNr(Ttl74368.OE2), Value.ERROR);
    buffer.propagate(errorEnable);
    assertInverted(errorEnable, 0x2A, 0, GROUP1_SIZE);
    assertError(errorEnable, GROUP1_SIZE, OUTPUTS.length);
  }

  @Test
  void enabledChannelInvertsUnknownAndErrorOnItsOwnInput() {
    final var buffer = new Ttl74368();
    final var state = enabled(buffer);
    setData(state, 0x11);
    state.setPortValue(Ttl74368.pinNrToPortNr(Ttl74368.A3), Value.UNKNOWN);
    state.setPortValue(Ttl74368.pinNrToPortNr(Ttl74368.A5), Value.ERROR);
    buffer.propagate(state);

    assertEquals(Value.FALSE, output(state, Ttl74368.Y1));
    assertEquals(Value.TRUE, output(state, Ttl74368.Y2));
    assertEquals(Value.UNKNOWN, output(state, Ttl74368.Y3));
    assertEquals(Value.TRUE, output(state, Ttl74368.Y4));
    assertEquals(Value.ERROR, output(state, Ttl74368.Y5));
    assertEquals(Value.TRUE, output(state, Ttl74368.Y6));
  }

  @Test
  void invalidExposedPowerInputsReleaseEveryOutput() {
    final var buffer = new Ttl74368();
    final var state = new TtlTestInstanceState(buffer, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    setLow(state, Ttl74368.OE1);
    setLow(state, Ttl74368.OE2);
    setData(state, 0x2A);
    buffer.propagate(state);
    assertInverted(state, 0x2A, 0, OUTPUTS.length);

    state.setPortValue(VCC_PORT, Value.FALSE);
    buffer.propagate(state);
    assertReleased(state, 0, OUTPUTS.length);

    state.setPortValue(VCC_PORT, Value.TRUE);
    buffer.propagate(state);
    assertInverted(state, 0x2A, 0, OUTPUTS.length);

    state.setPortValue(GND_PORT, Value.TRUE);
    buffer.propagate(state);
    assertReleased(state, 0, OUTPUTS.length);
  }

  /** Creates a device with both active-low enables asserted. */
  private static TtlTestInstanceState enabled(Ttl74368 buffer) {
    final var state = new TtlTestInstanceState(buffer, false);
    setLow(state, Ttl74368.OE1);
    setLow(state, Ttl74368.OE2);
    return state;
  }

  private static void setData(TtlTestInstanceState state, int pattern) {
    for (var index = 0; index < INPUTS.length; index++) {
      setLevel(state, INPUTS[index], (pattern & (1 << index)) != 0);
    }
  }

  private static void assertInverted(TtlTestInstanceState state, int pattern, int from, int to) {
    for (var index = from; index < to; index++) {
      final var expected = (pattern & (1 << index)) != 0 ? Value.FALSE : Value.TRUE;
      assertEquals(expected, output(state, OUTPUTS[index]));
    }
  }

  private static void assertReleased(TtlTestInstanceState state, int from, int to) {
    for (var index = from; index < to; index++) {
      assertEquals(Value.UNKNOWN, output(state, OUTPUTS[index]));
    }
  }

  private static void assertError(TtlTestInstanceState state, int from, int to) {
    for (var index = from; index < to; index++) {
      assertEquals(Value.ERROR, output(state, OUTPUTS[index]));
    }
  }

  private static Value output(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74368.pinNrToPortNr(dsPinNr));
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74368.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, boolean high) {
    state.setPortValue(Ttl74368.pinNrToPortNr(dsPinNr), high ? Value.TRUE : Value.FALSE);
  }

  private static void setLow(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, false);
  }

  private static void setHigh(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, true);
  }
}
