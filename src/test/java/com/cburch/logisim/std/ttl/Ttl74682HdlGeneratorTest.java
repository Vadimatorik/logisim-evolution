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

class Ttl74682HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlDrivesActiveLowEqualAndGreaterOutputs() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "oppP   <= P7&P6&P5&P4&P3&P2&P1&P0;"));
    assertTrue(containsIgnoringCase(hdl, "oppQ   <= Q7&Q6&Q5&Q4&Q3&Q2&Q1&Q0;"));
    assertTrue(containsIgnoringCase(
        hdl, "nPQ    <= '0' WHEN unsigned(oppP) = unsigned(oppQ) ELSE '1';"));
    assertTrue(containsIgnoringCase(
        hdl, "nPGTQ  <= '0' WHEN unsigned(oppP) > unsigned(oppQ) ELSE '1';"));
  }

  @Test
  void verilogDrivesActiveLowEqualAndGreaterOutputs() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign oppP   = {P7, P6, P5, P4, P3, P2, P1, P0};"));
    assertTrue(hdl.contains("assign oppQ   = {Q7, Q6, Q5, Q4, Q3, Q2, Q1, Q0};"));
    assertTrue(hdl.contains("assign nPQ    = (oppP == oppQ) ? 0 : 1;"));
    assertTrue(hdl.contains("assign nPGTQ  = (oppP > oppQ) ? 0 : 1;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74682HdlGenerator();
    final var attrs = new Ttl74682().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74682().createAttributeSet();
    return String.join(
        "\n", new Ttl74682HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
