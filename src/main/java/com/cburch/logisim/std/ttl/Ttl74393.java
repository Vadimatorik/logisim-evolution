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
import java.awt.Graphics2D;

/**
 * TTL 74x393: dual 4-bit binary ripple counter.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT393.pdf">Nexperia 74HC393</a>
 * data sheet. Each half advances on the high-to-low transition of {@code nCP}. {@code nQ0} is the
 * least significant bit, and the count wraps from 15 to 0. A high {@code nMR} asynchronously
 * clears that half and overrides its clock. The two counters are independent. Ripple delays are
 * not modeled: a falling edge updates the whole nibble in one step.
 */
public class Ttl74393 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74393";

  public static final int PORT_INDEX_1CP = 0;
  public static final int PORT_INDEX_1MR = 1;
  public static final int PORT_INDEX_1Q0 = 2;
  public static final int PORT_INDEX_1Q1 = 3;
  public static final int PORT_INDEX_1Q2 = 4;
  public static final int PORT_INDEX_1Q3 = 5;
  public static final int PORT_INDEX_2Q3 = 6;
  public static final int PORT_INDEX_2Q2 = 7;
  public static final int PORT_INDEX_2Q1 = 8;
  public static final int PORT_INDEX_2Q0 = 9;
  public static final int PORT_INDEX_2MR = 10;
  public static final int PORT_INDEX_2CP = 11;

  private static final int COUNTER_1 = 0;
  private static final int COUNTER_2 = 1;
  private static final int BITS = 4;
  private static final int STAGE_MASK = (1 << BITS) - 1;
  private static final int DELAY = 4;
  private static final BitWidth WIDTH = BitWidth.create(BITS);
  private static final byte[] OUTPUT_PINS = {3, 4, 5, 6, 8, 9, 10, 11};
  private static final String[] PORT_NAMES = {
    "1CP (Clock input, HIGH-to-LOW)",
    "1MR (Master reset, active HIGH)",
    "1Q0",
    "1Q1",
    "1Q2",
    "1Q3",
    "2Q3",
    "2Q2",
    "2Q1",
    "2Q0",
    "2MR (Master reset, active HIGH)",
    "2CP (Clock input, HIGH-to-LOW)"
  };
  private static final int[] OUTPUT_PORT_INDEXES = {
    PORT_INDEX_1Q0,
    PORT_INDEX_1Q1,
    PORT_INDEX_1Q2,
    PORT_INDEX_1Q3,
    PORT_INDEX_2Q0,
    PORT_INDEX_2Q1,
    PORT_INDEX_2Q2,
    PORT_INDEX_2Q3
  };
  private static final int[] OUTPUT_COUNTERS = {
    COUNTER_1, COUNTER_1, COUNTER_1, COUNTER_1, COUNTER_2, COUNTER_2, COUNTER_2, COUNTER_2
  };
  private static final int[] OUTPUT_BITS = {0, 1, 2, 3, 0, 1, 2, 3};

  /** Creates a 74393 dual 4-bit binary ripple counter. */
  public Ttl74393() {
    super(_ID, (byte) 14, OUTPUT_PINS, PORT_NAMES, new Ttl74393HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var gfx = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, false, false);
    Drawgates.paintPortNamesByPin(
        painter,
        x,
        y,
        height,
        new String[] {
          "1CP", "1MR", "1Q0", "1Q1", "1Q2", "1Q3", null,
          "2Q3", "2Q2", "2Q1", "2Q0", "2MR", "2CP", null
        });
    drawState(gfx, x, y, (TtlRegisterData) painter.getData());
  }

  private void drawState(Graphics2D gfx, int x, int y, TtlRegisterData state) {
    if (state == null) return;
    drawCounter(gfx, x + 46, y + 19, "1", state.getValue(COUNTER_1));
    drawCounter(gfx, x + 46, y + 31, "2", state.getValue(COUNTER_2));
    gfx.setColor(Color.BLACK);
  }

  private static void drawCounter(Graphics2D gfx, int x, int y, String label, Value word) {
    gfx.setColor(Color.BLACK);
    GraphicsUtil.drawCenteredText(gfx, label, x, y + 4);
    for (var bit = BITS - 1; bit >= 0; bit--) {
      final var originX = x + 10 + (BITS - 1 - bit) * 12;
      gfx.setColor(word.get(bit).getColor());
      gfx.fillOval(originX, y, 8, 8);
      gfx.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(gfx, word.get(bit).toDisplayString(), originX + 4, y + 4);
    }
  }

  private static TtlRegisterData getStateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WIDTH, 2);
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);
    final var clock1 =
        data.updateClock(state.getPortValue(PORT_INDEX_1CP), COUNTER_1, StdAttr.TRIG_FALLING);
    final var clock2 =
        data.updateClock(state.getPortValue(PORT_INDEX_2CP), COUNTER_2, StdAttr.TRIG_FALLING);
    advance(data, COUNTER_1, state.getPortValue(PORT_INDEX_1MR) == Value.TRUE, clock1);
    advance(data, COUNTER_2, state.getPortValue(PORT_INDEX_2MR) == Value.TRUE, clock2);
    for (var i = 0; i < OUTPUT_PORT_INDEXES.length; i++) {
      final var bit = data.getValue(OUTPUT_COUNTERS[i]).get(OUTPUT_BITS[i]);
      state.setPort(OUTPUT_PORT_INDEXES[i], bit, DELAY);
    }
  }

  private static void advance(TtlRegisterData data, int counter, boolean reset, boolean clocked) {
    if (reset) {
      data.setValue(counter, Value.createKnown(WIDTH, 0));
    } else if (clocked) {
      data.setValue(counter, nextCount(data.getValue(counter)));
    }
  }

  /**
   * Advances one binary count. An error anywhere makes the next word entirely error. A word that is
   * only unknown stays unknown. A known word increments modulo 16.
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
    return new int[] {PORT_INDEX_1CP, PORT_INDEX_2CP};
  }
}
