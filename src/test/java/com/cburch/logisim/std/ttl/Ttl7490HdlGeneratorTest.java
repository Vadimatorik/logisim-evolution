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

/** HDL text for the 74HC90 decade ripple counter. */
class Ttl7490HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlCountsOnTheFallingEdgeAndGivesSetToNinePriority() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "QA <= stateA;"));
    assertTrue(containsIgnoringCase(hdl, "QB <= stateB(0);"));
    assertTrue(containsIgnoringCase(hdl, "QD <= stateB(2);"));
    assertTrue(containsIgnoringCase(hdl, "\"100\" WHEN stateB = \"011\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "IF (R91 = '1' AND R92 = '1') THEN stateA <= '1';"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (R01 = '1' AND R02 = '1') THEN stateA <= '0';"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (falling_edge(clock)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (tick = '1') THEN stateA <= NOT stateA;"));
    assertTrue(containsIgnoringCase(hdl, "IF (R91 = '1' AND R92 = '1') THEN stateB <= \"100\";"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (R01 = '1' AND R02 = '1') THEN stateB <= \"000\";"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (falling_edge(clock2)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (tick2 = '1') THEN stateB <= nextB;"));
  }

  @Test
  void verilogCountsOnTheFallingEdgeAndGivesSetToNinePriority() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign QA = stateA;"));
    assertTrue(hdl.contains("assign QB = stateB[0];"));
    assertTrue(hdl.contains("assign QD = stateB[2];"));
    assertTrue(hdl.contains("(stateB == 3'b011) ? 3'b100 :"));
    assertTrue(hdl.contains("always @(negedge clock or posedge R01 or posedge R02"));
    assertTrue(hdl.contains("if (R91 == 1 && R92 == 1) stateA <= 1;"));
    assertTrue(hdl.contains("else if (R01 == 1 && R02 == 1) stateA <= 0;"));
    assertTrue(hdl.contains("else if (tick == 1) stateA <= ~stateA;"));
    assertTrue(hdl.contains("always @(negedge clock2 or posedge R01 or posedge R02"));
    assertTrue(hdl.contains("if (R91 == 1 && R92 == 1) stateB <= 3'b100;"));
    assertTrue(hdl.contains("else if (R01 == 1 && R02 == 1) stateB <= 0;"));
    assertTrue(hdl.contains("else if (tick2 == 1) stateB <= nextB;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl7490HdlGenerator();
    final var attrs = new Ttl7490().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl7490().createAttributeSet();
    return String.join(
        "\n", new Ttl7490HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
