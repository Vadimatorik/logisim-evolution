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
 * TTL 74x176: presettable decade counter/latch.
 *
 * <p>There is no Nexperia or TI 74HC176 data sheet. Simulation follows the NTE74176 pinout and
 * function, which match the Motorola SN74LS196 mode table for the same DIP-14:
 * <a href="https://www.farnell.com/datasheets/2044559.pdf">NTE74176</a>. {@code CLR} low
 * asynchronously clears every output and overrides load and both clocks. Otherwise {@code LOAD}
 * low makes the outputs follow {@code A} to {@code D} asynchronously. Otherwise a high-to-low edge
 * on {@code CLK1} toggles {@code QA}, and a high-to-low edge on {@code CLK2} advances {@code QB},
 * {@code QC} and {@code QD} through 0, 1, 2, 3 and 4. {@code QB} is the least significant bit of
 * that section. Codes 5, 6 and 7 are outside the published table; the next {@code CLK2} edge
 * returns the section to 0. The two sections are not connected inside the package. Nanosecond
 * ripple delay is not modeled.
 *
 * <p>An unknown or error input changes an output only when the two substitutions disagree. An error
 * on such an input makes the disagreed output an error; an unknown input makes it unknown.
 */
public class Ttl74176 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74176";

  public static final int DELAY = 1;

  /** Count/load, active low. Low loads {@code A} to {@code D} and overrides the clocks. */
  public static final byte LOAD = 1;

  public static final byte QC = 2;
  public static final byte C = 3;
  public static final byte A = 4;
  public static final byte QA = 5;

  /** Falling-edge clock for the divide-by-five section. */
  public static final byte CLK2 = 6;

  public static final byte GND = 7;

  /** Falling-edge clock for the divide-by-two section. */
  public static final byte CLK1 = 8;

  public static final byte QB = 9;
  public static final byte B = 10;
  public static final byte D = 11;
  public static final byte QD = 12;

  /** Asynchronous clear, active low. Overrides load and both clocks. */
  public static final byte CLR = 13;

  public static final byte VCC = 14;

  private static final int WIDTH_BITS = 4;
  private static final BitWidth WIDTH = BitWidth.create(WIDTH_BITS);
  private static final int DIVIDE_BY_FIVE_TERMINAL = 4;
  private static final byte[] OUTPUT_PINS = {QC, QA, QB, QD};
  private static final String[] PORT_NAMES = {
    "LOAD (count/load, active LOW)",
    "QC",
    "C",
    "A",
    "QA",
    "CLK2 (clock for divide-by-five)",
    "CLK1 (clock for divide-by-two)",
    "QB",
    "B",
    "D",
    "QD",
    "CLR (clear, active LOW)"
  };
  private static final String[] PIN_NAMES = {
    "LOAD", "QC", "C", "A", "QA", "CLK2", null,
    "CLK1", "QB", "B", "D", "QD", "CLR", null
  };

  private static final int BIT_CLR = 0;
  private static final int BIT_LOAD = 1;
  private static final int BIT_A = 2;
  private static final int BIT_B = 3;
  private static final int BIT_C = 4;
  private static final int BIT_D = 5;
  private static final int BIT_STORED_QA = 6;
  private static final int BIT_STORED_QB = 7;
  private static final int BIT_STORED_QC = 8;
  private static final int BIT_STORED_QD = 9;
  private static final int SOURCE_BITS = 10;
  private static final int CLOCK_DIV2 = 0;
  private static final int CLOCK_DIV5 = 1;

  /** Creates a 74176 presettable decade counter/latch. */
  public Ttl74176() {
    super(_ID, (byte) 14, OUTPUT_PINS, PORT_NAMES, new Ttl74176HdlGenerator());
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
    for (var bit = WIDTH_BITS - 1; bit >= 0; bit--) {
      final var shown = value.get(bit);
      final var originX = x + 36 + (WIDTH_BITS - 1 - bit) * 22;
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
    final var fallDiv2 = data.updateClock(input(state, CLK1), CLOCK_DIV2, StdAttr.TRIG_FALLING);
    final var fallDiv5 = data.updateClock(input(state, CLK2), CLOCK_DIV5, StdAttr.TRIG_FALLING);
    final var count =
        resolveCount(
            data.getValue(),
            fallDiv2,
            fallDiv5,
            input(state, CLR),
            input(state, LOAD),
            input(state, A),
            input(state, B),
            input(state, C),
            input(state, D));
    data.setValue(count);
    state.setPort(pinNrToPortNr(QA), count.get(0), DELAY);
    state.setPort(pinNrToPortNr(QB), count.get(1), DELAY);
    state.setPort(pinNrToPortNr(QC), count.get(2), DELAY);
    state.setPort(pinNrToPortNr(QD), count.get(3), DELAY);
  }

  private static TtlRegisterData getState(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WIDTH);
      state.setData(data);
    }
    return data;
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }

  /**
   * Clear wins over load, and load wins over counting. Clock edges are exact high-to-low
   * transitions; an unknown clock level is not an edge.
   */
  private static Value resolveCount(
      Value stored,
      boolean fallDiv2,
      boolean fallDiv5,
      Value clear,
      Value load,
      Value a,
      Value b,
      Value c,
      Value d) {
    final var sources = new Value[SOURCE_BITS];
    sources[BIT_CLR] = clear;
    sources[BIT_LOAD] = load;
    sources[BIT_A] = a;
    sources[BIT_B] = b;
    sources[BIT_C] = c;
    sources[BIT_D] = d;
    sources[BIT_STORED_QA] = stored.get(0);
    sources[BIT_STORED_QB] = stored.get(1);
    sources[BIT_STORED_QC] = stored.get(2);
    sources[BIT_STORED_QD] = stored.get(3);
    final var choice = new Choice(WIDTH_BITS);
    final var sawError = contains(sources, Value.ERROR);
    for (var mask = 0; mask < (1 << SOURCE_BITS); mask++) {
      if (!accepts(sources, mask)) {
        continue;
      }
      choice.accept(nextWord(mask, fallDiv2, fallDiv5), sawError);
    }
    return choice.value();
  }

  private static int nextWord(int mask, boolean fallDiv2, boolean fallDiv5) {
    if (!bitHigh(mask, BIT_CLR)) {
      return 0;
    }
    if (!bitHigh(mask, BIT_LOAD)) {
      return (bitHigh(mask, BIT_D) ? 8 : 0)
          + (bitHigh(mask, BIT_C) ? 4 : 0)
          + (bitHigh(mask, BIT_B) ? 2 : 0)
          + (bitHigh(mask, BIT_A) ? 1 : 0);
    }
    var qa = bitHigh(mask, BIT_STORED_QA) ? 1 : 0;
    var section =
        (bitHigh(mask, BIT_STORED_QD) ? 4 : 0)
            + (bitHigh(mask, BIT_STORED_QC) ? 2 : 0)
            + (bitHigh(mask, BIT_STORED_QB) ? 1 : 0);
    if (fallDiv2) {
      qa ^= 1;
    }
    if (fallDiv5) {
      section = section >= DIVIDE_BY_FIVE_TERMINAL ? 0 : section + 1;
    }
    return qa + (section << 1);
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

  private static boolean contains(Value[] sources, Value bit) {
    for (final var source : sources) {
      if (source == bit) {
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
    return new int[] {pinNrToPortNr(CLK1), pinNrToPortNr(CLK2)};
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
