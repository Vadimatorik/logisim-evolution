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

class Ttl74133Test {
  /** Data inputs in datasheet order, A through M. */
  private static final byte[] DATA_INPUTS = {
    Ttl74133.A, Ttl74133.B, Ttl74133.C, Ttl74133.D, Ttl74133.E, Ttl74133.F, Ttl74133.G,
    Ttl74133.H, Ttl74133.I, Ttl74133.J, Ttl74133.K, Ttl74133.L, Ttl74133.M
  };

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74133();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74133.A, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74133.B, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74133.C, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74133.D, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74133.E, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74133.F, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74133.G, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74133.Y, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74133.H, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74133.I, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74133.J, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74133.K, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74133.L, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74133.M, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void allHighInputsDriveTheOutputLow() {
    final var gate = new Ttl74133();
    final var state = allHigh(gate);
    gate.propagate(state);

    assertEquals(Value.FALSE, output(state));
  }

  @Test
  void anyLowInputDrivesTheOutputHigh() {
    final var gate = new Ttl74133();

    for (final var input : DATA_INPUTS) {
      final var state = allHigh(gate);
      set(state, input, Value.FALSE);
      gate.propagate(state);

      assertEquals(Value.TRUE, output(state));
    }
  }

  @Test
  void allLowInputsDriveTheOutputHigh() {
    final var gate = new Ttl74133();
    final var state = allHigh(gate);
    for (final var input : DATA_INPUTS) {
      set(state, input, Value.FALSE);
    }
    gate.propagate(state);

    assertEquals(Value.TRUE, output(state));
  }

  @Test
  void unknownAndErrorInputsPropagateUnlessALowInputDominates() {
    final var gate = new Ttl74133();

    final var unknown = allHigh(gate);
    set(unknown, Ttl74133.C, Value.UNKNOWN);
    gate.propagate(unknown);
    assertEquals(Value.UNKNOWN, output(unknown));

    final var error = allHigh(gate);
    set(error, Ttl74133.H, Value.ERROR);
    gate.propagate(error);
    assertEquals(Value.ERROR, output(error));

    final var errorAndUnknown = allHigh(gate);
    set(errorAndUnknown, Ttl74133.A, Value.ERROR);
    set(errorAndUnknown, Ttl74133.M, Value.UNKNOWN);
    gate.propagate(errorAndUnknown);
    assertEquals(Value.ERROR, output(errorAndUnknown));

    final var lowAndUnknown = allHigh(gate);
    set(lowAndUnknown, Ttl74133.B, Value.FALSE);
    set(lowAndUnknown, Ttl74133.L, Value.UNKNOWN);
    gate.propagate(lowAndUnknown);
    assertEquals(Value.TRUE, output(lowAndUnknown));
  }

  @Test
  void invalidExposedPowerInputsMakeTheOutputUnknown() {
    final var gate = new Ttl74133();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    for (final var input : DATA_INPUTS) {
      set(state, input, Value.TRUE);
    }
    gate.propagate(state);
    assertEquals(Value.FALSE, output(state));

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, output(state));

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.FALSE, output(state));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, output(state));
  }

  /** Creates a gate whose data inputs are all high. */
  private static TtlTestInstanceState allHigh(Ttl74133 gate) {
    final var state = new TtlTestInstanceState(gate, false);
    for (final var input : DATA_INPUTS) {
      set(state, input, Value.TRUE);
    }
    return state;
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74133.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void set(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl74133.pinNrToPortNr(dsPinNr), value);
  }

  private static Value output(TtlTestInstanceState state) {
    return state.getPortValue(Ttl74133.pinNrToPortNr(Ttl74133.Y));
  }
}
