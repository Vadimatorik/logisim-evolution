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

/** HDL text for the 74HC652 octal bus transceiver and registers. */
class Ttl74652HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlLoadsEachRegisterOnItsOwnRisingEdge() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "nextA <= A8&A7&A6&A5&A4&A3&A2&A1 WHEN tick = '1' ELSE regA;"));
    assertTrue(containsIgnoringCase(hdl, "nextB <= B8&B7&B6&B5&B4&B3&B2&B1 WHEN tick2 = '1' ELSE regB;"));
    assertTrue(containsIgnoringCase(hdl, "IF (rising_edge(clock)) THEN regA <= nextA;"));
    assertTrue(containsIgnoringCase(hdl, "IF (rising_edge(clock2)) THEN regB <= nextB;"));
  }

  @Test
  void vhdlDrivesEachBusFromTheLivePinOrTheRegister() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(
        containsIgnoringCase(
            hdl,
            "B1 <= A1 WHEN OEAB = '1' AND SAB = '0' ELSE regA(0) WHEN OEAB = '1' AND SAB = '1' ELSE 'Z';"));
    assertTrue(
        containsIgnoringCase(
            hdl,
            "B8 <= A8 WHEN OEAB = '1' AND SAB = '0' ELSE regA(7) WHEN OEAB = '1' AND SAB = '1' ELSE 'Z';"));
    assertTrue(
        containsIgnoringCase(
            hdl,
            "A1 <= B1 WHEN OEBA = '0' AND SBA = '0' ELSE regB(0) WHEN OEBA = '0' AND SBA = '1' ELSE 'Z';"));
    assertTrue(
        containsIgnoringCase(
            hdl,
            "A8 <= B8 WHEN OEBA = '0' AND SBA = '0' ELSE regB(7) WHEN OEBA = '0' AND SBA = '1' ELSE 'Z';"));
  }

  @Test
  void verilogLoadsEachRegisterOnItsOwnRisingEdge() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign nextA = (tick == 1) ? {A8, A7, A6, A5, A4, A3, A2, A1} : regA;"));
    assertTrue(hdl.contains("assign nextB = (tick2 == 1) ? {B8, B7, B6, B5, B4, B3, B2, B1} : regB;"));
    assertTrue(hdl.contains("always @(posedge clock)"));
    assertTrue(hdl.contains("always @(posedge clock2)"));
    assertTrue(hdl.contains("regA <= nextA;"));
    assertTrue(hdl.contains("regB <= nextB;"));
  }

  @Test
  void verilogDrivesEachBusFromTheLivePinOrTheRegister() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(
        hdl.contains(
            "assign B1 = (OEAB == 1 && SAB == 0) ? A1 : (OEAB == 1 && SAB == 1) ? regA[0] : 1'bz;"));
    assertTrue(
        hdl.contains(
            "assign B8 = (OEAB == 1 && SAB == 0) ? A8 : (OEAB == 1 && SAB == 1) ? regA[7] : 1'bz;"));
    assertTrue(
        hdl.contains(
            "assign A1 = (OEBA == 0 && SBA == 0) ? B1 : (OEBA == 0 && SBA == 1) ? regB[0] : 1'bz;"));
    assertTrue(
        hdl.contains(
            "assign A8 = (OEBA == 0 && SBA == 0) ? B8 : (OEBA == 0 && SBA == 1) ? regB[7] : 1'bz;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74652HdlGenerator();
    final var attrs = new Ttl74652().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74652().createAttributeSet();
    return String.join(
        "\n", new Ttl74652HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
