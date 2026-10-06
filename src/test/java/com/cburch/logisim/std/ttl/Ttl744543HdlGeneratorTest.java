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

class Ttl744543HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlStoresTheBcdCodeWhileLoadIsHigh() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "s_data <= D3 & D2 & D1 & D0;"));
    assertTrue(containsIgnoringCase(hdl, "process(LD, s_data)"));
    assertTrue(containsIgnoringCase(hdl, "if (LD = '1') then"));
    assertTrue(containsIgnoringCase(hdl, "s_bcd <= s_data;"));
  }

  @Test
  void vhdlDecodesGlyphsAndLetsPhaseAndBlankingOverrideThem() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "\"1111110\" when \"0000\","));
    assertTrue(containsIgnoringCase(hdl, "\"1111111\" when \"1000\","));
    assertTrue(containsIgnoringCase(hdl, "\"0000000\" when others;"));
    assertTrue(containsIgnoringCase(hdl, "s_segments <= (others => PH) when BI = '1' else"));
    assertTrue(containsIgnoringCase(hdl, "s_glyph xor (others => PH);"));
    assertTrue(containsIgnoringCase(hdl, "f <= s_segments(1);"));
    assertTrue(containsIgnoringCase(hdl, "g <= s_segments(0);"));
  }

  @Test
  void verilogStoresTheBcdCodeWhileLoadIsHigh() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign s_data = {D3, D2, D1, D0};"));
    assertTrue(hdl.contains("always @(*)"));
    assertTrue(hdl.contains("if (LD == 1)"));
    assertTrue(hdl.contains("s_bcd = s_data;"));
  }

  @Test
  void verilogDecodesGlyphsAndLetsPhaseAndBlankingOverrideThem() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("4'b0000 : s_glyph = 7'b1111110;"));
    assertTrue(hdl.contains("4'b1000 : s_glyph = 7'b1111111;"));
    assertTrue(hdl.contains("default : s_glyph = 7'b0000000;"));
    assertTrue(hdl.contains("assign s_segments = BI ? s_phase : (s_glyph ^ s_phase);"));
    assertTrue(hdl.contains("assign f = s_segments[1];"));
    assertTrue(hdl.contains("assign g = s_segments[0];"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl744543HdlGenerator();
    final var attrs = new Ttl744543().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl744543().createAttributeSet();
    return String.join("\n", new Ttl744543HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
