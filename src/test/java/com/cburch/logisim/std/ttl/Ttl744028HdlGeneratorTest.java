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

class Ttl744028HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlSelectsTheActiveHighOutputForABcdCode() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "s_addr <= A3 & A2 & A1 & A0;"));
    assertTrue(containsIgnoringCase(hdl, "Y0 <= '1' WHEN s_addr = \"0000\" ELSE '0';"));
    assertTrue(containsIgnoringCase(hdl, "Y9 <= '1' WHEN s_addr = \"1001\" ELSE '0';"));
    assertFalse(containsIgnoringCase(hdl, "\"1010\""));
  }

  @Test
  void verilogSelectsTheActiveHighOutputForABcdCode() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign s_addr = {A3, A2, A1, A0};"));
    assertTrue(hdl.contains("assign Y0 = (s_addr == 4'b0000);"));
    assertTrue(hdl.contains("assign Y9 = (s_addr == 4'b1001);"));
    assertFalse(hdl.contains("4'b1010"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl744028HdlGenerator();
    final var attrs = new Ttl744028().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl744028().createAttributeSet();
    return String.join(
        "\n", new Ttl744028HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
