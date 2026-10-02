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

/** Functional tests for the 74HC137 3-to-8 decoder with address latches. */
class Ttl74137Test {
  private static final byte[] OUTPUTS = {
    Ttl74137.Y0, Ttl74137.Y1, Ttl74137.Y2, Ttl74137.Y3,
    Ttl74137.Y4, Ttl74137.Y5, Ttl74137.Y6, Ttl74137.Y7
  };
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;
  private static final int NONE = -1;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74137();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74137.A0, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74137.A1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74137.A2, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74137.LE, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74137.E1, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74137.E2, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74137.Y7, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74137.Y6, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74137.Y5, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74137.Y4, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74137.Y3, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74137.Y2, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74137.Y1, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74137.Y0, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void latchEnableIsNotAGatedClock() {
    assertFalse(new Ttl74137().checkForGatedClocks(null));
  }

  @Test
  void transparentModeSelectsEachActiveLowOutput() {
    final var gate = new Ttl74137();
    final var state = driven(gate);

    for (var code = 0; code < OUTPUTS.length; code++) {
      setAddress(state, code);
      gate.propagate(state);
      assertDecoded(state, code);
    }
  }

  @Test
  void addressFollowsThePinsWhileLatchEnableStaysLow() {
    final var gate = new Ttl74137();
    final var state = driven(gate);
    setAddress(state, 3);
    gate.propagate(state);
    assertDecoded(state, 3);

    setAddress(state, 0);
    gate.propagate(state);
    assertDecoded(state, 0);
  }

  @Test
  void risingLatchEnableKeepsTheAddressFromBeforeTheEdge() {
    final var gate = new Ttl74137();
    final var state = driven(gate);
    setAddress(state, 3);
    gate.propagate(state);

    setLevel(state, Ttl74137.LE, true);
    setAddress(state, 0);
    gate.propagate(state);
    assertDecoded(state, 3);
  }

  @Test
  void addressChangesWhileLatchedAreIgnored() {
    final var gate = new Ttl74137();
    final var state = latched(gate, 5);

    setAddress(state, 1);
    gate.propagate(state);
    assertDecoded(state, 5);

    setLevel(state, Ttl74137.A0, Value.UNKNOWN);
    gate.propagate(state);
    assertDecoded(state, 5);
  }

  @Test
  void outputEnablesHideTheCodeWithoutChangingTheLatch() {
    final var gate = new Ttl74137();
    final var state = latched(gate, 4);

    setLevel(state, Ttl74137.E1, true);
    setAddress(state, 1);
    gate.propagate(state);
    assertDecoded(state, NONE);

    setLevel(state, Ttl74137.E1, false);
    gate.propagate(state);
    assertDecoded(state, 4);

    setLevel(state, Ttl74137.E2, false);
    gate.propagate(state);
    assertDecoded(state, NONE);

    setLevel(state, Ttl74137.E2, true);
    gate.propagate(state);
    assertDecoded(state, 4);
  }

  @Test
  void loweringLatchEnableFollowsTheNewAddressImmediately() {
    final var gate = new Ttl74137();
    final var state = latched(gate, 6);
    setAddress(state, 2);
    setLevel(state, Ttl74137.LE, false);
    gate.propagate(state);
    assertDecoded(state, 2);
  }

  @Test
  void unknownAddressAffectsOnlyTheCandidateOutputs() {
    final var gate = new Ttl74137();
    final var state = driven(gate);
    setLevel(state, Ttl74137.A0, Value.UNKNOWN);
    setLevel(state, Ttl74137.A1, false);
    setLevel(state, Ttl74137.A2, false);
    gate.propagate(state);

    assertOutput(state, Ttl74137.Y0, Value.UNKNOWN);
    assertOutput(state, Ttl74137.Y1, Value.UNKNOWN);
    assertOutput(state, Ttl74137.Y2, Value.TRUE);
    assertOutput(state, Ttl74137.Y7, Value.TRUE);
  }

  @Test
  void unknownAddressStaysStoredAfterTheLatchCloses() {
    final var gate = new Ttl74137();
    final var state = driven(gate);
    setLevel(state, Ttl74137.A0, Value.ERROR);
    setLevel(state, Ttl74137.A1, false);
    setLevel(state, Ttl74137.A2, false);
    gate.propagate(state);

    setLevel(state, Ttl74137.LE, true);
    setLevel(state, Ttl74137.A0, false);
    gate.propagate(state);
    assertOutput(state, Ttl74137.Y0, Value.ERROR);
    assertOutput(state, Ttl74137.Y1, Value.ERROR);
    assertOutput(state, Ttl74137.Y2, Value.TRUE);
  }

