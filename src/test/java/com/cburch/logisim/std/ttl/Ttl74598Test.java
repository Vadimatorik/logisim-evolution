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

/** Functional tests for the SN74LS598 8-bit shift register with input latches. */
class Ttl74598Test {
  private static final int PATTERN = 0xA5;
  private static final int REPLACEMENT = 0x5A;
  private static final int GND_PORT = 18;
  private static final int VCC_PORT = 19;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74598();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(18, hiddenPower.getPorts().size());
    assertPin(hiddenPower, Ttl74598.A, 10, 30, EndData.INPUT_OUTPUT);
    assertPin(hiddenPower, Ttl74598.B, 30, 30, EndData.INPUT_OUTPUT);
    assertPin(hiddenPower, Ttl74598.C, 50, 30, EndData.INPUT_OUTPUT);
    assertPin(hiddenPower, Ttl74598.D, 70, 30, EndData.INPUT_OUTPUT);
    assertPin(hiddenPower, Ttl74598.E, 90, 30, EndData.INPUT_OUTPUT);
    assertPin(hiddenPower, Ttl74598.F, 110, 30, EndData.INPUT_OUTPUT);
    assertPin(hiddenPower, Ttl74598.GQ, 130, 30, EndData.INPUT_OUTPUT);
    assertPin(hiddenPower, Ttl74598.H, 150, 30, EndData.INPUT_OUTPUT);
    assertPin(hiddenPower, Ttl74598.SRLOAD, 170, 30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74598.QH_PRIME, 190, -30, EndData.OUTPUT_ONLY);
    assertPin(hiddenPower, Ttl74598.SRCLR, 170, -30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74598.SRCK, 150, -30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74598.SRCKEN, 130, -30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74598.RCK, 110, -30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74598.OE, 90, -30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74598.SER1, 70, -30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74598.SER0, 50, -30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74598.DS, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(20, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 190, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void clocksAreTheStorageAndShiftPins() {
    final var gate = new Ttl74598();
    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(
        new int[] {Ttl74598.pinNrToPortNr(Ttl74598.RCK), Ttl74598.pinNrToPortNr(Ttl74598.SRCK)},
        gate.clockPinIndex(null));
  }

  @Test
  void risingStorageClockLatchesAndLoadCopiesTheLatch() {
    final var gate = new Ttl74598();
    final var state = cleared(gate, false);
    latch(gate, state, PATTERN);
    assertPattern(state, 0);

    set(state, Ttl74598.SRLOAD, Value.FALSE);
    gate.propagate(state);
    assertPattern(state, PATTERN);

    set(state, Ttl74598.SRLOAD, Value.TRUE);
    gate.propagate(state);
    assertPattern(state, PATTERN);
  }

  @Test
  void fallingStorageClockDoesNotLatch() {
    final var gate = new Ttl74598();
    final var state = cleared(gate, false);
    set(state, Ttl74598.OE, Value.TRUE);
    set(state, Ttl74598.RCK, Value.FALSE);
    gate.propagate(state);
    drive(state, PATTERN);
    set(state, Ttl74598.RCK, Value.TRUE);
    gate.propagate(state);
    drive(state, REPLACEMENT);
    set(state, Ttl74598.RCK, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl74598.OE, Value.FALSE);
    gate.propagate(state);
    load(gate, state);
    assertPattern(state, PATTERN);
  }

  @Test
  void clearResetsTheShiftRegisterAndLeavesTheLatch() {
    final var gate = new Ttl74598();
    final var state = cleared(gate, false);
    latch(gate, state, PATTERN);
    load(gate, state);
    assertPattern(state, PATTERN);

    set(state, Ttl74598.SRCLR, Value.FALSE);
    set(state, Ttl74598.SRCK, Value.TRUE);
    set(state, Ttl74598.SRCKEN, Value.FALSE);
    set(state, Ttl74598.SER0, Value.TRUE);
    gate.propagate(state);
    assertPattern(state, 0);

    set(state, Ttl74598.SRCLR, Value.TRUE);
    set(state, Ttl74598.SRCK, Value.FALSE);
    gate.propagate(state);
    assertPattern(state, 0);

    load(gate, state);
    assertPattern(state, PATTERN);
  }

  @Test
  void clearAndLoadTogetherMakeTheShiftRegisterUnknown() {
    final var gate = new Ttl74598();
    final var state = cleared(gate, false);
    latch(gate, state, PATTERN);
    load(gate, state);

    set(state, Ttl74598.SRCLR, Value.FALSE);
    set(state, Ttl74598.SRLOAD, Value.FALSE);
    gate.propagate(state);
    assertReleasedShift(state);
  }

  @Test
  void loadWhileTheShiftClockIsHighStillCopiesTheLatch() {
    final var gate = new Ttl74598();
    final var state = cleared(gate, false);
    latch(gate, state, PATTERN);
    set(state, Ttl74598.SRCK, Value.TRUE);
    gate.propagate(state);

    set(state, Ttl74598.SRLOAD, Value.FALSE);
    gate.propagate(state);
    assertPattern(state, PATTERN);
  }

  @Test
  void loadFollowsANewLatchWithoutWaitingForItsOwnEdge() {
    final var gate = new Ttl74598();
    final var state = cleared(gate, false);
    latch(gate, state, PATTERN);
    set(state, Ttl74598.SRLOAD, Value.FALSE);
    set(state, Ttl74598.OE, Value.TRUE);
    gate.propagate(state);
    drive(state, REPLACEMENT);
    set(state, Ttl74598.RCK, Value.TRUE);
    gate.propagate(state);
    set(state, Ttl74598.RCK, Value.FALSE);
    set(state, Ttl74598.OE, Value.FALSE);
    gate.propagate(state);
    assertPattern(state, REPLACEMENT);
  }

  @Test
  void shiftMovesTowardHAndSamplesTheSelectedSerialInput() {
    final var gate = new Ttl74598();
    final var state = cleared(gate, false);
    latch(gate, state, 0x01);
    load(gate, state);
    assertPattern(state, 0x01);

    set(state, Ttl74598.SRCKEN, Value.FALSE);
    set(state, Ttl74598.DS, Value.FALSE);
    set(state, Ttl74598.SER0, Value.FALSE);
    set(state, Ttl74598.SER1, Value.TRUE);
    rise(gate, state, Ttl74598.SRCK);
    assertPattern(state, 0x02);

    set(state, Ttl74598.DS, Value.TRUE);
    rise(gate, state, Ttl74598.SRCK);
    assertPattern(state, 0x05);
  }

  @Test
  void shiftEnableHighAndFallingShiftClockHold() {
    final var gate = new Ttl74598();
    final var state = cleared(gate, false);
    latch(gate, state, PATTERN);
    load(gate, state);

    set(state, Ttl74598.SRCKEN, Value.TRUE);
    set(state, Ttl74598.SER0, Value.TRUE);
    rise(gate, state, Ttl74598.SRCK);
    assertPattern(state, PATTERN);

    set(state, Ttl74598.SRCKEN, Value.FALSE);
    set(state, Ttl74598.SRCK, Value.FALSE);
    gate.propagate(state);
    assertPattern(state, PATTERN);
  }

  @Test
  void outputEnableReleasesParallelPinsAndLeavesTheSerialOutput() {
    final var gate = new Ttl74598();
    final var state = cleared(gate, false);
    latch(gate, state, 0x80);
    load(gate, state);
    assertPattern(state, 0x80);

    set(state, Ttl74598.OE, Value.TRUE);
    gate.propagate(state);
    for (var index = 0; index < 8; index++) {
      assertEquals(Value.UNKNOWN, state.getPortValue(index));
    }
    assertEquals(Value.TRUE, serial(state));
  }

  @Test
  void unknownSelectCopiesTheSerialBitWhenBothInputsMatch() {
    final var gate = new Ttl74598();
    final var state = cleared(gate, false);
    set(state, Ttl74598.SRCKEN, Value.FALSE);
    set(state, Ttl74598.DS, Value.UNKNOWN);
    set(state, Ttl74598.SER0, Value.TRUE);
    set(state, Ttl74598.SER1, Value.TRUE);
    rise(gate, state, Ttl74598.SRCK);
    assertEquals(Value.TRUE, state.getPortValue(Ttl74598.pinNrToPortNr(Ttl74598.A)));
    assertEquals(Value.FALSE, serial(state));
  }

  @Test
  void unknownSelectDisagreesWhenTheSerialInputsDiffer() {
    final var gate = new Ttl74598();
    final var state = cleared(gate, false);
    set(state, Ttl74598.SRCKEN, Value.FALSE);
    set(state, Ttl74598.DS, Value.UNKNOWN);
    set(state, Ttl74598.SER0, Value.FALSE);
    set(state, Ttl74598.SER1, Value.TRUE);
    rise(gate, state, Ttl74598.SRCK);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74598.pinNrToPortNr(Ttl74598.A)));
    assertEquals(Value.FALSE, state.getPortValue(Ttl74598.pinNrToPortNr(Ttl74598.B)));
  }

