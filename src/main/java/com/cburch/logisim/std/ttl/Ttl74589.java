/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;

/**
 * TTL 74HC589: 8-bit shift register with an input latch and a 3-state serial output.
 *
 * <p>Simulation follows the
 * <a href="https://www.onsemi.com/download/data-sheet/pdf/mc74hc589a-d.pdf">ON Semi MC74HC589A</a>
 * function table and the Fairchild MM74HC589 truth table. Stage {@code A} is the serial input
 * stage and stage {@code H} drives {@code QH}. A rising {@code RCK} stores {@code A}–{@code H} in
 * the input latch. {@code SLOAD} low copies that latch into the shift register immediately and
 * ignores {@code SCK}. {@code SLOAD} high and a rising {@code SCK} shift {@code SA} in at stage
 * {@code A}. {@code OE} high releases only {@code QH}; it does not freeze either register.
 * Nanosecond delays are not modeled. The serial output is three-state, so there is no HDL model.
 */
public class Ttl74589 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74589";

  public static final int DELAY = 1;

  public static final byte B = 1;
  public static final byte C = 2;
  public static final byte D = 3;
  public static final byte E = 4;
  public static final byte F = 5;
  public static final byte G = 6;
  public static final byte H = 7;
  public static final byte GND = 8;
  public static final byte QH = 9;
  /** Output enable, active low. High puts QH in high impedance. */
  public static final byte OE = 10;
  /** Shift clock. Rising edge shifts while SLOAD is high. */
  public static final byte SCK = 11;
  /** Latch clock. Rising edge stores the parallel inputs. */
  public static final byte RCK = 12;
  /** High shifts on SCK. Low copies the input latch into the shift register. */
  public static final byte SLOAD = 13;
  public static final byte SA = 14;
  public static final byte A = 15;
  public static final byte VCC = 16;

  private static final int STAGES = 8;
  private static final int LATCH = 0;
  private static final int SHIFT = STAGES;
  private static final int SHIFT_CLOCK = 0;
  private static final int LATCH_CLOCK = 1;
  private static final byte[] PARALLEL = {A, B, C, D, E, F, G, H};
  private static final byte[] OUTPUTS = {QH};
  private static final String[] PORT_NAMES = {
    "B parallel data",
    "C parallel data",
    "D parallel data",
    "E parallel data",
    "F parallel data",
    "G parallel data",
    "H parallel data",
    "QH serial output (3-state)",
    "OE output enable (active low)",
    "SCK shift clock",
    "RCK latch clock",
    "SLOAD (LOW: parallel load, HIGH: shift)",
    "SA serial data",
    "A parallel data"
  };

  /** Creates a 74589 shift register with an input latch and a 3-state serial output. */
  public Ttl74589() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, null);
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Power pins are omitted from the port list.
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    return (byte) ((dsPinNr <= GND) ? dsPinNr - 1 : dsPinNr - 2);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, true, false);
    Drawgates.paintPortNamesByPin(
        painter,
        x,
        y,
        height,
        new String[] {
          "B", "C", "D", "E", "F", "G", "H", null,
          "QH", "OE", "SCK", "RCK", "S/L", "SA", "A", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = stateData(state);
    final var shiftEdge = data.updateClock(input(state, SCK), SHIFT_CLOCK, StdAttr.TRIG_RISING);
    final var latchEdge = data.updateClock(input(state, RCK), LATCH_CLOCK, StdAttr.TRIG_RISING);
    if (latchEdge) {
      loadLatch(state, data);
    }
    final var mode = input(state, SLOAD);
    if (mode == Value.FALSE) {
      copyLatchToShift(data);
    } else if (mode == Value.TRUE && shiftEdge) {
      shiftIn(state, data);
    }
    publish(state, data);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {pinNrToPortNr(SCK), pinNrToPortNr(RCK)};
  }

  private static TtlRegisterData stateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.ONE, STAGES * 2);
      state.setData(data);
    }
    return data;
  }

  private static void loadLatch(InstanceState state, TtlRegisterData data) {
    for (var stage = 0; stage < STAGES; stage++) {
      data.setValue(LATCH + stage, input(state, PARALLEL[stage]));
    }
  }

  private static void copyLatchToShift(TtlRegisterData data) {
    for (var stage = 0; stage < STAGES; stage++) {
      data.setValue(SHIFT + stage, data.getValue(LATCH + stage));
    }
  }

  /** Shifts toward H. SA enters stage A, and the previous stage G becomes the new QH. */
  private static void shiftIn(InstanceState state, TtlRegisterData data) {
    for (var stage = STAGES - 1; stage > 0; stage--) {
      data.setValue(SHIFT + stage, data.getValue(SHIFT + stage - 1));
    }
    data.setValue(SHIFT, input(state, SA));
  }

  private static void publish(InstanceState state, TtlRegisterData data) {
    final var serial = data.getValue(SHIFT + STAGES - 1);
    final var visible = input(state, OE) == Value.FALSE ? serial : Value.UNKNOWN;
    state.setPort(pinNrToPortNr(QH), visible, DELAY);
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }
}
