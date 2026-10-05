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

/** HDL text for the 74HC40103 synchronous binary down counter. */
class Ttl7440103HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlResetsAndPresetsAsynchronouslyThenCountsOnTheRisingClock() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "s_next <= s_count WHEN tick = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "s_data WHEN PE = '0' ELSE"));
    assertTrue(
        containsIgnoringCase(
            hdl, "std_logic_vector(unsigned(s_count) - 1) WHEN TE = '0' ELSE"));
    assertTrue(
        containsIgnoringCase(hdl, "TC <= '0' WHEN TE = '0' AND s_count = \"00000000\" ELSE '1';"));
    assertTrue(containsIgnoringCase(hdl, "IF (MR = '0') THEN"));
    assertTrue(containsIgnoringCase(hdl, "s_count <= (OTHERS => '1');"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (PL = '0') THEN"));
    assertTrue(containsIgnoringCase(hdl, "s_count <= s_data;"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (rising_edge(clock)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "s_count <= s_next;"));
  }

  @Test
  void verilogResetsAndPresetsAsynchronouslyThenCountsOnTheRisingClock() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign s_next = (tick == 0) ? s_count :"));
    assertTrue(hdl.contains("(PE == 0) ? s_data :"));
    assertTrue(hdl.contains("(TE == 0) ? (s_count - 8'b00000001) :"));
    assertTrue(hdl.contains("assign TC = ((TE == 0) && (s_count == 8'b00000000)) ? 1'b0 : 1'b1;"));
    assertTrue(hdl.contains("always @(posedge clock or negedge MR or negedge PL or s_data)"));
    assertTrue(hdl.contains("if (MR == 0) s_count <= 8'b11111111;"));
    assertTrue(hdl.contains("else if (PL == 0) s_count <= s_data;"));
    assertTrue(hdl.contains("else s_count <= s_next;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl7440103HdlGenerator();
    final var attrs = new Ttl7440103().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl7440103().createAttributeSet();
    return String.join("\n", new Ttl7440103HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
