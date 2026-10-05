/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.prefs.AppPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class Ttl747002HdlGeneratorTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlNorUsesThe7402PortOrder() {
    final var generator = generator();
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertEquals(1, generator.logicalPort("gateA0"));
    assertEquals(2, generator.logicalPort("gateB0"));
    assertEquals(0, generator.logicalPort("gateO0"));
    assertEquals(9, generator.logicalPort("gateA3"));
    assertEquals(10, generator.logicalPort("gateB3"));
    assertEquals(11, generator.logicalPort("gateO3"));
    assertTrue(hdl.contains("gateO0 <=  NOT (gateA0 OR gateB0);"));
    assertTrue(hdl.contains("gateO3 <=  NOT (gateA3 OR gateB3);"));
  }

  @Test
  void verilogNorUsesThe7402PortOrder() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign gateO0 = ~(gateA0|gateB0);"));
    assertTrue(hdl.contains("assign gateO1 = ~(gateA1|gateB1);"));
    assertTrue(hdl.contains("assign gateO2 = ~(gateA2|gateB2);"));
    assertTrue(hdl.contains("assign gateO3 = ~(gateA3|gateB3);"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var gate = new Ttl747002();
    final var generator = generator();
    final var attrs = gate.createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));
    assertInstanceOf(Ttl747002HdlGenerator.class, gate.getHDLGenerator(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));
    assertNull(gate.getHDLGenerator(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static Ttl747002HdlGenerator generator() {
    return new Ttl747002HdlGenerator();
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl747002().createAttributeSet();
    return String.join("\n", generator().getModuleFunctionality(null, attrs).get());
  }
}
