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

class Ttl74375HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlFollowsDataWhileThePairEnableIsHigh() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "if (G12 = '1') then"));
    assertTrue(containsIgnoringCase(hdl, "s_q1 <= D1;"));
    assertTrue(containsIgnoringCase(hdl, "s_q2 <= D2;"));
    assertTrue(containsIgnoringCase(hdl, "if (G34 = '1') then"));
    assertTrue(containsIgnoringCase(hdl, "s_q3 <= D3;"));
    assertTrue(containsIgnoringCase(hdl, "s_q4 <= D4;"));
    assertTrue(containsIgnoringCase(hdl, "nQ1 <= not(s_q1);"));
    assertTrue(containsIgnoringCase(hdl, "Q4  <= s_q4;"));
  }

  @Test
  void verilogFollowsDataWhileThePairEnableIsHigh() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("if (G12 == 1) begin"));
    assertTrue(hdl.contains("s_q1 <= D1;"));
    assertTrue(hdl.contains("s_q2 <= D2;"));
    assertTrue(hdl.contains("if (G34 == 1) begin"));
    assertTrue(hdl.contains("s_q3 <= D3;"));
    assertTrue(hdl.contains("s_q4 <= D4;"));
    assertTrue(hdl.contains("assign nQ1 = ~s_q1;"));
    assertTrue(hdl.contains("assign Q4  = s_q4;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74375HdlGenerator();
    final var attrs = new Ttl74375().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74375().createAttributeSet();
    return String.join(
        "\n", new Ttl74375HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
