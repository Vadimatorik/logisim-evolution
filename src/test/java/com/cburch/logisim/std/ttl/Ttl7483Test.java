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

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC83 4-bit binary full adder. */
class Ttl7483Test {
  private static final byte[] A_BITS = {Ttl7483.A1, Ttl7483.A2, Ttl7483.A3, Ttl7483.A4};
  private static final byte[] B_BITS = {Ttl7483.B1, Ttl7483.B2, Ttl7483.B3, Ttl7483.B4};
  private static final byte[] SUM_BITS = {Ttl7483.S1, Ttl7483.S2, Ttl7483.S3, Ttl7483.S4};

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var adder = new Ttl7483();
    final var hiddenPower = createInstance(adder, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertSignalPorts(hiddenPower);

    final var shownPower = createInstance(adder, true);
    assertEquals(16, shownPower.getPorts().size());
    assertSignalPorts(shownPower);
    assertPort(shownPower, GND_PORT, 90, -30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 90, 30, EndData.INPUT_ONLY);
  }

  @Test
  void everyInputCombinationProducesTheBinarySum() {
    final var adder = new Ttl7483();

    for (var a = 0; a < 16; a++) {
      for (var b = 0; b < 16; b++) {
        for (var carryIn = 0; carryIn < 2; carryIn++) {
          final var state = new TtlTestInstanceState(adder, false);
          drive(state, a, b, carryIn == 1);
          adder.propagate(state);
          assertEquals(a + b + carryIn, sumValue(state));
        }
      }
    }
  }

