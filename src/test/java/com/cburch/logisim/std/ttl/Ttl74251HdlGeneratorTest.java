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

/** HDL text for the 74HC251 8-line to 1-line data selector. */
class Ttl74251HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlSelectsOneInputAndReleasesBothOutputs() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "s_select <= S2 & S1 & S0;"));
    assertTrue(containsIgnoringCase(hdl, "s_y <= I0 WHEN s_select = \"000\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "I3 WHEN s_select = \"011\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "I7;"));
    assertTrue(containsIgnoringCase(hdl, "Y  <= s_y WHEN nOE = '0' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "nY <= (NOT s_y) WHEN nOE = '0' ELSE 'Z';"));
  }

  @Test
  void verilogSelectsOneInputAndReleasesBothOutputs() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign s_select = {S2, S1, S0};"));
    assertTrue(hdl.contains("assign s_y = (s_select == 3'b000) ? I0 :"));
    assertTrue(hdl.contains("(s_select == 3'b011) ? I3 :"));
    assertTrue(hdl.contains("(s_select == 3'b110) ? I6 :"));
    assertTrue(hdl.contains("I7;"));
    assertTrue(hdl.contains("assign Y  = (nOE == 0) ? s_y : 1'bz;"));
    assertTrue(hdl.contains("assign nY = (nOE == 0) ? ~s_y : 1'bz;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74251HdlGenerator();
    final var attrs = new Ttl74251().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74251().createAttributeSet();
    return String.join(
        "\n", new Ttl74251HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
