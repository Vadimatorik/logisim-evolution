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
 * TTL 74x40: dual 4-input NAND buffer.
 *
 * <p>Model based on the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74ls40.pdf">TI SN74LS40 datasheet</a> (SDLS108).
 * Each output is low only when all four of its inputs are high. Pins 3 and 11 are not connected.
 * 74HC40 uses the same pinout and Boolean function. The higher output current that distinguishes
 * a NAND buffer from a 7420 NAND gate is not modeled.
 *
 * <p>A low level on any input forces that output high, even if another input is unknown or an
 * error. With no low input, an error wins over an unknown value, and an unknown value wins over
 * high. Nanosecond delays are not modeled.
 */
public class Ttl7440 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7440";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet.
  public static final byte A1 = 1;
  public static final byte B1 = 2;
  public static final byte C1 = 4;
  public static final byte D1 = 5;
  public static final byte Y1 = 6;
  public static final byte GND = 7;
  public static final byte Y2 = 8;
  public static final byte A2 = 9;
  public static final byte B2 = 10;
  public static final byte C2 = 12;
  public static final byte D2 = 13;
  public static final byte VCC = 14;

  private static final byte[] OUTPUTS = {Y1, Y2};
  private static final byte[] UNUSED_PINS = {3, 11};
  private static final byte[] GATE1 = {A1, B1, C1, D1};
  private static final byte[] GATE2 = {A2, B2, C2, D2};

  private static final String[] PORT_NAMES = {
    "1A", "1B", "1C", "1D", "1Y", "2Y", "2A", "2B", "2C", "2D"
  };

  /** Creates a 7440 dual 4-input NAND buffer. */
  public Ttl7440() {
    super(_ID, (byte) 14, OUTPUTS, UNUSED_PINS, PORT_NAMES, new Ttl7440HdlGenerator());
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Pins 3 and 11 are not connected, and the power pins are omitted from the port list. Pins 1
   * and 2 therefore keep a zero-based index, pins 4 to 6 shift back by one, pins 8 to 10 shift
   * back by three, and pins 12 and 13 shift back by four.
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    if (dsPinNr <= B1) {
      return (byte) (dsPinNr - 1);
    }
    if (dsPinNr <= Y1) {
      return (byte) (dsPinNr - 2);
    }
    if (dsPinNr <= B2) {
      return (byte) (dsPinNr - 3);
    }
    return (byte) (dsPinNr - 4);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    final var g = painter.getGraphics();
    // Same pin columns as the 7420: 1Y on pin 6, 2Y on pin 8, and pins 3 and 11 unused.
    Drawgates.paintAnd(g, x + 117, y + 20, 10, 10, true);
    Drawgates.paintAnd(g, x + 97, y + 40, 10, 10, true);
    g.drawLine(x + 121, y + 20, x + 130, y + 20);
    g.drawLine(x + 130, y + PIN_HEIGHT, x + 130, y + 20);
    g.drawLine(x + 101, y + 40, x + 110, y + 40);
    g.drawLine(x + 110, y + height - PIN_HEIGHT, x + 110, y + 40);
    for (var i = 0; i < 5; i++) {
      if (i == 2) {
        continue;
      }
      g.drawLine(
          x + 10 + i * 20, y + height - PIN_HEIGHT, x + 10 + i * 20, y + 36 + i * 2);
      g.drawLine(x + 10 + i * 20, y + 36 + i * 2, x + 87, y + 36 + i * 2);
      g.drawLine(x + 30 + i * 20, y + PIN_HEIGHT, x + 30 + i * 20, y + 24 - i * 2);
      g.drawLine(x + 30 + i * 20, y + 24 - i * 2, x + 107, y + 24 - i * 2);
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    state.setPort(pinNrToPortNr(Y1), nand(state, GATE1), DELAY);
    state.setPort(pinNrToPortNr(Y2), nand(state, GATE2), DELAY);
  }

  private static Value nand(InstanceState state, byte[] inputs) {
    var sawError = false;
    var sawUnknown = false;
    for (final var pin : inputs) {
      final var value = state.getPortValue(pinNrToPortNr(pin));
      if (value == Value.FALSE) {
        return Value.TRUE;
      }
      if (value == Value.ERROR) {
        sawError = true;
      } else if (value == Value.UNKNOWN) {
        sawUnknown = true;
      } else if (value != Value.TRUE) {
        sawError = true;
      }
    }
    if (sawError) {
      return Value.ERROR;
    }
    if (sawUnknown) {
      return Value.UNKNOWN;
    }
    return Value.FALSE;
  }
}
