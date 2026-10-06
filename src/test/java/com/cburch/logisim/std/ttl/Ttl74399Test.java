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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74399 quad 2-port register. */
class Ttl74399Test {
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;
  private static final byte[] PORT0 = {Ttl74399.I0A, Ttl74399.I0B, Ttl74399.I0C, Ttl74399.I0D};
  private static final byte[] PORT1 = {Ttl74399.I1A, Ttl74399.I1B, Ttl74399.I1C, Ttl74399.I1D};
  private static final byte[] OUTPUTS = {Ttl74399.QA, Ttl74399.QB, Ttl74399.QC, Ttl74399.QD};

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var register = new Ttl74399();
    final var hiddenPower = createInstance(register, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74399.S, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74399.QA, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74399.I0A, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74399.I1A, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74399.I1B, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74399.I0B, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74399.QB, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74399.CP, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74399.QC, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74399.I0C, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74399.I1C, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74399.I1D, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74399.I0D, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74399.QD, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(register, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void clockIsTheNinthDatasheetPin() {
    final var register = new Ttl74399();
    assertTrue(register.checkForGatedClocks(null));
    assertArrayEquals(new int[] {Ttl74399.pinNrToPortNr(Ttl74399.CP)}, register.clockPinIndex(null));
  }

  @Test
  void powerUpPresentsZeroUntilTheFirstRisingEdge() {
    final var register = new Ttl74399();
    final var state = new TtlTestInstanceState(register, false);
    setWord(state, PORT0, 0xF);
    setWord(state, PORT1, 0x0);
    set(state, Ttl74399.S, Value.FALSE);
    set(state, Ttl74399.CP, Value.FALSE);
    register.propagate(state);

    assertWord(state, 0);
  }

  @Test
  void risingEdgeWithSelectLowLoadsPort0() {
    final var register = new Ttl74399();
    final var state = new TtlTestInstanceState(register, false);

    for (var code = 0; code < 16; code++) {
      capture(register, state, Value.FALSE, code, code ^ 0xF);
      assertWord(state, code);
    }
  }

  @Test
  void risingEdgeWithSelectHighLoadsPort1() {
    final var register = new Ttl74399();
    final var state = new TtlTestInstanceState(register, false);

    for (var code = 0; code < 16; code++) {
      capture(register, state, Value.TRUE, code ^ 0xF, code);
      assertWord(state, code);
    }
  }

  @Test
  void fallingEdgeAndIdleDataChangesDoNotLoad() {
    final var register = new Ttl74399();
    final var state = new TtlTestInstanceState(register, false);
    capture(register, state, Value.FALSE, 0x5, 0xA);

    set(state, Ttl74399.S, Value.TRUE);
    setWord(state, PORT0, 0x3);
    setWord(state, PORT1, 0xC);
    register.propagate(state);
    assertWord(state, 0x5);

    set(state, Ttl74399.CP, Value.FALSE);
    register.propagate(state);
    assertWord(state, 0x5);

    set(state, Ttl74399.CP, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0xC);
  }

  @Test
  void unknownClockEdgesDoNotLoad() {
    final var register = new Ttl74399();
    final var state = new TtlTestInstanceState(register, false);
    capture(register, state, Value.FALSE, 0xA, 0x5);
    set(state, Ttl74399.S, Value.TRUE);
    setWord(state, PORT1, 0x1);

    set(state, Ttl74399.CP, Value.FALSE);
    register.propagate(state);
    set(state, Ttl74399.CP, Value.UNKNOWN);
    register.propagate(state);
    assertWord(state, 0xA);

    set(state, Ttl74399.CP, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0xA);

    set(state, Ttl74399.CP, Value.FALSE);
    register.propagate(state);
    set(state, Ttl74399.CP, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0x1);
  }

  @Test
  void unknownSelectKeepsBitsThatAlreadyAgree() {
    final var register = new Ttl74399();
    final var state = new TtlTestInstanceState(register, false);
    set(state, Ttl74399.I0A, Value.TRUE);
    set(state, Ttl74399.I1A, Value.TRUE);
    set(state, Ttl74399.I0B, Value.FALSE);
    set(state, Ttl74399.I1B, Value.FALSE);
    set(state, Ttl74399.I0C, Value.TRUE);
    set(state, Ttl74399.I1C, Value.FALSE);
    set(state, Ttl74399.I0D, Value.FALSE);
    set(state, Ttl74399.I1D, Value.TRUE);
    set(state, Ttl74399.S, Value.UNKNOWN);
    rise(register, state);

    assertBit(state, Ttl74399.QA, Value.TRUE);
    assertBit(state, Ttl74399.QB, Value.FALSE);
    assertBit(state, Ttl74399.QC, Value.UNKNOWN);
    assertBit(state, Ttl74399.QD, Value.UNKNOWN);
  }

  @Test
  void errorOnSelectMakesEveryBitAnError() {
    final var register = new Ttl74399();
    final var state = new TtlTestInstanceState(register, false);
    setWord(state, PORT0, 0x5);
    setWord(state, PORT1, 0x5);
    set(state, Ttl74399.S, Value.ERROR);
    rise(register, state);

    for (final var output : OUTPUTS) {
      assertBit(state, output, Value.ERROR);
    }
  }

  @Test
  void errorAndUnknownFollowOnlyTheSelectedPort() {
    final var register = new Ttl74399();
    final var state = new TtlTestInstanceState(register, false);
    set(state, Ttl74399.S, Value.FALSE);
    set(state, Ttl74399.I0A, Value.ERROR);
    set(state, Ttl74399.I1A, Value.TRUE);
    set(state, Ttl74399.I0B, Value.FALSE);
    set(state, Ttl74399.I1B, Value.ERROR);
    set(state, Ttl74399.I0C, Value.UNKNOWN);
    set(state, Ttl74399.I1C, Value.TRUE);
    set(state, Ttl74399.I0D, Value.TRUE);
    set(state, Ttl74399.I1D, Value.UNKNOWN);
    rise(register, state);

    assertBit(state, Ttl74399.QA, Value.ERROR);
    assertBit(state, Ttl74399.QB, Value.FALSE);
    assertBit(state, Ttl74399.QC, Value.UNKNOWN);
    assertBit(state, Ttl74399.QD, Value.TRUE);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var register = new Ttl74399();
    final var state = new TtlTestInstanceState(register, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    capture(register, state, Value.FALSE, 0x6, 0x9);
    assertWord(state, 0x6);

    state.setPortValue(VCC_PORT, Value.FALSE);
    register.propagate(state);
    assertUnknownOutputs(state);

    setWord(state, PORT0, 0x9);
    rise(register, state);
    state.setPortValue(VCC_PORT, Value.TRUE);
    register.propagate(state);
    assertWord(state, 0x6);

    state.setPortValue(GND_PORT, Value.TRUE);
    register.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(GND_PORT, Value.FALSE);
    register.propagate(state);
    assertWord(state, 0x6);
  }

  private static void capture(
      Ttl74399 register, TtlTestInstanceState state, Value select, int port0, int port1) {
    set(state, Ttl74399.S, select);
    setWord(state, PORT0, port0);
    setWord(state, PORT1, port1);
    rise(register, state);
  }

  private static void rise(Ttl74399 register, TtlTestInstanceState state) {
    set(state, Ttl74399.CP, Value.FALSE);
    register.propagate(state);
    set(state, Ttl74399.CP, Value.TRUE);
    register.propagate(state);
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
    assertEquals(expected, state.getPortValue(Ttl74399.pinNrToPortNr(pin)));
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74399.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void set(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl74399.pinNrToPortNr(pin), value);
  }
}
