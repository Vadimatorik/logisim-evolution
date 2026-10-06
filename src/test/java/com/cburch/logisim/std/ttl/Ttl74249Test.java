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

/** Functional tests for the 74249 BCD-to-seven-segment decoder. */
class Ttl74249Test {
  /** Active-high glyphs, bit 0 = segment a. Six and nine include tails. */
  private static final int[] GLYPHS = {
    0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07,
    0x7F, 0x6F, 0x58, 0x4C, 0x62, 0x69, 0x78, 0x00
  };

  private static final byte[] BCD_PINS = {Ttl74249.A, Ttl74249.B, Ttl74249.C, Ttl74249.D};

  private static final byte[] SEGMENT_PINS = {
    Ttl74249.SEG_A, Ttl74249.SEG_B, Ttl74249.SEG_C, Ttl74249.SEG_D,
    Ttl74249.SEG_E, Ttl74249.SEG_F, Ttl74249.SEG_G
  };

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;
  private static final int ALL_SEGMENTS = 0x7F;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var decoder = new Ttl74249();
    final var hiddenPower = createInstance(decoder, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74249.B, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74249.C, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74249.LT, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74249.BI_RBO, 70, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74249.RBI, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74249.D, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74249.A, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74249.SEG_E, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74249.SEG_D, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74249.SEG_C, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74249.SEG_B, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74249.SEG_A, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74249.SEG_G, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74249.SEG_F, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(decoder, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void glyphsFollowTheDatasheetIncludingTailsOnSixAndNine() {
    final var decoder = new Ttl74249();

    for (var code = 0; code < GLYPHS.length; code++) {
      final var state = released(decoder);
      setCode(state, code);
      decoder.propagate(state);

      expectGlyph(state, GLYPHS[code]);
      expectRbo(state, Value.UNKNOWN);
    }
  }

  @Test
  void blankingInputOverridesLampTest() {
    final var decoder = new Ttl74249();
    final var state = released(decoder);
    setCode(state, 8);
    setLow(state, Ttl74249.LT);
    setLow(state, Ttl74249.BI_RBO);
    decoder.propagate(state);

    expectGlyph(state, 0);
    expectRbo(state, Value.UNKNOWN);
  }

  @Test
  void lampTestOverridesRippleBlanking() {
    final var decoder = new Ttl74249();
    final var state = released(decoder);
    setCode(state, 0);
    setLow(state, Ttl74249.RBI);
    setLow(state, Ttl74249.LT);
    decoder.propagate(state);

    expectGlyph(state, ALL_SEGMENTS);
    expectRbo(state, Value.UNKNOWN);
  }

  @Test
  void rippleBlankingClearsOnlyZeroAndDrivesRbo() {
    final var decoder = new Ttl74249();
    final var blanked = released(decoder);
    setCode(blanked, 0);
    setLow(blanked, Ttl74249.RBI);
    decoder.propagate(blanked);
    expectGlyph(blanked, 0);
    expectRbo(blanked, Value.FALSE);

    final var shown = released(decoder);
    setCode(shown, 1);
    setLow(shown, Ttl74249.RBI);
    decoder.propagate(shown);
    expectGlyph(shown, GLYPHS[1]);
    expectRbo(shown, Value.UNKNOWN);
  }

  @Test
  void unknownLevelsAreNotActiveLow() {
    final var decoder = new Ttl74249();

    final var unknownRbi = released(decoder);
    setCode(unknownRbi, 0);
    setLevel(unknownRbi, Ttl74249.RBI, Value.UNKNOWN);
    decoder.propagate(unknownRbi);
    expectGlyph(unknownRbi, GLYPHS[0]);
    expectRbo(unknownRbi, Value.UNKNOWN);

    final var unknownLt = released(decoder);
    setCode(unknownLt, 0);
    setLevel(unknownLt, Ttl74249.LT, Value.UNKNOWN);
    decoder.propagate(unknownLt);
    expectGlyph(unknownLt, GLYPHS[0]);

    final var unknownBi = released(decoder);
    setCode(unknownBi, 5);
    setLevel(unknownBi, Ttl74249.BI_RBO, Value.UNKNOWN);
    decoder.propagate(unknownBi);
    expectGlyph(unknownBi, GLYPHS[5]);
  }

  @Test
  void unknownBcdLeavesSegmentsUnknown() {
    final var decoder = new Ttl74249();
    final var state = released(decoder);
    setCode(state, 0);
    setLevel(state, Ttl74249.A, Value.UNKNOWN);
    decoder.propagate(state);
    expectUnknownSegments(state);
    expectRbo(state, Value.UNKNOWN);

    final var error = released(decoder);
    setCode(error, 0);
    setLevel(error, Ttl74249.B, Value.ERROR);
    decoder.propagate(error);
    expectUnknownSegments(error);
    expectRbo(error, Value.UNKNOWN);
  }

  @Test
  void invalidExposedPowerInputsMakeSegmentsUnknown() {
    final var decoder = new Ttl74249();
    final var state = released(decoder, true);
    setCode(state, 8);
    decoder.propagate(state);
    expectGlyph(state, GLYPHS[8]);

    state.setPortValue(VCC_PORT, Value.FALSE);
    decoder.propagate(state);
    expectUnknownSegments(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    decoder.propagate(state);
    expectGlyph(state, GLYPHS[8]);

    state.setPortValue(GND_PORT, Value.TRUE);
    decoder.propagate(state);
    expectUnknownSegments(state);
  }

  /** Controls are inactive and the blanking pin is held high, so a code can be decoded. */
  private static TtlTestInstanceState released(Ttl74249 decoder) {
    return released(decoder, false);
  }

  private static TtlTestInstanceState released(Ttl74249 decoder, boolean showPower) {
    final var state = new TtlTestInstanceState(decoder, showPower);
    if (showPower) {
      state.setPortValue(GND_PORT, Value.FALSE);
      state.setPortValue(VCC_PORT, Value.TRUE);
    }
    setHigh(state, Ttl74249.LT);
    setHigh(state, Ttl74249.RBI);
    setHigh(state, Ttl74249.BI_RBO);
    return state;
  }

  private static void setCode(TtlTestInstanceState state, int code) {
    for (var bit = 0; bit < BCD_PINS.length; bit++) {
      setLevel(state, BCD_PINS[bit], (code & (1 << bit)) != 0 ? Value.TRUE : Value.FALSE);
    }
  }

  private static void expectGlyph(TtlTestInstanceState state, int mask) {
    for (var bit = 0; bit < SEGMENT_PINS.length; bit++) {
      assertEquals(bitValue(mask, bit), level(state, SEGMENT_PINS[bit]), "segment " + bit);
    }
  }

  private static void expectUnknownSegments(TtlTestInstanceState state) {
    for (final var pin : SEGMENT_PINS) {
      assertEquals(Value.UNKNOWN, level(state, pin));
    }
  }

  private static void expectRbo(TtlTestInstanceState state, Value expected) {
    assertEquals(expected, level(state, Ttl74249.BI_RBO));
  }

  private static Value bitValue(int mask, int bit) {
    return (mask & (1 << bit)) != 0 ? Value.TRUE : Value.FALSE;
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74249.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLow(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, Value.FALSE);
  }

  private static void setHigh(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, Value.TRUE);
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl74249.pinNrToPortNr(dsPinNr), value);
  }

  private static Value level(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74249.pinNrToPortNr(dsPinNr));
  }
}
