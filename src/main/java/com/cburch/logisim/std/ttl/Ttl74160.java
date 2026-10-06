/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.data.Value.falseColor;
import static com.cburch.logisim.data.Value.trueColor;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

/**
 * TTL 74HC160: synchronous presettable BCD decade counter with asynchronous reset.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC160.pdf">Nexperia 74HC160</a>
 * function table. {@code MR} low clears the outputs immediately. A rising {@code CP} loads {@code
 * D0} to {@code D3} when {@code PE} is low, and counts when {@code PE}, {@code CEP} and {@code CET}
 * are high. {@code TC} is high only when {@code CET} is high and the output code is {@code 1001}.
 *
 * <p>Counting uses the synchronous decade toggle equations {@code T0 = 1}, {@code T1 = Q0 & ~Q3},
 * {@code T2 = Q0 & Q1} and {@code T3 = Q0 & Q1 & Q2 | Q0 & Q3}. Codes 10 to 15 therefore return to
 * the BCD sequence within two clocks: 10 to 11 to 6, 12 to 13 to 4, and 14 to 15 to 2. An {@code
 * MR} level that is not exactly low is not a reset. Nanosecond delays are not modeled.
 */
public class Ttl74160 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74160";

  public static final int PORT_INDEX_MR = 0;
  public static final int PORT_INDEX_CP = 1;
  public static final int PORT_INDEX_D0 = 2;
  public static final int PORT_INDEX_D1 = 3;
  public static final int PORT_INDEX_D2 = 4;
  public static final int PORT_INDEX_D3 = 5;
  public static final int PORT_INDEX_CEP = 6;
  public static final int PORT_INDEX_PE = 7;
  public static final int PORT_INDEX_CET = 8;
  public static final int PORT_INDEX_Q3 = 9;
  public static final int PORT_INDEX_Q2 = 10;
  public static final int PORT_INDEX_Q1 = 11;
  public static final int PORT_INDEX_Q0 = 12;
  public static final int PORT_INDEX_TC = 13;

  private static final int DELAY = 1;
  private static final BitWidth WIDTH = BitWidth.create(4);
  private static final byte[] OUTPUT_PORTS = {11, 12, 13, 14, 15};
  private static final String[] PORT_NAMES = {
    "MR / CLR (master reset, active low)",
    "CP / CLK (clock)",
    "D0 / A",
    "D1 / B",
    "D2 / C",
    "D3 / D",
    "CEP / ENP (count enable)",
    "PE / LOAD (parallel enable, active low)",
    "CET / ENT (count enable carry)",
    "Q3 / QD",
    "Q2 / QC",
    "Q1 / QB",
    "Q0 / QA",
    "TC / RCO (terminal count)"
  };

  /** Creates a 74160 synchronous BCD decade counter. */
  public Ttl74160() {
    super(_ID, (byte) 16, OUTPUT_PORTS, PORT_NAMES, new Ttl74160HdlGenerator());
    super.setInstancePoker(Poker.class);
  }

  /** Toggles one stored bit when its drawn dot is clicked. */
  public static class Poker extends InstancePoker {
    private boolean isPressed = true;

    private boolean isInside(InstanceState state, MouseEvent e) {
      final var p = getTranslatedTtlXY(state, e);
      var inside = false;
      for (var i = 0; i < 4; i++) {
        final var dx = p.x - (56 + i * 10);
        final var dy = p.y - 30;
        inside |= dx * dx + dy * dy < 4 * 4;
      }
      return inside;
    }

    private int getIndex(InstanceState state, MouseEvent e) {
      final var p = getTranslatedTtlXY(state, e);
      for (var i = 0; i < 4; i++) {
        final var dx = p.x - (56 + i * 10);
        final var dy = p.y - 30;
        if (dx * dx + dy * dy < 4 * 4) return 3 - i;
      }
      return 0;
    }

    @Override
    public void mousePressed(InstanceState state, MouseEvent e) {
      isPressed = isInside(state, e);
    }

    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
      if (!state.getAttributeValue(TtlLibrary.DRAW_INTERNAL_STRUCTURE)) return;
      if (!isPressed || !isInside(state, e)) {
        isPressed = false;
        return;
      }
      final var data = (TtlRegisterData) state.getData();
      if (data == null) return;
      final var currentValue = data.getValue();
      var current = currentValue.isFullyDefined() ? currentValue.toLongValue() : 0L;
      current ^= 1L << getIndex(state, e);
      updateState(state, Value.createKnown(WIDTH, current));
      isPressed = false;
    }
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var gfx = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, false, false);
    Drawgates.paintPortNames(
        painter,
        x,
        y,
        height,
        new String[] {
          "MR", "CP", "D0", "D1", "D2", "D3", "CEP", "PE", "CET", "Q3", "Q2", "Q1", "Q0", "TC"
        });
    drawState(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private void drawState(Graphics2D gfx, int x, int y, int height, TtlRegisterData state) {
    if (state == null) return;
    for (var i = 0; i < 4; i++) {
      final var bit = state.getValue().get(3 - i);
      final var known = bit == Value.TRUE || bit == Value.FALSE;
      gfx.setColor(known ? (bit == Value.TRUE ? trueColor : falseColor) : bit.getColor());
      gfx.fillOval(x + 52 + i * 10, y + height / 2 - 4, 8, 8);
      gfx.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(gfx, bit.toDisplayString(), x + 56 + i * 10, y + height / 2);
    }
    gfx.setColor(Color.BLACK);
  }

  private static void updateState(InstanceState state, Value value) {
    getStateData(state).setValue(value);
    publish(state, value);
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
    final var triggered = data.updateClock(state.getPortValue(PORT_INDEX_CP), StdAttr.TRIG_RISING);
    final var masterReset = state.getPortValue(PORT_INDEX_MR);
    var counter = data.getValue();
    if (masterReset == Value.FALSE) {
      counter = Value.createKnown(WIDTH, 0);
    } else if (triggered) {
      counter = onRisingClock(state, counter);
    }
    updateState(state, counter);
  }

  /**
   * Loads, counts, or holds. A low {@code PE} wins over the count enables. Counting requires both
   * enables high. Any other combination of those three pins leaves the word unknown, or error when
   * one of them is error.
   */
  private static Value onRisingClock(InstanceState state, Value counter) {
    final var parallelEnable = state.getPortValue(PORT_INDEX_PE);
    final var countEnable = state.getPortValue(PORT_INDEX_CEP);
    final var carryEnable = state.getPortValue(PORT_INDEX_CET);
    if (parallelEnable == Value.FALSE) return loaded(state);
    if (parallelEnable == Value.TRUE
        && (countEnable == Value.FALSE || carryEnable == Value.FALSE)) {
      return counter;
    }
    if (parallelEnable == Value.TRUE
        && countEnable == Value.TRUE
        && carryEnable == Value.TRUE) {
      return nextCount(counter);
    }
    return contaminated(parallelEnable, countEnable, carryEnable);
  }

  private static Value loaded(InstanceState state) {
    return Value.create(
        new Value[] {
          state.getPortValue(PORT_INDEX_D0),
          state.getPortValue(PORT_INDEX_D1),
          state.getPortValue(PORT_INDEX_D2),
          state.getPortValue(PORT_INDEX_D3)
        });
  }

  /**
   * Next decade count. An error bit makes the whole word error, and any other undefined word stays
   * unknown. A known word follows the synchronous BCD toggle equations.
   */
  private static Value nextCount(Value current) {
    if (containsBit(current, Value.ERROR)) return Value.createError(WIDTH);
    if (!current.isFullyDefined()) return Value.createUnknown(WIDTH);
    final var q0 = current.get(0) == Value.TRUE;
    final var q1 = current.get(1) == Value.TRUE;
    final var q2 = current.get(2) == Value.TRUE;
    final var q3 = current.get(3) == Value.TRUE;
    return Value.createKnown(
        WIDTH,
        (nibble(!q0) << 0)
            | (nibble(q1 ^ (q0 && !q3)) << 1)
            | (nibble(q2 ^ (q0 && q1)) << 2)
            | (nibble(q3 ^ ((q0 && q1 && q2) || (q0 && q3))) << 3));
  }

  private static int nibble(boolean bit) {
    return bit ? 1 : 0;
  }

  /** High only for code 9 while {@code CET} is high. A low {@code CET} forces this output low. */
  private static Value terminalCount(Value word, Value carryEnable) {
    if (carryEnable == Value.FALSE) return Value.FALSE;
    final var pattern =
        new Value[] {Value.TRUE, Value.FALSE, Value.FALSE, Value.TRUE};
    var couldBeNine = true;
    var isNine = true;
    var error = carryEnable == Value.ERROR;
    for (var bit = 0; bit < 4; bit++) {
      final var value = word.get(bit);
      if (value == Value.ERROR) error = true;
      if (value != pattern[bit]) isNine = false;
      if (value != pattern[bit] && value != Value.UNKNOWN && value != Value.ERROR) {
        couldBeNine = false;
      }
    }
    if (!couldBeNine) return Value.FALSE;
    if (carryEnable == Value.TRUE && isNine && !error) return Value.TRUE;
    return error ? Value.ERROR : Value.UNKNOWN;
  }

  private static Value contaminated(Value... controls) {
    for (final var control : controls) {
      if (control == Value.ERROR) return Value.createError(WIDTH);
    }
    return Value.createUnknown(WIDTH);
  }

  private static boolean containsBit(Value word, Value bit) {
    for (var i = 0; i < word.getWidth(); i++) {
      if (word.get(i) == bit) return true;
    }
    return false;
  }

  private static void publish(InstanceState state, Value word) {
    state.setPort(PORT_INDEX_Q0, word.get(0), DELAY);
    state.setPort(PORT_INDEX_Q1, word.get(1), DELAY);
    state.setPort(PORT_INDEX_Q2, word.get(2), DELAY);
    state.setPort(PORT_INDEX_Q3, word.get(3), DELAY);
    state.setPort(
        PORT_INDEX_TC, terminalCount(word, state.getPortValue(PORT_INDEX_CET)), DELAY);
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
