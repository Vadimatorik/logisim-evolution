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

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Digital function table of the 74HC7014 hex non-inverting buffer. */
class Ttl747014Test {
  /** Datasheet inputs 1A, 2A, 3A, 4A, 5A, 6A in logical-port order. */
  private static final int[] INPUTS = {0, 2, 4, 7, 9, 11};

  /** Datasheet outputs 1Y, 2Y, 3Y, 4Y, 5Y, 6Y in logical-port order. */
  private static final int[] OUTPUTS = {1, 3, 5, 6, 8, 10};

  private static final int GND_PORT = 12;
  private static final int VCC_PORT = 13;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var buffer = new Ttl747014();
    final var hiddenPower = createInstance(buffer, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, 0, 10, 30, EndData.INPUT_ONLY); // pin 1: 1A
    assertPort(hiddenPower, 1, 30, 30, EndData.OUTPUT_ONLY); // pin 2: 1Y
    assertPort(hiddenPower, 2, 50, 30, EndData.INPUT_ONLY); // pin 3: 2A
    assertPort(hiddenPower, 3, 70, 30, EndData.OUTPUT_ONLY); // pin 4: 2Y
    assertPort(hiddenPower, 4, 90, 30, EndData.INPUT_ONLY); // pin 5: 3A
    assertPort(hiddenPower, 5, 110, 30, EndData.OUTPUT_ONLY); // pin 6: 3Y
    assertPort(hiddenPower, 6, 130, -30, EndData.OUTPUT_ONLY); // pin 8: 4Y
    assertPort(hiddenPower, 7, 110, -30, EndData.INPUT_ONLY); // pin 9: 4A
    assertPort(hiddenPower, 8, 90, -30, EndData.OUTPUT_ONLY); // pin 10: 5Y
    assertPort(hiddenPower, 9, 70, -30, EndData.INPUT_ONLY); // pin 11: 5A
    assertPort(hiddenPower, 10, 50, -30, EndData.OUTPUT_ONLY); // pin 12: 6Y
    assertPort(hiddenPower, 11, 30, -30, EndData.INPUT_ONLY); // pin 13: 6A

    final var shownPower = createInstance(buffer, true);
    assertEquals(14, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 130, 30, EndData.INPUT_ONLY); // pin 7: GND
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY); // pin 14: VCC
  }

  @Test
  void eachOutputCopiesItsInput() {
    final var buffer = new Ttl747014();
    final var levels = new Value[] {Value.FALSE, Value.TRUE, Value.UNKNOWN, Value.ERROR};

    for (final var level : levels) {
      final var inputs = new Value[INPUTS.length];
      for (var channel = 0; channel < inputs.length; channel++) {
        inputs[channel] = level;
      }
      final var state = new TtlTestInstanceState(buffer, false);
      drive(buffer, state, inputs);
      assertOutputs(state, inputs);
    }
  }

  @Test
  void channelsStayIndependentForWalkingOnesAndZeros() {
    final var buffer = new Ttl747014();

    for (var hot = 0; hot < INPUTS.length; hot++) {
      final var walkingOne = filled(Value.FALSE);
      walkingOne[hot] = Value.TRUE;
      final var oneState = new TtlTestInstanceState(buffer, false);
      drive(buffer, oneState, walkingOne);
      assertOutputs(oneState, walkingOne);

      final var walkingZero = filled(Value.TRUE);
      walkingZero[hot] = Value.FALSE;
      final var zeroState = new TtlTestInstanceState(buffer, false);
      drive(buffer, zeroState, walkingZero);
      assertOutputs(zeroState, walkingZero);
    }
  }

  @Test
  void oneChannelCanBeUnknownOrErrorWhileTheOthersStayKnown() {
    final var buffer = new Ttl747014();

    for (var hot = 0; hot < INPUTS.length; hot++) {
      final var unknown = filled(Value.FALSE);
      unknown[hot] = Value.UNKNOWN;
      final var unknownState = new TtlTestInstanceState(buffer, false);
      drive(buffer, unknownState, unknown);
      assertOutputs(unknownState, unknown);

      final var error = filled(Value.TRUE);
      error[hot] = Value.ERROR;
      final var errorState = new TtlTestInstanceState(buffer, false);
      drive(buffer, errorState, error);
      assertOutputs(errorState, error);
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var buffer = new Ttl747014();
    final var state = new TtlTestInstanceState(buffer, true);
    final var inputs = new Value[] {
      Value.TRUE, Value.FALSE, Value.TRUE, Value.FALSE, Value.TRUE, Value.FALSE
    };
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    drive(buffer, state, inputs);
    assertOutputs(state, inputs);

    state.setPortValue(VCC_PORT, Value.FALSE);
    buffer.propagate(state);
    assertOutputs(state, filled(Value.UNKNOWN));

    state.setPortValue(VCC_PORT, Value.TRUE);
    buffer.propagate(state);
    assertOutputs(state, inputs);

    state.setPortValue(GND_PORT, Value.TRUE);
    buffer.propagate(state);
    assertOutputs(state, filled(Value.UNKNOWN));

    state.setPortValue(GND_PORT, Value.FALSE);
    buffer.propagate(state);
    assertOutputs(state, inputs);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void drive(Ttl747014 buffer, TtlTestInstanceState state, Value[] inputs) {
    for (var channel = 0; channel < INPUTS.length; channel++) {
      state.setPortValue(INPUTS[channel], inputs[channel]);
    }
    buffer.propagate(state);
  }

  private static void assertOutputs(TtlTestInstanceState state, Value[] expected) {
    for (var channel = 0; channel < OUTPUTS.length; channel++) {
      assertEquals(expected[channel], state.getPortValue(OUTPUTS[channel]));
    }
  }

  private static Value[] filled(Value level) {
    final var values = new Value[INPUTS.length];
    for (var channel = 0; channel < values.length; channel++) {
      values[channel] = level;
    }
    return values;
  }
}
