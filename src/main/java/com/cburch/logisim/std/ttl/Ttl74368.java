/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x368: hex inverting buffer/line driver with three-state outputs.
 *
 * <p>Model based on the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT368.pdf">Nexperia
 * 74HC368/74HCT368 datasheet</a> and the ON Semiconductor MC74HC368A pinout. {@code n1OE} and
 * {@code n2OE} are active low and independent. {@code n1OE} enables buffers 1 through 4. {@code
 * n2OE} enables buffers 5 and 6. While an enable is low, each output in its group is the
 * complement of its data input. A high level releases only that group. Nexperia numbers the
 * channels {@code 1A}–{@code 6A}; Texas Instruments calls the same pins {@code 1A1}–{@code 1A4}
 * and {@code 2A1}–{@code 2A2}. High-impedance is represented as {@link Value#UNKNOWN}. Nanosecond
 * delays are not modeled.
 *
 * <p>An unknown enable releases only that enable's group. An error on an enable drives an error on
 * that group, because the output is neither reliably released nor reliably driving. While a group
 * is enabled, a defined input is inverted and an unknown or error input is left unchanged on that
 * channel only. {@link Value#not()} is not used, because it turns an unknown bit into an error.
 */
public class Ttl74368 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74368";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet.
  public static final byte OE1 = 1;
  public static final byte A1 = 2;
  public static final byte Y1 = 3;
  public static final byte A2 = 4;
  public static final byte Y2 = 5;
  public static final byte A3 = 6;
  public static final byte Y3 = 7;
  public static final byte GND = 8;
  public static final byte Y4 = 9;
  public static final byte A4 = 10;
  public static final byte Y5 = 11;
  public static final byte A5 = 12;
  public static final byte Y6 = 13;
  public static final byte A6 = 14;
  public static final byte OE2 = 15;
  public static final byte VCC = 16;

  /** Data inputs in channel order. */
  private static final byte[] INPUTS = {A1, A2, A3, A4, A5, A6};

  /** Data outputs in channel order. */
  private static final byte[] OUTPUTS = {Y1, Y2, Y3, Y4, Y5, Y6};

  /** Number of buffers controlled by {@code n1OE}. The rest belong to {@code n2OE}. */
  private static final int GROUP1_SIZE = 4;

  private static final String[] PORT_NAMES = {
    "n1OE Output enable 1 (active LOW)",
    "1A",
    "1Y",
    "2A",
    "2Y",
    "3A",
    "3Y",
    "4Y",
    "4A",
    "5Y",
    "5A",
    "6Y",
    "6A",
    "n2OE Output enable 2 (active LOW)"
  };

  /** Creates a 74368 hex inverting buffer/line driver with three-state outputs. */
  public Ttl74368() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74368HdlGenerator());
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

  /**
   * Inverts a defined bit and leaves unknown or error values unchanged.
   *
   * @param value data input
   * @return inverted value
   */
  private static Value invert(Value value) {
    if (value == Value.TRUE) {
      return Value.FALSE;
    }
    if (value == Value.FALSE) {
      return Value.TRUE;
    }
    return value;
  }

  /**
   * Resolves one channel from its active-low enable and data input.
   *
   * @param enable output-enable input
   * @param data data input
   * @return driven or released output
   */
  private static Value drivenValue(Value enable, Value data) {
    if (enable == Value.FALSE) {
      return invert(data);
    }
    if (enable == Value.ERROR) {
      return Value.ERROR;
    }
    return Value.UNKNOWN;
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
          "n1OE", "1A", "1Y", "2A", "2Y", "3A", "3Y", null,
          "4Y", "4A", "5Y", "5A", "6Y", "6A", "n2OE", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var outputEnable1 = state.getPortValue(pinNrToPortNr(OE1));
    final var outputEnable2 = state.getPortValue(pinNrToPortNr(OE2));
    for (var index = 0; index < OUTPUTS.length; index++) {
      final var enable = index < GROUP1_SIZE ? outputEnable1 : outputEnable2;
      final var data = state.getPortValue(pinNrToPortNr(INPUTS[index]));
      state.setPort(pinNrToPortNr(OUTPUTS[index]), drivenValue(enable, data), DELAY);
    }
  }
}
