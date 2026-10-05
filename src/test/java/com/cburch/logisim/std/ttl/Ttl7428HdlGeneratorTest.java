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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.prefs.AppPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class Ttl7428HdlGeneratorTest {
  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlDescribesEachGateAsANor() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "gateO0 <=  NOT (gateA0 OR gateB0);"));
    assertTrue(containsIgnoringCase(hdl, "gateO2 <=  NOT (gateA2 OR gateB2);"));
    assertTrue(containsIgnoringCase(hdl, "gateO3 <=  NOT (gateA3 OR gateB3);"));
  }

  @Test
  void verilogDescribesEachGateAsANor() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign gateO0 = ~(gateA0|gateB0);"));
    assertTrue(hdl.contains("assign gateO1 = ~(gateA1|gateB1);"));
    assertTrue(hdl.contains("assign gateO2 = ~(gateA2|gateB2);"));
  }

  @Test
  void hdlPortsFollowThe7402Package() {
    final var generator = new Ttl7428HdlGenerator();

    assertEquals(Ttl7428.PORT_1Y, generator.logicalPort("gateO0"));
    assertEquals(Ttl7428.PORT_1A, generator.logicalPort("gateA0"));
    assertEquals(Ttl7428.PORT_1B, generator.logicalPort("gateB0"));
    assertEquals(Ttl7428.PORT_3Y, generator.logicalPort("gateO2"));
    assertEquals(Ttl7428.PORT_3A, generator.logicalPort("gateA2"));
    assertEquals(Ttl7428.PORT_3B, generator.logicalPort("gateB2"));
    assertEquals(Ttl7428.PORT_4Y, generator.logicalPort("gateO3"));
    assertEquals(Ttl7428.PORT_4A, generator.logicalPort("gateA3"));
    assertEquals(Ttl7428.PORT_4B, generator.logicalPort("gateB3"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl7428HdlGenerator();
    final var component = new Ttl7428();
    final var attrs = component.createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));
    assertTrue(component.getHDLGenerator(attrs) instanceof Ttl7428HdlGenerator);

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));
    assertNull(component.getHDLGenerator(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl7428().createAttributeSet();
    return String.join(
        "\n", new Ttl7428HdlGenerator().getModuleFunctionality(null, attrs).get());
  }
}
