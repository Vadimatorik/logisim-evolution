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

/** Functional tests for the 74x155 dual 2-line to 4-line decoder/demultiplexer. */
class Ttl74155Test {
  private static final byte[] SECTION1 = {
    Ttl74155.Y1_0, Ttl74155.Y1_1, Ttl74155.Y1_2, Ttl74155.Y1_3
  };
  private static final byte[] SECTION2 = {
    Ttl74155.Y2_0, Ttl74155.Y2_1, Ttl74155.Y2_2, Ttl74155.Y2_3
  };
  private static final byte[] RESULT_OUTPUTS = {
    Ttl74155.Y1_0, Ttl74155.Y1_1, Ttl74155.Y1_2, Ttl74155.Y1_3,
    Ttl74155.Y2_0, Ttl74155.Y2_1, Ttl74155.Y2_2, Ttl74155.Y2_3
  };

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var decoder = new Ttl74155();
    final var hiddenPower = createInstance(decoder, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74155.C1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74155.G1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74155.B, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74155.Y1_3, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74155.Y1_2, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74155.Y1_1, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74155.Y1_0, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74155.Y2_0, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74155.Y2_1, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74155.Y2_2, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74155.Y2_3, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74155.A, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74155.G2, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74155.C2, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(decoder, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void eachAddressSelectsOneActiveLowOutputPerSection() {
    final var decoder = new Ttl74155();

    for (var address = 0; address < 4; address++) {
      final var state = driven(decoder, address, false, true, true, false);
      decoder.propagate(state);

      assertSection(state, SECTION1, address);
      assertSection(state, SECTION2, address);
    }
  }

  @Test
  void eitherInactiveStrobeOrInactiveDataHoldsItsSectionHigh() {
    final var decoder = new Ttl74155();

    for (var address = 0; address < 4; address++) {
      assertSections(decoder, address, true, true, true, false, -1, address);
      assertSections(decoder, address, true, false, true, false, -1, address);
      assertSections(decoder, address, false, false, true, false, -1, address);
      assertSections(decoder, address, false, true, false, false, address, -1);
      assertSections(decoder, address, false, true, false, true, address, -1);
      assertSections(decoder, address, false, true, true, true, address, -1);
    }
  }

  @Test
  void theSectionsDecodeIndependently() {
    final var decoder = new Ttl74155();
    final var state = driven(decoder, 2, false, true, false, true);
    decoder.propagate(state);

    assertSection(state, SECTION1, 2);
    assertSection(state, SECTION2, -1);

    set(state, Ttl74155.G2, Value.TRUE);
    set(state, Ttl74155.C2, Value.FALSE);
    set(state, Ttl74155.G1, Value.TRUE);
    decoder.propagate(state);

    assertSection(state, SECTION1, -1);
    assertSection(state, SECTION2, 2);
  }

  @Test
  void tyingTheDataInputsMakesAThreeToEightDecoder() {
    final var decoder = new Ttl74155();

    for (var code = 0; code < 8; code++) {
      final var address = code & 3;
      final var select = (code & 4) != 0;
      final var state = driven(decoder, address, false, select, true, select);
      decoder.propagate(state);

      assertSection(state, SECTION2, select ? -1 : address);
      assertSection(state, SECTION1, select ? address : -1);
    }
  }

  @Test
  void anUnknownAddressLeavesOnlyTheCandidateOutputsUnknown() {
    final var decoder = new Ttl74155();
    final var state = driven(decoder, 0, false, true, false, true);
    set(state, Ttl74155.A, Value.UNKNOWN);
    decoder.propagate(state);

    assertEquals(Value.UNKNOWN, level(state, Ttl74155.Y1_0));
    assertEquals(Value.UNKNOWN, level(state, Ttl74155.Y1_1));
    assertEquals(Value.TRUE, level(state, Ttl74155.Y1_2));
    assertEquals(Value.TRUE, level(state, Ttl74155.Y1_3));
    assertSection(state, SECTION2, -1);
  }

  @Test
  void anInactiveStrobeKeepsTheSectionHighWhenTheAddressIsUnknown() {
    final var decoder = new Ttl74155();
    final var state = new TtlTestInstanceState(decoder, false);
    set(state, Ttl74155.G1, Value.TRUE);
    set(state, Ttl74155.C1, Value.UNKNOWN);
    set(state, Ttl74155.A, Value.UNKNOWN);
    set(state, Ttl74155.B, Value.UNKNOWN);
    set(state, Ttl74155.G2, Value.FALSE);
    set(state, Ttl74155.C2, Value.UNKNOWN);
    decoder.propagate(state);

    assertSection(state, SECTION1, -1);
    assertSection(state, SECTION2, -1);
  }

  @Test
  void anErrorAddressMakesTheCandidateOutputsAnError() {
    final var decoder = new Ttl74155();
    final var state = driven(decoder, 0, false, true, false, true);
    set(state, Ttl74155.B, Value.ERROR);
    decoder.propagate(state);

    assertEquals(Value.ERROR, level(state, Ttl74155.Y1_0));
    assertEquals(Value.TRUE, level(state, Ttl74155.Y1_1));
    assertEquals(Value.ERROR, level(state, Ttl74155.Y1_2));
    assertEquals(Value.TRUE, level(state, Ttl74155.Y1_3));
    assertSection(state, SECTION2, -1);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var decoder = new Ttl74155();
    final var state = new TtlTestInstanceState(decoder, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    drive(state, 1, false, true, true, false);
    decoder.propagate(state);
    assertSection(state, SECTION1, 1);
    assertSection(state, SECTION2, 1);

    state.setPortValue(VCC_PORT, Value.FALSE);
    decoder.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    decoder.propagate(state);
    assertSection(state, SECTION1, 1);
    assertSection(state, SECTION2, 1);

    state.setPortValue(GND_PORT, Value.TRUE);
    decoder.propagate(state);
    assertUnknownOutputs(state);
  }

  private static void assertSections(
      Ttl74155 decoder,
      int address,
      boolean g1,
      boolean c1,
      boolean g2,
      boolean c2,
      int section1,
      int section2) {
    final var state = driven(decoder, address, g1, c1, g2, c2);
    decoder.propagate(state);
    assertSection(state, SECTION1, section1);
    assertSection(state, SECTION2, section2);
  }

  /** Checks one section. {@code selected} is the low output, or -1 when every output stays high. */
  private static void assertSection(TtlTestInstanceState state, byte[] outputs, int selected) {
    for (var index = 0; index < outputs.length; index++) {
      assertEquals(index == selected ? Value.FALSE : Value.TRUE, level(state, outputs[index]));
    }
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var output : RESULT_OUTPUTS) {
      assertEquals(Value.UNKNOWN, level(state, output));
    }
  }

  private static TtlTestInstanceState driven(
      Ttl74155 decoder, int address, boolean g1, boolean c1, boolean g2, boolean c2) {
    final var state = new TtlTestInstanceState(decoder, false);
    drive(state, address, g1, c1, g2, c2);
    return state;
  }

  private static void drive(
      TtlTestInstanceState state, int address, boolean g1, boolean c1, boolean g2, boolean c2) {
    set(state, Ttl74155.A, (address & 1) != 0 ? Value.TRUE : Value.FALSE);
    set(state, Ttl74155.B, (address & 2) != 0 ? Value.TRUE : Value.FALSE);
    set(state, Ttl74155.G1, g1 ? Value.TRUE : Value.FALSE);
    set(state, Ttl74155.C1, c1 ? Value.TRUE : Value.FALSE);
    set(state, Ttl74155.G2, g2 ? Value.TRUE : Value.FALSE);
    set(state, Ttl74155.C2, c2 ? Value.TRUE : Value.FALSE);
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74155.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void set(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl74155.pinNrToPortNr(dsPinNr), value);
  }

  private static Value level(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74155.pinNrToPortNr(dsPinNr));
  }
}
