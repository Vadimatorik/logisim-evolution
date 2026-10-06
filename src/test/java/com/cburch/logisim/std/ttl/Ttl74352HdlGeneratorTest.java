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

/** HDL text for the 74x352 inverting dual 4-line to 1-line data selector. */
class Ttl74352HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlInvertsTheSelectedInputAndForcesTheOutputHighWhenInhibited() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "nY1 <= '1' WHEN nE1 = '1' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "NOT d1_0 WHEN s1 = '0' AND s0 = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "NOT d1_1 WHEN s1 = '0' AND s0 = '1' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "NOT d1_2 WHEN s1 = '1' AND s0 = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "NOT d1_3;"));
    assertTrue(containsIgnoringCase(hdl, "nY2 <= '1' WHEN nE2 = '1' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "NOT d2_3;"));
  }

  @Test
  void verilogInvertsTheSelectedInputAndForcesTheOutputHighWhenInhibited() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign nY1 = (nE1 == 1) ? 1'b1 :"));
    assertTrue(hdl.contains("(s1 == 0 && s0 == 0) ? ~d1_0 :"));
    assertTrue(hdl.contains("(s1 == 0 && s0 == 1) ? ~d1_1 :"));
    assertTrue(hdl.contains("(s1 == 1 && s0 == 0) ? ~d1_2 :"));
    assertTrue(hdl.contains("~d1_3;"));
    assertTrue(hdl.contains("assign nY2 = (nE2 == 1) ? 1'b1 :"));
    assertTrue(hdl.contains("~d2_3;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74352HdlGenerator();
    final var attrs = new Ttl74352().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74352().createAttributeSet();
    return String.join(
        "\n", new Ttl74352HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
