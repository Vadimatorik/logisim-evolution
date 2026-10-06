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

class Ttl74150HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlSelectsOneInputAndForcesTheOutputHigh() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "W <= '1' WHEN nG = '1' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "NOT s_data(to_integer(unsigned(s_select)));"));
    assertTrue(containsIgnoringCase(hdl, "s_select <= D & C & B & A;"));
    assertTrue(containsIgnoringCase(hdl, "E15 & E14 & E13 & E12 & E11 & E10 & E9 & E8 &"));
    assertTrue(containsIgnoringCase(hdl, "E7 & E6 & E5 & E4 & E3 & E2 & E1 & E0;"));
  }

  @Test
  void verilogSelectsOneInputAndForcesTheOutputHigh() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign W = (nG == 1) ? 1'b1 : ~s_data[s_select];"));
    assertTrue(hdl.contains("assign s_select = {D, C, B, A};"));
    assertTrue(hdl.contains("{E15, E14, E13, E12, E11, E10, E9, E8,"));
    assertTrue(hdl.contains("E7, E6, E5, E4, E3, E2, E1, E0};"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74150HdlGenerator();
    final var attrs = new Ttl74150().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74150().createAttributeSet();
    return String.join(
        "\n", new Ttl74150HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
