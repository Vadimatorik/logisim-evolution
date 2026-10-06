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

class Ttl744515HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlLatchesTheAddressWhileLeIsHighAndDecodesIt() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "IF (LE = '1') THEN"));
    assertTrue(containsIgnoringCase(hdl, "s_addr <= A3 & A2 & A1 & A0;"));
    assertTrue(containsIgnoringCase(hdl, "s_enabled <= (NOT nE);"));
    assertTrue(containsIgnoringCase(hdl, "Q0 <= NOT(s_enabled AND (s_addr = \"0000\"));"));
    assertTrue(containsIgnoringCase(hdl, "Q10 <= NOT(s_enabled AND (s_addr = \"1010\"));"));
    assertTrue(containsIgnoringCase(hdl, "Q15 <= NOT(s_enabled AND (s_addr = \"1111\"));"));
    assertFalse(containsIgnoringCase(hdl, "rising_edge"));
  }

  @Test
  void verilogLatchesTheAddressWhileLeIsHighAndDecodesIt() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("always @(*)"));
    assertTrue(hdl.contains("if (LE == 1) s_addr <= {A3, A2, A1, A0};"));
    assertTrue(hdl.contains("assign s_enabled = ~nE;"));
    assertTrue(hdl.contains("assign Q0 = ~(s_enabled & (s_addr == 4'b0000));"));
    assertTrue(hdl.contains("assign Q10 = ~(s_enabled & (s_addr == 4'b1010));"));
    assertTrue(hdl.contains("assign Q15 = ~(s_enabled & (s_addr == 4'b1111));"));
    assertFalse(hdl.contains("posedge"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl744515HdlGenerator();
    final var attrs = new Ttl744515().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl744515().createAttributeSet();
    return String.join(
        "\n", new Ttl744515HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
