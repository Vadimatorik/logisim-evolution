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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.prefs.AppPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** HDL for the 74HC7014 is six non-inverting buffer assignments. */
class Ttl747014HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlCopiesEachInputToItsOutput() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    for (var gate = 0; gate < 6; gate++) {
      assertTrue(hdl.contains("gateO" + gate + " <= gateA" + gate + ";"));
    }
    assertFalse(hdl.toLowerCase().contains("not"));
  }

  @Test
  void verilogCopiesEachInputToItsOutput() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    for (var gate = 0; gate < 6; gate++) {
      assertTrue(hdl.contains("assign gateO" + gate + " = gateA" + gate + ";"));
    }
    assertFalse(hdl.contains("~"));
    assertFalse(hdl.toLowerCase().contains("not"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var buffer = new Ttl747014();
    final var attrs = buffer.createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(buffer.isHDLSupportedComponent(attrs));
    assertNotNull(buffer.getHDLGenerator(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(buffer.isHDLSupportedComponent(attrs));
    assertNull(buffer.getHDLGenerator(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertFalse(buffer.getHDLGenerator(attrs).isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var buffer = new Ttl747014();
    final var attrs = buffer.createAttributeSet();
    attrs.setValue(TtlLibrary.VCC_GND, false);
    final var generator = (AbstractHdlGeneratorFactory) buffer.getHDLGenerator(attrs);
    return String.join("\n", generator.getModuleFunctionality(null, attrs).get());
  }
}
