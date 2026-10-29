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
    "CEP (Count enable, active low)",
    "PE (Parallel enable, active low)",
    "CET (Count enable carry, active low)",
    "Q3",
    "Q2",
    "Q1",
    "Q0",
    "TC (Terminal count, active low)"
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
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var rawState = painter.getData();
    final TtlRegisterData stored = rawState instanceof TtlRegisterData ? (TtlRegisterData) rawState : null;
    g.drawRect((x + 44), (y + 12), 12, 14);
    g.drawLine((x + 50), (y + 12), (x + 50), (y + 7));
    g.drawLine((x + 44), (((y + 12)) + 7) - 3, ((x + 44)) + 4, ((y + 12)) + 7);
    g.drawLine((x + 44), (((y + 12)) + 7) + 3, ((x + 44)) + 4, ((y + 12)) + 7);
    structureDot(g, (x + 50), ((y + 12)) + 7, stored, 0, 0);
    g.drawRect((x + 64), (y + 12), 12, 14);
    g.drawLine((x + 70), (y + 12), (x + 70), (y + 7));
    g.drawLine((x + 64), (((y + 12)) + 7) - 3, ((x + 64)) + 4, ((y + 12)) + 7);
    g.drawLine((x + 64), (((y + 12)) + 7) + 3, ((x + 64)) + 4, ((y + 12)) + 7);
    structureDot(g, (x + 70), ((y + 12)) + 7, stored, 0, 1);
    g.drawRect((x + 84), (y + 12), 12, 14);
    g.drawLine((x + 90), (y + 12), (x + 90), (y + 7));
    g.drawLine((x + 84), (((y + 12)) + 7) - 3, ((x + 84)) + 4, ((y + 12)) + 7);
    g.drawLine((x + 84), (((y + 12)) + 7) + 3, ((x + 84)) + 4, ((y + 12)) + 7);
    structureDot(g, (x + 90), ((y + 12)) + 7, stored, 0, 2);
    g.drawRect((x + 104), (y + 12), 12, 14);
    g.drawLine((x + 110), (y + 12), (x + 110), (y + 7));
    g.drawLine((x + 104), (((y + 12)) + 7) - 3, ((x + 104)) + 4, ((y + 12)) + 7);
    g.drawLine((x + 104), (((y + 12)) + 7) + 3, ((x + 104)) + 4, ((y + 12)) + 7);
    structureDot(g, (x + 110), ((y + 12)) + 7, stored, 0, 3);
    g.drawLine((x + 44), ((y + 12)) - 4, (x + 116), ((y + 12)) - 4);
    g.drawLine((x + 44), ((y + 12)) - 4, (x + 44), ((y + 12)) + 7);
    g.drawLine((x + 64), ((y + 12)) - 4, (x + 64), ((y + 12)) + 7);
    g.drawLine((x + 84), ((y + 12)) - 4, (x + 84), ((y + 12)) + 7);
    g.drawLine((x + 104), ((y + 12)) - 4, (x + 104), ((y + 12)) + 7);
    g.drawLine((x + 30), (y + height - 7), (x + 30), (y + height - 13));
    g.drawLine((x + 30), (y + height - 13), (x + 44), (y + height - 13));
    g.drawLine((x + 44), (y + height - 13), (x + 44), ((y + 12)) - 4);
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + height - 18));
    GraphicsUtil.drawCenteredText(g, "U", (x + 18), (y + height - 24));
    g.drawLine((x + 150), (y + 7), (x + 150), (y + 12));
    structureBubble(g, (x + 150), (y + 14));
    GraphicsUtil.drawCenteredText(g, "LD", (x + 140), (y + 24));
    g.drawLine((x + 130), (y + height - 7), (x + 130), (y + height - 12));
    structureBubble(g, (x + 130), (y + height - 14));
    GraphicsUtil.drawCenteredText(g, "P", (x + 122), (y + height - 24));
    g.drawLine((x + 130), (y + 7), (x + 130), (y + 12));
    structureBubble(g, (x + 130), (y + 14));
    GraphicsUtil.drawCenteredText(g, "T", (x + 122), (y + 24));
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 12));
    structureBubble(g, (x + 30), (y + 14));
    GraphicsUtil.drawCenteredText(g, "TC", (x + 40), (y + 24));
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
