/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.prefs.AppPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** HDL text for the 74HC83 4-bit binary full adder. */
class Ttl7483HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlAddsBothOperandsAndTheCarryInput() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(hdl.contains("oppA   <= \"0\"&A4&A3&A2&A1;"));
    assertTrue(hdl.contains("oppB   <= \"0\"&B4&B3&B2&B1;"));
    assertTrue(hdl.contains("oppC   <= \"0000\"&C0;"));
    assertTrue(
        hdl.contains(
            "result <= std_logic_vector(unsigned(oppA)+unsigned(oppB)+unsigned(oppC));"));
    assertTrue(hdl.contains("S1     <= result(0);"));
    assertTrue(hdl.contains("S2     <= result(1);"));
    assertTrue(hdl.contains("S3     <= result(2);"));
    assertTrue(hdl.contains("S4     <= result(3);"));
    assertTrue(hdl.contains("C4     <= result(4);"));
  }

  @Test
  void verilogAddsBothOperandsAndTheCarryInput() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign oppA   = {1'b0, A4, A3, A2, A1};"));
    assertTrue(hdl.contains("assign oppB   = {1'b0, B4, B3, B2, B1};"));
    assertTrue(hdl.contains("assign oppC   = {4'b0, C0};"));
    assertTrue(hdl.contains("assign result = oppA + oppB + oppC;"));
    assertTrue(hdl.contains("assign S1     = result[0];"));
    assertTrue(hdl.contains("assign S2     = result[1];"));
    assertTrue(hdl.contains("assign S3     = result[2];"));
    assertTrue(hdl.contains("assign S4     = result[3];"));
    assertTrue(hdl.contains("assign C4     = result[4];"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl7483HdlGenerator();
    final var attrs = new Ttl7483().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl7483().createAttributeSet();
    return String.join("\n", new Ttl7483HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
