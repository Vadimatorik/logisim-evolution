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
import static org.junit.jupiter.api.Assertions.fail;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC4059 programmable divide-by-n counter. */
class Ttl744059Test {
  private static final int GND_PORT = 22;
  private static final int VCC_PORT = 23;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl744059();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(22, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_CP, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_LE, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_J1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_J2, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_J3, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_J4, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_J16, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_J15, 150, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_J14, 170, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_J13, 190, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_KC, 210, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_KB, 230, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_KA, 210, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_J12, 190, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_J11, 170, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_J10, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_J9, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_J8, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_J7, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_J6, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_J5, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744059.PORT_INDEX_Q, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(24, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 230, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
    assertTrue(gate.checkForGatedClocks(null));
    assertEquals(Ttl744059.PORT_INDEX_CP, gate.clockPinIndex(null)[0]);
  }

  @Test
  void datasheetExamplesDivideByTheProgrammedN() {
    assertDivider(5, 4, 5, 9, 6, 1, 8479);
    assertDivider(8, 6, 7, 4, 5, 1, 12382);
    assertDivider(10, 9, 7, 4, 8, 0, 8479);
    assertDivider(2, 1, 1, 0, 0, 0, 3);
    assertDivider(4, 3, 1, 0, 0, 0, 7);
  }

  @Test
  void outputPulseLastsOneClockAndFallingEdgesDoNotCount() {
    final var gate = new Ttl744059();
    final var state = new TtlTestInstanceState(gate, false);
    masterPreset(gate, state, jam(8, 3, 0, 0, 0, 0));
    selectMode(state, 8, false);

    assertEquals(Value.FALSE, rise(gate, state));
    assertEquals(Value.FALSE, rise(gate, state));
    assertEquals(Value.FALSE, rise(gate, state));
    assertEquals(Value.TRUE, rise(gate, state));
    assertEquals(Value.TRUE, state.getPortValue(Ttl744059.PORT_INDEX_Q));
    assertEquals(Value.FALSE, rise(gate, state));
  }

  @Test
  void timerLatchHoldsUntilLatchEnableFalls() {
    final var gate = new Ttl744059();
    final var state = new TtlTestInstanceState(gate, false);
    masterPreset(gate, state, jam(8, 3, 0, 0, 0, 0));
    selectMode(state, 8, true);

    assertEquals(4, edgesUntilHigh(gate, state, 8));
    for (var extra = 0; extra < 5; extra++) {
      assertEquals(Value.TRUE, rise(gate, state));
    }
    state.setPortValue(Ttl744059.PORT_INDEX_LE, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.FALSE, state.getPortValue(Ttl744059.PORT_INDEX_Q));
    assertEquals(Value.TRUE, rise(gate, state));
  }

  @Test
  void latchEnableHighInTheFixedTenModeDoesNotHold() {
    final var gate = new Ttl744059();
    final var state = new TtlTestInstanceState(gate, false);
    masterPreset(gate, state, jam(10, 0, 1, 0, 0, 0));
    state.setPortValue(Ttl744059.PORT_INDEX_LE, Value.TRUE);
    state.setPortValue(Ttl744059.PORT_INDEX_KA, Value.FALSE);
    state.setPortValue(Ttl744059.PORT_INDEX_KB, Value.TRUE);
    state.setPortValue(Ttl744059.PORT_INDEX_KC, Value.FALSE);

    assertEquals(11, edgesUntilHigh(gate, state, 16));
    assertEquals(10, edgesUntilHigh(gate, state, 16));
  }

  @Test
  void presetInhibitDividesBy10000AfterTheJamCycle() {
    final var gate = new Ttl744059();
    final var state = new TtlTestInstanceState(gate, false);
    masterPreset(gate, state, jam(10, 0, 1, 0, 0, 0));
    state.setPortValue(Ttl744059.PORT_INDEX_LE, Value.FALSE);
    state.setPortValue(Ttl744059.PORT_INDEX_KA, Value.FALSE);
    state.setPortValue(Ttl744059.PORT_INDEX_KB, Value.TRUE);
    state.setPortValue(Ttl744059.PORT_INDEX_KC, Value.FALSE);

    assertEquals(11, edgesUntilHigh(gate, state, 16));
    setJam(state, 0xFFFF);
    assertEquals(10_000, edgesUntilHigh(gate, state, 10_005));
  }

  @Test
  void binaryDecadePresetKeepsDecimalPlaceValues() {
    assertDivider(8, 0, 15, 0, 0, 0, 120);
  }

  @Test
  void jamChangeAppliesOnTheFollowingCycle() {
    final var gate = new Ttl744059();
    final var state = new TtlTestInstanceState(gate, false);
    masterPreset(gate, state, jam(8, 3, 0, 0, 0, 0));
    selectMode(state, 8, false);
    assertEquals(Value.FALSE, rise(gate, state));
    assertEquals(Value.FALSE, rise(gate, state));
    setJam(state, jam(8, 2, 1, 0, 0, 0));
    assertEquals(Value.FALSE, rise(gate, state));
    assertEquals(Value.TRUE, rise(gate, state));
    assertEquals(10, edgesUntilHigh(gate, state, 16));
  }

  @Test
  void masterPresetClocksDoNotShortenTheFirstPeriod() {
    final var gate = new Ttl744059();
    final var state = new TtlTestInstanceState(gate, false);
    final var programmed = jam(8, 3, 0, 0, 0, 0);
    masterPreset(gate, state, programmed);
    for (var extra = 0; extra < 7; extra++) {
      assertEquals(Value.FALSE, rise(gate, state));
    }
    selectMode(state, 8, false);
    assertEquals(4, edgesUntilHigh(gate, state, 8));
  }

  @Test
  void unknownClockDoesNotCount() {
    final var gate = new Ttl744059();
    final var state = new TtlTestInstanceState(gate, false);
    masterPreset(gate, state, jam(8, 3, 0, 0, 0, 0));
    selectMode(state, 8, false);
    assertEquals(Value.FALSE, rise(gate, state));

    state.setPortValue(Ttl744059.PORT_INDEX_CP, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl744059.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.FALSE, state.getPortValue(Ttl744059.PORT_INDEX_Q));
    assertEquals(Value.FALSE, rise(gate, state));
    assertEquals(Value.FALSE, rise(gate, state));
    assertEquals(Value.TRUE, rise(gate, state));
  }

  @Test
  void unknownJamAtPresetOrReloadMakesTheOutputUnknown() {
    final var gate = new Ttl744059();
    final var state = new TtlTestInstanceState(gate, false);
    setJam(state, jam(8, 3, 0, 0, 0, 0));
    state.setPortValue(Ttl744059.PORT_INDEX_J1, Value.UNKNOWN);
    selectMasterPreset(state);
    rise(gate, state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl744059.PORT_INDEX_Q));

    masterPreset(gate, state, jam(8, 3, 0, 0, 0, 0));
    selectMode(state, 8, false);
    assertEquals(Value.FALSE, rise(gate, state));
    assertEquals(Value.FALSE, rise(gate, state));
    assertEquals(Value.FALSE, rise(gate, state));
    state.setPortValue(Ttl744059.PORT_INDEX_J1, Value.UNKNOWN);
    assertEquals(Value.UNKNOWN, rise(gate, state));
  }

