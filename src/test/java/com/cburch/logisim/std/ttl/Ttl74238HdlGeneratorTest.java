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

class Ttl74238HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlEnablesTheSelectedActiveHighOutput() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "s_enabled <= (NOT nE1) AND (NOT nE2) AND E3;"));
    assertTrue(
        containsIgnoringCase(
            hdl, "Y0 <= s_enabled AND (NOT A2) AND (NOT A1) AND (NOT A0);"));
    assertTrue(containsIgnoringCase(hdl, "Y3 <= s_enabled AND (NOT A2) AND A1 AND A0;"));
    assertTrue(containsIgnoringCase(hdl, "Y7 <= s_enabled AND A2 AND A1 AND A0;"));
  }

  @Test
  void verilogEnablesTheSelectedActiveHighOutput() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign s_enabled = ~nE1 & ~nE2 & E3;"));
    assertTrue(hdl.contains("assign Y0 = s_enabled & ~A2 & ~A1 & ~A0;"));
    assertTrue(hdl.contains("assign Y3 = s_enabled & ~A2 & A1 & A0;"));
    assertTrue(hdl.contains("assign Y7 = s_enabled & A2 & A1 & A0;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74238HdlGenerator();
    final var attrs = new Ttl74238().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74238().createAttributeSet();
    return String.join(
        "\n", new Ttl74238HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
