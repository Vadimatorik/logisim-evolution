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

class Ttl74174HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlClearsAsynchronouslyAndLoadsOnTheRisingEdge() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "D6&D5&D4&D3&D2&D1"));
    assertTrue(containsIgnoringCase(hdl, "if (nCLR = '0') then curState <= \"000000\";"));
    assertTrue(containsIgnoringCase(hdl, "elsif (rising_edge(clock)) then"));
    assertTrue(containsIgnoringCase(hdl, "Q1 <= curState(0);"));
    assertTrue(containsIgnoringCase(hdl, "Q6 <= curState(5);"));
  }

  @Test
  void verilogClearsAsynchronouslyAndLoadsOnTheRisingEdge() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("{D6, D5, D4, D3, D2, D1}"));
    assertTrue(hdl.contains("assign Q1        = curState[0];"));
    assertTrue(hdl.contains("assign Q6        = curState[5];"));
    assertTrue(hdl.contains("always @(posedge clock or negedge nCLR)"));
    assertTrue(hdl.contains("if (~nCLR) curState <= 0;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74174HdlGenerator();
    final var attrs = new Ttl74174().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74174().createAttributeSet();
    return String.join(
        "\n", new Ttl74174HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
