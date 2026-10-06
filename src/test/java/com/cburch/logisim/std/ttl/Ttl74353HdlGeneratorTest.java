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

class Ttl74353HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlInvertsASourceOrReleasesTheOutput() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "Y1 <= 'Z' WHEN G1 = '1' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "not C3_1 WHEN B = '1' AND A = '1' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "not C2_1 WHEN B = '1' AND A = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "not C1_1 WHEN B = '0' AND A = '1' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "not C0_1;"));
    assertTrue(containsIgnoringCase(hdl, "Y2 <= 'Z' WHEN G2 = '1' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "not C3_2 WHEN B = '1' AND A = '1' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "not C0_2;"));
  }

  @Test
  void verilogInvertsASourceOrReleasesTheOutput() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign Y1 = (G1 == 1) ? 1'bZ :"));
    assertTrue(hdl.contains("({B, A} == 2'b11) ? ~C3_1 :"));
    assertTrue(hdl.contains("({B, A} == 2'b10) ? ~C2_1 :"));
    assertTrue(hdl.contains("({B, A} == 2'b01) ? ~C1_1 :"));
    assertTrue(hdl.contains("~C0_1;"));
    assertTrue(hdl.contains("assign Y2 = (G2 == 1) ? 1'bZ :"));
    assertTrue(hdl.contains("({B, A} == 2'b11) ? ~C3_2 :"));
    assertTrue(hdl.contains("~C0_2;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74353HdlGenerator();
    final var attrs = new Ttl74353().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74353().createAttributeSet();
    return String.join(
        "\n", new Ttl74353HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
