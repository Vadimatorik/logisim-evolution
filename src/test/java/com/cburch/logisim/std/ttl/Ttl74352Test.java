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

/** Functional tests for the 74HC352 dual 4-line to 1-line inverting data selector. */
class Ttl74352Test {
  private static final byte[] SECTION1_DATA = {
    Ttl74352.L1_D0, Ttl74352.L1_D1, Ttl74352.L1_D2, Ttl74352.L1_D3
  };
  private static final byte[] SECTION2_DATA = {
    Ttl74352.L2_D0, Ttl74352.L2_D1, Ttl74352.L2_D2, Ttl74352.L2_D3
  };

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var mux = new Ttl74352();
    final var hiddenPower = createInstance(mux, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74352.L1_EN, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74352.S1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74352.L1_D3, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74352.L1_D2, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74352.L1_D1, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74352.L1_D0, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74352.L1_Y, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74352.L2_Y, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74352.L2_D0, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74352.L2_D1, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74352.L2_D2, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74352.L2_D3, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74352.S0, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74352.L2_EN, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(mux, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void everyBinaryInputCombinationFollowsTheFunctionTable() {
    final var mux = new Ttl74352();
    final var state = new TtlTestInstanceState(mux, false);

    for (var code = 0; code < 4096; code++) {
      final var n1e = bit(code, 0);
      final var s1 = bit(code, 1);
      final var d13 = bit(code, 2);
      final var d12 = bit(code, 3);
      final var d11 = bit(code, 4);
      final var d10 = bit(code, 5);
      final var d20 = bit(code, 6);
      final var d21 = bit(code, 7);
      final var d22 = bit(code, 8);
      final var d23 = bit(code, 9);
      final var s0 = bit(code, 10);
      final var n2e = bit(code, 11);

      setLevel(state, Ttl74352.L1_EN, n1e);
      setLevel(state, Ttl74352.S1, s1);
      setLevel(state, Ttl74352.L1_D3, d13);
      setLevel(state, Ttl74352.L1_D2, d12);
      setLevel(state, Ttl74352.L1_D1, d11);
      setLevel(state, Ttl74352.L1_D0, d10);
      setLevel(state, Ttl74352.L2_D0, d20);
      setLevel(state, Ttl74352.L2_D1, d21);
      setLevel(state, Ttl74352.L2_D2, d22);
      setLevel(state, Ttl74352.L2_D3, d23);
      setLevel(state, Ttl74352.S0, s0);
      setLevel(state, Ttl74352.L2_EN, n2e);
      mux.propagate(state);

      assertEquals(
          functionTable(n1e, s1, s0, d10, d11, d12, d13),
          level(state, Ttl74352.L1_Y),
          "section 1 code " + code);
      assertEquals(
          functionTable(n2e, s1, s0, d20, d21, d22, d23),
          level(state, Ttl74352.L2_Y),
          "section 2 code " + code);
    }
  }

  @Test
  void unknownSelectedDataPropagatesAndUnselectedDataDoesNot() {
    final var mux = new Ttl74352();
    final var state = enabled(mux);
    setLevel(state, Ttl74352.S1, false);
    setLevel(state, Ttl74352.S0, false);
    set(state, Ttl74352.L1_D0, Value.UNKNOWN);
    mux.propagate(state);
    assertEquals(Value.UNKNOWN, level(state, Ttl74352.L1_Y));

    set(state, Ttl74352.L1_D0, Value.ERROR);
    mux.propagate(state);
    assertEquals(Value.ERROR, level(state, Ttl74352.L1_Y));

    setLevel(state, Ttl74352.L1_D0, false);
    set(state, Ttl74352.L1_D3, Value.ERROR);
    mux.propagate(state);
    assertEquals(Value.TRUE, level(state, Ttl74352.L1_Y));
  }

  @Test
  void unknownEnableStaysHighOnlyWhenEveryEnabledChoiceIsHigh() {
    final var mux = new Ttl74352();
    final var state = enabled(mux);
    set(state, Ttl74352.L1_EN, Value.UNKNOWN);
    for (final var pin : SECTION1_DATA) {
      setLevel(state, pin, false);
    }
    mux.propagate(state);
    assertEquals(Value.TRUE, level(state, Ttl74352.L1_Y));

    for (final var pin : SECTION1_DATA) {
      setLevel(state, pin, true);
    }
    mux.propagate(state);
    assertEquals(Value.UNKNOWN, level(state, Ttl74352.L1_Y));

    set(state, Ttl74352.L1_EN, Value.ERROR);
    setLevel(state, Ttl74352.L1_D0, true);
    setLevel(state, Ttl74352.S1, false);
    setLevel(state, Ttl74352.S0, false);
    mux.propagate(state);
    assertEquals(Value.ERROR, level(state, Ttl74352.L1_Y));

    setLevel(state, Ttl74352.L1_D0, false);
    mux.propagate(state);
    assertEquals(Value.TRUE, level(state, Ttl74352.L1_Y));
  }

  @Test
  void unknownSelectIsDeterminedWhenTheCandidateDataAgree() {
    final var mux = new Ttl74352();
    final var state = enabled(mux);
    set(state, Ttl74352.S0, Value.UNKNOWN);
    setLevel(state, Ttl74352.S1, false);
    setLevel(state, Ttl74352.L1_D0, true);
    setLevel(state, Ttl74352.L1_D1, true);
    mux.propagate(state);
    assertEquals(Value.FALSE, level(state, Ttl74352.L1_Y));

    setLevel(state, Ttl74352.L1_D1, false);
    mux.propagate(state);
    assertEquals(Value.UNKNOWN, level(state, Ttl74352.L1_Y));
  }

  /**
   * Datasheet function table, independent of the component. A high strobe forces the output high.
   * Otherwise the output is the complement of the data input addressed by {@code s1 s0}.
   */
  private static Value functionTable(
      boolean inhibit, boolean s1, boolean s0, boolean d0, boolean d1, boolean d2, boolean d3) {
    if (inhibit) {
      return Value.TRUE;
    }
    final var select = (s1 ? 2 : 0) + (s0 ? 1 : 0);
    final var data = new boolean[] {d0, d1, d2, d3};
    return data[select] ? Value.FALSE : Value.TRUE;
  }

  /** Enables both sections and drives every data and select pin low. */
  private static TtlTestInstanceState enabled(Ttl74352 mux) {
    final var state = new TtlTestInstanceState(mux, false);
    setLevel(state, Ttl74352.L1_EN, false);
    setLevel(state, Ttl74352.L2_EN, false);
    setLevel(state, Ttl74352.S1, false);
    setLevel(state, Ttl74352.S0, false);
    for (final var pin : SECTION1_DATA) {
      setLevel(state, pin, false);
    }
    for (final var pin : SECTION2_DATA) {
      setLevel(state, pin, false);
    }
    return state;
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74352.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, boolean high) {
    set(state, dsPinNr, high ? Value.TRUE : Value.FALSE);
  }

  private static void set(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl74352.pinNrToPortNr(dsPinNr), value);
  }

  private static Value level(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74352.pinNrToPortNr(dsPinNr));
  }

  private static boolean bit(int code, int index) {
    return ((code >> index) & 1) != 0;
  }
}
