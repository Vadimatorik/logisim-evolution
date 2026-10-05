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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.prefs.AppPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** HDL checks for the 7446, which reuses the 7447 segment decoder. */
class Ttl7446HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlBlanksBeforeTheLampTestAndThenSuppressesAZero() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "(others => '1') when BI = '0' else"));
    assertTrue(containsIgnoringCase(hdl, "(others => '0') when LT = '0' else"));
    assertTrue(containsIgnoringCase(hdl, "(others => '1') when (RBI='0') and (bcd=x\"0\") else"));
  }

  @Test
  void verilogBlanksBeforeTheLampTestAndThenSuppressesAZero() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign realSegments = BI == 0 ? 7'h7F : LT == 0 ? 0 :"));
    assertTrue(hdl.contains("RBI == 0 && bcd == 0 ? 7'h7F : !segments;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var decoder = new Ttl7446();
    final var attrs = decoder.createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    final var generator = assertInstanceOf(Ttl7447HdlGenerator.class, decoder.getHDLGenerator(attrs));
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(decoder.isHDLSupportedComponent(attrs));
    assertFalse(generator.isHdlSupportedTarget(attrs));
    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var decoder = new Ttl7446();
    final var attrs = decoder.createAttributeSet();
    final var generator = assertInstanceOf(Ttl7447HdlGenerator.class, decoder.getHDLGenerator(attrs));
    return String.join("\n", generator.getModuleFunctionality(null, attrs).get());
  }
}
