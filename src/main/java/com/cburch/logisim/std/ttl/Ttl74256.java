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
 * TTL 74256: dual 4-bit addressable latch.
 *
 * <p>Simulation follows the Motorola SN74LS256 function table. Philips 74F256 uses the same DIP-16
 * pinout and the same four modes; it names pin 15 {@code MR} instead of {@code CL}. There is no
 * 74HC256 data sheet. Nanosecond delays are not modeled.
 *
 * <p>{@code CL} low and {@code E} high clears both sections. Both low make the addressed output of
 * each section follow its data input and force the other three outputs of that section low. {@code
 * CL} high and {@code E} low make only the addressed latch of each section follow its data input.
 * Both high hold every latch. The sections share {@code A0}, {@code A1}, {@code E} and {@code CL}.
 *
 * <p>A control that is not exactly high or low drives every output unknown, or error when that
 * control is an error, and leaves the stored bits unchanged. An address bit that is not exactly
 * high or low makes each latch that might still be selected combine the selected and unselected
 * results: a 0/1 conflict is an error, and any other disagreement is unknown. Unknown or error data
 * is written only into a latch that the address can still select.
 */
public class Ttl74256 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74256";

  public static final int DELAY = 1;

  public static final byte A0 = 1;
  public static final byte A1 = 2;
  public static final byte Da = 3;
  public static final byte Q0a = 4;
  public static final byte Q1a = 5;
  public static final byte Q2a = 6;
  public static final byte Q3a = 7;
  public static final byte GND = 8;
  public static final byte Q0b = 9;
  public static final byte Q1b = 10;
  public static final byte Q2b = 11;
  public static final byte Q3b = 12;
  public static final byte Db = 13;
  /** Enable, active low. */
  public static final byte E = 14;
  /** Clear, active low. Philips 74F256 calls this pin MR. */
  public static final byte CL = 15;
  public static final byte VCC = 16;

  private static final int SECTION_BITS = 4;
  private static final byte[] ADDRESS = {A0, A1};
  private static final byte[] OUTPUTS = {Q0a, Q1a, Q2a, Q3a, Q0b, Q1b, Q2b, Q3b};
  private static final byte[] DATA = {Da, Db};
  private static final String[] PORT_NAMES = {
    "A0 address",
    "A1 address",
    "Da data",
    "Q0a",
    "Q1a",
    "Q2a",
    "Q3a",
    "Q0b",
    "Q1b",
    "Q2b",
    "Q3b",
    "Db data",
    "nE (enable, active low)",
    "nCL / MR (clear, active low)"
  };

  /** Creates a 74256 dual 4-bit addressable latch. */
  public Ttl74256() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74256HdlGenerator());
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
          "A0", "A1", "Da", "Q0a", "Q1a", "Q2a", "Q3a", null,
          "Q0b", "Q1b", "Q2b", "Q3b", "Db", "nE", "nCL", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = stateData(state);
    final var enable = input(state, E);
    final var clear = input(state, CL);
    if (!isBinary(enable) || !isBinary(clear)) {
      publishLevel(state, enable == Value.ERROR || clear == Value.ERROR ? Value.ERROR : Value.UNKNOWN);
      return;
    }
    if (clear == Value.FALSE && enable == Value.TRUE) {
      clearAll(data);
    } else if (clear == Value.FALSE && enable == Value.FALSE) {
      writeTransparent(state, data, true);
    } else if (clear == Value.TRUE && enable == Value.FALSE) {
      writeTransparent(state, data, false);
    }
    publish(state, data);
  }

  private static TtlRegisterData stateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.ONE, DATA.length * SECTION_BITS);
      state.setData(data);
    }
    return data;
  }

  private static void clearAll(TtlRegisterData data) {
    for (var index = 0; index < DATA.length * SECTION_BITS; index++) {
      data.setValue(index, Value.FALSE);
    }
  }

  /**
   * Writes the transparent mode. In demultiplexer mode every unselected latch is cleared. In
   * addressable-latch mode an unselected latch keeps the value it already stores.
   */
  private static void writeTransparent(
      InstanceState state, TtlRegisterData data, boolean clearUnselected) {
    for (var section = 0; section < DATA.length; section++) {
      final var dataIn = input(state, DATA[section]);
      final var base = section * SECTION_BITS;
      for (var index = 0; index < SECTION_BITS; index++) {
        final var stored = data.getValue(base + index);
        final var idle = clearUnselected ? Value.FALSE : stored;
        data.setValue(base + index, levelFor(state, index, dataIn, idle));
      }
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
    for (var index = 0; index < OUTPUTS.length; index++) {
      state.setPort(pinNrToPortNr(OUTPUTS[index]), data.getValue(index), DELAY);
    }
  }

  private static void publishLevel(InstanceState state, Value level) {
    for (final var pin : OUTPUTS) {
      state.setPort(pinNrToPortNr(pin), level, DELAY);
    }
  }

  private static boolean isBinary(Value value) {
    return value == Value.TRUE || value == Value.FALSE;
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }
}
