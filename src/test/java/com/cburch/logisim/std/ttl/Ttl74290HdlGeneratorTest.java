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

/** HDL text for the 74HC290 decade ripple counter. */
class Ttl74290HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlCountsOnTheFallingEdgeAndGivesSetToNinePriority() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "Q0 <= stateA;"));
    assertTrue(containsIgnoringCase(hdl, "Q1 <= stateB(0);"));
    assertTrue(containsIgnoringCase(hdl, "Q3 <= stateB(2);"));
    assertTrue(containsIgnoringCase(hdl, "\"100\" WHEN stateB = \"011\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "IF (MS1 = '1' AND MS2 = '1') THEN stateA <= '1';"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (MR1 = '1' AND MR2 = '1') THEN stateA <= '0';"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (falling_edge(clock)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (tick = '1') THEN stateA <= NOT stateA;"));
    assertTrue(containsIgnoringCase(hdl, "IF (MS1 = '1' AND MS2 = '1') THEN stateB <= \"100\";"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (MR1 = '1' AND MR2 = '1') THEN stateB <= \"000\";"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (falling_edge(clock2)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (tick2 = '1') THEN stateB <= nextB;"));
  }

  @Test
  void verilogCountsOnTheFallingEdgeAndGivesSetToNinePriority() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign Q0 = stateA;"));
    assertTrue(hdl.contains("assign Q1 = stateB[0];"));
    assertTrue(hdl.contains("assign Q3 = stateB[2];"));
    assertTrue(hdl.contains("(stateB == 3'b011) ? 3'b100 :"));
    assertTrue(hdl.contains("always @(negedge clock or posedge MR1 or posedge MR2"));
    assertTrue(hdl.contains("if (MS1 == 1 && MS2 == 1) stateA <= 1;"));
    assertTrue(hdl.contains("else if (MR1 == 1 && MR2 == 1) stateA <= 0;"));
    assertTrue(hdl.contains("else if (tick == 1) stateA <= ~stateA;"));
    assertTrue(hdl.contains("always @(negedge clock2 or posedge MR1 or posedge MR2"));
    assertTrue(hdl.contains("if (MS1 == 1 && MS2 == 1) stateB <= 3'b100;"));
    assertTrue(hdl.contains("else if (MR1 == 1 && MR2 == 1) stateB <= 0;"));
    assertTrue(hdl.contains("else if (tick2 == 1) stateB <= nextB;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74290HdlGenerator();
    final var attrs = new Ttl74290().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74290().createAttributeSet();
    return String.join(
        "\n", new Ttl74290HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
