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

/** HDL text checks for the 74x4020 14-stage binary ripple counter. */
class Ttl744020HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlCountsOnTheFallingClockAndClearsOnMasterReset() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "Q1  <= curState(0);"));
    assertTrue(containsIgnoringCase(hdl, "Q4  <= curState(3);"));
    assertTrue(containsIgnoringCase(hdl, "Q14 <= curState(13);"));
    assertFalse(containsIgnoringCase(hdl, "Q2  <="));
    assertFalse(containsIgnoringCase(hdl, "Q3  <="));
    assertTrue(containsIgnoringCase(hdl, "if (MR = '1') then"));
    assertTrue(containsIgnoringCase(hdl, "curState <= (others => '0');"));
    assertTrue(containsIgnoringCase(hdl, "elsif (falling_edge(clock)) then"));
    assertTrue(containsIgnoringCase(hdl, "if (tick = '1') then"));
    assertTrue(
        containsIgnoringCase(hdl, "curState <= std_logic_vector(unsigned(curState) + 1);"));
  }

  @Test
  void verilogCountsOnTheFallingClockAndClearsOnMasterReset() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign Q1  = curState[0];"));
    assertTrue(hdl.contains("assign Q4  = curState[3];"));
    assertTrue(hdl.contains("assign Q14 = curState[13];"));
    assertFalse(hdl.contains("assign Q2"));
    assertFalse(hdl.contains("assign Q3"));
    assertTrue(hdl.contains("always @(negedge clock or posedge MR)"));
    assertTrue(hdl.contains("if (MR == 1) curState <= 0;"));
    assertTrue(hdl.contains("else if (tick == 1) curState <= curState + 1;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl744020HdlGenerator();
    final var attrs = new Ttl744020().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl744020().createAttributeSet();
    return String.join(
        "\n", new Ttl744020HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
