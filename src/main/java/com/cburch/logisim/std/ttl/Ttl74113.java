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
 * TTL 74x113: dual negative-edge-triggered J-K flip-flop with preset.
 *
 * <p>Pin numbers and the function table follow the ST M74HC113 DIP-14 data sheet. Each flip-flop
 * has its own J, K, clock, and active-low preset. There is no clear input. J and K are transferred
 * on the high-to-low clock edge; setup time is not modeled. A low preset forces Q high and the
 * complementary output low, and it overrides the clock. The model starts with Q low and the
 * complementary output high. A physical 74HC113 powers up undefined. HC and HCT thresholds and
 * timing limits are not modeled. The Motorola SN74LS113A mode table describes the same digital
 * function.
 */
public class Ttl74113 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74113";

  public static final int PORT_INDEX_1CK = 0;
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
  public static final int PORT_INDEX_2CK = 11;

  private static final int DELAY = 8;
  private static final int Q1_BIT = 0;
  private static final int NQ1_BIT = 1;
  private static final int Q2_BIT = 2;
  private static final int NQ2_BIT = 3;
  private static final int CLOCK_PORT = 0;
  private static final int J_PORT = 1;
  private static final int K_PORT = 2;
  private static final int PRESET_PORT = 3;
  private static final int Q_BIT = 4;
  private static final int NQ_BIT = 5;
  private static final int Q_PORT = 6;
  private static final int NQ_PORT = 7;
  private static final int CLOCK_INDEX = 8;
  private static final BitWidth WIDTH = BitWidth.create(4);
  /** Known startup: Q low and complementary output high for both flip-flops. */
  private static final int COMPLEMENTARY_RESET = (1 << NQ1_BIT) | (1 << NQ2_BIT);
  private static final byte[] OUTPUT_PINS = {5, 6, 8, 9};
  private static final String[] PORT_NAMES = {
    "1CK (Clock, falling edge)",
    "1K",
    "1J",
    "1PR (Preset, active LOW)",
    "1Q",
    "1nQ",
    "2nQ",
    "2Q",
    "2PR (Preset, active LOW)",
    "2J",
    "2K",
    "2CK (Clock, falling edge)"
  };
  private static final int[][] HALVES = {
    {
      PORT_INDEX_1CK,
      PORT_INDEX_1J,
      PORT_INDEX_1K,
      PORT_INDEX_1PR,
      Q1_BIT,
      NQ1_BIT,
      PORT_INDEX_1Q,
      PORT_INDEX_1NQ,
      0
    },
    {
      PORT_INDEX_2CK,
      PORT_INDEX_2J,
      PORT_INDEX_2K,
      PORT_INDEX_2PR,
      Q2_BIT,
      NQ2_BIT,
      PORT_INDEX_2Q,
      PORT_INDEX_2NQ,
      1
    }
  };

  /** Creates a 74113 dual negative-edge J-K flip-flop with preset. */
  public Ttl74113() {
    super(_ID, (byte) 14, OUTPUT_PINS, PORT_NAMES, new Ttl74113HdlGenerator());
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
          "1CK", "1K", "1J", "1PR", "1Q", "1nQ", null, "2nQ", "2Q", "2PR", "2J", "2K", "2CK", null
        });
    drawState(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private void drawState(Graphics2D gfx, int x, int y, int height, TtlRegisterData state) {
    if (state == null) return;
    final var value = state.getValue();
    final var center = y + height / 2 - 4;
    drawBit(gfx, x + 36, center, value.get(Q1_BIT));
    drawBit(gfx, x + 52, center, value.get(NQ1_BIT));
    drawBit(gfx, x + 88, center, value.get(Q2_BIT));
    drawBit(gfx, x + 104, center, value.get(NQ2_BIT));
    gfx.setColor(Color.BLACK);
  }

  private static void drawBit(Graphics2D gfx, int x, int y, Value bit) {
    gfx.setColor(bit.getColor());
    gfx.fillOval(x, y, 8, 8);
    gfx.setColor(Color.WHITE);
    GraphicsUtil.drawCenteredText(gfx, bit.toDisplayString(), x + 4, y + 4);
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
    final var preset = state.getPortValue(half[PRESET_PORT]) == Value.FALSE;
    var q = values[half[Q_BIT]];
    var nq = values[half[NQ_BIT]];
    if (preset) {
      q = Value.TRUE;
      nq = Value.FALSE;
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
    return new int[] {PORT_INDEX_1CK, PORT_INDEX_2CK};
  }
}
