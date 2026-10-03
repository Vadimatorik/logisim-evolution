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
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * TTL 74x169: presettable synchronous 4-bit binary up/down counter.
 *
 * <p>Simulation follows the NXP
 * <a href="https://www.nxp.com/docs/en/data-sheet/74HC_HCT169.pdf">74HC/HCT169</a>
 * function table, which matches 74LVC169. {@code CP} clocks on the low-to-high edge. A low
 * {@code PE} loads {@code D0} to {@code D3} on that edge, ahead of counting. Counting needs
 * {@code PE} high and both {@code CEP} and {@code CET} low. {@code U/D} high counts up and low
 * counts down, modulo 16. {@code TC} is active-low and combinational: it is low only when
 * {@code CET} is low and the count is terminal for the selected direction (15 up, 0 down).
 * {@code CEP} does not affect {@code TC}. The 74161 enables and terminal count are active high.
 */
public class Ttl74169 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74169";

  public static final int PORT_INDEX_UD = 0;
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
    "U/D (Up/Down, HIGH counts up)",
    "CP (Clock, rising edge)",
    "D0",
    "D1",
    "D2",
    "D3",
    "CEP (Count enable, active LOW)",
    "PE (Parallel enable, active LOW)",
    "CET (Count enable carry, active LOW)",
    "Q3",
    "Q2",
    "Q1",
    "Q0",
    "TC (Terminal count, active LOW)"
  };

  /** Creates a 74169 synchronous 4-bit up/down counter. */
  public Ttl74169() {
    super(_ID, (byte) 16, OUTPUT_PORTS, PORT_NAMES, new Ttl74169HdlGenerator());
    super.setInstancePoker(Poker.class);
  }

  /** Toggles a bit of the internal count while the internal structure is drawn. */
  public static class Poker extends InstancePoker {
    private boolean pressed;

    private boolean isInside(InstanceState state, MouseEvent event) {
      final var point = getTranslatedTtlXY(state, event);
      for (var index = 0; index < WIDTH.getWidth(); index++) {
        final var dx = point.x - (56 + index * 10);
        final var dy = point.y - 30;
        if (dx * dx + dy * dy < 16) return true;
      }
      return false;
    }

    private int bitIndex(InstanceState state, MouseEvent event) {
      final var point = getTranslatedTtlXY(state, event);
      for (var index = 0; index < WIDTH.getWidth(); index++) {
        final var dx = point.x - (56 + index * 10);
        final var dy = point.y - 30;
        if (dx * dx + dy * dy < 16) return WIDTH.getWidth() - 1 - index;
      }
      return 0;
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
      final var data = (TtlRegisterData) state.getData();
      if (data == null) return;
      final var bits = data.getValue().getAll();
      final var index = bitIndex(state, event);
      bits[index] = bits[index] == Value.TRUE ? Value.FALSE : Value.TRUE;
      data.setValue(Value.create(bits));
      publish(state, data.getValue());
      pressed = false;
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
          "U/D", "CP", "D0", "D1", "D2", "D3", "CEP", "PE", "CET", "Q3", "Q2", "Q1", "Q0", "TC"
        });
    drawState(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private void drawState(Graphics2D gfx, int x, int y, int height, TtlRegisterData state) {
    if (state == null) return;
    for (var index = 0; index < WIDTH.getWidth(); index++) {
      final var bit = state.getValue().get(WIDTH.getWidth() - 1 - index);
      final var originX = x + 52 + index * 10;
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
    if (data.updateClock(state.getPortValue(PORT_INDEX_CP), StdAttr.TRIG_RISING)) {
      data.setValue(nextValue(state, data.getValue()));
    }
    publish(state, data.getValue());
  }

  /**
   * Next count after a rising edge. A low {@code PE} loads. Both enables low count in the
   * selected direction. Any other fully defined enable combination holds. An ambiguous control
   * keeps the word when every candidate matches, and otherwise makes the whole word unknown or
   * error.
   */
  private static Value nextValue(InstanceState state, Value current) {
    final var upDown = state.getPortValue(PORT_INDEX_UD);
    final var cep = state.getPortValue(PORT_INDEX_CEP);
    final var cet = state.getPortValue(PORT_INDEX_CET);
    final var pe = state.getPortValue(PORT_INDEX_PE);
    final var loaded = dataWord(state);
    if (pe == Value.FALSE) return loaded;
    if (pe == Value.TRUE && (cep == Value.TRUE || cet == Value.TRUE)) return current;
    if (pe == Value.TRUE && cep == Value.FALSE && cet == Value.FALSE) {
      return counted(current, upDown);
    }
    return ambiguous(pe, cep, cet, upDown, loaded, current);
  }

  private static Value dataWord(InstanceState state) {
    return Value.create(
        new Value[] {
          state.getPortValue(PORT_INDEX_D0),
          state.getPortValue(PORT_INDEX_D1),
          state.getPortValue(PORT_INDEX_D2),
          state.getPortValue(PORT_INDEX_D3)
        });
  }

  private static Value counted(Value current, Value upDown) {
    if (containsBit(current, Value.ERROR) || upDown == Value.ERROR) {
      return Value.createError(WIDTH);
    }
    if (!current.isFullyDefined() || (upDown != Value.TRUE && upDown != Value.FALSE)) {
      return Value.createUnknown(WIDTH);
    }
    final var delta = upDown == Value.TRUE ? 1 : -1;
    return Value.createKnown(WIDTH, (current.toLongValue() + delta) & 0xF);
  }

  private static Value ambiguous(
      Value pe, Value cep, Value cet, Value upDown, Value loaded, Value current) {
    final var countPossible = pe != Value.FALSE && cep != Value.TRUE && cet != Value.TRUE;
    final var holdPossible = pe != Value.FALSE && !(cep == Value.FALSE && cet == Value.FALSE);
    final var options = new ArrayList<Value>();
    if (pe != Value.TRUE) options.add(loaded);
    if (holdPossible) options.add(current);
    if (countPossible) options.add(counted(current, upDown));
    var errorSelects = pe == Value.ERROR;
    if (countPossible && (cep == Value.ERROR || cet == Value.ERROR || upDown == Value.ERROR)) {
      errorSelects = true;
    }
    return mergeWords(options, errorSelects);
  }

  private static Value mergeWords(List<Value> options, boolean errorSelects) {
    if (options.isEmpty()) {
      return errorSelects ? Value.createError(WIDTH) : Value.createUnknown(WIDTH);
    }
    final var first = options.get(0);
    for (var index = 1; index < options.size(); index++) {
      final var other = options.get(index);
      if (!sameWord(first, other)) {
        final var erroneous =
            errorSelects || containsBit(first, Value.ERROR) || containsBit(other, Value.ERROR);
        return erroneous ? Value.createError(WIDTH) : Value.createUnknown(WIDTH);
      }
    }
    return first;
  }

  private static boolean sameWord(Value left, Value right) {
    for (var index = 0; index < WIDTH.getWidth(); index++) {
      if (left.get(index) != right.get(index)) return false;
    }
    return true;
  }

  private static boolean containsBit(Value word, Value bit) {
    for (var index = 0; index < word.getWidth(); index++) {
      if (word.get(index) == bit) return true;
    }
    return false;
  }

  private static void publish(InstanceState state, Value value) {
    state.setPort(PORT_INDEX_Q0, value.get(0), DELAY);
    state.setPort(PORT_INDEX_Q1, value.get(1), DELAY);
    state.setPort(PORT_INDEX_Q2, value.get(2), DELAY);
    state.setPort(PORT_INDEX_Q3, value.get(3), DELAY);
    state.setPort(
        PORT_INDEX_TC,
        terminalCount(value, state.getPortValue(PORT_INDEX_CET), state.getPortValue(PORT_INDEX_UD)),
        DELAY);
  }

  /**
   * Active-low terminal count. High {@code CET} forces it high. It is low only when {@code CET}
   * is low and every bit proves the terminal code for the selected direction.
   */
  static Value terminalCount(Value count, Value cet, Value upDown) {
    if (cet == Value.TRUE) return Value.TRUE;
    final var upTerminal = terminal(count, true);
    final var downTerminal = terminal(count, false);
    if (upDown == Value.TRUE && upTerminal == Certainty.NO) return Value.TRUE;
    if (upDown == Value.FALSE && downTerminal == Certainty.NO) return Value.TRUE;
    if (upTerminal == Certainty.NO && downTerminal == Certainty.NO) return Value.TRUE;
    if (cet == Value.FALSE && upDown == Value.TRUE && upTerminal == Certainty.YES) {
      return Value.FALSE;
    }
    if (cet == Value.FALSE && upDown == Value.FALSE && downTerminal == Certainty.YES) {
      return Value.FALSE;
    }
    if (cet == Value.ERROR
        || upDown == Value.ERROR
        || upTerminal == Certainty.ERROR
        || downTerminal == Certainty.ERROR) {
      return Value.ERROR;
    }
    return Value.UNKNOWN;
  }

  private static Certainty terminal(Value count, boolean up) {
    final var blocking = up ? Value.FALSE : Value.TRUE;
    final var required = up ? Value.TRUE : Value.FALSE;
    var unknown = false;
    var error = false;
    for (var index = 0; index < count.getWidth(); index++) {
      final var bit = count.get(index);
      if (bit == blocking) return Certainty.NO;
      if (bit == required) continue;
      if (bit == Value.ERROR) error = true;
      else unknown = true;
    }
    if (error) return Certainty.ERROR;
    if (unknown) return Certainty.UNKNOWN;
    return Certainty.YES;
  }

  private enum Certainty {
    NO,
    YES,
    UNKNOWN,
    ERROR
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
