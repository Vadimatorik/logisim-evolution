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
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.prefs.AppPreferences;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC4511 BCD to 7-segment latch/decoder/driver. */
class Ttl744511Test {
  /** Glyph bit 0 is segment a and bit 6 is segment g. */
  private static final int[] GLYPH = {
    0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7C, 0x07,
    0x7F, 0x67, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
  };
  private static final byte[] SEGMENTS = {
    Ttl744511.SEGA,
    Ttl744511.SEGB,
    Ttl744511.SEGC,
    Ttl744511.SEGD,
    Ttl744511.SEGE,
    Ttl744511.SEGF,
    Ttl744511.SEGG
  };
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl744511();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744511.B, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744511.C, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744511.LT, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744511.BI, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744511.LE, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744511.D, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744511.A, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744511.SEGE, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744511.SEGD, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744511.SEGC, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744511.SEGB, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744511.SEGA, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744511.SEGG, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744511.SEGF, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void latchEnableIsNotAnEdgeClock() {
    assertFalse(new Ttl744511().checkForGatedClocks(null));
  }

  @Test
  void transparentModeDecodesDigitsAndBlanksInvalidCodes() {
    final var gate = new Ttl744511();
    final var state = new TtlTestInstanceState(gate, false);

    for (var code = 0; code < GLYPH.length; code++) {
      follow(gate, state, code);
      assertGlyph(state, GLYPH[code]);
    }
  }

