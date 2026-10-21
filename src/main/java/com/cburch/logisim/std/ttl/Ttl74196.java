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
 * TTL 74x196: presettable decade ripple counter.
 *
 * <p>Simulation follows the Motorola SN74LS196 and IN74HC196 function tables. {@code MR} low
 * asynchronously clears every output and overrides the other inputs. Otherwise {@code PL} low
 * copies {@code P0} to {@code P3} through to the outputs and overrides the clocks; while it stays
 * low the outputs keep following the data pins. {@code CP0} toggles {@code Q0} on a high-to-low
 * edge. {@code CP1} advances {@code Q1}, {@code Q2} and {@code Q3} through 0, 1, 2, 3 and 4 on a
 * high-to-low edge. {@code Q1} is the least significant bit of that section. The sections are not
 * connected internally: wiring {@code Q0} to {@code CP1} counts BCD 0 through 9, and wiring {@code
 * Q3} to {@code CP0} produces the bi-quinary sequence. Codes 5, 6 and 7 of the divide-by-five
 * section are outside the published table; the next {@code CP1} edge returns that section to 0.
 * Nanosecond ripple delays are not modeled.
 *
 * <p>An unknown or error input changes an output only when the two substitutions disagree. An error
 * on such an input makes the disagreed output an error; an unknown input makes it unknown.
 */
