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

/** HDL text for the 74HC590 counter with output register. */
class Ttl74590HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlClearsOnlyTheCounterAndStoresBeforeTheIncrement() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "s_next <= s_count WHEN tick = '0' ELSE"));
    assertTrue(
        containsIgnoringCase(
            hdl, "std_logic_vector(unsigned(s_count) + 1) WHEN CE = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "s_load <= s_count WHEN tick2 = '1' ELSE s_reg;"));
    assertTrue(containsIgnoringCase(hdl, "RCO <= '0' WHEN s_count = \"11111111\" ELSE '1';"));
    assertTrue(containsIgnoringCase(hdl, "Q0 <= s_reg(0) WHEN OE = '0' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "Q7 <= s_reg(7) WHEN OE = '0' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "IF (MRC = '0') THEN"));
    assertTrue(containsIgnoringCase(hdl, "s_count <= \"00000000\";"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (rising_edge(clock)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "IF (rising_edge(clock2)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "s_reg <= s_load;"));
    assertFalse(containsIgnoringCase(hdl, "s_reg <= \"00000000\""));
  }

  @Test
  void verilogClearsOnlyTheCounterAndStoresBeforeTheIncrement() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign s_next = (tick == 0) ? s_count : (CE == 0) ? (s_count + 1) : s_count;"));
    assertTrue(hdl.contains("assign s_load = (tick2 == 1) ? s_count : s_reg;"));
    assertTrue(hdl.contains("assign RCO = (s_count != 8'b11111111);"));
    assertTrue(hdl.contains("assign Q0 = (OE == 0) ? s_reg[0] : 1'bZ;"));
    assertTrue(hdl.contains("assign Q7 = (OE == 0) ? s_reg[7] : 1'bZ;"));
    assertTrue(hdl.contains("always @(posedge clock or negedge MRC)"));
    assertTrue(hdl.contains("if (MRC == 0) s_count <= 8'b00000000;"));
    assertTrue(hdl.contains("always @(posedge clock2)"));
    assertTrue(hdl.contains("s_reg <= s_load;"));
    assertFalse(hdl.contains("negedge CE"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74590HdlGenerator();
    final var attrs = new Ttl74590().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74590().createAttributeSet();
    return String.join("\n", new Ttl74590HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
