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

class Ttl74237HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlFollowsTheAddressWhileTheLatchEnableIsLow() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "IF (nLE = '0') THEN s_addr <= A2 & A1 & A0;"));
    assertTrue(containsIgnoringCase(hdl, "s_enabled <= (NOT nE1) AND E2;"));
  }

  @Test
  void vhdlDecodesTheLatchedAddressAsActiveHighOneHot() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "Y0 <= s_enabled WHEN s_addr = \"000\" ELSE '0';"));
    assertTrue(containsIgnoringCase(hdl, "Y1 <= s_enabled WHEN s_addr = \"001\" ELSE '0';"));
    assertTrue(containsIgnoringCase(hdl, "Y7 <= s_enabled WHEN s_addr = \"111\" ELSE '0';"));
  }

  @Test
  void verilogFollowsTheAddressWhileTheLatchEnableIsLow() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("if (nLE == 0) s_addr <= {A2, A1, A0};"));
    assertTrue(hdl.contains("assign s_enabled = ~nE1 & E2;"));
  }

  @Test
  void verilogDecodesTheLatchedAddressAsActiveHighOneHot() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign Y0 = (s_addr == 3'b000) ? s_enabled : 1'b0;"));
    assertTrue(hdl.contains("assign Y1 = (s_addr == 3'b001) ? s_enabled : 1'b0;"));
    assertTrue(hdl.contains("assign Y7 = (s_addr == 3'b111) ? s_enabled : 1'b0;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74237HdlGenerator();
    final var attrs = new Ttl74237().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74237().createAttributeSet();
    return String.join(
        "\n", new Ttl74237HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
