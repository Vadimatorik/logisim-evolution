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

/** HDL text for the 74HC169 synchronous 4-bit up/down counter. */
class Ttl74169HdlGeneratorTest {
  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlLoadsCountsAndDecodesTerminalCount() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "loaded <= D3&D2&D1&D0;"));
    assertTrue(containsIgnoringCase(hdl, "nextState <= curState when tick = '0'"));
    assertTrue(containsIgnoringCase(hdl, "loaded when PE = '0'"));
    assertTrue(containsIgnoringCase(hdl, "unsigned(curState) + 1"));
    assertTrue(containsIgnoringCase(hdl, "unsigned(curState) - 1"));
    assertTrue(containsIgnoringCase(hdl, "if (rising_edge(clock)) then"));
    assertTrue(containsIgnoringCase(hdl, "Q0 <= curState(0);"));
    assertTrue(containsIgnoringCase(hdl, "Q3 <= curState(3);"));
    assertTrue(containsIgnoringCase(hdl, "curState = \"1111\""));
    assertTrue(containsIgnoringCase(hdl, "curState = \"0000\""));
  }

  @Test
  void verilogLoadsCountsAndDecodesTerminalCount() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign loaded = {D3, D2, D1, D0};"));
    assertTrue(hdl.contains("assign upCount = curState + 4'b0001;"));
    assertTrue(hdl.contains("assign downCount = curState - 4'b0001;"));
    assertTrue(hdl.contains("PE == 0 ? loaded"));
    assertTrue(hdl.contains("assign Q0 = curState[0];"));
    assertTrue(hdl.contains("assign Q3 = curState[3];"));
    assertTrue(hdl.contains("curState == 4'b1111"));
    assertTrue(hdl.contains("curState == 4'b0000"));
    assertTrue(hdl.contains("always @(posedge clock)"));
    assertTrue(hdl.contains("curState <= nextState;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74169HdlGenerator();
    final var attrs = new Ttl74169().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74169().createAttributeSet();
    return String.join("\n", new Ttl74169HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