  @Test
  void errorSerialInputMakesTheShiftedBitAnError() {
    final var gate = new Ttl74598();
    final var state = cleared(gate, false);
    set(state, Ttl74598.SRCKEN, Value.FALSE);
    set(state, Ttl74598.DS, Value.FALSE);
    set(state, Ttl74598.SER0, Value.ERROR);
    rise(gate, state, Ttl74598.SRCK);
    assertEquals(Value.ERROR, state.getPortValue(Ttl74598.pinNrToPortNr(Ttl74598.A)));
    assertEquals(Value.FALSE, serial(state));
  }

  @Test
  void unknownLoadHoldsWhenTheLatchMatchesTheShiftRegister() {
    final var gate = new Ttl74598();
    final var state = cleared(gate, false);
    latch(gate, state, PATTERN);
    load(gate, state);
    set(state, Ttl74598.SRLOAD, Value.UNKNOWN);
    gate.propagate(state);
    assertPattern(state, PATTERN);
  }

  @Test
  void unknownLoadDisagreesWhenTheLatchDiffers() {
    final var gate = new Ttl74598();
    final var state = cleared(gate, false);
    latch(gate, state, PATTERN);
    set(state, Ttl74598.SRLOAD, Value.UNKNOWN);
    gate.propagate(state);
    for (var index = 0; index < 8; index++) {
      final var expected = ((PATTERN >> index) & 1) == 0 ? Value.FALSE : Value.UNKNOWN;
      assertEquals(expected, state.getPortValue(index));
    }
    assertEquals(Value.UNKNOWN, serial(state));
  }

