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
 * TTL 74x4072: dual 4-input OR gate.
 *
 * <p>Simulation follows the
 * <a href="https://media.digikey.com/pdf/Data%20Sheets/ST%20Microelectronics%20PDFS/M74HC4072.pdf">M74HC4072</a>
 * data sheet. Each output is high when any of its four inputs is high, and low only when all four
 * are low. Pins 6 and 8 are not connected. Toshiba's TC74HC4072A is pin-compatible with the 4072B
 * and marks those same pins as not connected.
 */
public class Ttl744072 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744072";

  public static final int Y1 = 0;
  public static final int A1 = 1;
  public static final int B1 = 2;
  public static final int C1 = 3;
  public static final int D1 = 4;
  public static final int A2 = 5;
  public static final int B2 = 6;
  public static final int C2 = 7;
  public static final int D2 = 8;
  public static final int Y2 = 9;

  private static final int DELAY = 1;
  private static final int GATE_WIDTH = 28;
  private static final int GATE_HEIGHT = 16;
  private static final byte PIN_COUNT = 14;
  private static final byte[] OUTPUT_PINS = {1, 13};
  private static final byte[] UNUSED_PINS = {6, 8};
  private static final String[] PORT_NAMES = {
    "1Y", "1A", "1B", "1C", "1D", "2A", "2B", "2C", "2D", "2Y"
  };

  public Ttl744072() {
    super(_ID, PIN_COUNT, OUTPUT_PINS, UNUSED_PINS, PORT_NAMES, new Ttl744072HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    final var g = painter.getGraphics();
    final var inset = AppPreferences.GATE_SHAPE.get().equals(AppPreferences.SHAPE_SHAPED) ? 4 : 0;
    // Outputs sit to the left of their inputs, so each OR symbol faces left.
    paintHalf(g, x, y, height, false, inset);
    paintHalf(g, x, y, height, true, inset);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    state.setPort(Y1, orInputs(state, A1, B1, C1, D1), DELAY);
    state.setPort(Y2, orInputs(state, A2, B2, C2, D2), DELAY);
  }

  private static Value orInputs(InstanceState state, int a, int b, int c, int d) {
    return state
        .getPortValue(a)
        .or(state.getPortValue(b))
        .or(state.getPortValue(c))
        .or(state.getPortValue(d));
  }

  /**
   * Draws one gate. Pin columns are 20 apart and start at {@code x + 10}. The bottom half uses
   * pins 1 to 5; the top half uses pins 9 to 13.
   */
  private static void paintHalf(
      Graphics g, int x, int y, int height, boolean top, int inset) {
    final int gateX = top ? x + 40 : x + 96;
    final int gateY = top ? y + 22 : y + 38;
    final int outputX = top ? x + 30 : x + 10;
    final int pinY = top ? y + PIN_HEIGHT : y + height - PIN_HEIGHT;
    final int[] columns =
        top
            ? new int[] {x + 50, x + 70, x + 90, x + 110}
            : new int[] {x + 30, x + 50, x + 70, x + 90};
    Drawgates.paintOr(g, gateX, gateY, GATE_WIDTH, GATE_HEIGHT, false, true);
    g.drawPolyline(new int[] {gateX, outputX, outputX}, new int[] {gateY, gateY, pinY}, 3);
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
