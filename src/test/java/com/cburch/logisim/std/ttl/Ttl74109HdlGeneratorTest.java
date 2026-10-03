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

/** Generated HDL for the 74109 dual positive-edge J-K flip-flop. */
class Ttl74109HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlClocksOnTheRisingEdgeWithClearBeforePreset() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "nQ1 <= NOT(state1);"));
    assertTrue(containsIgnoringCase(hdl, "nQ2 <= NOT(state2);"));
    assertTrue(
        containsIgnoringCase(hdl, "next1 <= (J1 AND NOT(state1)) OR (K1 AND state1);"));
    assertTrue(
        containsIgnoringCase(hdl, "next2 <= (J2 AND NOT(state2)) OR (K2 AND state2);"));
    assertTrue(containsIgnoringCase(hdl, "IF (nRD1 = '0') THEN state1 <= '0';"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (nSD1 = '0') THEN state1 <= '1';"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (rising_edge(clock)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (tick = '1') THEN state1 <= next1;"));
    assertTrue(containsIgnoringCase(hdl, "IF (nRD2 = '0') THEN state2 <= '0';"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (nSD2 = '0') THEN state2 <= '1';"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (rising_edge(clock2)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (tick2 = '1') THEN state2 <= next2;"));
  }

  @Test
  void verilogClocksOnTheRisingEdgeWithClearBeforePreset() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign nQ1 = ~state1;"));
    assertTrue(hdl.contains("assign nQ2 = ~state2;"));
    assertTrue(hdl.contains("assign next1 = (J1 & ~state1) | (K1 & state1);"));
    assertTrue(hdl.contains("assign next2 = (J2 & ~state2) | (K2 & state2);"));
    assertTrue(hdl.contains("always @(posedge clock or negedge nRD1 or negedge nSD1)"));
    assertTrue(hdl.contains("if (nRD1 == 0) state1 <= 0;"));
    assertTrue(hdl.contains("else if (nSD1 == 0) state1 <= 1;"));
    assertTrue(hdl.contains("else if (tick == 1) state1 <= next1;"));
    assertTrue(hdl.contains("always @(posedge clock2 or negedge nRD2 or negedge nSD2)"));
    assertTrue(hdl.contains("if (nRD2 == 0) state2 <= 0;"));
    assertTrue(hdl.contains("else if (nSD2 == 0) state2 <= 1;"));
    assertTrue(hdl.contains("else if (tick2 == 1) state2 <= next2;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74109HdlGenerator();
    final var attrs = new Ttl74109().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74109().createAttributeSet();
    return String.join("\n", new Ttl74109HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
