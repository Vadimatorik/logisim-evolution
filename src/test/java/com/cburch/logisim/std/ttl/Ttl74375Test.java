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

class Ttl74375Test {
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  private static final byte[] DATA = {Ttl74375.D1, Ttl74375.D2, Ttl74375.D3, Ttl74375.D4};
  private static final byte[] OUTPUTS = {Ttl74375.Q1, Ttl74375.Q2, Ttl74375.Q3, Ttl74375.Q4};
  private static final byte[] COMPLEMENTS = {
    Ttl74375.NQ1, Ttl74375.NQ2, Ttl74375.NQ3, Ttl74375.NQ4
  };
  private static final byte[] ENABLES = {
    Ttl74375.G12, Ttl74375.G12, Ttl74375.G34, Ttl74375.G34
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var latch = new Ttl74375();
    final var hiddenPower = createInstance(latch, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74375.D1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74375.NQ1, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74375.Q1, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74375.G12, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74375.Q2, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74375.NQ2, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74375.D2, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74375.D3, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74375.NQ3, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74375.Q3, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74375.G34, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74375.Q4, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74375.NQ4, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74375.D4, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(latch, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(GND_PORT).getType());
    assertEquals(EndData.INPUT_ONLY, shownPower.getPorts().get(VCC_PORT).getType());
  }

  @Test
  void eachLatchFollowsItsDataInputWhileThePairEnableIsHigh() {
    final var latch = new Ttl74375();
    for (var index = 0; index < DATA.length; index++) {
      for (final var data : new Value[] {Value.FALSE, Value.TRUE}) {
        final var state = holding(latch);
        set(state, ENABLES[index], Value.TRUE);
        set(state, DATA[index], data);
        latch.propagate(state);

        assertEquals(data, output(state, OUTPUTS[index]));
        assertEquals(Ttl74375.complement(data), output(state, COMPLEMENTS[index]));
      }
    }
  }

  @Test
  void lowEnableHoldsTheCapturedValue() {
    final var latch = new Ttl74375();
    for (var index = 0; index < DATA.length; index++) {
      final var state = holding(latch);
      set(state, ENABLES[index], Value.TRUE);
      set(state, DATA[index], Value.TRUE);
      latch.propagate(state);

      set(state, ENABLES[index], Value.FALSE);
      set(state, DATA[index], Value.FALSE);
      latch.propagate(state);

      assertEquals(Value.TRUE, output(state, OUTPUTS[index]));
      assertEquals(Value.FALSE, output(state, COMPLEMENTS[index]));
    }
  }

  @Test
  void theTwoPairsAreIndependent() {
    final var latch = new Ttl74375();
    final var state = holding(latch);
    set(state, Ttl74375.G12, Value.TRUE);
    set(state, Ttl74375.G34, Value.TRUE);
    set(state, Ttl74375.D1, Value.TRUE);
    set(state, Ttl74375.D2, Value.TRUE);
    set(state, Ttl74375.D3, Value.FALSE);
    set(state, Ttl74375.D4, Value.FALSE);
    latch.propagate(state);

    set(state, Ttl74375.G12, Value.FALSE);
    set(state, Ttl74375.D1, Value.FALSE);
    set(state, Ttl74375.D2, Value.FALSE);
    set(state, Ttl74375.D3, Value.TRUE);
    set(state, Ttl74375.D4, Value.TRUE);
    latch.propagate(state);

    assertEquals(Value.TRUE, output(state, Ttl74375.Q1));
    assertEquals(Value.FALSE, output(state, Ttl74375.NQ1));
    assertEquals(Value.TRUE, output(state, Ttl74375.Q2));
    assertEquals(Value.FALSE, output(state, Ttl74375.NQ2));
    assertEquals(Value.TRUE, output(state, Ttl74375.Q3));
    assertEquals(Value.FALSE, output(state, Ttl74375.NQ3));
    assertEquals(Value.TRUE, output(state, Ttl74375.Q4));
    assertEquals(Value.FALSE, output(state, Ttl74375.NQ4));
  }

  @Test
  void highEnableCopiesUnknownAndErrorData() {
    final var latch = new Ttl74375();
    final var state = holding(latch);
    set(state, Ttl74375.G12, Value.TRUE);
    set(state, Ttl74375.D1, Value.UNKNOWN);
    latch.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl74375.Q1));
    assertEquals(Value.UNKNOWN, output(state, Ttl74375.NQ1));

    set(state, Ttl74375.D1, Value.ERROR);
    latch.propagate(state);
    assertEquals(Value.ERROR, output(state, Ttl74375.Q1));
    assertEquals(Value.ERROR, output(state, Ttl74375.NQ1));
  }

  @Test
  void anUndefinedEnableHoldsOnlyAMatchingKnownBit() {
    final var latch = new Ttl74375();
    final var state = holding(latch);
    set(state, Ttl74375.G12, Value.TRUE);
    set(state, Ttl74375.D1, Value.TRUE);
    latch.propagate(state);

    set(state, Ttl74375.G12, Value.UNKNOWN);
    set(state, Ttl74375.D1, Value.TRUE);
    latch.propagate(state);
    assertEquals(Value.TRUE, output(state, Ttl74375.Q1));
    assertEquals(Value.FALSE, output(state, Ttl74375.NQ1));

    set(state, Ttl74375.D1, Value.FALSE);
    latch.propagate(state);
    assertEquals(Value.ERROR, output(state, Ttl74375.Q1));
    assertEquals(Value.ERROR, output(state, Ttl74375.NQ1));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var latch = new Ttl74375();
    final var state = new TtlTestInstanceState(latch, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    set(state, Ttl74375.G12, Value.TRUE);
    set(state, Ttl74375.D1, Value.TRUE);
    latch.propagate(state);
    assertEquals(Value.TRUE, output(state, Ttl74375.Q1));

    set(state, Ttl74375.G12, Value.FALSE);
    set(state, Ttl74375.D1, Value.FALSE);
    latch.propagate(state);
    assertEquals(Value.TRUE, output(state, Ttl74375.Q1));

    state.setPortValue(VCC_PORT, Value.FALSE);
    latch.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    latch.propagate(state);
    assertEquals(Value.TRUE, output(state, Ttl74375.Q1));
    assertEquals(Value.FALSE, output(state, Ttl74375.NQ1));

    state.setPortValue(GND_PORT, Value.TRUE);
    latch.propagate(state);
    assertUnknownOutputs(state);
  }

  /** Creates a device whose enables are low and whose data inputs are low. */
  private static TtlTestInstanceState holding(Ttl74375 latch) {
    final var state = new TtlTestInstanceState(latch, false);
    set(state, Ttl74375.G12, Value.FALSE);
    set(state, Ttl74375.G34, Value.FALSE);
    for (final var data : DATA) {
      set(state, data, Value.FALSE);
    }
    latch.propagate(state);
    return state;
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var output : OUTPUTS) {
      assertEquals(Value.UNKNOWN, output(state, output));
    }
    for (final var output : COMPLEMENTS) {
      assertEquals(Value.UNKNOWN, output(state, output));
    }
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74375.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void set(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl74375.pinNrToPortNr(dsPinNr), value);
  }

  private static Value output(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74375.pinNrToPortNr(dsPinNr));
  }
}
