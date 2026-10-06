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
import static org.junit.jupiter.api.Assertions.assertTrue;

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

/** Functional tests for the 74x121 non-retriggerable monostable. */
class Ttl74121Test {
  /** Datasheet point Rext = 10 kΩ, Cext = 100 nF is 700 µs, which is 14 ticks at 20 kHz. */
  private static final double DATASHEET_TICK_HZ = 20_000;
  /**
   * Unit tests have no simulator, so the component uses 1 Hz. These parts last 7 ticks at that
   * rate and stay inside the attribute limits.
   */
  private static final int SEVEN_TICK_REXT_KOHM = 10;
  private static final int SEVEN_TICK_CEXT_PF = 1_000_000_000;
  private static final int PULSE_TICKS = 7;
  private static final int DATASHEET_TICKS = 14;
  private static final int GND_PORT = 5;
  private static final int VCC_PORT = 6;
  private static final int[] OUTPUT_PORTS = {Ttl74121.PORT_INDEX_QBAR, Ttl74121.PORT_INDEX_Q};

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74121();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(5, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74121.PORT_INDEX_QBAR, 10, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74121.PORT_INDEX_A1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74121.PORT_INDEX_A2, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74121.PORT_INDEX_B, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74121.PORT_INDEX_Q, 110, 30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(7, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 130, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void widthTicksMatchesTheDatasheetExample() {
    assertEquals(DATASHEET_TICKS, Ttl74121.widthTicks(10, 100_000, DATASHEET_TICK_HZ));
    assertEquals(
        PULSE_TICKS, Ttl74121.widthTicks(SEVEN_TICK_REXT_KOHM, SEVEN_TICK_CEXT_PF, 1));
    assertEquals(
        DATASHEET_TICKS,
        Ttl74121.widthTicks(Ttl74121.INTERNAL_REXT_KOHM, SEVEN_TICK_CEXT_PF, 10));
    assertEquals(1, Ttl74121.widthTicks(10, 100_000, 1));
    assertEquals(1, Ttl74121.widthTicks(10, 100_000, 0));
    assertEquals(Integer.MAX_VALUE, Ttl74121.widthTicks(40, 1_000_000_000, 1.0e12));
  }

  @Test
  void risingBTriggersWhenA1IsLow() {
    final var gate = gate();
    final var state = arm(gate, false, true, false);
    state.setTickCount(100);

    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.TRUE);
    gate.propagate(state);

    assertPulse(state, Value.TRUE);
    assertFalse(gate.expire(state, 106));
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
    assertTrue(gate.expire(state, 107));
    gate.propagate(state);
    assertPulse(state, Value.FALSE);
  }

  @Test
  void risingBTriggersWhenOnlyA2IsLow() {
    final var gate = gate();
    final var state = arm(gate, true, false, false);

    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.TRUE);
    gate.propagate(state);

    assertPulse(state, Value.TRUE);
  }

  @Test
  void fallingA1TriggersWhenA2AndBAreHigh() {
    final var gate = gate();
    final var state = arm(gate, true, true, true);
    assertPulse(state, Value.FALSE);

    state.setPortValue(Ttl74121.PORT_INDEX_A1, Value.FALSE);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
  }

  @Test
  void fallingA2TriggersWhenA1AndBAreHigh() {
    final var gate = gate();
    final var state = arm(gate, true, true, true);

    state.setPortValue(Ttl74121.PORT_INDEX_A2, Value.FALSE);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
  }

  @Test
  void bothAInputsFallingTriggersWhenBIsHigh() {
    final var gate = gate();
    final var state = arm(gate, true, true, true);

    state.setPortValue(Ttl74121.PORT_INDEX_A1, Value.FALSE);
    state.setPortValue(Ttl74121.PORT_INDEX_A2, Value.FALSE);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
  }

  @Test
  void gatedLevelsDoNotTrigger() {
    final var gate = gate();
    final var bothAHigh = arm(gate, true, true, false);
    bothAHigh.setPortValue(Ttl74121.PORT_INDEX_B, Value.TRUE);
    gate.propagate(bothAHigh);
    assertPulse(bothAHigh, Value.FALSE);

    final var bLow = arm(gate, true, true, false);
    bLow.setPortValue(Ttl74121.PORT_INDEX_A1, Value.FALSE);
    gate.propagate(bLow);
    assertPulse(bLow, Value.FALSE);

    final var otherAAlreadyLow = arm(gate, true, false, true);
    otherAAlreadyLow.setPortValue(Ttl74121.PORT_INDEX_A1, Value.FALSE);
    gate.propagate(otherAAlreadyLow);
    assertPulse(otherAAlreadyLow, Value.FALSE);
  }

  @Test
  void anotherEdgeDoesNotRestartTheCapturedWidth() {
    final var gate = gate();
    final var state = arm(gate, false, true, false);
    state.setTickCount(100);
    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.TRUE);
    gate.propagate(state);

