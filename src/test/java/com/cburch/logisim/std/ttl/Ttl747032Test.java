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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.prefs.AppPreferences;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class Ttl747032Test {
  private static final int[] INPUTS = {
    Ttl747032.PORT_1A, Ttl747032.PORT_1B,
    Ttl747032.PORT_2A, Ttl747032.PORT_2B,
    Ttl747032.PORT_3A, Ttl747032.PORT_3B,
    Ttl747032.PORT_4A, Ttl747032.PORT_4B
  };

  private static final int[][] GATES = {
    {Ttl747032.PORT_1A, Ttl747032.PORT_1B, Ttl747032.PORT_1Y},
    {Ttl747032.PORT_2A, Ttl747032.PORT_2B, Ttl747032.PORT_2Y},
    {Ttl747032.PORT_3A, Ttl747032.PORT_3B, Ttl747032.PORT_3Y},
    {Ttl747032.PORT_4A, Ttl747032.PORT_4B, Ttl747032.PORT_4Y}
  };

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void usesThe7432PinoutAndKeepsPowerPortsLast() {
    final var gate = new Ttl747032();
    final var hiddenPower = TtlTestInstanceState.createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl747032.PORT_1A, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747032.PORT_1B, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747032.PORT_1Y, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl747032.PORT_2A, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747032.PORT_2B, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747032.PORT_2Y, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl747032.PORT_3Y, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl747032.PORT_3A, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747032.PORT_3B, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747032.PORT_4Y, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl747032.PORT_4A, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747032.PORT_4B, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = TtlTestInstanceState.createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertPort(shownPower, Ttl747032.PORT_GND, 130, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, Ttl747032.PORT_VCC, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void eachGateFollowsTheOrFunctionTable() {
    final var gate = new Ttl747032();
    final var state = new TtlTestInstanceState(gate, false);
    final var levels = new Value[] {Value.FALSE, Value.TRUE};

    for (var channel = 0; channel < GATES.length; channel++) {
      for (var left : levels) {
        for (var right : levels) {
          drive(state, Value.FALSE);
          state.setPortValue(GATES[channel][0], left);
          state.setPortValue(GATES[channel][1], right);
          gate.propagate(state);

          final var expected = left == Value.TRUE || right == Value.TRUE ? Value.TRUE : Value.FALSE;
          assertEquals(expected, state.getPortValue(GATES[channel][2]));
          for (var other = 0; other < GATES.length; other++) {
            if (other != channel) assertEquals(Value.FALSE, state.getPortValue(GATES[other][2]));
          }
        }
      }
    }
  }

  @Test
  void highInputMasksUnknownAndError() {
    final var gate = new Ttl747032();
    final var state = new TtlTestInstanceState(gate, false);

    drive(state, Value.FALSE);
    state.setPortValue(Ttl747032.PORT_1A, Value.TRUE);
    state.setPortValue(Ttl747032.PORT_1B, Value.UNKNOWN);
    state.setPortValue(Ttl747032.PORT_2A, Value.ERROR);
    state.setPortValue(Ttl747032.PORT_2B, Value.TRUE);
    gate.propagate(state);

    assertEquals(Value.TRUE, state.getPortValue(Ttl747032.PORT_1Y));
    assertEquals(Value.TRUE, state.getPortValue(Ttl747032.PORT_2Y));
  }

  @Test
  void lowInputPreservesUnknownAndError() {
    final var gate = new Ttl747032();
    final var state = new TtlTestInstanceState(gate, false);

    drive(state, Value.FALSE);
    state.setPortValue(Ttl747032.PORT_1B, Value.UNKNOWN);
    state.setPortValue(Ttl747032.PORT_2B, Value.ERROR);
    state.setPortValue(Ttl747032.PORT_3A, Value.UNKNOWN);
    state.setPortValue(Ttl747032.PORT_3B, Value.UNKNOWN);
    state.setPortValue(Ttl747032.PORT_4A, Value.ERROR);
    state.setPortValue(Ttl747032.PORT_4B, Value.ERROR);
    gate.propagate(state);

    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl747032.PORT_1Y));
    assertEquals(Value.ERROR, state.getPortValue(Ttl747032.PORT_2Y));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl747032.PORT_3Y));
    assertEquals(Value.ERROR, state.getPortValue(Ttl747032.PORT_4Y));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl747032();
    final var state = new TtlTestInstanceState(gate, true);
    drive(state, Value.FALSE);
    state.setPortValue(Ttl747032.PORT_1A, Value.TRUE);
    state.setPortValue(Ttl747032.PORT_GND, Value.FALSE);
    state.setPortValue(Ttl747032.PORT_VCC, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.TRUE, state.getPortValue(Ttl747032.PORT_1Y));

    state.setPortValue(Ttl747032.PORT_VCC, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(Ttl747032.PORT_VCC, Value.TRUE);
    state.setPortValue(Ttl747032.PORT_GND, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(Ttl747032.PORT_GND, Value.FALSE);
    state.setPortValue(Ttl747032.PORT_VCC, Value.UNKNOWN);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  @Test
  void hdlIsAnOrOfEachGateUntilPowerPinsAreShown() {
    final var gate = new Ttl747032();
    final var attrs = gate.createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(gate.isHDLSupportedComponent(attrs));
    final var vhdl = functionality(gate, HdlGeneratorFactory.VHDL).toLowerCase(Locale.ROOT);
    final var verilog = functionality(gate, HdlGeneratorFactory.VERILOG);
    for (var index = 0; index < 4; index++) {
      assertTrue(vhdl.contains("gateo" + index + " <= gatea" + index + " or gateb" + index + ";"));
      assertTrue(verilog.contains("assign gateO" + index + " = gateA" + index + "|gateB" + index + ";"));
    }

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(gate.isHDLSupportedComponent(attrs));
    assertNull(gate.getHDLGenerator(attrs));
    assertFalse(gate.isHDLSupportedComponent(null));
    assertNull(gate.getHDLGenerator(null));
  }

  private static String functionality(Ttl747032 gate, String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = gate.createAttributeSet();
    final var generator = (AbstractHdlGeneratorFactory) gate.getHDLGenerator(attrs);
    return String.join("\n", generator.getModuleFunctionality(null, attrs).get());
  }

  private static void drive(TtlTestInstanceState state, Value value) {
    for (var input : INPUTS) state.setPortValue(input, value);
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl747032.PORT_1Y));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl747032.PORT_2Y));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl747032.PORT_3Y));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl747032.PORT_4Y));
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }
}
