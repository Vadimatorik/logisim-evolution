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

/** HDL text for the 74HC4510 presettable BCD up/down counter. */
class Ttl744510HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlResetsAndLoadsAsynchronouslyAndCountsOnTheRisingClock() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "PROCESS(clock, MR, PL, D0, D1, D2, D3)"));
    assertTrue(containsIgnoringCase(hdl, "IF (MR = '1') THEN"));
    assertTrue(containsIgnoringCase(hdl, "s_count <= \"0000\";"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (PL = '1') THEN"));
    assertTrue(containsIgnoringCase(hdl, "s_count <= s_data;"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (rising_edge(clock)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (tick = '1' AND CE = '0') THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (UP_DN = '1') THEN"));
    assertTrue(containsIgnoringCase(hdl, "s_count <= s_up;"));
    assertTrue(containsIgnoringCase(hdl, "s_count <= s_down;"));
    assertTrue(containsIgnoringCase(hdl, "\"0001\" WHEN s_count = \"0000\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "\"0000\" WHEN s_count = \"1001\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "\"0110\" WHEN s_count = \"1011\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "\"1001\" WHEN s_count = \"0000\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "\"1101\" WHEN s_count = \"1010\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "\"0011\" WHEN s_count = \"1100\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "s_count(0) = '1' AND s_count(3) = '1'"));
    assertTrue(containsIgnoringCase(hdl, "CE = '0' AND UP_DN = '0' AND s_count = \"0000\""));
    assertFalse(containsIgnoringCase(hdl, "IF (rising_edge(clock)) THEN s_count <= \"0000\""));
  }

  @Test
  void verilogResetsAndLoadsAsynchronouslyAndCountsOnTheRisingClock() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("always @(posedge clock)"));
    assertTrue(hdl.contains("if ((tick == 1) && (MR == 0) && (PL == 0) && (CE == 0))"));
    assertTrue(hdl.contains("(UP_DN == 1) ? s_up : s_down"));
    assertTrue(hdl.contains("(s_count == 4'b0000) ? 4'b0001"));
    assertTrue(hdl.contains("(s_count == 4'b1001) ? 4'b0000"));
    assertTrue(hdl.contains("(s_count == 4'b1011) ? 4'b0110"));
    assertTrue(hdl.contains("(s_count == 4'b0000) ? 4'b1001"));
    assertTrue(hdl.contains("(s_count == 4'b1010) ? 4'b1101"));
    assertTrue(hdl.contains("(s_count == 4'b1100) ? 4'b0011"));
    assertTrue(hdl.contains("(s_count == 4'b1110) ? 4'b0001"));
    assertTrue(hdl.contains("always @(MR or PL or D0 or D1 or D2 or D3)"));
    assertTrue(hdl.contains("if (MR == 1)"));
    assertTrue(hdl.contains("s_count <= 4'b0000;"));
    assertTrue(hdl.contains("else if (PL == 1)"));
    assertTrue(hdl.contains("s_count <= s_data;"));
    assertTrue(hdl.contains("(s_count[0] == 1) && (s_count[3] == 1)"));
    assertTrue(hdl.contains("(CE == 0) && (UP_DN == 0) && (s_count == 4'b0000)"));
    assertFalse(hdl.contains("always @(posedge clock or posedge MR)"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl744510HdlGenerator();
    final var attrs = new Ttl744510().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl744510().createAttributeSet();
    return String.join(
        "\n", new Ttl744510HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