    state.setTickCount(102);
    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.FALSE);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.TRUE);
    gate.propagate(state);

    assertFalse(gate.expire(state, 106));
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
    assertTrue(gate.expire(state, 107));
    gate.propagate(state);
    assertPulse(state, Value.FALSE);
  }

  @Test
  void stableInputLevelsDoNotAbortAnActivePulse() {
    final var gate = gate();
    final var state = arm(gate, false, true, false);
    state.setTickCount(100);
    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.TRUE);
    gate.propagate(state);

    state.setPortValue(Ttl74121.PORT_INDEX_A1, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.FALSE);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);

    assertFalse(gate.expire(state, 106));
    assertTrue(gate.expire(state, 107));
    gate.propagate(state);
    assertPulse(state, Value.FALSE);
  }

  @Test
  void newEdgeAfterThePulseTriggersAgain() {
    final var gate = gate();
    final var state = arm(gate, false, true, false);
    state.setTickCount(100);
    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.TRUE);
    gate.propagate(state);
    assertTrue(gate.expire(state, 107));
    gate.propagate(state);
    assertPulse(state, Value.FALSE);

    state.setTickCount(200);
    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.TRUE);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
    assertFalse(gate.expire(state, 206));
    assertTrue(gate.expire(state, 207));
  }

  @Test
  void timingAttributesApplyOnTheNextTrigger() {
    final var gate = gate();
    final var state = arm(gate, false, true, false);
    state.setTickCount(100);
    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.TRUE);
    gate.propagate(state);

    state.getAttributeSet().setValue(Ttl74121.CEXT, 1_000);
    assertFalse(gate.expire(state, 101));
    gate.propagate(state);
    assertPulse(state, Value.TRUE);

    assertTrue(gate.expire(state, 107));
    gate.propagate(state);
    state.setTickCount(200);
    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.TRUE);
    gate.propagate(state);
    assertFalse(gate.expire(state, 200));
    assertTrue(gate.expire(state, 201));
  }

  @Test
  void internalResistorUsesTwoKiloohms() {
    final var gate = gate();
    final var state = arm(gate, false, true, false);
    state.getAttributeSet().setValue(Ttl74121.USE_RINT, true);
    state.setTickCount(100);
    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.TRUE);
    gate.propagate(state);

    assertFalse(gate.expire(state, 100));
    assertTrue(gate.expire(state, 101));
    gate.propagate(state);
    assertPulse(state, Value.FALSE);
  }

  @Test
  void undefinedLevelsDoNotCreateEdgesOrAbortAPulse() {
    final var gate = gate();
    final var state = arm(gate, false, true, false);

    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.TRUE);
    gate.propagate(state);
    assertPulse(state, Value.FALSE);

    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.TRUE);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);

    state.setPortValue(Ttl74121.PORT_INDEX_A1, Value.UNKNOWN);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.ERROR);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
  }

  @Test
  void wrongPowerPinsForceUnknownOutputs() {
    final var gate = gate();
    final var state = new TestInstanceState(gate, true);
    useSevenTickTiming(state);
    state.setPortValue(Ttl74121.PORT_INDEX_A1, Value.FALSE);
    state.setPortValue(Ttl74121.PORT_INDEX_A2, Value.TRUE);
    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.FALSE);
    gate.propagate(state);
    for (final var port : OUTPUT_PORTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(port));
    }

    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertPulse(state, Value.FALSE);
    state.setPortValue(Ttl74121.PORT_INDEX_B, Value.TRUE);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
  }

  private static Ttl74121 gate() {
    return new Ttl74121();
  }

  private static void useSevenTickTiming(TestInstanceState state) {
    state.getAttributeSet().setValue(Ttl74121.REXT, SEVEN_TICK_REXT_KOHM);
    state.getAttributeSet().setValue(Ttl74121.CEXT, SEVEN_TICK_CEXT_PF);
  }

  /** Records a stable input level so the next transition can be recognized as an edge. */
  private static TestInstanceState arm(Ttl74121 gate, boolean inputA1, boolean inputA2, boolean inputB) {
    final var state = new TestInstanceState(gate, false);
    useSevenTickTiming(state);
    state.setPortValue(Ttl74121.PORT_INDEX_A1, inputA1 ? Value.TRUE : Value.FALSE);
    state.setPortValue(Ttl74121.PORT_INDEX_A2, inputA2 ? Value.TRUE : Value.FALSE);
    state.setPortValue(Ttl74121.PORT_INDEX_B, inputB ? Value.TRUE : Value.FALSE);
    gate.propagate(state);
    assertPulse(state, Value.FALSE);
    return state;
  }

  private static void assertPulse(TestInstanceState state, Value output) {
    assertEquals(output, state.getPortValue(Ttl74121.PORT_INDEX_Q));
    assertEquals(output.not(), state.getPortValue(Ttl74121.PORT_INDEX_QBAR));
  }

  private static Instance createInstance(InstanceFactory factory, boolean showPowerPins) {
    return new TestInstanceState(factory, showPowerPins).getInstance();
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static final class TestInstanceState implements InstanceState {
    private final AttributeSet attrs;
    private final Instance instance;
    private final Map<Integer, Value> portValues = new HashMap<>();
    private InstanceData data;
    private int tickCount;

    private TestInstanceState(InstanceFactory factory, boolean showPowerPins) {
      attrs = factory.createAttributeSet();
      attrs.setValue(TtlLibrary.VCC_GND, showPowerPins);
      instance =
          Instance.getInstanceFor(factory.createComponent(Location.create(0, 0, false), attrs));
    }

    private void setPortValue(int portIndex, Value value) {
      portValues.put(portIndex, value);
    }

    private void setTickCount(int ticks) {
      tickCount = ticks;
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
      return tickCount;
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
  }
}
