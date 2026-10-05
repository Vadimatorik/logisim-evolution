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

/** Functional tests for the 74HC490 dual 4-bit decade counter. */
class Ttl74490Test {
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;
  private static final BitWidth NIBBLE = BitWidth.create(4);

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74490();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74490.PORT_INDEX_1CLK, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74490.PORT_INDEX_1CLR, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74490.PORT_INDEX_1QA, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74490.PORT_INDEX_1SET9, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74490.PORT_INDEX_1QB, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74490.PORT_INDEX_1QC, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74490.PORT_INDEX_1QD, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74490.PORT_INDEX_2QD, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74490.PORT_INDEX_2QC, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74490.PORT_INDEX_2QB, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74490.PORT_INDEX_2SET9, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74490.PORT_INDEX_2QA, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74490.PORT_INDEX_2CLR, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74490.PORT_INDEX_2CLK, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 150, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void eachHalfCountsZeroThroughNineOnItsOwnFallingClock() {
    final var gate = new Ttl74490();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);

    for (var code = 0; code < 10; code++) {
      assertHalf(state, 1, code);
      assertHalf(state, 2, 0);
      rise(gate, state, Ttl74490.PORT_INDEX_1CLK);
      assertHalf(state, 1, code);
      fall(gate, state, Ttl74490.PORT_INDEX_1CLK);
    }
    assertHalf(state, 1, 0);
    assertHalf(state, 2, 0);

