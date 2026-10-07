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

class Ttl74239HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlEnablesTheSelectedActiveHighOutput() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "s_enabled1 <= NOT nE1;"));
    assertTrue(
        containsIgnoringCase(
            hdl, "Y0_1 <= s_enabled1 AND (NOT A1_1) AND (NOT A0_1);"));
    assertTrue(containsIgnoringCase(hdl, "Y3_1 <= s_enabled1 AND A1_1 AND A0_1;"));
    assertTrue(containsIgnoringCase(hdl, "s_enabled2 <= NOT nE2;"));
    assertTrue(
        containsIgnoringCase(
            hdl, "Y1_2 <= s_enabled2 AND (NOT A1_2) AND A0_2;"));
    assertTrue(containsIgnoringCase(hdl, "Y2_2 <= s_enabled2 AND A1_2 AND (NOT A0_2);"));
  }

  @Test
  void verilogEnablesTheSelectedActiveHighOutput() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign s_enabled1 = ~nE1;"));
    assertTrue(hdl.contains("assign Y0_1 = s_enabled1 & ~A1_1 & ~A0_1;"));
    assertTrue(hdl.contains("assign Y3_1 = s_enabled1 & A1_1 & A0_1;"));
    assertTrue(hdl.contains("assign s_enabled2 = ~nE2;"));
    assertTrue(hdl.contains("assign Y1_2 = s_enabled2 & ~A1_2 & A0_2;"));
    assertTrue(hdl.contains("assign Y2_2 = s_enabled2 & A1_2 & ~A0_2;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74239HdlGenerator();
    final var attrs = new Ttl74239().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74239().createAttributeSet();
    return String.join(
        "\n", new Ttl74239HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
