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

/** Functional tests for the 74256 dual 4-bit addressable latch. */
class Ttl74256Test {
  private static final byte[] OUTPUTS = {
    Ttl74256.Q0a,
    Ttl74256.Q1a,
    Ttl74256.Q2a,
    Ttl74256.Q3a,
    Ttl74256.Q0b,
    Ttl74256.Q1b,
    Ttl74256.Q2b,
    Ttl74256.Q3b
  };
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74256();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74256.A0, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74256.A1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74256.Da, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74256.Q0a, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74256.Q1a, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74256.Q2a, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74256.Q3a, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74256.Q0b, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74256.Q1b, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74256.Q2b, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74256.Q3b, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74256.Db, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74256.E, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74256.CL, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void enableIsNotAnEdgeClock() {
    assertFalse(new Ttl74256().checkForGatedClocks(null));
  }

  @Test
  void resetClearsBothSections() {
    final var gate = new Ttl74256();
    final var state = new TtlTestInstanceState(gate, false);
    latchNibbles(gate, state, 0xF, 0xF);

    drive(state, 3, Value.TRUE, Value.TRUE, Value.TRUE, Value.FALSE);
    gate.propagate(state);

    assertNibbles(state, 0x0, 0x0);
  }

  @Test
  void demultiplexerFollowsEachSectionAndClearsTheOthers() {
    final var gate = new Ttl74256();
    final var state = new TtlTestInstanceState(gate, false);
    latchNibbles(gate, state, 0xF, 0xF);

    for (var address = 0; address < 4; address++) {
      drive(state, address, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE);
      gate.propagate(state);
      assertNibbles(state, 1 << address, 0x0);

      drive(state, address, Value.FALSE, Value.TRUE, Value.FALSE, Value.FALSE);
      gate.propagate(state);
      assertNibbles(state, 0x0, 1 << address);
    }
  }

