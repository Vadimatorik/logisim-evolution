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

class Ttl74280HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlXorsEveryDataInputAndInvertsTheOddParity() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(
        hdl, "s_odd <= I0 XOR I1 XOR I2 XOR I3 XOR I4 XOR I5 XOR I6 XOR I7 XOR I8;"));
    assertTrue(containsIgnoringCase(hdl, "PO    <= s_odd;"));
    assertTrue(containsIgnoringCase(hdl, "PE    <= NOT s_odd;"));
  }

  @Test
  void verilogXorsEveryDataInputAndInvertsTheOddParity() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign s_odd = I0 ^ I1 ^ I2 ^ I3 ^ I4 ^ I5 ^ I6 ^ I7 ^ I8;"));
    assertTrue(hdl.contains("assign PO    = s_odd;"));
    assertTrue(hdl.contains("assign PE    = ~s_odd;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74280HdlGenerator();
    final var attrs = new Ttl74280().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74280().createAttributeSet();
    return String.join(
        "\n", new Ttl74280HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
