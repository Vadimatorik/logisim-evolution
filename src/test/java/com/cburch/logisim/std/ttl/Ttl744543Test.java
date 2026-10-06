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
import com.cburch.logisim.prefs.PrefMonitorBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;

/** Function-table tests for the 744543 BCD latch/decoder/driver. */
class Ttl744543Test {
  /** Glyphs in datasheet order a, b, c, d, e, f, g. Codes 10 to 15 are blank. */
  private static final String[] GLYPHS = {
    "1111110",
    "0110000",
    "1101101",
    "1111001",
    "0110011",
    "1011011",
    "1011111",
    "1110000",
    "1111111",
    "1111011",
    "0000000",
    "0000000",
    "0000000",
    "0000000",
    "0000000",
    "0000000"
  };

  private static final byte[] SEGMENTS = {
    Ttl744543.QA,
    Ttl744543.QB,
    Ttl744543.QC,
    Ttl744543.QD,
    Ttl744543.QE,
    Ttl744543.QF,
    Ttl744543.QG
  };

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  private boolean originalStartup;

  @BeforeEach
  void rememberStartup() {
    originalStartup = AppPreferences.Memory_Startup_Unknown.get();
  }

  @AfterEach
  void restoreStartup() throws Exception {
    setStartupUnknown(originalStartup);
  }

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var decoder = new Ttl744543();
    final var hiddenPower = createInstance(decoder, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744543.LD, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744543.D2, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744543.D1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744543.D3, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744543.D0, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744543.PH, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744543.BI, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744543.QA, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744543.QB, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744543.QC, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744543.QD, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744543.QE, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744543.QG, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744543.QF, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(decoder, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void digitsZeroThroughFifteenMatchTheFunctionTable() {
    final var decoder = new Ttl744543();

    for (var code = 0; code < GLYPHS.length; code++) {
      final var state = transparent(decoder);
      setCode(state, code);
      decoder.propagate(state);
      expect(state, GLYPHS[code]);
    }
  }

  @Test
  void phaseInvertsEverySegmentIncludingIllegalCodes() {
    final var decoder = new Ttl744543();

    for (var code = 0; code < GLYPHS.length; code++) {
      final var state = transparent(decoder);
      set(state, Ttl744543.PH, Value.TRUE);
      setCode(state, code);
      decoder.propagate(state);
      expect(state, inverted(GLYPHS[code]));
    }
  }

  @Test
  void blankingOverridesTheCodeAndPhaseInvertsTheBlank() {
    final var decoder = new Ttl744543();
    final var state = transparent(decoder);
    setCode(state, 8);
    set(state, Ttl744543.BI, Value.TRUE);
    decoder.propagate(state);
    expect(state, "0000000");

    set(state, Ttl744543.PH, Value.TRUE);
    decoder.propagate(state);
    expect(state, "1111111");

    set(state, Ttl744543.LD, Value.FALSE);
    setCode(state, 1);
    decoder.propagate(state);
    expect(state, "1111111");
  }

  @Test
  void lowLoadInputHoldsTheCodeWhilePhaseAndBlankingStillApply() {
    final var decoder = new Ttl744543();
    final var state = transparent(decoder);
    setCode(state, 5);
    decoder.propagate(state);
    expect(state, "1011011");

    set(state, Ttl744543.LD, Value.FALSE);
    setCode(state, 2);
    decoder.propagate(state);
    expect(state, "1011011");

    set(state, Ttl744543.PH, Value.TRUE);
    decoder.propagate(state);
    expect(state, "0100100");

    set(state, Ttl744543.BI, Value.TRUE);
    decoder.propagate(state);
    expect(state, "1111111");

    set(state, Ttl744543.BI, Value.FALSE);
    set(state, Ttl744543.PH, Value.FALSE);
    decoder.propagate(state);
    expect(state, "1011011");
  }

  @Test
  void highLoadInputCapturesDuringBlanking() {
    final var decoder = new Ttl744543();
    final var state = transparent(decoder);
    setCode(state, 5);
    decoder.propagate(state);

    set(state, Ttl744543.BI, Value.TRUE);
    setCode(state, 3);
    decoder.propagate(state);
    expect(state, "0000000");

    set(state, Ttl744543.LD, Value.FALSE);
    setCode(state, 9);
    set(state, Ttl744543.BI, Value.FALSE);
    decoder.propagate(state);
    expect(state, "1111001");
  }

  @Test
  void unknownLoadInputHoldsOnlyWhenTheCodeIsUnchanged() {
    final var decoder = new Ttl744543();
    final var state = transparent(decoder);
    setCode(state, 4);
    decoder.propagate(state);

    set(state, Ttl744543.LD, Value.UNKNOWN);
    decoder.propagate(state);
    expect(state, "0110011");

    set(state, Ttl744543.LD, Value.ERROR);
    decoder.propagate(state);
    expect(state, "0110011");

    setCode(state, 7);
    set(state, Ttl744543.LD, Value.UNKNOWN);
    decoder.propagate(state);
    expect(state, "XXXXXXX");

    set(state, Ttl744543.LD, Value.TRUE);
    decoder.propagate(state);
    expect(state, "1110000");

    set(state, Ttl744543.LD, Value.FALSE);
    decoder.propagate(state);
    setCode(state, 1);
    set(state, Ttl744543.LD, Value.ERROR);
    decoder.propagate(state);
    expect(state, "EEEEEEE");
  }

  @Test
  void unknownBcdBitsAffectOnlySegmentsTheyCanChange() {
    final var decoder = new Ttl744543();
    final var state = transparent(decoder);

    set(state, Ttl744543.D3, Value.UNKNOWN);
    decoder.propagate(state);
    expect(state, "111111X");

    setCode(state, 0b1100);
    set(state, Ttl744543.D1, Value.UNKNOWN);
    set(state, Ttl744543.D0, Value.UNKNOWN);
    decoder.propagate(state);
    expect(state, "0000000");

    set(state, Ttl744543.D3, Value.TRUE);
    set(state, Ttl744543.D2, Value.FALSE);
    set(state, Ttl744543.D1, Value.FALSE);
    set(state, Ttl744543.D0, Value.UNKNOWN);
    decoder.propagate(state);
    expect(state, "1111X11");
  }

  @Test
  void unknownBlankAndPhaseBlurOnlySegmentsTheyCanChange() {
    final var decoder = new Ttl744543();
    final var state = transparent(decoder);
    set(state, Ttl744543.BI, Value.UNKNOWN);
    decoder.propagate(state);
    expect(state, "XXXXXX0");

    setCode(state, 8);
    decoder.propagate(state);
    expect(state, "XXXXXXX");

    setCode(state, 0);
    set(state, Ttl744543.BI, Value.FALSE);
    set(state, Ttl744543.PH, Value.UNKNOWN);
    decoder.propagate(state);
    expect(state, "XXXXXXX");

    set(state, Ttl744543.BI, Value.TRUE);
    decoder.propagate(state);
    expect(state, "XXXXXXX");
  }

  @Test
  void errorLevelsAreKeptWhenTheyChangeTheResult() {
    final var decoder = new Ttl744543();
    final var state = transparent(decoder);
    set(state, Ttl744543.PH, Value.ERROR);
    decoder.propagate(state);
    expect(state, "EEEEEEE");

    set(state, Ttl744543.PH, Value.FALSE);
    set(state, Ttl744543.BI, Value.ERROR);
    decoder.propagate(state);
    expect(state, "EEEEEE0");

    set(state, Ttl744543.BI, Value.FALSE);
    set(state, Ttl744543.D0, Value.ERROR);
    decoder.propagate(state);
    expect(state, "E11EEE0");
  }

  @Test
  void startupFollowsTheMemoryPreference() throws Exception {
    final var decoder = new Ttl744543();
    setStartupUnknown(false);
    final var known = held(decoder);
    setCode(known, 9);
    decoder.propagate(known);
    expect(known, "1111110");

    setStartupUnknown(true);
    final var unknown = held(decoder);
    setCode(unknown, 9);
    decoder.propagate(unknown);
    expect(unknown, "XXXXXXX");

    set(unknown, Ttl744543.LD, Value.TRUE);
    decoder.propagate(unknown);
    expect(unknown, "1111011");
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var decoder = new Ttl744543();
    final var state = new TtlTestInstanceState(decoder, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    set(state, Ttl744543.LD, Value.TRUE);
    set(state, Ttl744543.BI, Value.FALSE);
    set(state, Ttl744543.PH, Value.FALSE);
    setCode(state, 6);
    decoder.propagate(state);
    expect(state, "1011111");

    state.setPortValue(VCC_PORT, Value.FALSE);
    decoder.propagate(state);
    expect(state, "XXXXXXX");

    state.setPortValue(VCC_PORT, Value.TRUE);
    decoder.propagate(state);
    expect(state, "1011111");

    state.setPortValue(GND_PORT, Value.TRUE);
    decoder.propagate(state);
    expect(state, "XXXXXXX");
  }

  @Test
  void loadInputIsNotAGatedClock() {
    assertFalse(new Ttl744543().checkForGatedClocks(null));
  }

  private static TtlTestInstanceState transparent(Ttl744543 decoder) {
    final var state = new TtlTestInstanceState(decoder, false);
    set(state, Ttl744543.LD, Value.TRUE);
    set(state, Ttl744543.BI, Value.FALSE);
    set(state, Ttl744543.PH, Value.FALSE);
    setCode(state, 0);
    return state;
  }

  private static TtlTestInstanceState held(Ttl744543 decoder) {
    final var state = transparent(decoder);
    set(state, Ttl744543.LD, Value.FALSE);
    return state;
  }

  private static void setCode(TtlTestInstanceState state, int code) {
    set(state, Ttl744543.D0, bit(code, 0));
    set(state, Ttl744543.D1, bit(code, 1));
    set(state, Ttl744543.D2, bit(code, 2));
    set(state, Ttl744543.D3, bit(code, 3));
  }

  private static Value bit(int code, int index) {
    return ((code >> index) & 1) == 1 ? Value.TRUE : Value.FALSE;
  }

  private static String inverted(String glyph) {
    final var bits = new StringBuilder(glyph.length());
    for (var index = 0; index < glyph.length(); index++) {
      bits.append(glyph.charAt(index) == '1' ? '0' : '1');
    }
    return bits.toString();
  }

  private static void expect(TtlTestInstanceState state, String segments) {
    for (var index = 0; index < SEGMENTS.length; index++) {
      final var actual = state.getPortValue(Ttl744543.pinNrToPortNr(SEGMENTS[index]));
      assertEquals(level(segments.charAt(index)), actual, "segment " + index + " of " + segments);
    }
  }

  private static Value level(char symbol) {
    return switch (symbol) {
      case '1' -> Value.TRUE;
      case '0' -> Value.FALSE;
      case 'X' -> Value.UNKNOWN;
      case 'E' -> Value.ERROR;
      default -> throw new IllegalArgumentException("Bad segment symbol " + symbol);
    };
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl744543.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void set(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl744543.pinNrToPortNr(dsPinNr), value);
  }

  /**
   * Writes the cached startup flag directly. {@code PrefMonitorBoolean.set} only notifies the cache
   * when the stored preference actually changes, so a test cannot force both values through it.
   */
  private static void setStartupUnknown(boolean unknown) throws Exception {
    final Field value = PrefMonitorBoolean.class.getDeclaredField("value");
    value.setAccessible(true);
    value.setBoolean(AppPreferences.Memory_Startup_Unknown, unknown);
  }
}
