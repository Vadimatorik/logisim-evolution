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

/** Functional tests for the 74HC280 9-bit odd/even parity generator/checker. */
class Ttl74280Test {
  /** Data inputs in bit order, matching {@code Ttl74280} pin constants. */
  private static final byte[] DATA_INPUTS = {
    Ttl74280.I0, Ttl74280.I1, Ttl74280.I2, Ttl74280.I3, Ttl74280.I4,
    Ttl74280.I5, Ttl74280.I6, Ttl74280.I7, Ttl74280.I8
  };

  private static final int GND_PORT = 11;
  private static final int VCC_PORT = 12;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74280();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(11, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74280.I6, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74280.I7, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74280.I8, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74280.PE, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74280.PO, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74280.I0, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74280.I1, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74280.I2, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74280.I3, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74280.I4, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74280.I5, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(13, shownPower.getPorts().size());
    assertEquals(Location.create(130, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void everyInputCombinationProducesComplementaryParity() {
    final var gate = new Ttl74280();

    for (var mask = 0; mask < 512; mask++) {
      final var state = new TtlTestInstanceState(gate, false);
      applyMask(state, mask);
      gate.propagate(state);

      final var odd = (Integer.bitCount(mask) & 1) != 0;
      assertEquals(odd ? Value.FALSE : Value.TRUE, output(state, Ttl74280.PE), "PE mask " + mask);
      assertEquals(odd ? Value.TRUE : Value.FALSE, output(state, Ttl74280.PO), "PO mask " + mask);
    }
  }

  @Test
  void anUnknownInputMakesBothOutputsUnknown() {
    final var gate = new Ttl74280();
    final var state = new TtlTestInstanceState(gate, false);
    applyMask(state, 0b00001111);
    state.setPortValue(Ttl74280.pinNrToPortNr(Ttl74280.I8), Value.UNKNOWN);
    gate.propagate(state);

    assertEquals(Value.UNKNOWN, output(state, Ttl74280.PE));
    assertEquals(Value.UNKNOWN, output(state, Ttl74280.PO));
  }

  @Test
  void anErrorInputMakesBothOutputsAnError() {
    final var gate = new Ttl74280();
    final var state = new TtlTestInstanceState(gate, false);
    applyMask(state, 0);
    state.setPortValue(Ttl74280.pinNrToPortNr(Ttl74280.I0), Value.ERROR);
    state.setPortValue(Ttl74280.pinNrToPortNr(Ttl74280.I3), Value.UNKNOWN);
    gate.propagate(state);

    assertEquals(Value.ERROR, output(state, Ttl74280.PE));
    assertEquals(Value.ERROR, output(state, Ttl74280.PO));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74280();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    applyMask(state, 0);
    gate.propagate(state);
    assertEquals(Value.TRUE, output(state, Ttl74280.PE));
    assertEquals(Value.FALSE, output(state, Ttl74280.PO));

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl74280.PE));
    assertEquals(Value.UNKNOWN, output(state, Ttl74280.PO));

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.TRUE, output(state, Ttl74280.PE));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl74280.PE));
    assertEquals(Value.UNKNOWN, output(state, Ttl74280.PO));
  }

  private static void applyMask(TtlTestInstanceState state, int mask) {
    for (var bit = 0; bit < DATA_INPUTS.length; bit++) {
      state.setPortValue(
          Ttl74280.pinNrToPortNr(DATA_INPUTS[bit]),
          (mask & (1 << bit)) != 0 ? Value.TRUE : Value.FALSE);
    }
  }

  private static Value output(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74280.pinNrToPortNr(dsPinNr));
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74280.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
