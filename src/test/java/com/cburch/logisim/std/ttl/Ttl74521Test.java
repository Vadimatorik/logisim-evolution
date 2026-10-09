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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

class Ttl74521Test {
  private static final byte[] WORD_P = {
    Ttl74521.P0, Ttl74521.P1, Ttl74521.P2, Ttl74521.P3,
    Ttl74521.P4, Ttl74521.P5, Ttl74521.P6, Ttl74521.P7
  };
  private static final byte[] WORD_Q = {
    Ttl74521.Q0, Ttl74521.Q1, Ttl74521.Q2, Ttl74521.Q3,
    Ttl74521.Q4, Ttl74521.Q5, Ttl74521.Q6, Ttl74521.Q7
  };

  private static final int GND_PORT = 18;
  private static final int VCC_PORT = 19;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var comparator = new Ttl74521();
    final var hiddenPower = createInstance(comparator, false);

    assertEquals(18, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74521.E, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74521.P0, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74521.Q0, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74521.P1, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74521.Q1, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74521.P2, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74521.Q2, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74521.P3, 150, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74521.Q3, 170, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74521.P4, 190, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74521.Q4, 170, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74521.P5, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74521.Q5, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74521.P6, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74521.Q6, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74521.P7, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74521.Q7, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74521.PQ, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(comparator, true);
    assertEquals(20, shownPower.getPorts().size());
    assertEquals(Location.create(190, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void equalWordsDriveTheOutputLow() {
    final var comparator = new Ttl74521();
    final int[] patterns = {0x00, 0xFF, 0x55, 0xAA};

    for (final var pattern : patterns) {
      final var state = enabled(comparator);
      setWords(state, pattern, pattern);
      comparator.propagate(state);
      assertTrue(isLow(state, Ttl74521.PQ));
    }

    for (var bit = 0; bit < 8; bit++) {
      final var state = enabled(comparator);
      final var pattern = 1 << bit;
      setWords(state, pattern, pattern);
      comparator.propagate(state);
      assertTrue(isLow(state, Ttl74521.PQ));
    }
  }

  @Test
  void differingBitOrMagnitudeDrivesTheOutputHigh() {
    final var comparator = new Ttl74521();

    for (var bit = 0; bit < 8; bit++) {
      final var state = enabled(comparator);
      setWords(state, 0xA5, 0xA5 ^ (1 << bit));
      comparator.propagate(state);
      assertTrue(isHigh(state, Ttl74521.PQ));
    }

    final var greater = enabled(comparator);
    setWords(greater, 0xF0, 0x0F);
    comparator.propagate(greater);
    assertTrue(isHigh(greater, Ttl74521.PQ));

    final var less = enabled(comparator);
    setWords(less, 0x0F, 0xF0);
    comparator.propagate(less);
    assertTrue(isHigh(less, Ttl74521.PQ));
  }

  @Test
  void highEnableForcesTheOutputHigh() {
    final var comparator = new Ttl74521();

    final var equal = enabled(comparator);
    setHigh(equal, Ttl74521.E);
    setWords(equal, 0x55, 0x55);
    comparator.propagate(equal);
    assertTrue(isHigh(equal, Ttl74521.PQ));

    final var different = enabled(comparator);
    setHigh(different, Ttl74521.E);
    setWords(different, 0x55, 0xAA);
    comparator.propagate(different);
    assertTrue(isHigh(different, Ttl74521.PQ));
  }

  @Test
  void unknownLevelsAreNotAMatch() {
    final var comparator = new Ttl74521();

    final var unknownBit = enabled(comparator);
    setWords(unknownBit, 0x00, 0x00);
    unknownBit.setPortValue(Ttl74521.pinNrToPortNr(Ttl74521.P3), Value.UNKNOWN);
    comparator.propagate(unknownBit);
    assertTrue(isHigh(unknownBit, Ttl74521.PQ));

    final var errorBit = enabled(comparator);
    setWords(errorBit, 0xFF, 0xFF);
    errorBit.setPortValue(Ttl74521.pinNrToPortNr(Ttl74521.Q1), Value.ERROR);
    comparator.propagate(errorBit);
    assertTrue(isHigh(errorBit, Ttl74521.PQ));

    final var unknownEnable = enabled(comparator);
    setWords(unknownEnable, 0x55, 0x55);
    unknownEnable.setPortValue(Ttl74521.pinNrToPortNr(Ttl74521.E), Value.UNKNOWN);
    comparator.propagate(unknownEnable);
    assertTrue(isHigh(unknownEnable, Ttl74521.PQ));

    final var floating = new TtlTestInstanceState(comparator, false);
    comparator.propagate(floating);
    assertTrue(isHigh(floating, Ttl74521.PQ));
  }

  @Test
  void equalOutputCascadesIntoTheNextEnable() {
    final var comparator = new Ttl74521();
    final var highOrder = enabled(comparator);
    final var lowOrder = enabled(comparator);
    setWords(highOrder, 0x3C, 0x3C);
    setWords(lowOrder, 0x0F, 0x0F);

    comparator.propagate(highOrder);
    assertTrue(isLow(highOrder, Ttl74521.PQ));
    cascade(highOrder, lowOrder);
    comparator.propagate(lowOrder);
    assertTrue(isLow(lowOrder, Ttl74521.PQ));

    setWords(highOrder, 0x3C, 0x3D);
    comparator.propagate(highOrder);
    assertTrue(isHigh(highOrder, Ttl74521.PQ));
    cascade(highOrder, lowOrder);
    comparator.propagate(lowOrder);
    assertTrue(isHigh(lowOrder, Ttl74521.PQ));
  }

  @Test
  void invalidExposedPowerInputsMakeTheOutputUnknown() {
    final var comparator = new Ttl74521();
    final var state = new TtlTestInstanceState(comparator, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    setLow(state, Ttl74521.E);
    setWords(state, 0x55, 0x55);
    comparator.propagate(state);
    assertTrue(isLow(state, Ttl74521.PQ));

    state.setPortValue(VCC_PORT, Value.FALSE);
    comparator.propagate(state);
    assertEquals(Value.UNKNOWN, port(state, Ttl74521.PQ));

    state.setPortValue(VCC_PORT, Value.TRUE);
    comparator.propagate(state);
    assertTrue(isLow(state, Ttl74521.PQ));

    state.setPortValue(GND_PORT, Value.TRUE);
    comparator.propagate(state);
    assertEquals(Value.UNKNOWN, port(state, Ttl74521.PQ));
  }

  /** Creates an enabled comparator. Data pins stay unknown until a test sets them. */
  private static TtlTestInstanceState enabled(Ttl74521 comparator) {
    final var state = new TtlTestInstanceState(comparator, false);
    setLow(state, Ttl74521.E);
    return state;
  }

  private static void setWords(TtlTestInstanceState state, int wordP, int wordQ) {
    for (var bit = 0; bit < 8; bit++) {
      setLevel(state, WORD_P[bit], ((wordP >> bit) & 1) == 1);
      setLevel(state, WORD_Q[bit], ((wordQ >> bit) & 1) == 1);
    }
  }

  private static void cascade(TtlTestInstanceState from, TtlTestInstanceState to) {
    to.setPortValue(
        Ttl74521.pinNrToPortNr(Ttl74521.E),
        from.getPortValue(Ttl74521.pinNrToPortNr(Ttl74521.PQ)));
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74521.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, boolean high) {
    state.setPortValue(Ttl74521.pinNrToPortNr(dsPinNr), high ? Value.TRUE : Value.FALSE);
  }

  private static void setLow(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, false);
  }

  private static void setHigh(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, true);
  }

  private static Value port(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74521.pinNrToPortNr(dsPinNr));
  }

  private static boolean isLow(TtlTestInstanceState state, byte dsPinNr) {
    return port(state, dsPinNr) == Value.FALSE;
  }

  private static boolean isHigh(TtlTestInstanceState state, byte dsPinNr) {
    return port(state, dsPinNr) == Value.TRUE;
  }
}
