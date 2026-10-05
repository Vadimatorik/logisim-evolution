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

/** Functional tests for the SN74LV8154 dual 16-bit counters, modeled as 748154. */
class Ttl748154Test {
  private static final int GND_PORT = 18;
  private static final int VCC_PORT = 19;
  private static final int FULL = 0xFFFF;
  private static final int[] Y_PORTS = {
    Ttl748154.PORT_Y0,
    Ttl748154.PORT_Y1,
    Ttl748154.PORT_Y2,
    Ttl748154.PORT_Y3,
    Ttl748154.PORT_Y4,
    Ttl748154.PORT_Y5,
    Ttl748154.PORT_Y6,
    Ttl748154.PORT_Y7
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var gate = new Ttl748154();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(18, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl748154.PORT_CLKA, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl748154.PORT_CLKB, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl748154.PORT_GAL, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl748154.PORT_GAU, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl748154.PORT_GBL, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl748154.PORT_GBU, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl748154.PORT_RCLK, 130, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl748154.PORT_RCOA, 150, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl748154.PORT_CLKBEN, 170, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl748154.PORT_CCLR, 190, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl748154.PORT_Y7, 170, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl748154.PORT_Y6, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl748154.PORT_Y5, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl748154.PORT_Y4, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl748154.PORT_Y3, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl748154.PORT_Y2, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl748154.PORT_Y1, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl748154.PORT_Y0, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(20, shownPower.getPorts().size());
    assertPort(shownPower, GND_PORT, 190, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void clockPinsAreTheThreeRisingEdges() {
    final var gate = new Ttl748154();
    assertTrue(gate.checkForGatedClocks(null));
    assertArrayEquals(
        new int[] {Ttl748154.PORT_CLKA, Ttl748154.PORT_CLKB, Ttl748154.PORT_RCLK},
        gate.clockPinIndex(null));
  }

  @Test
  void clearZerosBothCountersWithoutChangingStorage() {
    final var gate = new Ttl748154();
    final var state = prepared(gate, false);
    clock(gate, state, Ttl748154.PORT_CLKA, 5);
    state.setPortValue(Ttl748154.PORT_CLKBEN, Value.FALSE);
    clock(gate, state, Ttl748154.PORT_CLKB, 3);
    state.setPortValue(Ttl748154.PORT_CLKBEN, Value.TRUE);
    latch(gate, state);
    assertEquals(5, readStored(gate, state, true));
    assertEquals(3, readStored(gate, state, false));

    state.setPortValue(Ttl748154.PORT_CCLR, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.TRUE, state.getPortValue(Ttl748154.PORT_RCOA));
    assertEquals(5, readStored(gate, state, true));
    assertEquals(3, readStored(gate, state, false));

    state.setPortValue(Ttl748154.PORT_CLKA, Value.TRUE);
    state.setPortValue(Ttl748154.PORT_CLKB, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl748154.PORT_CLKA, Value.FALSE);
    state.setPortValue(Ttl748154.PORT_CLKB, Value.FALSE);
    gate.propagate(state);
    assertEquals(5, readStored(gate, state, true));

    state.setPortValue(Ttl748154.PORT_CCLR, Value.TRUE);
    gate.propagate(state);
    latch(gate, state);
    assertEquals(0, readStored(gate, state, true));
    assertEquals(0, readStored(gate, state, false));
    clock(gate, state, Ttl748154.PORT_CLKA, 1);
    latch(gate, state);
    assertEquals(1, readStored(gate, state, true));
  }

  @Test
  void counterACountsOnTheRisingEdgeAndWraps() {
    final var gate = new Ttl748154();
    final var state = prepared(gate, false);
    assertEquals(Value.TRUE, state.getPortValue(Ttl748154.PORT_RCOA));

    state.setPortValue(Ttl748154.PORT_CLKA, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl748154.PORT_CLKA, Value.TRUE);
    gate.propagate(state);
    latch(gate, state);
    assertEquals(1, readStored(gate, state, true));

    state.setPortValue(Ttl748154.PORT_CLKA, Value.FALSE);
    gate.propagate(state);
    latch(gate, state);
    assertEquals(1, readStored(gate, state, true));

    state.setPortValue(Ttl748154.PORT_CLKA, Value.UNKNOWN);
    gate.propagate(state);
    state.setPortValue(Ttl748154.PORT_CLKA, Value.FALSE);
    gate.propagate(state);
    latch(gate, state);
    assertEquals(1, readStored(gate, state, true));

    clock(gate, state, Ttl748154.PORT_CLKA, 255);
    latch(gate, state);
    assertEquals(0x100, readStored(gate, state, true));
    assertEquals(0, readStored(gate, state, false));

    clock(gate, state, Ttl748154.PORT_CLKA, FULL - 0x100);
    assertEquals(Value.FALSE, state.getPortValue(Ttl748154.PORT_RCOA));
    latch(gate, state);
    assertEquals(FULL, readStored(gate, state, true));

    clock(gate, state, Ttl748154.PORT_CLKA, 1);
    assertEquals(Value.TRUE, state.getPortValue(Ttl748154.PORT_RCOA));
    latch(gate, state);
    assertEquals(0, readStored(gate, state, true));
  }

  @Test
  void counterBCountsOnlyWhenItsEnableIsLow() {
    final var gate = new Ttl748154();
    final var state = prepared(gate, false);
    clock(gate, state, Ttl748154.PORT_CLKB, 4);
    latch(gate, state);
    assertEquals(0, readStored(gate, state, false));

    state.setPortValue(Ttl748154.PORT_CLKBEN, Value.FALSE);
    clock(gate, state, Ttl748154.PORT_CLKB, 4);
    state.setPortValue(Ttl748154.PORT_CLKBEN, Value.TRUE);
    latch(gate, state);
    assertEquals(4, readStored(gate, state, false));
    assertEquals(0, readStored(gate, state, true));

    clock(gate, state, Ttl748154.PORT_CLKA, 2);
    latch(gate, state);
    assertEquals(4, readStored(gate, state, false));
    assertEquals(2, readStored(gate, state, true));
  }

  @Test
  void storageCapturesTheUpdatedCountAndThenHolds() {
    final var gate = new Ttl748154();
    final var state = prepared(gate, false);
    clock(gate, state, Ttl748154.PORT_CLKA, 6);
    assertEquals(0, readStored(gate, state, true));

    state.setPortValue(Ttl748154.PORT_CLKA, Value.TRUE);
    state.setPortValue(Ttl748154.PORT_RCLK, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl748154.PORT_CLKA, Value.FALSE);
    state.setPortValue(Ttl748154.PORT_RCLK, Value.FALSE);
    gate.propagate(state);
    assertEquals(7, readStored(gate, state, true));

    clock(gate, state, Ttl748154.PORT_CLKA, 3);
    assertEquals(7, readStored(gate, state, true));
    latch(gate, state);
    assertEquals(10, readStored(gate, state, true));
  }

  @Test
  void outputBusReadsTheSelectedStoredByte() {
    final var gate = new Ttl748154();
    final var state = prepared(gate, false);
    clock(gate, state, Ttl748154.PORT_CLKA, 0x0201);
    state.setPortValue(Ttl748154.PORT_CLKBEN, Value.FALSE);
    clock(gate, state, Ttl748154.PORT_CLKB, 0x0403);
    state.setPortValue(Ttl748154.PORT_CLKBEN, Value.TRUE);
    latch(gate, state);

    assertEquals(0x01, readByte(gate, state, Ttl748154.PORT_GAL));
    assertEquals(0x02, readByte(gate, state, Ttl748154.PORT_GAU));
    assertEquals(0x03, readByte(gate, state, Ttl748154.PORT_GBL));
    assertEquals(0x04, readByte(gate, state, Ttl748154.PORT_GBU));

    release(state);
    gate.propagate(state);
    assertBus(state, Value.UNKNOWN);
    assertEquals(Value.TRUE, state.getPortValue(Ttl748154.PORT_RCOA));
  }

  @Test
  void overlappingGateSelectsDriveAgreedBitsAndErrorTheRest() {
    final var gate = new Ttl748154();
    final var state = prepared(gate, false);
    clock(gate, state, Ttl748154.PORT_CLKA, 0xFF0F);
    latch(gate, state);

    select(state, Ttl748154.PORT_GAL, Ttl748154.PORT_GAU);
    gate.propagate(state);
    for (var bit = 0; bit < 4; bit++) {
      assertEquals(Value.TRUE, state.getPortValue(Y_PORTS[bit]), "Y" + bit);
    }
    for (var bit = 4; bit < 8; bit++) {
      assertEquals(Value.ERROR, state.getPortValue(Y_PORTS[bit]), "Y" + bit);
    }

    clock(gate, state, Ttl748154.PORT_CLKA, 0xF0);
    latch(gate, state);
    select(state, Ttl748154.PORT_GAL, Ttl748154.PORT_GAU);
    gate.propagate(state);
    assertBus(state, Value.TRUE);
  }

  @Test
  void unknownAndErrorControlsSpreadOnlyWhereTheChoicesDiffer() {
    final var gate = new Ttl748154();
    final var state = prepared(gate, false);
    clock(gate, state, Ttl748154.PORT_CLKA, 5);
    latch(gate, state);

    state.setPortValue(Ttl748154.PORT_CCLR, Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl748154.PORT_RCOA));
    assertEquals(5, readStored(gate, state, true));
    state.setPortValue(Ttl748154.PORT_CCLR, Value.TRUE);
    gate.propagate(state);
    latch(gate, state);
    select(state, Ttl748154.PORT_GAL, -1);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl748154.PORT_Y0));
    assertEquals(Value.FALSE, state.getPortValue(Ttl748154.PORT_Y1));
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl748154.PORT_Y2));
    for (var bit = 3; bit < Y_PORTS.length; bit++) {
      assertEquals(Value.FALSE, state.getPortValue(Y_PORTS[bit]), "Y" + bit);
    }

    final var again = prepared(gate, false);
    clock(gate, again, Ttl748154.PORT_CLKA, 5);
    latch(gate, again);
    again.setPortValue(Ttl748154.PORT_CCLR, Value.ERROR);
    gate.propagate(again);
    assertEquals(Value.ERROR, again.getPortValue(Ttl748154.PORT_RCOA));
    assertEquals(5, readStored(gate, again, true));
    again.setPortValue(Ttl748154.PORT_CCLR, Value.TRUE);
    gate.propagate(again);
    clock(gate, again, Ttl748154.PORT_CLKA, 1);
    assertEquals(Value.ERROR, again.getPortValue(Ttl748154.PORT_RCOA));

    final var enabled = prepared(gate, false);
    enabled.setPortValue(Ttl748154.PORT_CLKBEN, Value.UNKNOWN);
    clock(gate, enabled, Ttl748154.PORT_CLKB, 1);
    assertEquals(Value.TRUE, enabled.getPortValue(Ttl748154.PORT_RCOA));
    assertEquals(0, readStored(gate, enabled, false));
    latch(gate, enabled);
    select(enabled, Ttl748154.PORT_GBL, -1);
    gate.propagate(enabled);
    assertEquals(Value.UNKNOWN, enabled.getPortValue(Ttl748154.PORT_Y0));
    for (var bit = 1; bit < Y_PORTS.length; bit++) {
      assertEquals(Value.FALSE, enabled.getPortValue(Y_PORTS[bit]), "Y" + bit);
    }

    final var gates = prepared(gate, false);
    clock(gate, gates, Ttl748154.PORT_CLKA, 1);
    latch(gate, gates);
    gates.setPortValue(Ttl748154.PORT_GAL, Value.UNKNOWN);
    gate.propagate(gates);
    assertBus(gates, Value.UNKNOWN);
    gates.setPortValue(Ttl748154.PORT_GAL, Value.ERROR);
    gate.propagate(gates);
    assertBus(gates, Value.ERROR);
    assertEquals(Value.TRUE, gates.getPortValue(Ttl748154.PORT_RCOA));
  }

  @Test
  void oneBadCounterBitPoisonsTheNextCountAndTheCarry() {
    final var gate = new Ttl748154();
    final var state = prepared(gate, false);
    final var data = new TtlRegisterData(BitWidth.create(16), 4);
    final var bits = Value.createKnown(16, 0).getAll();
    bits[0] = Value.ERROR;
    data.setValue(Ttl748154.WORD_COUNTER_A, Value.create(bits));
    data.setValue(Ttl748154.WORD_COUNTER_B, Value.createKnown(16, 0));
    data.setValue(Ttl748154.WORD_STORE_A, Value.createKnown(16, 0x22));
    data.setValue(Ttl748154.WORD_STORE_B, Value.createKnown(16, 0));
    state.setData(data);

    gate.propagate(state);
    assertEquals(Value.ERROR, state.getPortValue(Ttl748154.PORT_RCOA));
    assertEquals(0x22, readStored(gate, state, true));

    clock(gate, state, Ttl748154.PORT_CLKA, 1);
    assertEquals(Value.ERROR, state.getPortValue(Ttl748154.PORT_RCOA));
    latch(gate, state);
    assertBusAfterSelect(gate, state, Ttl748154.PORT_GAL, Value.ERROR);

    final var unknown = new TtlRegisterData(BitWidth.create(16), 4);
    final var unknownBits = Value.createKnown(16, 0).getAll();
    unknownBits[3] = Value.UNKNOWN;
    unknown.setValue(Ttl748154.WORD_COUNTER_A, Value.create(unknownBits));
    unknown.setValue(Ttl748154.WORD_COUNTER_B, Value.createKnown(16, 0));
    unknown.setValue(Ttl748154.WORD_STORE_A, Value.createKnown(16, 0));
    unknown.setValue(Ttl748154.WORD_STORE_B, Value.createKnown(16, 0));
    state.setData(unknown);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl748154.PORT_RCOA));
    clock(gate, state, Ttl748154.PORT_CLKA, 1);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl748154.PORT_RCOA));
  }

  @Test
  void carryEnablesCounterBOnTheClockAfterAIsFull() {
    final var gate = new Ttl748154();
    final var state = prepared(gate, false);
    clock(gate, state, Ttl748154.PORT_CLKA, FULL - 1);
    assertEquals(Value.TRUE, state.getPortValue(Ttl748154.PORT_RCOA));
    state.setPortValue(Ttl748154.PORT_CLKBEN, state.getPortValue(Ttl748154.PORT_RCOA));
    pulseBoth(gate, state);
    assertEquals(Value.FALSE, state.getPortValue(Ttl748154.PORT_RCOA));
    latch(gate, state);
    assertEquals(FULL, readStored(gate, state, true));
    assertEquals(0, readStored(gate, state, false));

    state.setPortValue(Ttl748154.PORT_CLKBEN, state.getPortValue(Ttl748154.PORT_RCOA));
    pulseBoth(gate, state);
    assertEquals(Value.TRUE, state.getPortValue(Ttl748154.PORT_RCOA));
    latch(gate, state);
    assertEquals(0, readStored(gate, state, true));
    assertEquals(1, readStored(gate, state, false));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl748154();
    final var state = prepared(gate, true);
    clock(gate, state, Ttl748154.PORT_CLKA, 4);
    latch(gate, state);
    select(state, Ttl748154.PORT_GAL, -1);
    gate.propagate(state);
    assertDriven(state, 4);

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertEquals(Value.UNKNOWN, state.getPortValue(Ttl748154.PORT_RCOA));
    assertBus(state, Value.UNKNOWN);

    state.setPortValue(VCC_PORT, Value.TRUE);
    gate.propagate(state);
    assertDriven(state, 4);
    assertEquals(Value.TRUE, state.getPortValue(Ttl748154.PORT_RCOA));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertBus(state, Value.UNKNOWN);
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

  private static TestInstanceState prepared(Ttl748154 gate, boolean showPowerPins) {
    final var state = new TestInstanceState(gate, showPowerPins);
    if (showPowerPins) {
      state.setPortValue(GND_PORT, Value.FALSE);
      state.setPortValue(VCC_PORT, Value.TRUE);
    }
    release(state);
    state.setPortValue(Ttl748154.PORT_CLKA, Value.FALSE);
    state.setPortValue(Ttl748154.PORT_CLKB, Value.FALSE);
    state.setPortValue(Ttl748154.PORT_RCLK, Value.FALSE);
    state.setPortValue(Ttl748154.PORT_CLKBEN, Value.TRUE);
    state.setPortValue(Ttl748154.PORT_CCLR, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl748154.PORT_CCLR, Value.FALSE);
    gate.propagate(state);
    state.setPortValue(Ttl748154.PORT_CCLR, Value.TRUE);
    gate.propagate(state);
    latch(gate, state);
    return state;
  }

  private static void latch(Ttl748154 gate, TestInstanceState state) {
    pulse(gate, state, Ttl748154.PORT_RCLK);
  }

  private static void pulse(Ttl748154 gate, TestInstanceState state, int port) {
    state.setPortValue(port, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(port, Value.FALSE);
    gate.propagate(state);
  }

  private static void pulseBoth(Ttl748154 gate, TestInstanceState state) {
    state.setPortValue(Ttl748154.PORT_CLKA, Value.TRUE);
    state.setPortValue(Ttl748154.PORT_CLKB, Value.TRUE);
    gate.propagate(state);
    state.setPortValue(Ttl748154.PORT_CLKA, Value.FALSE);
    state.setPortValue(Ttl748154.PORT_CLKB, Value.FALSE);
    gate.propagate(state);
  }

  private static void clock(Ttl748154 gate, TestInstanceState state, int port, int pulses) {
    for (var i = 0; i < pulses; i++) {
      pulse(gate, state, port);
    }
  }

  private static void release(TestInstanceState state) {
    select(state, -1, -1);
  }

  private static void select(TestInstanceState state, int first, int second) {
    state.setPortValue(Ttl748154.PORT_GAL, gateLevel(Ttl748154.PORT_GAL, first, second));
    state.setPortValue(Ttl748154.PORT_GAU, gateLevel(Ttl748154.PORT_GAU, first, second));
    state.setPortValue(Ttl748154.PORT_GBL, gateLevel(Ttl748154.PORT_GBL, first, second));
    state.setPortValue(Ttl748154.PORT_GBU, gateLevel(Ttl748154.PORT_GBU, first, second));
  }

  private static Value gateLevel(int port, int first, int second) {
    return port == first || port == second ? Value.FALSE : Value.TRUE;
  }

  private static int readStored(Ttl748154 gate, TestInstanceState state, boolean counterA) {
    final var low = readByte(gate, state, counterA ? Ttl748154.PORT_GAL : Ttl748154.PORT_GBL);
    final var high = readByte(gate, state, counterA ? Ttl748154.PORT_GAU : Ttl748154.PORT_GBU);
    return low | (high << 8);
  }

  private static int readByte(Ttl748154 gate, TestInstanceState state, int gatePort) {
    select(state, gatePort, -1);
    gate.propagate(state);
    var value = 0;
    for (var bit = 0; bit < Y_PORTS.length; bit++) {
      final var pin = state.getPortValue(Y_PORTS[bit]);
      assertTrue(pin == Value.TRUE || pin == Value.FALSE, "Y" + bit);
      if (pin == Value.TRUE) value |= 1 << bit;
    }
    release(state);
    gate.propagate(state);
    return value;
  }

  private static void assertDriven(TestInstanceState state, int expected) {
    for (var bit = 0; bit < Y_PORTS.length; bit++) {
      final var want = ((expected >> bit) & 1) == 0 ? Value.FALSE : Value.TRUE;
      assertEquals(want, state.getPortValue(Y_PORTS[bit]), "Y" + bit);
    }
  }

  private static void assertBus(TestInstanceState state, Value value) {
    for (var bit = 0; bit < Y_PORTS.length; bit++) {
      assertEquals(value, state.getPortValue(Y_PORTS[bit]), "Y" + bit);
    }
  }

  private static void assertBusAfterSelect(
      Ttl748154 gate, TestInstanceState state, int gatePort, Value value) {
    select(state, gatePort, -1);
    gate.propagate(state);
    assertBus(state, value);
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
