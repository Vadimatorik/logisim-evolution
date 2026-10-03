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

class Ttl74682Test {
  private static final byte[] WORD_P = {
    Ttl74682.P0, Ttl74682.P1, Ttl74682.P2, Ttl74682.P3,
    Ttl74682.P4, Ttl74682.P5, Ttl74682.P6, Ttl74682.P7
  };
  private static final byte[] WORD_Q = {
    Ttl74682.Q0, Ttl74682.Q1, Ttl74682.Q2, Ttl74682.Q3,
    Ttl74682.Q4, Ttl74682.Q5, Ttl74682.Q6, Ttl74682.Q7
  };

  private static final int GND_PORT = 18;
  private static final int VCC_PORT = 19;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var comparator = new Ttl74682();
    final var hiddenPower = createInstance(comparator, false);

    assertEquals(18, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74682.PGTQ, 10, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74682.P0, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74682.Q0, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74682.P1, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74682.Q1, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74682.P2, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74682.Q2, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74682.P3, 150, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74682.Q3, 170, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74682.P4, 190, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74682.Q4, 170, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74682.P5, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74682.Q5, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74682.P6, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74682.Q6, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74682.P7, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74682.Q7, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74682.PQ, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(comparator, true);
    assertEquals(20, shownPower.getPorts().size());
    assertEquals(Location.create(190, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void comparesEveryPairOfEightBitWords() {
    final var comparator = new Ttl74682();
    final var state = new TtlTestInstanceState(comparator, false);

    for (var wordP = 0; wordP < 256; wordP++) {
      for (var wordQ = 0; wordQ < 256; wordQ++) {
        setWords(state, wordP, wordQ);
        comparator.propagate(state);
        assertEquals(wordP == wordQ, isLow(state, Ttl74682.PQ));
        assertEquals(wordP > wordQ, isLow(state, Ttl74682.PGTQ));
        assertTrue(isSolid(state, Ttl74682.PQ));
        assertTrue(isSolid(state, Ttl74682.PGTQ));
      }
    }
  }

  @Test
  void unknownOrErrorBitsMakeBothOutputsUnknown() {
    final var comparator = new Ttl74682();

    final var unknownBit = new TtlTestInstanceState(comparator, false);
    setWords(unknownBit, 0x00, 0x00);
    unknownBit.setPortValue(Ttl74682.pinNrToPortNr(Ttl74682.P3), Value.UNKNOWN);
    comparator.propagate(unknownBit);
    assertEquals(Value.UNKNOWN, port(unknownBit, Ttl74682.PQ));
    assertEquals(Value.UNKNOWN, port(unknownBit, Ttl74682.PGTQ));

    final var errorBit = new TtlTestInstanceState(comparator, false);
    setWords(errorBit, 0xFF, 0xFF);
    errorBit.setPortValue(Ttl74682.pinNrToPortNr(Ttl74682.Q1), Value.ERROR);
    comparator.propagate(errorBit);
    assertEquals(Value.UNKNOWN, port(errorBit, Ttl74682.PQ));
    assertEquals(Value.UNKNOWN, port(errorBit, Ttl74682.PGTQ));

    final var floating = new TtlTestInstanceState(comparator, false);
    comparator.propagate(floating);
    assertEquals(Value.UNKNOWN, port(floating, Ttl74682.PQ));
    assertEquals(Value.UNKNOWN, port(floating, Ttl74682.PGTQ));
  }

  @Test
  void invalidExposedPowerInputsMakeBothOutputsUnknown() {
    final var comparator = new Ttl74682();
    final var state = new TtlTestInstanceState(comparator, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    setWords(state, 0x80, 0x7F);
    comparator.propagate(state);
    assertTrue(isHigh(state, Ttl74682.PQ));
    assertTrue(isLow(state, Ttl74682.PGTQ));

    state.setPortValue(VCC_PORT, Value.FALSE);
    comparator.propagate(state);
    assertEquals(Value.UNKNOWN, port(state, Ttl74682.PQ));
    assertEquals(Value.UNKNOWN, port(state, Ttl74682.PGTQ));

    state.setPortValue(VCC_PORT, Value.TRUE);
    comparator.propagate(state);
    assertTrue(isHigh(state, Ttl74682.PQ));
    assertTrue(isLow(state, Ttl74682.PGTQ));

    state.setPortValue(GND_PORT, Value.TRUE);
    comparator.propagate(state);
    assertEquals(Value.UNKNOWN, port(state, Ttl74682.PQ));
    assertEquals(Value.UNKNOWN, port(state, Ttl74682.PGTQ));
  }

  private static void setWords(TtlTestInstanceState state, int wordP, int wordQ) {
    for (var bit = 0; bit < 8; bit++) {
      setLevel(state, WORD_P[bit], ((wordP >> bit) & 1) == 1);
      setLevel(state, WORD_Q[bit], ((wordQ >> bit) & 1) == 1);
    }
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74682.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, boolean high) {
    state.setPortValue(Ttl74682.pinNrToPortNr(dsPinNr), high ? Value.TRUE : Value.FALSE);
  }

  private static Value port(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74682.pinNrToPortNr(dsPinNr));
  }

  private static boolean isLow(TtlTestInstanceState state, byte dsPinNr) {
    return port(state, dsPinNr) == Value.FALSE;
  }

  private static boolean isHigh(TtlTestInstanceState state, byte dsPinNr) {
    return port(state, dsPinNr) == Value.TRUE;
  }

  private static boolean isSolid(TtlTestInstanceState state, byte dsPinNr) {
    final var value = port(state, dsPinNr);
    return value == Value.TRUE || value == Value.FALSE;
  }
}
