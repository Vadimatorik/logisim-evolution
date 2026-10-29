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
    "1RD (Reset, active low)",
    "1J",
    "1K",
    "1CP (Clock, rising edge)",
    "1SD (Set, active low)",
    "1Q",
    "1nQ",
    "2nQ",
    "2Q",
    "2SD (Set, active low)",
    "2CP (Clock, rising edge)",
    "2K",
    "2J",
    "2RD (Reset, active low)"
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
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var rawState = painter.getData();
    final TtlRegisterData stored =
        rawState instanceof TtlRegisterData ? (TtlRegisterData) rawState : null;
    // Lower half uses pins 1..7. Upper half uses pins 15..9. Each lead stays on its own pin.
    drawHalf(g, x, y, height, false, new int[] {1, 2, 3, 4, 5, 6, 7}, stored, 0);
    drawHalf(g, x, y, height, true, new int[] {15, 14, 13, 12, 11, 10, 9}, stored, 1);
  }

  /**
   * One J-K symbol. {@code pins} is reset, J, K, clock, set, Q, nQ. Active-low pins get a bubble
   * on the symbol edge, and the clock wedge stops on that edge.
   */
  private static void drawHalf(
      Graphics g,
      int x,
      int y,
      int height,
      boolean alongTop,
      int[] pins,
      TtlRegisterData stored,
      int word) {
    final var resetX = structureX(x, 16, pins[0]);
    final var jX = structureX(x, 16, pins[1]);
    final var kX = structureX(x, 16, pins[2]);
    final var clockX = structureX(x, 16, pins[3]);
    final var setX = structureX(x, 16, pins[4]);
    final var qX = structureX(x, 16, pins[5]);
    final var nqX = structureX(x, 16, pins[6]);
    final var left = Math.min(resetX, nqX) - 6;
    final var right = Math.max(resetX, nqX) + 6;
    final var top = alongTop ? y + 12 : y + 32;
    final var boxHeight = 16;
    g.drawRect(left, top, right - left, boxHeight);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 9));
    final var labelY = top + boxHeight / 2;
    GraphicsUtil.drawCenteredText(g, "J", jX, labelY);
    GraphicsUtil.drawCenteredText(g, "K", kX, labelY);
    GraphicsUtil.drawCenteredText(g, "Q", qX, labelY);
    GraphicsUtil.drawCenteredText(g, "Q", nqX, labelY);
    g.setFont(previousFont);
    structureDot(g, (jX + qX) / 2, labelY, stored, word, 0);

    final var edge = alongTop ? top : top + boxHeight;
    final var pinY = structureY(y, height, 16, pins[0]);
    drawLead(g, resetX, pinY, edge, true);
    drawLead(g, jX, pinY, edge, false);
    drawLead(g, kX, pinY, edge, false);
    drawClock(g, clockX, pinY, edge, alongTop);
    drawLead(g, setX, pinY, edge, true);
    drawLead(g, qX, pinY, edge, false);
    drawLead(g, nqX, pinY, edge, true);
  }

  /** Straight lead from the package pin to the symbol. A bubble sits on the symbol edge. */
  private static void drawLead(Graphics g, int pinX, int pinY, int edgeY, boolean bubble) {
    if (!bubble) {
      g.drawLine(pinX, pinY, pinX, edgeY);
      return;
    }
    final var stop = edgeY < pinY ? edgeY + 2 : edgeY - 2;
    g.drawLine(pinX, pinY, pinX, stop);
    structureBubble(g, pinX, edgeY);
  }

  /** Three segments meeting at the symbol edge, outside the rectangle. */
  private static void drawClock(Graphics g, int pinX, int pinY, int edgeY, boolean alongTop) {
    final var base = alongTop ? edgeY - 3 : edgeY + 3;
    g.drawLine(pinX, pinY, pinX, base);
    g.drawLine(pinX - 3, base, pinX, edgeY);
    g.drawLine(pinX + 3, base, pinX, edgeY);
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
