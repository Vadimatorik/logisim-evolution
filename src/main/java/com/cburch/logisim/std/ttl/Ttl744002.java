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
 * TTL 74x4002: dual 4-input NOR gate.
 *
 * <p>Model based on the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC4002.pdf">Nexperia 74HC4002
 * datasheet</a>. Each output is high only when all four of its inputs are low. Pins 6 and 8 are
 * not connected. This is not the 4000-series CD4002, which uses a different pinout.
 *
 * <p>A high level on any input forces that output low, even if another input is unknown or an
 * error. With no high input, an error wins over an unknown value, and an unknown value wins over
 * low. Nanosecond delays are not modeled.
 */
public class Ttl744002 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744002";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet.
  public static final byte Y1 = 1;
  public static final byte A1 = 2;
  public static final byte B1 = 3;
  public static final byte C1 = 4;
  public static final byte D1 = 5;
  public static final byte GND = 7;
  public static final byte A2 = 9;
  public static final byte B2 = 10;
  public static final byte C2 = 11;
  public static final byte D2 = 12;
  public static final byte Y2 = 13;
  public static final byte VCC = 14;

  private static final byte[] OUTPUTS = {Y1, Y2};
  private static final byte[] UNUSED_PINS = {6, 8};
  private static final byte[] GATE1 = {A1, B1, C1, D1};
  private static final byte[] GATE2 = {A2, B2, C2, D2};

  private static final String[] PORT_NAMES = {
    "1Y", "1A", "1B", "1C", "1D", "2A", "2B", "2C", "2D", "2Y"
  };

  /** Creates a 744002 dual 4-input NOR gate. */
  public Ttl744002() {
    super(_ID, (byte) 14, OUTPUTS, UNUSED_PINS, PORT_NAMES, new Ttl744002HdlGenerator());
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Pins 6 and 8 are not connected, and the power pins are omitted from the port list. Pins 1
   * to 5 therefore keep a zero-based index, and pins 9 to 13 shift back by the three omitted pins.
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    return (byte) (dsPinNr <= D1 ? dsPinNr - 1 : dsPinNr - 4);
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
          "1Y", "1A", "1B", "1C", "1D", null, null, null,
          "2A", "2B", "2C", "2D", "2Y", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    state.setPort(pinNrToPortNr(Y1), nor(state, GATE1), DELAY);
    state.setPort(pinNrToPortNr(Y2), nor(state, GATE2), DELAY);
  }

  private static Value nor(InstanceState state, byte[] inputs) {
    var sawError = false;
    var sawUnknown = false;
    for (final var pin : inputs) {
      final var value = state.getPortValue(pinNrToPortNr(pin));
      if (value == Value.TRUE) {
        return Value.FALSE;
      }
      if (value == Value.ERROR) {
        sawError = true;
      } else if (value == Value.UNKNOWN) {
        sawUnknown = true;
      } else if (value != Value.FALSE) {
        sawError = true;
      }
    }
    if (sawError) {
      return Value.ERROR;
    }
    if (sawUnknown) {
      return Value.UNKNOWN;
    }
    return Value.TRUE;
  }
}
