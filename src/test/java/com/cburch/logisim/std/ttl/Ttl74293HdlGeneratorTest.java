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

class Ttl74293HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlCountsEachSectionOnItsOwnFallingEdgeAndClearsOnBothResets() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "falling_edge(A)"));
    assertTrue(containsIgnoringCase(hdl, "falling_edge(B)"));
    assertTrue(containsIgnoringCase(hdl, "nMR <= '0' WHEN R01 = '1' AND R02 = '1' ELSE '1';"));
    assertTrue(containsIgnoringCase(hdl, "IF (nMR = '0') THEN stateA <= '0';"));
    assertTrue(containsIgnoringCase(hdl, "IF (nMR = '0') THEN stateB <= \"000\";"));
  }

  @Test
  void verilogCountsEachSectionOnItsOwnFallingEdgeAndClearsOnBothResets() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("always @(negedge A or negedge nMR)"));
    assertTrue(hdl.contains("always @(negedge B or negedge nMR)"));
    assertTrue(hdl.contains("assign nMR = ~(R01 & R02);"));
    assertTrue(hdl.contains("if (nMR == 0) stateA <= 0;"));
    assertTrue(hdl.contains("if (nMR == 0) stateB <= 0;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74293HdlGenerator();
    final var attrs = new Ttl74293().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74293().createAttributeSet();
    return String.join(
        "\n", new Ttl74293HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
