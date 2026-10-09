/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.std.ttl.TtlTestInstanceState.createInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Functional tests for the MM74C923 20-key encoder. */
class Ttl74923Test {
  /** At 1 Hz this capacitor makes press debounce last 9 ticks and the release mask 6 ticks. */
  private static final int SLOW_CKBM_PF = 900_000_000;

  private static final int PRESS_TICKS = 9;
  private static final int MASK_TICKS = 6;
  private static final int GND_PORT = 17;
  private static final int VCC_PORT = 18;
  private static final byte[] ROWS = {
    Ttl74923.Y1, Ttl74923.Y2, Ttl74923.Y3, Ttl74923.Y4, Ttl74923.Y5
  };
  private static final byte[] COLUMNS = {Ttl74923.X1, Ttl74923.X2, Ttl74923.X3, Ttl74923.X4};
  private static final byte[] DATA = {
    Ttl74923.DATA_A, Ttl74923.DATA_B, Ttl74923.DATA_C, Ttl74923.DATA_D, Ttl74923.DATA_E
  };
  private static final byte[] OUTPUTS = {
    Ttl74923.X4,
    Ttl74923.X3,
    Ttl74923.X2,
    Ttl74923.X1,
    Ttl74923.DAV,
    Ttl74923.DATA_E,
    Ttl74923.DATA_D,
    Ttl74923.DATA_C,
    Ttl74923.DATA_B,
    Ttl74923.DATA_A
  };

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var encoder = new Ttl74923();
    final var hiddenPower = createInstance(encoder, false);

