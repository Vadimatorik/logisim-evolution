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

class Ttl74253HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlSelectsASourceOrReleasesTheOutput() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "Y1 <= 'Z' WHEN n1OE = '1' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "I3_1 WHEN S1 = '1' AND S0 = '1' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "I2_1 WHEN S1 = '1' AND S0 = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "I1_1 WHEN S1 = '0' AND S0 = '1' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "I0_1;"));
    assertTrue(containsIgnoringCase(hdl, "Y2 <= 'Z' WHEN n2OE = '1' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "I3_2 WHEN S1 = '1' AND S0 = '1' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "I0_2;"));
  }

  @Test
  void verilogSelectsASourceOrReleasesTheOutput() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign Y1 = (n1OE == 1) ? 1'bZ :"));
    assertTrue(hdl.contains("({S1, S0} == 2'b11) ? I3_1 :"));
    assertTrue(hdl.contains("({S1, S0} == 2'b10) ? I2_1 :"));
    assertTrue(hdl.contains("({S1, S0} == 2'b01) ? I1_1 :"));
    assertTrue(hdl.contains("I0_1;"));
    assertTrue(hdl.contains("assign Y2 = (n2OE == 1) ? 1'bZ :"));
    assertTrue(hdl.contains("({S1, S0} == 2'b11) ? I3_2 :"));
    assertTrue(hdl.contains("I0_2;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74253HdlGenerator();
    final var attrs = new Ttl74253().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74253().createAttributeSet();
    return String.join(
        "\n", new Ttl74253HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
