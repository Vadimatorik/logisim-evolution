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

/** HDL text for the 74256 dual 4-bit addressable latch. */
class Ttl74256HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlClearsResetsAndFollowsEachAddressedLatch() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "Q0a <= state(0);"));
    assertTrue(containsIgnoringCase(hdl, "Q3b <= state(7);"));
    assertTrue(containsIgnoringCase(hdl, "IF (nCL = '0' AND nE = '1') THEN"));
    assertTrue(containsIgnoringCase(hdl, "state <= \"00000000\";"));
    assertTrue(
        containsIgnoringCase(
            hdl,
            "IF (A1 = '0' AND A0 = '0') THEN state(0) <= Da; ELSE state(0) <= '0'; END IF;"));
    assertTrue(
        containsIgnoringCase(
            hdl,
            "IF (A1 = '1' AND A0 = '1') THEN state(7) <= Db; ELSE state(7) <= '0'; END IF;"));
    assertTrue(
        containsIgnoringCase(hdl, "IF (A1 = '1' AND A0 = '1') THEN state(7) <= Db; END IF;"));
    assertFalse(containsIgnoringCase(hdl, "rising_edge"));
  }

  @Test
  void verilogClearsResetsAndFollowsEachAddressedLatch() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign Q0a = state[0];"));
    assertTrue(hdl.contains("assign Q3b = state[7];"));
    assertTrue(hdl.contains("always @(*)"));
    assertTrue(hdl.contains("if (nCL == 0 && nE == 1) state = 8'b00000000;"));
    assertTrue(hdl.contains("state[0] = (A1 == 0 && A0 == 0) ? Da : 1'b0;"));
    assertTrue(hdl.contains("state[7] = (A1 == 1 && A0 == 1) ? Db : 1'b0;"));
    assertTrue(hdl.contains("if (A1 == 1 && A0 == 1) state[7] = Db;"));
    assertFalse(hdl.contains("posedge"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74256HdlGenerator();
    final var attrs = new Ttl74256().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74256().createAttributeSet();
    return String.join("\n", new Ttl74256HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
