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

/** HDL text for the 74x09 open-drain AND gate. */
class Ttl7409HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlDrivesLowOrReleasesEachOutput() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    for (var gate = 0; gate < 4; gate++) {
      assertTrue(
          containsIgnoringCase(
              hdl,
              "gateO"
                  + gate
                  + " <= '0' when (gateA"
                  + gate
                  + " and gateB"
                  + gate
                  + ") = '0' else 'Z';"));
    }
  }

  @Test
  void verilogDrivesLowOrReleasesEachOutput() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    for (var gate = 0; gate < 4; gate++) {
      assertTrue(
          hdl.contains(
              "assign gateO"
                  + gate
                  + " = (gateA"
                  + gate
                  + " & gateB"
                  + gate
                  + ") ? 1'bZ : 1'b0;"));
    }
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl7409.OpenDrainAndHdlGenerator();
    final var attrs = new Ttl7409().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl7409().createAttributeSet();
    return String.join(
        "\n", new Ttl7409.OpenDrainAndHdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