  @Test
  void unknownLatchEnableMergesTheTransparentAndStoredCodes() {
    final var gate = new Ttl74137();
    final var state = latched(gate, 5);
    setAddress(state, 0);
    setLevel(state, Ttl74137.LE, Value.UNKNOWN);
    gate.propagate(state);

    assertOutput(state, Ttl74137.Y0, Value.UNKNOWN);
    assertOutput(state, Ttl74137.Y5, Value.UNKNOWN);
    assertOutput(state, Ttl74137.Y1, Value.TRUE);

    setLevel(state, Ttl74137.LE, true);
    gate.propagate(state);
    assertDecoded(state, 5);
  }

  @Test
  void errorOnAnEnableMakesOnlyTheSelectedOutputAnError() {
    final var gate = new Ttl74137();
    final var state = latched(gate, 2);
    setLevel(state, Ttl74137.E1, Value.ERROR);
    gate.propagate(state);

    assertOutput(state, Ttl74137.Y2, Value.ERROR);
    assertOutput(state, Ttl74137.Y0, Value.TRUE);
    assertOutput(state, Ttl74137.Y7, Value.TRUE);

    setLevel(state, Ttl74137.E1, false);
    setLevel(state, Ttl74137.E2, Value.UNKNOWN);
    gate.propagate(state);
    assertOutput(state, Ttl74137.Y2, Value.UNKNOWN);
    assertOutput(state, Ttl74137.Y3, Value.TRUE);
  }

  @Test
  void latchedStartupFollowsTheMemoryPreference() {
    final var gate = new Ttl74137();
    final var state = enabled(gate);
    setLevel(state, Ttl74137.LE, true);
    setAddress(state, 7);
    gate.propagate(state);

    if (AppPreferences.Memory_Startup_Unknown.get()) {
      for (final var output : OUTPUTS) {
        assertOutput(state, output, Value.UNKNOWN);
      }
    } else {
      assertDecoded(state, 0);
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74137();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    enable(state);
    setLevel(state, Ttl74137.LE, false);
    setAddress(state, 3);
    gate.propagate(state);
    assertDecoded(state, 3);

    setLevel(state, Ttl74137.LE, true);
    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    for (final var output : OUTPUTS) {
      assertOutput(state, output, Value.UNKNOWN);
    }

    state.setPortValue(VCC_PORT, Value.TRUE);
    setAddress(state, 0);
    gate.propagate(state);
    assertDecoded(state, 3);
  }

  /** Enabled, transparent device. The first propagation establishes a known latch state. */
  private static TtlTestInstanceState driven(Ttl74137 gate) {
    final var state = enabled(gate);
    setLevel(state, Ttl74137.LE, false);
    setAddress(state, 0);
    gate.propagate(state);
    return state;
  }

  private static TtlTestInstanceState latched(Ttl74137 gate, int code) {
    final var state = driven(gate);
    setAddress(state, code);
    gate.propagate(state);
    setLevel(state, Ttl74137.LE, true);
    gate.propagate(state);
    return state;
  }

  private static TtlTestInstanceState enabled(Ttl74137 gate) {
    final var state = new TtlTestInstanceState(gate, false);
    enable(state);
    return state;
  }

  private static void enable(TtlTestInstanceState state) {
    setLevel(state, Ttl74137.E1, false);
    setLevel(state, Ttl74137.E2, true);
  }

  private static void setAddress(TtlTestInstanceState state, int code) {
    setLevel(state, Ttl74137.A0, (code & 1) != 0);
    setLevel(state, Ttl74137.A1, (code & 2) != 0);
    setLevel(state, Ttl74137.A2, (code & 4) != 0);
  }

  private static void assertDecoded(TtlTestInstanceState state, int selected) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      final var expected = index == selected ? Value.FALSE : Value.TRUE;
      assertOutput(state, OUTPUTS[index], expected);
    }
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74137.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, boolean high) {
    setLevel(state, dsPinNr, high ? Value.TRUE : Value.FALSE);
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl74137.pinNrToPortNr(dsPinNr), value);
  }

  private static void assertOutput(TtlTestInstanceState state, byte dsPinNr, Value expected) {
    assertEquals(expected, state.getPortValue(Ttl74137.pinNrToPortNr(dsPinNr)));
  }
}
