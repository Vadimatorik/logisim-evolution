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

/** HDL text for the 74HC191 synchronous presettable binary up/down counter. */
class Ttl74191HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlLoadsWhileParallelLoadIsLowAndCountsOnTheRisingEdge() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(
        containsIgnoringCase(
            hdl,
            "next <= std_logic_vector(unsigned(state) + 1) when tick = '1' and CE = '0' and UD = '0' else"));
    assertTrue(
        containsIgnoringCase(
            hdl,
            "std_logic_vector(unsigned(state) - 1) when tick = '1' and CE = '0' and UD = '1' else"));
    assertTrue(containsIgnoringCase(hdl, "if (PL = '0') then state <= D3 & D2 & D1 & D0;"));
    assertTrue(containsIgnoringCase(hdl, "elsif (rising_edge(clock)) then state <= next;"));
    assertTrue(
        containsIgnoringCase(
            hdl,
            "TC <= '1' when (UD = '0' and state = \"1111\") or (UD = '1' and state = \"0000\") else '0';"));
    assertTrue(
        containsIgnoringCase(hdl, "RC <= '0' when TC = '1' and CE = '0' and clock = '0' else '1';"));
  }

  @Test
  void verilogLoadsWhileParallelLoadIsLowAndCountsOnTheRisingEdge() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign next = (tick == 1 && CE == 0 && UD == 0) ? state + 1 :"));
    assertTrue(hdl.contains("(tick == 1 && CE == 0 && UD == 1) ? state - 1 : state;"));
    assertTrue(hdl.contains("always @(posedge clock or negedge PL or D0 or D1 or D2 or D3)"));
    assertTrue(hdl.contains("if (PL == 0) state <= {D3, D2, D1, D0};"));
    assertTrue(hdl.contains("else state <= next;"));
    assertTrue(
        hdl.contains(
            "assign TC = ((UD == 0 && state == 4'b1111) || (UD == 1 && state == 4'b0000)) ? 1'b1 : 1'b0;"));
    assertTrue(hdl.contains("assign RC = (TC == 1 && CE == 0 && clock == 0) ? 1'b0 : 1'b1;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74191HdlGenerator();
    final var attrs = new Ttl74191().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74191().createAttributeSet();
    return String.join("\n", new Ttl74191HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
