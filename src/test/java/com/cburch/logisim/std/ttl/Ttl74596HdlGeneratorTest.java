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

class Ttl74596HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlSinksOnlyAStoredZeroAndReleasesOtherwise() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(
        containsIgnoringCase(hdl, "QA <= '0' WHEN nOE = '0' AND storeReg(0) = '0' ELSE 'Z';"));
    assertTrue(
        containsIgnoringCase(hdl, "QH <= '0' WHEN nOE = '0' AND storeReg(7) = '0' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "QHp <= shiftReg(7);"));
    assertTrue(
        containsIgnoringCase(
            hdl, "nextShift <= shiftReg(6 DOWNTO 0) & SER WHEN tick = '1' ELSE shiftReg;"));
    assertTrue(containsIgnoringCase(hdl, "shiftReg WHEN tick2 = '1' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "IF (nSRCLR = '0') THEN shiftReg <= (OTHERS => '0');"));
    assertFalse(containsIgnoringCase(hdl, "storeReg <= nextShift"));
    assertFalse(containsIgnoringCase(hdl, "QA <= storeReg(0)"));
  }

  @Test
  void verilogSinksOnlyAStoredZeroAndReleasesOtherwise() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign QA = (nOE == 0 && storeReg[0] == 0) ? 1'b0 : 1'bZ;"));
    assertTrue(hdl.contains("assign QH = (nOE == 0 && storeReg[7] == 0) ? 1'b0 : 1'bZ;"));
    assertTrue(hdl.contains("assign QHp = shiftReg[7];"));
    assertTrue(hdl.contains("assign nextShift = (tick == 1) ? {shiftReg[6:0], SER} : shiftReg;"));
    assertTrue(
        hdl.contains(
            "assign nextStore = (tick2 == 1) ? ((nSRCLR == 0) ? 8'b0 : shiftReg) : storeReg;"));
    assertTrue(hdl.contains("if (nSRCLR == 0) shiftReg <= 8'b0;"));
    assertFalse(hdl.contains("assign QA = (nOE == 0) ? storeReg[0]"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74596HdlGenerator();
    final var attrs = new Ttl74596().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74596().createAttributeSet();
    return String.join(
        "\n", new Ttl74596HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
