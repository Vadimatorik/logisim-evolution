/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.prefs.AppPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Generated VHDL and Verilog for the 74323 shift register. */
class Ttl74323HdlGeneratorTest {
  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlClearsBeforeLoadAndShifts() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(hdl.contains("\"00000000\" WHEN nCLR = '0' ELSE"));
    assertTrue(hdl.contains("s_load WHEN S1 = '1' AND S0 = '1' ELSE"));
    assertTrue(hdl.contains("s_right WHEN S1 = '0' AND S0 = '1' ELSE"));
    assertTrue(hdl.contains("s_left WHEN S1 = '1' AND S0 = '0' ELSE"));
    assertTrue(hdl.contains("s_right <= s_reg(6 DOWNTO 0) & SR;"));
    assertTrue(hdl.contains("s_left <= SL & s_reg(7 DOWNTO 1);"));
  }

  @Test
  void vhdlReleasesTheBusWhileLoadingOrDisabled() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(hdl.contains("Q0 <= s_reg(0);"));
    assertTrue(hdl.contains("Q7 <= s_reg(7);"));
    assertTrue(hdl.contains("IO0 <= s_reg(0) WHEN s_oe = '1' ELSE 'Z';"));
    assertTrue(hdl.contains("IO7 <= s_reg(7) WHEN s_oe = '1' ELSE 'Z';"));
    assertTrue(
        hdl.contains(
            "s_oe <= '1' WHEN nOE1 = '0' AND nOE2 = '0' AND NOT(S0 = '1' AND S1 = '1') ELSE"));
  }

  @Test
  void verilogClearsBeforeLoadAndShifts() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("(nCLR == 0) ? 8'b00000000 :"));
    assertTrue(hdl.contains("((S1 == 1) && (S0 == 1)) ? s_load :"));
    assertTrue(hdl.contains("((S1 == 0) && (S0 == 1)) ? s_right :"));
    assertTrue(hdl.contains("((S1 == 1) && (S0 == 0)) ? s_left :"));
    assertTrue(hdl.contains("assign s_right = {s_reg[6:0], SR};"));
    assertTrue(hdl.contains("assign s_left = {SL, s_reg[7:1]};"));
  }

  @Test
  void verilogReleasesTheBusWhileLoadingOrDisabled() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign Q0 = s_reg[0];"));
    assertTrue(hdl.contains("assign Q7 = s_reg[7];"));
    assertTrue(hdl.contains("assign IO0 = s_oe ? s_reg[0] : 1'bZ;"));
    assertTrue(hdl.contains("assign IO7 = s_oe ? s_reg[7] : 1'bZ;"));
    assertTrue(
        hdl.contains("assign s_oe = (nOE1 == 0) && (nOE2 == 0) && !((S0 == 1) && (S1 == 1));"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74323HdlGenerator();
    final var attrs = new Ttl74323().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));
    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74323().createAttributeSet();
    return String.join("\n", new Ttl74323HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
