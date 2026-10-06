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

/** HDL text for the 74HC168 synchronous BCD decade up/down counter. */
class Ttl74168HdlGeneratorTest {
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
    assertTrue(containsIgnoringCase(hdl, "upCount when UD = '1'"));
    assertTrue(containsIgnoringCase(hdl, "\"0110\" when \"1011\""));
    assertTrue(containsIgnoringCase(hdl, "\"0010\" when others"));
    assertTrue(containsIgnoringCase(hdl, "\"1001\" when \"0000\""));
    assertTrue(containsIgnoringCase(hdl, "if (rising_edge(clock)) then"));
    assertTrue(containsIgnoringCase(hdl, "Q0 <= curState(0);"));
    assertTrue(containsIgnoringCase(hdl, "Q3 <= curState(3);"));
    assertTrue(containsIgnoringCase(hdl, "curState(0) = '1'"));
    assertTrue(containsIgnoringCase(hdl, "curState(3) = '1'"));
    assertTrue(containsIgnoringCase(hdl, "curState = \"0000\""));
  }

  @Test
  void verilogLoadsCountsAndDecodesTerminalCount() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign loaded = {D3, D2, D1, D0};"));
    assertTrue(hdl.contains("(curState == 4'b1001) ? 4'b0000"));
    assertTrue(hdl.contains("(curState == 4'b1011) ? 4'b0110"));
    assertTrue(hdl.contains("(curState == 4'b0000) ? 4'b1001"));
    assertTrue(hdl.contains("PE == 0 ? loaded"));
    assertTrue(hdl.contains("UD == 1 ? upCount : downCount"));
    assertTrue(hdl.contains("assign Q0 = curState[0];"));
    assertTrue(hdl.contains("assign Q3 = curState[3];"));
    assertTrue(hdl.contains("curState[0] == 1 && curState[3] == 1"));
    assertTrue(hdl.contains("curState == 4'b0000"));
    assertTrue(hdl.contains("always @(posedge clock)"));
    assertTrue(hdl.contains("curState <= nextState;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74168HdlGenerator();
    final var attrs = new Ttl74168().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74168().createAttributeSet();
    return String.join("\n", new Ttl74168HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
