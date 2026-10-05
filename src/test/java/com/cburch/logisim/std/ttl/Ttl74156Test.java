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

class Ttl74156Test {
  /** Section 1 outputs ordered from 1Y0 to 1Y3. */
  private static final byte[] SECTION1 = {
    Ttl74156.Y1_0, Ttl74156.Y1_1, Ttl74156.Y1_2, Ttl74156.Y1_3
  };

  /** Section 2 outputs ordered from 2Y0 to 2Y3. */
  private static final byte[] SECTION2 = {
    Ttl74156.Y2_0, Ttl74156.Y2_1, Ttl74156.Y2_2, Ttl74156.Y2_3
  };

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var decoder = new Ttl74156();
    final var hiddenPower = createInstance(decoder, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74156.C1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74156.G1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74156.B, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74156.Y1_3, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74156.Y1_2, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74156.Y1_1, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74156.Y1_0, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74156.Y2_0, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74156.Y2_1, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74156.Y2_2, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74156.Y2_3, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74156.A, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74156.G2, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74156.C2, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(decoder, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void eachSectionPullsDownOnlyItsSelectedOutput() {
    final var decoder = new Ttl74156();

    for (var code = 0; code < 4; code++) {
      final var state = driven(decoder, code, true, false);
      decoder.propagate(state);
      assertSelected(state, SECTION1, code);
      assertReleased(state, SECTION2);

      final var other = driven(decoder, code, false, true);
      decoder.propagate(other);
      assertReleased(other, SECTION1);
      assertSelected(other, SECTION2, code);
    }
  }

  @Test
  void bothSectionsCanSelectTheSameAddressAtOnce() {
    final var decoder = new Ttl74156();
    final var state = driven(decoder, 2, true, true);
    decoder.propagate(state);

    assertSelected(state, SECTION1, 2);
    assertSelected(state, SECTION2, 2);
  }

  @Test
  void theWrongEnablePolarityReleasesThatSection() {
    final var decoder = new Ttl74156();

    final var section1StrobeHigh = driven(decoder, 0, true, true);
    setHigh(section1StrobeHigh, Ttl74156.G1);
    decoder.propagate(section1StrobeHigh);
    assertReleased(section1StrobeHigh, SECTION1);
    assertSelected(section1StrobeHigh, SECTION2, 0);

    final var section1DataLow = driven(decoder, 1, true, true);
    setLow(section1DataLow, Ttl74156.C1);
    decoder.propagate(section1DataLow);
    assertReleased(section1DataLow, SECTION1);
    assertSelected(section1DataLow, SECTION2, 1);

    final var section2DataHigh = driven(decoder, 3, true, true);
    setHigh(section2DataHigh, Ttl74156.C2);
    decoder.propagate(section2DataHigh);
    assertSelected(section2DataHigh, SECTION1, 3);
    assertReleased(section2DataHigh, SECTION2);
  }

  @Test
  void anUnknownAddressReleasesBothSections() {
    final var decoder = new Ttl74156();
    final var state = driven(decoder, 0, true, true);
    state.setPortValue(Ttl74156.pinNrToPortNr(Ttl74156.A), Value.UNKNOWN);
    decoder.propagate(state);

    assertReleased(state, SECTION1);
    assertReleased(state, SECTION2);
  }

  @Test
  void anUnknownEnableReleasesOnlyItsOwnSection() {
    final var decoder = new Ttl74156();
    final var state = driven(decoder, 1, true, true);
    state.setPortValue(Ttl74156.pinNrToPortNr(Ttl74156.C1), Value.ERROR);
    decoder.propagate(state);

    assertReleased(state, SECTION1);
    assertSelected(state, SECTION2, 1);
  }

  @Test
  void tiedEnablesDecodeThreeBitsIntoEightOpenCollectorOutputs() {
    final var decoder = new Ttl74156();

    for (var code = 0; code < 8; code++) {
      final var state = new TtlTestInstanceState(decoder, false);
      final var highOrder = code >= 4;
      setAddress(state, code & 0x3);
      setLow(state, Ttl74156.G1);
      setLow(state, Ttl74156.G2);
      setLevel(state, Ttl74156.C1, highOrder);
      setLevel(state, Ttl74156.C2, highOrder);
      decoder.propagate(state);

      if (highOrder) {
        assertSelected(state, SECTION1, code & 0x3);
        assertReleased(state, SECTION2);
      } else {
        assertReleased(state, SECTION1);
        assertSelected(state, SECTION2, code & 0x3);
      }
    }
  }

  @Test
  void invalidExposedPowerInputsReleaseEveryOutput() {
    final var decoder = new Ttl74156();
    final var state = driven(decoder, 0, true, true, true);
    decoder.propagate(state);
    assertSelected(state, SECTION1, 0);
    assertSelected(state, SECTION2, 0);

    state.setPortValue(VCC_PORT, Value.FALSE);
    decoder.propagate(state);
    assertReleased(state, SECTION1);
    assertReleased(state, SECTION2);

    state.setPortValue(VCC_PORT, Value.TRUE);
    decoder.propagate(state);
    assertSelected(state, SECTION1, 0);
    assertSelected(state, SECTION2, 0);

    state.setPortValue(GND_PORT, Value.TRUE);
    decoder.propagate(state);
    assertReleased(state, SECTION1);
    assertReleased(state, SECTION2);
  }

  /**
   * Drives a known address and chooses which sections are enabled.
   *
   * <p>When power pins are hidden, the returned state leaves them unconnected.
   */
  private static TtlTestInstanceState driven(
      Ttl74156 decoder, int code, boolean section1, boolean section2) {
    return driven(decoder, code, section1, section2, false);
  }

  private static TtlTestInstanceState driven(
      Ttl74156 decoder, int code, boolean section1, boolean section2, boolean showPower) {
    final var state = new TtlTestInstanceState(decoder, showPower);
    setAddress(state, code);
    setLevel(state, Ttl74156.G1, !section1);
    setLevel(state, Ttl74156.C1, section1);
    setLevel(state, Ttl74156.G2, !section2);
    setLevel(state, Ttl74156.C2, !section2);
    if (showPower) {
      state.setPortValue(GND_PORT, Value.FALSE);
      state.setPortValue(VCC_PORT, Value.TRUE);
    }
    return state;
  }

  private static void setAddress(TtlTestInstanceState state, int code) {
    setLevel(state, Ttl74156.A, (code & 1) != 0);
    setLevel(state, Ttl74156.B, (code & 2) != 0);
  }

  private static void assertSelected(TtlTestInstanceState state, byte[] outputs, int selected) {
    for (var index = 0; index < outputs.length; index++) {
      final var expected = index == selected ? Value.FALSE : Value.UNKNOWN;
      assertEquals(expected, state.getPortValue(Ttl74156.pinNrToPortNr(outputs[index])));
    }
  }

  private static void assertReleased(TtlTestInstanceState state, byte[] outputs) {
    for (final var output : outputs) {
      assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74156.pinNrToPortNr(output)));
    }
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74156.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, boolean high) {
    state.setPortValue(Ttl74156.pinNrToPortNr(dsPinNr), high ? Value.TRUE : Value.FALSE);
  }

  private static void setLow(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, false);
  }

  private static void setHigh(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, true);
  }
}
