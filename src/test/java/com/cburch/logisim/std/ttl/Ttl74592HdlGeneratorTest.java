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

/** HDL text for the 74HC592 input register and binary counter. */
class Ttl74592HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlClearsBeforeLoadAndCountsOnTheCounterClock() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "s_data <= H & G & F & E & D & C & B & A;"));
    assertTrue(containsIgnoringCase(hdl, "RCO <= '1' WHEN s_count = \"11111111\" ELSE '0';"));
    assertTrue(containsIgnoringCase(hdl, "IF (rising_edge(clock)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "s_reg <= s_reg_next;"));
    assertTrue(containsIgnoringCase(hdl, "IF (CCLR = '0') THEN"));
    assertTrue(containsIgnoringCase(hdl, "s_count <= \"00000000\";"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (CLOAD = '0') THEN"));
    assertTrue(containsIgnoringCase(hdl, "s_count <= s_reg;"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (rising_edge(clock2)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (CCKEN = '0' AND tick2 = '1') THEN"));
    assertTrue(containsIgnoringCase(hdl, "s_count <= std_logic_vector(unsigned(s_count) + 1);"));
  }

  @Test
  void verilogClearsBeforeLoadAndCountsOnTheCounterClock() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign s_data = {H, G, F, E, D, C, B, A};"));
    assertTrue(hdl.contains("assign RCO = (s_count == 8'b11111111);"));
    assertTrue(hdl.contains("always @(posedge clock)"));
    assertTrue(hdl.contains("s_reg <= s_reg_next;"));
    assertTrue(hdl.contains("always @(posedge clock2 or negedge CCLR or negedge CLOAD or s_reg)"));
    assertTrue(hdl.contains("if (CCLR == 0)"));
    assertTrue(hdl.contains("s_count <= 8'b00000000;"));
    assertTrue(hdl.contains("else if (CLOAD == 0)"));
    assertTrue(hdl.contains("s_count <= s_reg;"));
    assertTrue(hdl.contains("else if ((CCKEN == 0) && (tick2 == 1))"));
    assertTrue(hdl.contains("s_count <= s_count + 8'b00000001;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74592HdlGenerator();
    final var attrs = new Ttl74592().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74592().createAttributeSet();
    return String.join("\n", new Ttl74592HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