  @Test
  void addressableLatchKeepsUnselectedBitsIndependently() {
    final var gate = new Ttl74256();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    drive(state, 0, Value.TRUE, Value.FALSE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertNibbles(state, 0x1, 0x0);

    drive(state, 1, Value.FALSE, Value.TRUE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertNibbles(state, 0x1, 0x2);

    drive(state, 1, Value.TRUE, Value.FALSE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertNibbles(state, 0x3, 0x0);
  }

  @Test
  void memoryHoldsWhileDataAndAddressChange() {
    final var gate = new Ttl74256();
    final var state = new TtlTestInstanceState(gate, false);
    latchNibbles(gate, state, 0xA, 0x5);

    drive(state, 0, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    drive(state, 3, Value.TRUE, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);

    assertNibbles(state, 0xA, 0x5);
  }

  @Test
  void demultiplexerOverwritesBeforeTheLatchHolds() {
    final var gate = new Ttl74256();
    final var state = new TtlTestInstanceState(gate, false);
    latchNibbles(gate, state, 0xF, 0xF);

    drive(state, 2, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertNibbles(state, 0x4, 0x0);

    drive(state, 0, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertNibbles(state, 0x4, 0x0);

    drive(state, 0, Value.TRUE, Value.TRUE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertNibbles(state, 0x5, 0x1);
  }

  @Test
  void unknownControlsDriveEveryOutputWithoutWriting() {
    final var gate = new Ttl74256();
    final var state = new TtlTestInstanceState(gate, false);
    latchNibbles(gate, state, 0xA, 0x5);

    drive(state, 1, Value.FALSE, Value.TRUE, Value.UNKNOWN, Value.TRUE);
    gate.propagate(state);
    assertAll(state, Value.UNKNOWN);

    drive(state, 0, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertNibbles(state, 0xA, 0x5);
  }

  @Test
  void errorControlsDriveEveryOutputWithoutWriting() {
    final var gate = new Ttl74256();
    final var state = new TtlTestInstanceState(gate, false);
    latchNibbles(gate, state, 0xA, 0x5);

    drive(state, 1, Value.TRUE, Value.FALSE, Value.TRUE, Value.ERROR);
    gate.propagate(state);
    assertAll(state, Value.ERROR);

    drive(state, 0, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertNibbles(state, 0xA, 0x5);
  }

  @Test
  void unknownDataIsStoredOnlyInTheAddressedLatch() {
    final var gate = new Ttl74256();
    final var state = new TtlTestInstanceState(gate, false);
    reset(gate, state);

    drive(state, 2, Value.UNKNOWN, Value.TRUE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertOutput(state, 2, Value.UNKNOWN);
    assertOutput(state, 6, Value.TRUE);
    assertIdleExcept(state, 2, 6);

    drive(state, 2, Value.ERROR, Value.TRUE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    drive(state, 2, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);

    assertOutput(state, 2, Value.ERROR);
    assertOutput(state, 6, Value.TRUE);
    assertIdleExcept(state, 2, 6);
  }

  @Test
  void partialAddressCombinesSelectedAndUnselectedResults() {
    final var gate = new Ttl74256();
    final var state = new TtlTestInstanceState(gate, false);
    final var low = Value.FALSE;
    final var error = Value.ERROR;
    reset(gate, state);

    drive(state, 0, Value.TRUE, low, low, Value.TRUE);
    set(state, Ttl74256.A0, Value.UNKNOWN);
    gate.propagate(state);
    assertOutputs(state, error, error, low, low, low, low, low, low);

    reset(gate, state);
    latchNibbles(gate, state, 0x1, 0x0);
    drive(state, 0, low, low, low, Value.TRUE);
    set(state, Ttl74256.A0, Value.UNKNOWN);
    gate.propagate(state);
    assertOutputs(state, error, low, low, low, low, low, low, low);

    reset(gate, state);
    drive(state, 0, low, low, low, low);
    set(state, Ttl74256.A0, Value.UNKNOWN);
    gate.propagate(state);
    assertNibbles(state, 0x0, 0x0);

    drive(state, 0, Value.TRUE, Value.TRUE, low, low);
    set(state, Ttl74256.A0, Value.UNKNOWN);
    gate.propagate(state);
    assertOutputs(state, error, error, low, low, error, error, low, low);
  }

  @Test
  void startupFollowsTheMemoryPreference() {
    final var previous = AppPreferences.Memory_Startup_Unknown.get();
    try {
      AppPreferences.Memory_Startup_Unknown.set(false);
      final var cleared = new Ttl74256();
      final var clearedState = new TtlTestInstanceState(cleared, false);
      drive(clearedState, 0, Value.TRUE, Value.TRUE, Value.TRUE, Value.TRUE);
      cleared.propagate(clearedState);
      assertNibbles(clearedState, 0x0, 0x0);

      AppPreferences.Memory_Startup_Unknown.set(true);
      final var unknown = new Ttl74256();
      final var unknownState = new TtlTestInstanceState(unknown, false);
      drive(unknownState, 0, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE);
      unknown.propagate(unknownState);
      assertAll(unknownState, Value.UNKNOWN);
    } finally {
      AppPreferences.Memory_Startup_Unknown.set(previous);
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74256();
    final var state = new TtlTestInstanceState(gate, true);
    power(state, Value.FALSE, Value.TRUE);
    latchNibbles(gate, state, 0xA, 0x5);
    drive(state, 0, Value.FALSE, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertNibbles(state, 0xA, 0x5);

    power(state, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertAll(state, Value.UNKNOWN);

    power(state, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertNibbles(state, 0xA, 0x5);

    power(state, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertAll(state, Value.UNKNOWN);
  }

  private static void reset(Ttl74256 gate, TtlTestInstanceState state) {
    drive(state, 0, Value.FALSE, Value.FALSE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
  }

  private static void latchNibbles(Ttl74256 gate, TtlTestInstanceState state, int sectionA, int sectionB) {
    for (var index = 0; index < 4; index++) {
      final var dataA = ((sectionA >> index) & 1) == 1 ? Value.TRUE : Value.FALSE;
      final var dataB = ((sectionB >> index) & 1) == 1 ? Value.TRUE : Value.FALSE;
      drive(state, index, dataA, dataB, Value.FALSE, Value.TRUE);
      gate.propagate(state);
    }
  }

  private static void drive(
      TtlTestInstanceState state,
      int address,
      Value dataA,
      Value dataB,
      Value enable,
      Value clear) {
    set(state, Ttl74256.A0, bit(address, 0));
    set(state, Ttl74256.A1, bit(address, 1));
    set(state, Ttl74256.Da, dataA);
    set(state, Ttl74256.Db, dataB);
    set(state, Ttl74256.E, enable);
    set(state, Ttl74256.CL, clear);
  }

  private static Value bit(int address, int place) {
    return ((address >> place) & 1) == 1 ? Value.TRUE : Value.FALSE;
  }

  private static void power(TtlTestInstanceState state, Value ground, Value supply) {
    state.setPortValue(GND_PORT, ground);
    state.setPortValue(VCC_PORT, supply);
  }

  private static void assertNibbles(TtlTestInstanceState state, int sectionA, int sectionB) {
    for (var index = 0; index < 4; index++) {
      assertOutput(state, index, bit(sectionA, index));
      assertOutput(state, index + 4, bit(sectionB, index));
    }
  }

  private static void assertIdleExcept(TtlTestInstanceState state, int first, int second) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      if (index == first || index == second) continue;
      assertOutput(state, index, Value.FALSE);
    }
  }

  private static void assertOutputs(TtlTestInstanceState state, Value... expected) {
    assertEquals(OUTPUTS.length, expected.length);
    for (var index = 0; index < OUTPUTS.length; index++) {
      assertOutput(state, index, expected[index]);
    }
  }

  private static void assertAll(TtlTestInstanceState state, Value expected) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      assertOutput(state, index, expected);
    }
  }

  private static void assertOutput(TtlTestInstanceState state, int index, Value expected) {
    assertEquals(
        expected, state.getPortValue(Ttl74256.pinNrToPortNr(OUTPUTS[index])), "Q" + index);
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74256.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void set(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl74256.pinNrToPortNr(dsPinNr), value);
  }
}
