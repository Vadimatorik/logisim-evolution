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
    final var gfx = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, false, false);
    Drawgates.paintPortNamesByPin(
        painter,
        x,
        y,
        height,
        new String[] {
          "Q12", "Q6", "Q5", "Q7", "Q4", "Q3", "Q2", null,
          "Q1", "CP", "MR", "Q9", "Q8", "Q10", "Q11", null
        });
    drawState(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private void drawState(Graphics2D gfx, int x, int y, int height, TtlRegisterData state) {
    if (state == null) return;
    for (var stage = STAGES - 1; stage >= 0; stage--) {
      final var bit = state.getValue().get(stage);
      final var originX = x + 20 + (STAGES - 1 - stage) * 10;
      gfx.setColor(bit.getColor());
      gfx.fillOval(originX, y + height / 2 - 4, 8, 8);
      gfx.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(gfx, bit.toDisplayString(), originX + 4, y + height / 2);
    }
    gfx.setColor(Color.BLACK);
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
}
