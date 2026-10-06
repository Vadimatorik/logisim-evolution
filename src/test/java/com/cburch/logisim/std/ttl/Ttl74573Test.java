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

/** Functional tests for the 74HC573 octal transparent latch. */
class Ttl74573Test {
  private static final int GND_PORT = 18;
  private static final int VCC_PORT = 19;
  private static final int[] OUTPUTS = {
    Ttl74573.PORT_INDEX_Q0,
    Ttl74573.PORT_INDEX_Q1,
    Ttl74573.PORT_INDEX_Q2,
    Ttl74573.PORT_INDEX_Q3,
    Ttl74573.PORT_INDEX_Q4,
    Ttl74573.PORT_INDEX_Q5,
    Ttl74573.PORT_INDEX_Q6,
    Ttl74573.PORT_INDEX_Q7
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74573();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(18, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74573.PORT_INDEX_nOE, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74573.PORT_INDEX_D0, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74573.PORT_INDEX_D1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74573.PORT_INDEX_D2, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74573.PORT_INDEX_D3, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74573.PORT_INDEX_D4, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74573.PORT_INDEX_D5, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74573.PORT_INDEX_D6, 150, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74573.PORT_INDEX_D7, 170, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74573.PORT_INDEX_LE, 190, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74573.PORT_INDEX_Q7, 170, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74573.PORT_INDEX_Q6, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74573.PORT_INDEX_Q5, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74573.PORT_INDEX_Q4, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74573.PORT_INDEX_Q3, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74573.PORT_INDEX_Q2, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74573.PORT_INDEX_Q1, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74573.PORT_INDEX_Q0, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(20, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 190, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void latchEnableIsNotAClock() {
    assertFalse(new Ttl74573().checkForGatedClocks(null));
  }

  @Test
  void transparentModeFollowsTheDataInputs() {
    final var gate = new Ttl74573();
    final var state = new TtlTestInstanceState(gate, false);
    final var patterns = new int[] {0x00, 0xFF, 0x55, 0xAA, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80};

    for (final var pattern : patterns) {
      present(state, pattern, true, true);
      gate.propagate(state);
      assertOutputs(state, pattern);
    }
  }

  @Test
  void fallingLatchEnableHoldsTheWord() {
    final var gate = new Ttl74573();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0x55, true, true);
    gate.propagate(state);
    assertOutputs(state, 0x55);

    present(state, 0x55, false, true);
    gate.propagate(state);
    present(state, 0x00, false, true);
    gate.propagate(state);
    present(state, 0xFF, false, true);
    gate.propagate(state);
    assertOutputs(state, 0x55);
  }

  @Test
  void unknownLatchEnableHoldsTheWord() {
    final var gate = new Ttl74573();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0x3C, true, true);
    gate.propagate(state);
    state.setPortValue(Ttl74573.PORT_INDEX_LE, Value.UNKNOWN);
    state.setPortValue(Ttl74573.PORT_INDEX_D0, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, 0x3C);
  }

  @Test
  void outputEnableHidesTheLatchWithoutChangingIt() {
    final var gate = new Ttl74573();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0x0F, true, true);
    gate.propagate(state);
    assertOutputs(state, 0x0F);

    present(state, 0x0F, true, false);
    gate.propagate(state);
    assertOutputsUnknown(state);

    present(state, 0x3C, true, false);
    gate.propagate(state);
    assertOutputsUnknown(state);

    present(state, 0x3C, false, false);
    gate.propagate(state);
    assertOutputsUnknown(state);

    present(state, 0x3C, false, true);
    gate.propagate(state);
    assertOutputs(state, 0x3C);
  }

  @Test
  void unknownOrErrorOutputEnableReleasesTheOutputs() {
    final var gate = new Ttl74573();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0xA5, true, true);
    gate.propagate(state);

    state.setPortValue(Ttl74573.PORT_INDEX_nOE, Value.UNKNOWN);
    gate.propagate(state);
    assertOutputsUnknown(state);

    state.setPortValue(Ttl74573.PORT_INDEX_nOE, Value.ERROR);
    gate.propagate(state);
    assertOutputsUnknown(state);

    state.setPortValue(Ttl74573.PORT_INDEX_nOE, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, 0xA5);
  }

  @Test
  void unknownDataBitIsStored() {
    final var gate = new Ttl74573();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0x00, true, true);
    state.setPortValue(Ttl74573.PORT_INDEX_D4, Value.UNKNOWN);
    gate.propagate(state);

    for (var bit = 0; bit < OUTPUTS.length; bit++) {
      final var expected = bit == 4 ? Value.UNKNOWN : Value.FALSE;
      assertEquals(expected, state.getPortValue(OUTPUTS[bit]), "Q" + bit);
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74573();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    present(state, 0x55, true, true);
    gate.propagate(state);
    present(state, 0xAA, false, true);
    gate.propagate(state);
    assertOutputs(state, 0x55);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertOutputsUnknown(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, 0x55);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertOutputsUnknown(state);

    state.setPortValue(GND_PORT, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, 0x55);
  }

  private static void present(
      TtlTestInstanceState state, int data, boolean latchEnabled, boolean outputsEnabled) {
    state.setPortValue(Ttl74573.PORT_INDEX_LE, latchEnabled ? Value.TRUE : Value.FALSE);
    state.setPortValue(Ttl74573.PORT_INDEX_nOE, outputsEnabled ? Value.FALSE : Value.TRUE);
    for (var bit = 0; bit < 8; bit++) {
      state.setPortValue(
          Ttl74573.PORT_INDEX_D0 + bit, ((data >> bit) & 1) == 1 ? Value.TRUE : Value.FALSE);
    }
  }

  private static void assertOutputs(TtlTestInstanceState state, int expected) {
    for (var bit = 0; bit < OUTPUTS.length; bit++) {
      final var expectedBit = ((expected >> bit) & 1) == 1 ? Value.TRUE : Value.FALSE;
      assertEquals(expectedBit, state.getPortValue(OUTPUTS[bit]), "Q" + bit);
    }
  }

  private static void assertOutputsUnknown(TtlTestInstanceState state) {
    for (final var output : OUTPUTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(output));
    }
  }

  private static void assertPort(Instance instance, int port, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
