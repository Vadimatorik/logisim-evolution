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

/** Functional tests for the 74HC533 octal inverting transparent latch. */
class Ttl74533Test {
  private static final int GND_PORT = 18;
  private static final int VCC_PORT = 19;
  private static final byte[] OUTPUTS = {
    Ttl74533.Q0, Ttl74533.Q1, Ttl74533.Q2, Ttl74533.Q3,
    Ttl74533.Q4, Ttl74533.Q5, Ttl74533.Q6, Ttl74533.Q7
  };
  private static final byte[] INPUTS = {
    Ttl74533.D0, Ttl74533.D1, Ttl74533.D2, Ttl74533.D3,
    Ttl74533.D4, Ttl74533.D5, Ttl74533.D6, Ttl74533.D7
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74533();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(18, hiddenPower.getPorts().size());
    assertPin(hiddenPower, Ttl74533.OE, 10, 30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74533.Q0, 30, 30, EndData.OUTPUT_ONLY);
    assertPin(hiddenPower, Ttl74533.D0, 50, 30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74533.D1, 70, 30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74533.Q1, 90, 30, EndData.OUTPUT_ONLY);
    assertPin(hiddenPower, Ttl74533.Q2, 110, 30, EndData.OUTPUT_ONLY);
    assertPin(hiddenPower, Ttl74533.D2, 130, 30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74533.D3, 150, 30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74533.Q3, 170, 30, EndData.OUTPUT_ONLY);
    assertPin(hiddenPower, Ttl74533.LE, 190, -30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74533.Q4, 170, -30, EndData.OUTPUT_ONLY);
    assertPin(hiddenPower, Ttl74533.D4, 150, -30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74533.D5, 130, -30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74533.Q5, 110, -30, EndData.OUTPUT_ONLY);
    assertPin(hiddenPower, Ttl74533.Q6, 90, -30, EndData.OUTPUT_ONLY);
    assertPin(hiddenPower, Ttl74533.D6, 70, -30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74533.D7, 50, -30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74533.Q7, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(20, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 190, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void latchEnableIsLevelSensitive() {
    assertFalse(new Ttl74533().checkForGatedClocks(null));
  }

  @Test
  void transparentModeInvertsDataImmediately() {
    final var gate = new Ttl74533();
    final var state = enabled(gate, false);
    final var patterns = new int[] {0x00, 0xFF, 0x55, 0xAA, 0x0F, 0xF0};

    for (final var pattern : patterns) {
      present(gate, state, pattern);
      assertWord(state, (~pattern) & 0xFF);
    }
    for (var bit = 0; bit < 8; bit++) {
      final var pattern = 1 << bit;
      present(gate, state, pattern);
      assertWord(state, (~pattern) & 0xFF);
    }
  }

  @Test
  void lowLatchEnableHoldsWhileDataChanges() {
    final var gate = new Ttl74533();
    final var state = enabled(gate, false);
    present(gate, state, 0x3C);
    assertWord(state, (~0x3C) & 0xFF);

    set(state, Ttl74533.LE, Value.FALSE);
    gate.propagate(state);
    present(gate, state, ~0x3C);
    assertWord(state, (~0x3C) & 0xFF);
  }

  @Test
  void outputEnableReleasesPinsWithoutChangingTheLatch() {
    final var gate = new Ttl74533();
    final var state = enabled(gate, false);
    present(gate, state, 0xA5);
    set(state, Ttl74533.LE, Value.FALSE);
    gate.propagate(state);

    set(state, Ttl74533.OE, Value.TRUE);
    present(gate, state, 0x5A);
    assertReleased(state);

    set(state, Ttl74533.OE, Value.FALSE);
    gate.propagate(state);
    assertWord(state, (~0xA5) & 0xFF);

    set(state, Ttl74533.OE, Value.TRUE);
    set(state, Ttl74533.LE, Value.TRUE);
    present(gate, state, 0x0F);
    assertReleased(state);

    set(state, Ttl74533.OE, Value.FALSE);
    gate.propagate(state);
    assertWord(state, (~0x0F) & 0xFF);
  }

  @Test
  void unknownLatchEnableCorruptsOnlyBitsThatWouldChange() {
    final var gate = new Ttl74533();
    final var state = enabled(gate, false);
    present(gate, state, 0x0F);
    set(state, Ttl74533.LE, Value.FALSE);
    gate.propagate(state);

    set(state, Ttl74533.LE, Value.UNKNOWN);
    present(gate, state, 0x00);
    assertBit(state, 0, Value.UNKNOWN);
    assertBit(state, 1, Value.UNKNOWN);
    assertBit(state, 2, Value.UNKNOWN);
    assertBit(state, 3, Value.UNKNOWN);
    assertBit(state, 4, Value.TRUE);
    assertBit(state, 5, Value.TRUE);
    assertBit(state, 6, Value.TRUE);
    assertBit(state, 7, Value.TRUE);
  }

  @Test
  void errorLatchEnableMarksOnlyDisagreedBitsAsError() {
    final var gate = new Ttl74533();
    final var state = enabled(gate, false);
    present(gate, state, 0x0F);
    set(state, Ttl74533.LE, Value.FALSE);
    gate.propagate(state);

    set(state, Ttl74533.LE, Value.ERROR);
    present(gate, state, 0x00);
    assertBit(state, 0, Value.ERROR);
    assertBit(state, 3, Value.ERROR);
    assertBit(state, 4, Value.TRUE);
    assertBit(state, 7, Value.TRUE);
  }

  @Test
  void unknownOrErrorDataAffectsOnlyThatBitWhileTransparent() {
    final var gate = new Ttl74533();
    final var state = enabled(gate, false);
    present(gate, state, 0x00);
    assertWord(state, 0xFF);

    set(state, Ttl74533.D3, Value.UNKNOWN);
    gate.propagate(state);
    assertBit(state, 3, Value.UNKNOWN);
    assertBit(state, 0, Value.TRUE);
    assertBit(state, 7, Value.TRUE);

    set(state, Ttl74533.LE, Value.FALSE);
    set(state, Ttl74533.D3, Value.TRUE);
    gate.propagate(state);
    assertBit(state, 3, Value.UNKNOWN);

    set(state, Ttl74533.LE, Value.TRUE);
    set(state, Ttl74533.D3, Value.ERROR);
    gate.propagate(state);
    assertBit(state, 3, Value.ERROR);
    assertBit(state, 2, Value.TRUE);
  }

  @Test
  void unknownOutputEnableHidesTheWordAndErrorMakesItAnError() {
    final var gate = new Ttl74533();
    final var state = enabled(gate, false);
    present(gate, state, 0x11);
    set(state, Ttl74533.LE, Value.FALSE);
    gate.propagate(state);

    set(state, Ttl74533.OE, Value.UNKNOWN);
    gate.propagate(state);
    assertReleased(state);

    set(state, Ttl74533.OE, Value.FALSE);
    gate.propagate(state);
    assertWord(state, (~0x11) & 0xFF);

    set(state, Ttl74533.OE, Value.ERROR);
    gate.propagate(state);
    for (final var pin : OUTPUTS) {
      assertEquals(Value.ERROR, state.getPortValue(Ttl74533.pinNrToPortNr(pin)));
    }

    set(state, Ttl74533.OE, Value.FALSE);
    gate.propagate(state);
    assertWord(state, (~0x11) & 0xFF);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74533();
    final var state = enabled(gate, true);
    present(gate, state, 0x00);
    assertWord(state, 0xFF);
    set(state, Ttl74533.LE, Value.FALSE);
    gate.propagate(state);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertReleased(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    present(gate, state, 0xFF);
    assertWord(state, 0xFF);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertReleased(state);
  }

  private static void assertPin(Instance instance, byte pin, int x, int y, int type) {
    assertPort(instance, Ttl74533.pinNrToPortNr(pin), x, y, type);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static TtlTestInstanceState enabled(Ttl74533 gate, boolean showPowerPins) {
    final var state = new TtlTestInstanceState(gate, showPowerPins);
    if (showPowerPins) {
      state.setPortValue(GND_PORT, Value.FALSE);
      state.setPortValue(VCC_PORT, Value.TRUE);
    }
    set(state, Ttl74533.OE, Value.FALSE);
    set(state, Ttl74533.LE, Value.TRUE);
    setData(state, 0x00);
    gate.propagate(state);
    return state;
  }

  private static void present(Ttl74533 gate, TtlTestInstanceState state, int data) {
    setData(state, data);
    gate.propagate(state);
  }

  private static void set(TtlTestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl74533.pinNrToPortNr(pin), value);
  }

  private static void setData(TtlTestInstanceState state, int data) {
    for (var bit = 0; bit < INPUTS.length; bit++) {
      final var value = ((data >> bit) & 1) == 0 ? Value.FALSE : Value.TRUE;
      set(state, INPUTS[bit], value);
    }
  }

  private static void assertWord(TtlTestInstanceState state, int word) {
    for (var bit = 0; bit < OUTPUTS.length; bit++) {
      final var expected = ((word >> bit) & 1) == 0 ? Value.FALSE : Value.TRUE;
      assertBit(state, bit, expected);
    }
  }

  private static void assertBit(TtlTestInstanceState state, int bit, Value expected) {
    assertEquals(expected, state.getPortValue(Ttl74533.pinNrToPortNr(OUTPUTS[bit])), "Q" + bit);
  }

  private static void assertReleased(TtlTestInstanceState state) {
    for (var bit = 0; bit < OUTPUTS.length; bit++) {
      assertBit(state, bit, Value.UNKNOWN);
    }
  }
}
