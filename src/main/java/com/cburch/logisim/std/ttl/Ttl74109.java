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
 * TTL 74x109: dual positive-edge-triggered J-K flip-flop with set and reset.
 *
 * <p>Pin numbers follow the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT109.pdf">Nexperia 74HC109</a>
 * DIP-16 package. The function table follows that sheet and the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74hc109.pdf">TI SNx4HC109</a> data sheet. The clock
 * captures on the low-to-high transition. The K pin is the complement of a 7476 K input: J and K
 * tied together make a D flip-flop, J=0 K=0 loads 0, J=1 K=1 loads 1, J=0 K=1 holds, and J=1 K=0
 * toggles. While both preset and clear are low, both outputs of that flip-flop are high. That
 * state does not persist: the remaining active input wins when exactly one is released, and both
 * outputs become unknown when both are released together. HC and HCT input thresholds and timing
 * limits are not modeled.
 *
 * <p>The HDL model keeps complementary outputs and gives clear priority when preset and clear are
 * both low. That export cannot reproduce the both-high simulator state.
 */
public class Ttl74109 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74109";

  public static final int PORT_INDEX_1RD = 0;
  public static final int PORT_INDEX_1J = 1;
  public static final int PORT_INDEX_1K = 2;
  public static final int PORT_INDEX_1CP = 3;
  public static final int PORT_INDEX_1SD = 4;
  public static final int PORT_INDEX_1Q = 5;
  public static final int PORT_INDEX_1NQ = 6;
  public static final int PORT_INDEX_2NQ = 7;
  public static final int PORT_INDEX_2Q = 8;
  public static final int PORT_INDEX_2SD = 9;
  public static final int PORT_INDEX_2CP = 10;
  public static final int PORT_INDEX_2K = 11;
  public static final int PORT_INDEX_2J = 12;
  public static final int PORT_INDEX_2RD = 13;

  private static final int DELAY = 8;
  private static final int Q1_BIT = 0;
  private static final int NQ1_BIT = 1;
  private static final int Q2_BIT = 2;
  private static final int NQ2_BIT = 3;
  private static final int CLOCK_PORT = 0;
  private static final int J_PORT = 1;
  private static final int K_PORT = 2;
  private static final int SET_PORT = 3;
  private static final int CLEAR_PORT = 4;
  private static final int Q_BIT = 5;
  private static final int NQ_BIT = 6;
  private static final int Q_PORT = 7;
  private static final int NQ_PORT = 8;
  private static final int CLOCK_INDEX = 9;
  private static final BitWidth WIDTH = BitWidth.create(4);
  /** Known startup: Q low and complementary output high for both flip-flops. */
  private static final int COMPLEMENTARY_RESET = (1 << NQ1_BIT) | (1 << NQ2_BIT);
  private static final byte[] OUTPUT_PINS = {6, 7, 9, 10};
  private static final String[] PORT_NAMES = {
    "1RD (Reset, active LOW)",
    "1J",
    "1K",
    "1CP (Clock, rising edge)",
    "1SD (Set, active LOW)",
    "1Q",
    "1nQ",
    "2nQ",
    "2Q",
    "2SD (Set, active LOW)",
    "2CP (Clock, rising edge)",
    "2K",
    "2J",
    "2RD (Reset, active LOW)"
  };
  private static final String[] PIN_NAMES = {
    "1RD", "1J", "1K", "1CP", "1SD", "1Q", "1nQ", null,
    "2nQ", "2Q", "2SD", "2CP", "2K", "2J", "2RD", null
  };
  private static final int[][] HALVES = {
    {
      PORT_INDEX_1CP,
      PORT_INDEX_1J,
      PORT_INDEX_1K,
      PORT_INDEX_1SD,
      PORT_INDEX_1RD,
      Q1_BIT,
      NQ1_BIT,
      PORT_INDEX_1Q,
      PORT_INDEX_1NQ,
      0
    },
    {
      PORT_INDEX_2CP,
      PORT_INDEX_2J,
      PORT_INDEX_2K,
      PORT_INDEX_2SD,
      PORT_INDEX_2RD,
      Q2_BIT,
      NQ2_BIT,
      PORT_INDEX_2Q,
      PORT_INDEX_2NQ,
      1
    }
  };

  /** Creates a 74109 dual positive-edge J-K flip-flop. */
  public Ttl74109() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl74109HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var gfx = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, false, false);
    Drawgates.paintPortNamesByPin(painter, x, y, height, PIN_NAMES);
    drawState(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private void drawState(Graphics2D gfx, int x, int y, int height, TtlRegisterData state) {
    if (state == null) return;
    final var value = state.getValue();
    final var center = y + height / 2 - 4;
    drawBit(gfx, x + 48, center, value.get(Q1_BIT));
    drawBit(gfx, x + 64, center, value.get(NQ1_BIT));
    drawBit(gfx, x + 96, center, value.get(Q2_BIT));
    drawBit(gfx, x + 112, center, value.get(NQ2_BIT));
    gfx.setColor(Color.BLACK);
  }

  private static void drawBit(Graphics2D gfx, int x, int y, Value bit) {
    gfx.setColor(bit.getColor());
    gfx.fillOval(x, y, 8, 8);
    gfx.setColor(Color.WHITE);
    GraphicsUtil.drawCenteredText(gfx, bit.toDisplayString(), x + 4, y + 4);
    gfx.setColor(Color.BLACK);
  }

  private static TtlRegisterData getStateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WIDTH);
      if (data.getValue().isFullyDefined()) {
        data.setValue(Value.createKnown(WIDTH, COMPLEMENTARY_RESET));
      }
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);
    final var values = data.getValue().getAll();
    for (final var half : HALVES) {
      updateHalf(state, data, values, half);
    }
    data.setValue(Value.create(values));
    for (final var half : HALVES) {
      state.setPort(half[Q_PORT], values[half[Q_BIT]], DELAY);
      state.setPort(half[NQ_PORT], values[half[NQ_BIT]], DELAY);
    }
  }

  private static void updateHalf(
      InstanceState state, TtlRegisterData data, Value[] values, int[] half) {
    final var triggered =
        data.updateClock(
            state.getPortValue(half[CLOCK_PORT]), half[CLOCK_INDEX], StdAttr.TRIG_RISING);
    final var set = state.getPortValue(half[SET_PORT]) == Value.FALSE;
    final var clear = state.getPortValue(half[CLEAR_PORT]) == Value.FALSE;
    var q = values[half[Q_BIT]];
    var nq = values[half[NQ_BIT]];
    if (set && clear) {
      q = Value.TRUE;
      nq = Value.TRUE;
    } else if (set) {
      q = Value.TRUE;
      nq = Value.FALSE;
    } else if (clear) {
      q = Value.FALSE;
      nq = Value.TRUE;
    } else if (q == Value.TRUE && nq == Value.TRUE) {
      q = Value.UNKNOWN;
      nq = Value.UNKNOWN;
    } else if (triggered) {
      q = nextJk(q, state.getPortValue(half[J_PORT]), state.getPortValue(half[K_PORT]));
      nq = complement(q);
    }
    values[half[Q_BIT]] = q;
    values[half[NQ_BIT]] = nq;
  }

  /**
   * Next Q after a rising clock. J=1 with K=1 loads 1, and J=0 with K=0 loads 0, even when Q is
   * not a normal level. Any other unknown or error input makes the result unknown or error when
   * the alternatives disagree.
   */
  private static Value nextJk(Value q, Value j, Value k) {
    if (j == Value.ERROR || k == Value.ERROR || q == Value.ERROR) {
      if (j == Value.TRUE && k == Value.TRUE) return Value.TRUE;
      if (j == Value.FALSE && k == Value.FALSE) return Value.FALSE;
      if (j == Value.TRUE && k == Value.FALSE) return complement(q);
      if (j == Value.FALSE && k == Value.TRUE) return q;
      return Value.ERROR;
    }
    Value result = null;
    for (final var jTry : alternatives(j)) {
      for (final var kTry : alternatives(k)) {
        for (final var qTry : alternatives(q)) {
          final var candidate = definedJk(qTry, jTry, kTry);
          if (result == null) {
            result = candidate;
          } else if (result != candidate) {
            return Value.UNKNOWN;
          }
        }
      }
    }
    return result == null ? Value.UNKNOWN : result;
  }

  private static Value definedJk(Value q, Value j, Value k) {
    if (j == Value.FALSE && k == Value.FALSE) return Value.FALSE;
    if (j == Value.TRUE && k == Value.TRUE) return Value.TRUE;
    if (j == Value.TRUE && k == Value.FALSE) return complement(q);
    return q;
  }

  private static Value[] alternatives(Value bit) {
    if (bit == Value.TRUE || bit == Value.FALSE) return new Value[] {bit};
    return new Value[] {Value.FALSE, Value.TRUE};
  }

  private static Value complement(Value bit) {
    if (bit == Value.TRUE) return Value.FALSE;
    if (bit == Value.FALSE) return Value.TRUE;
    if (bit == Value.UNKNOWN) return Value.UNKNOWN;
    return Value.ERROR;
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
