/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.proj.Project;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC4040 12-stage binary ripple counter. */
class Ttl744040Test {
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;
  private static final BitWidth WIDTH = BitWidth.create(12);
  private static final int[] STAGE_BITS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
  private static final int[] STAGE_PORTS = {
    Ttl744040.PORT_INDEX_Q1,
    Ttl744040.PORT_INDEX_Q2,
    Ttl744040.PORT_INDEX_Q3,
    Ttl744040.PORT_INDEX_Q4,
    Ttl744040.PORT_INDEX_Q5,
    Ttl744040.PORT_INDEX_Q6,
    Ttl744040.PORT_INDEX_Q7,
    Ttl744040.PORT_INDEX_Q8,
    Ttl744040.PORT_INDEX_Q9,
    Ttl744040.PORT_INDEX_Q10,
    Ttl744040.PORT_INDEX_Q11,
    Ttl744040.PORT_INDEX_Q12
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl744040();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744040.PORT_INDEX_Q12, 10, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744040.PORT_INDEX_Q6, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744040.PORT_INDEX_Q5, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744040.PORT_INDEX_Q7, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744040.PORT_INDEX_Q4, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744040.PORT_INDEX_Q3, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744040.PORT_INDEX_Q2, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744040.PORT_INDEX_Q1, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744040.PORT_INDEX_CP, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744040.PORT_INDEX_MR, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744040.PORT_INDEX_Q9, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744040.PORT_INDEX_Q8, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744040.PORT_INDEX_Q10, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744040.PORT_INDEX_Q11, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 150, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void outputsFollowBinaryStageWeights() {
    final var gate = new Ttl744040();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    assertCount(state, 0);

    for (var count = 1; count <= 64; count++) {
      pulse(gate, state);
      assertCount(state, count);
    }

    clock(gate, state, 2048 - 64);
    assertCount(state, 2048);
    assertEquals(Value.TRUE, state.getPortValue(Ttl744040.PORT_INDEX_Q12));
    clock(gate, state, 2047);
    assertCount(state, 4095);
    pulse(gate, state);
    assertCount(state, 0);
  }

  @Test
  void masterResetClearsAndOverridesTheClock() {
    final var gate = new Ttl744040();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    clock(gate, state, 8);
    assertCount(state, 8);

    state.setPortValue(Ttl744040.PORT_INDEX_CP, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl744040.PORT_INDEX_MR, Value.TRUE);
    state.setPortValue(Ttl744040.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 0);

    pulse(gate, state);
    assertCount(state, 0);
    state.setPortValue(Ttl744040.PORT_INDEX_MR, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 0);
    clock(gate, state, 8);
    assertCount(state, 8);
  }

  @Test
  void risingAndUnknownClocksDoNotCount() {
    final var gate = new Ttl744040();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);

    state.setPortValue(Ttl744040.PORT_INDEX_CP, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);

    state.setPortValue(Ttl744040.PORT_INDEX_CP, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl744040.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 0);
  }

  @Test
  void unknownResetDoesNotClearAndAFallingEdgeStillCounts() {
    final var gate = new Ttl744040();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    clock(gate, state, 8);

    state.setPortValue(Ttl744040.PORT_INDEX_MR, Value.UNKNOWN);
    gate.propagate(state);
    assertCount(state, 8);

    pulse(gate, state);
    assertCount(state, 9);
  }

