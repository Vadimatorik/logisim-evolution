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

/** HDL text for the 74177 presettable binary counter/latch. */
class Ttl74177HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlClearsLoadsAndCountsOnFallingClocks() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "s_next8 <= \"001\" WHEN s_div8 = \"000\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "\"111\" WHEN s_div8 = \"110\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "\"000\";"));
    assertTrue(containsIgnoringCase(hdl, "IF (nCLR = '0') THEN s_qa <= '0';"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (nLOAD = '0') THEN s_qa <= A;"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (falling_edge(clock)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (tick = '1') THEN s_qa <= NOT s_qa;"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (nLOAD = '0') THEN s_div8 <= D & C & B;"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (falling_edge(clock2)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (tick2 = '1') THEN s_div8 <= s_next8;"));
    assertFalse(containsIgnoringCase(hdl, "rising_edge"));
  }

  @Test
  void verilogClearsLoadsAndCountsOnFallingClocks() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("(s_div8 == 3'b000) ? 3'b001 :"));
    assertTrue(hdl.contains("(s_div8 == 3'b110) ? 3'b111 : 3'b000;"));
    assertTrue(hdl.contains("always @(negedge clock or negedge nCLR or nLOAD or A)"));
    assertTrue(hdl.contains("if (nCLR == 0) s_qa <= 0;"));
    assertTrue(hdl.contains("else if (nLOAD == 0) s_qa <= A;"));
    assertTrue(hdl.contains("else if (tick == 1) s_qa <= ~s_qa;"));
    assertTrue(hdl.contains("always @(negedge clock2 or negedge nCLR or nLOAD or B or C or D)"));
    assertTrue(hdl.contains("else if (nLOAD == 0) s_div8 <= {D, C, B};"));
    assertTrue(hdl.contains("else if (tick2 == 1) s_div8 <= s_next8;"));
    assertFalse(hdl.contains("posedge"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74177HdlGenerator();
    final var attrs = new Ttl74177().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74177().createAttributeSet();
    return String.join("\n", new Ttl74177HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
