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
import static com.cburch.logisim.std.ttl.TtlTestInstanceState.createInstance;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Tests for the TTL 747001 quad 2-input AND Schmitt trigger. */
class Ttl747001Test {
  private static final int[][] GATES = {
    {Ttl747001.PORT_INDEX_1A, Ttl747001.PORT_INDEX_1B, Ttl747001.PORT_INDEX_1Y},
    {Ttl747001.PORT_INDEX_2A, Ttl747001.PORT_INDEX_2B, Ttl747001.PORT_INDEX_2Y},
    {Ttl747001.PORT_INDEX_3A, Ttl747001.PORT_INDEX_3B, Ttl747001.PORT_INDEX_3Y},
    {Ttl747001.PORT_INDEX_4A, Ttl747001.PORT_INDEX_4B, Ttl747001.PORT_INDEX_4Y}
  };

  private static final int[] OUTPUTS = {
    Ttl747001.PORT_INDEX_1Y,
    Ttl747001.PORT_INDEX_2Y,
    Ttl747001.PORT_INDEX_3Y,
    Ttl747001.PORT_INDEX_4Y
  };

  private static final int GND_PORT = 12;
  private static final int VCC_PORT = 13;

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void usesPhysicalPinoutAndKeepsPowerPortsLast() {
    final var gate = new Ttl747001();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl747001.PORT_INDEX_1A, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747001.PORT_INDEX_1B, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747001.PORT_INDEX_1Y, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl747001.PORT_INDEX_2A, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747001.PORT_INDEX_2B, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747001.PORT_INDEX_2Y, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl747001.PORT_INDEX_3Y, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl747001.PORT_INDEX_3A, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747001.PORT_INDEX_3B, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747001.PORT_INDEX_4Y, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl747001.PORT_INDEX_4A, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl747001.PORT_INDEX_4B, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertPort(shownPower, Ttl747001.PORT_INDEX_1A, 10, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, Ttl747001.PORT_INDEX_4B, 30, -30, EndData.INPUT_ONLY);
    assertPort(shownPower, GND_PORT, 130, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void eachGateIsAndAndLeavesTheOthersLow() {
    final var gate = new Ttl747001();
    final var state = new TtlTestInstanceState(gate, false);

    for (final var pins : GATES) {
      for (final var inputA : new Value[] {Value.FALSE, Value.TRUE}) {
        for (final var inputB : new Value[] {Value.FALSE, Value.TRUE}) {
          driveAllLow(state);
          state.setPortValue(pins[0], inputA);
          state.setPortValue(pins[1], inputB);
          gate.propagate(state);
          final var bothHigh = inputA == Value.TRUE && inputB == Value.TRUE;
          assertEquals(bothHigh ? Value.TRUE : Value.FALSE, state.getPortValue(pins[2]));
          assertOtherOutputs(state, pins[2], Value.FALSE);
        }
      }
    }
  }

  @Test
  void lowInputForcesLowEvenWhenTheOtherInputIsUnsettled() {
    final var gate = new Ttl747001();
    final var state = new TtlTestInstanceState(gate, false);

    for (final var unsettled : new Value[] {Value.UNKNOWN, Value.ERROR}) {
      driveAllLow(state);
      state.setPortValue(Ttl747001.PORT_INDEX_1A, Value.FALSE);
      state.setPortValue(Ttl747001.PORT_INDEX_1B, unsettled);
      gate.propagate(state);
      assertEquals(Value.FALSE, state.getPortValue(Ttl747001.PORT_INDEX_1Y));
      assertOtherOutputs(state, Ttl747001.PORT_INDEX_1Y, Value.FALSE);

      driveAllLow(state);
      state.setPortValue(Ttl747001.PORT_INDEX_1A, Value.TRUE);
      state.setPortValue(Ttl747001.PORT_INDEX_1B, unsettled);
      gate.propagate(state);
      assertEquals(Value.ERROR, state.getPortValue(Ttl747001.PORT_INDEX_1Y));
      assertOtherOutputs(state, Ttl747001.PORT_INDEX_1Y, Value.FALSE);
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl747001();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    driveAllLow(state);
    gate.propagate(state);
    assertEquals(Value.FALSE, state.getPortValue(Ttl747001.PORT_INDEX_1Y));

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    driveAllLow(state);
    gate.propagate(state);
    assertEquals(Value.FALSE, state.getPortValue(Ttl747001.PORT_INDEX_1Y));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  @Test
  void hdlEmitsAndForEachGate() {
    for (final var index : new int[] {0, 1, 2, 3}) {
      final var vhdl = functionality(HdlGeneratorFactory.VHDL).replaceAll("\\s+", " ");
      assertTrue(
          containsIgnoringCase(
              vhdl, "gateO" + index + " <= gateA" + index + " AND gateB" + index));

      final var verilog = functionality(HdlGeneratorFactory.VERILOG);
      assertTrue(
          verilog.contains(
              "assign gateO" + index + " = gateA" + index + "&gateB" + index + ";"));
    }
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var gate = new Ttl747001();
    final var attrs = gate.createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(gate.isHDLSupportedComponent(attrs));
    final var generator = gate.getHDLGenerator(attrs);
    assertFalse(generator.isHdlSupportedTarget(null));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(gate.isHDLSupportedComponent(attrs));
    assertNull(gate.getHDLGenerator(attrs));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var gate = new Ttl747001();
    final var attrs = gate.createAttributeSet();
    final var generator = (AbstractHdlGeneratorFactory) gate.getHDLGenerator(attrs);
    return String.join("\n", generator.getModuleFunctionality(null, attrs).get());
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void driveAllLow(TtlTestInstanceState state) {
    for (final var pins : GATES) {
      state.setPortValue(pins[0], Value.FALSE);
      state.setPortValue(pins[1], Value.FALSE);
    }
  }

  private static void assertOtherOutputs(TtlTestInstanceState state, int driven, Value expected) {
    for (final var output : OUTPUTS) {
      if (output != driven) {
        assertEquals(expected, state.getPortValue(output));
      }
    }
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var output : OUTPUTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(output));
    }
  }
}
