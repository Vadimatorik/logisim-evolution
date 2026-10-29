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
 * TTL 74x12: triple 3-input NAND gate with open-drain outputs.
 *
 * <p>Model based on the <a href="https://www.ti.com/lit/ds/symlink/sn74ls12.pdf">SN74LS12
 * datasheet</a>. TI and Nexperia do not publish a 74HC12 data sheet; the pinout matches that
 * datasheet and the 74HC10. Each gate drives low only when all three inputs are high. Any other
 * determined combination releases the output. A released output is {@link Value#UNKNOWN}, so a
 * pull-up in the circuit can raise it. A low input releases the output regardless of the other
 * inputs. An unknown or error input leaves the output in error when no input is low. Input
 * thresholds of the CMOS and TTL variants are not modeled. There is no HDL model: an open-drain
 * high-impedance output is not a push-pull gate network.
 */
public class Ttl7412 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7412";

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

  public Ttl7412() {
    super(_ID, PIN_COUNT, OUTPUT_PINS, PORT_NAMES, null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    final var g = painter.getGraphics();
    g.setColor(Color.BLACK);
    final var top = y + AbstractTtlGate.PIN_HEIGHT;
    final var bottom = y + height - AbstractTtlGate.PIN_HEIGHT;
    drawNand(g, x + 50, y + 18, top, true, new int[] {x + 10, x + 30, x + 30}, new boolean[] {false, false, true}, bottom, top);
    drawNand(g, x + 110, y + 44, bottom, false, new int[] {x + 50, x + 70, x + 90}, new boolean[] {false, false, false}, bottom, top);
    drawNand(g, x + 130, y + 18, top, true, new int[] {x + 70, x + 90, x + 110}, new boolean[] {true, true, true}, bottom, top);
  }

  /**
   * One open-collector NAND. Inputs arrive at the left edge on their own rows and do not enter the
   * gate body.
   */
  private static void drawNand(
      Graphics g,
      int outputX,
      int outputY,
      int outputPinY,
      boolean outputOnTop,
      int[] inputX,
      boolean[] inputOnTop,
      int bottom,
      int top) {
    Drawgates.paintAnd(g, outputX, outputY, 10, 10, true);
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
      final var nand =
          state
              .getPortValue(gate[0])
              .and(state.getPortValue(gate[1]))
              .and(state.getPortValue(gate[2]))
              .not();
      state.setPort(gate[3], nand == Value.TRUE ? Value.UNKNOWN : nand, DELAY);
    }
  }
}
