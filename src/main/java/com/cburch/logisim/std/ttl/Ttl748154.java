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
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Graphics2D;

/**
 * TTL 748154: dual 16-bit binary counters with 3-state output registers.
 *
 * <p>There is no 74HC8154. Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74lv8154.pdf">SN74LV8154</a> data sheet
 * (SCLS589B). {@code CLKA}, {@code CLKB} and {@code RCLK} are rising-edge clocks. {@code CCLR}
 * low asynchronously clears both counters and leaves the storage register unchanged. {@code
 * CLKBEN} low lets counter B count on the next {@code CLKB} edge. {@code RCOA} is active low while
 * counter A holds {@code FFFF}. One low gate among {@code GAL}, {@code GAU}, {@code GBL} and
 * {@code GBU} drives that stored byte onto {@code Y7}..{@code Y0}; all four gates high release the
 * bus. The data sheet does not define more than one gate low: bits that agree are driven, and bits
 * that disagree are an error. In one propagation step the clear is applied first, then count
 * edges, then {@code RCLK} copies the updated counters. Nanosecond delays are not modeled.
 *
 * <p>No HDL generator is provided. The exporter gives every 1-bit clock the same tick name, so
 * three independent clocks cannot be described without changing the clock-port width.
 */
public class Ttl748154 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "748154";

  public static final int DELAY = 1;

  public static final int PORT_CLKA = 0;
  public static final int PORT_CLKB = 1;
  public static final int PORT_GAL = 2;
  public static final int PORT_GAU = 3;
  public static final int PORT_GBL = 4;
  public static final int PORT_GBU = 5;
  public static final int PORT_RCLK = 6;
  public static final int PORT_RCOA = 7;
  public static final int PORT_CLKBEN = 8;
  public static final int PORT_CCLR = 9;
  public static final int PORT_Y7 = 10;
  public static final int PORT_Y6 = 11;
  public static final int PORT_Y5 = 12;
  public static final int PORT_Y4 = 13;
  public static final int PORT_Y3 = 14;
  public static final int PORT_Y2 = 15;
  public static final int PORT_Y1 = 16;
  public static final int PORT_Y0 = 17;

  static final int WORD_COUNTER_A = 0;
  static final int WORD_COUNTER_B = 1;
  static final int WORD_STORE_A = 2;
  static final int WORD_STORE_B = 3;

  private static final int WORD_COUNT = 4;
  private static final int WORD_BITS = 16;
  private static final int BYTE_BITS = 8;
  private static final int FULL_COUNT = 0xFFFF;
  private static final int BODY_CENTER = 100;
  private static final BitWidth WIDTH = BitWidth.create(WORD_BITS);
  private static final BitWidth BYTE = BitWidth.create(BYTE_BITS);
  private static final Value ZERO = Value.createKnown(WIDTH, 0);
  private static final int CLOCK_A = 0;
  private static final int CLOCK_B = 1;
  private static final int CLOCK_REGISTER = 2;
  private static final byte[] OUTPUT_PINS = {8, 12, 13, 14, 15, 16, 17, 18, 19};
  private static final String[] PORT_NAMES = {
    "CLKA (counter A clock)",
    "CLKB (counter B clock)",
    "GAL (A low byte, active low)",
    "GAU (A high byte, active low)",
    "GBL (B low byte, active low)",
    "GBU (B high byte, active low)",
    "RCLK (storage register clock)",
    "RCOA (counter A carry, active low)",
    "CLKBEN (counter B enable, active low)",
    "CCLR (clear both counters, active low)",
    "Y7",
    "Y6",
    "Y5",
    "Y4",
    "Y3",
    "Y2",
    "Y1",
    "Y0"
  };
  private static final String[] PIN_LABELS = {
    "CLKA", "CLKB", "GAL", "GAU", "GBL", "GBU", "RCLK", "RCOA", "BEN", null,
    "CLR", "Y7", "Y6", "Y5", "Y4", "Y3", "Y2", "Y1", "Y0", null
  };

  /** Creates a 748154 dual 16-bit counter with a shared 3-state storage register. */
  public Ttl748154() {
    super(_ID, (byte) 20, OUTPUT_PINS, PORT_NAMES, null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var gfx = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, false, false);
    Drawgates.paintPortNamesByPin(painter, x, y, height, PIN_LABELS);
    drawState(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private static void drawState(Graphics2D gfx, int x, int y, int height, TtlRegisterData state) {
    if (state == null) return;
    final var center = y + height / 2;
    GraphicsUtil.drawCenteredText(
        gfx,
        "A " + hex(state.getValue(WORD_COUNTER_A)) + "  B " + hex(state.getValue(WORD_COUNTER_B)),
        x + BODY_CENTER,
        center - BYTE_BITS);
    GraphicsUtil.drawCenteredText(
        gfx,
        "SA "
            + hex(state.getValue(WORD_STORE_A))
            + "  SB "
            + hex(state.getValue(WORD_STORE_B)),
        x + BODY_CENTER,
        center + BYTE_BITS);
  }

  private static String hex(Value word) {
    if (word.isFullyDefined()) return String.format("%04X", word.toLongValue());
    return word.toHexString();
  }

  private static TtlRegisterData getState(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WIDTH, WORD_COUNT);
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getState(state);
    final var clear = state.getPortValue(PORT_CCLR);
    final var countA = data.updateClock(state.getPortValue(PORT_CLKA), CLOCK_A);
    final var countB = data.updateClock(state.getPortValue(PORT_CLKB), CLOCK_B);
    final var capture = data.updateClock(state.getPortValue(PORT_RCLK), CLOCK_REGISTER);

    final var nextA = afterClear(clear, data.getValue(WORD_COUNTER_A), countA);
    final var countedB =
        enabledCount(data.getValue(WORD_COUNTER_B), countB, state.getPortValue(PORT_CLKBEN));
    final var nextB = selectActiveLow(clear, ZERO, countedB);
    data.setValue(WORD_COUNTER_A, nextA);
    data.setValue(WORD_COUNTER_B, nextB);
    if (capture) {
      data.setValue(WORD_STORE_A, nextA);
      data.setValue(WORD_STORE_B, nextB);
    }

    state.setPort(PORT_RCOA, rippleCarry(nextA), DELAY);
    driveBus(
        state,
        state.getPortValue(PORT_GAL),
        state.getPortValue(PORT_GAU),
        state.getPortValue(PORT_GBL),
        state.getPortValue(PORT_GBU),
        data.getValue(WORD_STORE_A),
        data.getValue(WORD_STORE_B));
  }

  /**
   * Applies an active-low clear, or a rising edge when the clear is inactive. An unknown or error
   * clear keeps the word only when both choices are the same bit.
   */
  private static Value afterClear(Value clear, Value current, boolean clockEdge) {
    final var counted = clockEdge ? increment(current) : current;
    return selectActiveLow(clear, ZERO, counted);
  }

  /** Counter B advances on its rising edge only while {@code CLKBEN} is low. */
  private static Value enabledCount(Value current, boolean clockEdge, Value enable) {
    if (!clockEdge) return current;
    return selectActiveLow(enable, increment(current), current);
  }

  private static Value selectActiveLow(Value control, Value whenLow, Value whenHigh) {
    if (control == Value.FALSE) return whenLow;
    if (control == Value.TRUE) return whenHigh;
    return combine(whenLow, whenHigh, control == Value.ERROR);
  }

  private static Value increment(Value current) {
    if (contains(current, Value.ERROR)) return Value.createError(WIDTH);
    if (!current.isFullyDefined()) return Value.createUnknown(WIDTH);
    return Value.createKnown(WIDTH, (current.toLongValue() + 1) & FULL_COUNT);
  }

  /** Active low when counter A is {@code FFFF}. Any bad bit makes the carry bad too. */
  private static Value rippleCarry(Value counter) {
    if (contains(counter, Value.ERROR)) return Value.ERROR;
    if (!counter.isFullyDefined()) return Value.UNKNOWN;
    return counter.toLongValue() == FULL_COUNT ? Value.FALSE : Value.TRUE;
  }

  private static void driveBus(
      InstanceState state,
      Value gal,
      Value gau,
      Value gbl,
      Value gbu,
      Value storedA,
      Value storedB) {
    final var gates = new Value[] {gal, gau, gbl, gbu};
    for (final var gate : gates) {
      if (gate == Value.ERROR) {
        driveByte(state, Value.createError(BYTE));
        return;
      }
      if (gate != Value.TRUE && gate != Value.FALSE) {
        driveByte(state, Value.createUnknown(BYTE));
        return;
      }
    }
    final var bytes =
        new Value[] {
          byteOf(storedA, 0),
          byteOf(storedA, BYTE_BITS),
          byteOf(storedB, 0),
          byteOf(storedB, BYTE_BITS)
        };
    Value selected = null;
    for (var index = 0; index < gates.length; index++) {
      if (gates[index] != Value.FALSE) continue;
      selected = selected == null ? bytes[index] : combine(selected, bytes[index], true);
    }
    driveByte(state, selected == null ? Value.createUnknown(BYTE) : selected);
  }

  private static Value byteOf(Value word, int shift) {
    final var bits = new Value[BYTE_BITS];
    for (var bit = 0; bit < BYTE_BITS; bit++) {
      bits[bit] = word.get(shift + bit);
    }
    return Value.create(bits);
  }

  private static void driveByte(InstanceState state, Value bits) {
    for (var bit = 0; bit < BYTE_BITS; bit++) {
      state.setPort(PORT_Y0 - bit, bits.get(bit), DELAY);
    }
  }

  /**
   * Merges two words bit by bit. Equal bits pass through. A known disagreement is an error when
   * {@code conflictIsError} is set, and unknown otherwise. An error bit forces an error.
   */
  private static Value combine(Value left, Value right, boolean conflictIsError) {
    final var bits = new Value[left.getWidth()];
    for (var index = 0; index < bits.length; index++) {
      final var first = left.get(index);
      final var second = right.get(index);
      if (first == second) bits[index] = first;
      else if (first == Value.ERROR || second == Value.ERROR) bits[index] = Value.ERROR;
      else if (conflictIsError && first != Value.UNKNOWN && second != Value.UNKNOWN) {
        bits[index] = Value.ERROR;
      } else bits[index] = Value.UNKNOWN;
    }
    return Value.create(bits);
  }

  private static boolean contains(Value word, Value bit) {
    for (var index = 0; index < word.getWidth(); index++) {
      if (word.get(index) == bit) return true;
    }
    return false;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_CLKA, PORT_CLKB, PORT_RCLK};
  }
}
