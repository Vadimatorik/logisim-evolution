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

class Ttl74155HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlSelectsSectionOneOnlyWhenTheStrobeIsLowAndTheDataIsHigh() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(
        containsIgnoringCase(
            hdl, "Y1_0  <=  NOT( NOT(B) AND NOT(A) AND NOT(G1) AND C1 );"));
    assertTrue(
        containsIgnoringCase(hdl, "Y1_3  <=  NOT( B AND A AND NOT(G1) AND C1 );"));
  }

  @Test
  void vhdlSelectsSectionTwoOnlyWhenTheStrobeIsHighAndTheDataIsLow() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(
        containsIgnoringCase(
            hdl, "Y2_0  <=  NOT( NOT(B) AND NOT(A) AND G2 AND NOT(C2) );"));
    assertTrue(
        containsIgnoringCase(
            hdl, "Y2_2  <=  NOT( B AND NOT(A) AND G2 AND NOT(C2) );"));
  }

  @Test
  void verilogSelectsSectionOneOnlyWhenTheStrobeIsLowAndTheDataIsHigh() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign  Y1_0  =  ~( ~(B) & ~(A) & ~(G1) & C1 );"));
    assertTrue(hdl.contains("assign  Y1_3  =  ~( B & A & ~(G1) & C1 );"));
  }

  @Test
  void verilogSelectsSectionTwoOnlyWhenTheStrobeIsHighAndTheDataIsLow() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign  Y2_0  =  ~( ~(B) & ~(A) & G2 & ~(C2) );"));
    assertTrue(hdl.contains("assign  Y2_2  =  ~( B & ~(A) & G2 & ~(C2) );"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74155HdlGenerator();
    final var attrs = new Ttl74155().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74155().createAttributeSet();
    return String.join("\n", new Ttl74155HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
