/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.prefs.AppPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** HDL text tests for the 744050 hex non-inverting buffer. */
class Ttl744050HdlGeneratorTest {
  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlCopiesEachInputToItsOutput() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(hdl.contains("Y1 <= A1;"));
    assertTrue(hdl.contains("Y2 <= A2;"));
    assertTrue(hdl.contains("Y3 <= A3;"));
    assertTrue(hdl.contains("Y4 <= A4;"));
    assertTrue(hdl.contains("Y5 <= A5;"));
    assertTrue(hdl.contains("Y6 <= A6;"));
  }

  @Test
  void verilogCopiesEachInputToItsOutput() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign Y1 = A1;"));
    assertTrue(hdl.contains("assign Y2 = A2;"));
    assertTrue(hdl.contains("assign Y3 = A3;"));
    assertTrue(hdl.contains("assign Y4 = A4;"));
    assertTrue(hdl.contains("assign Y5 = A5;"));
    assertTrue(hdl.contains("assign Y6 = A6;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl744050HdlGenerator();
    final var attrs = new Ttl744050().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl744050().createAttributeSet();
    return String.join(
        "\n", new Ttl744050HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
