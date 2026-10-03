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

/** Functional tests for the 74HC597 shift register with input storage. */
class Ttl74597Test {
  private static final byte[] DATA = {
    Ttl74597.D0, Ttl74597.D1, Ttl74597.D2, Ttl74597.D3,
    Ttl74597.D4, Ttl74597.D5, Ttl74597.D6, Ttl74597.D7
  };
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;
  /** Q after shifting 0xA5 toward the output with DS held low. */
  private static final Value[] SHIFTED_A5 = {
    Value.FALSE, Value.TRUE, Value.FALSE, Value.FALSE,
    Value.TRUE, Value.FALSE, Value.TRUE, Value.FALSE
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74597();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74597.D1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74597.D2, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74597.D3, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74597.D4, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74597.D5, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74597.D6, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74597.D7, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74597.Q, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74597.MR, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74597.SHCP, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74597.STCP, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74597.PL, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74597.DS, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74597.D0, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void bothClocksAreGatedClocks() {
    final var gate = new Ttl74597();

    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(
        new int[] {Ttl74597.pinNrToPortNr(Ttl74597.SHCP), Ttl74597.pinNrToPortNr(Ttl74597.STCP)},
        gate.clockPinIndex(null));
  }

  @Test
  void storageStaysHiddenUntilParallelLoad() {
    final var gate = new Ttl74597();
    final var state = cleared(gate);

    setParallel(state, 0x80);
    pulse(gate, state, Ttl74597.STCP);
    assertEquals(Value.FALSE, level(state, Ttl74597.Q));

    setLevel(state, Ttl74597.PL, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.TRUE, level(state, Ttl74597.Q));

    setLevel(state, Ttl74597.PL, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.TRUE, level(state, Ttl74597.Q));
  }

  @Test
  void parallelLoadUsesStoredBitsAfterThePinsChange() {
    final var gate = new Ttl74597();
    final var state = cleared(gate);
    setParallel(state, 0x80);
    pulse(gate, state, Ttl74597.STCP);

    setParallel(state, 0x00);
    gate.propagate(state);
    setLevel(state, Ttl74597.PL, Value.FALSE);
    gate.propagate(state);

    assertEquals(Value.TRUE, level(state, Ttl74597.Q));
  }

  @Test
  void storageClockWhileLoadIsLowUpdatesTheOutput() {
    final var gate = new Ttl74597();
    final var state = cleared(gate);
    setLevel(state, Ttl74597.PL, Value.FALSE);
    gate.propagate(state);

    setParallel(state, 0x80);
    pulse(gate, state, Ttl74597.STCP);
    assertEquals(Value.TRUE, level(state, Ttl74597.Q));

    setParallel(state, 0x00);
    gate.propagate(state);
    assertEquals(Value.TRUE, level(state, Ttl74597.Q));

    pulse(gate, state, Ttl74597.STCP);
    assertEquals(Value.FALSE, level(state, Ttl74597.Q));
  }

  @Test
  void shiftMovesTowardQAndSerialInputEntersAtD0() {
    final var gate = new Ttl74597();
    final var state = cleared(gate);
    load(gate, state, 0xA5);
    assertEquals(Value.TRUE, level(state, Ttl74597.Q));

    setLevel(state, Ttl74597.DS, Value.FALSE);
    for (final var expected : SHIFTED_A5) {
      pulse(gate, state, Ttl74597.SHCP);
      assertEquals(expected, level(state, Ttl74597.Q));
    }
  }

  @Test
  void resetClearsOnlyTheShiftRegister() {
    final var gate = new Ttl74597();
    final var state = cleared(gate);
    load(gate, state, 0x80);
    assertEquals(Value.TRUE, level(state, Ttl74597.Q));

    setParallel(state, 0x00);
    setLevel(state, Ttl74597.MR, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.FALSE, level(state, Ttl74597.Q));

    setLevel(state, Ttl74597.MR, Value.TRUE);
    gate.propagate(state);
    setLevel(state, Ttl74597.PL, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.TRUE, level(state, Ttl74597.Q));
  }

  @Test
  void simultaneousClocksShiftTheOldBitsAndStoreTheNewOnes() {
    final var gate = new Ttl74597();
    final var state = cleared(gate);
    load(gate, state, 0x40);
    assertEquals(Value.FALSE, level(state, Ttl74597.Q));

    setParallel(state, 0x00);
    setLevel(state, Ttl74597.DS, Value.FALSE);
    riseBoth(gate, state);
    assertEquals(Value.TRUE, level(state, Ttl74597.Q));

    setLevel(state, Ttl74597.PL, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.FALSE, level(state, Ttl74597.Q));
  }

  @Test
  void shiftClockIsIgnoredWhileParallelLoadIsLow() {
    final var gate = new Ttl74597();
    final var state = cleared(gate);
    setParallel(state, 0x80);
    pulse(gate, state, Ttl74597.STCP);
    setLevel(state, Ttl74597.PL, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.TRUE, level(state, Ttl74597.Q));

    setLevel(state, Ttl74597.DS, Value.FALSE);
    pulse(gate, state, Ttl74597.SHCP);
    assertEquals(Value.TRUE, level(state, Ttl74597.Q));
  }

  @Test
  void heldLevelsAndUnknownEdgesDoNotTransfer() {
    final var gate = new Ttl74597();
    final var state = cleared(gate);
    load(gate, state, 0x80);
    assertEquals(Value.TRUE, level(state, Ttl74597.Q));

    setLevel(state, Ttl74597.SHCP, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.FALSE, level(state, Ttl74597.Q));
    setLevel(state, Ttl74597.SHCP, Value.TRUE);
    gate.propagate(state);
    setLevel(state, Ttl74597.SHCP, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.FALSE, level(state, Ttl74597.Q));

    setParallel(state, 0x00);
    setLevel(state, Ttl74597.STCP, Value.TRUE);
    gate.propagate(state);
    setLevel(state, Ttl74597.STCP, Value.TRUE);
    gate.propagate(state);
    setParallel(state, 0xFF);
    setLevel(state, Ttl74597.STCP, Value.FALSE);
    gate.propagate(state);
    setLevel(state, Ttl74597.PL, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.FALSE, level(state, Ttl74597.Q));

    setLevel(state, Ttl74597.PL, Value.TRUE);
    gate.propagate(state);
    setParallel(state, 0x80);
    pulse(gate, state, Ttl74597.STCP);
    setLevel(state, Ttl74597.PL, Value.FALSE);
    gate.propagate(state);
    setLevel(state, Ttl74597.PL, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.TRUE, level(state, Ttl74597.Q));

    setLevel(state, Ttl74597.SHCP, Value.UNKNOWN);
    gate.propagate(state);
    setLevel(state, Ttl74597.SHCP, Value.FALSE);
    gate.propagate(state);
    setLevel(state, Ttl74597.STCP, Value.UNKNOWN);
    gate.propagate(state);
    setLevel(state, Ttl74597.STCP, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.TRUE, level(state, Ttl74597.Q));
  }

  @Test
  void unknownControlsLeaveTheShiftRegisterAlone() {
    final var gate = new Ttl74597();
    final var state = cleared(gate);
    load(gate, state, 0x80);

    setLevel(state, Ttl74597.MR, Value.UNKNOWN);
    gate.propagate(state);
    setLevel(state, Ttl74597.MR, Value.ERROR);
    gate.propagate(state);
    setLevel(state, Ttl74597.MR, Value.TRUE);
    setLevel(state, Ttl74597.PL, Value.UNKNOWN);
    gate.propagate(state);
    setLevel(state, Ttl74597.PL, Value.ERROR);
    gate.propagate(state);

    assertEquals(Value.TRUE, level(state, Ttl74597.Q));
  }

  @Test
  void bothControlsLowMakeTheShiftRegisterUnknown() {
    final var gate = new Ttl74597();
    final var state = cleared(gate);
    load(gate, state, 0x80);

    setLevel(state, Ttl74597.PL, Value.FALSE);
    setLevel(state, Ttl74597.MR, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, level(state, Ttl74597.Q));

    setLevel(state, Ttl74597.PL, Value.TRUE);
    setLevel(state, Ttl74597.MR, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, level(state, Ttl74597.Q));

    setLevel(state, Ttl74597.MR, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.FALSE, level(state, Ttl74597.Q));
  }

  @Test
  void unknownAndErrorParallelBitsAreStored() {
    final var gate = new Ttl74597();
    final var state = cleared(gate);

    setParallel(state, 0x00);
    setLevel(state, Ttl74597.D7, Value.UNKNOWN);
    pulse(gate, state, Ttl74597.STCP);
    setLevel(state, Ttl74597.PL, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, level(state, Ttl74597.Q));

    setLevel(state, Ttl74597.PL, Value.TRUE);
    gate.propagate(state);
    setLevel(state, Ttl74597.D7, Value.ERROR);
    pulse(gate, state, Ttl74597.STCP);
    setLevel(state, Ttl74597.PL, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.ERROR, level(state, Ttl74597.Q));
  }

  @Test
  void invalidExposedPowerInputsMakeTheOutputUnknown() {
    final var gate = new Ttl74597();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    arm(state);
    setLevel(state, Ttl74597.MR, Value.FALSE);
    gate.propagate(state);
    setLevel(state, Ttl74597.MR, Value.TRUE);
    gate.propagate(state);
    load(gate, state, 0x80);
    assertEquals(Value.TRUE, level(state, Ttl74597.Q));

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, level(state, Ttl74597.Q));

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.TRUE, level(state, Ttl74597.Q));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, level(state, Ttl74597.Q));
  }

  /** Clears the shift register and leaves both clocks low with load released. */
  private static TtlTestInstanceState cleared(Ttl74597 gate) {
    final var state = new TtlTestInstanceState(gate, false);
    arm(state);
    setLevel(state, Ttl74597.MR, Value.FALSE);
    gate.propagate(state);
    setLevel(state, Ttl74597.MR, Value.TRUE);
    gate.propagate(state);
    return state;
  }

  private static void arm(TtlTestInstanceState state) {
    setParallel(state, 0x00);
    setLevel(state, Ttl74597.DS, Value.FALSE);
    setLevel(state, Ttl74597.SHCP, Value.FALSE);
    setLevel(state, Ttl74597.STCP, Value.FALSE);
    setLevel(state, Ttl74597.PL, Value.TRUE);
    setLevel(state, Ttl74597.MR, Value.TRUE);
  }

  /** Clocks the parallel byte into storage and copies it into the shift register. */
  private static void load(Ttl74597 gate, TtlTestInstanceState state, int bits) {
    setParallel(state, bits);
    pulse(gate, state, Ttl74597.STCP);
    setLevel(state, Ttl74597.PL, Value.FALSE);
    gate.propagate(state);
    setLevel(state, Ttl74597.PL, Value.TRUE);
    gate.propagate(state);
  }

  private static void setParallel(TtlTestInstanceState state, int bits) {
    for (var stage = 0; stage < DATA.length; stage++) {
      final var bit = ((bits >> stage) & 1) == 0 ? Value.FALSE : Value.TRUE;
      setLevel(state, DATA[stage], bit);
    }
  }

  private static void pulse(Ttl74597 gate, TtlTestInstanceState state, byte clockPin) {
    setLevel(state, clockPin, Value.TRUE);
    gate.propagate(state);
    setLevel(state, clockPin, Value.FALSE);
    gate.propagate(state);
  }

  private static void riseBoth(Ttl74597 gate, TtlTestInstanceState state) {
    setLevel(state, Ttl74597.SHCP, Value.FALSE);
    setLevel(state, Ttl74597.STCP, Value.FALSE);
    gate.propagate(state);
    setLevel(state, Ttl74597.SHCP, Value.TRUE);
    setLevel(state, Ttl74597.STCP, Value.TRUE);
    gate.propagate(state);
    setLevel(state, Ttl74597.SHCP, Value.FALSE);
    setLevel(state, Ttl74597.STCP, Value.FALSE);
    gate.propagate(state);
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74597.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl74597.pinNrToPortNr(dsPinNr), value);
  }

  private static Value level(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74597.pinNrToPortNr(dsPinNr));
  }
}
