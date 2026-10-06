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

/** Functional tests for the 74HC590 8-bit counter with output register. */
class Ttl74590Test {
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;
  private static final byte[] Q_PINS = {
    Ttl74590.Q0, Ttl74590.Q1, Ttl74590.Q2, Ttl74590.Q3,
    Ttl74590.Q4, Ttl74590.Q5, Ttl74590.Q6, Ttl74590.Q7
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74590();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPin(hiddenPower, Ttl74590.Q1, 10, 30, EndData.OUTPUT_ONLY);
    assertPin(hiddenPower, Ttl74590.Q2, 30, 30, EndData.OUTPUT_ONLY);
    assertPin(hiddenPower, Ttl74590.Q3, 50, 30, EndData.OUTPUT_ONLY);
    assertPin(hiddenPower, Ttl74590.Q4, 70, 30, EndData.OUTPUT_ONLY);
    assertPin(hiddenPower, Ttl74590.Q5, 90, 30, EndData.OUTPUT_ONLY);
    assertPin(hiddenPower, Ttl74590.Q6, 110, 30, EndData.OUTPUT_ONLY);
    assertPin(hiddenPower, Ttl74590.Q7, 130, 30, EndData.OUTPUT_ONLY);
    assertPin(hiddenPower, Ttl74590.RCO, 150, -30, EndData.OUTPUT_ONLY);
    assertPin(hiddenPower, Ttl74590.MRC, 130, -30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74590.CPC, 110, -30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74590.CE, 90, -30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74590.CPR, 70, -30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74590.OE, 50, -30, EndData.INPUT_ONLY);
    assertPin(hiddenPower, Ttl74590.Q0, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 150, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void bothClocksAreReported() {
    final var gate = new Ttl74590();
    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(
        new int[] {Ttl74590.pinNrToPortNr(Ttl74590.CPC), Ttl74590.pinNrToPortNr(Ttl74590.CPR)},
        gate.clockPinIndex(null));
  }

  @Test
  void masterResetClearsTheCounterWithoutTouchingTheRegister() {
    final var gate = new Ttl74590();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    assertRegister(state, 0);
    assertBit(state, Ttl74590.RCO, Value.TRUE);

    count(gate, state, 5);
    assertRegister(state, 0);
    capture(gate, state);
    assertRegister(state, 5);

    set(state, Ttl74590.MRC, Value.FALSE);
    gate.propagate(state);
    assertRegister(state, 5);
    assertBit(state, Ttl74590.RCO, Value.TRUE);

    set(state, Ttl74590.CPC, Value.TRUE);
    gate.propagate(state);
    set(state, Ttl74590.MRC, Value.TRUE);
    gate.propagate(state);
    capture(gate, state);
    assertRegister(state, 0);

    count(gate, state, 1);
    capture(gate, state);
    assertRegister(state, 1);
  }

  @Test
  void countingAdvancesOnlyOnARisingEnabledCounterClock() {
    final var gate = new Ttl74590();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);

    set(state, Ttl74590.CE, Value.TRUE);
    rise(gate, state, Ttl74590.CPC);
    fall(gate, state, Ttl74590.CPC);
    capture(gate, state);
    assertRegister(state, 0);

    set(state, Ttl74590.CE, Value.FALSE);
    set(state, Ttl74590.CPC, Value.TRUE);
    gate.propagate(state);
    fall(gate, state, Ttl74590.CPC);
    capture(gate, state);
    assertRegister(state, 1);

    fall(gate, state, Ttl74590.CPC);
    capture(gate, state);
    assertRegister(state, 1);

    count(gate, state, 1);
    capture(gate, state);
    assertRegister(state, 2);
  }

  @Test
  void countEnableEdgesDoNotClock() {
    final var gate = new Ttl74590();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    count(gate, state, 3);
    capture(gate, state);
    assertRegister(state, 3);

    set(state, Ttl74590.CE, Value.TRUE);
    rise(gate, state, Ttl74590.CPC);
    set(state, Ttl74590.CE, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl74590.CE, Value.TRUE);
    gate.propagate(state);
    fall(gate, state, Ttl74590.CPC);
    set(state, Ttl74590.CE, Value.FALSE);
    gate.propagate(state);
    capture(gate, state);
    assertRegister(state, 3);

    rise(gate, state, Ttl74590.CPC);
    capture(gate, state);
    assertRegister(state, 4);
  }

  @Test
  void registerStoresTheCounterOnTheRisingRegisterClock() {
    final var gate = new Ttl74590();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    count(gate, state, 4);
    assertRegister(state, 0);

    rise(gate, state, Ttl74590.CPR);
    assertRegister(state, 4);

    count(gate, state, 3);
    assertRegister(state, 4);
    fall(gate, state, Ttl74590.CPR);
    rise(gate, state, Ttl74590.CPR);
    assertRegister(state, 7);
  }

  @Test
  void tiedClocksLeaveTheRegisterOneCountBehind() {
    final var gate = new Ttl74590();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    set(state, Ttl74590.CE, Value.FALSE);

    for (var code = 0; code < 5; code++) {
      set(state, Ttl74590.CPC, Value.FALSE);
      set(state, Ttl74590.CPR, Value.FALSE);
      gate.propagate(state);
      set(state, Ttl74590.CPC, Value.TRUE);
      set(state, Ttl74590.CPR, Value.TRUE);
      gate.propagate(state);
      assertRegister(state, code);
    }
  }

  @Test
  void counterWrapsAndRippleCarryIsLowOnlyAt255() {
    final var gate = new Ttl74590();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    assertBit(state, Ttl74590.RCO, Value.TRUE);

    count(gate, state, 254);
    assertBit(state, Ttl74590.RCO, Value.TRUE);
    count(gate, state, 1);
    assertBit(state, Ttl74590.RCO, Value.FALSE);
    capture(gate, state);
    assertRegister(state, 255);

    count(gate, state, 1);
    assertBit(state, Ttl74590.RCO, Value.TRUE);
    capture(gate, state);
    assertRegister(state, 0);
  }

  @Test
  void rippleCarryStaysLowAt255WhenCountingIsInhibited() {
    final var gate = new Ttl74590();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    count(gate, state, 255);
    assertBit(state, Ttl74590.RCO, Value.FALSE);

    set(state, Ttl74590.CE, Value.TRUE);
    gate.propagate(state);
    assertBit(state, Ttl74590.RCO, Value.FALSE);
    rise(gate, state, Ttl74590.CPC);
    fall(gate, state, Ttl74590.CPC);
    capture(gate, state);
    assertRegister(state, 255);
    assertBit(state, Ttl74590.RCO, Value.FALSE);
  }

  @Test
  void outputEnableReleasesTheRegisterPins() {
    final var gate = new Ttl74590();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    count(gate, state, 0xA5);
    capture(gate, state);
    assertRegister(state, 0xA5);
    assertBit(state, Ttl74590.RCO, Value.TRUE);

    set(state, Ttl74590.OE, Value.TRUE);
    gate.propagate(state);
    for (final var pin : Q_PINS) {
      assertBit(state, pin, Value.UNKNOWN);
    }
    assertBit(state, Ttl74590.RCO, Value.TRUE);

    set(state, Ttl74590.OE, Value.FALSE);
    gate.propagate(state);
    assertRegister(state, 0xA5);
  }

  @Test
  void uncertainControlsChangeOnlyTheBitsThatDisagree() {
    final var gate = new Ttl74590();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    set(state, Ttl74590.MRC, Value.ERROR);
    gate.propagate(state);
    capture(gate, state);
    assertRegister(state, 0);
    assertBit(state, Ttl74590.RCO, Value.TRUE);

    set(state, Ttl74590.MRC, Value.TRUE);
    count(gate, state, 5);
    capture(gate, state);
    set(state, Ttl74590.MRC, Value.UNKNOWN);
    gate.propagate(state);
    capture(gate, state);
    assertBit(state, Ttl74590.Q0, Value.UNKNOWN);
    assertBit(state, Ttl74590.Q1, Value.FALSE);
    assertBit(state, Ttl74590.Q2, Value.UNKNOWN);
    assertBit(state, Ttl74590.Q3, Value.FALSE);
    assertBit(state, Ttl74590.RCO, Value.TRUE);

    set(state, Ttl74590.MRC, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl74590.MRC, Value.TRUE);
    count(gate, state, 1);
    capture(gate, state);
    set(state, Ttl74590.CE, Value.ERROR);
    rise(gate, state, Ttl74590.CPC);
    capture(gate, state);
    assertBit(state, Ttl74590.Q0, Value.ERROR);
    assertBit(state, Ttl74590.Q1, Value.ERROR);
    assertBit(state, Ttl74590.Q2, Value.FALSE);
    assertBit(state, Ttl74590.RCO, Value.TRUE);

    set(state, Ttl74590.OE, Value.UNKNOWN);
    gate.propagate(state);
    assertBit(state, Ttl74590.Q2, Value.UNKNOWN);
    assertBit(state, Ttl74590.RCO, Value.TRUE);
    set(state, Ttl74590.OE, Value.ERROR);
    gate.propagate(state);
    assertBit(state, Ttl74590.Q2, Value.ERROR);
    assertBit(state, Ttl74590.RCO, Value.TRUE);
  }

  @Test
  void unknownClockEdgesDoNotCount() {
    final var gate = new Ttl74590();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    set(state, Ttl74590.CE, Value.FALSE);

    set(state, Ttl74590.CPC, Value.UNKNOWN);
    gate.propagate(state);
    set(state, Ttl74590.CPC, Value.TRUE);
    gate.propagate(state);
    capture(gate, state);
    assertRegister(state, 0);

    set(state, Ttl74590.CPC, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl74590.CPC, Value.TRUE);
    gate.propagate(state);
    capture(gate, state);
    assertRegister(state, 1);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74590();
    final var state = new TestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    reset(gate, state);
    count(gate, state, 8);
    capture(gate, state);
    assertRegister(state, 8);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertBit(state, Ttl74590.Q0, Value.UNKNOWN);
    assertBit(state, Ttl74590.RCO, Value.UNKNOWN);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertRegister(state, 8);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertBit(state, Ttl74590.Q3, Value.UNKNOWN);
    assertBit(state, Ttl74590.RCO, Value.UNKNOWN);
  }

  private static Instance createInstance(InstanceFactory factory, boolean showPowerPins) {
    final var attrs = factory.createAttributeSet();
    attrs.setValue(TtlLibrary.VCC_GND, showPowerPins);
    return Instance.getInstanceFor(factory.createComponent(Location.create(0, 0, false), attrs));
  }

  private static void assertPin(Instance instance, byte pin, int x, int y, int type) {
    assertPort(instance, Ttl74590.pinNrToPortNr(pin), x, y, type);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void reset(Ttl74590 gate, TestInstanceState state) {
    set(state, Ttl74590.MRC, Value.TRUE);
    set(state, Ttl74590.CE, Value.TRUE);
    set(state, Ttl74590.OE, Value.FALSE);
    set(state, Ttl74590.CPC, Value.FALSE);
    set(state, Ttl74590.CPR, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl74590.MRC, Value.FALSE);
    gate.propagate(state);
    set(state, Ttl74590.MRC, Value.TRUE);
    gate.propagate(state);
    capture(gate, state);
  }

  private static void count(Ttl74590 gate, TestInstanceState state, int pulses) {
    set(state, Ttl74590.CE, Value.FALSE);
    for (var i = 0; i < pulses; i++) {
      rise(gate, state, Ttl74590.CPC);
      fall(gate, state, Ttl74590.CPC);
    }
  }

  private static void capture(Ttl74590 gate, TestInstanceState state) {
    fall(gate, state, Ttl74590.CPR);
    rise(gate, state, Ttl74590.CPR);
  }

  private static void rise(Ttl74590 gate, TestInstanceState state, byte pin) {
    set(state, pin, Value.FALSE);
    gate.propagate(state);
    set(state, pin, Value.TRUE);
    gate.propagate(state);
  }

  private static void fall(Ttl74590 gate, TestInstanceState state, byte pin) {
    set(state, pin, Value.FALSE);
    gate.propagate(state);
  }

  private static void set(TestInstanceState state, byte pin, Value value) {
    state.setPortValue(Ttl74590.pinNrToPortNr(pin), value);
  }

  private static void assertRegister(TestInstanceState state, int code) {
    for (var bit = 0; bit < Q_PINS.length; bit++) {
      final var expected = ((code >> bit) & 1) == 0 ? Value.FALSE : Value.TRUE;
      assertBit(state, Q_PINS[bit], expected);
    }
  }

  private static void assertBit(TestInstanceState state, byte pin, Value expected) {
    assertEquals(expected, state.getPortValue(Ttl74590.pinNrToPortNr(pin)), "pin " + pin);
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