  @Test
  void lampTestOverridesBlankingAndTheStoredCode() {
    final var gate = new Ttl744511();
    final var state = new TtlTestInstanceState(gate, false);
    follow(gate, state, 0);

    drive(state, 0, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertGlyph(state, 0x7F);

    drive(state, 4, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertGlyph(state, GLYPH[0]);
  }

  @Test
  void blankingClearsSegmentsWithoutChangingTheLatch() {
    final var gate = new Ttl744511();
    final var state = new TtlTestInstanceState(gate, false);
    follow(gate, state, 8);

    drive(state, 8, Value.TRUE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    assertGlyph(state, 0x00);

    drive(state, 1, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertGlyph(state, GLYPH[8]);
  }

  @Test
  void highLatchEnableHoldsTheCodeWhileInputsChange() {
    final var gate = new Ttl744511();
    final var state = new TtlTestInstanceState(gate, false);
    follow(gate, state, 5);

    drive(state, 0, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    drive(state, 9, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertGlyph(state, GLYPH[5]);

    follow(gate, state, 2);
    assertGlyph(state, GLYPH[2]);
  }

  @Test
  void latchedBlankStaysBlankUntilNewDataIsAdmitted() {
    final var gate = new Ttl744511();
    final var state = new TtlTestInstanceState(gate, false);
    follow(gate, state, 0xA);

    drive(state, 3, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertGlyph(state, 0x00);

    drive(state, 3, Value.TRUE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    drive(state, 3, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertGlyph(state, 0x00);

    follow(gate, state, 3);
    assertGlyph(state, GLYPH[3]);
  }

  @Test
  void unknownLatchEnableDoesNotWrite() {
    final var gate = new Ttl744511();
    final var state = new TtlTestInstanceState(gate, false);
    follow(gate, state, 4);

    drive(state, 9, Value.UNKNOWN, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    drive(state, 1, Value.ERROR, Value.TRUE, Value.TRUE);
    gate.propagate(state);

    assertGlyph(state, GLYPH[4]);
  }

  @Test
  void unknownBcdBitAffectsOnlyTheSegmentsThatDependOnIt() {
    final var gate = new Ttl744511();
    final var state = new TtlTestInstanceState(gate, false);
    follow(gate, state, 0);
    set(state, Ttl744511.A, Value.UNKNOWN);
    gate.propagate(state);

    assertSegment(state, 0, Value.UNKNOWN);
    assertSegment(state, 1, Value.TRUE);
    assertSegment(state, 2, Value.TRUE);
    assertSegment(state, 3, Value.UNKNOWN);
    assertSegment(state, 4, Value.UNKNOWN);
    assertSegment(state, 5, Value.UNKNOWN);
    assertSegment(state, 6, Value.FALSE);

    set(state, Ttl744511.A, Value.ERROR);
    gate.propagate(state);
    assertSegment(state, 0, Value.ERROR);
    assertSegment(state, 1, Value.TRUE);
    assertSegment(state, 6, Value.FALSE);
  }

  @Test
  void uncertainControlsConflictOnlyWhereTheyChangeTheSegments() {
    final var gate = new Ttl744511();
    final var state = new TtlTestInstanceState(gate, false);
    follow(gate, state, 0);

    set(state, Ttl744511.LT, Value.UNKNOWN);
    gate.propagate(state);
    assertSegment(state, 0, Value.TRUE);
    assertSegment(state, 6, Value.UNKNOWN);

    set(state, Ttl744511.LT, Value.ERROR);
    gate.propagate(state);
    assertSegment(state, 6, Value.ERROR);

    follow(gate, state, 8);
    set(state, Ttl744511.BI, Value.UNKNOWN);
    gate.propagate(state);
    assertSegment(state, 0, Value.UNKNOWN);

    follow(gate, state, 0xF);
    set(state, Ttl744511.BI, Value.UNKNOWN);
    gate.propagate(state);
    assertGlyph(state, 0x00);

    set(state, Ttl744511.BI, Value.ERROR);
    gate.propagate(state);
    assertGlyph(state, 0x00);
  }

  @Test
  void startupFollowsTheMemoryPreference() {
    final var prefs = AppPreferences.getPrefs();
    final var stored = prefs.getBoolean("MemStartUnknown", false);
    try {
      // The monitor updates its cache from a preference event, so write the node directly.
      prefs.putBoolean("MemStartUnknown", false);
      final var cleared = new Ttl744511();
      final var clearedState = new TtlTestInstanceState(cleared, false);
      drive(clearedState, 7, Value.TRUE, Value.TRUE, Value.TRUE);
      cleared.propagate(clearedState);
      assertGlyph(clearedState, GLYPH[0]);

      prefs.putBoolean("MemStartUnknown", true);
      final var unknown = new Ttl744511();
      final var unknownState = new TtlTestInstanceState(unknown, false);
      drive(unknownState, 0, Value.TRUE, Value.TRUE, Value.TRUE);
      unknown.propagate(unknownState);
      for (var segment = 0; segment < SEGMENTS.length; segment++) {
        assertSegment(unknownState, segment, Value.UNKNOWN);
      }
    } finally {
      prefs.putBoolean("MemStartUnknown", stored);
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl744511();
    final var state = new TtlTestInstanceState(gate, true);
    power(state, Value.FALSE, Value.TRUE);
    follow(gate, state, 5);
    drive(state, 5, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertGlyph(state, GLYPH[5]);

    power(state, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertAll(state, Value.UNKNOWN);

    power(state, Value.FALSE, Value.TRUE);
    drive(state, 0, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertGlyph(state, GLYPH[5]);

    power(state, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertAll(state, Value.UNKNOWN);
  }

  private static void follow(Ttl744511 gate, TtlTestInstanceState state, int code) {
    drive(state, code, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
  }

  private static void drive(
      TtlTestInstanceState state, int code, Value latchEnable, Value lampTest, Value blanking) {
    set(state, Ttl744511.A, bit(code, 0));
    set(state, Ttl744511.B, bit(code, 1));
    set(state, Ttl744511.C, bit(code, 2));
    set(state, Ttl744511.D, bit(code, 3));
    set(state, Ttl744511.LE, latchEnable);
    set(state, Ttl744511.LT, lampTest);
    set(state, Ttl744511.BI, blanking);
  }

  private static Value bit(int code, int place) {
    return ((code >> place) & 1) == 1 ? Value.TRUE : Value.FALSE;
  }

  private static void power(TtlTestInstanceState state, Value ground, Value supply) {
    state.setPortValue(GND_PORT, ground);
    state.setPortValue(VCC_PORT, supply);
  }

  private static void assertGlyph(TtlTestInstanceState state, int mask) {
    for (var segment = 0; segment < SEGMENTS.length; segment++) {
      assertSegment(state, segment, bit(mask, segment));
    }
  }

  private static void assertAll(TtlTestInstanceState state, Value expected) {
    for (var segment = 0; segment < SEGMENTS.length; segment++) {
      assertSegment(state, segment, expected);
    }
  }

  private static void assertSegment(TtlTestInstanceState state, int segment, Value expected) {
    assertEquals(
        expected,
        state.getPortValue(Ttl744511.pinNrToPortNr(SEGMENTS[segment])),
        "segment " + segment);
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl744511.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void set(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl744511.pinNrToPortNr(dsPinNr), value);
  }
}
