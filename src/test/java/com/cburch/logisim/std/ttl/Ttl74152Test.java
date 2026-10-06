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

class Ttl74152Test {
  /** Data inputs ordered by the address that selects them. */
  private static final byte[] DATA = {
    Ttl74152.D0, Ttl74152.D1, Ttl74152.D2, Ttl74152.D3,
    Ttl74152.D4, Ttl74152.D5, Ttl74152.D6, Ttl74152.D7
  };

  private static final int GND_PORT = 12;
  private static final int VCC_PORT = 13;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var selector = new Ttl74152();
    final var hiddenPower = createInstance(selector, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74152.D4, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74152.D3, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74152.D2, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74152.D1, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74152.D0, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74152.W, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74152.C, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74152.B, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74152.A, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74152.D7, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74152.D6, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74152.D5, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(selector, true);
    assertEquals(14, shownPower.getPorts().size());
    assertEquals(Location.create(130, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void eachAddressCopiesItsDataInputAndIgnoresTheOthers() {
    final var selector = new Ttl74152();

    for (var address = 0; address < DATA.length; address++) {
      final var onlySelected = 1 << address;
      final var othersOnly = onlySelected ^ 0xFF;

      assertEquals(Value.TRUE, outputFor(selector, address, onlySelected));
      assertEquals(Value.FALSE, outputFor(selector, address, othersOnly));
    }
  }

  @Test
  void unknownAndErrorOnTheSelectedInputPassThrough() {
    final var selector = new Ttl74152();
    final var unknown = driven(selector, 3, 0);
    setValue(unknown, Ttl74152.D3, Value.UNKNOWN);
    selector.propagate(unknown);
    assertEquals(Value.UNKNOWN, output(unknown));

    final var error = driven(selector, 3, 0);
    setValue(error, Ttl74152.D3, Value.ERROR);
    selector.propagate(error);
    assertEquals(Value.ERROR, output(error));

    final var neighbor = driven(selector, 3, 0xFF);
    setValue(neighbor, Ttl74152.D4, Value.ERROR);
    selector.propagate(neighbor);
    assertEquals(Value.TRUE, output(neighbor));
  }

  @Test
  void unknownOrErrorOnASelectInputMakesTheOutputUnknownOrError() {
    final var selector = new Ttl74152();
    final var unknown = driven(selector, 0, 0xFF);
    setValue(unknown, Ttl74152.A, Value.UNKNOWN);
    selector.propagate(unknown);
    assertEquals(Value.UNKNOWN, output(unknown));

    final var error = driven(selector, 0, 0xFF);
    setValue(error, Ttl74152.C, Value.ERROR);
    selector.propagate(error);
    assertEquals(Value.ERROR, output(error));

    final var both = driven(selector, 0, 0xFF);
    setValue(both, Ttl74152.A, Value.UNKNOWN);
    setValue(both, Ttl74152.C, Value.ERROR);
    selector.propagate(both);
    assertEquals(Value.ERROR, output(both));
  }

  @Test
  void invalidExposedPowerInputsMakeTheOutputUnknown() {
    final var selector = new Ttl74152();
    final var state = new TtlTestInstanceState(selector, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    drive(state, 5, 0xFF);
    selector.propagate(state);
    assertEquals(Value.TRUE, output(state));

    state.setPortValue(VCC_PORT, Value.FALSE);
    selector.propagate(state);
    assertEquals(Value.UNKNOWN, output(state));

    state.setPortValue(VCC_PORT, Value.TRUE);
    selector.propagate(state);
    assertEquals(Value.TRUE, output(state));

    state.setPortValue(GND_PORT, Value.TRUE);
    selector.propagate(state);
    assertEquals(Value.UNKNOWN, output(state));
  }

  private static Value outputFor(Ttl74152 selector, int address, int data) {
    final var state = driven(selector, address, data);
    selector.propagate(state);
    return output(state);
  }

  /** Drives a known address and an 8-bit data word, bit 0 being D0. */
  private static TtlTestInstanceState driven(Ttl74152 selector, int address, int data) {
    final var state = new TtlTestInstanceState(selector, false);
    drive(state, address, data);
    return state;
  }

  private static void drive(TtlTestInstanceState state, int address, int data) {
    setLevel(state, Ttl74152.A, (address & 1) != 0);
    setLevel(state, Ttl74152.B, (address & 2) != 0);
    setLevel(state, Ttl74152.C, (address & 4) != 0);
    for (var index = 0; index < DATA.length; index++) {
      setLevel(state, DATA[index], ((data >> index) & 1) != 0);
    }
  }

  private static Value output(TtlTestInstanceState state) {
    return state.getPortValue(Ttl74152.pinNrToPortNr(Ttl74152.W));
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74152.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, boolean high) {
    setValue(state, dsPinNr, high ? Value.TRUE : Value.FALSE);
  }

  private static void setValue(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl74152.pinNrToPortNr(dsPinNr), value);
  }
}
