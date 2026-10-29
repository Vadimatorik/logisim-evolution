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
 * TTL 74x4024: 7-stage binary ripple counter.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/cd54hc4024.pdf">CD74HC4024</a> and
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC4024.pdf">74HC4024</a> data
 * sheets. Output {@code Qn} divides {@code CP} by {@code 2^n}. The counter advances on the
 * high-to-low transition of {@code CP}. A high {@code MR} asynchronously clears every stage.
 * Nexperia's 74HC4024 data sheet uses output names one lower for the same pins ({@code Q0} to
 * {@code Q6}). Pins 8, 10 and 13 are not connected.
 */
public class Ttl744024 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744024";

  public static final int PORT_INDEX_CP = 0;
  public static final int PORT_INDEX_MR = 1;
  public static final int PORT_INDEX_Q7 = 2;
  public static final int PORT_INDEX_Q6 = 3;
  public static final int PORT_INDEX_Q5 = 4;
  public static final int PORT_INDEX_Q4 = 5;
  public static final int PORT_INDEX_Q3 = 6;
  public static final int PORT_INDEX_Q2 = 7;
  public static final int PORT_INDEX_Q1 = 8;

  private static final int DELAY = 4;
  private static final int STAGES = 7;
  private static final int STAGE_MASK = (1 << STAGES) - 1;
  private static final BitWidth WIDTH = BitWidth.create(STAGES);
  private static final byte[] OUTPUT_PORTS = {3, 4, 5, 6, 9, 11, 12};
  private static final byte[] UNUSED_PINS = {8, 10, 13};
  private static final String[] PORT_NAMES = {
    "CP (Clock input)",
    "MR (Master reset, active high)",
    "Q7",
    "Q6",
    "Q5",
    "Q4",
    "Q3",
    "Q2",
    "Q1"
  };
  /** Stage index of each visible output, parallel to {@code OUTPUT_PORT_INDEXES}. Bit 0 is Q1. */
  private static final int[] OUTPUT_STAGE_BITS = {6, 5, 4, 3, 2, 1, 0};

  private static final int[] OUTPUT_PORT_INDEXES = {
    PORT_INDEX_Q7,
    PORT_INDEX_Q6,
    PORT_INDEX_Q5,
    PORT_INDEX_Q4,
    PORT_INDEX_Q3,
    PORT_INDEX_Q2,
    PORT_INDEX_Q1
  };

  /** Creates a 744024 7-stage binary ripple counter. */
  public Ttl744024() {
    super(_ID, (byte) 14, OUTPUT_PORTS, UNUSED_PINS, PORT_NAMES, new Ttl744024HdlGenerator());
  }

    @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var bodyFont = g.getFont();
    g.setFont(bodyFont.deriveFont(9f));
    g.drawRect((x + 12), (y + 18), 122, 24);
    GraphicsUtil.drawCenteredText(g, "CNT", (x + 73), (y + 30));
    g.setFont(bodyFont);
    final var top = y + PIN_HEIGHT;
    final var bottom = y + height - PIN_HEIGHT;
    g.drawLine((x + 10), bottom, (x + 10), (y + 42));
    structureBubble(g, (x + 30), (y + 42));
    g.drawLine((x + 30), bottom, (x + 30), (y + 44));
    g.drawLine((x + 50), bottom, (x + 50), (y + 42));
    g.drawLine((x + 70), bottom, (x + 70), (y + 42));
    g.drawLine((x + 90), bottom, (x + 90), (y + 42));
    g.drawLine((x + 110), bottom, (x + 110), (y + 42));
    g.drawLine((x + 130), bottom, (x + 130), (y + 42));
    g.drawLine((x + 130), top, (x + 130), (y + 18));
    g.drawLine((x + 110), top, (x + 110), (y + 18));
    g.drawLine((x + 90), top, (x + 90), (y + 18));
    g.drawLine((x + 70), top, (x + 70), (y + 18));
    g.drawLine((x + 50), top, (x + 50), (y + 18));
    g.drawLine((x + 30), top, (x + 30), (y + 18));
    g.drawLine((x + 10), top, (x + 10), (y + 18));
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
   * only unknown stays unknown. A known word increments modulo 2^7.
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
