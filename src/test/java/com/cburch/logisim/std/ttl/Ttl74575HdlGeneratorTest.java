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

/** HDL text for the 74575 octal D flip-flop with synchronous clear. */
class Ttl74575HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlClearsOnTheRisingEdgeAndReleasesWhenDisabled() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "next <= (others => '0') WHEN nCLR = '0' AND tick = '1' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "D8&D7&D6&D5&D4&D3&D2&D1 WHEN tick = '1' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "Q1   <= state(0) WHEN nOE = '0' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "Q8   <= state(7) WHEN nOE = '0' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "IF (rising_edge(clock)) THEN state <= next;"));
    assertFalse(containsIgnoringCase(hdl, "negedge"));
    assertFalse(containsIgnoringCase(hdl, "IF (nCLR = '0')"));
  }

  @Test
  void verilogClearsOnTheRisingEdgeAndReleasesWhenDisabled() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign next = (tick == 0) ? state : ((nCLR == 0) ? 8'b0 : {D8, D7, D6, D5, D4, D3, D2, D1});"));
    assertTrue(hdl.contains("assign Q1 = (nOE == 0) ? state[0] : 1'bz;"));
    assertTrue(hdl.contains("assign Q8 = (nOE == 0) ? state[7] : 1'bz;"));
    assertTrue(hdl.contains("always @(posedge clock)"));
    assertTrue(hdl.contains("state <= next;"));
    assertFalse(hdl.contains("negedge"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74575HdlGenerator();
    final var attrs = new Ttl74575().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74575().createAttributeSet();
    return String.join(
        "\n", new Ttl74575HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
