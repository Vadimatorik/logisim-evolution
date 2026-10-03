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

class Ttl74147HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlResolvesThePriorityCascadeFromTheHighestNumberedInput() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "\"1001\" WHEN nI9 = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "\"0100\" WHEN nI4 = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "\"0001\" WHEN nI1 = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "\"0000\";"));
    assertTrue(containsIgnoringCase(hdl, "nY0 <= NOT s_code(0);"));
    assertTrue(containsIgnoringCase(hdl, "nY3 <= NOT s_code(3);"));
  }

  @Test
  void verilogResolvesThePriorityCascadeFromTheHighestNumberedInput() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign s_code = (nI9 == 0) ? 4'b1001 :"));
    assertTrue(hdl.contains("(nI4 == 0) ? 4'b0100 :"));
    assertTrue(hdl.contains("(nI1 == 0) ? 4'b0001 :"));
    assertTrue(hdl.contains("4'b0000;"));
    assertTrue(hdl.contains("assign nY0 = ~s_code[0];"));
    assertTrue(hdl.contains("assign nY3 = ~s_code[3];"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74147HdlGenerator();
    final var attrs = new Ttl74147().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74147().createAttributeSet();
    return String.join(
        "\n", new Ttl74147HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