    assertEquals(17, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74923.Y1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74923.Y2, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74923.Y3, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74923.Y4, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74923.Y5, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74923.OSC, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74923.X4, 150, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74923.X3, 170, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74923.X2, 190, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74923.X1, 170, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74923.DAV, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74923.OE, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74923.DATA_E, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74923.DATA_D, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74923.DATA_C, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74923.DATA_B, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74923.DATA_A, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(encoder, true);
    assertEquals(19, shownPower.getPorts().size());
    assertEquals(Location.create(190, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void timingFormulasFollowTheTypicalCurves() {
    assertEquals(0, Ttl74923.columnStepTicks(0, 1_000));
    assertEquals(1, Ttl74923.columnStepTicks(10_000, 10_000));
    assertEquals(1, Ttl74923.columnStepTicks(100_000, 1_000));
    assertEquals(1, Ttl74923.columnStepTicks(1_000_000, 100));
    assertEquals(2, Ttl74923.columnStepTicks(10_000_000, 20));
    assertEquals(1, Ttl74923.columnStepTicks(100_000, 0));
    assertEquals(Integer.MAX_VALUE, Ttl74923.columnStepTicks(10_000_000, 1.0e12));

    assertEquals(10, Ttl74923.pressDebounceTicks(1_000_000, 1_000));
    assertEquals(1, Ttl74923.pressDebounceTicks(1_000_000, 1));
    assertEquals(0, Ttl74923.pressDebounceTicks(0, 1_000));
    assertEquals(1, Ttl74923.pressDebounceTicks(1_000_000, 0));
    assertEquals(PRESS_TICKS, Ttl74923.pressDebounceTicks(SLOW_CKBM_PF, 1));
    assertEquals(Integer.MAX_VALUE, Ttl74923.pressDebounceTicks(1_000_000_000, 1.0e12));

    assertEquals(7, Ttl74923.releaseMaskTicks(1_000_000, 1_000));
    assertEquals(MASK_TICKS, Ttl74923.releaseMaskTicks(SLOW_CKBM_PF, 1));
    assertEquals(0, Ttl74923.releaseMaskTicks(0, 1_000));
  }

  @Test
  void risingClockDrivesOneOpenDrainColumn() {
    final var encoder = new Ttl74923();
    final var state = slowState(encoder);
    encoder.propagate(state);
    assertColumn(state, 0);

    for (var step = 1; step <= 4; step++) {
      rise(encoder, state);
      assertColumn(state, step % 4);
    }
  }

  @Test
  void eachKeyLatchesItsDatasheetCode() {
    final var encoder = new Ttl74923();

    for (var key = 0; key < 20; key++) {
      final var state = slowState(encoder);
      final var column = key % 4;
      scanTo(encoder, state, column);
      setLow(state, ROWS[key / 4]);
      encoder.propagate(state);
      assertFalse(isHigh(state, Ttl74923.DAV));

      state.setTickCount(state.getTickCount() + PRESS_TICKS);
      encoder.propagate(state);

      assertTrue(isHigh(state, Ttl74923.DAV));
      assertCode(state, key);
      assertColumn(state, column);
    }
  }

  @Test
  void releasedKeyKeepsTheCodeAndDropsDataAvailable() {
    final var encoder = new Ttl74923();
    final var state = acceptKey(encoder, 0);

    setHigh(state, Ttl74923.Y1);
    encoder.propagate(state);

    assertFalse(isHigh(state, Ttl74923.DAV));
    assertCode(state, 0);
  }

  @Test
  void bounceShorterThanDebounceDoesNotLatch() {
    final var encoder = new Ttl74923();
    final var state = slowState(encoder);
    encoder.propagate(state);
    setLow(state, Ttl74923.Y1);
    encoder.propagate(state);

    state.setTickCount(PRESS_TICKS - 1);
    encoder.propagate(state);
    assertFalse(isHigh(state, Ttl74923.DAV));

    setHigh(state, Ttl74923.Y1);
    encoder.propagate(state);
    state.setTickCount(100);
    encoder.propagate(state);

    assertFalse(isHigh(state, Ttl74923.DAV));
    assertCode(state, 0);
  }

  @Test
  void releaseMaskIgnoresANewClosure() {
    final var encoder = new Ttl74923();
    final var state = acceptKey(encoder, 0);
    final var releasedAt = state.getTickCount();

    setHigh(state, Ttl74923.Y1);
    encoder.propagate(state);
    setLow(state, Ttl74923.Y1);
    encoder.propagate(state);
    state.setTickCount(releasedAt + MASK_TICKS - 1);
    encoder.propagate(state);

    assertFalse(isHigh(state, Ttl74923.DAV));
    assertCode(state, 0);

    state.setTickCount(releasedAt + MASK_TICKS);
    encoder.propagate(state);
    assertFalse(isHigh(state, Ttl74923.DAV));
    state.setTickCount(releasedAt + MASK_TICKS + PRESS_TICKS);
    encoder.propagate(state);
    assertTrue(isHigh(state, Ttl74923.DAV));
    assertCode(state, 0);
  }

  @Test
  void secondKeyIsIgnoredUntilTheFirstIsReleased() {
    final var encoder = new Ttl74923();
    final var state = acceptKey(encoder, 0);

    setLow(state, Ttl74923.Y2);
    rise(encoder, state);
    state.setTickCount(state.getTickCount() + 20);
    encoder.propagate(state);

    assertTrue(isHigh(state, Ttl74923.DAV));
    assertCode(state, 0);
    assertColumn(state, 0);

    setHigh(state, Ttl74923.Y1);
    final var releasedAt = state.getTickCount();
    encoder.propagate(state);
    state.setTickCount(releasedAt + MASK_TICKS - 1);
    encoder.propagate(state);

    assertFalse(isHigh(state, Ttl74923.DAV));
    assertCode(state, 0);
  }

  @Test
  void laterColumnIsAcceptedAfterRelease() {
    final var encoder = new Ttl74923();
    final var state = immediateState(encoder);
    encoder.propagate(state);
    setLow(state, Ttl74923.Y1);
    encoder.propagate(state);
    assertCode(state, 0);

    setHigh(state, Ttl74923.Y1);
    encoder.propagate(state);
    rise(encoder, state);
    setLow(state, Ttl74923.Y2);
    encoder.propagate(state);

    assertTrue(isHigh(state, Ttl74923.DAV));
    assertCode(state, 5);
    assertColumn(state, 1);
  }

  @Test
  void theLowestRowWinsWhenSeveralAreLow() {
    final var encoder = new Ttl74923();
    final var state = immediateState(encoder);
    encoder.propagate(state);
    setLow(state, Ttl74923.Y1);
    setLow(state, Ttl74923.Y5);
    encoder.propagate(state);

    assertTrue(isHigh(state, Ttl74923.DAV));
    assertCode(state, 0);
  }

  @Test
  void floatingRowsAreNotPressed() {
    final var encoder = new Ttl74923();
    final var state = new TtlTestInstanceState(encoder, false);
    setLow(state, Ttl74923.OE);
    encoder.propagate(state);

    assertFalse(isHigh(state, Ttl74923.DAV));
    assertCode(state, 0);
    assertColumn(state, 0);
  }

  @Test
  void outputEnableReleasesTheDataPins() {
    final var encoder = new Ttl74923();
    final var state = acceptKey(encoder, 0);

    setHigh(state, Ttl74923.OE);
    encoder.propagate(state);
    assertUnknownData(state);
    assertTrue(isHigh(state, Ttl74923.DAV));

    state.setPortValue(Ttl74923.pinNrToPortNr(Ttl74923.OE), Value.UNKNOWN);
    encoder.propagate(state);
    assertUnknownData(state);
    assertTrue(isHigh(state, Ttl74923.DAV));

    setLow(state, Ttl74923.OE);
    encoder.propagate(state);
    assertCode(state, 0);
  }

  @Test
  void omittedBounceCapacitorAcceptsImmediately() {
    final var encoder = new Ttl74923();
    final var state = immediateState(encoder);
    encoder.propagate(state);
    setLow(state, Ttl74923.Y1);
    encoder.propagate(state);

    assertTrue(isHigh(state, Ttl74923.DAV));
    assertCode(state, 0);

    setHigh(state, Ttl74923.Y1);
    setLow(state, Ttl74923.Y2);
    encoder.propagate(state);

    assertTrue(isHigh(state, Ttl74923.DAV));
    assertCode(state, 4);
  }

  @Test
  void lowRowFreezesTheColumnAcrossAClockEdge() {
    final var encoder = new Ttl74923();
    final var state = slowState(encoder);
    encoder.propagate(state);
    setLow(state, Ttl74923.Y1);
    rise(encoder, state);

    assertColumn(state, 0);
    assertFalse(isHigh(state, Ttl74923.DAV));
    state.setTickCount(state.getTickCount() + PRESS_TICKS);
    encoder.propagate(state);
    assertCode(state, 0);
    assertColumn(state, 0);
  }

  @Test
  void capacitorOscillatorScansWhileOscIsFloating() {
    final var encoder = new Ttl74923();
    final var state = slowState(encoder);
    state.getAttributeSet().setValue(Ttl74923.COSC, 1_000_000);
    encoder.propagate(state);
    assertColumn(state, 0);

    state.setTickCount(1);
    encoder.propagate(state);
    assertColumn(state, 1);
  }

  @Test
  void drivenOscIgnoresTheCapacitor() {
    final var encoder = new Ttl74923();
    final var state = slowState(encoder);
    state.getAttributeSet().setValue(Ttl74923.COSC, 1_000_000);
    setLow(state, Ttl74923.OSC);
    encoder.propagate(state);
    state.setTickCount(5);
    encoder.propagate(state);

    assertColumn(state, 0);
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var encoder = new Ttl74923();
    final var state = new TtlTestInstanceState(encoder, true);
    state.getAttributeSet().setValue(Ttl74923.CKBM, 0);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    setLow(state, Ttl74923.OE);
    releaseRows(state);
    encoder.propagate(state);
    setLow(state, Ttl74923.Y1);
    encoder.propagate(state);
    assertTrue(isHigh(state, Ttl74923.DAV));

    state.setPortValue(VCC_PORT, Value.FALSE);
    encoder.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    encoder.propagate(state);
    assertTrue(isHigh(state, Ttl74923.DAV));
    assertCode(state, 0);

    state.setPortValue(GND_PORT, Value.TRUE);
    encoder.propagate(state);
    assertUnknownOutputs(state);
  }

  private static TtlTestInstanceState slowState(Ttl74923 encoder) {
    final var state = new TtlTestInstanceState(encoder, false);
    state.getAttributeSet().setValue(Ttl74923.CKBM, SLOW_CKBM_PF);
    setLow(state, Ttl74923.OE);
    releaseRows(state);
    return state;
  }

  private static TtlTestInstanceState immediateState(Ttl74923 encoder) {
    final var state = new TtlTestInstanceState(encoder, false);
    state.getAttributeSet().setValue(Ttl74923.CKBM, 0);
    setLow(state, Ttl74923.OE);
    releaseRows(state);
    return state;
  }

  /** One rising edge of OSC. The pin is returned low first so the edge is defined. */
  private static void rise(Ttl74923 encoder, TtlTestInstanceState state) {
    setLow(state, Ttl74923.OSC);
    encoder.propagate(state);
    setHigh(state, Ttl74923.OSC);
    encoder.propagate(state);
  }

  /** Propagates until the requested column is driven, then returns with the rows released. */
  private static void scanTo(Ttl74923 encoder, TtlTestInstanceState state, int column) {
    encoder.propagate(state);
    for (var step = 0; step < column; step++) {
      rise(encoder, state);
    }
    assertColumn(state, column);
  }

  private static TtlTestInstanceState acceptKey(Ttl74923 encoder, int key) {
    final var state = slowState(encoder);
    final var column = key % 4;
    scanTo(encoder, state, column);
    setLow(state, ROWS[key / 4]);
    encoder.propagate(state);
    state.setTickCount(state.getTickCount() + PRESS_TICKS);
    encoder.propagate(state);
    assertTrue(isHigh(state, Ttl74923.DAV));
    assertCode(state, key);
    return state;
  }

  private static void releaseRows(TtlTestInstanceState state) {
    for (final var row : ROWS) {
      setHigh(state, row);
    }
  }

  private static void assertColumn(TtlTestInstanceState state, int column) {
    for (var index = 0; index < COLUMNS.length; index++) {
      final var expected = index == column ? Value.FALSE : Value.UNKNOWN;
      assertEquals(expected, port(state, COLUMNS[index]));
    }
  }

  private static void assertCode(TtlTestInstanceState state, int code) {
    for (var bit = 0; bit < DATA.length; bit++) {
      final var expected = (code & (1 << bit)) == 0 ? Value.FALSE : Value.TRUE;
      assertEquals(expected, port(state, DATA[bit]));
    }
  }

  private static void assertUnknownData(TtlTestInstanceState state) {
    for (final var pin : DATA) {
      assertEquals(Value.UNKNOWN, port(state, pin));
    }
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var pin : OUTPUTS) {
      assertEquals(Value.UNKNOWN, port(state, pin));
    }
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74923.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static Value port(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74923.pinNrToPortNr(dsPinNr));
  }

  private static void setLow(TtlTestInstanceState state, byte dsPinNr) {
    state.setPortValue(Ttl74923.pinNrToPortNr(dsPinNr), Value.FALSE);
  }

  private static void setHigh(TtlTestInstanceState state, byte dsPinNr) {
    state.setPortValue(Ttl74923.pinNrToPortNr(dsPinNr), Value.TRUE);
  }

  private static boolean isHigh(TtlTestInstanceState state, byte dsPinNr) {
    return port(state, dsPinNr) == Value.TRUE;
  }
}
