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

/** HDL text for the 74HC4511 BCD to 7-segment latch/decoder/driver. */
class Ttl744511HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlFollowsWhileLatchEnableIsLowAndHoldsOtherwise() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "IF (LE = '0') THEN"));
    assertTrue(containsIgnoringCase(hdl, "stored <= D&C&B&A;"));
    assertTrue(containsIgnoringCase(hdl, "Sega <= shown(0);"));
    assertTrue(containsIgnoringCase(hdl, "Segg <= shown(6);"));
    assertTrue(containsIgnoringCase(hdl, "WHEN LT = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "WHEN BI = '0' ELSE"));
    assertFalse(containsIgnoringCase(hdl, "rising_edge"));
  }

  @Test
  void vhdlUsesThe4511GlyphsIncludingBlankCodes() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "\"011\"&X\"F\" WHEN X\"0\""));
    assertTrue(containsIgnoringCase(hdl, "\"111\"&X\"C\" WHEN X\"6\""));
    assertTrue(containsIgnoringCase(hdl, "\"110\"&X\"7\" WHEN X\"9\""));
    assertTrue(containsIgnoringCase(hdl, "\"000\"&X\"0\" WHEN OTHERS"));
  }

  @Test
  void verilogFollowsWhileLatchEnableIsLowAndHoldsOtherwise() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("always @(*)"));
    assertTrue(hdl.contains("if (LE == 0) stored = {D, C, B, A};"));
    assertTrue(hdl.contains("assign Sega = shown[0];"));
    assertTrue(hdl.contains("assign Segg = shown[6];"));
    assertTrue(hdl.contains("(LT == 0) ? 7'b1111111 : (BI == 0) ? 7'b0000000 : segments"));
    assertFalse(hdl.contains("posedge"));
  }

  @Test
  void verilogUsesThe4511GlyphsIncludingBlankCodes() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("4'h0 : s_decoder1_reg = {3'b011, 4'hF};"));
    assertTrue(hdl.contains("4'h6 : s_decoder1_reg = {3'b111, 4'hC};"));
    assertTrue(hdl.contains("4'h9 : s_decoder1_reg = {3'b110, 4'h7};"));
    assertTrue(hdl.contains("default : s_decoder1_reg = {3'b000, 4'h0};"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl744511HdlGenerator();
    final var attrs = new Ttl744511().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl744511().createAttributeSet();
    return String.join("\n", new Ttl744511HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
