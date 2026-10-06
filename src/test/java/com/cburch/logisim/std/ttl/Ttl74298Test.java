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

/** Functional tests for the 74HC298 quad 2-input multiplexer with storage. */
class Ttl74298Test {
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;
  private static final byte[] WORD1 = {Ttl74298.A1, Ttl74298.B1, Ttl74298.C1, Ttl74298.D1};
  private static final byte[] WORD2 = {Ttl74298.A2, Ttl74298.B2, Ttl74298.C2, Ttl74298.D2};
  private static final byte[] OUTPUTS = {Ttl74298.QA, Ttl74298.QB, Ttl74298.QC, Ttl74298.QD};

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var mux = new Ttl74298();
    final var hiddenPower = createInstance(mux, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74298.B2, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74298.A2, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74298.A1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74298.B1, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74298.C2, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74298.D2, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74298.D1, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74298.C1, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74298.WS, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74298.CLK, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74298.QD, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74298.QC, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74298.QB, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74298.QA, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(mux, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void powerUpPresentsZeroUntilTheFirstFallingEdge() {
    final var mux = new Ttl74298();
    final var state = new TtlTestInstanceState(mux, false);
    setWord(state, WORD1, 0xF);
    setWord(state, WORD2, 0x0);
    set(state, Ttl74298.WS, Value.FALSE);
    set(state, Ttl74298.CLK, Value.FALSE);
    mux.propagate(state);

    assertWord(state, 0);
  }

  @Test
  void fallingEdgeWithWordSelectLowLoadsWord1() {
    final var mux = new Ttl74298();
    final var state = new TtlTestInstanceState(mux, false);

    for (var code = 0; code < 16; code++) {
      capture(mux, state, Value.FALSE, code, code ^ 0xF);
      assertWord(state, code);
    }
  }

  @Test
  void fallingEdgeWithWordSelectHighLoadsWord2() {
    final var mux = new Ttl74298();
    final var state = new TtlTestInstanceState(mux, false);

    for (var code = 0; code < 16; code++) {
      capture(mux, state, Value.TRUE, code ^ 0xF, code);
      assertWord(state, code);
    }
  }

  @Test
  void risingEdgeAndIdleDataChangesDoNotLoad() {
    final var mux = new Ttl74298();
    final var state = new TtlTestInstanceState(mux, false);
    capture(mux, state, Value.FALSE, 0x5, 0xA);

    set(state, Ttl74298.WS, Value.TRUE);
    setWord(state, WORD1, 0x3);
    setWord(state, WORD2, 0xC);
    mux.propagate(state);
    assertWord(state, 0x5);

    set(state, Ttl74298.CLK, Value.TRUE);
    mux.propagate(state);
    assertWord(state, 0x5);

    set(state, Ttl74298.CLK, Value.FALSE);
    mux.propagate(state);
    assertWord(state, 0xC);
  }

  @Test
  void unknownClockEdgesDoNotLoad() {
    final var mux = new Ttl74298();
    final var state = new TtlTestInstanceState(mux, false);
    capture(mux, state, Value.FALSE, 0xA, 0x5);
    set(state, Ttl74298.WS, Value.TRUE);
    setWord(state, WORD2, 0x1);

    set(state, Ttl74298.CLK, Value.TRUE);
    mux.propagate(state);
    set(state, Ttl74298.CLK, Value.UNKNOWN);
    mux.propagate(state);
    assertWord(state, 0xA);

    set(state, Ttl74298.CLK, Value.FALSE);
    mux.propagate(state);
    assertWord(state, 0xA);

    set(state, Ttl74298.CLK, Value.TRUE);
    mux.propagate(state);
    set(state, Ttl74298.CLK, Value.FALSE);
    mux.propagate(state);
    assertWord(state, 0x1);
  }

  @Test
  void unknownWordSelectKeepsBitsThatAlreadyAgree() {
    final var mux = new Ttl74298();
    final var state = new TtlTestInstanceState(mux, false);
    set(state, Ttl74298.A1, Value.TRUE);
    set(state, Ttl74298.A2, Value.TRUE);
    set(state, Ttl74298.B1, Value.FALSE);
    set(state, Ttl74298.B2, Value.FALSE);
    set(state, Ttl74298.C1, Value.TRUE);
    set(state, Ttl74298.C2, Value.FALSE);
    set(state, Ttl74298.D1, Value.FALSE);
    set(state, Ttl74298.D2, Value.TRUE);
    set(state, Ttl74298.WS, Value.UNKNOWN);
    fall(mux, state);

    assertBit(state, Ttl74298.QA, Value.TRUE);
    assertBit(state, Ttl74298.QB, Value.FALSE);
    assertBit(state, Ttl74298.QC, Value.UNKNOWN);
    assertBit(state, Ttl74298.QD, Value.UNKNOWN);
  }

  @Test
  void errorOnWordSelectMakesEveryBitAnError() {
    final var mux = new Ttl74298();
    final var state = new TtlTestInstanceState(mux, false);
    setWord(state, WORD1, 0x5);
    setWord(state, WORD2, 0x5);
    set(state, Ttl74298.WS, Value.ERROR);
    fall(mux, state);

    for (final var output : OUTPUTS) {
      assertBit(state, output, Value.ERROR);
    }
  }

  @Test
  void errorAndUnknownFollowOnlyTheSelectedSource() {
    final var mux = new Ttl74298();
    final var state = new TtlTestInstanceState(mux, false);
    set(state, Ttl74298.WS, Value.FALSE);
    set(state, Ttl74298.A1, Value.ERROR);
    set(state, Ttl74298.A2, Value.TRUE);
    set(state, Ttl74298.B1, Value.FALSE);
    set(state, Ttl74298.B2, Value.ERROR);
    set(state, Ttl74298.C1, Value.UNKNOWN);
    set(state, Ttl74298.C2, Value.TRUE);
    set(state, Ttl74298.D1, Value.TRUE);
    set(state, Ttl74298.D2, Value.UNKNOWN);
    fall(mux, state);

    assertBit(state, Ttl74298.QA, Value.ERROR);
    assertBit(state, Ttl74298.QB, Value.FALSE);
    assertBit(state, Ttl74298.QC, Value.UNKNOWN);
    assertBit(state, Ttl74298.QD, Value.TRUE);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var mux = new Ttl74298();
    final var state = new TtlTestInstanceState(mux, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    capture(mux, state, Value.FALSE, 0x6, 0x9);
    assertWord(state, 0x6);

    state.setPortValue(VCC_PORT, Value.FALSE);
    mux.propagate(state);
    assertUnknownOutputs(state);

    setWord(state, WORD1, 0x9);
    fall(mux, state);
    state.setPortValue(VCC_PORT, Value.TRUE);
    mux.propagate(state);
    assertWord(state, 0x6);

    state.setPortValue(GND_PORT, Value.TRUE);
    mux.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(GND_PORT, Value.FALSE);
    mux.propagate(state);
    assertWord(state, 0x6);
  }

  private static void capture(
      Ttl74298 mux, TtlTestInstanceState state, Value select, int word1, int word2) {
    set(state, Ttl74298.WS, select);
    setWord(state, WORD1, word1);
    setWord(state, WORD2, word2);
    fall(mux, state);
  }

  private static void fall(Ttl74298 mux, TtlTestInstanceState state) {
    set(state, Ttl74298.CLK, Value.TRUE);
    mux.propagate(state);
    set(state, Ttl74298.CLK, Value.FALSE);
    mux.propagate(state);
  }

  private static void setWord(TtlTestInstanceState state, byte[] pins, int code) {
    for (var bit = 0; bit < pins.length; bit++) {
      set(state, pins[bit], ((code >> bit) & 1) == 1 ? Value.TRUE : Value.FALSE);
    }
  }

  private static void assertWord(TtlTestInstanceState state, int code) {
    for (var bit = 0; bit < OUTPUTS.length; bit++) {
      assertBit(state, OUTPUTS[bit], ((code >> bit) & 1) == 1 ? Value.TRUE : Value.FALSE);
    }
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var output : OUTPUTS) {
      assertBit(state, output, Value.UNKNOWN);
    }
  }

  private static void assertBit(TtlTestInstanceState state, byte pin, Value expected) {
    assertEquals(expected, state.getPortValue(Ttl74298.pinNrToPortNr(pin)));
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74298.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void set(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl74298.pinNrToPortNr(pin), value);
  }
}
