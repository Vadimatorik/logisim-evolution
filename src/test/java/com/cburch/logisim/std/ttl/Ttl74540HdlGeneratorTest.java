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

class Ttl74540HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlInvertsEachChannelWhileBothEnablesAreLow() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(
        containsIgnoringCase(
            hdl, "Y1 <= NOT A1 WHEN nOE1 = '0' AND nOE2 = '0' ELSE 'Z';"));
    assertTrue(
        containsIgnoringCase(
            hdl, "Y8 <= NOT A8 WHEN nOE1 = '0' AND nOE2 = '0' ELSE 'Z';"));
  }

  @Test
  void verilogInvertsEachChannelWhileBothEnablesAreLow() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign Y1 = (nOE1 == 0 && nOE2 == 0) ? ~A1 : 1'bZ;"));
    assertTrue(hdl.contains("assign Y8 = (nOE1 == 0 && nOE2 == 0) ? ~A8 : 1'bZ;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74540HdlGenerator();
    final var attrs = new Ttl74540().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74540().createAttributeSet();
    return String.join(
        "\n", new Ttl74540HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
