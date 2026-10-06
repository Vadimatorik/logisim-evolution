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

/** HDL text for the 74HC279 quad S-R latch. */
class Ttl74279HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlGivesSetPriorityOverReset() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "Q1 <= state(0);"));
    assertTrue(containsIgnoringCase(hdl, "Q4 <= state(3);"));
    assertTrue(containsIgnoringCase(hdl, "IF (S1A = '0' OR S1B = '0') THEN state(0) <= '1';"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (R1 = '0') THEN state(0) <= '0';"));
    assertTrue(containsIgnoringCase(hdl, "IF (S2 = '0') THEN state(1) <= '1';"));
    assertTrue(containsIgnoringCase(hdl, "IF (S3A = '0' OR S3B = '0') THEN state(2) <= '1';"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (R4 = '0') THEN state(3) <= '0';"));
    assertFalse(containsIgnoringCase(hdl, "rising_edge"));
  }

  @Test
  void verilogGivesSetPriorityOverReset() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign Q1 = state[0];"));
    assertTrue(hdl.contains("assign Q4 = state[3];"));
    assertTrue(hdl.contains("always @(*)"));
    assertTrue(hdl.contains("if (S1A == 0 || S1B == 0) state[0] = 1'b1;"));
    assertTrue(hdl.contains("else if (R1 == 0) state[0] = 1'b0;"));
    assertTrue(hdl.contains("if (S4 == 0) state[3] = 1'b1;"));
    assertTrue(hdl.contains("else if (R4 == 0) state[3] = 1'b0;"));
    assertFalse(hdl.contains("posedge"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74279HdlGenerator();
    final var attrs = new Ttl74279().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74279().createAttributeSet();
    return String.join("\n", new Ttl74279HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
