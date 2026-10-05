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

/** HDL text for the 7445 BCD-to-decimal decoder/driver. */
class Ttl7445HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlSelectsOneDecimalOutputAndLeavesTheOthersHigh() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "O0 <= NOT (NOT D AND NOT C AND NOT B AND NOT A);"));
    assertTrue(containsIgnoringCase(hdl, "O2 <= NOT (NOT D AND NOT C AND B AND NOT A);"));
    assertTrue(containsIgnoringCase(hdl, "O9 <= NOT (D AND NOT C AND NOT B AND A);"));
    assertFalse(containsIgnoringCase(hdl, "O10"));
  }

  @Test
  void verilogSelectsOneDecimalOutputAndLeavesTheOthersHigh() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign O0 = ~( ~D & ~C & ~B & ~A );"));
    assertTrue(hdl.contains("assign O2 = ~( ~D & ~C & B & ~A );"));
    assertTrue(hdl.contains("assign O9 = ~( D & ~C & ~B & A );"));
    assertFalse(hdl.contains("O10"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl7445HdlGenerator();
    final var attrs = new Ttl7445().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl7445().createAttributeSet();
    return String.join(
        "\n", new Ttl7445HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
