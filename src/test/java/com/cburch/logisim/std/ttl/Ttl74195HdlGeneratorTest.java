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

class Ttl74195HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlLoadsOnALowParallelEnableAndShiftsOtherwise() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "nextState <= curState when tick = '0' else"));
    assertTrue(containsIgnoringCase(hdl, "D3&D2&D1&D0 when PE = '0' else"));
    assertTrue(containsIgnoringCase(hdl, "curState(2 downto 0) & serial"));
    assertTrue(
        containsIgnoringCase(
            hdl, "serial <= (J and not(curState(0))) or (K and curState(0));"));
  }

  @Test
  void vhdlClearsAsynchronouslyAndInvertsTheLastStage() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "if (MR = '0') then curState <= \"0000\";"));
    assertTrue(containsIgnoringCase(hdl, "elsif (rising_edge(clock)) then"));
    assertTrue(containsIgnoringCase(hdl, "nQ3 <= not(curState(3));"));
    assertTrue(containsIgnoringCase(hdl, "Q0  <= curState(0);"));
  }

  @Test
  void verilogLoadsOnALowParallelEnableAndShiftsOtherwise() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign serial    = (J & ~curState[0]) | (K & curState[0]);"));
    assertTrue(hdl.contains("assign nextState = (tick == 0) ? curState :"));
    assertTrue(hdl.contains("(PE == 0) ? {D3, D2, D1, D0} :"));
    assertTrue(hdl.contains("{curState[2:0], serial};"));
  }

  @Test
  void verilogClearsAsynchronouslyAndInvertsTheLastStage() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("always @(posedge clock or negedge MR)"));
    assertTrue(hdl.contains("if (~MR) curState <= 0;"));
    assertTrue(hdl.contains("assign nQ3       = ~curState[3];"));
    assertTrue(hdl.contains("assign Q0        = curState[0];"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74195HdlGenerator();
    final var attrs = new Ttl74195().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74195().createAttributeSet();
    return String.join(
        "\n", new Ttl74195HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