  @Test
  void errorOnAModeInputMakesTheOutputAnError() {
    final var gate = new Ttl744059();
    final var state = new TtlTestInstanceState(gate, false);
    masterPreset(gate, state, jam(8, 3, 0, 0, 0, 0));
    selectMode(state, 8, false);
    state.setPortValue(Ttl744059.PORT_INDEX_KA, Value.ERROR);
    assertEquals(Value.ERROR, rise(gate, state));
  }

  @Test
  void invalidExposedPowerInputsMakeTheOutputUnknown() {
    final var gate = new Ttl744059();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    masterPreset(gate, state, jam(8, 3, 0, 0, 0, 0));
    selectMode(state, 8, false);
    assertEquals(4, edgesUntilHigh(gate, state, 8));
    assertEquals(Value.TRUE, state.getPortValue(Ttl744059.PORT_INDEX_Q));

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl744059.PORT_INDEX_Q));
    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.TRUE, state.getPortValue(Ttl744059.PORT_INDEX_Q));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl744059.PORT_INDEX_Q));
    state.setPortValue(GND_PORT, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.TRUE, state.getPortValue(Ttl744059.PORT_INDEX_Q));
  }

  private static void assertDivider(
      int mode, int dec1, int dec2, int dec3, int dec4, int dec5, int divisor) {
    final var gate = new Ttl744059();
    final var state = new TtlTestInstanceState(gate, false);
    masterPreset(gate, state, jam(mode, dec1, dec2, dec3, dec4, dec5));
    selectMode(state, mode, false);
    assertEquals(divisor + 1, edgesUntilHigh(gate, state, divisor + 5), "first period");
    assertEquals(divisor, edgesUntilHigh(gate, state, divisor + 5), "steady period");
  }

  private static void masterPreset(Ttl744059 gate, TtlTestInstanceState state, int jamWord) {
    setJam(state, jamWord);
    selectMasterPreset(state);
    state.setPortValue(Ttl744059.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    for (var clock = 0; clock < 3; clock++) {
      assertEquals(Value.FALSE, rise(gate, state));
    }
  }

  private static void selectMasterPreset(TtlTestInstanceState state) {
    state.setPortValue(Ttl744059.PORT_INDEX_LE, Value.FALSE);
    state.setPortValue(Ttl744059.PORT_INDEX_KA, Value.FALSE);
    state.setPortValue(Ttl744059.PORT_INDEX_KB, Value.FALSE);
    state.setPortValue(Ttl744059.PORT_INDEX_KC, Value.FALSE);
  }

  private static void selectMode(TtlTestInstanceState state, int mode, boolean latch) {
    final boolean kaHigh;
    final boolean kbHigh;
    final boolean kcHigh;
    switch (mode) {
      case 2 -> {
        kaHigh = true;
        kbHigh = true;
        kcHigh = true;
      }
      case 4 -> {
        kaHigh = false;
        kbHigh = true;
        kcHigh = true;
      }
      case 5 -> {
        kaHigh = true;
        kbHigh = false;
        kcHigh = true;
      }
      case 8 -> {
        kaHigh = false;
        kbHigh = false;
        kcHigh = true;
      }
      case 10 -> {
        kaHigh = true;
        kbHigh = true;
        kcHigh = false;
      }
      default -> throw new IllegalArgumentException("mode " + mode);
    }
    state.setPortValue(Ttl744059.PORT_INDEX_LE, latch ? Value.TRUE : Value.FALSE);
    state.setPortValue(Ttl744059.PORT_INDEX_KA, kaHigh ? Value.TRUE : Value.FALSE);
    state.setPortValue(Ttl744059.PORT_INDEX_KB, kbHigh ? Value.TRUE : Value.FALSE);
    state.setPortValue(Ttl744059.PORT_INDEX_KC, kcHigh ? Value.TRUE : Value.FALSE);
  }

  /** Packs decade presets into {@code J1} to {@code J16}, with {@code J1} as bit 0. */
  private static int jam(int mode, int dec1, int dec2, int dec3, int dec4, int dec5) {
    final var decades = ((dec2 & 0xF) << 4) | ((dec3 & 0xF) << 8) | ((dec4 & 0xF) << 12);
    final int first;
    if (mode == 2) first = (dec1 & 0x1) | ((dec5 & 0x7) << 1);
    else if (mode == 4) first = (dec1 & 0x3) | ((dec5 & 0x3) << 2);
    else if (mode == 10) first = dec1 & 0xF;
    else if (mode == 5 || mode == 8) first = (dec1 & 0x7) | ((dec5 & 0x1) << 3);
    else throw new IllegalArgumentException("mode " + mode);
    return decades | first;
  }

  private static void setJam(TtlTestInstanceState state, int jamWord) {
    final var ports = new int[] {
      Ttl744059.PORT_INDEX_J1,
      Ttl744059.PORT_INDEX_J2,
      Ttl744059.PORT_INDEX_J3,
      Ttl744059.PORT_INDEX_J4,
      Ttl744059.PORT_INDEX_J5,
      Ttl744059.PORT_INDEX_J6,
      Ttl744059.PORT_INDEX_J7,
      Ttl744059.PORT_INDEX_J8,
      Ttl744059.PORT_INDEX_J9,
      Ttl744059.PORT_INDEX_J10,
      Ttl744059.PORT_INDEX_J11,
      Ttl744059.PORT_INDEX_J12,
      Ttl744059.PORT_INDEX_J13,
      Ttl744059.PORT_INDEX_J14,
      Ttl744059.PORT_INDEX_J15,
      Ttl744059.PORT_INDEX_J16
    };
    for (var bit = 0; bit < ports.length; bit++) {
      state.setPortValue(ports[bit], ((jamWord >> bit) & 1) == 0 ? Value.FALSE : Value.TRUE);
    }
  }

  private static int edgesUntilHigh(Ttl744059 gate, TtlTestInstanceState state, int limit) {
    for (var edges = 1; edges <= limit; edges++) {
      if (rise(gate, state) == Value.TRUE) return edges;
    }
    fail("Q stayed low");
    return 0;
  }

  private static Value rise(Ttl744059 gate, TtlTestInstanceState state) {
    state.setPortValue(Ttl744059.PORT_INDEX_CP, Value.TRUE);
    gate.propagate(state);
    final var output = state.getPortValue(Ttl744059.PORT_INDEX_Q);
    state.setPortValue(Ttl744059.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    assertEquals(output, state.getPortValue(Ttl744059.PORT_INDEX_Q));
    return output;
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }
}
