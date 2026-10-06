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
 * TTL 74x365: hex non-inverting buffer/line driver with three-state outputs.
 *
 * <p>Model based on the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT365.pdf">Nexperia
 * 74HC365/74HCT365 datasheet</a>. {@code nOE1} and {@code nOE2} are active low and common to all
 * six buffers. While both are low, each output follows its data input. A high level on either
 * enable releases every output. The 74x367 is a different device: its two enables split the six
 * buffers into groups of four and two. High-impedance is represented as {@link Value#UNKNOWN}.
 * Nanosecond delays are not modeled.
 *
 * <p>An unknown or error value on either enable releases every output. While the device is
 * enabled, an unknown or error data input is copied to that channel only.
 */
public class Ttl74365 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74365";

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

  private static final String[] PORT_NAMES = {
    "nOE1 Output enable 1 (active low)",
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
    "nOE2 Output enable 2 (active low)"
  };

  /** Creates a 74365 hex buffer/line driver with three-state outputs. */
  public Ttl74365() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74365HdlGenerator());
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
          "nOE1", "1A", "1Y", "2A", "2Y", "3A", "3Y", null,
          "4Y", "4A", "5Y", "5A", "6Y", "6A", "nOE2", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var outputEnable1 = state.getPortValue(pinNrToPortNr(OE1));
    final var outputEnable2 = state.getPortValue(pinNrToPortNr(OE2));
    final var enabled = outputEnable1 == Value.FALSE && outputEnable2 == Value.FALSE;
    for (var index = 0; index < OUTPUTS.length; index++) {
      final var value =
          enabled ? state.getPortValue(pinNrToPortNr(INPUTS[index])) : Value.UNKNOWN;
      state.setPort(pinNrToPortNr(OUTPUTS[index]), value, DELAY);
    }
  }
}
