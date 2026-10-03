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

/** HDL text for the 74HC160 synchronous BCD decade counter. */
class Ttl74160HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlClearsLoadsAndCountsOnTheRisingClock() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "Q0 <= curState(0);"));
    assertTrue(containsIgnoringCase(hdl, "Q3 <= curState(3);"));
    assertTrue(containsIgnoringCase(hdl, "IF (MR = '0') THEN curState <= \"0000\";"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (rising_edge(clock)) THEN"));
    assertTrue(containsIgnoringCase(hdl, "loaded WHEN tick = '1' AND PE = '0' ELSE"));
    assertTrue(containsIgnoringCase(hdl, "CEP = '1' AND CET = '1' ELSE"));
    assertTrue(
        containsIgnoringCase(
            hdl, "TC <= CET AND curState(0) AND NOT(curState(1))"));
    assertTrue(containsIgnoringCase(hdl, "curState(0) AND curState(3)"));
  }

  @Test
  void verilogClearsLoadsAndCountsOnTheRisingClock() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign Q0 = curState[0];"));
    assertTrue(hdl.contains("assign Q3 = curState[3];"));
    assertTrue(hdl.contains("assign TC = CET & curState[0] & ~curState[1] & ~curState[2] & curState[3];"));
    assertTrue(hdl.contains("assign loaded = {D3, D2, D1, D0};"));
    assertTrue(hdl.contains("(tick == 1 && PE == 0) ? loaded :"));
    assertTrue(hdl.contains("(tick == 1 && PE == 1 && CEP == 1 && CET == 1) ? counted :"));
    assertTrue(hdl.contains("always @(posedge clock or negedge MR)"));
    assertTrue(hdl.contains("if (MR == 0) curState <= 4'b0000;"));
    assertTrue(hdl.contains("curState[0] & curState[3]"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74160HdlGenerator();
    final var attrs = new Ttl74160().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74160().createAttributeSet();
    return String.join("\n", new Ttl74160HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
