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

/** HDL text for the 74HC196 presettable decade ripple counter. */
class Ttl74196HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlClearsBeforeLoadAndCountsOnFallingClocks() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "\"001\" WHEN stateB = \"000\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "\"100\" WHEN stateB = \"011\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "\"000\";"));
    assertTrue(containsIgnoringCase(hdl, "IF (MR = '0') THEN stateA <= '0';"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (PL = '0') THEN stateA <= P0;"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (falling_edge(clock)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (tick = '1') THEN stateA <= NOT stateA;"));
    assertTrue(containsIgnoringCase(hdl, "IF (MR = '0') THEN stateB <= \"000\";"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (PL = '0') THEN stateB <= P3 & P2 & P1;"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (falling_edge(clock2)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (tick2 = '1') THEN stateB <= nextB;"));
  }

  @Test
  void verilogClearsBeforeLoadAndCountsOnFallingClocks() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("(stateB == 3'b000) ? 3'b001 :"));
    assertTrue(hdl.contains("(stateB == 3'b011) ? 3'b100 :"));
    assertTrue(hdl.contains("3'b000;"));
    assertTrue(hdl.contains("if (MR == 0) stateA <= 0;"));
    assertTrue(hdl.contains("else if (PL == 0) stateA <= P0;"));
    assertTrue(hdl.contains("else if (tick == 1) stateA <= ~stateA;"));
    assertTrue(hdl.contains("if (MR == 0) stateB <= 0;"));
    assertTrue(hdl.contains("else if (PL == 0) stateB <= {P3, P2, P1};"));
    assertTrue(hdl.contains("else if (tick2 == 1) stateB <= nextB;"));
    assertTrue(hdl.contains("always @(negedge clock or negedge MR or negedge PL or P0)"));
    assertTrue(hdl.contains("always @(negedge clock2 or negedge MR or negedge PL or P1 or P2 or P3)"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74196HdlGenerator();
    final var attrs = new Ttl74196().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74196().createAttributeSet();
    return String.join("\n", new Ttl74196HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
