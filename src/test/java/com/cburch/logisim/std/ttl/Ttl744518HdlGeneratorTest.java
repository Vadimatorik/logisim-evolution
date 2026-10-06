/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.fpga.hdlgenerator.HdlText.containsIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.prefs.AppPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** HDL text for the 74HC4518 dual synchronous BCD counter. */
class Ttl744518HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlCountsOnEitherEdgeAndResetsAsynchronously() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "IF (MR_1 = '1') THEN count1 <= \"0000\";"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (rising_edge(CP0_1)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (CP1_1 = '1') THEN"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (falling_edge(CP1_1)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (CP0_1 = '0') THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (MR_2 = '1') THEN count2 <= \"0000\";"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (rising_edge(CP0_2)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (falling_edge(CP1_2)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "Q0_1 <= count1(0);"));
    assertTrue(containsIgnoringCase(hdl, "Q3_2 <= count2(3);"));
    assertTrue(containsIgnoringCase(hdl, "count1(0) <= NOT count1(0);"));
    assertTrue(
        containsIgnoringCase(
            hdl, "count1(1) <= count1(1) XOR (count1(0) AND NOT count1(3));"));
    assertTrue(containsIgnoringCase(hdl, "count2(0) <= NOT count2(0);"));
  }

  @Test
  void verilogCountsOnEitherEdgeAndResetsAsynchronously() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("always @(posedge CP0_1 or negedge CP1_1 or posedge MR_1)"));
    assertTrue(hdl.contains("if (MR_1) count1 <= 0;"));
    assertTrue(hdl.contains("else if (CP0_1 == 1 && CP1_1 == 1)"));
    assertTrue(hdl.contains("else if (CP0_1 == 0 && CP1_1 == 0)"));
    assertTrue(hdl.contains("always @(posedge CP0_2 or negedge CP1_2 or posedge MR_2)"));
    assertTrue(hdl.contains("if (MR_2) count2 <= 0;"));
    assertTrue(hdl.contains("assign Q0_1 = count1[0];"));
    assertTrue(hdl.contains("assign Q3_2 = count2[3];"));
    assertTrue(hdl.contains("count1[0] <= ~count1[0];"));
    assertTrue(
        hdl.contains(
            "count1[3] <= count1[3] ^ ((count1[0] & count1[1] & count1[2])"
                + " | (count1[0] & count1[3]));"));
    assertTrue(hdl.contains("count2[0] <= ~count2[0];"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl744518HdlGenerator();
    final var attrs = new Ttl744518().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl744518().createAttributeSet();
    return String.join(
        "\n", new Ttl744518HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
