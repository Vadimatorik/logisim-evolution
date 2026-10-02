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

/** Generated HDL for the 74137 decoder with transparent address latches. */
class Ttl74137HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlLatchesWhileEnableIsLowAndDecodesActiveLow() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "s_enabled <= '1' when nE1 = '0' and E2 = '1' else '0';"));
    assertTrue(
        containsIgnoringCase(
            hdl, "nY0 <= '0' when s_enabled = '1' and s_address = \"000\" else '1';"));
    assertTrue(
        containsIgnoringCase(
            hdl, "nY7 <= '0' when s_enabled = '1' and s_address = \"111\" else '1';"));
    assertTrue(containsIgnoringCase(hdl, "process(nLE, A0, A1, A2)"));
    assertTrue(containsIgnoringCase(hdl, "if (nLE = '0') then"));
    assertTrue(containsIgnoringCase(hdl, "s_address <= A2 & A1 & A0;"));
    assertFalse(containsIgnoringCase(hdl, "rising_edge"));
    assertFalse(containsIgnoringCase(hdl, "falling_edge"));
  }

  @Test
  void verilogLatchesWhileEnableIsLowAndDecodesActiveLow() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign s_enabled = (nE1 == 0) && (E2 == 1);"));
    assertTrue(hdl.contains("assign nY0 = (s_enabled == 1 && s_address == 3'b000) ? 1'b0 : 1'b1;"));
    assertTrue(hdl.contains("assign nY7 = (s_enabled == 1 && s_address == 3'b111) ? 1'b0 : 1'b1;"));
    assertTrue(hdl.contains("always @(*)"));
    assertTrue(hdl.contains("if (nLE == 0) s_address = {A2, A1, A0};"));
    assertFalse(hdl.contains("posedge"));
    assertFalse(hdl.contains("negedge"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74137HdlGenerator();
    final var attrs = new Ttl74137().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74137().createAttributeSet();
    return String.join("\n", new Ttl74137HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
