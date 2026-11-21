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
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/**
 * TTL 74x4060: 14-stage binary ripple counter.
 *
 * <p>Simulation follows the
 * <a href="https://www.onsemi.com/download/data-sheet/pdf/mc74hc4060a-d.pdf">MC74HC4060A</a> and
 * <a href="https://www.ti.com/lit/ds/symlink/cd74hc4060.pdf">CD74HC4060</a> data sheets, where output
 * {@code Qn} divides {@code RS} by {@code 2^n}. The counter advances on the high-to-low transition
 * of {@code RS}. A high {@code MR} asynchronously clears every stage. Pin names follow the
 * Nexperia 74HC4060 pinout: {@code Q3} is pin 7 and {@code Q13} is pin 3. {@code RTC} is the
 * NAND output and {@code CTC} is the following inverter; an external clock drives {@code RS}.
 */
public class Ttl744060 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744060";

  public static final int PORT_INDEX_Q12 = 0;
  public static final int PORT_INDEX_Q13 = 1;
  public static final int PORT_INDEX_Q14 = 2;
  public static final int PORT_INDEX_Q6 = 3;
  public static final int PORT_INDEX_Q5 = 4;
  public static final int PORT_INDEX_Q7 = 5;
  public static final int PORT_INDEX_Q4 = 6;
  public static final int PORT_INDEX_CTC = 7;
  public static final int PORT_INDEX_RTC = 8;
  public static final int PORT_INDEX_RS = 9;
  public static final int PORT_INDEX_MR = 10;
  public static final int PORT_INDEX_Q9 = 11;
  public static final int PORT_INDEX_Q8 = 12;
  public static final int PORT_INDEX_Q10 = 13;

  private static final int DELAY = 4;
  private static final int STAGES = 14;
  private static final int STAGE_MASK = (1 << STAGES) - 1;
  private static final BitWidth WIDTH = BitWidth.create(STAGES);
  private static final byte[] OUTPUT_PORTS = {1, 2, 3, 4, 5, 6, 7, 9, 10, 13, 14, 15};
  private static final String[] PORT_NAMES = {
    "Q11",
    "Q12",
    "Q13",
    "Q5",
    "Q4",
    "Q6",
    "Q3",
    "CTC (External capacitor)",
    "RTC (External resistor)",
    "RS (Clock input)",
    "MR (Master reset, active HIGH)",
    "Q8",
    "Q7",
    "Q9"
  };
  /** Stage index of each visible output, parallel to {@code OUTPUT_PORT_INDEXES}. Bit 0 is Q1. */
  private static final int[] OUTPUT_STAGE_BITS = {11, 12, 13, 5, 4, 6, 3, 8, 7, 9};

  private static final int[] OUTPUT_PORT_INDEXES = {
    PORT_INDEX_Q12,
    PORT_INDEX_Q13,
    PORT_INDEX_Q14,
    PORT_INDEX_Q6,
    PORT_INDEX_Q5,
    PORT_INDEX_Q7,
    PORT_INDEX_Q4,
    PORT_INDEX_Q9,
    PORT_INDEX_Q8,
    PORT_INDEX_Q10
  };

  /** Creates a 744060 14-stage binary ripple counter. */
  public Ttl744060() {
    super(_ID, (byte) 16, OUTPUT_PORTS, PORT_NAMES, null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    drawStructure(g, x, y, height);
  }

  /**
   * Oscillator NAND, a following inverter, and one block for the fourteen ripple stages. An
   * inverted MR holds the NAND output high, so the oscillator stops while the counter is cleared.
   * With MR low, RTC is the inverse of RS and CTC follows RS; the bubbled clock counts on the
   * falling edge. Q1, Q2, Q3 and Q11 stay inside the block.
   */
  private static void drawStructure(Graphics g, int x, int y, int height) {
    final var previousFont = g.getFont();
    drawSymbols(g, x, y);
    drawWires(g, x, y, height);
    g.setFont(previousFont);
  }

  /** NAND and NOT follow the selected gate shape. The counter block is the ripple chain. */
  private static void drawSymbols(Graphics g, int x, int y) {
    final var rectangular =
        AppPreferences.GATE_SHAPE.get().equals(AppPreferences.SHAPE_RECTANGULAR);
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 6));
    drawNand(g, x, y, rectangular);
    drawInverter(g, x, y, rectangular);
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 7));
    g.drawRect(x + 44, y + 16, 42, 14);
    GraphicsUtil.drawText(
        g, "COUNTER", x + 65, y + 23, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER_OVERALL);
    drawBubble(g, x + 87, y + 27);
  }

  /** Output toward RTC, RS on the upper input, inverted MR on the lower input. */
  private static void drawNand(Graphics g, int x, int y, boolean rectangular) {
    final var left = x + 114;
    final var top = y + 11;
    final var right = left + 10;
    final var bottom = top + 12;
    final var midY = top + 6;
    if (rectangular) {
      g.drawRect(left, top, 10, 12);
      GraphicsUtil.drawText(
          g, "&", left + 5, midY, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER_OVERALL);
    } else {
      drawAndShape(g, left, top, right, bottom);
    }
    drawBubble(g, left - 1, y + 19);
    drawBubble(g, right + 1, midY);
  }

  /** CTC leaves the right-hand point. The input is the center of the opposite side. */
  private static void drawInverter(Graphics g, int x, int y, boolean rectangular) {
    final var left = x + 134;
    final var top = y + 11;
    final var right = left + 10;
    final var bottom = top + 12;
    final var midY = top + 6;
    if (rectangular) {
      g.drawRect(left, top, 10, 12);
      GraphicsUtil.drawText(
          g, "1", left + 5, midY, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER_OVERALL);
    } else {
      drawInverterShape(g, left, top, right, midY, bottom);
    }
    drawBubble(g, right + 1, midY);
  }

  /** Arc nose stops one pixel before the output bubble. The flat back is the input face. */
  private static void drawAndShape(Graphics g, int left, int top, int right, int bottom) {
    final var midY = (top + bottom) / 2;
    final var radius = (bottom - top) / 2;
    final var arcX = right - 1 - radius;
    g.drawLine(left, top, arcX, top);
    g.drawLine(left, bottom, arcX, bottom);
    g.drawLine(left, top, left, bottom);
    GraphicsUtil.drawCenteredArc(g, arcX, midY, radius, -90, 180);
  }

  /** Closed triangle. Bevel join keeps the apex on the bubble instead of mitering into it. */
  private static void drawInverterShape(
      Graphics g, int left, int top, int apexX, int apexY, int bottom) {
    final var g2 = (Graphics2D) g;
    final var previous = g2.getStroke();
    g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
    g.drawPolygon(new int[] {left, left, apexX}, new int[] {top, bottom, apexY}, 3);
    g2.setStroke(previous);
  }

  /**
   * Pin 12 runs straight left into the counter and straight right into the NAND. Clock enters the
   * same side. The output bus leaves the opposite side.
   */
  private static void drawWires(Graphics g, int x, int y, int height) {
    final var pinTop = PIN_HEIGHT;
    final var pinBottom = height - PIN_HEIGHT;
    wire(g, x, y, 110, pinTop, 110, 15, 114, 15);
    wire(g, x, y, 90, pinTop, 90, 19);
    wire(g, x, y, 86, 19, 112, 19);
    wire(g, x, y, 126, 17, 134, 17);
    wire(g, x, y, 130, pinTop, 130, 17);
    wire(g, x, y, 146, 17, 150, 17, 150, pinTop);
    wire(g, x, y, 150, 17, 150, 27, 88, 27);
    drawNode(g, x + 90, y + 19);
    drawNode(g, x + 130, y + 17);
    drawNode(g, x + 150, y + 17);
    drawBus(g, x, y, 29.5f, 12f, 70.5f, 12f);
    drawBus(g, x, y, 40f, 13f, 40f, 47f);
    drawBus(g, x, y, 9.5f, 48f, 130.5f, 48f);
    drawBus(g, x, y, 41f, 23f, 44f, 23f);
    wire(g, x, y, 30, pinTop, 30, 12);
    wire(g, x, y, 50, pinTop, 50, 12);
    wire(g, x, y, 70, pinTop, 70, 12);
    for (var pinX : new int[] {10, 30, 50, 70, 90, 110, 130}) {
      wire(g, x, y, pinX, 48, pinX, pinBottom);
    }
  }

  private static void wire(Graphics g, int x, int y, int... points) {
    for (var i = 2; i < points.length; i += 2) {
      g.drawLine(x + points[i - 2], y + points[i - 1], x + points[i], y + points[i + 1]);
    }
  }

  /** Width-2 bus. A corner is one path so the join does not leave a white square. */
  private static void drawBus(Graphics g, int x, int y, float... points) {
    final var g2 = (Graphics2D) g;
    final var previous = g2.getStroke();
    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
    final var path = new Path2D.Float();
    path.moveTo(x + points[0], y + points[1]);
    for (var i = 2; i < points.length; i += 2) {
      path.lineTo(x + points[i], y + points[i + 1]);
    }
    g2.draw(path);
    g2.setStroke(previous);
  }

  /** Even 2×2 inversion bubble. The center sits on the wire axis, one pixel outside the block. */
  private static void drawBubble(Graphics g, int centerX, int centerY) {
    g.drawOval(centerX - 1, centerY - 1, 2, 2);
  }

  /** Junction of three or more branches. The oval center is the common point. */
  private static void drawNode(Graphics g, int centerX, int centerY) {
    g.fillOval(centerX - 1, centerY - 1, 2, 2);
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
    final var clocked =
        data.updateClock(state.getPortValue(PORT_INDEX_RS), StdAttr.TRIG_FALLING);
    if (state.getPortValue(PORT_INDEX_MR) == Value.TRUE) {
      data.setValue(Value.createKnown(WIDTH, 0));
    } else if (clocked) {
      data.setValue(nextCount(data.getValue()));
    }
    final var value = data.getValue();
    for (var i = 0; i < OUTPUT_PORT_INDEXES.length; i++) {
      state.setPort(OUTPUT_PORT_INDEXES[i], value.get(OUTPUT_STAGE_BITS[i]), DELAY);
    }
    final var rtc =
        state.getPortValue(PORT_INDEX_RS).and(state.getPortValue(PORT_INDEX_MR).not()).not();
    state.setPort(PORT_INDEX_RTC, rtc, DELAY);
    state.setPort(PORT_INDEX_CTC, rtc.not(), DELAY);
  }

  /**
   * Advances one binary count. An error anywhere makes the next word entirely error. A word that is
   * only unknown stays unknown. A known word increments modulo 2^14.
   */
  private static Value nextCount(Value current) {
    if (containsBit(current, Value.ERROR)) return Value.createError(WIDTH);
    if (!current.isFullyDefined()) return Value.createUnknown(WIDTH);
    return Value.createKnown(WIDTH, (current.toLongValue() + 1) & STAGE_MASK);
  }

  private static boolean containsBit(Value word, Value bit) {
    for (var i = 0; i < word.getWidth(); i++) {
      if (word.get(i) == bit) return true;
    }
    return false;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_RS};
  }
}