    for (var code = 0; code < 10; code++) {
      assertHalf(state, 2, code);
      assertHalf(state, 1, 0);
      rise(gate, state, Ttl74490.PORT_INDEX_2CLK);
      assertHalf(state, 2, code);
      fall(gate, state, Ttl74490.PORT_INDEX_2CLK);
    }
    assertHalf(state, 1, 0);
    assertHalf(state, 2, 0);
  }

  @Test
  void clearAndSetToNineAreAsynchronousAndSetToNineWins() {
    final var gate = new Ttl74490();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    clock(gate, state, Ttl74490.PORT_INDEX_1CLK, 4);
    clock(gate, state, Ttl74490.PORT_INDEX_2CLK, 6);
    assertHalf(state, 1, 4);
    assertHalf(state, 2, 6);

    state.setPortValue(Ttl74490.PORT_INDEX_1CLK, Value.TRUE);
    state.setPortValue(Ttl74490.PORT_INDEX_1CLR, Value.TRUE);
    gate.propagate(state);
    assertHalf(state, 1, 0);
    assertHalf(state, 2, 6);
    fall(gate, state, Ttl74490.PORT_INDEX_1CLK);
    assertHalf(state, 1, 0);

    state.setPortValue(Ttl74490.PORT_INDEX_1CLR, Value.FALSE);
    state.setPortValue(Ttl74490.PORT_INDEX_2SET9, Value.TRUE);
    gate.propagate(state);
    assertHalf(state, 1, 0);
    assertHalf(state, 2, 9);

    state.setPortValue(Ttl74490.PORT_INDEX_1CLR, Value.TRUE);
    state.setPortValue(Ttl74490.PORT_INDEX_1SET9, Value.TRUE);
    state.setPortValue(Ttl74490.PORT_INDEX_2CLR, Value.TRUE);
    gate.propagate(state);
    assertHalf(state, 1, 9);
    assertHalf(state, 2, 9);

    state.setPortValue(Ttl74490.PORT_INDEX_1SET9, Value.FALSE);
    gate.propagate(state);
    assertHalf(state, 1, 0);
    assertHalf(state, 2, 9);
  }

  @Test
  void unspecifiedCodesReturnToZero() {
    final var gate = new Ttl74490();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    forceHalf(state, 0, 10);
    forceHalf(state, 1, 15);
    fallFromHigh(gate, state, Ttl74490.PORT_INDEX_1CLK);
    fallFromHigh(gate, state, Ttl74490.PORT_INDEX_2CLK);
    assertHalf(state, 1, 0);
    assertHalf(state, 2, 0);
  }

  @Test
  void unknownControlsStayInactiveAndFallingEdgesStillCount() {
    final var gate = new Ttl74490();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    clock(gate, state, Ttl74490.PORT_INDEX_1CLK, 3);

    state.setPortValue(Ttl74490.PORT_INDEX_1CLR, Value.UNKNOWN);
    state.setPortValue(Ttl74490.PORT_INDEX_1SET9, Value.UNKNOWN);
    gate.propagate(state);
    assertHalf(state, 1, 3);

    fallFromHigh(gate, state, Ttl74490.PORT_INDEX_1CLK);
    assertHalf(state, 1, 4);
  }

  @Test
  void unknownAndErrorCodesFollowTheNextClock() {
    final var gate = new Ttl74490();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    forceHalf(state, 0, Value.createUnknown(NIBBLE));
    fallFromHigh(gate, state, Ttl74490.PORT_INDEX_1CLK);
    assertHalfValue(state, 1, Value.UNKNOWN);

    final var bits = Value.createKnown(NIBBLE, 0).getAll();
    bits[0] = Value.ERROR;
    forceHalf(state, 0, Value.create(bits));
    fallFromHigh(gate, state, Ttl74490.PORT_INDEX_1CLK);
    assertHalfValue(state, 1, Value.ERROR);
    assertHalf(state, 2, 0);

    state.setPortValue(Ttl74490.PORT_INDEX_1CLR, Value.TRUE);
    gate.propagate(state);
    assertHalf(state, 1, 0);
  }

  @Test
  void errorOnOneControlAffectsOnlyThatHalf() {
    final var gate = new Ttl74490();
    final var state = new TestInstanceState(gate, false);
    reset(gate, state);
    clock(gate, state, Ttl74490.PORT_INDEX_1CLK, 2);
    clock(gate, state, Ttl74490.PORT_INDEX_2CLK, 5);

    state.setPortValue(Ttl74490.PORT_INDEX_1SET9, Value.ERROR);
    gate.propagate(state);
    assertHalfValue(state, 1, Value.ERROR);
    assertHalf(state, 2, 5);

    state.setPortValue(Ttl74490.PORT_INDEX_1SET9, Value.FALSE);
    state.setPortValue(Ttl74490.PORT_INDEX_2CLR, Value.ERROR);
    gate.propagate(state);
    assertHalf(state, 1, 2);
    assertHalfValue(state, 2, Value.ERROR);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74490();
    final var state = new TestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    reset(gate, state);
    clock(gate, state, Ttl74490.PORT_INDEX_1CLK, 8);
    clock(gate, state, Ttl74490.PORT_INDEX_2CLK, 3);
    assertHalf(state, 1, 8);
    assertHalf(state, 2, 3);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertHalfValue(state, 1, Value.UNKNOWN);
    assertHalfValue(state, 2, Value.UNKNOWN);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertHalf(state, 1, 8);
    assertHalf(state, 2, 3);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertHalfValue(state, 1, Value.UNKNOWN);
    assertHalfValue(state, 2, Value.UNKNOWN);
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

  private static void reset(Ttl74490 gate, TestInstanceState state) {
    state.setPortValue(Ttl74490.PORT_INDEX_1CLK, Value.FALSE);
    state.setPortValue(Ttl74490.PORT_INDEX_2CLK, Value.FALSE);
    state.setPortValue(Ttl74490.PORT_INDEX_1SET9, Value.FALSE);
    state.setPortValue(Ttl74490.PORT_INDEX_2SET9, Value.FALSE);
    state.setPortValue(Ttl74490.PORT_INDEX_1CLR, Value.TRUE);
    state.setPortValue(Ttl74490.PORT_INDEX_2CLR, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl74490.PORT_INDEX_1CLR, Value.FALSE);
    state.setPortValue(Ttl74490.PORT_INDEX_2CLR, Value.FALSE);
    gate.propagate(state);
  }

  private static void rise(Ttl74490 gate, TestInstanceState state, int clockPort) {
    state.setPortValue(clockPort, Value.TRUE);
    gate.propagate(state);
  }

  private static void fall(Ttl74490 gate, TestInstanceState state, int clockPort) {
    state.setPortValue(clockPort, Value.FALSE);
    gate.propagate(state);
  }

  /** Drives a falling edge when the clock may already be low, as after an unknown control. */
  private static void fallFromHigh(Ttl74490 gate, TestInstanceState state, int clockPort) {
    rise(gate, state, clockPort);
    fall(gate, state, clockPort);
  }

  private static void clock(Ttl74490 gate, TestInstanceState state, int clockPort, int pulses) {
    for (var i = 0; i < pulses; i++) {
      rise(gate, state, clockPort);
      fall(gate, state, clockPort);
    }
  }

  private static void forceHalf(TestInstanceState state, int which, int code) {
    forceHalf(state, which, Value.createKnown(NIBBLE, code));
  }

  private static void forceHalf(TestInstanceState state, int which, Value value) {
    final var data = (TtlRegisterData) state.getData();
    data.setValue(which, value);
  }

  private static void assertHalf(TestInstanceState state, int half, int code) {
    assertHalfValue(state, half, Value.createKnown(NIBBLE, code));
  }

  private static void assertHalfValue(TestInstanceState state, int half, Value expected) {
    final int qa;
    final int qb;
    final int qc;
    final int qd;
    if (half == 1) {
      qa = Ttl74490.PORT_INDEX_1QA;
      qb = Ttl74490.PORT_INDEX_1QB;
      qc = Ttl74490.PORT_INDEX_1QC;
      qd = Ttl74490.PORT_INDEX_1QD;
    } else {
      qa = Ttl74490.PORT_INDEX_2QA;
      qb = Ttl74490.PORT_INDEX_2QB;
      qc = Ttl74490.PORT_INDEX_2QC;
      qd = Ttl74490.PORT_INDEX_2QD;
    }
    if (expected.isFullyDefined()) {
      final var code = expected.toLongValue();
      assertEquals(bit(code, 0), state.getPortValue(qa), "half " + half + " QA");
      assertEquals(bit(code, 1), state.getPortValue(qb), "half " + half + " QB");
      assertEquals(bit(code, 2), state.getPortValue(qc), "half " + half + " QC");
      assertEquals(bit(code, 3), state.getPortValue(qd), "half " + half + " QD");
    } else {
      final var bit = expected.get(0);
      assertEquals(bit, state.getPortValue(qa), "half " + half);
      assertEquals(bit, state.getPortValue(qb), "half " + half);
      assertEquals(bit, state.getPortValue(qc), "half " + half);
      assertEquals(bit, state.getPortValue(qd), "half " + half);
    }
  }

  private static Value bit(long code, int index) {
    return ((code >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
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
