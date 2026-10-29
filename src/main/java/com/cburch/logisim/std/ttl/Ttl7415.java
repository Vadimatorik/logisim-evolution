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
import java.awt.Color;
import java.awt.Graphics;

/**
 * TTL 74x15: triple 3-input AND gate with open-collector outputs.
 *
 * <p>Model based on the
 * <a href="https://www.electronicoscaldas.com/datasheet/DM74LS15_National.pdf">DM74LS15 datasheet</a>
 * and the
 * <a href="https://hallaweb.jlab.org/tech/Detectors/public_html/manuals/chip_specs/M-Z/motorola/74ls15.pdf">SN74LS15
 * datasheet</a>. The DIP pinout matches the 7411. Each gate releases its output only when every
 * input is high and drives the output low when any input is low. A released output is {@link
 * Value#UNKNOWN}, so a pull-up in the circuit can raise it. An unknown or error input leaves the
 * output in error, unless another input is low: a low input drives the output low regardless of
 * the other inputs. There is no HDL model: an open-collector high-impedance output is not a
 * push-pull gate network.
 */
public class Ttl7415 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7415";

  public static final int PORT_INDEX_1A = 0;
  public static final int PORT_INDEX_1B = 1;
  public static final int PORT_INDEX_2A = 2;
  public static final int PORT_INDEX_2B = 3;
  public static final int PORT_INDEX_2C = 4;
  public static final int PORT_INDEX_2Y = 5;
  public static final int PORT_INDEX_3Y = 6;
  public static final int PORT_INDEX_3A = 7;
  public static final int PORT_INDEX_3B = 8;
  public static final int PORT_INDEX_3C = 9;
  public static final int PORT_INDEX_1Y = 10;
  public static final int PORT_INDEX_1C = 11;

  private static final int DELAY = 1;
  private static final byte PIN_COUNT = 14;
  private static final byte[] OUTPUT_PINS = {6, 8, 12};
  private static final String[] PORT_NAMES = {
    "1A", "1B", "2A", "2B", "2C", "2Y", "3Y", "3A", "3B", "3C", "1Y", "1C"
  };
  private static final int[][] GATES = {
    {PORT_INDEX_1A, PORT_INDEX_1B, PORT_INDEX_1C, PORT_INDEX_1Y},
    {PORT_INDEX_2A, PORT_INDEX_2B, PORT_INDEX_2C, PORT_INDEX_2Y},
    {PORT_INDEX_3A, PORT_INDEX_3B, PORT_INDEX_3C, PORT_INDEX_3Y}
  };

  public Ttl7415() {
    super(_ID, PIN_COUNT, OUTPUT_PINS, null, null, PORT_NAMES, false, DEFAULT_HEIGHT, null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    final var g = painter.getGraphics();
    g.setColor(Color.BLACK);
    final var top = y + AbstractTtlGate.PIN_HEIGHT;
    final var bottom = y + height - AbstractTtlGate.PIN_HEIGHT;
    drawAnd(g, x + 50, y + 18, top, true, new int[] {x + 10, x + 30, x + 30}, new boolean[] {false, false, true}, bottom, top);
    drawAnd(g, x + 110, y + 44, bottom, false, new int[] {x + 50, x + 70, x + 90}, new boolean[] {false, false, false}, bottom, top);
    drawAnd(g, x + 130, y + 18, top, true, new int[] {x + 70, x + 90, x + 110}, new boolean[] {true, true, true}, bottom, top);
  }

  /** One open-collector AND. Each input stops on the left edge of its own gate. */
  private static void drawAnd(
      Graphics g,
      int outputX,
      int outputY,
      int outputPinY,
      boolean outputOnTop,
      int[] inputX,
      boolean[] inputOnTop,
      int bottom,
      int top) {
    Drawgates.paintAnd(g, outputX, outputY, 10, 10, false);
    final var diamondY = outputOnTop ? outputY - 8 : outputY + 8;
    Drawgates.paintOpenCollector(g, outputX, diamondY);
    g.drawLine(outputX, outputY, outputX, diamondY);
    g.drawLine(outputX, outputOnTop ? diamondY - 3 : diamondY + 3, outputX, outputPinY);
    final var inputEdge = outputX - 10;
    for (var i = 0; i < inputX.length; i++) {
      final var row = outputY - 4 + i * 4;
      final var pinY = inputOnTop[i] ? top : bottom;
      g.drawLine(inputX[i], pinY, inputX[i], row);
      g.drawLine(inputX[i], row, inputEdge, row);
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    for (final var gate : GATES) {
      final var and =
          state
              .getPortValue(gate[0])
              .and(state.getPortValue(gate[1]))
              .and(state.getPortValue(gate[2]));
      state.setPort(gate[3], and == Value.TRUE ? Value.UNKNOWN : and, DELAY);
    }
  }
}