  @Test
  void unknownOrErrorInputCorruptsOnlyThatBitAndHigherResults() {
    final var adder = new Ttl7483();

    final var unknownHighBit = driven(adder, 0b1111, 0b0001, false);
    unknownHighBit.setPortValue(Ttl7483.pinToPort(Ttl7483.A4), Value.UNKNOWN);
    adder.propagate(unknownHighBit);
    assertEquals(Value.FALSE, output(unknownHighBit, Ttl7483.S1));
    assertEquals(Value.FALSE, output(unknownHighBit, Ttl7483.S2));
    assertEquals(Value.FALSE, output(unknownHighBit, Ttl7483.S3));
    assertEquals(Value.UNKNOWN, output(unknownHighBit, Ttl7483.S4));
    assertEquals(Value.UNKNOWN, output(unknownHighBit, Ttl7483.C4));

    final var unknownMiddleBit = driven(adder, 0b0001, 0b0001, false);
    unknownMiddleBit.setPortValue(Ttl7483.pinToPort(Ttl7483.A2), Value.UNKNOWN);
    adder.propagate(unknownMiddleBit);
    assertEquals(Value.FALSE, output(unknownMiddleBit, Ttl7483.S1));
    assertEquals(Value.UNKNOWN, output(unknownMiddleBit, Ttl7483.S2));
    assertEquals(Value.UNKNOWN, output(unknownMiddleBit, Ttl7483.S3));
    assertEquals(Value.UNKNOWN, output(unknownMiddleBit, Ttl7483.S4));
    assertEquals(Value.UNKNOWN, output(unknownMiddleBit, Ttl7483.C4));

    final var nilHighBit = driven(adder, 0b1111, 0b0001, false);
    nilHighBit.setPortValue(Ttl7483.pinToPort(Ttl7483.A4), Value.NIL);
    adder.propagate(nilHighBit);
    assertEquals(Value.FALSE, output(nilHighBit, Ttl7483.S1));
    assertEquals(Value.UNKNOWN, output(nilHighBit, Ttl7483.S4));
    assertEquals(Value.UNKNOWN, output(nilHighBit, Ttl7483.C4));

    final var errorHighBit = driven(adder, 0b1111, 0b0001, false);
    errorHighBit.setPortValue(Ttl7483.pinToPort(Ttl7483.B4), Value.ERROR);
    adder.propagate(errorHighBit);
    assertEquals(Value.FALSE, output(errorHighBit, Ttl7483.S1));
    assertEquals(Value.FALSE, output(errorHighBit, Ttl7483.S2));
    assertEquals(Value.FALSE, output(errorHighBit, Ttl7483.S3));
    assertEquals(Value.ERROR, output(errorHighBit, Ttl7483.S4));
    assertEquals(Value.ERROR, output(errorHighBit, Ttl7483.C4));

    final var errorLowBit = driven(adder, 0b0001, 0b0001, false);
    errorLowBit.setPortValue(Ttl7483.pinToPort(Ttl7483.B1), Value.ERROR);
    adder.propagate(errorLowBit);
    for (final var pin : SUM_BITS) {
      assertEquals(Value.ERROR, output(errorLowBit, pin));
    }
    assertEquals(Value.ERROR, output(errorLowBit, Ttl7483.C4));

    final var mixed = driven(adder, 0b0000, 0b0000, false);
    mixed.setPortValue(Ttl7483.pinToPort(Ttl7483.A1), Value.ERROR);
    mixed.setPortValue(Ttl7483.pinToPort(Ttl7483.B1), Value.UNKNOWN);
    adder.propagate(mixed);
    assertEquals(Value.ERROR, output(mixed, Ttl7483.S1));
    assertEquals(Value.ERROR, output(mixed, Ttl7483.C4));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var adder = new Ttl7483();
    final var state = new TtlTestInstanceState(adder, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    drive(state, 0b0101, 0b0011, false);
    adder.propagate(state);
    assertEquals(8, sumValue(state));

    state.setPortValue(VCC_PORT, Value.FALSE);
    adder.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    adder.propagate(state);
    assertEquals(8, sumValue(state));

    state.setPortValue(GND_PORT, Value.TRUE);
    adder.propagate(state);
    assertUnknownOutputs(state);
  }

  private static void assertSignalPorts(Instance instance) {
    assertPort(instance, Ttl7483.pinToPort(Ttl7483.A4), 10, 30, EndData.INPUT_ONLY);
    assertPort(instance, Ttl7483.pinToPort(Ttl7483.S3), 30, 30, EndData.OUTPUT_ONLY);
    assertPort(instance, Ttl7483.pinToPort(Ttl7483.A3), 50, 30, EndData.INPUT_ONLY);
    assertPort(instance, Ttl7483.pinToPort(Ttl7483.B3), 70, 30, EndData.INPUT_ONLY);
    assertPort(instance, Ttl7483.pinToPort(Ttl7483.S2), 110, 30, EndData.OUTPUT_ONLY);
    assertPort(instance, Ttl7483.pinToPort(Ttl7483.B2), 130, 30, EndData.INPUT_ONLY);
    assertPort(instance, Ttl7483.pinToPort(Ttl7483.A2), 150, 30, EndData.INPUT_ONLY);
    assertPort(instance, Ttl7483.pinToPort(Ttl7483.S1), 150, -30, EndData.OUTPUT_ONLY);
    assertPort(instance, Ttl7483.pinToPort(Ttl7483.A1), 130, -30, EndData.INPUT_ONLY);
    assertPort(instance, Ttl7483.pinToPort(Ttl7483.B1), 110, -30, EndData.INPUT_ONLY);
    assertPort(instance, Ttl7483.pinToPort(Ttl7483.C0), 70, -30, EndData.INPUT_ONLY);
    assertPort(instance, Ttl7483.pinToPort(Ttl7483.C4), 50, -30, EndData.OUTPUT_ONLY);
    assertPort(instance, Ttl7483.pinToPort(Ttl7483.S4), 30, -30, EndData.OUTPUT_ONLY);
    assertPort(instance, Ttl7483.pinToPort(Ttl7483.B4), 10, -30, EndData.INPUT_ONLY);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static TtlTestInstanceState driven(Ttl7483 adder, int a, int b, boolean carryIn) {
    final var state = new TtlTestInstanceState(adder, false);
    drive(state, a, b, carryIn);
    return state;
  }

  private static void drive(TtlTestInstanceState state, int a, int b, boolean carryIn) {
    setNibble(state, A_BITS, a);
    setNibble(state, B_BITS, b);
    state.setPortValue(Ttl7483.pinToPort(Ttl7483.C0), carryIn ? Value.TRUE : Value.FALSE);
  }

  private static void setNibble(TtlTestInstanceState state, byte[] pins, int value) {
    for (var bit = 0; bit < pins.length; bit++) {
      final var high = ((value >> bit) & 1) == 1;
      state.setPortValue(Ttl7483.pinToPort(pins[bit]), high ? Value.TRUE : Value.FALSE);
    }
  }

  private static Value output(TtlTestInstanceState state, byte pin) {
    return state.getPortValue(Ttl7483.pinToPort(pin));
  }

  private static int sumValue(TtlTestInstanceState state) {
    var sum = 0;
    for (var bit = 0; bit < SUM_BITS.length; bit++) {
      sum |= level(output(state, SUM_BITS[bit])) << bit;
    }
    sum |= level(output(state, Ttl7483.C4)) << SUM_BITS.length;
    return sum;
  }

  private static int level(Value value) {
    assertTrue(value == Value.TRUE || value == Value.FALSE);
    return value == Value.TRUE ? 1 : 0;
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var pin : SUM_BITS) {
      assertEquals(Value.UNKNOWN, output(state, pin));
    }
    assertEquals(Value.UNKNOWN, output(state, Ttl7483.C4));
  }
}
