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
import java.util.function.IntUnaryOperator;

/**
 * TTL 74x40102: 8-bit synchronous BCD down counter.
 *
 * <p>Simulation follows the Philips 74HC/HCT40102 function table (December 1990). {@code MR} low
 * asynchronously loads the maximum count, decimal 99. {@code PL} low asynchronously and
 * transparently jams {@code P0} to {@code P7}. On the rising edge of {@code CP}, with both of
 * those high, {@code PE} low loads the same jam inputs even if {@code TE} is high. Otherwise
 * {@code TE} low counts down and {@code TE} high holds. {@code TC} is combinational and active
 * low: it is low only while the code is zero and {@code TE} is low. During continuous counting
 * that zero state lasts one clock period, because the next rising edge leaves it for 99. The
 * level of {@code CP} does not gate {@code TC}. Nanosecond delays are not modeled.
 *
 * <p>The counter is two cascaded decades, units in {@code P3} to {@code P0} and tens in {@code P7}
 * to {@code P4}. A non-zero nibble decrements. A zero nibble becomes 9 and borrows. From 00 the
 * next clock is 99. Philips publishes no illegal-code diagram; a nibble of 10 to 15 uses the same
 * rule, so 10 becomes 9 on the next clock.
 *
 * <p>There are no parallel {@code Q} pins. The code is instance state and is drawn when the
 * internal structure is shown. An unknown or error input changes a state bit or {@code TC} only
 * when the two substitutions disagree. An error on such an input makes the disagreed bit an error;
 * an unknown input makes it unknown.
 */
