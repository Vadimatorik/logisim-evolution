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

/** HDL text for the 74HC40102 synchronous BCD down counter. */
class Ttl7440102HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlResetsPresetsAndCountsOnTheRisingClock() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "s_units_next <= \"1001\" WHEN s_units = \"0000\" ELSE"));
    assertTrue(
        containsIgnoringCase(
            hdl, "s_tens_next <= \"1001\" WHEN s_units = \"0000\" AND s_tens = \"0000\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "s_sync <= s_data WHEN PE = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "s_counted WHEN TE = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "TC <= '0' WHEN s_count = \"00000000\" AND TE = '0' ELSE '1';"));
    assertTrue(
        containsIgnoringCase(
            hdl, "count : PROCESS (clock, MR, PL, P0, P1, P2, P3, P4, P5, P6, P7) IS"));
    assertTrue(containsIgnoringCase(hdl, "IF (MR = '0') THEN"));
    assertTrue(containsIgnoringCase(hdl, "s_count <= \"10011001\";"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (PL = '0') THEN"));
    assertTrue(containsIgnoringCase(hdl, "s_count <= P7 & P6 & P5 & P4 & P3 & P2 & P1 & P0;"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (rising_edge(clock)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (tick = '1') THEN"));
    assertTrue(containsIgnoringCase(hdl, "s_count <= s_sync;"));
  }

  @Test
  void verilogFollowsJamInputsWhilePresetStaysLow() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign s_units_next = (s_units == 4'b0000) ? 4'b1001 : (s_units - 4'b0001);"));
    assertTrue(hdl.contains("(s_tens == 4'b0000) ? 4'b1001 : (s_tens - 4'b0001);"));
    assertTrue(hdl.contains("assign s_sync = (PE == 0) ? s_data : (TE == 0) ? s_counted : s_count;"));
    assertTrue(hdl.contains("assign TC = ((s_count == 8'b00000000) && (TE == 0)) ? 1'b0 : 1'b1;"));
    assertTrue(hdl.contains("always @(posedge clock or negedge clock or negedge MR or negedge PL"));
    assertTrue(hdl.contains("or P0 or P1 or P2 or P3 or P4 or P5 or P6 or P7)"));
    assertTrue(hdl.contains("if (MR == 0) s_count <= 8'b10011001;"));
    assertTrue(hdl.contains("else if (PL == 0) s_count <= {P7, P6, P5, P4, P3, P2, P1, P0};"));
    assertTrue(
        hdl.contains(
            "else if (clock == 1 && s_clock_was_low == 1 && tick == 1) s_count <= s_sync;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl7440102HdlGenerator();
    final var attrs = new Ttl7440102().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl7440102().createAttributeSet();
    return String.join(
        "\n", new Ttl7440102HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
