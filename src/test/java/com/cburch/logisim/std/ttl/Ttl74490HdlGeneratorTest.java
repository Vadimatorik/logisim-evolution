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

/** HDL text for the 74HC490 dual decade counter. */
class Ttl74490HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlCountsOnTheFallingClockAndLetsSetToNineOverrideClear() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "s_next1 <= \"0001\" WHEN s_count1 = \"0000\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "\"1001\" WHEN s_count1 = \"1000\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "\"0000\";"));
    assertTrue(containsIgnoringCase(hdl, "s_next2 <= \"0001\" WHEN s_count2 = \"0000\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "IF (SET91 = '1') THEN s_count1 <= \"1001\";"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (CLR1 = '1') THEN s_count1 <= \"0000\";"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (falling_edge(clock)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (tick = '1') THEN s_count1 <= s_next1; END IF;"));
    assertTrue(containsIgnoringCase(hdl, "IF (SET92 = '1') THEN s_count2 <= \"1001\";"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (falling_edge(clock2)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (tick2 = '1') THEN s_count2 <= s_next2; END IF;"));
  }

  @Test
  void verilogCountsOnTheFallingClockAndLetsSetToNineOverrideClear() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign s_next1 = (s_count1 == 4'b0000) ? 4'b0001 :"));
    assertTrue(hdl.contains("(s_count1 == 4'b1000) ? 4'b1001 :"));
    assertTrue(hdl.contains("assign s_next2 = (s_count2 == 4'b0000) ? 4'b0001 :"));
    assertTrue(hdl.contains("4'b0000;"));
    assertTrue(hdl.contains("always @(negedge clock or posedge SET91 or posedge CLR1)"));
    assertTrue(hdl.contains("if (SET91 == 1) s_count1 <= 4'b1001;"));
    assertTrue(hdl.contains("else if (CLR1 == 1) s_count1 <= 4'b0000;"));
    assertTrue(hdl.contains("else if (tick == 1) s_count1 <= s_next1;"));
    assertTrue(hdl.contains("always @(negedge clock2 or posedge SET92 or posedge CLR2)"));
    assertTrue(hdl.contains("if (SET92 == 1) s_count2 <= 4'b1001;"));
    assertTrue(hdl.contains("else if (tick2 == 1) s_count2 <= s_next2;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74490HdlGenerator();
    final var attrs = new Ttl74490().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74490().createAttributeSet();
    return String.join("\n", new Ttl74490HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
