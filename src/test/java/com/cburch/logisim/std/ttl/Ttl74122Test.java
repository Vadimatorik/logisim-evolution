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

/** Functional tests for the 74x122 retriggerable monostable. */
class Ttl74122Test {
  /** Datasheet point Rext = 10 kΩ, Cext = 100 nF is 450 µs, which is 9 ticks at 20 kHz. */
  private static final double DATASHEET_TICK_HZ = 20_000;
  /**
   * Unit tests have no simulator, so the component uses 1 Hz. These parts last 9 ticks at that
   * rate and stay inside the attribute limits.
   */
  private static final int NINE_TICK_REXT_KOHM = 200;
  private static final int NINE_TICK_CEXT_PF = 100_000_000;
  private static final int PULSE_TICKS = 9;
  private static final int GND_PORT = 7;
  private static final int VCC_PORT = 8;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74122();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(7, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74122.PORT_INDEX_A1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74122.PORT_INDEX_A2, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74122.PORT_INDEX_B1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74122.PORT_INDEX_B2, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74122.PORT_INDEX_CLR, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74122.PORT_INDEX_QBAR, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74122.PORT_INDEX_Q, 130, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(9, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 130, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void widthTicksMatchesTheDatasheetExample() {
    assertEquals(PULSE_TICKS, Ttl74122.widthTicks(10, 100_000, DATASHEET_TICK_HZ));
    assertEquals(PULSE_TICKS, Ttl74122.widthTicks(NINE_TICK_REXT_KOHM, NINE_TICK_CEXT_PF, 1));
    assertEquals(1, Ttl74122.widthTicks(10, 100_000, 1));
    assertEquals(1, Ttl74122.widthTicks(10, 100_000, 0));
    assertEquals(Integer.MAX_VALUE, Ttl74122.widthTicks(260, 1_000_000_000, 1.0e12));
  }

  @Test
  void risingB1TriggersWhenA1IsLowAndB2IsHigh() {
    final var gate = gate();
    final var state = arm(gate, false, true, false, true);
    state.setTickCount(100);

    state.setPortValue(Ttl74122.PORT_INDEX_B1, Value.TRUE);
    gate.propagate(state);

    assertPulse(state, Value.TRUE);
    assertFalse(gate.expire(state, 108));
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
    assertTrue(gate.expire(state, 109));
    gate.propagate(state);
    assertPulse(state, Value.FALSE);
  }

  @Test
  void risingB2TriggersWhenA2IsLowAndB1IsHigh() {
    final var gate = gate();
    final var state = arm(gate, true, false, true, false);

    state.setPortValue(Ttl74122.PORT_INDEX_B2, Value.TRUE);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
  }

  @Test
  void fallingA1TriggersWhenBothAWereHighAndBothBAreHigh() {
    final var gate = gate();
    final var state = arm(gate, true, true, true, true);
    assertPulse(state, Value.FALSE);

    state.setPortValue(Ttl74122.PORT_INDEX_A1, Value.FALSE);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
  }

  @Test
  void fallingA2TriggersWhenBothAWereHighAndBothBAreHigh() {
    final var gate = gate();
    final var state = arm(gate, true, true, true, true);

    state.setPortValue(Ttl74122.PORT_INDEX_A2, Value.FALSE);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
  }

  @Test
  void bothAInputsFallingTogetherTriggers() {
    final var gate = gate();
    final var state = arm(gate, true, true, true, true);

    state.setPortValue(Ttl74122.PORT_INDEX_A1, Value.FALSE);
    state.setPortValue(Ttl74122.PORT_INDEX_A2, Value.FALSE);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
  }

  @Test
  void fallingA1DoesNotTriggerWhenA2IsAlreadyLow() {
    final var gate = gate();
    final var state = arm(gate, true, false, true, true);

    state.setPortValue(Ttl74122.PORT_INDEX_A1, Value.FALSE);
    gate.propagate(state);
    assertPulse(state, Value.FALSE);
  }

  @Test
  void risingClearTriggersWhenAnAIsLowAndBothBAreHigh() {
    final var gate = gate();
    final var state = new TestInstanceState(gate, false);
    useNineTickTiming(state);
    state.setPortValue(Ttl74122.PORT_INDEX_A1, Value.TRUE);
    state.setPortValue(Ttl74122.PORT_INDEX_A2, Value.FALSE);
    state.setPortValue(Ttl74122.PORT_INDEX_B1, Value.TRUE);
    state.setPortValue(Ttl74122.PORT_INDEX_B2, Value.TRUE);
    state.setPortValue(Ttl74122.PORT_INDEX_CLR, Value.FALSE);
    gate.propagate(state);
    assertPulse(state, Value.FALSE);

    state.setPortValue(Ttl74122.PORT_INDEX_CLR, Value.TRUE);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
  }

  @Test
  void clearEndsThePulseImmediately() {
    final var gate = gate();
    final var state = arm(gate, false, true, false, true);
    state.setPortValue(Ttl74122.PORT_INDEX_B1, Value.TRUE);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);

    state.setPortValue(Ttl74122.PORT_INDEX_CLR, Value.FALSE);
    gate.propagate(state);
    assertPulse(state, Value.FALSE);
  }

  @Test
  void retriggerRestartsTheCapturedWidth() {
    final var gate = gate();
    final var state = arm(gate, false, true, false, true);
    state.setTickCount(100);
    state.setPortValue(Ttl74122.PORT_INDEX_B1, Value.TRUE);
    gate.propagate(state);

    state.setTickCount(104);
    state.setPortValue(Ttl74122.PORT_INDEX_B1, Value.FALSE);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
    state.setPortValue(Ttl74122.PORT_INDEX_B1, Value.TRUE);
    gate.propagate(state);

    assertFalse(gate.expire(state, 112));
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
    assertTrue(gate.expire(state, 113));
    gate.propagate(state);
    assertPulse(state, Value.FALSE);
  }

  @Test
  void stableInputLevelsDoNotAbortAnActivePulse() {
    final var gate = gate();
    final var state = arm(gate, false, true, false, true);
    state.setTickCount(100);
    state.setPortValue(Ttl74122.PORT_INDEX_B1, Value.TRUE);
    gate.propagate(state);

    state.setPortValue(Ttl74122.PORT_INDEX_A1, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74122.PORT_INDEX_B1, Value.FALSE);
    state.setPortValue(Ttl74122.PORT_INDEX_B2, Value.FALSE);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);

    assertFalse(gate.expire(state, 108));
    assertTrue(gate.expire(state, 109));
    gate.propagate(state);
    assertPulse(state, Value.FALSE);
  }

