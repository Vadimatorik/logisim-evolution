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

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
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

/** Functional tests for the 74HC4015 dual 4-bit static shift register. */
class Ttl744015Test {
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;
  private static final int[] REGISTER1 = {
    Ttl744015.PORT_INDEX_1Q0,
    Ttl744015.PORT_INDEX_1Q1,
    Ttl744015.PORT_INDEX_1Q2,
    Ttl744015.PORT_INDEX_1Q3
  };
  private static final int[] REGISTER2 = {
    Ttl744015.PORT_INDEX_2Q0,
    Ttl744015.PORT_INDEX_2Q1,
    Ttl744015.PORT_INDEX_2Q2,
    Ttl744015.PORT_INDEX_2Q3
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl744015();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744015.PORT_INDEX_2CP, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744015.PORT_INDEX_2Q3, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744015.PORT_INDEX_1Q2, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744015.PORT_INDEX_1Q1, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744015.PORT_INDEX_1Q0, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744015.PORT_INDEX_1MR, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744015.PORT_INDEX_1D, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744015.PORT_INDEX_1CP, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744015.PORT_INDEX_1Q3, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744015.PORT_INDEX_2Q2, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744015.PORT_INDEX_2Q1, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744015.PORT_INDEX_2Q0, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744015.PORT_INDEX_2MR, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744015.PORT_INDEX_2D, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 150, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void eachHalfShiftsOnItsOwnRisingEdge() {
    final var gate = new Ttl744015();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);

    shift(gate, state, Ttl744015.PORT_INDEX_1CP, Ttl744015.PORT_INDEX_1D, Value.TRUE);
    assertRegister(state, REGISTER1, 0b0001);
    assertRegister(state, REGISTER2, 0);
    shift(gate, state, Ttl744015.PORT_INDEX_1CP, Ttl744015.PORT_INDEX_1D, Value.FALSE);
    shift(gate, state, Ttl744015.PORT_INDEX_1CP, Ttl744015.PORT_INDEX_1D, Value.TRUE);
    shift(gate, state, Ttl744015.PORT_INDEX_1CP, Ttl744015.PORT_INDEX_1D, Value.TRUE);
    assertRegister(state, REGISTER1, 0b1011);
    assertRegister(state, REGISTER2, 0);

    state.setPortValue(Ttl744015.PORT_INDEX_1D, Value.FALSE);
    state.setPortValue(Ttl744015.PORT_INDEX_2D, Value.TRUE);
    gate.propagate(state);
    assertRegister(state, REGISTER1, 0b1011);
    assertRegister(state, REGISTER2, 0);

    shift(gate, state, Ttl744015.PORT_INDEX_2CP, Ttl744015.PORT_INDEX_2D, Value.TRUE);
    shift(gate, state, Ttl744015.PORT_INDEX_2CP, Ttl744015.PORT_INDEX_2D, Value.FALSE);
    shift(gate, state, Ttl744015.PORT_INDEX_2CP, Ttl744015.PORT_INDEX_2D, Value.TRUE);
    shift(gate, state, Ttl744015.PORT_INDEX_2CP, Ttl744015.PORT_INDEX_2D, Value.TRUE);
    assertRegister(state, REGISTER1, 0b1011);
    assertRegister(state, REGISTER2, 0b1011);
  }

  @Test
  void fallingAndLevelClocksDoNotShift() {
    final var gate = new Ttl744015();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    shift(gate, state, Ttl744015.PORT_INDEX_1CP, Ttl744015.PORT_INDEX_1D, Value.TRUE);
    assertRegister(state, REGISTER1, 0b0001);

    state.setPortValue(Ttl744015.PORT_INDEX_1D, Value.FALSE);
    state.setPortValue(Ttl744015.PORT_INDEX_1CP, Value.TRUE);
    gate.propagate(state);
    assertRegister(state, REGISTER1, 0b0010);
    state.setPortValue(Ttl744015.PORT_INDEX_1D, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl744015.PORT_INDEX_1CP, Value.FALSE);
    gate.propagate(state);
    assertRegister(state, REGISTER1, 0b0010);
  }

