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
 * TTL 74x114: dual negative-edge-triggered J-K flip-flop with preset, common clear and common
 * clock.
 *
 * <p>Pin numbers follow the Hitachi HD74HC114 DIP-14 package, which matches the SN74LS114 pinout.
 * Both flip-flops share the active-low clear and the clock. Each flip-flop has its own J, K, and
 * active-low preset. J and K are transferred on the high-to-low clock edge; setup time is not
 * modeled. While preset and clear are both low, both outputs of that flip-flop are high. That
 * state does not persist: the remaining active input wins when exactly one is released, and both
 * outputs become unknown when both are released together. HC and HCT input thresholds and timing
 * limits are not modeled. The model starts with Q low and the complementary output high. A
 * physical 74HC114 powers up undefined.
 *
 * <p>The HDL model keeps complementary outputs and gives clear priority when preset and clear are
 * both low. That export cannot reproduce the both-high simulator state.
 */
public class Ttl74114 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74114";

  public static final int PORT_INDEX_CLR = 0;
  public static final int PORT_INDEX_1K = 1;
  public static final int PORT_INDEX_1J = 2;
  public static final int PORT_INDEX_1PR = 3;
  public static final int PORT_INDEX_1Q = 4;
  public static final int PORT_INDEX_1NQ = 5;
  public static final int PORT_INDEX_2NQ = 6;
  public static final int PORT_INDEX_2Q = 7;
  public static final int PORT_INDEX_2PR = 8;
  public static final int PORT_INDEX_2J = 9;
  public static final int PORT_INDEX_2K = 10;
  public static final int PORT_INDEX_CLK = 11;

  private static final int DELAY = 8;
  private static final int Q1_BIT = 0;
  private static final int NQ1_BIT = 1;
  private static final int Q2_BIT = 2;
  private static final int NQ2_BIT = 3;
  private static final int J_PORT = 0;
  private static final int K_PORT = 1;
  private static final int PRESET_PORT = 2;
  private static final int Q_BIT = 3;
  private static final int NQ_BIT = 4;
  private static final int Q_PORT = 5;
  private static final int NQ_PORT = 6;
  private static final BitWidth WIDTH = BitWidth.create(4);
  /** Known startup: Q low and complementary output high for both flip-flops. */
  private static final int COMPLEMENTARY_RESET = (1 << NQ1_BIT) | (1 << NQ2_BIT);
  private static final byte[] OUTPUT_PINS = {5, 6, 8, 9};
  private static final String[] PORT_NAMES = {
    "nCLR (Clear, active low)",
    "1K",
    "1J",
    "1PR (Preset, active low)",
    "1Q",
    "1nQ",
    "2nQ",
    "2Q",
    "2PR (Preset, active low)",
    "2J",
    "2K",
    "CLK (Clock, falling edge)"
  };
  private static final int[][] HALVES = {
    {
      PORT_INDEX_1J,
      PORT_INDEX_1K,
      PORT_INDEX_1PR,
      Q1_BIT,
      NQ1_BIT,
      PORT_INDEX_1Q,
      PORT_INDEX_1NQ
    },
    {
      PORT_INDEX_2J,
      PORT_INDEX_2K,
      PORT_INDEX_2PR,
      Q2_BIT,
      NQ2_BIT,
      PORT_INDEX_2Q,
      PORT_INDEX_2NQ
    }
  };

  /** Creates a 74114 dual negative-edge J-K flip-flop with common clear and clock. */
  public Ttl74114() {
    super(_ID, (byte) 14, OUTPUT_PINS, PORT_NAMES, new Ttl74114HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 8));
    g.drawRect(x + 85, y + 28, 10, 10);
    g.drawRect(x + 105, y + 28, 10, 10);
    g.drawRect(x + 125, y + 12, 10, 10);
    g.drawRect(x + 105, y + 12, 10, 10);
    g.drawLine(x + 90, y + height - 22, x + 90, y + height - 7);
    g.drawLine(x + 110, y + height - 22, x + 110, y + height - 7);
    g.drawLine(x + 130, y + 12, x + 130, y + 7);
    g.drawLine(x + 110, y + 12, x + 110, y + 7);
    g.drawLine(x + 85, y + 24, x + 135, y + 24);
    g.drawLine(x + 10, y + height - 7, x + 10, y + height - 9);
    g.drawLine(x + 10, y + height - 9, x + 81, y + height - 9);
    g.drawLine(x + 81, y + height - 9, x + 81, y + 24);
    g.drawLine(x + 81, y + 24, x + 85, y + 24);
    g.drawLine(x + 87, y + 24, x + 87, y + 28);
    g.drawLine(x + 107, y + 24, x + 107, y + 28);
    g.drawLine(x + 127, y + 24, x + 127, y + 22);
    g.drawLine(x + 107, y + 24, x + 107, y + 22);
    g.drawLine(x + 85, y + 24, x + 135, y + 24);
    g.drawLine(x + 70, y + height - 7, x + 70, y + height - 9);
    g.drawLine(x + 70, y + height - 9, x + 81, y + height - 9);
    g.drawLine(x + 81, y + height - 9, x + 81, y + 24);
    g.drawLine(x + 81, y + 24, x + 85, y + 24);
    g.drawLine(x + 90, y + 24, x + 90, y + 28);
    g.drawLine(x + 110, y + 24, x + 110, y + 28);
    g.drawLine(x + 130, y + 24, x + 130, y + 22);
    g.drawLine(x + 110, y + 24, x + 110, y + 22);
    g.drawLine(x + 85, y + 25, x + 135, y + 25);
    g.drawLine(x + 90, y + 7, x + 90, y + 9);
    g.drawLine(x + 90, y + 9, x + 81, y + 9);
    g.drawLine(x + 81, y + 9, x + 81, y + 25);
    g.drawLine(x + 81, y + 25, x + 85, y + 25);
    g.drawLine(x + 93, y + 25, x + 93, y + 28);
    g.drawLine(x + 113, y + 25, x + 113, y + 28);
    g.drawLine(x + 133, y + 25, x + 133, y + 22);
    g.drawLine(x + 113, y + 25, x + 113, y + 22);
    g.drawLine(x + 85, y + 25, x + 135, y + 25);
    g.drawLine(x + 30, y + 7, x + 30, y + 9);
    g.drawLine(x + 30, y + 9, x + 81, y + 9);
    g.drawLine(x + 81, y + 9, x + 81, y + 25);
    g.drawLine(x + 81, y + 25, x + 85, y + 25);
    g.drawLine(x + 87, y + 25, x + 87, y + 28);
    g.drawLine(x + 107, y + 25, x + 107, y + 28);
    g.drawLine(x + 127, y + 25, x + 127, y + 22);
    g.drawLine(x + 107, y + 25, x + 107, y + 22);
    g.drawLine(x + 30, y + height - 7, x + 30, y + height - 19);
    g.drawLine(x + 30, y + height - 19, x + 87, y + height - 19);
    g.drawLine(x + 87, y + height - 19, x + 87, y + height - 22);
    g.drawLine(x + 50, y + height - 7, x + 50, y + height - 16);
    g.drawLine(x + 50, y + height - 16, x + 89, y + height - 16);
    g.drawLine(x + 89, y + height - 16, x + 89, y + height - 22);
    g.drawLine(x + 70, y + 7, x + 70, y + 9);
    g.drawLine(x + 70, y + 9, x + 127, y + 9);
    g.drawLine(x + 127, y + 9, x + 127, y + 12);
    g.drawLine(x + 50, y + 7, x + 50, y + 6);
    g.drawLine(x + 50, y + 6, x + 129, y + 6);
    g.drawLine(x + 129, y + 6, x + 129, y + 12);
    g.drawOval(x + 79, y + 22, 4, 4);
    g.drawOval(x + 79, y + 22, 4, 4);
    g.drawOval(x + 79, y + 23, 4, 4);
    GraphicsUtil.drawCenteredText(g, "Q", x + 90, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "Q", x + 110, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "Q", x + 130, y + 17);
    GraphicsUtil.drawCenteredText(g, "Q", x + 110, y + 17);
    g.setFont(previousFont);
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
    final var triggered =
        data.updateClock(state.getPortValue(PORT_INDEX_CLK), StdAttr.TRIG_FALLING);
    final var clear = state.getPortValue(PORT_INDEX_CLR) == Value.FALSE;
    for (final var half : HALVES) {
      updateHalf(state, values, half, triggered, clear);
    }
    data.setValue(Value.create(values));
    for (final var half : HALVES) {
      state.setPort(half[Q_PORT], values[half[Q_BIT]], DELAY);
      state.setPort(half[NQ_PORT], values[half[NQ_BIT]], DELAY);
    }
  }

  private static void updateHalf(
      InstanceState state, Value[] values, int[] half, boolean triggered, boolean clear) {
    final var preset = state.getPortValue(half[PRESET_PORT]) == Value.FALSE;
    var q = values[half[Q_BIT]];
    var nq = values[half[NQ_BIT]];
    if (preset && clear) {
      q = Value.TRUE;
      nq = Value.TRUE;
    } else if (preset) {
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
    return new int[] {PORT_INDEX_CLK};
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
