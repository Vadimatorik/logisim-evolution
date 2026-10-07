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

/** HDL text for the 74HC242 quad inverting bus transceiver. */
class Ttl74242HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlInvertsTheSelectedBusAndReleasesTheOther() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "B0 <= NOT A0 WHEN OEA = '0' AND OEB = '0' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "B3 <= NOT A3 WHEN OEA = '0' AND OEB = '0' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "A0 <= NOT B0 WHEN OEA = '1' AND OEB = '1' ELSE 'Z';"));
    assertTrue(containsIgnoringCase(hdl, "A3 <= NOT B3 WHEN OEA = '1' AND OEB = '1' ELSE 'Z';"));
  }

  @Test
  void verilogInvertsTheSelectedBusAndReleasesTheOther() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign B0 = (OEA == 0 && OEB == 0) ? ~A0 : 1'bz;"));
    assertTrue(hdl.contains("assign B3 = (OEA == 0 && OEB == 0) ? ~A3 : 1'bz;"));
    assertTrue(hdl.contains("assign A0 = (OEA == 1 && OEB == 1) ? ~B0 : 1'bz;"));
    assertTrue(hdl.contains("assign A3 = (OEA == 1 && OEB == 1) ? ~B3 : 1'bz;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74242HdlGenerator();
    final var attrs = new Ttl74242().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74242().createAttributeSet();
    return String.join(
        "\n", new Ttl74242HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