  @Test
  void masterResetClearsOneHalfAndOverridesItsClock() {
    final var gate = new Ttl744015();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    shift(gate, state, Ttl744015.PORT_INDEX_1CP, Ttl744015.PORT_INDEX_1D, Value.TRUE);
    shift(gate, state, Ttl744015.PORT_INDEX_2CP, Ttl744015.PORT_INDEX_2D, Value.TRUE);
    shift(gate, state, Ttl744015.PORT_INDEX_2CP, Ttl744015.PORT_INDEX_2D, Value.TRUE);

    state.setPortValue(Ttl744015.PORT_INDEX_1CP, Value.TRUE);
    state.setPortValue(Ttl744015.PORT_INDEX_1D, Value.TRUE);
    state.setPortValue(Ttl744015.PORT_INDEX_1MR, Value.TRUE);
    gate.propagate(state);
    assertRegister(state, REGISTER1, 0);
    assertRegister(state, REGISTER2, 0b0011);

    state.setPortValue(Ttl744015.PORT_INDEX_1CP, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl744015.PORT_INDEX_1CP, Value.TRUE);
    gate.propagate(state);
    assertRegister(state, REGISTER1, 0);

    state.setPortValue(Ttl744015.PORT_INDEX_1MR, Value.FALSE);
    state.setPortValue(Ttl744015.PORT_INDEX_1CP, Value.FALSE);
    gate.propagate(state);
    assertRegister(state, REGISTER1, 0);
    assertRegister(state, REGISTER2, 0b0011);
    shift(gate, state, Ttl744015.PORT_INDEX_1CP, Ttl744015.PORT_INDEX_1D, Value.TRUE);
    assertRegister(state, REGISTER1, 0b0001);
  }

  @Test
  void unknownClockDoesNotShift() {
    final var gate = new Ttl744015();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    shift(gate, state, Ttl744015.PORT_INDEX_1CP, Ttl744015.PORT_INDEX_1D, Value.TRUE);

    state.setPortValue(Ttl744015.PORT_INDEX_1D, Value.FALSE);
    state.setPortValue(Ttl744015.PORT_INDEX_1CP, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl744015.PORT_INDEX_1CP, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl744015.PORT_INDEX_1CP, Value.FALSE);
    gate.propagate(state);
    assertRegister(state, REGISTER1, 0b0001);

    shift(gate, state, Ttl744015.PORT_INDEX_1CP, Ttl744015.PORT_INDEX_1D, Value.FALSE);
    assertRegister(state, REGISTER1, 0b0010);
  }

  @Test
  void unknownResetDoesNotClearAndARisingEdgeStillShifts() {
    final var gate = new Ttl744015();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    shift(gate, state, Ttl744015.PORT_INDEX_1CP, Ttl744015.PORT_INDEX_1D, Value.TRUE);
    shift(gate, state, Ttl744015.PORT_INDEX_1CP, Ttl744015.PORT_INDEX_1D, Value.FALSE);
    shift(gate, state, Ttl744015.PORT_INDEX_1CP, Ttl744015.PORT_INDEX_1D, Value.TRUE);
    shift(gate, state, Ttl744015.PORT_INDEX_1CP, Ttl744015.PORT_INDEX_1D, Value.TRUE);
    assertRegister(state, REGISTER1, 0b1011);

    state.setPortValue(Ttl744015.PORT_INDEX_1MR, Value.UNKNOWN);
    gate.propagate(state);
    assertRegister(state, REGISTER1, 0b1011);

    shift(gate, state, Ttl744015.PORT_INDEX_1CP, Ttl744015.PORT_INDEX_1D, Value.FALSE);
    assertRegister(state, REGISTER1, 0b0110);
  }

