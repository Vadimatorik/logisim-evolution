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
import java.awt.Font;
import java.awt.Graphics;

/**
 * TTL 74x4040: 12-stage binary ripple counter.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74hc4040.pdf">SN74HC4040</a> and
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT4040.pdf">74HC4040</a> data
 * sheets. Output {@code Qn} divides {@code CP} by {@code 2^n}. The counter advances on the
 * high-to-low transition of {@code CP}. A high {@code MR} asynchronously clears every stage.
 * Nexperia's 74HC4040 data sheet uses output names one lower for the same pins ({@code Q0} to
 * {@code Q11}).
 */
public class Ttl744040 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744040";

  public static final int PORT_INDEX_Q12 = 0;
  public static final int PORT_INDEX_Q6 = 1;
  public static final int PORT_INDEX_Q5 = 2;
  public static final int PORT_INDEX_Q7 = 3;
  public static final int PORT_INDEX_Q4 = 4;
  public static final int PORT_INDEX_Q3 = 5;
  public static final int PORT_INDEX_Q2 = 6;
  public static final int PORT_INDEX_Q1 = 7;
  public static final int PORT_INDEX_CP = 8;
  public static final int PORT_INDEX_MR = 9;
  public static final int PORT_INDEX_Q9 = 10;
  public static final int PORT_INDEX_Q8 = 11;
  public static final int PORT_INDEX_Q10 = 12;
  public static final int PORT_INDEX_Q11 = 13;

  private static final int DELAY = 4;
  private static final int STAGES = 12;
  private static final int STAGE_MASK = (1 << STAGES) - 1;
  private static final BitWidth WIDTH = BitWidth.create(STAGES);
  private static final byte[] OUTPUT_PORTS = {1, 2, 3, 4, 5, 6, 7, 9, 12, 13, 14, 15};
  private static final String[] PORT_NAMES = {
    "Q12",
    "Q6",
    "Q5",
    "Q7",
    "Q4",
    "Q3",
    "Q2",
    "Q1",
    "CP (Clock input)",
    "MR (Master reset, active high)",
    "Q9",
    "Q8",
    "Q10",
    "Q11"
  };
  /** Stage index of each visible output, parallel to {@code OUTPUT_PORT_INDEXES}. Bit 0 is Q1. */
  private static final int[] OUTPUT_STAGE_BITS = {11, 5, 4, 6, 3, 2, 1, 0, 8, 7, 9, 10};

  private static final int[] OUTPUT_PORT_INDEXES = {
    PORT_INDEX_Q12,
    PORT_INDEX_Q6,
    PORT_INDEX_Q5,
    PORT_INDEX_Q7,
    PORT_INDEX_Q4,
    PORT_INDEX_Q3,
    PORT_INDEX_Q2,
    PORT_INDEX_Q1,
    PORT_INDEX_Q9,
    PORT_INDEX_Q8,
    PORT_INDEX_Q10,
    PORT_INDEX_Q11
  };

  /** Creates a 744040 12-stage binary ripple counter. */
  public Ttl744040() {
    super(_ID, (byte) 16, OUTPUT_PORTS, PORT_NAMES, new Ttl744040HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 8));
    g.drawRect(x + 5, y + 28, 10, 10);
    g.drawRect(x + 25, y + 28, 10, 10);
    g.drawRect(x + 45, y + 28, 10, 10);
    g.drawRect(x + 65, y + 28, 10, 10);
    g.drawRect(x + 85, y + 28, 10, 10);
    g.drawRect(x + 105, y + 28, 10, 10);
    g.drawRect(x + 125, y + 28, 10, 10);
    g.drawRect(x + 145, y + 12, 10, 10);
    g.drawRect(x + 85, y + 12, 10, 10);
    g.drawRect(x + 65, y + 12, 10, 10);
    g.drawRect(x + 45, y + 12, 10, 10);
    g.drawRect(x + 25, y + 12, 10, 10);
    g.drawLine(x + 10, y + height - 22, x + 10, y + height - 7);
    g.drawLine(x + 30, y + height - 22, x + 30, y + height - 7);
    g.drawLine(x + 50, y + height - 22, x + 50, y + height - 7);
    g.drawLine(x + 70, y + height - 22, x + 70, y + height - 7);
    g.drawLine(x + 90, y + height - 22, x + 90, y + height - 7);
    g.drawLine(x + 110, y + height - 22, x + 110, y + height - 7);
    g.drawLine(x + 130, y + height - 22, x + 130, y + height - 7);
    g.drawLine(x + 150, y + 12, x + 150, y + 7);
    g.drawLine(x + 90, y + 12, x + 90, y + 7);
    g.drawLine(x + 70, y + 12, x + 70, y + 7);
    g.drawLine(x + 50, y + 12, x + 50, y + 7);
    g.drawLine(x + 30, y + 12, x + 30, y + 7);
    g.drawLine(x + 5, y + 24, x + 155, y + 24);
    g.drawLine(x + 130, y + 7, x + 130, y + 9);
    g.drawLine(x + 130, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 24);
    g.drawLine(x + 1, y + 24, x + 5, y + 24);
    g.drawLine(x + 7, y + 24, x + 7, y + 28);
    g.drawLine(x + 27, y + 24, x + 27, y + 28);
    g.drawLine(x + 47, y + 24, x + 47, y + 28);
    g.drawLine(x + 67, y + 24, x + 67, y + 28);
    g.drawLine(x + 87, y + 24, x + 87, y + 28);
    g.drawLine(x + 107, y + 24, x + 107, y + 28);
    g.drawLine(x + 127, y + 24, x + 127, y + 28);
    g.drawLine(x + 147, y + 24, x + 147, y + 22);
    g.drawLine(x + 87, y + 24, x + 87, y + 22);
    g.drawLine(x + 67, y + 24, x + 67, y + 22);
    g.drawLine(x + 47, y + 24, x + 47, y + 22);
    g.drawLine(x + 27, y + 24, x + 27, y + 22);
    g.drawLine(x + 5, y + 25, x + 155, y + 25);
    g.drawLine(x + 110, y + 7, x + 110, y + 9);
    g.drawLine(x + 110, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 25);
    g.drawLine(x + 1, y + 25, x + 5, y + 25);
    g.drawLine(x + 10, y + 25, x + 10, y + 28);
    g.drawLine(x + 30, y + 25, x + 30, y + 28);
    g.drawLine(x + 50, y + 25, x + 50, y + 28);
    g.drawLine(x + 70, y + 25, x + 70, y + 28);
    g.drawLine(x + 90, y + 25, x + 90, y + 28);
    g.drawLine(x + 110, y + 25, x + 110, y + 28);
    g.drawLine(x + 130, y + 25, x + 130, y + 28);
    g.drawLine(x + 150, y + 25, x + 150, y + 22);
    g.drawLine(x + 90, y + 25, x + 90, y + 22);
    g.drawLine(x + 70, y + 25, x + 70, y + 22);
    g.drawLine(x + 50, y + 25, x + 50, y + 22);
    g.drawLine(x + 30, y + 25, x + 30, y + 22);
    GraphicsUtil.drawCenteredText(g, "2", x + 10, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "6", x + 30, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "5", x + 50, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "7", x + 70, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "4", x + 90, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "3", x + 110, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "2", x + 130, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "1", x + 150, y + 17);
    GraphicsUtil.drawCenteredText(g, "9", x + 90, y + 17);
    GraphicsUtil.drawCenteredText(g, "8", x + 70, y + 17);
    GraphicsUtil.drawCenteredText(g, "0", x + 50, y + 17);
    GraphicsUtil.drawCenteredText(g, "1", x + 30, y + 17);
    g.setFont(previousFont);
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
        data.updateClock(state.getPortValue(PORT_INDEX_CP), StdAttr.TRIG_FALLING);
    if (state.getPortValue(PORT_INDEX_MR) == Value.TRUE) {
      data.setValue(Value.createKnown(WIDTH, 0));
    } else if (clocked) {
      data.setValue(nextCount(data.getValue()));
    }
    final var value = data.getValue();
    for (var i = 0; i < OUTPUT_PORT_INDEXES.length; i++) {
      state.setPort(OUTPUT_PORT_INDEXES[i], value.get(OUTPUT_STAGE_BITS[i]), DELAY);
    }
  }

  /**
   * Advances one binary count. An error anywhere makes the next word entirely error. A word that is
   * only unknown stays unknown. A known word increments modulo 2^12.
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
    return new int[] {PORT_INDEX_CP};
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

  private static void structureBubble(Graphics g, int centerX, int centerY) {
    g.drawOval(centerX - 2, centerY - 2, 4, 4);
  }

  private static void structureDot(
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
    g.fillOval(centerX - 3, centerY - 3, 6, 6);
    g.setColor(Color.WHITE);
    GraphicsUtil.drawCenteredText(g, bitValue.toDisplayString(), centerX, centerY);
    g.setColor(Color.BLACK);
  }

}
