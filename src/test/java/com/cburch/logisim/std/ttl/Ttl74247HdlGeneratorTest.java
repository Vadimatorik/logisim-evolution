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

/** HDL text for the 74x247 BCD to 7-segment decoder/driver. */
class Ttl74247HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlBlanksFromBiBeforeLampTestAndInvertsTheGlyph() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "Sega <= realSegments(0);"));
    assertTrue(containsIgnoringCase(hdl, "Segg <= realSegments(6);"));
    assertTrue(containsIgnoringCase(hdl, "bcd <= D&C&B&A;"));
    assertTrue(containsIgnoringCase(hdl, "WHEN BI = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "WHEN LT = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "(RBI='0') AND (bcd=x\"0\")"));
    assertTrue(containsIgnoringCase(hdl, "NOT(segments);"));
  }

  @Test
  void vhdlKeepsTheTailsOnSixAndNine() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "\"011\"&X\"F\" WHEN X\"0\""));
    assertTrue(containsIgnoringCase(hdl, "\"111\"&X\"D\" WHEN X\"6\""));
    assertTrue(containsIgnoringCase(hdl, "\"110\"&X\"F\" WHEN X\"9\""));
    assertFalse(containsIgnoringCase(hdl, "\"111\"&X\"C\" WHEN X\"6\""));
    assertFalse(containsIgnoringCase(hdl, "\"110\"&X\"7\" WHEN X\"9\""));
    assertTrue(containsIgnoringCase(hdl, "\"000\"&X\"0\" WHEN OTHERS"));
  }

  @Test
  void verilogBlanksFromBiBeforeLampTestAndInvertsTheGlyph() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign Sega = realSegments[0];"));
    assertTrue(hdl.contains("assign Segg = realSegments[6];"));
    assertTrue(hdl.contains("assign bcd  = {D, C, B, A};"));
    assertTrue(hdl.contains(
        "assign realSegments = BI == 0 ? 7'h7F : LT == 0 ? 0 : RBI == 0 && bcd == 0 ? 7'h7F : !segments;"));
  }

  @Test
  void verilogKeepsTheTailsOnSixAndNine() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("4'h0 : s_decoder1_reg = {3'b011, 4'hF};"));
    assertTrue(hdl.contains("4'h6 : s_decoder1_reg = {3'b111, 4'hD};"));
    assertTrue(hdl.contains("4'h9 : s_decoder1_reg = {3'b110, 4'hF};"));
    assertFalse(hdl.contains("4'h6 : s_decoder1_reg = {3'b111, 4'hC};"));
    assertFalse(hdl.contains("4'h9 : s_decoder1_reg = {3'b110, 4'h7};"));
    assertTrue(hdl.contains("default : s_decoder1_reg = {3'b000, 4'h0};"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74247HdlGenerator();
    final var attrs = new Ttl74247().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74247().createAttributeSet();
    return String.join("\n", new Ttl74247HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
