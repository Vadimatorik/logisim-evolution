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
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;

/**
 * TTL 74x191: synchronous presettable 4-bit binary up/down counter.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC191.pdf">74HC191</a> and
 * <a href="https://www.ti.com/lit/ds/symlink/sn74hc191.pdf">SN74HC191</a> data sheets. A low
 * {@code PL} asynchronously loads {@code D0} to {@code D3}. While {@code PL} is high, a rising
 * {@code CP} counts when {@code CE} is low: low {@code U/D} counts up and high {@code U/D} counts
 * down, modulo 16. {@code TC} is high at terminal count (15 counting up, 0 counting down).
 * {@code RC} is a low pulse that follows {@code CP} only while that terminal count is active and
 * {@code CE} is low. Setup constraints on {@code CE} and {@code U/D} are not modeled.
 */
public class Ttl74191 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74191";

  public static final int PORT_INDEX_D1 = 0;
  public static final int PORT_INDEX_Q1 = 1;
  public static final int PORT_INDEX_Q0 = 2;
  public static final int PORT_INDEX_CE = 3;
  public static final int PORT_INDEX_UD = 4;
  public static final int PORT_INDEX_Q2 = 5;
  public static final int PORT_INDEX_Q3 = 6;
  public static final int PORT_INDEX_D3 = 7;
  public static final int PORT_INDEX_D2 = 8;
  public static final int PORT_INDEX_PL = 9;
  public static final int PORT_INDEX_TC = 10;
  public static final int PORT_INDEX_RC = 11;
  public static final int PORT_INDEX_CP = 12;
  public static final int PORT_INDEX_D0 = 13;

  private static final int DELAY = 1;
  private static final int BITS = 4;
  private static final int MASK = (1 << BITS) - 1;
  private static final BitWidth WIDTH = BitWidth.create(BITS);
  private static final byte[] OUTPUT_PORTS = {2, 3, 6, 7, 12, 13};
  private static final int[] OUTPUT_INDEXES = {
    PORT_INDEX_Q0, PORT_INDEX_Q1, PORT_INDEX_Q2, PORT_INDEX_Q3
  };
  private static final String[] PORT_NAMES = {
    "D1",
    "Q1",
    "Q0",
    "CE (Count enable, active low)",
    "U/D (Up/down, low counts up)",
    "Q2",
    "Q3",
    "D3",
    "D2",
    "PL (Parallel load, active low)",
    "TC (Terminal count)",
    "RC (Ripple clock, active low)",
    "CP (Clock, rising edge)",
    "D0"
  };

  /** Creates a 74191 synchronous presettable 4-bit binary up/down counter. */
  public Ttl74191() {
    super(_ID, (byte) 16, OUTPUT_PORTS, PORT_NAMES, new Ttl74191HdlGenerator());
    super.setInstancePoker(Poker.class);
  }

  /** Toggles one stored bit when the internal structure is drawn. */
  public static class Poker extends InstancePoker {
    private boolean pressed;

    private boolean isInside(InstanceState state, MouseEvent event) {
      return bitIndex(state, event) >= 0;
    }

    private int bitIndex(InstanceState state, MouseEvent event) {
      final var point = getTranslatedTtlXY(state, event);
      for (var shown = 0; shown < BITS; shown++) {
        final var dx = point.x - (56 + shown * 10);
        final var dy = point.y - 30;
        if (dx * dx + dy * dy < 4 * 4) return BITS - 1 - shown;
      }
      return -1;
    }

    @Override
    public void mousePressed(InstanceState state, MouseEvent event) {
      pressed = isInside(state, event);
    }

    @Override
    public void mouseReleased(InstanceState state, MouseEvent event) {
      if (!state.getAttributeValue(TtlLibrary.DRAW_INTERNAL_STRUCTURE)) return;
      if (!pressed || !isInside(state, event)) {
        pressed = false;
        return;
      }
      final var index = bitIndex(state, event);
      final var data = (TtlRegisterData) state.getData();
      pressed = false;
      if (data == null) return;
      final var bits = data.getValue().getAll();
      if (bits[index] == Value.TRUE) {
        bits[index] = Value.FALSE;
      } else if (bits[index] == Value.FALSE) {
        bits[index] = Value.TRUE;
      } else {
        return;
      }
      data.setValue(Value.create(bits));
      publish(state);
    }
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var rawState = painter.getData();
    final TtlRegisterData stored = rawState instanceof TtlRegisterData ? (TtlRegisterData) rawState : null;
    g.drawRect((x + 14), (y + height - 28), 12, 12);
    g.drawLine((x + 50), (y + height - 7), (x + 50), (y + height - 13));
    g.drawLine((x + 50), (y + height - 13), (x + 20), (y + height - 13));
    g.drawLine((x + 20), (y + height - 13), (x + 20), ((y + height - 28)) + 12);
    g.drawLine((x + 14), (((y + height - 28)) + 6) - 3, ((x + 14)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 14), (((y + height - 28)) + 6) + 3, ((x + 14)) + 4, ((y + height - 28)) + 6);
    structureDot(g, (x + 20), ((y + height - 28)) + 6, stored, 0, 0);
    g.drawRect((x + 47), (y + height - 28), 12, 12);
    g.drawLine((x + 30), (y + height - 7), (x + 30), (y + height - 13));
    g.drawLine((x + 30), (y + height - 13), (x + 53), (y + height - 13));
    g.drawLine((x + 53), (y + height - 13), (x + 53), ((y + height - 28)) + 12);
    g.drawLine((x + 47), (((y + height - 28)) + 6) - 3, ((x + 47)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 47), (((y + height - 28)) + 6) + 3, ((x + 47)) + 4, ((y + height - 28)) + 6);
    g.drawRect((x + 80), (y + height - 28), 12, 12);
    g.drawLine((x + 110), (y + height - 7), (x + 110), (y + height - 13));
    g.drawLine((x + 110), (y + height - 13), (x + 86), (y + height - 13));
    g.drawLine((x + 86), (y + height - 13), (x + 86), ((y + height - 28)) + 12);
    g.drawLine((x + 80), (((y + height - 28)) + 6) - 3, ((x + 80)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 80), (((y + height - 28)) + 6) + 3, ((x + 80)) + 4, ((y + height - 28)) + 6);
    g.drawRect((x + 113), (y + height - 28), 12, 12);
    g.drawLine((x + 130), (y + height - 7), (x + 130), (y + height - 13));
    g.drawLine((x + 130), (y + height - 13), (x + 119), (y + height - 13));
    g.drawLine((x + 119), (y + height - 13), (x + 119), ((y + height - 28)) + 12);
    g.drawLine((x + 113), (((y + height - 28)) + 6) - 3, ((x + 113)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 113), (((y + height - 28)) + 6) + 3, ((x + 113)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 14), ((y + height - 28)) + 18, (x + 125), ((y + height - 28)) + 18);
    g.drawLine((x + 14), ((y + height - 28)) + 18, (x + 14), ((y + height - 28)) + 7);
    g.drawLine((x + 47), ((y + height - 28)) + 18, (x + 47), ((y + height - 28)) + 7);
    g.drawLine((x + 80), ((y + height - 28)) + 18, (x + 80), ((y + height - 28)) + 7);
    g.drawLine((x + 113), ((y + height - 28)) + 18, (x + 113), ((y + height - 28)) + 7);
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 13));
    g.drawLine((x + 50), (y + 13), (x + 14), (y + 13));
    g.drawLine((x + 14), (y + 13), (x + 14), ((y + height - 28)) + 18);
    g.drawLine((x + 70), (y + height - 7), (x + 70), (y + height - 13));
    g.drawLine((x + 70), (y + height - 13), (x + 119), (y + height - 13));
    g.drawLine((x + 119), (y + height - 13), (x + 119), (y + 16));
    structureBubble(g, (x + 70), (y + height - 7));
    GraphicsUtil.drawCenteredText(g, "CE", (x + 70), (y + height - 24));
    g.drawLine((x + 110), (y + 7), (x + 110), (y + 13));
    g.drawLine((x + 110), (y + 13), (x + 119), (y + 13));
    g.drawLine((x + 119), (y + 13), (x + 119), (y + 16));
    structureBubble(g, (x + 110), (y + 7));
    GraphicsUtil.drawCenteredText(g, "LD", (x + 110), (y + 24));
    g.drawLine((x + 90), (y + height - 7), (x + 90), (y + height - 13));
    g.drawLine((x + 90), (y + height - 13), (x + 119), (y + height - 13));
    g.drawLine((x + 119), (y + height - 13), (x + 119), (y + 16));
    GraphicsUtil.drawCenteredText(g, "U", (x + 90), (y + height - 24));
    g.drawLine((x + 90), (y + 7), (x + 90), (y + 13));
    g.drawLine((x + 90), (y + 13), (x + 119), (y + 13));
    g.drawLine((x + 119), (y + 13), (x + 119), (y + 16));
    GraphicsUtil.drawCenteredText(g, "TC", (x + 90), (y + 24));
    g.drawLine((x + 70), (y + 7), (x + 70), (y + 13));
    g.drawLine((x + 70), (y + 13), (x + 119), (y + 13));
    g.drawLine((x + 119), (y + 13), (x + 119), (y + 16));
    structureBubble(g, (x + 70), (y + 7));
    GraphicsUtil.drawCenteredText(g, "RC", (x + 70), (y + 24));
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
    final var parallelLoad = state.getPortValue(PORT_INDEX_PL);
    final var countEnable = state.getPortValue(PORT_INDEX_CE);
    final var direction = state.getPortValue(PORT_INDEX_UD);
    final var rising = data.updateClock(state.getPortValue(PORT_INDEX_CP), StdAttr.TRIG_RISING);
    if (parallelLoad == Value.FALSE) {
      data.setValue(loadedWord(state));
    } else if (parallelLoad == Value.TRUE && rising && countEnable == Value.FALSE) {
      if (direction == Value.FALSE) {
        data.setValue(step(data.getValue(), 1));
      } else if (direction == Value.TRUE) {
        data.setValue(step(data.getValue(), -1));
      }
    }
    publish(state);
  }

  private static Value loadedWord(InstanceState state) {
    return Value.create(
        new Value[] {
          state.getPortValue(PORT_INDEX_D0),
          state.getPortValue(PORT_INDEX_D1),
          state.getPortValue(PORT_INDEX_D2),
          state.getPortValue(PORT_INDEX_D3)
        });
  }

  /**
   * Moves a binary count by {@code delta}. An error anywhere makes the next word entirely error. A
   * word that is only unknown stays unknown. A known word wraps modulo 16.
   */
  private static Value step(Value current, int delta) {
    if (containsBit(current, Value.ERROR)) return Value.createError(WIDTH);
    if (!current.isFullyDefined()) return Value.createUnknown(WIDTH);
    return Value.createKnown(WIDTH, (current.toLongValue() + delta) & MASK);
  }

  private static boolean containsBit(Value word, Value bit) {
    for (var index = 0; index < word.getWidth(); index++) {
      if (word.get(index) == bit) return true;
    }
    return false;
  }

  private static void publish(InstanceState state) {
    final var count = getStateData(state).getValue();
    for (var bit = 0; bit < BITS; bit++) {
      state.setPort(OUTPUT_INDEXES[bit], count.get(bit), DELAY);
    }
    final var terminal = terminalCount(count, state.getPortValue(PORT_INDEX_UD));
    state.setPort(PORT_INDEX_TC, terminal, DELAY);
    state.setPort(
        PORT_INDEX_RC,
        rippleClock(terminal, state.getPortValue(PORT_INDEX_CE), state.getPortValue(PORT_INDEX_CP)),
        DELAY);
  }

  /**
   * Terminal count is the AND of every bit when counting up, and the NOR of every bit when counting
   * down. A known mismatch forces low. An unknown direction is low when neither terminal state is
   * possible.
   */
  private static Value terminalCount(Value count, Value direction) {
    final var countingUp = matches(count, MASK);
    final var countingDown = matches(count, 0);
    if (direction == Value.FALSE) return countingUp;
    if (direction == Value.TRUE) return countingDown;
    if (countingUp == countingDown) return countingUp;
    if (countingUp == Value.ERROR || countingDown == Value.ERROR) return Value.ERROR;
    return Value.UNKNOWN;
  }

  private static Value matches(Value count, int pattern) {
    var sawError = false;
    var sawUnknown = false;
    for (var bit = 0; bit < BITS; bit++) {
      final var expected = ((pattern >> bit) & 1) == 0 ? Value.FALSE : Value.TRUE;
      final var actual = count.get(bit);
      if (actual == expected) continue;
      if (actual == Value.TRUE || actual == Value.FALSE) return Value.FALSE;
      if (actual == Value.ERROR) sawError = true;
      else sawUnknown = true;
    }
    if (sawError) return Value.ERROR;
    return sawUnknown ? Value.UNKNOWN : Value.TRUE;
  }

  /**
   * Ripple clock is low only while terminal count is high, count enable is low, and the clock is
   * low. Any input that proves that condition false forces the output high.
   */
  private static Value rippleClock(Value terminal, Value countEnable, Value clock) {
    if (terminal == Value.FALSE || countEnable == Value.TRUE || clock == Value.TRUE) {
      return Value.TRUE;
    }
    if (terminal == Value.TRUE && countEnable == Value.FALSE && clock == Value.FALSE) {
      return Value.FALSE;
    }
    if (terminal == Value.ERROR || countEnable == Value.ERROR || clock == Value.ERROR) {
      return Value.ERROR;
    }
    return Value.UNKNOWN;
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
