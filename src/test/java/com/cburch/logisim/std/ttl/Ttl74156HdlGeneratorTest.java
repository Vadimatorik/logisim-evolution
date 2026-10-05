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

class Ttl74156HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlEnablesSection1OnlyWhenItsStrobeIsLowAndItsDataIsHigh() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(
        hdl, "n1Y0 <= '0' WHEN n1G = '0' AND C1 = '1' AND B = '0' AND A = '0' ELSE '1';"));
    assertTrue(containsIgnoringCase(
        hdl, "n1Y3 <= '0' WHEN n1G = '0' AND C1 = '1' AND B = '1' AND A = '1' ELSE '1';"));
  }

  @Test
  void vhdlEnablesSection2OnlyWhenBothOfItsEnablesAreLow() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(
        hdl, "n2Y0 <= '0' WHEN n2G = '0' AND n2C = '0' AND B = '0' AND A = '0' ELSE '1';"));
    assertTrue(containsIgnoringCase(
        hdl, "n2Y2 <= '0' WHEN n2G = '0' AND n2C = '0' AND B = '1' AND A = '0' ELSE '1';"));
  }

  @Test
  void verilogKeepsTheOppositeEnablePolarityOfTheTwoSections() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains(
        "assign n1Y1 = (n1G == 0 && C1 == 1 && B == 0 && A == 1) ? 1'b0 : 1'b1;"));
    assertTrue(hdl.contains(
        "assign n2Y3 = (n2G == 0 && n2C == 0 && B == 1 && A == 1) ? 1'b0 : 1'b1;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74156HdlGenerator();
    final var attrs = new Ttl74156().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74156().createAttributeSet();
    return String.join(
        "\n", new Ttl74156HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
