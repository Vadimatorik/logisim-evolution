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

/** HDL text for the 7417 open-collector buffers. */
class Ttl7417HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlReleasesOnHighAndSinksOnLow() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "gateO0 <= 'Z' WHEN gateA0 = '1' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "'0' WHEN gateA0 = '0' ELSE 'X';"));
    assertTrue(containsIgnoringCase(hdl, "gateO5 <= 'Z' WHEN gateA5 = '1' ELSE"));
  }

  @Test
  void verilogReleasesOnHighAndSinksOnLow() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign gateO0 = (gateA0 == 1) ? 1'bZ : (gateA0 == 0) ? 1'b0 : 1'bX;"));
    assertTrue(hdl.contains("assign gateO5 = (gateA5 == 1) ? 1'bZ : (gateA5 == 0) ? 1'b0 : 1'bX;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl7417HdlGenerator();
    final var attrs = new Ttl7417().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl7417().createAttributeSet();
    return String.join("\n", new Ttl7417HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
