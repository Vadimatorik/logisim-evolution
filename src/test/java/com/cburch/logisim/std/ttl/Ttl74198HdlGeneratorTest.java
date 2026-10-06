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

class Ttl74198HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlLoadsAndShiftsOnTheSelectedMode() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "nextState <= curState when tick = '0' else"));
    assertTrue(containsIgnoringCase(hdl, "D7&D6&D5&D4&D3&D2&D1&D0 when S1 = '1' and S0 = '1' else"));
    assertTrue(
        containsIgnoringCase(hdl, "curState(6 downto 0)&DSR when S1 = '0' and S0 = '1' else"));
    assertTrue(
        containsIgnoringCase(hdl, "DSL&curState(7 downto 1) when S1 = '1' and S0 = '0' else"));
    assertTrue(containsIgnoringCase(hdl, "Q0 <= curState(0);"));
    assertTrue(containsIgnoringCase(hdl, "Q7 <= curState(7);"));
  }

  @Test
  void vhdlClearsAsynchronously() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "if (MR = '0') then curState <= \"00000000\";"));
    assertTrue(containsIgnoringCase(hdl, "elsif (rising_edge(clock)) then"));
  }

  @Test
  void verilogLoadsAndShiftsOnTheSelectedMode() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign nextState = (tick == 0) ? curState :"));
    assertTrue(hdl.contains("(S1 == 1 && S0 == 1) ? {D7, D6, D5, D4, D3, D2, D1, D0} :"));
    assertTrue(hdl.contains("(S1 == 0 && S0 == 1) ? {curState[6:0], DSR} :"));
    assertTrue(hdl.contains("(S1 == 1 && S0 == 0) ? {DSL, curState[7:1]} :"));
    assertTrue(hdl.contains("assign Q0 = curState[0];"));
    assertTrue(hdl.contains("assign Q7 = curState[7];"));
  }

  @Test
  void verilogClearsAsynchronously() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("always @(posedge clock or negedge MR)"));
    assertTrue(hdl.contains("if (~MR) curState <= 0;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74198HdlGenerator();
    final var attrs = new Ttl74198().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74198().createAttributeSet();
    return String.join(
        "\n", new Ttl74198HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
