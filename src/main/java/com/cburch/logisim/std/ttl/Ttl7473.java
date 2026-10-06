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
 * TTL 7473: dual negative-edge J-K flip-flop with clear.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC73.pdf">Nexperia 74HC73</a>
 * data sheet. Each half samples J and K on the high-to-low clock transition. An active-low reset
 * forces Q low and its complement high, overriding the clock and the data inputs. Releasing reset
 * does not capture J and K. VCC is pin 4 and GND is pin 11. The legacy bipolar 7473 is
 * master-slave and can react while its clock is high; this model does not.
 */
public class Ttl7473 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7473";

  public static final int CP1 = 0;
  public static final int R1 = 1;
  public static final int K1 = 2;
  public static final int CP2 = 3;
  public static final int R2 = 4;
  public static final int J2 = 5;
  public static final int NQ2 = 6;
  public static final int Q2 = 7;
  public static final int K2 = 8;
  public static final int Q1 = 9;
  public static final int NQ1 = 10;
  public static final int J1 = 11;

  private static final byte[] OUTPUT_PORTS = {8, 9, 12, 13};
  private static final String[] PORT_NAMES = {
    "1CP (Clock 1, falling edge)",
    "1R (Reset 1, active low)",
    "1K",
    "2CP (Clock 2, falling edge)",
    "2R (Reset 2, active low)",
    "2J",
    "2nQ",
    "2Q",
    "2K",
    "1Q",
    "1nQ",
    "1J"
  };
  private static final BitWidth WIDTH = BitWidth.create(2);
  private static final int DELAY = 8;

  /** Creates the 14-pin dual J-K flip-flop. VCC is pin 4 and GND is pin 11. */
  public Ttl7473() {
    super(
        _ID,
        (byte) 14,
        OUTPUT_PORTS,
        null,
        PORT_NAMES,
        (byte) 4,
        (byte) 11,
        new Ttl7473HdlGenerator());
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
          "1CP", "1R", "1K", null, "2CP", "2R", "2J",
          "2nQ", "2Q", "2K", null, "1Q", "1nQ", "1J"
        });
    drawState(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private void drawState(Graphics2D gfx, int x, int y, int height, TtlRegisterData state) {
    if (state == null) return;
    drawBit(gfx, x + 48, y + height / 2, state.getValue().get(0));
    drawBit(gfx, x + 84, y + height / 2, state.getValue().get(1));
    gfx.setColor(Color.BLACK);
  }

  private static void drawBit(Graphics2D gfx, int x, int y, Value bit) {
    gfx.setColor(bit.getColor());
    gfx.fillOval(x, y - 4, 8, 8);
    gfx.setColor(Color.WHITE);
    GraphicsUtil.drawCenteredText(gfx, bit.toDisplayString(), x + 4, y);
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
    final var triggered1 = data.updateClock(state.getPortValue(CP1), 0, StdAttr.TRIG_FALLING);
    final var triggered2 = data.updateClock(state.getPortValue(CP2), 1, StdAttr.TRIG_FALLING);
    final var current = data.getValue();
    final var q1 = nextState(current.get(0), state.getPortValue(R1), state.getPortValue(J1),
        state.getPortValue(K1), triggered1);
    final var q2 = nextState(current.get(1), state.getPortValue(R2), state.getPortValue(J2),
        state.getPortValue(K2), triggered2);
    data.setValue(Value.create(new Value[] {q1, q2}));

    state.setPort(Q1, q1, DELAY);
    state.setPort(NQ1, q1.not(), DELAY);
    state.setPort(Q2, q2, DELAY);
    state.setPort(NQ2, q2.not(), DELAY);
  }

  /**
   * Active-low reset wins over the clock. A defined reset that is high applies J and K only on a
   * falling edge. Any other reset level stores unknown.
   */
  private static Value nextState(Value q, Value reset, Value j, Value k, boolean triggered) {
    if (reset == Value.FALSE) return Value.FALSE;
    if (reset != Value.TRUE) return Value.UNKNOWN;
    if (!triggered) return q;
    return nextJk(q, j, k);
  }

  /**
   * Fully defined J and K follow the data-sheet rows, including a load that replaces an unknown
   * Q. A partial J or K keeps the next state only when every resolved row agrees.
   */
  private static Value nextJk(Value q, Value j, Value k) {
    if (j == Value.TRUE && k == Value.FALSE) return Value.TRUE;
    if (j == Value.FALSE && k == Value.TRUE) return Value.FALSE;
    if (j == Value.FALSE && k == Value.FALSE) return q;
    if (j == Value.TRUE && k == Value.TRUE) return q.not();

    Value agreed = null;
    for (var jTry : new Value[] {Value.FALSE, Value.TRUE}) {
      if (j == Value.FALSE && jTry == Value.TRUE) continue;
      if (j == Value.TRUE && jTry == Value.FALSE) continue;
      for (var kTry : new Value[] {Value.FALSE, Value.TRUE}) {
        if (k == Value.FALSE && kTry == Value.TRUE) continue;
        if (k == Value.TRUE && kTry == Value.FALSE) continue;
        final var candidate = nextJk(q, jTry, kTry);
        if (agreed == null) {
          agreed = candidate;
        } else if (!agreed.equals(candidate)) {
          return Value.UNKNOWN;
        }
      }
    }
    return agreed == null ? Value.UNKNOWN : agreed;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {CP1, CP2};
  }
}
