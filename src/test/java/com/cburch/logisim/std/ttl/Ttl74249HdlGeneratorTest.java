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

/** Checks the exported 74249 decoder, including the tailed 6 and 9 glyphs. */
class Ttl74249HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlKeepsTailsOnSixAndNineAndBlanksBeforeTheLampTest() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "\"111\"&X\"D\""));
    assertTrue(containsIgnoringCase(hdl, "\"110\"&X\"F\""));
    assertFalse(containsIgnoringCase(hdl, "\"111\"&X\"C\""));
    assertFalse(containsIgnoringCase(hdl, "\"110\"&X\"7\""));
    assertTrue(containsIgnoringCase(hdl, "realSegments <= (OTHERS => '0') WHEN BI = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "(OTHERS => '1') WHEN LT = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "(OTHERS => '0') WHEN (RBI='0') AND (bcd=x\"0\") ELSE"));
  }

  @Test
  void verilogKeepsTailsOnSixAndNineAndBlanksBeforeTheLampTest() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("{3'b111, 4'hD}"));
    assertTrue(hdl.contains("{3'b110, 4'hF}"));
    assertFalse(hdl.contains("{3'b111, 4'hC}"));
    assertFalse(hdl.contains("{3'b110, 4'h7}"));
    assertTrue(hdl.contains(
        "assign realSegments = BI == 0 ? 7'h00 : LT == 0 ? 7'h7F : RBI == 0 && bcd == 0 ? 7'h00 : segments;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74249HdlGenerator();
    final var attrs = new Ttl74249().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74249().createAttributeSet();
    return String.join(
        "\n", new Ttl74249HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
