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

/** HDL text for the 74399 quad 2-port register. */
class Ttl74399HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlStoresTheSelectedPortOnTheRisingEdge() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "nextState <= curState when tick = '0' else"));
    assertTrue(containsIgnoringCase(hdl, "I0d&I0c&I0b&I0a when S = '0' else"));
    assertTrue(containsIgnoringCase(hdl, "I1d&I1c&I1b&I1a;"));
    assertTrue(containsIgnoringCase(hdl, "if (rising_edge(clock)) then"));
    assertTrue(containsIgnoringCase(hdl, "Qa <= curState(0);"));
    assertTrue(containsIgnoringCase(hdl, "Qd <= curState(3);"));
  }

  @Test
  void verilogStoresTheSelectedPortOnTheRisingEdge() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign nextState = (tick == 0) ? curState :"));
    assertTrue(hdl.contains("(S == 0) ? {I0d, I0c, I0b, I0a} :"));
    assertTrue(hdl.contains("{I1d, I1c, I1b, I1a};"));
    assertTrue(hdl.contains("always @(posedge clock)"));
    assertTrue(hdl.contains("assign Qa = curState[0];"));
    assertTrue(hdl.contains("assign Qd = curState[3];"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74399HdlGenerator();
    final var attrs = new Ttl74399().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74399().createAttributeSet();
    return String.join("\n", new Ttl74399HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
