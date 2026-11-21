/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Marcin Orlowski (http://MarcinOrlowski.com), 2021.
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/**
 * TTL 74x139 Dual 2-line to 4-line decoders/multiplexers Model based on
 * https://www.ti.com/product/SN74LS139A datasheet.
 */
public class Ttl74139 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74139";

  // IC pin indices as specified in the datasheet
  public static final byte L1_nEN = 1;
  public static final byte L1_A = 2;
  public static final byte L1_B = 3;
  public static final byte L1_nY0 = 4;
  public static final byte L1_nY1 = 5;
  public static final byte L1_nY2 = 6;
  public static final byte L1_nY3 = 7;
  public static final byte GND = 8;

  public static final byte L2_nY3 = 9;
  public static final byte L2_nY2 = 10;
  public static final byte L2_nY1 = 11;
  public static final byte L2_nY0 = 12;
  public static final byte L2_B = 13;
  public static final byte L2_A = 14;
  public static final byte L2_nEN = 15;
  public static final byte VCC = 16;

  public static final int DELAY = 1;

  /** Tall enough for a 5 px label with the text bounds inside the block. */
  private static final int BLOCK_HEIGHT = 10;

  /** Section 2: inputs on the top edge, output bus on the bottom edge. */
  private static final int UPPER_LEFT = 25;

  private static final int UPPER_TOP = 16;

  private static final int UPPER_WIDTH = 60;

  /** Section 1, clear of the notch: inputs on the bottom edge, output bus on the top edge. */
  private static final int LOWER_LEFT = 12;

  private static final int LOWER_TOP = 34;

  private static final int LOWER_WIDTH = 32;

  private static final int UPPER_BUS_Y = 12;

  private static final int LOWER_ADDRESS_Y = 48;

  /** Below the lower block and 4 px clear of the address bus. */
  private static final int LOWER_OUTPUT_Y = 44;

  private static final int CONTACT_MARGIN = 5;

  public Ttl74139() {
    super(
        _ID,
        (byte) 16,
        new byte[] {
          L1_nY0, L1_nY1, L1_nY2, L1_nY3,
          L2_nY0, L2_nY1, L2_nY2, L2_nY3
        },
        new String[] {
          "1nG Enable (active LOW)", "1A", "1B", "1nY0", "1nY1", "1nY2", "1nY3",
          "2nY3", "2nY2", "2nY1", "2nY0", "2B", "2A", "2nG Enable (active LOW)"
        },
        null);
  }

  /**
   * Two independent 2-to-4 decoders. Enable and the address bus sit on the long edge toward the
   * input pins; the active-low output bus leaves the opposite long edge. The side edges stay empty.
   */
  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, false, false);
    final var previousFont = g.getFont();
    g.setColor(Color.BLACK);
    g.setFont(new Font(Font.DIALOG, Font.BOLD, 5));
    final var pinTop = y + PIN_HEIGHT;
    final var pinBottom = y + height - PIN_HEIGHT;
    drawBlock(g, x + UPPER_LEFT, y + UPPER_TOP, UPPER_WIDTH, true, "DEC2");
    drawBlock(g, x + LOWER_LEFT, y + LOWER_TOP, LOWER_WIDTH, false, "DEC1");
    final var address2 = (pinX(x, L2_A) + pinX(x, L2_B)) / 2;
    drawAddress(g, x, pinTop, address2, y + UPPER_TOP, y + UPPER_BUS_Y, L2_A, L2_B);
    drawEnable(g, pinX(x, L2_nEN), x + UPPER_LEFT + CONTACT_MARGIN, pinTop, y + UPPER_TOP - 2);
    drawUpperOutputs(g, x, y, pinTop);
    drawAddress(
        g,
        x,
        pinBottom,
        x + LOWER_LEFT + LOWER_WIDTH - CONTACT_MARGIN,
        y + LOWER_TOP + BLOCK_HEIGHT,
        y + LOWER_ADDRESS_Y,
        L1_A,
        L1_B);
    drawEnable(
        g,
        pinX(x, L1_nEN),
        x + LOWER_LEFT + CONTACT_MARGIN,
        pinBottom,
        y + LOWER_TOP + BLOCK_HEIGHT + 2);
    drawLowerOutputs(g, x, y, pinBottom);
    g.setFont(previousFont);
  }

  /** Enable bubble on the input edge, output bubble on the opposite long edge. */
  private static void drawBlock(
      Graphics2D g, int left, int top, int width, boolean inputsOnTop, String name) {
    final var bottom = top + BLOCK_HEIGHT;
    final var midX = left + width / 2;
    final var midY = top + BLOCK_HEIGHT / 2;
    g.drawRect(left, top, width, BLOCK_HEIGHT);
    bubble(g, left + CONTACT_MARGIN, inputsOnTop ? top - 1 : bottom + 1);
    bubble(g, midX, inputsOnTop ? bottom + 1 : top - 1);
    GraphicsUtil.drawText(
        g, name, midX, midY, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER_OVERALL);
  }

  /**
   * Address bus of one section. The run ends half a pixel outside the extreme taps, and the spur
   * stops one pixel outside the bus centre so the joint has no gap and no tail.
   */
  private static void drawAddress(
      Graphics2D g,
      int originX,
      int pinY,
      int addressX,
      int blockEdgeY,
      int busY,
      byte pinA,
      byte pinB) {
    final var tapA = pinX(originX, pinA);
    final var tapB = pinX(originX, pinB);
    final var leftEnd = Math.min(addressX, Math.min(tapA, tapB));
    final var rightEnd = Math.max(addressX, Math.max(tapA, tapB));
    bus(g, leftEnd - 0.5f, busY, rightEnd + 0.5f, busY);
    final var joinY = blockEdgeY < busY ? busY - 1 : busY + 1;
    bus(g, addressX, blockEdgeY, addressX, joinY);
    g.drawLine(tapA, pinY, tapA, busY);
    g.drawLine(tapB, pinY, tapB, busY);
  }

  /**
   * Active-low enable. The lead stops on the far side of the bubble. When the pin column is not the
   * contact column, the lead turns four pixels inside the package edge and then follows the bubble
   * axis.
   */
  private static void drawEnable(Graphics2D g, int pinColumn, int contactColumn, int pinY, int farSideY) {
    if (pinColumn == contactColumn) {
      g.drawLine(pinColumn, pinY, pinColumn, farSideY);
      return;
    }
    final var bendY = pinY > farSideY ? pinY - 4 : pinY + 4;
    g.drawLine(pinColumn, pinY, pinColumn, bendY);
    g.drawLine(pinColumn, bendY, contactColumn, bendY);
    g.drawLine(contactColumn, bendY, contactColumn, farSideY);
  }

  /**
   * Section 2 output bus. It leaves the bottom edge, runs to the right of the address spur, then
   * joins the row of top output pins.
   */
  private static void drawUpperOutputs(Graphics2D g, int originX, int originY, int pinY) {
    final var column = originX + UPPER_LEFT + UPPER_WIDTH / 2;
    final var farSide = originY + UPPER_TOP + BLOCK_HEIGHT + 2;
    final var corridor = farSide + 6;
    final var riser = originX + UPPER_LEFT + UPPER_WIDTH + 3;
    final var busY = originY + UPPER_BUS_Y;
    final var lastTap = pinX(originX, L2_nY3);
    bus(g, column, farSide, column, corridor, riser, corridor, riser, busY, lastTap + 0.5f, busY);
    for (var pin : new byte[] {L2_nY0, L2_nY1, L2_nY2, L2_nY3}) {
      final var tap = pinX(originX, pin);
      g.drawLine(tap, pinY, tap, busY);
    }
  }

  /**
   * Section 1 output bus. It leaves the top edge, stays left of the upper output, and drops beside
   * the block onto its own level, 4 px clear of the address bus.
   */
  private static void drawLowerOutputs(Graphics2D g, int originX, int originY, int pinY) {
    final var column = originX + LOWER_LEFT + LOWER_WIDTH / 2;
    final var farSide = originY + LOWER_TOP - 2;
    final var corridor = farSide - 2;
    final var riser = originX + LOWER_LEFT + LOWER_WIDTH + 4;
    final var busY = originY + LOWER_OUTPUT_Y;
    final var lastTap = pinX(originX, L1_nY3);
    bus(g, column, farSide, column, corridor, riser, corridor, riser, busY, lastTap + 0.5f, busY);
    for (var pin : new byte[] {L1_nY0, L1_nY1, L1_nY2, L1_nY3}) {
      final var tap = pinX(originX, pin);
      g.drawLine(tap, pinY, tap, busY);
    }
  }

  private static int pinX(int originX, int pin) {
    if (pin <= GND) {
      return originX + (pin - 1) * 20 + 10;
    }
    return originX + (VCC - pin) * 20 + 10;
  }

  /** Active-low mark. Even 2×2 oval, centre on the wire axis. */
  private static void bubble(Graphics2D g, int centerX, int centerY) {
    g.drawOval(centerX - 1, centerY - 1, 2, 2);
  }

  /** Width-2 bus. A corner is one polyline so the joint has no gap. */
  private static void bus(Graphics2D g, float... points) {
    final var previous = g.getStroke();
    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
    final var path = new Path2D.Float();
    path.moveTo(points[0], points[1]);
    for (var i = 2; i < points.length; i += 2) {
      path.lineTo(points[i], points[i + 1]);
    }
    g.draw(path);
    g.setStroke(previous);
  }

  // Port consts are datasheet based (1-indexed), but in code we have them 0-indexed
  // with GND, VCC omitted (thus indices are shifted comparing to datasheet numbers).
  protected byte mapPort(byte dsIdx) {
    return (byte) ((dsIdx <= GND) ? dsIdx - 1 : dsIdx - 2);
  }

  protected void computeState(InstanceState state, byte inEn, byte inA, byte inB, byte[] outPorts) {
    final var enabled = state.getPortValue(mapPort(inEn)) == Value.FALSE; // Active LOW
    final var A = state.getPortValue(mapPort(inA)) == Value.TRUE ? (byte) 1 : 0;
    final var B = state.getPortValue(mapPort(inB)) == Value.TRUE ? (byte) 2 : 0;

    final int[][] outputPortStates = {
      {1, 0, 0, 0},
      {0, 1, 0, 0},
      {0, 0, 1, 0},
      {0, 0, 0, 1},
    };

    for (var i = 0; i < 4; i++) { // Active LOW
      final var val =
          enabled ? (outputPortStates[A + B][i] == 0 ? Value.TRUE : Value.FALSE) : Value.TRUE;
      state.setPort(mapPort(outPorts[i]), val, DELAY);
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    byte[] out1 = {L1_nY0, L1_nY1, L1_nY2, L1_nY3};
    computeState(state, L1_nEN, L1_A, L1_B, out1);
    byte[] out2 = {L2_nY0, L2_nY1, L2_nY2, L2_nY3};
    computeState(state, L2_nEN, L2_A, L2_B, out2);
  }
}
