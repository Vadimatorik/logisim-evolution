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

/** Functional tests for the 74HC563 octal inverting transparent latch. */
class Ttl74563Test {
  private static final int GND_PORT = 18;
  private static final int VCC_PORT = 19;
  private static final int[] OUTPUTS = {
    Ttl74563.PORT_INDEX_Q0,
    Ttl74563.PORT_INDEX_Q1,
    Ttl74563.PORT_INDEX_Q2,
    Ttl74563.PORT_INDEX_Q3,
    Ttl74563.PORT_INDEX_Q4,
    Ttl74563.PORT_INDEX_Q5,
    Ttl74563.PORT_INDEX_Q6,
    Ttl74563.PORT_INDEX_Q7
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74563();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(18, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74563.PORT_INDEX_nOE, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74563.PORT_INDEX_D0, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74563.PORT_INDEX_D1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74563.PORT_INDEX_D2, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74563.PORT_INDEX_D3, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74563.PORT_INDEX_D4, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74563.PORT_INDEX_D5, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74563.PORT_INDEX_D6, 150, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74563.PORT_INDEX_D7, 170, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74563.PORT_INDEX_LE, 190, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74563.PORT_INDEX_Q7, 170, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74563.PORT_INDEX_Q6, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74563.PORT_INDEX_Q5, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74563.PORT_INDEX_Q4, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74563.PORT_INDEX_Q3, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74563.PORT_INDEX_Q2, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74563.PORT_INDEX_Q1, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74563.PORT_INDEX_Q0, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(20, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 190, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void latchEnableIsNotAClock() {
    assertFalse(new Ttl74563().checkForGatedClocks(null));
  }

  @Test
  void transparentModeDrivesTheComplement() {
    final var gate = new Ttl74563();
    final var patterns = new int[] {0x00, 0xFF, 0x55, 0xAA, 0x01, 0x80, 0x0F, 0xF0};

    for (final var pattern : patterns) {
      final var state = new TtlTestInstanceState(gate, false);
      present(state, pattern, true, true);
      gate.propagate(state);
      assertOutputs(state, pattern ^ 0xFF);
    }
  }

  @Test
  void eachDataBitInvertsOntoItsOwnOutput() {
    final var gate = new Ttl74563();

    for (var bit = 0; bit < 8; bit++) {
      final var state = new TtlTestInstanceState(gate, false);
      present(state, 1 << bit, true, true);
      gate.propagate(state);
      assertOutputs(state, (~(1 << bit)) & 0xFF);
    }
  }

  @Test
  void fallingLatchEnableHoldsTheComplement() {
    final var gate = new Ttl74563();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0x55, true, true);
    gate.propagate(state);
    assertOutputs(state, 0xAA);

    present(state, 0x55, false, true);
    gate.propagate(state);
    present(state, 0x00, false, true);
    gate.propagate(state);
    present(state, 0xFF, false, true);
    gate.propagate(state);
    assertOutputs(state, 0xAA);
  }

  @Test
  void outputEnableHidesTheLatchWithoutChangingIt() {
    final var gate = new Ttl74563();
    final var state = new TtlTestInstanceState(gate, false);

    present(state, 0x0F, true, true);
    gate.propagate(state);
    assertOutputs(state, 0xF0);

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
    assertOutputs(state, 0xC3);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74563();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    present(state, 0x55, true, true);
    gate.propagate(state);
    assertOutputs(state, 0xAA);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertOutputsUnknown(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, 0xAA);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertOutputsUnknown(state);
  }

  private static void present(
      TtlTestInstanceState state, int data, boolean latchEnabled, boolean outputsEnabled) {
    state.setPortValue(Ttl74563.PORT_INDEX_LE, latchEnabled ? Value.TRUE : Value.FALSE);
    state.setPortValue(Ttl74563.PORT_INDEX_nOE, outputsEnabled ? Value.FALSE : Value.TRUE);
    for (var bit = 0; bit < 8; bit++) {
      state.setPortValue(
          Ttl74563.PORT_INDEX_D0 + bit, ((data >> bit) & 1) == 1 ? Value.TRUE : Value.FALSE);
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
