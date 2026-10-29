/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;

/**
 * TTL 74x293: 4-bit binary ripple counter.
 *
 * <p>The digital function follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74ls293.pdf">SN74LS293</a> and
 * <a href="https://cdn-reichelt.de/documents/datenblatt/A200/HD74LS293.pdf">HD74LS293</a> data
 * sheets.
 * Those parts are electrically and functionally the same counter as the 7493; only the terminals
 * are rearranged, with GND on pin 7 and VCC on pin 14. A high-to-low edge on {@code A} toggles
 * {@code QA}. A high-to-low edge on {@code B} advances {@code QB}, {@code QC} and {@code QD}.
 * {@code QA} is not tied to {@code B} inside the package. Both {@code R0(1)} and {@code R0(2)}
 * high asynchronously clear every stage and override the clocks. One reset input does not.
 * Pins 1, 2, 3 and 6 are not connected. This is not an electrical model of a 74HC293.
 */
public class Ttl74293 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74293";

  public static final int PORT_INDEX_QC = 0;
  public static final int PORT_INDEX_QB = 1;
  public static final int PORT_INDEX_QD = 2;
  public static final int PORT_INDEX_QA = 3;
  public static final int PORT_INDEX_A = 4;
  public static final int PORT_INDEX_B = 5;
  public static final int PORT_INDEX_R0_1 = 6;
  public static final int PORT_INDEX_R0_2 = 7;

  private static final byte[] OUTPUT_PORTS = {4, 5, 8, 9};
  private static final byte[] UNUSED_PINS = {1, 2, 3, 6};
  private static final String[] PORT_NAMES = {
    "QC",
    "QB",
    "QD",
    "QA",
    "A (Clock A, falling edge)",
    "B (Clock B, falling edge)",
    "R0(1) (Reset, active high)",
    "R0(2) (Reset, active high)"
  };
  private static final BitWidth WIDTH = BitWidth.create(4);

  /** Creates the 74293. Power stays on the conventional pins 14 and 7. */
  public Ttl74293() {
    super(_ID, (byte) 14, OUTPUT_PORTS, UNUSED_PINS, PORT_NAMES, new Ttl74293HdlGenerator());
  }

  private static final int PINS = 14;
  private static final int BUBBLE_SIZE = 4;

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var rawState = painter.getData();
    final TtlRegisterData stored =
        rawState instanceof TtlRegisterData register ? register : null;
    // QA (9) follows clock A (10). QB (5), QC (4) and QD (8) follow clock B (11).
    drawSingleStage(g, x, y, height, true, 9, 10, 0, y + 14, stored);
    drawRippleChain(
        g, x, y, height, false, new int[] {5, 4}, new int[] {1, 2}, 11, y + height - 22, stored);
    drawQdStage(g, x, y, height, y + height - 22, y + 30, stored);
    drawAsyncReset(g, x, y, height, 12, 13, y + 14, y + height - 22, y + 30);
  }

  private static void drawQdStage(
      Graphics g,
      int x,
      int y,
      int height,
      int sourceRowTop,
      int rowTop,
      TtlRegisterData stored) {
    final var boxWidth = 10;
    final var boxHeight = 10;
    final var mid = rowTop + boxHeight / 2;
    final var center = structureX(x, PINS, 8);
    final var left = center - boxWidth / 2;
    final var right = left + boxWidth;
    final var pinY = structureY(y, height, PINS, 8);
    g.drawRect(left, rowTop, boxWidth, boxHeight);
    drawClockMark(g, left, mid);
    drawBit(g, center, mid, stored, 0, 3);
    g.drawLine(right, mid, center, mid);
    g.drawLine(center, mid, center, pinY);
    final var qcCenter = structureX(x, PINS, 4);
    final var qcMid = sourceRowTop + boxHeight / 2;
    final var qcNode = qcCenter + boxWidth / 2 + 3;
    g.drawLine(qcNode, qcMid, qcNode, mid);
    g.drawLine(qcNode, mid, left - BUBBLE_SIZE, mid);
    drawClockBubble(g, left, mid);
    drawNode(g, qcNode, qcMid);
  }

  private static void drawSingleStage(
      Graphics g,
      int x,
      int y,
      int height,
      boolean alongTop,
      int outputPin,
      int clockPin,
      int bit,
      int rowTop,
      TtlRegisterData stored) {
    final var boxWidth = 10;
    final var boxHeight = 10;
    final var center = structureX(x, PINS, outputPin);
    final var left = center - boxWidth / 2;
    final var right = left + boxWidth;
    final var mid = rowTop + boxHeight / 2;
    final var pinY = structureY(y, height, PINS, outputPin);
    final var towardPins = alongTop ? pinY + 2 : pinY - 2;
    g.drawRect(left, rowTop, boxWidth, boxHeight);
    drawClockMark(g, left, mid);
    drawBit(g, center, mid, stored, 0, bit);
    final var nodeX = right + 3;
    g.drawLine(right, mid, nodeX, mid);
    g.drawLine(nodeX, mid, nodeX, towardPins);
    g.drawLine(nodeX, towardPins, center, towardPins);
    g.drawLine(center, towardPins, center, pinY);
    final var clockX = structureX(x, PINS, clockPin);
    final var clockPinY = structureY(y, height, PINS, clockPin);
    g.drawLine(clockX, clockPinY, clockX, mid);
    g.drawLine(clockX, mid, left - BUBBLE_SIZE, mid);
    drawClockBubble(g, left, mid);
  }

  private static void drawRippleChain(
      Graphics g,
      int x,
      int y,
      int height,
      boolean outputsAlongTop,
      int[] outputPins,
      int[] bits,
      int clockPin,
      int rowTop,
      TtlRegisterData stored) {
    final var boxWidth = 10;
    final var boxHeight = 10;
    final var mid = rowTop + boxHeight / 2;
    final var towardPins = outputsAlongTop ? y + PIN_HEIGHT + 2 : y + height - PIN_HEIGHT - 2;
    for (var stage = 0; stage < outputPins.length; stage++) {
      final var center = structureX(x, PINS, outputPins[stage]);
      final var left = center - boxWidth / 2;
      final var right = left + boxWidth;
      final var pinY = structureY(y, height, PINS, outputPins[stage]);
      g.drawRect(left, rowTop, boxWidth, boxHeight);
      drawClockMark(g, left, mid);
      drawBit(g, center, mid, stored, 0, bits[stage]);
      final var nodeX = right + 3;
      g.drawLine(right, mid, nodeX, mid);
      g.drawLine(nodeX, mid, nodeX, towardPins);
      g.drawLine(nodeX, towardPins, center, towardPins);
      g.drawLine(center, towardPins, center, pinY);
      if (stage + 1 < outputPins.length) {
        final var nextLeft = structureX(x, PINS, outputPins[stage + 1]) - boxWidth / 2;
        g.drawLine(nodeX, mid, nextLeft - BUBBLE_SIZE, mid);
        drawClockBubble(g, nextLeft, mid);
        drawNode(g, nodeX, mid);
      }
    }
    final var firstLeft = structureX(x, PINS, outputPins[0]) - boxWidth / 2;
    final var clockX = structureX(x, PINS, clockPin);
    final var clockPinY = structureY(y, height, PINS, clockPin);
    g.drawLine(clockX, clockPinY, clockX, mid);
    g.drawLine(clockX, mid, firstLeft - BUBBLE_SIZE, mid);
    drawClockBubble(g, firstLeft, mid);
  }

  private static void drawAsyncReset(
      Graphics g,
      int x,
      int y,
      int height,
      int resetPinA,
      int resetPinB,
      int rowTopA,
      int rowTopB,
      int rowTopC) {
    final var r1 = structureX(x, PINS, resetPinA);
    final var r2 = structureX(x, PINS, resetPinB);
    final var topPinY = y + PIN_HEIGHT;
    final var gateMid = y + 22;
    g.drawLine(r1, topPinY, r1, gateMid);
    g.drawLine(r2, topPinY, r2, gateMid);
    g.drawRect(r1, gateMid - 4, r2 - r1, 8);
    final var mergeX = (r1 + r2) / 2;
    var railY = rowTopA - 3;
    g.drawLine(mergeX, gateMid + 4, mergeX, railY);
    for (final var segment :
        new int[][] {{rowTopA, 9}, {rowTopC, 8}, {rowTopB, 5}, {rowTopB, 4}}) {
      final var rail = segment[0] - 3;
      final var px = structureX(x, PINS, segment[1]);
      if (rail != railY) {
        g.drawLine(mergeX, railY, mergeX, rail);
        railY = rail;
      }
      g.drawLine(mergeX, rail, px, rail);
      g.drawLine(px, rail, px, segment[0]);
      drawNode(g, px, rail);
    }
    drawNode(g, mergeX, gateMid + 4);
  }

  private static void drawClockMark(Graphics g, int left, int mid) {
    g.drawLine(left, mid - 2, left + 3, mid);
    g.drawLine(left, mid + 2, left + 3, mid);
  }

  private static void drawClockBubble(Graphics g, int inputX, int mid) {
    g.drawOval(inputX - BUBBLE_SIZE, mid - BUBBLE_SIZE / 2, BUBBLE_SIZE, BUBBLE_SIZE);
  }

  private static void drawNode(Graphics g, int nodeX, int nodeY) {
    g.fillOval(nodeX - 2, nodeY - 2, 4, 4);
  }

  private static void drawBit(
      Graphics g, int centerX, int centerY, TtlRegisterData data, int word, int bit) {
    if (data == null) {
      return;
    }
    final var value = data.getValue(word);
    if (bit < 0 || bit >= value.getWidth()) {
      return;
    }
    final var bitValue = value.get(bit);
    g.setColor(bitValue.getColor());
    g.fillOval(centerX - 2, centerY - 2, 4, 4);
    g.setColor(Color.BLACK);
  }

  private static TtlRegisterData getStateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WIDTH);
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);
    final var clockATriggered =
        data.updateClock(state.getPortValue(PORT_INDEX_A), 0, StdAttr.TRIG_FALLING);
    final var clockBTriggered =
        data.updateClock(state.getPortValue(PORT_INDEX_B), 1, StdAttr.TRIG_FALLING);
    final var values = data.getValue().getAll();

    if (state.getPortValue(PORT_INDEX_R0_1) == Value.TRUE
        && state.getPortValue(PORT_INDEX_R0_2) == Value.TRUE) {
      data.setValue(Value.createKnown(WIDTH, 0));
    } else {
      if (clockATriggered) values[0] = values[0].not();
      if (clockBTriggered) {
        var carry = Value.TRUE;
        for (var i = 1; i < values.length; i++) {
          final var oldValue = values[i];
          values[i] = oldValue.xor(carry);
          carry = oldValue.and(carry);
        }
      }
      data.setValue(Value.create(values));
    }

    state.setPort(PORT_INDEX_QA, data.getValue().get(0), 4);
    state.setPort(PORT_INDEX_QB, data.getValue().get(1), 4);
    state.setPort(PORT_INDEX_QC, data.getValue().get(2), 4);
    state.setPort(PORT_INDEX_QD, data.getValue().get(3), 4);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_B, PORT_INDEX_A};
  }

  private static int structureX(int originX, int pins, int pin) {
    if (pin <= pins / 2) {
      return originX + (pin - 1) * 20 + 10;
    }
    return originX + (pins - pin) * 20 + 10;
  }

  private static int structureY(int originY, int bodyHeight, int pins, int pin) {
    if (pin <= pins / 2) {
      return originY + bodyHeight - AbstractTtlGate.PIN_HEIGHT;
    }
    return originY + AbstractTtlGate.PIN_HEIGHT;
  }

}
