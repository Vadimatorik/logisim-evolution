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
 * TTL 74x112: dual negative-edge-triggered J-K flip-flop with set and reset.
 *
 * <p>Pin numbers follow the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT112.pdf">Nexperia 74HC112</a>
 * DIP-16 package: complementary outputs are pins 6 and 7, and ground is pin 8. The function table
 * follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74hc112.pdf">TI SNx4HC112</a> data sheet. While both
 * preset and clear are low, both outputs of that flip-flop are high. That state does not persist:
 * the remaining active input wins when exactly one is released, and both outputs become unknown
 * when both are released together. Nexperia's printed row for the same input combination is marked
 * undetermined. HC and HCT input thresholds and timing limits are not modeled.
 *
 * <p>The HDL model keeps complementary outputs and gives clear priority when preset and clear are
 * both low. That export cannot reproduce the both-high simulator state.
 */
public class Ttl74112 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74112";

  public static final int PORT_INDEX_1CP = 0;
  public static final int PORT_INDEX_1K = 1;
  public static final int PORT_INDEX_1J = 2;
  public static final int PORT_INDEX_1SD = 3;
  public static final int PORT_INDEX_1Q = 4;
  public static final int PORT_INDEX_1NQ = 5;
  public static final int PORT_INDEX_2NQ = 6;
  public static final int PORT_INDEX_2Q = 7;
  public static final int PORT_INDEX_2SD = 8;
  public static final int PORT_INDEX_2J = 9;
  public static final int PORT_INDEX_2K = 10;
  public static final int PORT_INDEX_2CP = 11;
  public static final int PORT_INDEX_2RD = 12;
  public static final int PORT_INDEX_1RD = 13;

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
  private static final byte[] OUTPUT_PINS = {5, 6, 7, 9};
  private static final String[] PORT_NAMES = {
    "1CP (Clock, falling edge)",
    "1K",
    "1J",
    "1SD (Set, active low)",
    "1Q",
    "1nQ",
    "2nQ",
    "2Q",
    "2SD (Set, active low)",
    "2J",
    "2K",
    "2CP (Clock, falling edge)",
    "2RD (Reset, active low)",
    "1RD (Reset, active low)"
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

  /** Creates a 74112 dual negative-edge J-K flip-flop. */
  public Ttl74112() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl74112HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var rawState = painter.getData();
    final TtlRegisterData stored = rawState instanceof TtlRegisterData ? (TtlRegisterData) rawState : null;
    g.drawRect((x + 14), (y + 18), 58, 24);
    GraphicsUtil.drawCenteredText(g, "J", (x + 22), (y + 26));
    GraphicsUtil.drawCenteredText(g, "K", (x + 22), (y + 36));
    GraphicsUtil.drawCenteredText(g, "Q", (x + 64), (y + 26));
    GraphicsUtil.drawCenteredText(g, "Q", (x + 64), (y + 36));
    structureBubble(g, (x + 64), (y + 32));
    structureDot(g, (x + 43), (y + 30), stored, 0, 0);
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + height - 13));
    g.drawLine((x + 10), (y + height - 13), (x + 26), (y + height - 13));
    g.drawLine((x + 26), (y + height - 13), (x + 26), (y + 42));
    g.drawLine((x + 26) - 3, (y + 42), (x + 26), ((y + 42)) - 4);
    g.drawLine((x + 26) + 3, (y + 42), (x + 26), ((y + 42)) - 4);
    structureBubble(g, (x + 26), ((y + 42)) + 6);
    g.drawLine((x + 50), (y + height - 7), (x + 50), (y + 42));
    g.drawLine((x + 30), (y + height - 7), (x + 30), (y + 42));
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 18));
    structureBubble(g, (x + 30), ((y + 18)) - 5);
    g.drawLine((x + 70), (y + height - 7), (x + 70), (y + height - 13));
    g.drawLine((x + 70), (y + height - 13), (x + 51), (y + height - 13));
    g.drawLine((x + 51), (y + height - 13), (x + 51), (y + 42));
    structureBubble(g, (x + 51), ((y + 42)) + 5);
    g.drawLine((x + 90), (y + height - 7), (x + 90), (y + height - 13));
    g.drawLine((x + 90), (y + height - 13), (x + 59), (y + height - 13));
    g.drawLine((x + 59), (y + height - 13), (x + 59), (y + 42));
    g.drawLine((x + 110), (y + height - 7), (x + 110), (y + height - 13));
    g.drawLine((x + 110), (y + height - 13), (x + 68), (y + height - 13));
    g.drawLine((x + 68), (y + height - 13), (x + 68), (y + 42));
    structureBubble(g, (x + 68), ((y + 42)) + 5);
    g.drawRect((x + 80), (y + 18), 58, 24);
    GraphicsUtil.drawCenteredText(g, "J", (x + 88), (y + 26));
    GraphicsUtil.drawCenteredText(g, "K", (x + 88), (y + 36));
    GraphicsUtil.drawCenteredText(g, "Q", (x + 130), (y + 26));
    GraphicsUtil.drawCenteredText(g, "Q", (x + 130), (y + 36));
    structureBubble(g, (x + 130), (y + 32));
    structureDot(g, (x + 109), (y + 30), stored, 0, 1);
    g.drawLine((x + 70), (y + 7), (x + 70), (y + 13));
    g.drawLine((x + 70), (y + 13), (x + 92), (y + 13));
    g.drawLine((x + 92), (y + 13), (x + 92), (y + 18));
    g.drawLine((x + 92) - 3, (y + 18), (x + 92), ((y + 18)) + 4);
    g.drawLine((x + 92) + 3, (y + 18), (x + 92), ((y + 18)) + 4);
    structureBubble(g, (x + 92), ((y + 18)) - 6);
    g.drawLine((x + 110), (y + 7), (x + 110), (y + 18));
    g.drawLine((x + 90), (y + 7), (x + 90), (y + 18));
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 13));
    g.drawLine((x + 50), (y + 13), (x + 117), (y + 13));
    g.drawLine((x + 117), (y + 13), (x + 117), (y + 18));
    structureBubble(g, (x + 117), ((y + 18)) - 5);
    g.drawLine((x + 130), (y + 7), (x + 130), (y + 18));
    structureBubble(g, (x + 130), ((y + 18)) - 5);
    g.drawLine((x + 150), (y + 7), (x + 150), (y + 13));
    g.drawLine((x + 150), (y + 13), (x + 134), (y + 13));
    g.drawLine((x + 134), (y + 13), (x + 134), (y + 18));
    g.drawLine((x + 130), (y + height - 7), (x + 130), (y + 42));
    structureBubble(g, (x + 130), ((y + 42)) + 5);
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
            state.getPortValue(half[CLOCK_PORT]), half[CLOCK_INDEX], StdAttr.TRIG_FALLING);
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
   * Next Q after a falling clock. A fully specified J=1, K=0 or J=0, K=1 forces the result. Any
   * other unknown or error input makes the result unknown or error when the alternatives disagree.
   */
  private static Value nextJk(Value q, Value j, Value k) {
    if (j == Value.ERROR || k == Value.ERROR || q == Value.ERROR) {
      if (j == Value.TRUE && k == Value.FALSE) return Value.TRUE;
      if (j == Value.FALSE && k == Value.TRUE) return Value.FALSE;
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
    if (j == Value.FALSE && k == Value.FALSE) return q;
    if (j == Value.TRUE && k == Value.FALSE) return Value.TRUE;
    if (j == Value.FALSE && k == Value.TRUE) return Value.FALSE;
    return q.not();
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
