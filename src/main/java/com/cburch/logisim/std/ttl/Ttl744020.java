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
 * TTL 74x4020: 14-stage binary ripple counter.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/cd74hc4020.pdf">CD74HC4020</a> and
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT4020.pdf">74HC4020</a> data
 * sheets. Output {@code Qn} divides {@code CP} by {@code 2^n}. The counter advances on the
 * high-to-low transition of {@code CP}. A high {@code MR} asynchronously clears every stage.
 * {@code Q2} and {@code Q3} are not brought out. Nexperia's 74HC4020 data sheet uses output names
 * one lower for the same pins ({@code Q0} and {@code Q3} to {@code Q13}).
 */
public class Ttl744020 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744020";

  public static final int PORT_INDEX_Q12 = 0;
  public static final int PORT_INDEX_Q13 = 1;
  public static final int PORT_INDEX_Q14 = 2;
  public static final int PORT_INDEX_Q6 = 3;
  public static final int PORT_INDEX_Q5 = 4;
  public static final int PORT_INDEX_Q7 = 5;
  public static final int PORT_INDEX_Q4 = 6;
  public static final int PORT_INDEX_Q1 = 7;
  public static final int PORT_INDEX_CP = 8;
  public static final int PORT_INDEX_MR = 9;
  public static final int PORT_INDEX_Q9 = 10;
  public static final int PORT_INDEX_Q8 = 11;
  public static final int PORT_INDEX_Q10 = 12;
  public static final int PORT_INDEX_Q11 = 13;

  private static final int DELAY = 4;
  private static final int STAGES = 14;
  private static final int STAGE_MASK = (1 << STAGES) - 1;
  private static final BitWidth WIDTH = BitWidth.create(STAGES);
  private static final byte[] OUTPUT_PORTS = {1, 2, 3, 4, 5, 6, 7, 9, 12, 13, 14, 15};
  private static final String[] PORT_NAMES = {
    "Q12",
    "Q13",
    "Q14",
    "Q6",
    "Q5",
    "Q7",
    "Q4",
    "Q1",
    "CP (Clock input)",
    "MR (Master reset, active high)",
    "Q9",
    "Q8",
    "Q10",
    "Q11"
  };
  /** Stage index of each visible output, parallel to {@code OUTPUT_PORT_INDEXES}. Bit 0 is Q1. */
  private static final int[] OUTPUT_STAGE_BITS = {11, 12, 13, 5, 4, 6, 3, 0, 8, 7, 9, 10};

  private static final int[] OUTPUT_PORT_INDEXES = {
    PORT_INDEX_Q12,
    PORT_INDEX_Q13,
    PORT_INDEX_Q14,
    PORT_INDEX_Q6,
    PORT_INDEX_Q5,
    PORT_INDEX_Q7,
    PORT_INDEX_Q4,
    PORT_INDEX_Q1,
    PORT_INDEX_Q9,
    PORT_INDEX_Q8,
    PORT_INDEX_Q10,
    PORT_INDEX_Q11
  };

  /** Creates a 744020 14-stage binary ripple counter. */
  public Ttl744020() {
    super(_ID, (byte) 16, OUTPUT_PORTS, PORT_NAMES, new Ttl744020HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var rawState = painter.getData();
    final TtlRegisterData stored = rawState instanceof TtlRegisterData ? (TtlRegisterData) rawState : null;
    g.drawRect((x + 14), (y + 11), 9, 12);
    g.drawLine((x + 150), (y + 7), (x + 150), (y + 13));
    g.drawLine((x + 150), (y + 13), (x + 18), (y + 13));
    g.drawLine((x + 18), (y + 13), (x + 18), (y + 11));
    g.drawLine((x + 14), (((y + 11)) + 6) - 3, ((x + 14)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 14), (((y + 11)) + 6) + 3, ((x + 14)) + 4, ((y + 11)) + 6);
    structureDot(g, (x + 18), ((y + 11)) + 6, stored, 0, 0);
    g.drawRect((x + 25), (y + 11), 9, 12);
    g.drawLine((x + 130), (y + height - 7), (x + 130), (y + height - 13));
    g.drawLine((x + 130), (y + height - 13), (x + 29), (y + height - 13));
    g.drawLine((x + 29), (y + height - 13), (x + 29), ((y + 11)) + 12);
    g.drawLine((x + 25), (((y + 11)) + 6) - 3, ((x + 25)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 25), (((y + 11)) + 6) + 3, ((x + 25)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 36), (y + 11), 9, 12);
    g.drawLine((x + 90), (y + height - 7), (x + 90), (y + height - 13));
    g.drawLine((x + 90), (y + height - 13), (x + 40), (y + height - 13));
    g.drawLine((x + 40), (y + height - 13), (x + 40), ((y + 11)) + 12);
    g.drawLine((x + 36), (((y + 11)) + 6) - 3, ((x + 36)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 36), (((y + 11)) + 6) + 3, ((x + 36)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 47), (y + 11), 9, 12);
    g.drawLine((x + 70), (y + height - 7), (x + 70), (y + height - 13));
    g.drawLine((x + 70), (y + height - 13), (x + 51), (y + height - 13));
    g.drawLine((x + 51), (y + height - 13), (x + 51), ((y + 11)) + 12);
    g.drawLine((x + 47), (((y + 11)) + 6) - 3, ((x + 47)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 47), (((y + 11)) + 6) + 3, ((x + 47)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 58), (y + 11), 9, 12);
    g.drawLine((x + 110), (y + height - 7), (x + 110), (y + height - 13));
    g.drawLine((x + 110), (y + height - 13), (x + 62), (y + height - 13));
    g.drawLine((x + 62), (y + height - 13), (x + 62), ((y + 11)) + 12);
    g.drawLine((x + 58), (((y + 11)) + 6) - 3, ((x + 58)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 58), (((y + 11)) + 6) + 3, ((x + 58)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 69), (y + 11), 9, 12);
    g.drawLine((x + 70), (y + 7), (x + 70), (y + 13));
    g.drawLine((x + 70), (y + 13), (x + 73), (y + 13));
    g.drawLine((x + 73), (y + 13), (x + 73), (y + 11));
    g.drawLine((x + 69), (((y + 11)) + 6) - 3, ((x + 69)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 69), (((y + 11)) + 6) + 3, ((x + 69)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 80), (y + 11), 9, 12);
    g.drawLine((x + 90), (y + 7), (x + 90), (y + 13));
    g.drawLine((x + 90), (y + 13), (x + 84), (y + 13));
    g.drawLine((x + 84), (y + 13), (x + 84), (y + 11));
    g.drawLine((x + 80), (((y + 11)) + 6) - 3, ((x + 80)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 80), (((y + 11)) + 6) + 3, ((x + 80)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 91), (y + 11), 9, 12);
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 13));
    g.drawLine((x + 50), (y + 13), (x + 95), (y + 13));
    g.drawLine((x + 95), (y + 13), (x + 95), (y + 11));
    g.drawLine((x + 91), (((y + 11)) + 6) - 3, ((x + 91)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 91), (((y + 11)) + 6) + 3, ((x + 91)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 102), (y + 11), 9, 12);
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 13));
    g.drawLine((x + 30), (y + 13), (x + 106), (y + 13));
    g.drawLine((x + 106), (y + 13), (x + 106), (y + 11));
    g.drawLine((x + 102), (((y + 11)) + 6) - 3, ((x + 102)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 102), (((y + 11)) + 6) + 3, ((x + 102)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 113), (y + 11), 9, 12);
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + height - 13));
    g.drawLine((x + 10), (y + height - 13), (x + 117), (y + height - 13));
    g.drawLine((x + 117), (y + height - 13), (x + 117), ((y + 11)) + 12);
    g.drawLine((x + 113), (((y + 11)) + 6) - 3, ((x + 113)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 113), (((y + 11)) + 6) + 3, ((x + 113)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 124), (y + 11), 9, 12);
    g.drawLine((x + 30), (y + height - 7), (x + 30), (y + height - 13));
    g.drawLine((x + 30), (y + height - 13), (x + 128), (y + height - 13));
    g.drawLine((x + 128), (y + height - 13), (x + 128), ((y + 11)) + 12);
    g.drawLine((x + 124), (((y + 11)) + 6) - 3, ((x + 124)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 124), (((y + 11)) + 6) + 3, ((x + 124)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 135), (y + 11), 9, 12);
    g.drawLine((x + 50), (y + height - 7), (x + 50), (y + height - 13));
    g.drawLine((x + 50), (y + height - 13), (x + 139), (y + height - 13));
    g.drawLine((x + 139), (y + height - 13), (x + 139), ((y + 11)) + 12);
    g.drawLine((x + 135), (((y + 11)) + 6) - 3, ((x + 135)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 135), (((y + 11)) + 6) + 3, ((x + 135)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 23), ((y + 11)) + 6, (x + 21), ((y + 11)) + 6);
    g.drawLine((x + 34), ((y + 11)) + 6, (x + 32), ((y + 11)) + 6);
    g.drawLine((x + 45), ((y + 11)) + 6, (x + 43), ((y + 11)) + 6);
    g.drawLine((x + 56), ((y + 11)) + 6, (x + 54), ((y + 11)) + 6);
    g.drawLine((x + 67), ((y + 11)) + 6, (x + 65), ((y + 11)) + 6);
    g.drawLine((x + 78), ((y + 11)) + 6, (x + 76), ((y + 11)) + 6);
    g.drawLine((x + 89), ((y + 11)) + 6, (x + 87), ((y + 11)) + 6);
    g.drawLine((x + 100), ((y + 11)) + 6, (x + 98), ((y + 11)) + 6);
    g.drawLine((x + 111), ((y + 11)) + 6, (x + 109), ((y + 11)) + 6);
    g.drawLine((x + 122), ((y + 11)) + 6, (x + 120), ((y + 11)) + 6);
    g.drawLine((x + 133), ((y + 11)) + 6, (x + 131), ((y + 11)) + 6);
    g.drawLine((x + 130), (y + 7), (x + 130), (y + 13));
    g.drawLine((x + 130), (y + 13), (x + 9), (y + 13));
    g.drawLine((x + 9), (y + 13), (x + 9), ((y + 11)) + 6);
    structureBubble(g, (x + 9), ((y + 11)) + 6);
    g.drawLine((x + 18), (y + 27), (x + 139), (y + 27));
    g.drawLine((x + 18), (y + 27), (x + 18), (y + 11));
    g.drawLine((x + 18), (y + 27), (x + 18), ((y + 11)) + 14);
    g.drawLine((x + 29), (y + 27), (x + 29), (y + 11));
    g.drawLine((x + 29), (y + 27), (x + 29), ((y + 11)) + 14);
    g.drawLine((x + 40), (y + 27), (x + 40), (y + 11));
    g.drawLine((x + 40), (y + 27), (x + 40), ((y + 11)) + 14);
    g.drawLine((x + 51), (y + 27), (x + 51), (y + 11));
    g.drawLine((x + 51), (y + 27), (x + 51), ((y + 11)) + 14);
    g.drawLine((x + 62), (y + 27), (x + 62), (y + 11));
    g.drawLine((x + 62), (y + 27), (x + 62), ((y + 11)) + 14);
    g.drawLine((x + 73), (y + 27), (x + 73), (y + 11));
    g.drawLine((x + 73), (y + 27), (x + 73), ((y + 11)) + 14);
    g.drawLine((x + 84), (y + 27), (x + 84), (y + 11));
    g.drawLine((x + 84), (y + 27), (x + 84), ((y + 11)) + 14);
    g.drawLine((x + 95), (y + 27), (x + 95), (y + 11));
    g.drawLine((x + 95), (y + 27), (x + 95), ((y + 11)) + 14);
    g.drawLine((x + 106), (y + 27), (x + 106), (y + 11));
    g.drawLine((x + 106), (y + 27), (x + 106), ((y + 11)) + 14);
    g.drawLine((x + 117), (y + 27), (x + 117), (y + 11));
    g.drawLine((x + 117), (y + 27), (x + 117), ((y + 11)) + 14);
    g.drawLine((x + 128), (y + 27), (x + 128), (y + 11));
    g.drawLine((x + 128), (y + 27), (x + 128), ((y + 11)) + 14);
    g.drawLine((x + 139), (y + 27), (x + 139), (y + 11));
    g.drawLine((x + 139), (y + 27), (x + 139), ((y + 11)) + 14);
    g.drawLine((x + 110), (y + 7), (x + 110), (y + 13));
    g.drawLine((x + 110), (y + 13), (x + 18), (y + 13));
    g.drawLine((x + 18), (y + 13), (x + 18), (y + 27));
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
