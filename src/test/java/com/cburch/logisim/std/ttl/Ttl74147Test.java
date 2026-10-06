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

class Ttl74147Test {
  /** Data inputs ordered by the decimal number they encode. Index 0 is input 1. */
  private static final byte[] DATA_INPUTS = {
    Ttl74147.I1, Ttl74147.I2, Ttl74147.I3, Ttl74147.I4, Ttl74147.I5,
    Ttl74147.I6, Ttl74147.I7, Ttl74147.I8, Ttl74147.I9
  };

  private static final byte[] CODE_OUTPUTS = {
    Ttl74147.Y3, Ttl74147.Y2, Ttl74147.Y1, Ttl74147.Y0
  };

  private static final int GND_PORT = 13;
  private static final int VCC_PORT = 14;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var encoder = new Ttl74147();
    final var hiddenPower = createInstance(encoder, false);

    assertEquals(13, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74147.I4, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74147.I5, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74147.I6, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74147.I7, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74147.I8, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74147.Y2, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74147.Y1, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74147.Y0, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74147.I9, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74147.I1, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74147.I2, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74147.I3, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74147.Y3, 50, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(encoder, true);
    assertEquals(15, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void eachDataInputEncodesItsDecimalNumber() {
    final var encoder = new Ttl74147();
    final var idle = allInactive(encoder);
    encoder.propagate(idle);
    assertEquals(0, encodedNumber(idle));

    for (var number = 1; number <= DATA_INPUTS.length; number++) {
      final var state = allInactive(encoder);
      setLow(state, DATA_INPUTS[number - 1]);
      encoder.propagate(state);
      assertEquals(number, encodedNumber(state));
    }
  }

  @Test
  void theHighestNumberedAssertedInputWins() {
    final var encoder = new Ttl74147();

    for (var expected = 1; expected <= DATA_INPUTS.length; expected++) {
      final var state = allInactive(encoder);
      for (var number = 1; number <= expected; number++) {
        setLow(state, DATA_INPUTS[number - 1]);
      }
      encoder.propagate(state);
      assertEquals(expected, encodedNumber(state));
    }
  }

  @Test
  void unknownInputsAreTreatedAsTheInactiveLevel() {
    final var encoder = new Ttl74147();
    final var floating = new TtlTestInstanceState(encoder, false);
    encoder.propagate(floating);
    assertEquals(0, encodedNumber(floating));

    final var state = allInactive(encoder);
    state.setPortValue(Ttl74147.pinNrToPortNr(Ttl74147.I9), Value.UNKNOWN);
    state.setPortValue(Ttl74147.pinNrToPortNr(Ttl74147.I8), Value.ERROR);
    setLow(state, Ttl74147.I4);
    encoder.propagate(state);
    assertEquals(4, encodedNumber(state));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var encoder = new Ttl74147();
    final var state = new TtlTestInstanceState(encoder, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    for (final var input : DATA_INPUTS) {
      setHigh(state, input);
    }
    setLow(state, Ttl74147.I3);
    encoder.propagate(state);
    assertEquals(3, encodedNumber(state));

    state.setPortValue(VCC_PORT, Value.FALSE);
    encoder.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    encoder.propagate(state);
    assertEquals(3, encodedNumber(state));

    state.setPortValue(GND_PORT, Value.TRUE);
    encoder.propagate(state);
    assertUnknownOutputs(state);
  }

  /** Creates a device whose data inputs are all held at the inactive level. */
  private static TtlTestInstanceState allInactive(Ttl74147 encoder) {
    final var state = new TtlTestInstanceState(encoder, false);
    for (final var input : DATA_INPUTS) {
      setHigh(state, input);
    }
    return state;
  }

  /** Converts the active-low BCD outputs back into the decimal number they encode. */
  private static int encodedNumber(TtlTestInstanceState state) {
    return (isLow(state, Ttl74147.Y3) ? 8 : 0)
        + (isLow(state, Ttl74147.Y2) ? 4 : 0)
        + (isLow(state, Ttl74147.Y1) ? 2 : 0)
        + (isLow(state, Ttl74147.Y0) ? 1 : 0);
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var output : CODE_OUTPUTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74147.pinNrToPortNr(output)));
    }
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74147.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLow(TtlTestInstanceState state, byte dsPinNr) {
    state.setPortValue(Ttl74147.pinNrToPortNr(dsPinNr), Value.FALSE);
  }

  private static void setHigh(TtlTestInstanceState state, byte dsPinNr) {
    state.setPortValue(Ttl74147.pinNrToPortNr(dsPinNr), Value.TRUE);
  }

  private static boolean isLow(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74147.pinNrToPortNr(dsPinNr)) == Value.FALSE;
  }
}