  @Test
  void timingAttributesApplyOnTheNextTrigger() {
    final var gate = gate();
    final var state = arm(gate, false, true, false, true);
    state.setTickCount(100);
    state.setPortValue(Ttl74122.PORT_INDEX_B1, Value.TRUE);
    gate.propagate(state);

    state.getAttributeSet().setValue(Ttl74122.CEXT, 1_000);
    assertFalse(gate.expire(state, 101));
    gate.propagate(state);
    assertPulse(state, Value.TRUE);

    assertTrue(gate.expire(state, 109));
    gate.propagate(state);
    state.setTickCount(200);
    state.setPortValue(Ttl74122.PORT_INDEX_B1, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74122.PORT_INDEX_B1, Value.TRUE);
    gate.propagate(state);
    assertFalse(gate.expire(state, 200));
    assertTrue(gate.expire(state, 201));
  }

  @Test
  void undefinedLevelsDoNotCreateEdgesOrClearAPulse() {
    final var gate = gate();
    final var state = arm(gate, false, true, false, true);

    state.setPortValue(Ttl74122.PORT_INDEX_B1, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl74122.PORT_INDEX_B1, Value.TRUE);
    gate.propagate(state);
    assertPulse(state, Value.FALSE);

    state.setPortValue(Ttl74122.PORT_INDEX_B1, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74122.PORT_INDEX_B1, Value.TRUE);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);

