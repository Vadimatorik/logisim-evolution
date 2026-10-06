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

/** HDL text for the 74HC597 storage register and shift register. */
class Ttl74597HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlStoresLoadsAndShifts() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "Q <= shiftReg(7);"));
    assertTrue(
        containsIgnoringCase(hdl, "storeReg <= D7 & D6 & D5 & D4 & D3 & D2 & D1 & D0;"));
    assertTrue(containsIgnoringCase(hdl, "IF (MR = '0' AND PL = '1') THEN"));
    assertTrue(containsIgnoringCase(hdl, "shiftReg <= (OTHERS => '0');"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (PL = '0' AND MR = '1') THEN"));
    assertTrue(containsIgnoringCase(hdl, "ELSE shiftReg <= storeReg;"));
    assertTrue(containsIgnoringCase(hdl, "shiftReg <= shiftReg(6 DOWNTO 0) & DS;"));
    assertFalse(containsIgnoringCase(hdl, "shiftReg <= (OTHERS => 'X')"));
  }

  @Test
  void verilogStoresLoadsAndShifts() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign Q = shiftReg[7];"));
    assertTrue(hdl.contains("if (tick2 == 1) storeReg <= {D7, D6, D5, D4, D3, D2, D1, D0};"));
    assertTrue(hdl.contains("always @(posedge clock2)"));
    assertTrue(hdl.contains("if (MR == 0 && PL == 1) shiftReg <= 8'b0;"));
    assertTrue(
        hdl.contains(
            "else if (PL == 0 && MR == 1) shiftReg <= (tick2 == 1) ? {D7, D6, D5, D4, D3, D2, D1, D0} : storeReg;"));
    assertTrue(hdl.contains("else if (tick == 1) shiftReg <= {shiftReg[6:0], DS};"));
    assertTrue(hdl.contains("posedge clock or posedge clock2 or negedge MR or negedge PL"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74597HdlGenerator();
    final var attrs = new Ttl74597().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74597().createAttributeSet();
    return String.join("\n", new Ttl74597HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
