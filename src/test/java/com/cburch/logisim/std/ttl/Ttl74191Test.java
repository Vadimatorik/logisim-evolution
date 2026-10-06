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

/** Functional tests for the 74HC191 synchronous presettable binary up/down counter. */
class Ttl74191Test {
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;
  private static final BitWidth WIDTH = BitWidth.create(4);
  private static final int[] OUTPUTS = {
    Ttl74191.PORT_INDEX_Q0,
    Ttl74191.PORT_INDEX_Q1,
    Ttl74191.PORT_INDEX_Q2,
    Ttl74191.PORT_INDEX_Q3
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74191();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74191.PORT_INDEX_D1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74191.PORT_INDEX_Q1, 30, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74191.PORT_INDEX_Q0, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74191.PORT_INDEX_CE, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74191.PORT_INDEX_UD, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74191.PORT_INDEX_Q2, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74191.PORT_INDEX_Q3, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74191.PORT_INDEX_D3, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74191.PORT_INDEX_D2, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74191.PORT_INDEX_PL, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74191.PORT_INDEX_TC, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74191.PORT_INDEX_RC, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74191.PORT_INDEX_CP, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74191.PORT_INDEX_D0, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 150, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void parallelLoadTracksDataWithoutAClock() {
    final var gate = new Ttl74191();
    final var state = new TestInstanceState(gate, false);
    hold(state, Value.FALSE, Value.TRUE, Value.FALSE);

    for (final var value : new int[] {0x0, 0xF, 0x5, 0xA, 0x1, 0x2, 0x4, 0x8}) {
      present(state, value);
      state.setPortValue(Ttl74191.PORT_INDEX_PL, Value.FALSE);
      gate.propagate(state);
      assertCount(state, value);
    }

    present(state, 0x3);
    state.setPortValue(Ttl74191.PORT_INDEX_PL, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0x8);
  }

  @Test
  void parallelLoadOverridesARisingClock() {
    final var gate = new Ttl74191();
    final var state = new TestInstanceState(gate, false);
    load(gate, state, 0, Value.FALSE, Value.FALSE);

    present(state, 0x3);
    state.setPortValue(Ttl74191.PORT_INDEX_PL, Value.FALSE);
    state.setPortValue(Ttl74191.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74191.PORT_INDEX_CP, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0x3);

    state.setPortValue(Ttl74191.PORT_INDEX_PL, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0x3);
  }

  @Test
  void countsUpAndWrapsFromFifteenToZero() {
    final var gate = new Ttl74191();
    final var state = new TestInstanceState(gate, false);
    load(gate, state, 0, Value.FALSE, Value.FALSE);

    for (var count = 1; count <= 15; count++) {
      rising(gate, state);
      assertCount(state, count);
    }
    rising(gate, state);
    assertCount(state, 0);
  }

  @Test
  void countsDownAndWrapsFromZeroToFifteen() {
    final var gate = new Ttl74191();
    final var state = new TestInstanceState(gate, false);
    load(gate, state, 0, Value.TRUE, Value.FALSE);

    rising(gate, state);
    assertCount(state, 15);
    rising(gate, state);
    assertCount(state, 14);

    load(gate, state, 1, Value.TRUE, Value.FALSE);
    rising(gate, state);
    assertCount(state, 0);
    rising(gate, state);
    assertCount(state, 15);
  }

  @Test
  void highCountEnableAndAFallingEdgeHoldTheCount() {
    final var gate = new Ttl74191();
    final var state = new TestInstanceState(gate, false);
    load(gate, state, 4, Value.FALSE, Value.TRUE);

    rising(gate, state);
    assertCount(state, 4);

    state.setPortValue(Ttl74191.PORT_INDEX_CE, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74191.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 4);

    rising(gate, state);
    assertCount(state, 5);
  }

  @Test
  void terminalCountFollowsDirectionWithoutAClock() {
    final var gate = new Ttl74191();
    final var state = new TestInstanceState(gate, false);
    load(gate, state, 15, Value.FALSE, Value.TRUE);
    assertCount(state, 15);
    assertEquals(Value.TRUE, state.getPortValue(Ttl74191.PORT_INDEX_TC));
    assertEquals(Value.TRUE, state.getPortValue(Ttl74191.PORT_INDEX_RC));

    state.setPortValue(Ttl74191.PORT_INDEX_UD, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 15);
    assertEquals(Value.FALSE, state.getPortValue(Ttl74191.PORT_INDEX_TC));

    load(gate, state, 0, Value.TRUE, Value.TRUE);
    assertEquals(Value.TRUE, state.getPortValue(Ttl74191.PORT_INDEX_TC));

    state.setPortValue(Ttl74191.PORT_INDEX_UD, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 0);
    assertEquals(Value.FALSE, state.getPortValue(Ttl74191.PORT_INDEX_TC));
  }

  @Test
  void rippleClockPulsesOnlyWhileEnabledAtTerminalCount() {
    final var gate = new Ttl74191();
    final var state = new TestInstanceState(gate, false);
    load(gate, state, 15, Value.FALSE, Value.TRUE);

    state.setPortValue(Ttl74191.PORT_INDEX_CP, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 15);
    assertEquals(Value.TRUE, state.getPortValue(Ttl74191.PORT_INDEX_RC));

    state.setPortValue(Ttl74191.PORT_INDEX_CE, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 15);
    assertEquals(Value.TRUE, state.getPortValue(Ttl74191.PORT_INDEX_RC));

    state.setPortValue(Ttl74191.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 15);
    assertEquals(Value.TRUE, state.getPortValue(Ttl74191.PORT_INDEX_TC));
    assertEquals(Value.FALSE, state.getPortValue(Ttl74191.PORT_INDEX_RC));

    state.setPortValue(Ttl74191.PORT_INDEX_CP, Value.TRUE);
    gate.propagate(state);
    assertCount(state, 0);
    assertEquals(Value.FALSE, state.getPortValue(Ttl74191.PORT_INDEX_TC));
    assertEquals(Value.TRUE, state.getPortValue(Ttl74191.PORT_INDEX_RC));

    load(gate, state, 7, Value.FALSE, Value.FALSE);
    assertEquals(Value.FALSE, state.getPortValue(Ttl74191.PORT_INDEX_TC));
    assertEquals(Value.TRUE, state.getPortValue(Ttl74191.PORT_INDEX_RC));
  }

  @Test
  void unknownControlsDoNotLoadOrCount() {
    final var gate = new Ttl74191();
    final var state = new TestInstanceState(gate, false);
    load(gate, state, 5, Value.FALSE, Value.FALSE);

    present(state, 9);
    state.setPortValue(Ttl74191.PORT_INDEX_PL, Value.UNKNOWN);
    gate.propagate(state);
    assertCount(state, 5);

    state.setPortValue(Ttl74191.PORT_INDEX_PL, Value.TRUE);
    state.setPortValue(Ttl74191.PORT_INDEX_CE, Value.UNKNOWN);
    rising(gate, state);
    assertCount(state, 5);

    state.setPortValue(Ttl74191.PORT_INDEX_CE, Value.FALSE);
    state.setPortValue(Ttl74191.PORT_INDEX_UD, Value.UNKNOWN);
    rising(gate, state);
    assertCount(state, 5);

    state.setPortValue(Ttl74191.PORT_INDEX_UD, Value.FALSE);
    state.setPortValue(Ttl74191.PORT_INDEX_CP, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl74191.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    assertCount(state, 5);

    rising(gate, state);
    assertCount(state, 6);
  }

  @Test
  void unknownLoadBitStaysOnThatOutput() {
    final var gate = new Ttl74191();
    final var state = new TestInstanceState(gate, false);
    hold(state, Value.FALSE, Value.TRUE, Value.FALSE);
    state.setPortValue(Ttl74191.PORT_INDEX_D0, Value.UNKNOWN);
    state.setPortValue(Ttl74191.PORT_INDEX_D1, Value.FALSE);
    state.setPortValue(Ttl74191.PORT_INDEX_D2, Value.TRUE);
    state.setPortValue(Ttl74191.PORT_INDEX_D3, Value.FALSE);
    state.setPortValue(Ttl74191.PORT_INDEX_PL, Value.FALSE);
    gate.propagate(state);

    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74191.PORT_INDEX_Q0));
    assertEquals(Value.FALSE, state.getPortValue(Ttl74191.PORT_INDEX_Q1));
    assertEquals(Value.TRUE, state.getPortValue(Ttl74191.PORT_INDEX_Q2));
    assertEquals(Value.FALSE, state.getPortValue(Ttl74191.PORT_INDEX_Q3));
    assertEquals(Value.FALSE, state.getPortValue(Ttl74191.PORT_INDEX_TC));
    assertEquals(Value.TRUE, state.getPortValue(Ttl74191.PORT_INDEX_RC));
  }

