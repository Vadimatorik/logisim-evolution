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

/** HDL text for the 74HC543 octal registered transceiver. */
class Ttl74543HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlOpensEachLatchWhileItsEnablesAreLow() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "IF (nCEAB = '0' AND nLEAB = '0') THEN"));
    assertTrue(containsIgnoringCase(hdl, "regAB <= A7&A6&A5&A4&A3&A2&A1&A0;"));
    assertTrue(containsIgnoringCase(hdl, "IF (nCEBA = '0' AND nLEBA = '0') THEN"));
    assertTrue(containsIgnoringCase(hdl, "regBA <= B7&B6&B5&B4&B3&B2&B1&B0;"));
    assertFalse(containsIgnoringCase(hdl, "rising_edge"));
  }

  @Test
  void vhdlDrivesEachBusFromItsLatch() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(
        containsIgnoringCase(
            hdl, "B0 <= regAB(0) WHEN nCEAB = '0' AND nOEAB = '0' ELSE 'Z';"));
    assertTrue(
        containsIgnoringCase(
            hdl, "B7 <= regAB(7) WHEN nCEAB = '0' AND nOEAB = '0' ELSE 'Z';"));
    assertTrue(
        containsIgnoringCase(
            hdl, "A0 <= regBA(0) WHEN nCEBA = '0' AND nOEBA = '0' ELSE 'Z';"));
    assertTrue(
        containsIgnoringCase(
            hdl, "A7 <= regBA(7) WHEN nCEBA = '0' AND nOEBA = '0' ELSE 'Z';"));
  }

  @Test
  void verilogOpensEachLatchWhileItsEnablesAreLow() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("always @(*)"));
    assertTrue(hdl.contains("if (nCEAB == 0 && nLEAB == 0) regAB <= {A7, A6, A5, A4, A3, A2, A1, A0};"));
    assertTrue(hdl.contains("if (nCEBA == 0 && nLEBA == 0) regBA <= {B7, B6, B5, B4, B3, B2, B1, B0};"));
    assertFalse(hdl.contains("posedge"));
  }

  @Test
  void verilogDrivesEachBusFromItsLatch() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign B0 = (nCEAB == 0 && nOEAB == 0) ? regAB[0] : 1'bz;"));
    assertTrue(hdl.contains("assign B7 = (nCEAB == 0 && nOEAB == 0) ? regAB[7] : 1'bz;"));
    assertTrue(hdl.contains("assign A0 = (nCEBA == 0 && nOEBA == 0) ? regBA[0] : 1'bz;"));
    assertTrue(hdl.contains("assign A7 = (nCEBA == 0 && nOEBA == 0) ? regBA[7] : 1'bz;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74543HdlGenerator();
    final var attrs = new Ttl74543().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74543().createAttributeSet();
    return String.join(
        "\n", new Ttl74543HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
