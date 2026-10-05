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

class Ttl74152HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlSelectsEachDataInputFromTheAddress() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "W <= D0 WHEN s_select = \"000\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "D3 WHEN s_select = \"011\" ELSE"));
    assertTrue(containsIgnoringCase(hdl, "D7;"));
    assertTrue(containsIgnoringCase(hdl, "s_select <= C & B & A;"));
  }

  @Test
  void verilogSelectsEachDataInputFromTheAddress() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign W = (s_select == 3'b000) ? D0 :"));
    assertTrue(hdl.contains("(s_select == 3'b011) ? D3 :"));
    assertTrue(hdl.contains("D7;"));
    assertTrue(hdl.contains("assign s_select = {C, B, A};"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74152HdlGenerator();
    final var attrs = new Ttl74152().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74152().createAttributeSet();
    return String.join(
        "\n", new Ttl74152HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
