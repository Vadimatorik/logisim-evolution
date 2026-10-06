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

/** Functional tests for the 74x247 BCD to 7-segment decoder/driver. */
class Ttl74247Test {
  /** Lit-segment masks. Bit 0 is segment a and bit 6 is segment g. */
  private static final int[] GLYPH = {
    0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07,
    0x7F, 0x6F, 0x58, 0x4C, 0x62, 0x69, 0x78, 0x00
  };
  private static final byte[] SEGMENTS = {
    Ttl74247.SEGA,
    Ttl74247.SEGB,
    Ttl74247.SEGC,
    Ttl74247.SEGD,
    Ttl74247.SEGE,
    Ttl74247.SEGF,
    Ttl74247.SEGG
  };
  private static final byte[] DATA = {Ttl74247.A, Ttl74247.B, Ttl74247.C, Ttl74247.D};
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;
  private static final int SEGMENT_A = 0;
  private static final int SEGMENT_B = 1;
  private static final int SEGMENT_C = 2;
  private static final int SEGMENT_D = 3;
  private static final int SEGMENT_E = 4;
  private static final int SEGMENT_F = 5;
  private static final int SEGMENT_G = 6;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74247();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74247.B, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74247.C, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74247.LT, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74247.BI_RBO, 70, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl74247.RBI, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74247.D, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74247.A, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74247.SEGE, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74247.SEGD, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74247.SEGC, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74247.SEGB, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74247.SEGA, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74247.SEGG, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74247.SEGF, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void everyCodeProducesItsGlyphWithTailsOnSixAndNine() {
    final var gate = new Ttl74247();
    final var state = new TtlTestInstanceState(gate, false);

    for (var code = 0; code < GLYPH.length; code++) {
      present(gate, state, code, Value.TRUE, Value.TRUE, Value.TRUE);
      assertGlyph(state, GLYPH[code]);
      assertEquals(Value.UNKNOWN, level(state, Ttl74247.BI_RBO));
    }
  }

  @Test
  void sixAndNineKeepTheTailSegmentsThat7447Omits() {
    final var gate = new Ttl74247();
    final var state = new TtlTestInstanceState(gate, false);

    present(gate, state, 6, Value.TRUE, Value.TRUE, Value.TRUE);
    assertEquals(Value.FALSE, segment(state, SEGMENT_A));
    assertEquals(Value.TRUE, segment(state, SEGMENT_B));

    present(gate, state, 9, Value.TRUE, Value.TRUE, Value.TRUE);
    assertEquals(Value.FALSE, segment(state, SEGMENT_D));
    assertEquals(Value.TRUE, segment(state, SEGMENT_E));
  }

  @Test
  void blankingInputOverridesLampTest() {
    final var gate = new Ttl74247();
    final var state = new TtlTestInstanceState(gate, false);

    present(gate, state, 8, Value.FALSE, Value.TRUE, Value.FALSE);
    assertGlyph(state, 0x00);

    present(gate, state, 8, Value.FALSE, Value.TRUE, Value.TRUE);
    assertGlyph(state, 0x7F);
    assertEquals(Value.UNKNOWN, level(state, Ttl74247.BI_RBO));
  }

  @Test
  void rippleBlankingClearsOnlyZeroAndDrivesTheSharedPinLow() {
    final var gate = new Ttl74247();
    final var state = new TtlTestInstanceState(gate, false);

    present(gate, state, 0, Value.TRUE, Value.FALSE, Value.TRUE);
    assertGlyph(state, 0x00);
    assertEquals(Value.FALSE, level(state, Ttl74247.BI_RBO));

    present(gate, state, 5, Value.TRUE, Value.FALSE, Value.TRUE);
    assertGlyph(state, GLYPH[5]);
    assertEquals(Value.UNKNOWN, level(state, Ttl74247.BI_RBO));

    present(gate, state, 0, Value.TRUE, Value.TRUE, Value.TRUE);
    assertGlyph(state, GLYPH[0]);
    assertEquals(Value.UNKNOWN, level(state, Ttl74247.BI_RBO));
  }

  @Test
  void lampTestDuringRippleBlankingLightsEverySegmentAndReleasesThePin() {
    final var gate = new Ttl74247();
    final var state = new TtlTestInstanceState(gate, false);

    present(gate, state, 0, Value.FALSE, Value.FALSE, Value.TRUE);
    assertGlyph(state, 0x7F);
    assertEquals(Value.UNKNOWN, level(state, Ttl74247.BI_RBO));
  }

