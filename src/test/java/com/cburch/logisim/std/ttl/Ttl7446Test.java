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

/** Functional tests for the 7446 BCD to 7-segment decoder. */
class Ttl7446Test {
  /**
   * Active-low segment levels for codes 0 through 15. Bit 0 is {@code a} and bit 6 is {@code g};
   * a set bit is a high output, which leaves that segment off.
   */
  private static final int[] SEGMENT_LEVELS = {
    0x40, 0x79, 0x24, 0x30, 0x19, 0x12, 0x03, 0x78,
    0x00, 0x18, 0x27, 0x33, 0x1D, 0x16, 0x07, 0x7F
  };

  private static final int[] SEGMENT_PORTS = {
    Ttl7446.PORT_INDEX_QA,
    Ttl7446.PORT_INDEX_QB,
    Ttl7446.PORT_INDEX_QC,
    Ttl7446.PORT_INDEX_QD,
    Ttl7446.PORT_INDEX_QE,
    Ttl7446.PORT_INDEX_QF,
    Ttl7446.PORT_INDEX_QG
  };

  private static final int ALL_OFF = 0x7F;
  private static final int ALL_ON = 0x00;
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var decoder = new Ttl7446();
    final var hiddenPower = createInstance(decoder, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl7446.PORT_INDEX_B, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7446.PORT_INDEX_C, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7446.PORT_INDEX_LT, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7446.PORT_INDEX_BI_RBO, 70, 30, EndData.INPUT_OUTPUT);
    assertPort(hiddenPower, Ttl7446.PORT_INDEX_RBI, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7446.PORT_INDEX_D, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7446.PORT_INDEX_A, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7446.PORT_INDEX_QE, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7446.PORT_INDEX_QD, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7446.PORT_INDEX_QC, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7446.PORT_INDEX_QB, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7446.PORT_INDEX_QA, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7446.PORT_INDEX_QG, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7446.PORT_INDEX_QF, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(decoder, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void eachBcdCodeProducesItsDatasheetPattern() {
    final var decoder = new Ttl7446();

    for (var code = 0; code < SEGMENT_LEVELS.length; code++) {
      final var state = driven(decoder, code, Value.TRUE, Value.TRUE, Value.TRUE);
      decoder.propagate(state);

      assertSegments(state, SEGMENT_LEVELS[code]);
      assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7446.PORT_INDEX_BI_RBO));
    }
  }

  @Test
  void blankingInputOverridesTheLampTest() {
    final var decoder = new Ttl7446();
    final var state = driven(decoder, 5, Value.FALSE, Value.FALSE, Value.TRUE);
    decoder.propagate(state);

    assertSegments(state, ALL_OFF);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7446.PORT_INDEX_BI_RBO));
  }

  @Test
  void lampTestTurnsEverySegmentOnAndReleasesRippleBlanking() {
    final var decoder = new Ttl7446();
    final var state = driven(decoder, 0, Value.TRUE, Value.FALSE, Value.FALSE);
    decoder.propagate(state);

    assertSegments(state, ALL_ON);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7446.PORT_INDEX_BI_RBO));
  }

  @Test
  void rippleBlankingSuppressesAZeroAndPassesEveryOtherCode() {
    final var decoder = new Ttl7446();
    final var blanked = driven(decoder, 0, Value.TRUE, Value.TRUE, Value.FALSE);
    decoder.propagate(blanked);

    assertSegments(blanked, ALL_OFF);
    assertEquals(Value.FALSE, blanked.getPortValue(Ttl7446.PORT_INDEX_BI_RBO));

    final var shown = driven(decoder, 5, Value.TRUE, Value.TRUE, Value.FALSE);
    decoder.propagate(shown);

    assertSegments(shown, SEGMENT_LEVELS[5]);
    assertEquals(Value.UNKNOWN, shown.getPortValue(Ttl7446.PORT_INDEX_BI_RBO));
  }

  @Test
  void unknownBcdInputsLeaveTheSegmentsUnknown() {
    final var decoder = new Ttl7446();
    final var state = driven(decoder, 0, Value.TRUE, Value.TRUE, Value.TRUE);
    state.setPortValue(Ttl7446.PORT_INDEX_A, Value.UNKNOWN);
    decoder.propagate(state);

    for (final var port : SEGMENT_PORTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(port));
    }
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl7446.PORT_INDEX_BI_RBO));
  }

  @Test
  void invalidExposedPowerInputsMakeSegmentOutputsUnknown() {
    final var decoder = new Ttl7446();
    final var state = driven(decoder, 3, Value.TRUE, Value.TRUE, Value.TRUE, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    decoder.propagate(state);
    assertSegments(state, SEGMENT_LEVELS[3]);

    state.setPortValue(VCC_PORT, Value.FALSE);
    decoder.propagate(state);
    assertUnknownSegments(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    decoder.propagate(state);
    assertSegments(state, SEGMENT_LEVELS[3]);

    state.setPortValue(GND_PORT, Value.TRUE);
    decoder.propagate(state);
    assertUnknownSegments(state);
  }

  private static TtlTestInstanceState driven(
      Ttl7446 decoder, int code, Value blanking, Value lampTest, Value rippleBlanking) {
    return driven(decoder, code, blanking, lampTest, rippleBlanking, false);
  }

  private static TtlTestInstanceState driven(
      Ttl7446 decoder,
      int code,
      Value blanking,
      Value lampTest,
      Value rippleBlanking,
      boolean showPowerPins) {
    final var state = new TtlTestInstanceState(decoder, showPowerPins);
    state.setPortValue(Ttl7446.PORT_INDEX_A, bit(code, 0));
    state.setPortValue(Ttl7446.PORT_INDEX_B, bit(code, 1));
    state.setPortValue(Ttl7446.PORT_INDEX_C, bit(code, 2));
    state.setPortValue(Ttl7446.PORT_INDEX_D, bit(code, 3));
    state.setPortValue(Ttl7446.PORT_INDEX_BI_RBO, blanking);
    state.setPortValue(Ttl7446.PORT_INDEX_LT, lampTest);
    state.setPortValue(Ttl7446.PORT_INDEX_RBI, rippleBlanking);
    return state;
  }

  private static Value bit(int code, int shift) {
    return ((code >> shift) & 1) == 1 ? Value.TRUE : Value.FALSE;
  }

  private static void assertSegments(TtlTestInstanceState state, int levels) {
    for (var index = 0; index < SEGMENT_PORTS.length; index++) {
      final var expected = ((levels >> index) & 1) == 1 ? Value.TRUE : Value.FALSE;
      assertEquals(expected, state.getPortValue(SEGMENT_PORTS[index]));
    }
  }

  private static void assertUnknownSegments(TtlTestInstanceState state) {
    for (final var port : SEGMENT_PORTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(port));
    }
  }

  private static void assertPort(Instance instance, int port, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }
}
