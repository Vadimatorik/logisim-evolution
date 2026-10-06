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

/** HDL text for the 74HC162 synchronous BCD decade counter. */
class Ttl74162HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlSamplesResetLoadAndCountOnTheRisingClock() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "s_counted <= \"0001\" WHEN s_count = \"0000\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "\"0000\" WHEN s_count = \"1001\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "\"0110\" WHEN s_count = \"1011\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "s_next <= s_count WHEN tick = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "\"0000\" WHEN MR = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "s_data WHEN PE = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "s_counted WHEN CEP = '1' AND CET = '1' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "TC <= '1' WHEN CET = '1' AND s_count = \"1001\" ELSE '0';"));
    assertTrue(containsIgnoringCase(hdl, "IF (rising_edge(clock)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "s_count <= s_next;"));
    assertFalse(containsIgnoringCase(hdl, "IF (MR = '0') THEN"));
  }

  @Test
  void verilogSamplesResetLoadAndCountOnTheRisingClock() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("(s_count == 4'b0000) ? 4'b0001 :"));
    assertTrue(hdl.contains("(s_count == 4'b1001) ? 4'b0000 :"));
    assertTrue(hdl.contains("(s_count == 4'b1011) ? 4'b0110 :"));
    assertTrue(hdl.contains("assign s_next = (tick == 0) ? s_count :"));
    assertTrue(hdl.contains("(MR == 0) ? 4'b0000 :"));
    assertTrue(hdl.contains("(PE == 0) ? s_data :"));
    assertTrue(hdl.contains("((CEP == 1) && (CET == 1)) ? s_counted :"));
    assertTrue(hdl.contains("assign TC = (CET == 1) && (s_count == 4'b1001);"));
    assertTrue(hdl.contains("always @(posedge clock)"));
    assertTrue(hdl.contains("s_count <= s_next;"));
    assertFalse(hdl.contains("negedge MR"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74162HdlGenerator();
    final var attrs = new Ttl74162().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74162().createAttributeSet();
    return String.join("\n", new Ttl74162HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
