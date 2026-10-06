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
import com.cburch.logisim.prefs.AppPreferences;

import java.awt.Graphics;

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
  private static final int GATE_WIDTH = 28;
  private static final int GATE_HEIGHT = 16;
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
    super.paintBase(painter, false, false);
    final var g = painter.getGraphics();
    final var inset = AppPreferences.GATE_SHAPE.get().equals(AppPreferences.SHAPE_SHAPED) ? 4 : 0;
    // 1Y and 2Y sit to the left of their inputs, as on the 4072. Pins 6 and 8 stay open.
    paintHalf(g, x, y, height, false, inset);
    paintHalf(g, x, y, height, true, inset);
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

  /**
   * Draws one NOR gate. Pin columns are 20 apart and start at {@code x + 10}. The bottom half uses
   * pins 1 to 5; the top half uses pins 9 to 13.
   */
  private static void paintHalf(Graphics g, int x, int y, int height, boolean top, int inset) {
    final int gateX = top ? x + 40 : x + 96;
    final int gateY = top ? y + 22 : y + 38;
    final int outputX = top ? x + 30 : x + 10;
    final int pinY = top ? y + PIN_HEIGHT : y + height - PIN_HEIGHT;
    final int[] columns =
        top
            ? new int[] {x + 50, x + 70, x + 90, x + 110}
            : new int[] {x + 30, x + 50, x + 70, x + 90};
    Drawgates.paintOr(g, gateX, gateY, GATE_WIDTH, GATE_HEIGHT, true, true);
    g.drawPolyline(new int[] {gateX - 4, outputX, outputX}, new int[] {gateY, gateY, pinY}, 3);
    final var inputX = gateX + GATE_WIDTH - inset;
    for (var i = 0; i < columns.length; i++) {
      final var rail = top ? y + 10 + i : y + 48 + i;
      final var entry = top ? y + 16 + i * 4 : y + 32 + i * 4;
      g.drawPolyline(
          new int[] {columns[i], columns[i], inputX, inputX},
          new int[] {pinY, rail, rail, entry},
          4);
    }
  }

}
