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

class Ttl74298HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlStoresTheSelectedWordOnTheFallingEdge() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "nextState <= curState when tick = '0' else"));
    assertTrue(containsIgnoringCase(hdl, "D1&C1&B1&A1 when WS = '0' else"));
    assertTrue(containsIgnoringCase(hdl, "D2&C2&B2&A2;"));
    assertTrue(containsIgnoringCase(hdl, "if (falling_edge(clock)) then"));
    assertTrue(containsIgnoringCase(hdl, "QA <= curState(0);"));
    assertTrue(containsIgnoringCase(hdl, "QD <= curState(3);"));
  }

  @Test
  void verilogStoresTheSelectedWordOnTheFallingEdge() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign nextState = (tick == 0) ? curState :"));
    assertTrue(hdl.contains("(WS == 0) ? {D1, C1, B1, A1} :"));
    assertTrue(hdl.contains("{D2, C2, B2, A2};"));
    assertTrue(hdl.contains("always @(negedge clock)"));
    assertTrue(hdl.contains("assign QA = curState[0];"));
    assertTrue(hdl.contains("assign QD = curState[3];"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74298HdlGenerator();
    final var attrs = new Ttl74298().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74298().createAttributeSet();
    return String.join(
        "\n", new Ttl74298HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
