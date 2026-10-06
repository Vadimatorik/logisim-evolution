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

class Ttl74260HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlNorsEachGroupOfFiveInputs() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "Y1 <= "));
    assertTrue(containsIgnoringCase(hdl, "NOT (A1 OR B1 OR C1 OR D1 OR E1);"));
    assertTrue(containsIgnoringCase(hdl, "Y2 <= "));
    assertTrue(containsIgnoringCase(hdl, "NOT (A2 OR B2 OR C2 OR D2 OR E2);"));
  }

  @Test
  void verilogNorsEachGroupOfFiveInputs() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign Y1 = ~(A1|B1|C1|D1|E1);"));
    assertTrue(hdl.contains("assign Y2 = ~(A2|B2|C2|D2|E2);"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74260HdlGenerator();
    final var attrs = new Ttl74260().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74260().createAttributeSet();
    return String.join(
        "\n", new Ttl74260HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
