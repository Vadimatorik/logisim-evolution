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
 * TTL 74HC4520: dual synchronous 4-bit binary counter.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT4520.pdf">Nexperia 74HC4520</a>
 * and <a href="https://www.ti.com/lit/ds/symlink/cd74hc4520.pdf">TI CD74HC4520</a> data sheets. The
 * DIP-16 pin numbers match. Nexperia calls the clocks {@code nCP0} and {@code nCP1}; TI calls the
 * same pins {@code nCP} and {@code nE}. Each half advances on the rising edge of {@code nCP0}
 * while {@code nCP1} is high, and on the falling edge of {@code nCP1} while {@code nCP0} is low.
 * Those two rows are the rising edge of {@code nCP0 OR NOT nCP1}, so both inputs changing in one
 * propagation count once. A high {@code nMR} asynchronously clears that half and overrides the
 * clock. The count is binary and wraps from 15 to 0.
 */
public class Ttl744520 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744520";

  public static final int PORT_INDEX_1CP0 = 0;
  public static final int PORT_INDEX_1CP1 = 1;
  public static final int PORT_INDEX_1Q0 = 2;
  public static final int PORT_INDEX_1Q1 = 3;
  public static final int PORT_INDEX_1Q2 = 4;
  public static final int PORT_INDEX_1Q3 = 5;
  public static final int PORT_INDEX_1MR = 6;
  public static final int PORT_INDEX_2CP0 = 7;
  public static final int PORT_INDEX_2CP1 = 8;
  public static final int PORT_INDEX_2Q0 = 9;
  public static final int PORT_INDEX_2Q1 = 10;
  public static final int PORT_INDEX_2Q2 = 11;
  public static final int PORT_INDEX_2Q3 = 12;
  public static final int PORT_INDEX_2MR = 13;

  private static final int DELAY = 4;
  private static final int BITS = 4;
  private static final int COUNTERS = 2;
  private static final int COUNT_MASK = (1 << BITS) - 1;
  private static final BitWidth WIDTH = BitWidth.create(BITS);
  private static final byte[] OUTPUT_PORTS = {3, 4, 5, 6, 11, 12, 13, 14};
  private static final String[] PORT_NAMES = {
    "1CP0 (clock, rising edge; TI: 1CP)",
    "1CP1 (clock, falling edge; TI: 1E)",
    "1Q0",
    "1Q1",
    "1Q2",
    "1Q3",
    "1MR (master reset, active HIGH)",
    "2CP0 (clock, rising edge; TI: 2CP)",
    "2CP1 (clock, falling edge; TI: 2E)",
    "2Q0",
    "2Q1",
    "2Q2",
    "2Q3",
    "2MR (master reset, active HIGH)"
  };
  private static final int[] COUNTER1_OUTPUTS = {
    PORT_INDEX_1Q0, PORT_INDEX_1Q1, PORT_INDEX_1Q2, PORT_INDEX_1Q3
  };
  private static final int[] COUNTER2_OUTPUTS = {
    PORT_INDEX_2Q0, PORT_INDEX_2Q1, PORT_INDEX_2Q2, PORT_INDEX_2Q3
  };

  /** Creates a 744520 dual synchronous 4-bit binary counter. */
  public Ttl744520() {
    super(_ID, (byte) 16, OUTPUT_PORTS, PORT_NAMES, new Ttl744520HdlGenerator());
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
          "1CP0", "1CP1", "1Q0", "1Q1", "1Q2", "1Q3", "1MR", null,
          "2CP0", "2CP1", "2Q0", "2Q1", "2Q2", "2Q3", "2MR", null
        });
    drawState(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private void drawState(Graphics2D gfx, int x, int y, int height, TtlRegisterData state) {
    if (state == null) return;
    drawNibble(gfx, x + 28, y, height, state.getValue(0));
    drawNibble(gfx, x + 96, y, height, state.getValue(1));
    gfx.setColor(Color.BLACK);
  }

  private static void drawNibble(Graphics2D gfx, int originX, int y, int height, Value word) {
    for (var bit = 0; bit < BITS; bit++) {
      final var value = word.get(BITS - 1 - bit);
      final var dotX = originX + bit * 10;
      gfx.setColor(value.getColor());
      gfx.fillOval(dotX, y + height / 2 - 4, 8, 8);
      gfx.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(gfx, value.toDisplayString(), dotX + 4, y + height / 2);
    }
  }

  private static TtlRegisterData getStateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WIDTH, COUNTERS);
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);
    advance(
        data,
        0,
        state.getPortValue(PORT_INDEX_1CP0),
        state.getPortValue(PORT_INDEX_1CP1),
        state.getPortValue(PORT_INDEX_1MR));
    advance(
        data,
        1,
        state.getPortValue(PORT_INDEX_2CP0),
        state.getPortValue(PORT_INDEX_2CP1),
        state.getPortValue(PORT_INDEX_2MR));
    drive(state, data.getValue(0), COUNTER1_OUTPUTS);
    drive(state, data.getValue(1), COUNTER2_OUTPUTS);
  }

  /**
   * Updates one half. The clock history is recorded even while reset is active, so an edge that
   * arrives during reset is not counted after reset is released.
   */
  private static void advance(TtlRegisterData data, int index, Value cp0, Value cp1, Value reset) {
    final var clocked = data.updateClock(derivedClock(cp0, cp1), index, StdAttr.TRIG_RISING);
    if (reset == Value.TRUE) {
      data.setValue(index, Value.createKnown(WIDTH, 0));
    } else if (clocked) {
      data.setValue(index, nextCount(data.getValue(index)));
    }
  }

  /** Rising edge of {@code CP0 OR NOT CP1}, which is the datasheet's two count rows. */
  private static Value derivedClock(Value cp0, Value cp1) {
    return cp0.or(cp1.not());
  }

  private static void drive(InstanceState state, Value word, int[] ports) {
    for (var bit = 0; bit < BITS; bit++) {
      state.setPort(ports[bit], word.get(bit), DELAY);
    }
  }

  /**
   * Advances one binary count. An error anywhere makes the next word entirely error. A word that is
   * only unknown stays unknown. A known word increments modulo 16.
   */
  private static Value nextCount(Value current) {
    if (containsBit(current, Value.ERROR)) return Value.createError(WIDTH);
    if (!current.isFullyDefined()) return Value.createUnknown(WIDTH);
    return Value.createKnown(WIDTH, (current.toLongValue() + 1) & COUNT_MASK);
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
    return new int[] {
      PORT_INDEX_1CP0, PORT_INDEX_1CP1, PORT_INDEX_2CP0, PORT_INDEX_2CP1
    };
  }
}
