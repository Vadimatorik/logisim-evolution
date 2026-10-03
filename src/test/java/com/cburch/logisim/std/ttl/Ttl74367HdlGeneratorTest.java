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

class Ttl74367HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlCopiesEachGroupOnlyWhileItsEnableIsLow() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "s_group1 <= NOT nOE1;"));
    assertTrue(containsIgnoringCase(hdl, "s_group2 <= NOT nOE2;"));
    assertTrue(containsIgnoringCase(hdl, "Y1 <= A1 WHEN s_group1 = '1' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "Y4 <= A4 WHEN s_group1 = '1' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "Y5 <= A5 WHEN s_group2 = '1' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "Y6 <= A6 WHEN s_group2 = '1' ELSE 'Z';"));
  }

  @Test
  void verilogCopiesEachGroupOnlyWhileItsEnableIsLow() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign s_group1 = ~nOE1;"));
    assertTrue(hdl.contains("assign s_group2 = ~nOE2;"));
    assertTrue(hdl.contains("assign Y1 = s_group1 ? A1 : 1'bZ;"));
    assertTrue(hdl.contains("assign Y4 = s_group1 ? A4 : 1'bZ;"));
    assertTrue(hdl.contains("assign Y5 = s_group2 ? A5 : 1'bZ;"));
    assertTrue(hdl.contains("assign Y6 = s_group2 ? A6 : 1'bZ;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74367HdlGenerator();
    final var attrs = new Ttl74367().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74367().createAttributeSet();
    return String.join(
        "\n", new Ttl74367HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