public class Ttl74196 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74196";

  public static final int DELAY = 4;

  /** Asynchronous parallel load, active low. While low, the outputs follow {@code P0} to {@code P3}. */
  public static final int PORT_INDEX_PL = 0;

  public static final int PORT_INDEX_Q2 = 1;
  public static final int PORT_INDEX_P2 = 2;
  public static final int PORT_INDEX_P0 = 3;
  public static final int PORT_INDEX_Q0 = 4;

  /** Falling-edge clock for the divide-by-five section. */
  public static final int PORT_INDEX_CP1 = 5;

  /** Falling-edge clock for the divide-by-two section. */
  public static final int PORT_INDEX_CP0 = 6;

  public static final int PORT_INDEX_Q1 = 7;
  public static final int PORT_INDEX_P1 = 8;
  public static final int PORT_INDEX_P3 = 9;
  public static final int PORT_INDEX_Q3 = 10;

  /** Asynchronous master reset, active low. Overrides load and both clocks. */
  public static final int PORT_INDEX_MR = 11;

  private static final int WIDTH = 4;
  private static final int DIVIDE_BY_FIVE_TERMINAL = 4;
  private static final int CLOCK_0 = 0;
  private static final int CLOCK_1 = 1;
  private static final byte[] OUTPUT_PINS = {2, 5, 9, 12};
  private static final String[] PORT_NAMES = {
    "PL (parallel load, active low)",
    "Q2",
    "P2",
    "P0",
    "Q0",
    "CP1 (divide-by-five clock)",
    "CP0 (divide-by-two clock)",
    "Q1",
    "P1",
    "P3",
    "Q3",
    "MR (master reset, active low)"
  };
  private static final String[] PIN_NAMES = {
    "PL", "Q2", "P2", "P0", "Q0", "CP1", null,
    "CP0", "Q1", "P1", "P3", "Q3", "MR", null
  };

  private static final int BIT_MR = 0;
  private static final int BIT_PL = 1;
  private static final int BIT_P0 = 2;
  private static final int BIT_Q0 = 6;
  private static final int SOURCE_BITS = 10;

  /** Creates a 74196 presettable decade ripple counter. */
  public Ttl74196() {
    super(_ID, (byte) 14, OUTPUT_PINS, PORT_NAMES, new Ttl74196HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var rawState = painter.getData();
    final TtlRegisterData stored = rawState instanceof TtlRegisterData ? (TtlRegisterData) rawState : null;
    g.drawRect((x + 84), (y + height - 28), 12, 14);
    g.drawLine((x + 90), ((y + height - 28)) + 14, (x + 90), (y + height - 7));
    g.drawLine((x + 84), (((y + height - 28)) + 7) - 3, ((x + 84)) + 4, ((y + height - 28)) + 7);
    g.drawLine((x + 84), (((y + height - 28)) + 7) + 3, ((x + 84)) + 4, ((y + height - 28)) + 7);
    structureDot(g, (x + 90), ((y + height - 28)) + 7, stored, 0, 0);
    g.drawLine((x + 130), (y + 7), (x + 130), (y + 13));
    g.drawLine((x + 130), (y + 13), (x + 79), (y + 13));
    g.drawLine((x + 79), (y + 13), (x + 79), ((y + height - 28)) + 7);
    structureBubble(g, (x + 79), ((y + height - 28)) + 7);
    g.drawLine((x + 90), (y + height - 32), (x + 90), (y + height - 32));
    g.drawLine((x + 90), (y + height - 32), (x + 90), ((y + height - 28)) + 0);
    g.drawLine((x + 90), (y + height - 32), (x + 90), (y + height - 28));
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 13));
    g.drawLine((x + 30), (y + 13), (x + 90), (y + 13));
    g.drawLine((x + 90), (y + 13), (x + 90), (y + height - 32));
    structureBubble(g, (x + 30), (y + 7));
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + 30));
    structureBubble(g, (x + 10), (y + height - 7));
    GraphicsUtil.drawCenteredText(g, "LD", (x + 10), (y + 30));
    g.drawLine((x + 70), (y + height - 7), (x + 70), (y + 30));
    GraphicsUtil.drawCenteredText(g, "P0", (x + 70), (y + 30));
    g.drawRect((x + 14), (y + 11), 12, 12);
    g.drawLine((x + 110), (y + 7), (x + 110), (y + 13));
    g.drawLine((x + 110), (y + 13), (x + 20), (y + 13));
    g.drawLine((x + 20), (y + 13), (x + 20), (y + 11));
    g.drawLine((x + 14), (((y + 11)) + 6) - 3, ((x + 14)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 14), (((y + 11)) + 6) + 3, ((x + 14)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 51), (y + 11), 12, 12);
    g.drawLine((x + 30), (y + height - 7), (x + 30), (y + height - 13));
    g.drawLine((x + 30), (y + height - 13), (x + 57), (y + height - 13));
    g.drawLine((x + 57), (y + height - 13), (x + 57), ((y + 11)) + 12);
    g.drawLine((x + 51), (((y + 11)) + 6) - 3, ((x + 51)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 51), (((y + 11)) + 6) + 3, ((x + 51)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 88), (y + 11), 12, 12);
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 13));
    g.drawLine((x + 50), (y + 13), (x + 94), (y + 13));
    g.drawLine((x + 94), (y + 13), (x + 94), (y + 11));
    g.drawLine((x + 88), (((y + 11)) + 6) - 3, ((x + 88)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 88), (((y + 11)) + 6) + 3, ((x + 88)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 26), ((y + 11)) + 6, (x + 47), ((y + 11)) + 6);
    g.drawLine((x + 63), ((y + 11)) + 6, (x + 84), ((y + 11)) + 6);
    g.drawLine((x + 110), (y + height - 7), (x + 110), (y + height - 13));
    g.drawLine((x + 110), (y + height - 13), (x + 9), (y + height - 13));
    g.drawLine((x + 9), (y + height - 13), (x + 9), ((y + 11)) + 6);
    structureBubble(g, (x + 9), ((y + 11)) + 6);
    g.drawLine((x + 20), (y + 27), (x + 94), (y + 27));
    g.drawLine((x + 20), (y + 27), (x + 20), (y + 11));
    g.drawLine((x + 20), (y + 27), (x + 20), ((y + 11)) + 14);
    g.drawLine((x + 57), (y + 27), (x + 57), (y + 11));
    g.drawLine((x + 57), (y + 27), (x + 57), ((y + 11)) + 14);
    g.drawLine((x + 94), (y + 27), (x + 94), (y + 11));
    g.drawLine((x + 94), (y + 27), (x + 94), ((y + 11)) + 14);
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 13));
    g.drawLine((x + 30), (y + 13), (x + 20), (y + 13));
    g.drawLine((x + 20), (y + 13), (x + 20), (y + 27));
    structureBubble(g, (x + 30), (y + 7));
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + 30));
    structureBubble(g, (x + 10), (y + height - 7));
    GraphicsUtil.drawCenteredText(g, "LD", (x + 10), (y + 30));
    g.drawLine((x + 90), (y + 7), (x + 90), (y + 30));
    GraphicsUtil.drawCenteredText(g, "P1", (x + 90), (y + 30));
    g.drawLine((x + 50), (y + height - 7), (x + 50), (y + 30));
    GraphicsUtil.drawCenteredText(g, "P2", (x + 50), (y + 30));
    g.drawLine((x + 70), (y + 7), (x + 70), (y + 30));
    GraphicsUtil.drawCenteredText(g, "P3", (x + 70), (y + 30));
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getState(state);
    final var clock0 =
        data.updateClock(state.getPortValue(PORT_INDEX_CP0), CLOCK_0, StdAttr.TRIG_FALLING);
    final var clock1 =
        data.updateClock(state.getPortValue(PORT_INDEX_CP1), CLOCK_1, StdAttr.TRIG_FALLING);
    final var stored = data.getValue();
    final var count =
        resolve(
            clock0,
            clock1,
            new Value[] {
              state.getPortValue(PORT_INDEX_MR),
              state.getPortValue(PORT_INDEX_PL),
              state.getPortValue(PORT_INDEX_P0),
              state.getPortValue(PORT_INDEX_P1),
              state.getPortValue(PORT_INDEX_P2),
              state.getPortValue(PORT_INDEX_P3),
              stored.get(0),
              stored.get(1),
              stored.get(2),
              stored.get(3)
            });
    data.setValue(count);
    state.setPort(PORT_INDEX_Q0, count.get(0), DELAY);
    state.setPort(PORT_INDEX_Q1, count.get(1), DELAY);
    state.setPort(PORT_INDEX_Q2, count.get(2), DELAY);
    state.setPort(PORT_INDEX_Q3, count.get(3), DELAY);
  }

  private static TtlRegisterData getState(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.create(WIDTH));
      state.setData(data);
    }
    return data;
  }

  /**
   * Reset wins over load, and load wins over counting. Clock edges are consumed even while reset
   * or load is active, so they do not count after the control is released.
   */
  private static Value resolve(boolean clock0, boolean clock1, Value[] sources) {
    final var choice = new Choice(WIDTH);
    for (var mask = 0; mask < (1 << SOURCE_BITS); mask++) {
      if (!accepts(sources, mask)) {
        continue;
      }
      choice.accept(nextCode(mask, clock0, clock1), errorIn(sources));
    }
    return choice.value();
  }

  /** Next word for one fully defined substitution of the controls and the stored word. */
  private static int nextCode(int mask, boolean clock0, boolean clock1) {
    if (!bitHigh(mask, BIT_MR)) {
      return 0;
    }
    if (!bitHigh(mask, BIT_PL)) {
      return (mask >> BIT_P0) & 0xF;
    }
    var code = (mask >> BIT_Q0) & 0xF;
    if (clock0) {
      code ^= 1;
    }
    if (clock1) {
      final var section = (code >> 1) & 7;
      final var advanced = section >= DIVIDE_BY_FIVE_TERMINAL ? 0 : section + 1;
      code = (code & 1) | (advanced << 1);
    }
    return code;
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
    return new int[] {PORT_INDEX_CP1, PORT_INDEX_CP0};
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
