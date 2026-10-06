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
 * TTL 74x4516: presettable synchronous 4-bit binary up/down counter.
 *
 * <p>Simulation follows the Philips 74HC/HCT4516 product specification (December 1990). The device
 * is pin compatible with the 4516 of the 4000B series. {@code MR} high asynchronously clears the
 * counter and overrides every other input. While {@code MR} is low and {@code PL} is high, {@code
 * Q0} to {@code Q3} follow {@code D0} to {@code D3} without a clock. When both {@code PL} and
 * {@code CE} are low, the counter changes on the low-to-high transition of {@code CP}: {@code
 * UP/DN} high counts up and low counts down, modulo 16. {@code TC} is combinational and active low.
 * It is low only while {@code CE} is low and the count is 15 in the up direction or 0 in the down
 * direction. Nanosecond delays are not modeled.
 *
 * <p>An unknown or error input changes an output only when the two substitutions disagree. An error
 * on such an input makes the disagreed output an error; an unknown input makes it unknown.
 */
public class Ttl744516 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744516";

  public static final int DELAY = 1;

  /** Asynchronous parallel load, active high. */
  public static final byte PL = 1;

  public static final byte Q3 = 2;
  public static final byte D3 = 3;
  public static final byte D0 = 4;

  /** Count enable, active low. */
  public static final byte CE = 5;

  public static final byte Q0 = 6;

  /** Terminal count, active low. */
  public static final byte TC = 7;

  public static final byte GND = 8;

  /** Asynchronous master reset, active high. */
  public static final byte MR = 9;

  /** High counts up, low counts down. */
  public static final byte UPDN = 10;

  public static final byte Q1 = 11;
  public static final byte D1 = 12;
  public static final byte D2 = 13;
  public static final byte Q2 = 14;

  /** Rising-edge clock. */
  public static final byte CP = 15;

  public static final byte VCC = 16;

  private static final int WIDTH = 4;
  private static final byte[] OUTPUT_PINS = {Q3, Q0, TC, Q1, Q2};
  private static final String[] PORT_NAMES = {
    "PL (parallel load, active high)",
    "Q3",
    "D3",
    "D0",
    "CE (count enable, active low)",
    "Q0",
    "TC (terminal count, active low)",
    "MR (master reset, active high)",
    "UP/DN (HIGH counts up)",
    "Q1",
    "D1",
    "D2",
    "Q2",
    "CP (clock)"
  };
  private static final String[] PIN_NAMES = {
    "PL", "Q3", "D3", "D0", "CE", "Q0", "TC", null,
    "MR", "U/D", "Q1", "D1", "D2", "Q2", "CP", null
  };

  private static final int BIT_MR = 0;
  private static final int BIT_PL = 1;
  private static final int BIT_CE = 2;
  private static final int BIT_UP = 3;
  private static final int BIT_DATA = 4;
  private static final int BIT_STORED = 8;
  private static final int COUNT_BITS = 12;

  /** Creates a 744516 presettable 4-bit binary up/down counter. */
  public Ttl744516() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl744516HdlGenerator());
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

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var gfx = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, false, false);
    Drawgates.paintPortNamesByPin(painter, x, y, height, PIN_NAMES);
    drawCount(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private static void drawCount(Graphics2D gfx, int x, int y, int height, TtlRegisterData data) {
    if (data == null) {
      return;
    }
    final var value = data.getValue();
    for (var bit = WIDTH - 1; bit >= 0; bit--) {
      final var shown = value.get(bit);
      final var originX = x + 48 + (WIDTH - 1 - bit) * 16;
      gfx.setColor(shown.getColor());
      gfx.fillOval(originX, y + height / 2 - 7, 14, 14);
      gfx.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(gfx, shown.toDisplayString(), originX + 7, y + height / 2);
    }
    gfx.setColor(Color.BLACK);
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
              input(state, PL),
              input(state, CE),
              input(state, UPDN),
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
    state.setPort(
        pinNrToPortNr(TC), resolveTerminal(count, input(state, CE), input(state, UPDN)), DELAY);
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
   * Reset wins over load, and load wins over counting. Reset and load are level sensitive.
   * Counting happens only on a rising edge while {@code CE} is low.
   */
  private static Value resolveCount(Value stored, boolean triggered, Value[] controls) {
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
      if (bitHigh(mask, BIT_MR)) {
        next = 0;
      } else if (bitHigh(mask, BIT_PL)) {
        next = data;
      } else if (triggered && !bitHigh(mask, BIT_CE)) {
        next = bitHigh(mask, BIT_UP) ? (current + 1) & 0xF : (current - 1) & 0xF;
      } else {
        next = current;
      }
      choice.accept(next, errorIn(sources));
    }
    return choice.value();
  }

  /** Terminal count follows the current word, {@code CE} and {@code UP/DN} without a clock. */
  private static Value resolveTerminal(Value count, Value enable, Value direction) {
    final var sources =
        new Value[] {enable, direction, count.get(0), count.get(1), count.get(2), count.get(3)};
    final var choice = new Choice(1);
    for (var mask = 0; mask < (1 << sources.length); mask++) {
      if (!accepts(sources, mask)) {
        continue;
      }
      final var code = (mask >> 2) & 0xF;
      final var countingUp = bitHigh(mask, 1);
      final var terminal =
          !bitHigh(mask, 0) && ((countingUp && code == 0xF) || (!countingUp && code == 0));
      choice.accept(terminal ? 0 : 1, errorIn(sources));
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
}