public class Ttl7440102 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "7440102";

  public static final int DELAY = 1;

  /** Rising-edge clock. */
  public static final byte CP = 1;

  /** Asynchronous master reset, active low. Clears to decimal 99. */
  public static final byte MR = 2;

  /** Terminal enable, active low. Also gates {@link #TC}. */
  public static final byte TE = 3;

  public static final byte P0 = 4;
  public static final byte P1 = 5;
  public static final byte P2 = 6;
  public static final byte P3 = 7;

  public static final byte GND = 8;

  /** Asynchronous preset, active low. Jam inputs stay transparent while this pin is low. */
  public static final byte PL = 9;

  public static final byte P4 = 10;
  public static final byte P5 = 11;
  public static final byte P6 = 12;
  public static final byte P7 = 13;

  /** Terminal count, active low. Low only at code 0 while {@link #TE} is low. */
  public static final byte TC = 14;

  /** Synchronous preset, active low. Sampled on the rising edge of {@link #CP}. */
  public static final byte PE = 15;

  public static final byte VCC = 16;

  static final int WIDTH = 8;
  static final int MAX_COUNT = 0x99;

  private static final int NIBBLE = 4;
  private static final int NIBBLE_MASK = 0xF;
  private static final int BCD_NINE = 9;
  private static final int CODE_MASK = 0xFF;
  static final BitWidth CODE_WIDTH = BitWidth.create(WIDTH);
  private static final byte[] OUTPUT_PINS = {TC};
  static final byte[] JAM_PINS = {P0, P1, P2, P3, P4, P5, P6, P7};
  private static final String[] PORT_NAMES = {
    "CP (clock)",
    "MR (asynchronous reset to 99, active low)",
    "TE (terminal enable, active low)",
    "P0",
    "P1",
    "P2",
    "P3",
    "PL (asynchronous preset, active low)",
    "P4",
    "P5",
    "P6",
    "P7",
    "TC (terminal count, active low)",
    "PE (synchronous preset, active low)"
  };
  private static final String[] PIN_NAMES = {
    "CP", "MR", "TE", "P0", "P1", "P2", "P3", null,
    "PL", "P4", "P5", "P6", "P7", "TC", "PE", null
  };

  private static final int IDX_MR = 0;
  private static final int IDX_PL = 1;
  private static final int IDX_PE = 2;
  private static final int IDX_TE = 3;
  private static final int IDX_JAM_SYNC = 4;
  private static final int IDX_STORED_SYNC = 12;
  private static final int IDX_JAM_ASYNC = 2;
  private static final int IDX_STORED_ASYNC = 10;

  /** Creates a 7440102 8-bit synchronous BCD down counter. */
  public Ttl7440102() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl7440102HdlGenerator());
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

  /** Next counted code. Each nibble decrements, and a zero nibble wraps to 9 and borrows. */
  static int bcdDown(int code) {
    final var units = code & NIBBLE_MASK;
    final var tens = (code >> NIBBLE) & NIBBLE_MASK;
    final var nextUnits = units == 0 ? BCD_NINE : units - 1;
    final var nextTens = units != 0 ? tens : (tens == 0 ? BCD_NINE : tens - 1);
    return (nextTens << NIBBLE) | nextUnits;
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
      final var slot = WIDTH - 1 - bit;
      final var gap = bit < NIBBLE ? 8 : 0;
      final var originX = x + 14 + slot * 15 + gap;
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
    final var jam = new Value[WIDTH];
    for (var bit = 0; bit < WIDTH; bit++) {
      jam[bit] = input(state, JAM_PINS[bit]);
    }
    final var count =
        resolveCount(
            data.getValue(),
            triggered,
            input(state, MR),
            input(state, PL),
            input(state, PE),
            input(state, TE),
            jam);
    data.setValue(count);
    state.setPort(pinNrToPortNr(TC), resolveTerminal(count, input(state, TE)), DELAY);
  }

  private static TtlRegisterData getState(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(CODE_WIDTH);
      state.setData(data);
    }
    return data;
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }

  /**
   * {@code MR} wins over {@code PL}, and {@code PL} wins over the clock. Preset and counting are
   * sampled only on a rising edge.
   */
  private static Value resolveCount(
      Value stored,
      boolean triggered,
      Value masterReset,
      Value asyncPreset,
      Value syncPreset,
      Value terminalEnable,
      Value[] jam) {
    final var sources = new Value[triggered ? IDX_STORED_SYNC + WIDTH : IDX_STORED_ASYNC + WIDTH];
    sources[IDX_MR] = masterReset;
    sources[IDX_PL] = asyncPreset;
    if (triggered) {
      sources[IDX_PE] = syncPreset;
      sources[IDX_TE] = terminalEnable;
    }
    final var jamAt = triggered ? IDX_JAM_SYNC : IDX_JAM_ASYNC;
    final var storedAt = triggered ? IDX_STORED_SYNC : IDX_STORED_ASYNC;
    for (var bit = 0; bit < WIDTH; bit++) {
      sources[jamAt + bit] = jam[bit];
      sources[storedAt + bit] = stored.get(bit);
    }
    return substitute(sources, WIDTH, mask -> nextCount(mask, triggered));
  }

  private static int nextCount(int mask, boolean triggered) {
    if (!bitHigh(mask, IDX_MR)) {
      return MAX_COUNT;
    }
    final var jamAt = triggered ? IDX_JAM_SYNC : IDX_JAM_ASYNC;
    final var storedAt = triggered ? IDX_STORED_SYNC : IDX_STORED_ASYNC;
    final var jam = (mask >> jamAt) & CODE_MASK;
    if (!bitHigh(mask, IDX_PL)) {
      return jam;
    }
    final var stored = (mask >> storedAt) & CODE_MASK;
    if (!triggered) {
      return stored;
    }
    if (!bitHigh(mask, IDX_PE)) {
      return jam;
    }
    if (!bitHigh(mask, IDX_TE)) {
      return bcdDown(stored);
    }
    return stored;
  }

  /** Terminal count follows the current word and {@code TE} without waiting for a clock. */
  private static Value resolveTerminal(Value count, Value terminalEnable) {
    final var sources = new Value[WIDTH + 1];
    sources[0] = terminalEnable;
    for (var bit = 0; bit < WIDTH; bit++) {
      sources[bit + 1] = count.get(bit);
    }
    return substitute(
        sources,
        1,
        mask -> {
          final var countingEnabled = !bitHigh(mask, 0);
          final var code = (mask >> 1) & CODE_MASK;
          return countingEnabled && code == 0 ? 0 : 1;
        });
  }

  /**
   * Tries every value of the unknown or error bits. Defined bits stay fixed, so a fully driven
   * counter is resolved in one pass.
   */
  private static Value substitute(Value[] sources, int width, IntUnaryOperator next) {
    var fixed = 0;
    final var floating = new int[sources.length];
    var floatCount = 0;
    var sawError = false;
    for (var index = 0; index < sources.length; index++) {
      final var bit = sources[index];
      if (bit == Value.TRUE) {
        fixed |= 1 << index;
      } else if (bit != Value.FALSE) {
        floating[floatCount++] = index;
        if (bit == Value.ERROR) {
          sawError = true;
        }
      }
    }
    final var choice = new Choice(width);
    final var variants = 1 << floatCount;
    for (var variant = 0; variant < variants; variant++) {
      var mask = fixed;
      for (var index = 0; index < floatCount; index++) {
        if ((variant & (1 << index)) != 0) {
          mask |= 1 << floating[index];
        }
      }
      choice.accept(next.applyAsInt(mask), sawError);
    }
    return choice.value();
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
