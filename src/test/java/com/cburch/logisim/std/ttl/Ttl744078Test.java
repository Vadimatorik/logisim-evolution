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

class Ttl744078Test {
  /** Data inputs in bit order, so bit 0 is {@code A} and bit 7 is {@code H}. */
  private static final byte[] INPUTS = {
    Ttl744078.A, Ttl744078.B, Ttl744078.C, Ttl744078.D,
    Ttl744078.E, Ttl744078.F, Ttl744078.G, Ttl744078.H
  };

  private static final int GND_PORT = 10;
  private static final int VCC_PORT = 11;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl744078();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(10, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744078.Y, 10, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744078.A, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744078.B, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744078.C, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744078.D, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744078.E, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744078.F, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744078.G, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744078.H, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744078.X, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(12, shownPower.getPorts().size());
    assertEquals(Location.create(130, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void everyInputCombinationDrivesTheOrAndItsComplement() {
    final var gate = new Ttl744078();

    for (var code = 0; code < 256; code++) {
      final var state = new TtlTestInstanceState(gate, false);
      for (var bit = 0; bit < INPUTS.length; bit++) {
        state.setPortValue(
            Ttl744078.pinNrToPortNr(INPUTS[bit]),
            ((code & (1 << bit)) == 0) ? Value.FALSE : Value.TRUE);
      }
      gate.propagate(state);

      final var anyHigh = code != 0;
      assertEquals(anyHigh ? Value.TRUE : Value.FALSE, output(state, Ttl744078.Y));
      assertEquals(anyHigh ? Value.FALSE : Value.TRUE, output(state, Ttl744078.X));
    }
  }

  @Test
  void oneHighInputForcesBothOutputsDespiteUnknowns() {
    final var gate = new Ttl744078();
    final var state = new TtlTestInstanceState(gate, false);
    state.setPortValue(Ttl744078.pinNrToPortNr(Ttl744078.A), Value.TRUE);
    state.setPortValue(Ttl744078.pinNrToPortNr(Ttl744078.H), Value.UNKNOWN);
    gate.propagate(state);

    assertEquals(Value.TRUE, output(state, Ttl744078.Y));
    assertEquals(Value.FALSE, output(state, Ttl744078.X));
  }

  @Test
  void anUnresolvedInputMakesBothOutputsAnError() {
    final var gate = new Ttl744078();
    final var state = new TtlTestInstanceState(gate, false);
    for (final var input : INPUTS) {
      state.setPortValue(Ttl744078.pinNrToPortNr(input), Value.FALSE);
    }
    state.setPortValue(Ttl744078.pinNrToPortNr(Ttl744078.C), Value.UNKNOWN);
    gate.propagate(state);

    assertEquals(Value.ERROR, output(state, Ttl744078.Y));
    assertEquals(Value.ERROR, output(state, Ttl744078.X));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl744078();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    for (final var input : INPUTS) {
      state.setPortValue(Ttl744078.pinNrToPortNr(input), Value.FALSE);
    }
    gate.propagate(state);
    assertEquals(Value.FALSE, output(state, Ttl744078.Y));
    assertEquals(Value.TRUE, output(state, Ttl744078.X));

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl744078.Y));
    assertEquals(Value.UNKNOWN, output(state, Ttl744078.X));

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.FALSE, output(state, Ttl744078.Y));
    assertEquals(Value.TRUE, output(state, Ttl744078.X));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl744078.Y));
    assertEquals(Value.UNKNOWN, output(state, Ttl744078.X));
  }

  private static Value output(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl744078.pinNrToPortNr(dsPinNr));
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl744078.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
