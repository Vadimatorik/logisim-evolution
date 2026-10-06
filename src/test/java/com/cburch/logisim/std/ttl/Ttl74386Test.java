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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.prefs.AppPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Tests for the TTL 74386 quad 2-input exclusive-OR gate. */
class Ttl74386Test {
  private static final int[][] GATES = {
    {Ttl74386.PORT_INDEX_1A, Ttl74386.PORT_INDEX_1B, Ttl74386.PORT_INDEX_1Y},
    {Ttl74386.PORT_INDEX_2A, Ttl74386.PORT_INDEX_2B, Ttl74386.PORT_INDEX_2Y},
    {Ttl74386.PORT_INDEX_3A, Ttl74386.PORT_INDEX_3B, Ttl74386.PORT_INDEX_3Y},
    {Ttl74386.PORT_INDEX_4A, Ttl74386.PORT_INDEX_4B, Ttl74386.PORT_INDEX_4Y}
  };

  private static final int[] OUTPUTS = {
    Ttl74386.PORT_INDEX_1Y,
    Ttl74386.PORT_INDEX_2Y,
    Ttl74386.PORT_INDEX_3Y,
    Ttl74386.PORT_INDEX_4Y
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
    final var gate = new Ttl74386();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74386.PORT_INDEX_1A, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74386.PORT_INDEX_1B, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74386.PORT_INDEX_1Y, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74386.PORT_INDEX_2Y, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74386.PORT_INDEX_2A, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74386.PORT_INDEX_2B, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74386.PORT_INDEX_3A, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74386.PORT_INDEX_3B, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74386.PORT_INDEX_3Y, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74386.PORT_INDEX_4Y, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74386.PORT_INDEX_4A, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74386.PORT_INDEX_4B, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertPort(shownPower, Ttl74386.PORT_INDEX_1A, 10, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, Ttl74386.PORT_INDEX_4B, 30, -30, EndData.INPUT_ONLY);
    assertPort(shownPower, GND_PORT, 130, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void eachGateFollowsTheXorTruthTable() {
    final var gate = new Ttl74386();
    final var state = new TtlTestInstanceState(gate, false);
    final var levels = new Value[] {Value.FALSE, Value.TRUE};

    for (final var pins : GATES) {
      for (final var inputA : levels) {
        for (final var inputB : levels) {
          driveAllLow(state);
          state.setPortValue(pins[0], inputA);
          state.setPortValue(pins[1], inputB);
          gate.propagate(state);

          final var expected = inputA == inputB ? Value.FALSE : Value.TRUE;
          assertEquals(expected, state.getPortValue(pins[2]));
          for (final var output : OUTPUTS) {
            if (output != pins[2]) {
              assertEquals(Value.FALSE, state.getPortValue(output));
            }
          }
        }
      }
    }
  }

  @Test
  void unsettledInputMakesTheOutputAnError() {
    final var gate = new Ttl74386();
    final var state = new TtlTestInstanceState(gate, false);

    for (final var unsettled : new Value[] {Value.UNKNOWN, Value.ERROR}) {
      driveAllLow(state);
      state.setPortValue(Ttl74386.PORT_INDEX_2A, unsettled);
      state.setPortValue(Ttl74386.PORT_INDEX_2B, Value.TRUE);
      gate.propagate(state);
      assertEquals(Value.ERROR, state.getPortValue(Ttl74386.PORT_INDEX_2Y));
      assertEquals(Value.FALSE, state.getPortValue(Ttl74386.PORT_INDEX_1Y));
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74386();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    driveHighOnFirstGate(state, gate);
    assertEquals(Value.TRUE, state.getPortValue(Ttl74386.PORT_INDEX_1Y));

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    driveHighOnFirstGate(state, gate);
    assertEquals(Value.TRUE, state.getPortValue(Ttl74386.PORT_INDEX_1Y));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  @Test
  void vhdlExclusiveOrFollowsThe386PinOrder() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(containsIgnoringCase(hdl, "Y1 <= A1 XOR B1;"));
    assertTrue(containsIgnoringCase(hdl, "Y2 <= A2 XOR B2;"));
    assertTrue(containsIgnoringCase(hdl, "Y3 <= A3 XOR B3;"));
    assertTrue(containsIgnoringCase(hdl, "Y4 <= A4 XOR B4;"));
  }

  @Test
  void verilogExclusiveOrFollowsThe386PinOrder() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign Y1 = A1^B1;"));
    assertTrue(hdl.contains("assign Y2 = A2^B2;"));
    assertTrue(hdl.contains("assign Y3 = A3^B3;"));
    assertTrue(hdl.contains("assign Y4 = A4^B4;"));
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var generator = new Ttl74386HdlGenerator();
    final var attrs = new Ttl74386().createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(generator.isHdlSupportedTarget(attrs));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(generator.isHdlSupportedTarget(attrs));

    assertFalse(generator.isHdlSupportedTarget(null));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Ttl74386().createAttributeSet();
    return String.join("\n", new Ttl74386HdlGenerator().getModuleFunctionality(null, attrs).get());
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

  private static void driveHighOnFirstGate(TtlTestInstanceState state, Ttl74386 gate) {
    driveAllLow(state);
    state.setPortValue(Ttl74386.PORT_INDEX_1A, Value.TRUE);
    state.setPortValue(Ttl74386.PORT_INDEX_1B, Value.FALSE);
    gate.propagate(state);
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var output : OUTPUTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(output));
    }
  }
}
