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

/** HDL text for the 74LVC1G74 single D flip-flop. */
class Ttl741G74HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlGivesSetAndResetPriorityOverTheRisingClock() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "Q  <= s_state(0);"));
    assertTrue(containsIgnoringCase(hdl, "nQ <= s_state(1);"));
    assertTrue(containsIgnoringCase(hdl, "s_next <= s_state WHEN tick = '0' ELSE (NOT D) & D;"));
    assertTrue(containsIgnoringCase(hdl, "IF ((SD = '0') AND (RD = '0')) THEN s_state <= \"11\";"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (SD = '0') THEN s_state <= \"01\";"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (RD = '0') THEN s_state <= \"10\";"));
    assertTrue(containsIgnoringCase(hdl, "ELSIF (rising_edge(clock)) THEN s_state <= s_next;"));
  }

  @Test
  void verilogGivesSetAndResetPriorityOverTheRisingClock() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign Q      = s_state[0];"));
    assertTrue(hdl.contains("assign nQ     = s_state[1];"));
    assertTrue(hdl.contains("assign s_next = (tick == 0) ? s_state : {~D, D};"));
    assertTrue(hdl.contains("always @(posedge clock or negedge SD or posedge SD or negedge RD or posedge RD)"));
    assertTrue(hdl.contains("if ((SD == 0) && (RD == 0)) s_state <= 2'b11;"));
    assertTrue(hdl.contains("else if (SD == 0) s_state <= 2'b01;"));
    assertTrue(hdl.contains("else if (RD == 0) s_state <= 2'b10;"));
    assertTrue(hdl.contains("else if (tick == 1) s_state <= s_next;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl741G74HdlGenerator();
    final var attrs = new Ttl741G74().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl741G74().createAttributeSet();
    return String.join("\n", new Ttl741G74HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