  @Test
  void undefinedCountBecomesUnknownOnTheNextClock() {
    final var gate = new Ttl74191();
    final var state = new TestInstanceState(gate, false);
    load(gate, state, 0, Value.FALSE, Value.FALSE);
    final var data = new TtlRegisterData(WIDTH);
    data.setValue(Value.createUnknown(WIDTH));
    state.setData(data);

    rising(gate, state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74191.PORT_INDEX_Q0));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74191.PORT_INDEX_Q3));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74191.PORT_INDEX_TC));

    load(gate, state, 0, Value.FALSE, Value.TRUE);
    assertCount(state, 0);
  }

  @Test
  void errorInTheCountBecomesErrorOnTheNextClock() {
    final var gate = new Ttl74191();
    final var state = new TestInstanceState(gate, false);
    load(gate, state, 0, Value.FALSE, Value.FALSE);
    final var data = new TtlRegisterData(WIDTH);
    final var bits = Value.createKnown(WIDTH, 0).getAll();
    bits[0] = Value.ERROR;
    data.setValue(Value.create(bits));
    state.setData(data);

    rising(gate, state);
    assertEquals(Value.ERROR, state.getPortValue(Ttl74191.PORT_INDEX_Q0));
    assertEquals(Value.ERROR, state.getPortValue(Ttl74191.PORT_INDEX_Q3));

    load(gate, state, 0, Value.TRUE, Value.TRUE);
    assertCount(state, 0);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74191();
    final var state = new TestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    load(gate, state, 8, Value.FALSE, Value.TRUE);
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
  void clockMetadataNamesTheCountClock() {
    final var gate = new Ttl74191();
    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(new int[] {Ttl74191.PORT_INDEX_CP}, gate.clockPinIndex(null));
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

  private static void hold(TestInstanceState state, Value direction, Value countEnable, Value clock) {
    state.setPortValue(Ttl74191.PORT_INDEX_UD, direction);
    state.setPortValue(Ttl74191.PORT_INDEX_CE, countEnable);
    state.setPortValue(Ttl74191.PORT_INDEX_CP, clock);
    state.setPortValue(Ttl74191.PORT_INDEX_PL, Value.TRUE);
  }

  private static void present(TestInstanceState state, int value) {
    state.setPortValue(Ttl74191.PORT_INDEX_D0, bit(value, 0));
    state.setPortValue(Ttl74191.PORT_INDEX_D1, bit(value, 1));
    state.setPortValue(Ttl74191.PORT_INDEX_D2, bit(value, 2));
    state.setPortValue(Ttl74191.PORT_INDEX_D3, bit(value, 3));
  }

  private static void load(
      Ttl74191 gate, TestInstanceState state, int value, Value direction, Value countEnable) {
    hold(state, direction, countEnable, Value.FALSE);
    present(state, value);
    state.setPortValue(Ttl74191.PORT_INDEX_PL, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74191.PORT_INDEX_PL, Value.TRUE);
    gate.propagate(state);
  }

  private static void rising(Ttl74191 gate, TestInstanceState state) {
    state.setPortValue(Ttl74191.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74191.PORT_INDEX_CP, Value.TRUE);
    gate.propagate(state);
  }

  private static void assertCount(TestInstanceState state, int count) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      assertEquals(bit(count, index), state.getPortValue(OUTPUTS[index]), "Q" + index);
    }
    final var terminal = expectedTerminal(count, state.getPortValue(Ttl74191.PORT_INDEX_UD));
    assertEquals(terminal, state.getPortValue(Ttl74191.PORT_INDEX_TC), "TC");
    assertEquals(
        expectedRipple(
            terminal,
            state.getPortValue(Ttl74191.PORT_INDEX_CE),
            state.getPortValue(Ttl74191.PORT_INDEX_CP)),
        state.getPortValue(Ttl74191.PORT_INDEX_RC),
        "RC");
  }

  private static Value expectedTerminal(int count, Value direction) {
    if (direction == Value.FALSE) return count == 15 ? Value.TRUE : Value.FALSE;
    if (direction == Value.TRUE) return count == 0 ? Value.TRUE : Value.FALSE;
    if (count != 0 && count != 15) return Value.FALSE;
    return Value.UNKNOWN;
  }

  private static Value expectedRipple(Value terminal, Value countEnable, Value clock) {
    if (terminal == Value.TRUE && countEnable == Value.FALSE && clock == Value.FALSE) {
      return Value.FALSE;
    }
    if (terminal == Value.FALSE || countEnable == Value.TRUE || clock == Value.TRUE) {
      return Value.TRUE;
    }
    return Value.UNKNOWN;
  }

  private static Value bit(int value, int index) {
    return ((value >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
  }

  private static void assertUnknownOutputs(TestInstanceState state) {
    for (final var port : OUTPUTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(port));
    }
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74191.PORT_INDEX_TC));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74191.PORT_INDEX_RC));
  }

  private static final class TestInstanceState implements InstanceState {
    private final AttributeSet attrs;
    private final Instance instance;
    private final Map<Integer, Value> portValues = new HashMap<>();
    private InstanceData data;

    private TestInstanceState(InstanceFactory factory, boolean showPowerPins) {
      attrs = factory.createAttributeSet();
      attrs.setValue(TtlLibrary.VCC_GND, showPowerPins);
      instance = Instance.getInstanceFor(factory.createComponent(Location.create(0, 0, false), attrs));
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
