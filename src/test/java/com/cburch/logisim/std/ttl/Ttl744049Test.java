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

/** Functional tests for the 74HC4049 hex inverting level shifter. */
class Ttl744049Test {
  private static final int GND_PORT = 12;
  private static final int VCC_PORT = 13;
  private static final int[] INPUT_PORTS = {
    Ttl744049.PORT_A1,
    Ttl744049.PORT_A2,
    Ttl744049.PORT_A3,
    Ttl744049.PORT_A4,
    Ttl744049.PORT_A5,
    Ttl744049.PORT_A6
  };
  private static final int[] OUTPUT_PORTS = {
    Ttl744049.PORT_Y1,
    Ttl744049.PORT_Y2,
    Ttl744049.PORT_Y3,
    Ttl744049.PORT_Y4,
    Ttl744049.PORT_Y5,
    Ttl744049.PORT_Y6
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl744049();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744049.PORT_Y1, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744049.PORT_A1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744049.PORT_Y2, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744049.PORT_A2, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744049.PORT_Y3, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744049.PORT_A3, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744049.PORT_A4, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744049.PORT_Y4, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744049.PORT_A5, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744049.PORT_Y5, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744049.PORT_A6, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744049.PORT_Y6, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 150, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, 30, EndData.INPUT_ONLY);
  }

  @Test
  void everyInputPatternIsInverted() {
    final var gate = new Ttl744049();
    final var state = new TestInstanceState(gate, false);

    for (var pattern = 0; pattern < 64; pattern++) {
      drive(state, pattern);
      gate.propagate(state);
      assertOutputs(state, (~pattern) & 0x3F, "pattern " + pattern);
    }
  }

  @Test
  void unknownAndErrorStayOnTheirOwnChannel() {
    final var gate = new Ttl744049();
    final var state = new TestInstanceState(gate, false);
    drive(state, 0);

    state.setPortValue(Ttl744049.PORT_A3, Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(Value.ERROR, state.getPortValue(Ttl744049.PORT_Y3));
    assertEquals(Value.TRUE, state.getPortValue(Ttl744049.PORT_Y1));
    assertEquals(Value.TRUE, state.getPortValue(Ttl744049.PORT_Y6));

    state.setPortValue(Ttl744049.PORT_A3, Value.FALSE);
    state.setPortValue(Ttl744049.PORT_A6, Value.ERROR);
    gate.propagate(state);
    assertEquals(Value.TRUE, state.getPortValue(Ttl744049.PORT_Y3));
    assertEquals(Value.ERROR, state.getPortValue(Ttl744049.PORT_Y6));
    assertEquals(Value.TRUE, state.getPortValue(Ttl744049.PORT_Y1));
  }

  @Test
  void shownPowerPinsRequireAValidSupply() {
    final var gate = new Ttl744049();
    final var state = new TestInstanceState(gate, true);
    drive(state, 0x15);

    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertOutputs(state, (~0x15) & 0x3F, "powered");

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(GND_PORT, Value.FALSE);
    gate.propagate(state);
    assertOutputs(state, (~0x15) & 0x3F, "powered again");
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

  private static void drive(TestInstanceState state, int pattern) {
    for (var bit = 0; bit < INPUT_PORTS.length; bit++) {
      state.setPortValue(INPUT_PORTS[bit], ((pattern >> bit) & 1) == 0 ? Value.FALSE : Value.TRUE);
    }
  }

  private static void assertOutputs(TestInstanceState state, int expected, String message) {
    for (var bit = 0; bit < OUTPUT_PORTS.length; bit++) {
      final var bitValue = ((expected >> bit) & 1) == 0 ? Value.FALSE : Value.TRUE;
      assertEquals(bitValue, state.getPortValue(OUTPUT_PORTS[bit]), message);
    }
  }

  private static void assertUnknownOutputs(TestInstanceState state) {
    for (final var port : OUTPUT_PORTS) {
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
