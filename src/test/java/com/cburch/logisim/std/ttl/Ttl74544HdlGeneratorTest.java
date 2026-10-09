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

/** HDL text for the 74HC544 octal inverting registered transceiver. */
class Ttl74544HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlLatchesWhileBothEnablesAreLowAndInvertsTheDrivenBus() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "IF (nEAB = '0' AND nLEAB = '0' AND NOT(nEBA = '0' AND nOEBA = '0')) THEN"));
    assertTrue(containsIgnoringCase(hdl, "stateAB <= NOT(A7&A6&A5&A4&A3&A2&A1&A0);"));
    assertTrue(containsIgnoringCase(hdl, "IF (nEBA = '0' AND nLEBA = '0' AND NOT(nEAB = '0' AND nOEAB = '0')) THEN"));
    assertTrue(containsIgnoringCase(hdl, "stateBA <= NOT(B7&B6&B5&B4&B3&B2&B1&B0);"));
    assertTrue(containsIgnoringCase(hdl, "B0 <= NOT stateAB(0) WHEN nEAB = '0' AND nOEAB = '0' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "B7 <= NOT stateAB(7) WHEN nEAB = '0' AND nOEAB = '0' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "A0 <= NOT stateBA(0) WHEN nEBA = '0' AND nOEBA = '0' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "A7 <= NOT stateBA(7) WHEN nEBA = '0' AND nOEBA = '0' ELSE 'Z';"));
  }

  @Test
  void verilogLatchesWhileBothEnablesAreLowAndInvertsTheDrivenBus() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("always @(*)"));
    assertTrue(hdl.contains("if (nEAB == 0 && nLEAB == 0 && !(nEBA == 0 && nOEBA == 0))"));
    assertTrue(hdl.contains("stateAB <= ~{A7, A6, A5, A4, A3, A2, A1, A0};"));
    assertTrue(hdl.contains("if (nEBA == 0 && nLEBA == 0 && !(nEAB == 0 && nOEAB == 0))"));
    assertTrue(hdl.contains("stateBA <= ~{B7, B6, B5, B4, B3, B2, B1, B0};"));
    assertTrue(hdl.contains("assign B0 = (nEAB == 0 && nOEAB == 0) ? ~stateAB[0] : 1'bz;"));
    assertTrue(hdl.contains("assign B7 = (nEAB == 0 && nOEAB == 0) ? ~stateAB[7] : 1'bz;"));
    assertTrue(hdl.contains("assign A0 = (nEBA == 0 && nOEBA == 0) ? ~stateBA[0] : 1'bz;"));
    assertTrue(hdl.contains("assign A7 = (nEBA == 0 && nOEBA == 0) ? ~stateBA[7] : 1'bz;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74544HdlGenerator();
    final var attrs = new Ttl74544().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74544().createAttributeSet();
    return String.join(
        "\n", new Ttl74544HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
