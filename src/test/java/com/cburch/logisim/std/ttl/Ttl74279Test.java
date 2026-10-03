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
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC279 quad S-R latch. */
class Ttl74279Test {
  private static final byte[] OUTPUTS = {Ttl74279.Q1, Ttl74279.Q2, Ttl74279.Q3, Ttl74279.Q4};
  private static final byte[] INPUTS = {
    Ttl74279.R1,
    Ttl74279.S1A,
    Ttl74279.S1B,
    Ttl74279.R2,
    Ttl74279.S2,
    Ttl74279.R3,
    Ttl74279.S3A,
    Ttl74279.S3B,
    Ttl74279.R4,
    Ttl74279.S4
  };
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74279();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74279.R1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74279.S1A, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74279.S1B, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74279.Q1, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74279.R2, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74279.S2, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74279.Q2, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74279.Q3, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74279.R3, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74279.S3A, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74279.S3B, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74279.Q4, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74279.R4, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74279.S4, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void latchEnableIsNotAnEdgeClock() {
    assertFalse(new Ttl74279().checkForGatedClocks(null));
  }

  @Test
  void startupLevelIsHeldUntilSetOrReset() {
    final var state = heldState();
    assertOutputs(state, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
  }

  @Test
  void eachLatchSetsResetsAndHolds() {
    final var gate = new Ttl74279();
    final var state = heldState();

    setLatch(state, 0, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE);

    holdLatch(state, 0);
    gate.propagate(state);
    assertOutputs(state, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE);

    setLatch(state, 0, Value.TRUE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);

    for (var index = 1; index < OUTPUTS.length; index++) {
      setLatch(state, index, Value.FALSE, Value.TRUE, Value.TRUE);
      gate.propagate(state);
      assertOutput(state, index, Value.TRUE);
      holdLatch(state, index);
      gate.propagate(state);
      assertOutput(state, index, Value.TRUE);
      setLatch(state, index, Value.TRUE, Value.TRUE, Value.FALSE);
      gate.propagate(state);
      assertOutput(state, index, Value.FALSE);
    }
  }

  @Test
  void eitherSetInputOfADualLatchSetsIt() {
    final var gate = new Ttl74279();
    final var state = heldState();

    setLatch(state, 0, Value.TRUE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertOutput(state, 0, Value.TRUE);

    setLatch(state, 0, Value.TRUE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    setLatch(state, 2, Value.FALSE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertOutput(state, 0, Value.FALSE);
    assertOutput(state, 2, Value.TRUE);

    setLatch(state, 2, Value.TRUE, Value.TRUE, Value.TRUE);
    setPin(state, Ttl74279.S3A, Value.TRUE);
    setPin(state, Ttl74279.S3B, Value.FALSE);
    setPin(state, Ttl74279.R3, Value.FALSE);
    gate.propagate(state);
    assertOutput(state, 2, Value.TRUE);
  }

  @Test
  void latchesDoNotDisturbEachOther() {
    final var gate = new Ttl74279();
    final var state = heldState();
    setLatch(state, 0, Value.FALSE, Value.TRUE, Value.TRUE);
    setLatch(state, 1, Value.TRUE, Value.TRUE, Value.FALSE);
    setLatch(state, 2, Value.TRUE, Value.FALSE, Value.TRUE);
    setLatch(state, 3, Value.TRUE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    holdAll(state);
    gate.propagate(state);
    assertOutputs(state, Value.TRUE, Value.FALSE, Value.TRUE, Value.FALSE);

    setLatch(state, 1, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, Value.TRUE, Value.TRUE, Value.TRUE, Value.FALSE);
  }

  @Test
  void bothInputsLowForceHighUntilOneIsReleased() {
    final var gate = new Ttl74279();
    final var state = heldState();

    setLatch(state, 1, Value.FALSE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    assertOutput(state, 1, Value.TRUE);

    setLatch(state, 1, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertOutput(state, 1, Value.TRUE);

    holdLatch(state, 1);
    gate.propagate(state);
    assertOutput(state, 1, Value.TRUE);

    setLatch(state, 1, Value.FALSE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    setLatch(state, 1, Value.TRUE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    assertOutput(state, 1, Value.FALSE);

    setLatch(state, 0, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    setPin(state, Ttl74279.S1A, Value.TRUE);
    gate.propagate(state);
    assertOutput(state, 0, Value.TRUE);
    setPin(state, Ttl74279.S1B, Value.TRUE);
    setPin(state, Ttl74279.R1, Value.TRUE);
    gate.propagate(state);
    assertOutput(state, 0, Value.UNKNOWN);
  }

  @Test
  void releasingBothInputsTogetherMakesTheLevelUnknown() {
    final var gate = new Ttl74279();
    final var state = heldState();

    setLatch(state, 3, Value.FALSE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    holdLatch(state, 3);
    gate.propagate(state);

    assertOutput(state, 3, Value.UNKNOWN);
    gate.propagate(state);
    assertOutput(state, 3, Value.UNKNOWN);

    setLatch(state, 3, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    assertOutput(state, 3, Value.TRUE);
  }

  @Test
  void lowSetForcesHighWhenResetIsNotBinary() {
    final var gate = new Ttl74279();
    final var state = heldState();

    setLatch(state, 1, Value.FALSE, Value.TRUE, Value.UNKNOWN);
    gate.propagate(state);
    assertOutput(state, 1, Value.TRUE);

    setLatch(state, 2, Value.FALSE, Value.ERROR, Value.ERROR);
    gate.propagate(state);
    assertOutput(state, 2, Value.TRUE);
  }

  @Test
  void unknownInputsCombineTheBinaryPossibilities() {
    final var gate = new Ttl74279();
    final var state = heldState();
    setLatch(state, 0, Value.FALSE, Value.TRUE, Value.TRUE);
    setLatch(state, 1, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    holdAll(state);
    gate.propagate(state);

    setPin(state, Ttl74279.S2, Value.UNKNOWN);
    gate.propagate(state);
    assertOutput(state, 1, Value.TRUE);

    setLatch(state, 1, Value.TRUE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    holdLatch(state, 1);
    gate.propagate(state);
    setPin(state, Ttl74279.S2, Value.UNKNOWN);
    gate.propagate(state);
    assertOutput(state, 1, Value.UNKNOWN);

    setLatch(state, 1, Value.TRUE, Value.TRUE, Value.FALSE);
    gate.propagate(state);
    holdLatch(state, 1);
    gate.propagate(state);
    setPin(state, Ttl74279.R2, Value.UNKNOWN);
    gate.propagate(state);
    assertOutput(state, 1, Value.FALSE);

    setLatch(state, 1, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    holdLatch(state, 1);
    gate.propagate(state);
    setPin(state, Ttl74279.R2, Value.UNKNOWN);
    gate.propagate(state);
    assertOutput(state, 1, Value.UNKNOWN);

    holdLatch(state, 1);
    gate.propagate(state);
    setPin(state, Ttl74279.R2, Value.FALSE);
    setPin(state, Ttl74279.S2, Value.ERROR);
    gate.propagate(state);
    assertOutput(state, 1, Value.ERROR);

    setPin(state, Ttl74279.S1A, Value.UNKNOWN);
    setPin(state, Ttl74279.S1B, Value.TRUE);
    setPin(state, Ttl74279.R1, Value.TRUE);
    gate.propagate(state);
    assertOutput(state, 0, Value.TRUE);
    assertOutput(state, 1, Value.ERROR);
  }

  @Test
  void invalidExposedPowerLeavesTheStoredLevelUntouched() {
    final var gate = new Ttl74279();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    holdAll(state);
    setLatch(state, 0, Value.FALSE, Value.TRUE, Value.TRUE);
    gate.propagate(state);
    holdAll(state);
    gate.propagate(state);
    assertOutput(state, 0, Value.TRUE);

    state.setPortValue(VCC_PORT, Value.FALSE);
    setLatch(state, 0, Value.FALSE, Value.FALSE, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    holdAll(state);
    gate.propagate(state);
    assertOutput(state, 0, Value.TRUE);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  private static TtlTestInstanceState heldState() {
    final var state = new TtlTestInstanceState(new Ttl74279(), false);
    holdAll(state);
    new Ttl74279().propagate(state);
    return state;
  }

  private static void holdAll(TtlTestInstanceState state) {
    for (final var pin : INPUTS) {
      setPin(state, pin, Value.TRUE);
    }
  }

  private static void holdLatch(TtlTestInstanceState state, int index) {
    setLatch(state, index, Value.TRUE, Value.TRUE, Value.TRUE);
  }

  /** Drives one latch. For a single-set latch {@code setB} is ignored. */
  private static void setLatch(
      TtlTestInstanceState state, int index, Value setA, Value setB, Value reset) {
    switch (index) {
      case 0 -> {
        setPin(state, Ttl74279.S1A, setA);
        setPin(state, Ttl74279.S1B, setB);
        setPin(state, Ttl74279.R1, reset);
      }
      case 1 -> {
        setPin(state, Ttl74279.S2, setA);
        setPin(state, Ttl74279.R2, reset);
      }
      case 2 -> {
        setPin(state, Ttl74279.S3A, setA);
        setPin(state, Ttl74279.S3B, setB);
        setPin(state, Ttl74279.R3, reset);
      }
      case 3 -> {
        setPin(state, Ttl74279.S4, setA);
        setPin(state, Ttl74279.R4, reset);
      }
      default -> throw new IllegalArgumentException("latch " + index);
    }
  }

  private static void assertOutputs(
      TtlTestInstanceState state, Value q1, Value q2, Value q3, Value q4) {
    assertOutput(state, 0, q1);
    assertOutput(state, 1, q2);
    assertOutput(state, 2, q3);
    assertOutput(state, 3, q4);
  }

  private static void assertOutput(TtlTestInstanceState state, int index, Value expected) {
    assertEquals(expected, state.getPortValue(Ttl74279.pinNrToPortNr(OUTPUTS[index])));
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var output : OUTPUTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74279.pinNrToPortNr(output)));
    }
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74279.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setPin(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl74279.pinNrToPortNr(pin), value);
  }
}
