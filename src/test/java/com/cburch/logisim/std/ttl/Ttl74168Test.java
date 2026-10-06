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
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Functional tests for the 74HC168 synchronous BCD decade up/down counter. */
class Ttl74168Test {
  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;
  private static final BitWidth WIDTH = BitWidth.create(4);

  private boolean savedStartup;

  @BeforeEach
  void useUnknownStartup() {
    savedStartup = AppPreferences.Memory_Startup_Unknown.get();
    setStartup(true);
  }

  @AfterEach
  void restoreStartup() {
    setStartup(savedStartup);
  }

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl74168();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74168.PORT_INDEX_UD, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74168.PORT_INDEX_CP, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74168.PORT_INDEX_D0, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74168.PORT_INDEX_D1, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74168.PORT_INDEX_D2, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74168.PORT_INDEX_D3, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74168.PORT_INDEX_CEP, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74168.PORT_INDEX_PE, 150, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74168.PORT_INDEX_CET, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74168.PORT_INDEX_Q3, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74168.PORT_INDEX_Q2, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74168.PORT_INDEX_Q1, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74168.PORT_INDEX_Q0, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74168.PORT_INDEX_TC, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(16, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 150, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void clockIsTheRisingCpPort() {
    final var gate = new Ttl74168();
    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(new int[] {Ttl74168.PORT_INDEX_CP}, gate.clockPinIndex(null));
  }

  @Test
  void unknownStartupStaysUnknownUntilALoad() {
    final var gate = new Ttl74168();
    final var state = new TestInstanceState(gate, false);
    controls(state, Value.TRUE, Value.TRUE, Value.FALSE, Value.TRUE);
    state.setPortValue(Ttl74168.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    assertAll(state, Value.UNKNOWN);
    assertEquals(Value.UNKNOWN, terminal(state));

    load(gate, state, 6);
    assertKnown(state, 6);
  }

  @Test
  void knownStartupIsZeroAndTerminalCountFollowsIt() {
    setStartup(false);
    final var gate = new Ttl74168();
    final var state = new TestInstanceState(gate, false);
    controls(state, Value.FALSE, Value.TRUE, Value.FALSE, Value.TRUE);
    state.setPortValue(Ttl74168.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    assertKnown(state, 0);
    assertEquals(Value.FALSE, terminal(state));

    state.setPortValue(Ttl74168.PORT_INDEX_UD, Value.TRUE);
    gate.propagate(state);
    assertKnown(state, 0);
    assertEquals(Value.TRUE, terminal(state));
  }

  @Test
  void synchronousLoadOverridesTheCountEnables() {
    final var gate = new Ttl74168();
    final var state = new TestInstanceState(gate, false);
    load(gate, state, 0);

    data(state, 5);
    controls(state, Value.TRUE, Value.FALSE, Value.FALSE, Value.FALSE);
    pulse(gate, state);
    assertKnown(state, 5);
    assertEquals(Value.TRUE, terminal(state));

    data(state, 9);
    pulse(gate, state);
    assertKnown(state, 9);
    assertEquals(Value.FALSE, terminal(state));
  }

  @Test
  void countsUpAndDownThroughTheDecade() {
    final var gate = new Ttl74168();
    final var state = new TestInstanceState(gate, false);
    load(gate, state, 0);
    controls(state, Value.TRUE, Value.FALSE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertKnown(state, 0);
    assertEquals(Value.TRUE, terminal(state));

    for (var count = 1; count <= 9; count++) {
      pulse(gate, state);
      assertKnown(state, count);
      assertEquals(count == 9 ? Value.FALSE : Value.TRUE, terminal(state), "up " + count);
    }
    pulse(gate, state);
    assertKnown(state, 0);
    assertEquals(Value.TRUE, terminal(state));

    controls(state, Value.FALSE, Value.FALSE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.FALSE, terminal(state));
    for (var count = 9; count >= 0; count--) {
      pulse(gate, state);
      assertKnown(state, count);
      assertEquals(count == 0 ? Value.FALSE : Value.TRUE, terminal(state), "down " + count);
    }
  }

  @Test
  void illegalCodesRejoinTheDecadeAndTerminalCountFollowsQ0AndQ3() {
    final var gate = new Ttl74168();
    final var state = new TestInstanceState(gate, false);
    for (var code = 0; code < 16; code++) {
      load(gate, state, code);
      controls(state, Value.TRUE, Value.FALSE, Value.FALSE, Value.TRUE);
      gate.propagate(state);
      assertEquals(upTerminal(code) ? Value.FALSE : Value.TRUE, terminal(state), "up tc " + code);
      pulse(gate, state);
      assertKnown(state, Ttl74168.NEXT_UP[code]);

      load(gate, state, code);
      controls(state, Value.FALSE, Value.FALSE, Value.FALSE, Value.TRUE);
      gate.propagate(state);
      assertKnown(state, code);
      assertEquals(code == 0 ? Value.FALSE : Value.TRUE, terminal(state), "down tc " + code);
      pulse(gate, state);
      assertKnown(state, Ttl74168.NEXT_DOWN[code]);
    }
  }

  @Test
  void eitherEnableHighHoldsAndCetAloneReleasesTerminalCount() {
    final var gate = new Ttl74168();
    final var state = new TestInstanceState(gate, false);
    load(gate, state, 9);
    controls(state, Value.TRUE, Value.TRUE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.FALSE, terminal(state));
    pulse(gate, state);
    assertKnown(state, 9);
    assertEquals(Value.FALSE, terminal(state));

    state.setPortValue(Ttl74168.PORT_INDEX_CEP, Value.FALSE);
    state.setPortValue(Ttl74168.PORT_INDEX_CET, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.TRUE, terminal(state));
    pulse(gate, state);
    assertKnown(state, 9);
    assertEquals(Value.TRUE, terminal(state));

    state.setPortValue(Ttl74168.PORT_INDEX_CET, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.FALSE, terminal(state));
  }

  @Test
  void directionChangesTerminalCountWithoutAClock() {
    final var gate = new Ttl74168();
    final var state = new TestInstanceState(gate, false);
    load(gate, state, 0);
    controls(state, Value.TRUE, Value.FALSE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.TRUE, terminal(state));

    state.setPortValue(Ttl74168.PORT_INDEX_UD, Value.FALSE);
    gate.propagate(state);
    assertKnown(state, 0);
    assertEquals(Value.FALSE, terminal(state));

    load(gate, state, 9);
    controls(state, Value.FALSE, Value.TRUE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    assertKnown(state, 9);
    assertEquals(Value.TRUE, terminal(state));
    state.setPortValue(Ttl74168.PORT_INDEX_UD, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.FALSE, terminal(state));
  }

  @Test
  void fallingAndUnknownClocksDoNotCount() {
    final var gate = new Ttl74168();
    final var state = new TestInstanceState(gate, false);
    load(gate, state, 3);
    controls(state, Value.TRUE, Value.FALSE, Value.FALSE, Value.TRUE);
    gate.propagate(state);
    pulse(gate, state);
    assertKnown(state, 4);

    gate.propagate(state);
    assertKnown(state, 4);
    state.setPortValue(Ttl74168.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    assertKnown(state, 4);

    state.setPortValue(Ttl74168.PORT_INDEX_CP, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl74168.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    assertKnown(state, 4);
  }

  @Test
  void loadKeepsUnknownAndErrorDataBits() {
    final var gate = new Ttl74168();
    final var state = new TestInstanceState(gate, false);
    load(gate, state, 0);
    data(state, Value.UNKNOWN, Value.TRUE, Value.FALSE, Value.TRUE);
    controls(state, Value.TRUE, Value.UNKNOWN, Value.ERROR, Value.FALSE);
    pulse(gate, state);
    assertBits(state, Value.UNKNOWN, Value.TRUE, Value.FALSE, Value.TRUE);

    data(state, Value.ERROR, Value.FALSE, Value.TRUE, Value.FALSE);
    pulse(gate, state);
    assertBits(state, Value.ERROR, Value.FALSE, Value.TRUE, Value.FALSE);
  }

  @Test
  void ambiguousControlsMakeTheWholeWordUnknownOrError() {
    final var gate = new Ttl74168();
    final var state = new TestInstanceState(gate, false);
    load(gate, state, 1);
    data(state, 4);
    controls(state, Value.TRUE, Value.TRUE, Value.TRUE, Value.UNKNOWN);
    pulse(gate, state);
    assertAll(state, Value.UNKNOWN);

    load(gate, state, 4);
    data(state, 4);
    controls(state, Value.TRUE, Value.TRUE, Value.TRUE, Value.UNKNOWN);
    pulse(gate, state);
    assertKnown(state, 4);

    load(gate, state, 1);
    data(state, 4);
    controls(state, Value.TRUE, Value.TRUE, Value.TRUE, Value.ERROR);
    pulse(gate, state);
    assertAll(state, Value.ERROR);

    load(gate, state, 1);
    controls(state, Value.TRUE, Value.UNKNOWN, Value.FALSE, Value.TRUE);
    pulse(gate, state);
    assertAll(state, Value.UNKNOWN);

    load(gate, state, 1);
    controls(state, Value.TRUE, Value.UNKNOWN, Value.TRUE, Value.TRUE);
    pulse(gate, state);
    assertKnown(state, 1);

    load(gate, state, 1);
    controls(state, Value.UNKNOWN, Value.FALSE, Value.FALSE, Value.TRUE);
    pulse(gate, state);
    assertAll(state, Value.UNKNOWN);

    load(gate, state, 1);
    controls(state, Value.ERROR, Value.FALSE, Value.FALSE, Value.TRUE);
    pulse(gate, state);
    assertAll(state, Value.ERROR);
  }

  @Test
  void undefinedOrErrorStateSpreadsWhenCounting() {
    final var gate = new Ttl74168();
    final var state = new TestInstanceState(gate, false);
    load(gate, state, 0);
    final var unknown = Value.createUnknown(WIDTH);
    inject(gate, state, unknown);
    controls(state, Value.TRUE, Value.FALSE, Value.FALSE, Value.TRUE);
    pulse(gate, state);
    assertAll(state, Value.UNKNOWN);

    final var bits = Value.createKnown(WIDTH, 2).getAll();
    bits[0] = Value.ERROR;
    inject(gate, state, Value.create(bits));
    pulse(gate, state);
    assertAll(state, Value.ERROR);

    controls(state, Value.TRUE, Value.TRUE, Value.TRUE, Value.TRUE);
    pulse(gate, state);
    assertAll(state, Value.ERROR);
  }

  @Test
  void terminalCountUsesThreeValuedDirectionAndCarryEnable() {
    final var gate = new Ttl74168();
    final var state = new TestInstanceState(gate, false);
    data(state, Value.UNKNOWN, Value.TRUE, Value.TRUE, Value.TRUE);
    controls(state, Value.TRUE, Value.TRUE, Value.FALSE, Value.FALSE);
    pulse(gate, state);
    assertEquals(Value.UNKNOWN, terminal(state));

    data(state, Value.FALSE, Value.TRUE, Value.TRUE, Value.TRUE);
    pulse(gate, state);
    assertEquals(Value.TRUE, terminal(state));

    data(state, Value.TRUE, Value.UNKNOWN, Value.ERROR, Value.TRUE);
    controls(state, Value.TRUE, Value.TRUE, Value.FALSE, Value.FALSE);
    pulse(gate, state);
    assertEquals(Value.FALSE, terminal(state));

    load(gate, state, 11);
    controls(state, Value.TRUE, Value.FALSE, Value.UNKNOWN, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, terminal(state));
    state.setPortValue(Ttl74168.PORT_INDEX_CET, Value.ERROR);
    gate.propagate(state);
    assertEquals(Value.ERROR, terminal(state));
    state.setPortValue(Ttl74168.PORT_INDEX_CET, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.TRUE, terminal(state));

    load(gate, state, 7);
    controls(state, Value.ERROR, Value.FALSE, Value.ERROR, Value.TRUE);
    gate.propagate(state);
    assertKnown(state, 7);
    assertEquals(Value.TRUE, terminal(state));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl74168();
    final var state = new TestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    load(gate, state, 9);
    assertKnown(state, 9);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertAll(state, Value.UNKNOWN);
    assertEquals(Value.UNKNOWN, terminal(state));

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertKnown(state, 9);

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertAll(state, Value.UNKNOWN);
  }

  private static boolean upTerminal(int code) {
    return (code & 0x9) == 0x9;
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

  /** Preference updates arrive on the prefs thread, so wait until {@code get} matches. */
  private static void setStartup(boolean unknown) {
    AppPreferences.Memory_Startup_Unknown.set(unknown);
    final var deadline = System.nanoTime() + 2_000_000_000L;
    while (AppPreferences.Memory_Startup_Unknown.get() != unknown) {
      if (System.nanoTime() > deadline) {
        throw new AssertionError("Memory startup preference did not become " + unknown);
      }
      try {
        Thread.sleep(10);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new AssertionError(ex);
      }
    }
  }

  private static void controls(
      TestInstanceState state, Value upDown, Value cep, Value cet, Value pe) {
    state.setPortValue(Ttl74168.PORT_INDEX_UD, upDown);
    state.setPortValue(Ttl74168.PORT_INDEX_CEP, cep);
    state.setPortValue(Ttl74168.PORT_INDEX_CET, cet);
    state.setPortValue(Ttl74168.PORT_INDEX_PE, pe);
  }

  private static void data(TestInstanceState state, int value) {
    data(state, bit(value, 0), bit(value, 1), bit(value, 2), bit(value, 3));
  }

  private static void data(TestInstanceState state, Value d0, Value d1, Value d2, Value d3) {
    state.setPortValue(Ttl74168.PORT_INDEX_D0, d0);
    state.setPortValue(Ttl74168.PORT_INDEX_D1, d1);
    state.setPortValue(Ttl74168.PORT_INDEX_D2, d2);
    state.setPortValue(Ttl74168.PORT_INDEX_D3, d3);
  }

  private static Value bit(int value, int index) {
    return ((value >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
  }

  private static void pulse(Ttl74168 gate, TestInstanceState state) {
    state.setPortValue(Ttl74168.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl74168.PORT_INDEX_CP, Value.TRUE);
    gate.propagate(state);
  }

  private static void load(Ttl74168 gate, TestInstanceState state, int value) {
    data(state, value);
    controls(state, Value.TRUE, Value.TRUE, Value.TRUE, Value.FALSE);
    pulse(gate, state);
  }

  private static void inject(Ttl74168 gate, TestInstanceState state, Value value) {
    state.setPortValue(Ttl74168.PORT_INDEX_CP, Value.FALSE);
    gate.propagate(state);
    final var data = new TtlRegisterData(WIDTH);
    data.setValue(value);
    state.setData(data);
    gate.propagate(state);
  }

  private static Value terminal(TestInstanceState state) {
    return state.getPortValue(Ttl74168.PORT_INDEX_TC);
  }

  private static void assertKnown(TestInstanceState state, int count) {
    assertBits(state, bit(count, 0), bit(count, 1), bit(count, 2), bit(count, 3));
  }

  private static void assertAll(TestInstanceState state, Value bit) {
    assertBits(state, bit, bit, bit, bit);
  }

  private static void assertBits(
      TestInstanceState state, Value q0, Value q1, Value q2, Value q3) {
    assertEquals(q0, state.getPortValue(Ttl74168.PORT_INDEX_Q0));
    assertEquals(q1, state.getPortValue(Ttl74168.PORT_INDEX_Q1));
    assertEquals(q2, state.getPortValue(Ttl74168.PORT_INDEX_Q2));
    assertEquals(q3, state.getPortValue(Ttl74168.PORT_INDEX_Q3));
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
