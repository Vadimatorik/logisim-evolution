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

class Ttl744015HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlShiftsEachRegisterTowardQ3AndClearsOnMasterReset() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "next1 <= state1(2 downto 0) & D1;"));
    assertTrue(containsIgnoringCase(hdl, "next2 <= state2(2 downto 0) & D2;"));
    assertTrue(containsIgnoringCase(hdl, "if (MR1 = '1') then state1 <= \"0000\";"));
    assertTrue(containsIgnoringCase(hdl, "if (MR2 = '1') then state2 <= \"0000\";"));
    assertTrue(containsIgnoringCase(hdl, "elsif (rising_edge(clock)) then"));
    assertTrue(containsIgnoringCase(hdl, "elsif (rising_edge(clock2)) then"));
    assertTrue(containsIgnoringCase(hdl, "Q10 <= state1(0);"));
    assertTrue(containsIgnoringCase(hdl, "Q23 <= state2(3);"));
  }

  @Test
  void verilogShiftsEachRegisterTowardQ3AndClearsOnMasterReset() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign next1 = {state1[2:0], D1};"));
    assertTrue(hdl.contains("assign next2 = {state2[2:0], D2};"));
    assertTrue(hdl.contains("if (MR1 == 1) state1 <= 4'b0000;"));
    assertTrue(hdl.contains("if (MR2 == 1) state2 <= 4'b0000;"));
    assertTrue(hdl.contains("always @(posedge clock or posedge MR1)"));
    assertTrue(hdl.contains("always @(posedge clock2 or posedge MR2)"));
    assertTrue(hdl.contains("assign Q10 = state1[0];"));
    assertTrue(hdl.contains("assign Q23 = state2[3];"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl744015HdlGenerator();
    final var attrs = new Ttl744015().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl744015().createAttributeSet();
    return String.join(
        "\n", new Ttl744015HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