  @Test
  void rippleBlankingOutputCascadesIntoTheNextRippleBlankingInput() {
    final var gate = new Ttl74247();
    final var leading = new TtlTestInstanceState(gate, false);
    final var trailing = new TtlTestInstanceState(gate, false);

    present(gate, leading, 0, Value.TRUE, Value.FALSE, Value.TRUE);
    assertEquals(Value.FALSE, level(leading, Ttl74247.BI_RBO));

    present(gate, trailing, 0, Value.TRUE, level(leading, Ttl74247.BI_RBO), Value.TRUE);
    assertGlyph(trailing, 0x00);
    assertEquals(Value.FALSE, level(trailing, Ttl74247.BI_RBO));

    present(gate, leading, 5, Value.TRUE, Value.TRUE, Value.TRUE);
    assertGlyph(leading, GLYPH[5]);
    present(gate, trailing, 3, Value.TRUE, Value.TRUE, Value.TRUE);
    assertGlyph(trailing, GLYPH[3]);
    assertEquals(Value.UNKNOWN, level(trailing, Ttl74247.BI_RBO));
  }

  @Test
  void anUnknownBcdBitLeavesOnlyTheAgreedSegmentsDefined() {
    final var gate = new Ttl74247();
    final var state = new TtlTestInstanceState(gate, false);

    setBit(state, Ttl74247.A, Value.UNKNOWN);
    setBit(state, Ttl74247.B, Value.FALSE);
    setBit(state, Ttl74247.C, Value.FALSE);
    setBit(state, Ttl74247.D, Value.FALSE);
    presentControls(state, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);

    assertEquals(Value.FALSE, segment(state, SEGMENT_B));
    assertEquals(Value.FALSE, segment(state, SEGMENT_C));
    assertEquals(Value.TRUE, segment(state, SEGMENT_G));
    assertEquals(Value.UNKNOWN, segment(state, SEGMENT_A));
    assertEquals(Value.UNKNOWN, segment(state, SEGMENT_D));
    assertEquals(Value.UNKNOWN, segment(state, SEGMENT_E));
    assertEquals(Value.UNKNOWN, segment(state, SEGMENT_F));
  }

  @Test
  void anErrorOnABcdBitMakesDisagreementsAnError() {
    final var gate = new Ttl74247();
    final var state = new TtlTestInstanceState(gate, false);

    setBit(state, Ttl74247.A, Value.ERROR);
    setBit(state, Ttl74247.B, Value.FALSE);
    setBit(state, Ttl74247.C, Value.FALSE);
    setBit(state, Ttl74247.D, Value.FALSE);
    presentControls(state, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);

    assertEquals(Value.FALSE, segment(state, SEGMENT_B));
    assertEquals(Value.FALSE, segment(state, SEGMENT_C));
    assertEquals(Value.TRUE, segment(state, SEGMENT_G));
    assertEquals(Value.ERROR, segment(state, SEGMENT_A));
  }

  @Test
  void invalidExposedPowerInputsMakeSegmentOutputsUnknown() {
    final var gate = new Ttl74247();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    present(gate, state, 8, Value.TRUE, Value.TRUE, Value.TRUE);
    assertGlyph(state, GLYPH[8]);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    for (final var pin : SEGMENTS) {
      assertEquals(Value.UNKNOWN, level(state, pin));
    }

    state.setPortValue(VCC_PORT, Value.TRUE);
    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    for (final var pin : SEGMENTS) {
      assertEquals(Value.UNKNOWN, level(state, pin));
    }
  }

  private static void present(
      Ttl74247 gate, TtlTestInstanceState state, int code, Value lampTest, Value rippleBlanking,
      Value blanking) {
    for (var place = 0; place < DATA.length; place++) {
      final var high = ((code >> place) & 1) == 1;
      setBit(state, DATA[place], high ? Value.TRUE : Value.FALSE);
    }
    presentControls(state, lampTest, rippleBlanking, blanking);
    gate.propagate(state);
  }

  private static void presentControls(
      TtlTestInstanceState state, Value lampTest, Value rippleBlanking, Value blanking) {
    setBit(state, Ttl74247.LT, lampTest);
    setBit(state, Ttl74247.RBI, rippleBlanking);
    setBit(state, Ttl74247.BI_RBO, blanking);
  }

  private static void assertGlyph(TtlTestInstanceState state, int lit) {
    for (var segment = 0; segment < SEGMENTS.length; segment++) {
      final var on = ((lit >> segment) & 1) == 1;
      assertEquals(on ? Value.FALSE : Value.TRUE, segment(state, segment));
    }
  }

  private static Value segment(TtlTestInstanceState state, int segment) {
    return level(state, SEGMENTS[segment]);
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74247.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setBit(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl74247.pinNrToPortNr(dsPinNr), value);
  }

  private static Value level(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74247.pinNrToPortNr(dsPinNr));
  }
}