  @Test
  void unknownAndErrorDataShiftInWithoutChangingTheOtherBits() {
    final var gate = new Ttl744015();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    shift(gate, state, Ttl744015.PORT_INDEX_1CP, Ttl744015.PORT_INDEX_1D, Value.TRUE);
    shift(gate, state, Ttl744015.PORT_INDEX_1CP, Ttl744015.PORT_INDEX_1D, Value.TRUE);

    shift(gate, state, Ttl744015.PORT_INDEX_1CP, Ttl744015.PORT_INDEX_1D, Value.UNKNOWN);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl744015.PORT_INDEX_1Q0));
    assertEquals(Value.TRUE, state.getPortValue(Ttl744015.PORT_INDEX_1Q1));
    assertEquals(Value.TRUE, state.getPortValue(Ttl744015.PORT_INDEX_1Q2));
    assertEquals(Value.FALSE, state.getPortValue(Ttl744015.PORT_INDEX_1Q3));

    shift(gate, state, Ttl744015.PORT_INDEX_1CP, Ttl744015.PORT_INDEX_1D, Value.ERROR);
    assertEquals(Value.ERROR, state.getPortValue(Ttl744015.PORT_INDEX_1Q0));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl744015.PORT_INDEX_1Q1));
    assertEquals(Value.TRUE, state.getPortValue(Ttl744015.PORT_INDEX_1Q2));
    assertEquals(Value.TRUE, state.getPortValue(Ttl744015.PORT_INDEX_1Q3));

    final var stored = (TtlRegisterData) state.getData();
    stored.setValue(
        0, Value.create(new Value[] {Value.ERROR, Value.TRUE, Value.FALSE, Value.TRUE}));
    shift(gate, state, Ttl744015.PORT_INDEX_1CP, Ttl744015.PORT_INDEX_1D, Value.FALSE);
    assertEquals(Value.FALSE, state.getPortValue(Ttl744015.PORT_INDEX_1Q0));
    assertEquals(Value.ERROR, state.getPortValue(Ttl744015.PORT_INDEX_1Q1));
    assertEquals(Value.TRUE, state.getPortValue(Ttl744015.PORT_INDEX_1Q2));
    assertEquals(Value.FALSE, state.getPortValue(Ttl744015.PORT_INDEX_1Q3));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl744015();
    final var state = new TestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    reset(gate, state);
    shift(gate, state, Ttl744015.PORT_INDEX_1CP, Ttl744015.PORT_INDEX_1D, Value.TRUE);
    shift(gate, state, Ttl744015.PORT_INDEX_2CP, Ttl744015.PORT_INDEX_2D, Value.TRUE);
    assertRegister(state, REGISTER1, 0b0001);
    assertRegister(state, REGISTER2, 0b0001);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertRegister(state, REGISTER1, 0b0001);
    assertRegister(state, REGISTER2, 0b0001);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  private static Instance createInstance(InstanceFactory factory, boolean showPowerPins) {
    final var attrs = factory.createAttributeSet();
    attrs.setValue(TtlLibrary.VCC_GND, showPowerPins);
    return Instance.getInstanceFor(factory.createComponent(Location.create(0, 0, false), attrs));
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void reset(Ttl744015 gate, TestInstanceState state) {
    state.setPortValue(Ttl744015.PORT_INDEX_1CP, Value.FALSE);
    state.setPortValue(Ttl744015.PORT_INDEX_2CP, Value.FALSE);
    state.setPortValue(Ttl744015.PORT_INDEX_1D, Value.FALSE);
    state.setPortValue(Ttl744015.PORT_INDEX_2D, Value.FALSE);
    state.setPortValue(Ttl744015.PORT_INDEX_1MR, Value.TRUE);
    state.setPortValue(Ttl744015.PORT_INDEX_2MR, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl744015.PORT_INDEX_1MR, Value.FALSE);
    state.setPortValue(Ttl744015.PORT_INDEX_2MR, Value.FALSE);
    gate.propagate(state);
  }

  private static void shift(
      Ttl744015 gate, TestInstanceState state, int clockPort, int dataPort, Value data) {
    state.setPortValue(dataPort, data);
    state.setPortValue(clockPort, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(clockPort, Value.FALSE);
    gate.propagate(state);
  }

  private static void assertRegister(TestInstanceState state, int[] ports, int bits) {
    for (var stage = 0; stage < ports.length; stage++) {
      final var expected = ((bits >> stage) & 1) == 0 ? Value.FALSE : Value.TRUE;
      assertEquals(expected, state.getPortValue(ports[stage]), "stage " + stage);
    }
  }

  private static void assertUnknownOutputs(TestInstanceState state) {
    for (final var port : REGISTER1) {
      assertEquals(Value.UNKNOWN, state.getPortValue(port));
    }
    for (final var port : REGISTER2) {
      assertEquals(Value.UNKNOWN, state.getPortValue(port));
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
          Instance.getInstanceFor(factory.createComponent(Location.create(0, 0, false), attrs));
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