    state.setPortValue(Ttl74122.PORT_INDEX_CLR, Value.UNKNOWN);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
    state.setPortValue(Ttl74122.PORT_INDEX_CLR, Value.ERROR);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
  }

  @Test
  void gatedLevelsDoNotTrigger() {
    final var gate = gate();
    final var blockedByOtherB = arm(gate, false, true, false, false);
    blockedByOtherB.setPortValue(Ttl74122.PORT_INDEX_B1, Value.TRUE);
    gate.propagate(blockedByOtherB);
    assertPulse(blockedByOtherB, Value.FALSE);

    final var blockedByBothA = arm(gate, true, true, false, true);
    blockedByBothA.setPortValue(Ttl74122.PORT_INDEX_B1, Value.TRUE);
    gate.propagate(blockedByBothA);
    assertPulse(blockedByBothA, Value.FALSE);

    final var blockedByB = arm(gate, true, true, true, false);
    blockedByB.setPortValue(Ttl74122.PORT_INDEX_A1, Value.FALSE);
    gate.propagate(blockedByB);
    assertPulse(blockedByB, Value.FALSE);

    final var heldInClear = arm(gate, false, true, false, true);
    heldInClear.setPortValue(Ttl74122.PORT_INDEX_CLR, Value.FALSE);
    heldInClear.setPortValue(Ttl74122.PORT_INDEX_B1, Value.TRUE);
    gate.propagate(heldInClear);
    assertPulse(heldInClear, Value.FALSE);
  }

  @Test
  void wrongPowerPinsForceUnknownOutputs() {
    final var gate = gate();
    final var state = new TestInstanceState(gate, true);
    state.setPortValue(Ttl74122.PORT_INDEX_A1, Value.FALSE);
    state.setPortValue(Ttl74122.PORT_INDEX_A2, Value.TRUE);
    state.setPortValue(Ttl74122.PORT_INDEX_B1, Value.FALSE);
    state.setPortValue(Ttl74122.PORT_INDEX_B2, Value.TRUE);
    state.setPortValue(Ttl74122.PORT_INDEX_CLR, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74122.PORT_INDEX_Q));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74122.PORT_INDEX_QBAR));

    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74122.PORT_INDEX_B1, Value.TRUE);
    gate.propagate(state);
    assertPulse(state, Value.TRUE);
  }

  private static Ttl74122 gate() {
    return new Ttl74122();
  }

  private static void useNineTickTiming(TestInstanceState state) {
    state.getAttributeSet().setValue(Ttl74122.REXT, NINE_TICK_REXT_KOHM);
    state.getAttributeSet().setValue(Ttl74122.CEXT, NINE_TICK_CEXT_PF);
  }

  /** Records a stable input level so the next transition can be recognized as an edge. */
  private static TestInstanceState arm(
      Ttl74122 gate, boolean inputA1, boolean inputA2, boolean inputB1, boolean inputB2) {
    final var state = new TestInstanceState(gate, false);
    useNineTickTiming(state);
    state.setPortValue(Ttl74122.PORT_INDEX_A1, inputA1 ? Value.TRUE : Value.FALSE);
    state.setPortValue(Ttl74122.PORT_INDEX_A2, inputA2 ? Value.TRUE : Value.FALSE);
    state.setPortValue(Ttl74122.PORT_INDEX_B1, inputB1 ? Value.TRUE : Value.FALSE);
    state.setPortValue(Ttl74122.PORT_INDEX_B2, inputB2 ? Value.TRUE : Value.FALSE);
    state.setPortValue(Ttl74122.PORT_INDEX_CLR, Value.TRUE);
    gate.propagate(state);
    assertPulse(state, Value.FALSE);
    return state;
  }

  private static void assertPulse(TestInstanceState state, Value output) {
    assertEquals(output, state.getPortValue(Ttl74122.PORT_INDEX_Q));
    assertEquals(output.not(), state.getPortValue(Ttl74122.PORT_INDEX_QBAR));
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
          Instance.getInstanceFor(
              factory.createComponent(Location.create(0, 0, false), attrs));
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