  @Test
  void invalidPowerReleasesOutputsWithoutForgettingState() {
    final var gate = new Ttl74598();
    final var state = cleared(gate, true);
    latch(gate, state, PATTERN);
    load(gate, state);
    assertPattern(state, PATTERN);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertReleasedShift(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertPattern(state, PATTERN);
  }

  private static TtlTestInstanceState cleared(Ttl74598 gate, boolean showPowerPins) {
    final var state = new TtlTestInstanceState(gate, showPowerPins);
    if (showPowerPins) {
      state.setPortValue(GND_PORT, Value.FALSE);
      state.setPortValue(VCC_PORT, Value.TRUE);
    }
    set(state, Ttl74598.SRCLR, Value.TRUE);
    set(state, Ttl74598.SRLOAD, Value.TRUE);
    set(state, Ttl74598.SRCK, Value.FALSE);
    set(state, Ttl74598.SRCKEN, Value.TRUE);
    set(state, Ttl74598.RCK, Value.FALSE);
    set(state, Ttl74598.OE, Value.FALSE);
    set(state, Ttl74598.DS, Value.FALSE);
    set(state, Ttl74598.SER0, Value.FALSE);
    set(state, Ttl74598.SER1, Value.FALSE);
    drive(state, 0);
    set(state, Ttl74598.SRCLR, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl74598.SRCLR, Value.TRUE);
    gate.propagate(state);
    return state;
  }

  /** Latches {@code pattern} on a rising {@code RCK} while the parallel pins are inputs. */
  private static void latch(Ttl74598 gate, TtlTestInstanceState state, int pattern) {
    set(state, Ttl74598.OE, Value.TRUE);
    set(state, Ttl74598.SRLOAD, Value.TRUE);
    set(state, Ttl74598.RCK, Value.FALSE);
    gate.propagate(state);
    drive(state, pattern);
    set(state, Ttl74598.RCK, Value.TRUE);
    gate.propagate(state);
    set(state, Ttl74598.RCK, Value.FALSE);
    set(state, Ttl74598.OE, Value.FALSE);
    gate.propagate(state);
  }

  private static void load(Ttl74598 gate, TtlTestInstanceState state) {
    set(state, Ttl74598.OE, Value.FALSE);
    set(state, Ttl74598.SRLOAD, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl74598.SRLOAD, Value.TRUE);
    gate.propagate(state);
  }

  private static void rise(Ttl74598 gate, TtlTestInstanceState state, byte pin) {
    set(state, pin, Value.FALSE);
    gate.propagate(state);
    set(state, pin, Value.TRUE);
    gate.propagate(state);
  }

  private static void drive(TtlTestInstanceState state, int pattern) {
    for (var index = 0; index < 8; index++) {
      final var bit = ((pattern >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
      state.setPortValue(index, bit);
    }
  }

  private static void set(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl74598.pinNrToPortNr(pin), value);
  }

  private static Value serial(TtlTestInstanceState state) {
    return state.getPortValue(Ttl74598.pinNrToPortNr(Ttl74598.QH_PRIME));
  }

  private static void assertPattern(TtlTestInstanceState state, int pattern) {
    for (var index = 0; index < 8; index++) {
      final var expected = ((pattern >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
      assertEquals(expected, state.getPortValue(index), "parallel bit " + index);
    }
    final var serialBit = ((pattern >> 7) & 1) == 0 ? Value.FALSE : Value.TRUE;
    assertEquals(serialBit, serial(state));
  }

  private static void assertReleasedShift(TtlTestInstanceState state) {
    for (var index = 0; index < 8; index++) {
      assertEquals(Value.UNKNOWN, state.getPortValue(index));
    }
    assertEquals(Value.UNKNOWN, serial(state));
  }

  private static void assertPin(Instance instance, byte pin, int x, int y, int type) {
    assertPort(instance, Ttl74598.pinNrToPortNr(pin), x, y, type);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }
}