  @Test
  void undefinedCountBecomesUnknownOnTheNextClock() {
    final var gate = new Ttl744040();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    final var data = new TtlRegisterData(WIDTH);
    data.setValue(Value.createUnknown(WIDTH));
    state.setData(data);

    pulse(gate, state);
    assertUnknownOutputs(state);

    state.setPortValue(Ttl744040.PORT_INDEX_MR, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
  }

  @Test
  void errorInTheCountBecomesErrorOnTheNextClock() {
    final var gate = new Ttl744040();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    final var data = new TtlRegisterData(WIDTH);
    final var bits = Value.createKnown(WIDTH, 0).getAll();
    bits[0] = Value.ERROR;
    data.setValue(Value.create(bits));
    state.setData(data);

    pulse(gate, state);
    assertErrorOutputs(state);

    state.setPortValue(Ttl744040.PORT_INDEX_MR, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl744040();
    final var state = new TestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    reset(gate, state);
    clock(gate, state, 8);
    assertCount(state, 8);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 8);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  @Test
  void gatedClockIsTheExternalClockPin() {
    final var gate = new Ttl744040();
    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(new int[] {Ttl744040.PORT_INDEX_CP}, gate.clockPinIndex(null));
  }

  private static Instance createInstance(InstanceFactory factory, boolean showPowerPins) {
    final var attrs = factory.createAttributeSet();
    attrs.setValue(TtlLibrary.VCC_GND, showPowerPins);
    return Instance.getInstanceFor(
        factory.createComponent(Location.create(0, 0, false), attrs));
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void reset(Ttl744040 gate, TestInstanceState state) {
    state.setPortValue(Ttl744040.PORT_INDEX_CP, Value.FALSE);
    state.setPortValue(Ttl744040.PORT_INDEX_MR, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl744040.PORT_INDEX_MR, Value.FALSE);
    gate.propagate(state);
  }

  private static void pulse(Ttl744040 gate, TestInstanceState state) {
    state.setPortValue(Ttl744040.PORT_INDEX_CP, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl744040.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
  }

  private static void clock(Ttl744040 gate, TestInstanceState state, int pulses) {
    for (var i = 0; i < pulses; i++) {
      pulse(gate, state);
    }
  }

  private static void assertCount(TestInstanceState state, int count) {
    for (var i = 0; i < STAGE_BITS.length; i++) {
      final var expected = ((count >> STAGE_BITS[i]) & 1) == 0 ? Value.FALSE : Value.TRUE;
      assertEquals(expected, state.getPortValue(STAGE_PORTS[i]), "count " + count);
    }
  }

  private static void assertUnknownOutputs(TestInstanceState state) {
    for (final var port : STAGE_PORTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(port));
    }
  }

  private static void assertErrorOutputs(TestInstanceState state) {
    for (final var port : STAGE_PORTS) {
      assertEquals(Value.ERROR, state.getPortValue(port));
    }
  }

  private static final class TestInstanceState implements InstanceState {
    private final AttributeSet attrs;
    private final Instance instance;
    private final Map<Integer, Value> portValues = new HashMap<>();
    private InstanceData data;

    private TestInstanceState(InstanceFactory factory, boolean showPowerPins) {
      attrs = factory.createAttributeSet();
      attrs.setValue(TtlLibrary.VCC_GND, showPowerPins);
      instance =
          Instance.getInstanceFor(
              factory.createComponent(Location.create(0, 0, false), attrs));
    }

    @Override
    public void fireInvalidated() {}

    @Override
    public AttributeSet getAttributeSet() {
      return attrs;
    }

    @Override
    public <E> E getAttributeValue(Attribute<E> attr) {
      return attrs.getValue(attr);
    }

    @Override
    public InstanceData getData() {
      return data;
    }

    @Override
    public InstanceFactory getFactory() {
      return instance.getFactory();
    }

    @Override
    public Instance getInstance() {
      return instance;
    }

    @Override
    public int getPortIndex(Port port) {
      return instance.getPorts().indexOf(port);
    }

    @Override
    public Value getPortValue(int portIndex) {
      return portValues.getOrDefault(portIndex, Value.UNKNOWN);
    }

    @Override
    public Project getProject() {
      return null;
    }

    @Override
    public int getTickCount() {
      return 0;
    }

    @Override
    public boolean isCircuitRoot() {
      return true;
    }

    @Override
    public boolean isPortConnected(int portIndex) {
      return false;
    }

    @Override
    public CircuitState createCircuitSubstateFor(Circuit circ) {
      return null;
    }

    @Override
    public void setData(InstanceData value) {
      data = value;
    }

    @Override
    public void setPort(int portIndex, Value value, int delay) {
      portValues.put(portIndex, value);
    }

    private void setPortValue(int portIndex, Value value) {
      portValues.put(portIndex, value);
    }
  }
}
