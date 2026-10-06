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

/** HDL text for the 74HC563 octal inverting transparent latch. */
class Ttl74563HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlIsTransparentWhileLeIsHighAndReleasesTheBus() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "IF (LE = '1') THEN"));
    assertTrue(containsIgnoringCase(hdl, "state <= NOT(D7&D6&D5&D4&D3&D2&D1&D0);"));
    assertTrue(containsIgnoringCase(hdl, "Q0 <= state(0) WHEN nOE = '0' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "Q7 <= state(7) WHEN nOE = '0' ELSE 'Z';"));
  }

  @Test
  void verilogIsTransparentWhileLeIsHighAndReleasesTheBus() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("always @(*)"));
    assertTrue(hdl.contains("if (LE) state <= ~{D7, D6, D5, D4, D3, D2, D1, D0};"));
    assertTrue(hdl.contains("assign Q0 = (nOE == 0) ? state[0] : 1'bz;"));
    assertTrue(hdl.contains("assign Q7 = (nOE == 0) ? state[7] : 1'bz;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74563HdlGenerator();
    final var attrs = new Ttl74563().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74563().createAttributeSet();
    return String.join(
        "\n", new Ttl74563HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
