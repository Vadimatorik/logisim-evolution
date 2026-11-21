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

import static com.cburch.logisim.util.GraphicsUtil.H_CENTER;
import static com.cburch.logisim.util.GraphicsUtil.V_CENTER_OVERALL;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

/**
 * TTL 74x157: Quadruple 2-line to 1-line data selector
 *
 * <p>Model based on https://www.ti.com/product/SN74LS157 datasheet.
 */
public class Ttl74157 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74157";

  // IC pin indices as specified in the datasheet
  public static final byte SELECT = 1;
  public static final byte L1_A = 2;
  public static final byte L1_B = 3;
  public static final byte L1_Y = 4;
  public static final byte L2_A = 5;
  public static final byte L2_B = 6;
  public static final byte L2_Y = 7;
  public static final byte GND = 8;

  public static final byte L3_Y = 9;
  public static final byte L3_B = 10;
  public static final byte L3_A = 11;
  public static final byte L4_Y = 12;
  public static final byte L4_B = 13;
  public static final byte L4_A = 14;
  public static final byte STROBE = 15;
  public static final byte VCC = 16;

  public static final int DELAY = 1;

  // Needed for 74x158 implementation which is 74x157 with inverted output.
  protected final boolean invertOutput;

  protected static final String[] pinNames = {
    "SELECT", "1A", "1B", "1Y", "2A", "2B", "2Y",
    "3Y", "3B", "3A", "4Y", "4B", "4A", "nSTROBE (active LOW)"
  };

  private static final byte pinCount = 16;
  private static final byte[] outPins = {L1_Y, L2_Y, L3_Y, L4_Y};

  /** Long edges face the pin columns. Four inputs fit on the left with a step of 4. */
  private static final int BLOCK_WIDTH = 12;

  private static final int BLOCK_HEIGHT = 16;

  public Ttl74157() {
    super(_ID, pinCount, outPins, pinNames, null);
    invertOutput = false;
  }

  public Ttl74157(String icName, boolean invertOutput) {
    super(icName, pinCount, outPins, pinNames, null);
    this.invertOutput = invertOutput;
  }

  /**
   * Four 2-to-1 selectors. A low select passes A and a high select passes B. The common strobe is
   * active low: when it is high every output is low. Its bubble is on the input edge of each
   * selector. The 74158 adds a bubble on each output.
   */
  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.BOLD, 3));
    drawStructure(g, x, y, height);
    g.setFont(previousFont);
  }

  /**
   * Each selector is a 12×16 block. Data, select and strobe enter the long edge toward the data
   * pins; Y leaves the opposite edge toward its pin. Sections 4 and 3 stand on the top pins,
   * sections 1 and 2 on the bottom pins. Select crosses the strobe rail only at the middle of both
   * segments.
   */
  private void drawStructure(Graphics g, int x, int y, int height) {
    final var pinTop = y + PIN_HEIGHT;
    final var pinBottom = y + height - PIN_HEIGHT;
    drawBlocks(g, x, y);
    drawData(g, x, y, pinTop, pinBottom);
    drawOutputs(g, x, y, pinTop, pinBottom);
    drawSelect(g, x, y, pinBottom);
    drawStrobe(g, x, y, pinTop);
    drawNodes(g, x, y);
  }

  /** Dialog bold 3 "MUX" over the section number is 7×10 and sits inside the 12×16 block. */
  private static void drawBlocks(Graphics g, int x, int y) {
    drawBlock(g, x + 74, y + 11, 4);
    drawBlock(g, x + 134, y + 11, 3);
    drawBlock(g, x + 54, y + 33, 1);
    drawBlock(g, x + 114, y + 33, 2);
  }

  private static void drawBlock(Graphics g, int left, int top, int number) {
    g.drawRect(left, top, BLOCK_WIDTH, BLOCK_HEIGHT);
    // Bold "MUX" has a larger left bearing, so the advance-width center sits one pixel right.
    GraphicsUtil.drawText(
        g,
        "MUX\n" + number,
        left + BLOCK_WIDTH / 2 - 1,
        top + BLOCK_HEIGHT / 2,
        H_CENTER,
        V_CENTER_OVERALL);
  }

  /** B, the nearer pin, stops before A's lead. Both enter the left edge. */
  private static void drawData(Graphics g, int x, int y, int pinTop, int pinBottom) {
    wire(g, x + 70, pinTop, x + 70, y + 13, x + 74, y + 13);
    wire(g, x + 50, pinTop, x + 50, y + 17, x + 74, y + 17);
    wire(g, x + 130, pinTop, x + 130, y + 13, x + 134, y + 13);
    wire(g, x + 110, pinTop, x + 110, y + 17, x + 134, y + 17);
    wire(g, x + 50, pinBottom, x + 50, y + 47, x + 54, y + 47);
    wire(g, x + 30, pinBottom, x + 30, y + 43, x + 54, y + 43);
    wire(g, x + 110, pinBottom, x + 110, y + 47, x + 114, y + 47);
    wire(g, x + 90, pinBottom, x + 90, y + 43, x + 114, y + 43);
  }

  /** Y leaves the right edge and turns onto its own pin column. */
  private void drawOutputs(Graphics g, int x, int y, int pinTop, int pinBottom) {
    drawOutput(g, x + 86, y + 19, x + 90, pinTop);
    drawOutput(g, x + 146, y + 19, x + 150, pinTop);
    drawOutput(g, x + 66, y + 41, x + 70, pinBottom);
    drawOutput(g, x + 126, y + 41, x + 130, pinBottom);
  }

  /** The 74158 bubble stays on the output edge. The lead meets its far side along the axis. */
  private void drawOutput(Graphics g, int faceX, int outY, int columnX, int pinY) {
    if (invertOutput) {
      drawBubble(g, faceX + 1, outY);
      wire(g, faceX + 2, outY, columnX, outY, columnX, pinY);
    } else {
      wire(g, faceX, outY, columnX, outY, columnX, pinY);
    }
  }

  /**
   * Select from pin 1. The run at y=25 feeds sections 4 and the riser at x=70. That riser and the
   * one at x=104 cross the strobe rail at the middle of both segments. Sections 1 and 2 stay on
   * the pin side of the rail.
   */
  private static void drawSelect(Graphics g, int x, int y, int pinBottom) {
    wire(g, x + 10, pinBottom, x + 10, y + 25, x + 74, y + 25);
    wire(g, x + 10, y + 39, x + 54, y + 39);
    wire(g, x + 70, y + 25, x + 70, y + 33, x + 104, y + 33);
    wire(g, x + 104, y + 25, x + 104, y + 33, x + 104, y + 39, x + 114, y + 39);
    wire(g, x + 104, y + 25, x + 134, y + 25);
  }

  /**
   * Active-low strobe. The rail runs from the first tap to the last: section 1 on the left, section
   * 3 at the joint, section 2 at the right end, section 4 on the lead from the pin. Each bubble is
   * on the left edge.
   */
  private static void drawStrobe(Graphics g, int x, int y, int pinTop) {
    wire(g, x + 30, pinTop, x + 30, y + 21, x + 72, y + 21);
    wire(g, x + 42, y + 21, x + 42, y + 35, x + 52, y + 35);
    wire(g, x + 42, y + 29, x + 98, y + 29);
    wire(g, x + 98, y + 29, x + 98, y + 21, x + 132, y + 21);
    wire(g, x + 98, y + 29, x + 110, y + 29, x + 110, y + 35, x + 112, y + 35);
    drawBubble(g, x + 73, y + 21);
    drawBubble(g, x + 133, y + 21);
    drawBubble(g, x + 53, y + 35);
    drawBubble(g, x + 113, y + 35);
  }

  /** Branches only. A bend of one wire is not a joint. */
  private static void drawNodes(Graphics g, int x, int y) {
    drawNode(g, x + 10, y + 39);
    drawNode(g, x + 42, y + 29);
    drawNode(g, x + 70, y + 25);
    drawNode(g, x + 98, y + 29);
    drawNode(g, x + 104, y + 33);
  }

  // Port consts are datasheet based (1-indexed), but in code we have them 0-indexed
  // with GND, VCC omitted (thus indices are shifted comparing to datasheet numbers).
  protected byte mapPort(byte dsIdx) {
    return (byte) ((dsIdx <= GND) ? dsIdx - 1 : dsIdx - 2);
  }

  protected Value computeState(InstanceState state, byte inA, byte inB) {
    final var strobe = state.getPortValue(mapPort(STROBE)) == Value.TRUE;
    final var select = state.getPortValue(mapPort(SELECT)) == Value.TRUE;
    final var A = state.getPortValue(mapPort(inA)) == Value.TRUE;
    final var B = state.getPortValue(mapPort(inB)) == Value.TRUE;

    var Y = strobe ? false : (select ? B : A);
    if (this.invertOutput) Y = !Y;

    return Y ? Value.TRUE : Value.FALSE;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    state.setPort(mapPort(L1_Y), computeState(state, L1_A, L1_B), DELAY);
    state.setPort(mapPort(L2_Y), computeState(state, L2_A, L2_B), DELAY);
    state.setPort(mapPort(L3_Y), computeState(state, L3_A, L3_B), DELAY);
    state.setPort(mapPort(L4_Y), computeState(state, L4_A, L4_B), DELAY);
  }

  private static void wire(Graphics g, int... points) {
    for (var i = 2; i < points.length; i += 2) {
      g.drawLine(points[i - 2], points[i - 1], points[i], points[i + 1]);
    }
  }

  /** Active-low mark. Even 2×2 oval, center on the wire axis. */
  private static void drawBubble(Graphics g, int centerX, int centerY) {
    g.drawOval(centerX - 1, centerY - 1, 2, 2);
  }

  /** Junction of three branches. The oval center is the common point. */
  private static void drawNode(Graphics g, int centerX, int centerY) {
    g.fillOval(centerX - 1, centerY - 1, 2, 2);
  }
}
