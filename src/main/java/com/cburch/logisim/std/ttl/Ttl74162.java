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
 * TTL 74x162: synchronous presettable BCD decade counter with synchronous reset.
 *
 * <p>Simulation follows the Philips 74HC/HCT162 function table (December 1990). Reset, parallel
 * load and counting are sampled on the rising edge of {@code CP}. {@code MR} low clears the
 * counter. Otherwise {@code PE} low loads {@code D0} to {@code D3}. Otherwise both {@code CEP} and
 * {@code CET} must be high to count. {@code TC} is combinational: it is high only while {@code CET}
 * is high and the code is 9 ({@code Q3 Q2 Q1 Q0} = 1001). Nanosecond delays are not modeled.
 *
 * <p>The decade cycle is 0 through 9 and back to 0. Illegal codes return to that cycle in one or
 * two clocks, as on the 74HC160/162 output state diagram: 10 goes to 11 and then 6, 12 goes to 13
 * and then 4, and 14 goes to 15 and then 2.
 *
 * <p>An unknown or error input changes an output only when the two substitutions disagree. An error
 * on such an input makes the disagreed output an error; an unknown input makes it unknown.
 */
public class Ttl74162 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74162";

  public static final int DELAY = 1;

  /** Synchronous master reset, active low. */
  public static final byte MR = 1;

  /** Rising-edge clock. */
  public static final byte CP = 2;

  public static final byte D0 = 3;
  public static final byte D1 = 4;
  public static final byte D2 = 5;
  public static final byte D3 = 6;

  /** Count enable. Both this pin and {@link #CET} must be high to count. */
  public static final byte CEP = 7;

  public static final byte GND = 8;

  /** Parallel enable, active low. Load ignores {@link #CEP} and {@link #CET}. */
  public static final byte PE = 9;

  /** Count-enable carry. Also gates {@link #TC}. */
  public static final byte CET = 10;

  public static final byte Q3 = 11;
  public static final byte Q2 = 12;
  public static final byte Q1 = 13;
  public static final byte Q0 = 14;

  /** Terminal count. High only at code 9 while {@link #CET} is high. */
  public static final byte TC = 15;

  public static final byte VCC = 16;

  /**
   * Next code while counting. Indexes 10 to 15 are the illegal BCD codes, which rejoin the decade
   * cycle within two clocks.
   */
  static final int[] NEXT_COUNT = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 11, 6, 13, 4, 15, 2};

  private static final int WIDTH = 4;
  private static final byte[] OUTPUT_PINS = {Q3, Q2, Q1, Q0, TC};
  private static final String[] PORT_NAMES = {
    "MR (synchronous reset, active low)",
    "CP (clock)",
    "D0",
    "D1",
    "D2",
    "D3",
    "CEP (count enable)",
    "PE (parallel enable, active low)",
    "CET (count enable carry)",
    "Q3",
    "Q2",
    "Q1",
    "Q0",
    "TC (terminal count)"
  };
  private static final String[] PIN_NAMES = {
    "MR", "CP", "D0", "D1", "D2", "D3", "CEP", null,
    "PE", "CET", "Q3", "Q2", "Q1", "Q0", "TC", null
  };

  private static final int BIT_MR = 0;
  private static final int BIT_PE = 1;
  private static final int BIT_CEP = 2;
  private static final int BIT_CET = 3;
  private static final int BIT_DATA = 4;
  private static final int BIT_STORED = 8;
  private static final int COUNT_BITS = 12;
  private static final int TERMINAL_CODE = 9;

  /** Creates a 74162 synchronous presettable BCD decade counter. */
  public Ttl74162() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl74162HdlGenerator());
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Power pins are omitted from the port list.
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    return (byte) ((dsPinNr <= GND) ? dsPinNr - 1 : dsPinNr - 2);
  }

  /** Next counted code, including the illegal states 10 to 15. */
  static int nextCount(int code) {
    return NEXT_COUNT[code & 0xF];
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
    g.drawRect((x + 84), (y + 12), 12, 14);
    g.drawLine((x + 90), (y + 12), (x + 90), (y + 7));
    g.drawLine((x + 84), (((y + 12)) + 7) - 3, ((x + 84)) + 4, ((y + 12)) + 7);
    g.drawLine((x + 84), (((y + 12)) + 7) + 3, ((x + 84)) + 4, ((y + 12)) + 7);
    g.drawRect((x + 104), (y + 12), 12, 14);
    g.drawLine((x + 110), (y + 12), (x + 110), (y + 7));
    g.drawLine((x + 104), (((y + 12)) + 7) - 3, ((x + 104)) + 4, ((y + 12)) + 7);
    g.drawLine((x + 104), (((y + 12)) + 7) + 3, ((x + 104)) + 4, ((y + 12)) + 7);
    g.drawLine((x + 44), ((y + 12)) - 4, (x + 116), ((y + 12)) - 4);
    g.drawLine((x + 44), ((y + 12)) - 4, (x + 44), ((y + 12)) + 7);
    g.drawLine((x + 64), ((y + 12)) - 4, (x + 64), ((y + 12)) + 7);
    g.drawLine((x + 84), ((y + 12)) - 4, (x + 84), ((y + 12)) + 7);
    g.drawLine((x + 104), ((y + 12)) - 4, (x + 104), ((y + 12)) + 7);
    g.drawLine((x + 30), (y + height - 7), (x + 30), (y + height - 13));
    g.drawLine((x + 30), (y + height - 13), (x + 44), (y + height - 13));
    g.drawLine((x + 44), (y + height - 13), (x + 44), ((y + 12)) - 4);
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + height - 12));
    structureBubble(g, (x + 10), (y + height - 14));
    GraphicsUtil.drawCenteredText(g, "R", (x + 18), (y + height - 24));
    g.drawLine((x + 150), (y + 7), (x + 150), (y + 12));
    structureBubble(g, (x + 150), (y + 14));
    GraphicsUtil.drawCenteredText(g, "LD", (x + 140), (y + 24));
    g.drawLine((x + 130), (y + height - 7), (x + 130), (y + height - 18));
    GraphicsUtil.drawCenteredText(g, "P", (x + 122), (y + height - 24));
    g.drawLine((x + 130), (y + 7), (x + 130), (y + 18));
    GraphicsUtil.drawCenteredText(g, "T", (x + 122), (y + 24));
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 18));
    GraphicsUtil.drawCenteredText(g, "TC", (x + 40), (y + 24));
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getState(state);
    final var triggered = data.updateClock(input(state, CP), StdAttr.TRIG_RISING);
    final var count =
        resolveCount(
            data.getValue(),
            triggered,
            new Value[] {
              input(state, MR),
              input(state, PE),
              input(state, CEP),
              input(state, CET),
              input(state, D0),
              input(state, D1),
              input(state, D2),
              input(state, D3)
            });
    data.setValue(count);
    state.setPort(pinNrToPortNr(Q0), count.get(0), DELAY);
    state.setPort(pinNrToPortNr(Q1), count.get(1), DELAY);
    state.setPort(pinNrToPortNr(Q2), count.get(2), DELAY);
    state.setPort(pinNrToPortNr(Q3), count.get(3), DELAY);
    state.setPort(pinNrToPortNr(TC), resolveTerminal(count, input(state, CET)), DELAY);
  }

  private static TtlRegisterData getState(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.create(WIDTH));
      state.setData(data);
    }
    return data;
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }

  /**
   * Reset wins over load, and load wins over counting. Controls are sampled only on a rising edge;
   * otherwise the stored word is kept, unknown controls included.
   */
  private static Value resolveCount(Value stored, boolean triggered, Value[] controls) {
    if (!triggered) {
      return stored;
    }
    final var sources = new Value[COUNT_BITS];
    System.arraycopy(controls, 0, sources, 0, controls.length);
    for (var bit = 0; bit < WIDTH; bit++) {
      sources[BIT_STORED + bit] = stored.get(bit);
    }
    final var choice = new Choice(WIDTH);
    for (var mask = 0; mask < (1 << COUNT_BITS); mask++) {
      if (!accepts(sources, mask)) {
        continue;
      }
      final var data = (mask >> BIT_DATA) & 0xF;
      final var current = (mask >> BIT_STORED) & 0xF;
      final int next;
      if (!bitHigh(mask, BIT_MR)) {
        next = 0;
      } else if (!bitHigh(mask, BIT_PE)) {
        next = data;
      } else if (bitHigh(mask, BIT_CEP) && bitHigh(mask, BIT_CET)) {
        next = nextCount(current);
      } else {
        next = current;
      }
      choice.accept(next, errorIn(sources));
    }
    return choice.value();
  }

  /** Terminal count follows the current word and {@code CET} without waiting for a clock. */
  private static Value resolveTerminal(Value count, Value carryPin) {
    final var sources =
        new Value[] {carryPin, count.get(0), count.get(1), count.get(2), count.get(3)};
    final var choice = new Choice(1);
    for (var mask = 0; mask < (1 << sources.length); mask++) {
      if (!accepts(sources, mask)) {
        continue;
      }
      final var code = mask >> 1;
      final var terminal = bitHigh(mask, 0) && code == TERMINAL_CODE;
      choice.accept(terminal ? 1 : 0, errorIn(sources));
    }
    return choice.value();
  }

  private static boolean accepts(Value[] sources, int mask) {
    for (var index = 0; index < sources.length; index++) {
      final var actual = sources[index];
      final var high = bitHigh(mask, index);
      if (actual == Value.TRUE && !high) {
        return false;
      }
      if (actual == Value.FALSE && high) {
        return false;
      }
    }
    return true;
  }

  private static boolean errorIn(Value[] sources) {
    for (final var source : sources) {
      if (source == Value.ERROR) {
        return true;
      }
    }
    return false;
  }

  private static boolean bitHigh(int mask, int bit) {
    return (mask & (1 << bit)) != 0;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {pinNrToPortNr(CP)};
  }

  /** Merges every accepted substitution. Disagreements become unknown, or error if one was seen. */
  private static final class Choice {
    private final Value[] bits;
    private final boolean[] conflict;
    private boolean sawError;
    private boolean any;

    private Choice(int width) {
      bits = new Value[width];
      conflict = new boolean[width];
    }

    private void accept(int value, boolean error) {
      sawError |= error;
      if (!any) {
        any = true;
        for (var index = 0; index < bits.length; index++) {
          bits[index] = ((value >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
        }
        return;
      }
      for (var index = 0; index < bits.length; index++) {
        final var bit = ((value >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
        if (bits[index] != bit) {
          conflict[index] = true;
        }
      }
    }

    private Value value() {
      if (!any) {
        return Value.createUnknown(BitWidth.create(bits.length));
      }
      for (var index = 0; index < bits.length; index++) {
        if (conflict[index]) {
          bits[index] = sawError ? Value.ERROR : Value.UNKNOWN;
        }
      }
      return Value.create(bits);
    }
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
