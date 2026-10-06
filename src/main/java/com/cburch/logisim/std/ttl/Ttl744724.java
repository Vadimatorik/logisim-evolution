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
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74HC4724: 8-bit addressable latch.
 *
 * <p>No manufacturer publishes a 74HC4724 data sheet. The model follows the Philips
 * <a href="https://pdf.dzsc.com/F47/HEF4724.pdf">HEF4724B</a> (January 1995) and Fairchild
 * CD4724BC function tables, which describe this device number. The DIP-16 pin positions match the
 * 74HC259, but pin 15 {@code CL} is active high. {@code E} is active low, as {@code LE} is on the
 * 259. Philips names the outputs {@code O0} to {@code O7}; the symbol uses {@code Q}. Nanosecond
 * delays are not modeled.
 *
 * <p>{@code CL} and {@code E} high clear every latch. {@code CL} high and {@code E} low make the
 * addressed output follow {@code D} and clear every other latch. Both low make only the addressed
 * latch follow {@code D}. {@code CL} low and {@code E} high hold the latches. A control that is
 * not exactly high or low does not write. An address bit that is not exactly high or low makes
 * each latch that might still be selected combine the selected and unselected results: a 0/1
 * conflict is an error, and any other disagreement is unknown.
 */
public class Ttl744724 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "744724";

  public static final int DELAY = 1;

  public static final byte A0 = 1;
  public static final byte A1 = 2;
  public static final byte A2 = 3;
  public static final byte Q0 = 4;
  public static final byte Q1 = 5;
  public static final byte Q2 = 6;
  public static final byte Q3 = 7;
  public static final byte GND = 8;
  public static final byte Q4 = 9;
  public static final byte Q5 = 10;
  public static final byte Q6 = 11;
  public static final byte Q7 = 12;
  public static final byte D = 13;
  /** Enable, active low. The addressed latch is transparent while this pin is low. */
  public static final byte E = 14;
  /** Clear, active high. High with {@link #E} high clears every latch. */
  public static final byte CL = 15;
  public static final byte VCC = 16;

  private static final int BITS = 8;
  private static final byte[] ADDRESS = {A0, A1, A2};
  private static final byte[] OUTPUTS = {Q0, Q1, Q2, Q3, Q4, Q5, Q6, Q7};
  private static final String[] PORT_NAMES = {
    "A0 address",
    "A1 address",
    "A2 address",
    "Q0",
    "Q1",
    "Q2",
    "Q3",
    "Q4",
    "Q5",
    "Q6",
    "Q7",
    "D data",
    "E (enable, active low)",
    "CL (clear, active high)"
  };

  /** Creates a 744724 8-bit addressable latch. */
  public Ttl744724() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl744724HdlGenerator());
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
          "A0", "A1", "A2", "Q0", "Q1", "Q2", "Q3", null,
          "Q4", "Q5", "Q6", "Q7", "D", "E", "CL", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = stateData(state);
    final var clear = input(state, CL);
    final var enable = input(state, E);
    if (clear == Value.TRUE && enable == Value.TRUE) {
      clearAll(data);
    } else if (clear == Value.TRUE && enable == Value.FALSE) {
      writeTransparent(state, data, true);
    } else if (clear == Value.FALSE && enable == Value.FALSE) {
      writeTransparent(state, data, false);
    }
    publish(state, data);
  }

  private static TtlRegisterData stateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.ONE, BITS);
      state.setData(data);
    }
    return data;
  }

  private static void clearAll(TtlRegisterData data) {
    for (var index = 0; index < BITS; index++) {
      data.setValue(index, Value.FALSE);
    }
  }

  /**
   * Writes the transparent mode. In demultiplexer mode every unselected latch is cleared. In
   * addressable-latch mode an unselected latch keeps the value it already stores.
   */
  private static void writeTransparent(
      InstanceState state, TtlRegisterData data, boolean clearUnselected) {
    final var dataIn = input(state, D);
    for (var index = 0; index < BITS; index++) {
      final var idle = clearUnselected ? Value.FALSE : data.getValue(index);
      data.setValue(index, levelFor(state, index, dataIn, idle));
    }
  }

  private static Value levelFor(InstanceState state, int index, Value dataIn, Value idle) {
    if (addressExcludes(state, index)) return idle;
    if (addressSelects(state, index)) return dataIn;
    return combine(dataIn, idle);
  }

  private static boolean addressExcludes(InstanceState state, int index) {
    for (var place = 0; place < ADDRESS.length; place++) {
      final var bit = input(state, ADDRESS[place]);
      final var expectedHigh = ((index >> place) & 1) == 1;
      if (expectedHigh && bit == Value.FALSE) return true;
      if (!expectedHigh && bit == Value.TRUE) return true;
    }
    return false;
  }

  private static boolean addressSelects(InstanceState state, int index) {
    if (addressExcludes(state, index)) return false;
    for (final var pin : ADDRESS) {
      final var bit = input(state, pin);
      if (bit != Value.TRUE && bit != Value.FALSE) return false;
    }
    return true;
  }

  private static Value combine(Value whenSelected, Value whenIdle) {
    if (whenSelected == whenIdle) return whenSelected;
    if (whenSelected == Value.ERROR || whenIdle == Value.ERROR) return Value.ERROR;
    if (whenSelected.isFullyDefined() && whenIdle.isFullyDefined()) return Value.ERROR;
    return Value.UNKNOWN;
  }

  private static void publish(InstanceState state, TtlRegisterData data) {
    for (var index = 0; index < BITS; index++) {
      state.setPort(pinNrToPortNr(OUTPUTS[index]), data.getValue(index), DELAY);
    }
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }
}
