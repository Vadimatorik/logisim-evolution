/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Arrays;

/**
 * TTL 74x598: 8-bit shift register with input latches and 3-state parallel I/O.
 *
 * <p>There is no 74HC598. Simulation follows the TI SN74LS598 in the March 1988 TTL data book
 * (pages 2-999 to 2-1006): an 8-bit storage latch feeds an 8-bit shift register, {@code RCK} and
 * {@code SRCK} are rising-edge clocks, {@code SRCLR} asynchronously clears only the shift
 * register, and {@code SRLOAD} copies the latch into the shift register for as long as it stays
 * low. Parallel pins {@code A/QA} through {@code H/QH} are inputs while {@code G} is high and
 * shift-register outputs while {@code G} is low. {@code QH'} is the true {@code QH} bit and stays
 * driven. {@code DS} low selects {@code SER0}; {@code DS} high selects {@code SER1}. Serial data
 * enters at {@code A} and shifts toward {@code H}.
 *
 * <p>Philips 74F598 ignores the load while the shift clock is high. This model follows TI, so a
 * low {@code SRLOAD} loads even when {@code SRCK} is high. Asserting {@code SRCLR} and {@code
 * SRLOAD} together makes the shift register unknown: the 597 function table calls that
 * combination invalid, and the LS598 diagram drives reset and load into the same flip-flop.
 * Nanosecond delays are not modeled. There is no HDL model: the part has two clocks, a
 * level-sensitive load, and eight three-state pins.
 *
 * <p>An unknown or error input changes a result only when the two substitutions disagree. An error
 * on such an input makes the disagreed bit an error; an unknown input makes it unknown.
 */
public class Ttl74598 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74598";

  public static final int DELAY = 1;

  public static final byte A = 1;
  public static final byte B = 2;
  public static final byte C = 3;
  public static final byte D = 4;
  public static final byte E = 5;
  public static final byte F = 6;

  /** Datasheet pin {@code G/QG}. */
  public static final byte GQ = 7;

  public static final byte H = 8;

  /** Active low. Level-sensitive copy from the storage latch into the shift register. */
  public static final byte SRLOAD = 9;

  public static final byte GND = 10;

  /** True serial output of {@code H}. Push-pull, independent of {@link #OE}. */
  public static final byte QH_PRIME = 11;

  /** Active low asynchronous clear of the shift register only. */
  public static final byte SRCLR = 12;

  /** Rising-edge shift clock. */
  public static final byte SRCK = 13;

  /** Active low shift-clock enable. */
  public static final byte SRCKEN = 14;

  /** Rising-edge storage-latch clock. */
  public static final byte RCK = 15;

  /** Datasheet pin {@code G}. Active-low output enable for the parallel pins. */
  public static final byte OE = 16;

  public static final byte SER1 = 17;
  public static final byte SER0 = 18;

  /** Low selects {@link #SER0}. High selects {@link #SER1}. */
  public static final byte DS = 19;

  public static final byte VCC = 20;

  private static final int WIDTH = 8;
  private static final int BIT_CLR = 0;
  private static final int BIT_LOAD = 1;
  private static final int BIT_EN = 2;
  private static final int BIT_DS = 3;
  private static final int BIT_SER0 = 4;
  private static final int BIT_SER1 = 5;
  private static final int BIT_STORE = 6;
  private static final int SOURCE_COUNT = BIT_STORE + WIDTH;
  private static final int CLOCK_RCK = 0;
  private static final int CLOCK_SRCK = 1;
  private static final byte[] PARALLEL = {A, B, C, D, E, F, GQ, H};
  private static final byte[] OUTPUT_PINS = {QH_PRIME};
  private static final byte[] INOUT_PINS = PARALLEL;
  private static final String[] PORT_NAMES = {
    "A/QA",
    "B/QB",
    "C/QC",
    "D/QD",
    "E/QE",
    "F/QF",
    "G/QG",
    "H/QH",
    "SRLOAD (active low)",
    "QH'",
    "SRCLR (active low)",
    "SRCK",
    "SRCKEN (active low)",
    "RCK",
    "G (output enable, active low)",
    "SER1",
    "SER0",
    "DS"
  };
  private static final String[] PIN_NAMES = {
    "A", "B", "C", "D", "E", "F", "G", "H", "LOAD", null,
    "QH'", "CLR", "SCK", "EN", "RCK", "OE", "S1", "S0", "DS", null
  };

  /** Creates a 74598 8-bit shift register with input latches. */
  public Ttl74598() {
    super(_ID, (byte) 20, OUTPUT_PINS, new byte[] {}, INOUT_PINS, PORT_NAMES, null);
  }

  /**
   * Logical port index for a datasheet pin. Power pins are omitted, so pins after {@link #GND}
   * shift down by two.
   */
  public static int pinNrToPortNr(byte pin) {
    return pin < GND ? pin - 1 : pin - 2;
  }

  @Override
  public void propagate(InstanceState state) {
    if (state.getAttributeValue(TtlLibrary.VCC_GND)) {
      // Shown power appends GND and then VCC after the signal ports.
      final var groundPort = pinNumber - 2;
      final var supplyPort = pinNumber - 1;
      if (state.getPortValue(groundPort) != Value.FALSE
          || state.getPortValue(supplyPort) != Value.TRUE) {
        releaseOutputs(state);
        return;
      }
    }
    propagateTtl(state);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getState(state);
    final var parallel = new Value[WIDTH];
    for (var index = 0; index < WIDTH; index++) {
      parallel[index] = state.getPortValue(pinNrToPortNr(PARALLEL[index]));
    }
    final var storageClock = data.updateClock(state.getPortValue(pinNrToPortNr(RCK)), CLOCK_RCK);
    final var shiftClock =
        data.updateClock(state.getPortValue(pinNrToPortNr(SRCK)), CLOCK_SRCK, StdAttr.TRIG_RISING);
    if (storageClock) {
      System.arraycopy(parallel, 0, data.storage, 0, WIDTH);
    }
    data.shift =
        resolveShift(
            data.storage,
            data.shift,
            shiftClock,
            state.getPortValue(pinNrToPortNr(SRCLR)),
            state.getPortValue(pinNrToPortNr(SRLOAD)),
            state.getPortValue(pinNrToPortNr(SRCKEN)),
            state.getPortValue(pinNrToPortNr(DS)),
            state.getPortValue(pinNrToPortNr(SER0)),
            state.getPortValue(pinNrToPortNr(SER1)));
    driveOutputs(state, data.shift, state.getPortValue(pinNrToPortNr(OE)));
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var gfx = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, false, false);
    Drawgates.paintPortNamesByPin(painter, x, y, height, PIN_NAMES);
    final var data = (State) painter.getData();
    if (data == null) return;
    drawWord(gfx, x, y, height, data.storage, -8);
    drawWord(gfx, x, y, height, data.shift, 8);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {pinNrToPortNr(RCK), pinNrToPortNr(SRCK)};
  }

  private static void releaseOutputs(InstanceState state) {
    for (var index = 0; index < WIDTH; index++) {
      state.setPort(pinNrToPortNr(PARALLEL[index]), Value.UNKNOWN, DELAY);
    }
    state.setPort(pinNrToPortNr(QH_PRIME), Value.UNKNOWN, DELAY);
  }

  private static void driveOutputs(InstanceState state, Value[] shift, Value outputEnable) {
    state.setPort(pinNrToPortNr(QH_PRIME), shift[WIDTH - 1], DELAY);
    for (var index = 0; index < WIDTH; index++) {
      state.setPort(pinNrToPortNr(PARALLEL[index]), parallelBit(shift[index], outputEnable), DELAY);
    }
  }

  /** {@code G} low drives the shift bit. {@code G} high releases the pin. */
  private static Value parallelBit(Value driven, Value outputEnable) {
    if (outputEnable == Value.FALSE) return driven;
    if (outputEnable == Value.TRUE) return Value.UNKNOWN;
    final var choice = new BitChoice(1);
    final var error = outputEnable == Value.ERROR;
    choice.accept(new Value[] {driven}, error);
    choice.accept(new Value[] {Value.UNKNOWN}, error);
    return choice.value()[0];
  }

  private static Value[] resolveShift(
      Value[] storage,
      Value[] shift,
      boolean shiftClock,
      Value clearPin,
      Value loadPin,
      Value enablePin,
      Value select,
      Value serial0,
      Value serial1) {
    final var sources = new Value[SOURCE_COUNT];
    sources[BIT_CLR] = clearPin;
    sources[BIT_LOAD] = loadPin;
    sources[BIT_EN] = enablePin;
    sources[BIT_DS] = select;
    sources[BIT_SER0] = serial0;
    sources[BIT_SER1] = serial1;
    System.arraycopy(storage, 0, sources, BIT_STORE, WIDTH);
    final var choice = new BitChoice(WIDTH);
    final var error = contains(sources, Value.ERROR);
    for (var mask = 0; mask < (1 << SOURCE_COUNT); mask++) {
      if (!accepts(sources, mask)) continue;
      choice.accept(apply(mask, shiftClock, shift), error);
    }
    return choice.value();
  }

  /**
   * Clear wins over a lone load. Both active is the invalid 597 combination and yields unknown.
   * Otherwise a low load copies the latch. Otherwise a rising {@code SRCK} with {@code SRCKEN} low
   * shifts toward {@code H}.
   */
  private static Value[] apply(int mask, boolean shiftClock, Value[] shift) {
    final var clear = !bitHigh(mask, BIT_CLR);
    final var load = !bitHigh(mask, BIT_LOAD);
    if (clear && load) return filled(Value.UNKNOWN);
    if (clear) return filled(Value.FALSE);
    if (load) {
      final var loaded = new Value[WIDTH];
      for (var index = 0; index < WIDTH; index++) {
        loaded[index] = bitHigh(mask, BIT_STORE + index) ? Value.TRUE : Value.FALSE;
      }
      return loaded;
    }
    if (!(shiftClock && !bitHigh(mask, BIT_EN))) return shift.clone();
    final var shifted = new Value[WIDTH];
    shifted[0] = bitHigh(mask, BIT_DS) ? bit(mask, BIT_SER1) : bit(mask, BIT_SER0);
    for (var index = 1; index < WIDTH; index++) {
      shifted[index] = shift[index - 1];
    }
    return shifted;
  }

  private static boolean accepts(Value[] sources, int mask) {
    for (var index = 0; index < sources.length; index++) {
      final var actual = sources[index];
      final var high = bitHigh(mask, index);
      if (actual == Value.TRUE && !high) return false;
      if (actual == Value.FALSE && high) return false;
    }
    return true;
  }

  private static boolean contains(Value[] sources, Value bit) {
    for (final var source : sources) {
      if (source == bit) return true;
    }
    return false;
  }

  private static boolean bitHigh(int mask, int bit) {
    return (mask & (1 << bit)) != 0;
  }

  private static Value bit(int mask, int index) {
    return bitHigh(mask, index) ? Value.TRUE : Value.FALSE;
  }

  private static Value[] filled(Value bit) {
    final var word = new Value[WIDTH];
    Arrays.fill(word, bit);
    return word;
  }

  private static State getState(InstanceState state) {
    var data = (State) state.getData();
    if (data == null) {
      data = new State();
      state.setData(data);
    }
    return data;
  }

  private static void drawWord(Graphics2D gfx, int x, int y, int height, Value[] word, int row) {
    for (var index = 0; index < WIDTH; index++) {
      final var bit = word[index];
      final var originX = x + 24 + index * 20;
      final var originY = y + height / 2 + row - 4;
      gfx.setColor(bit.getColor());
      gfx.fillOval(originX, originY, 8, 8);
      gfx.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(gfx, bit.toDisplayString(), originX + 4, originY + 4);
    }
    gfx.setColor(Color.BLACK);
  }

  private static final class State extends ClockState implements InstanceData {
    private Value[] storage;
    private Value[] shift;

    private State() {
      final var reset = AppPreferences.Memory_Startup_Unknown.get() ? Value.UNKNOWN : Value.FALSE;
      storage = filled(reset);
      shift = filled(reset);
    }

    @Override
    public State clone() {
      final var copy = (State) super.clone();
      copy.storage = storage.clone();
      copy.shift = shift.clone();
      return copy;
    }
  }

  /** Merges every accepted substitution. Disagreements become unknown, or error if one was seen. */
  private static final class BitChoice {
    private final Value[] bits;
    private final boolean[] conflict;
    private boolean sawError;
    private boolean any;

    private BitChoice(int width) {
      bits = new Value[width];
      conflict = new boolean[width];
    }

    private void accept(Value[] next, boolean error) {
      sawError |= error;
      if (!any) {
        any = true;
        System.arraycopy(next, 0, bits, 0, bits.length);
        return;
      }
      for (var index = 0; index < bits.length; index++) {
        if (bits[index] != next[index]) conflict[index] = true;
      }
    }

    private Value[] value() {
      if (!any) return filled(Value.UNKNOWN);
      final var merged = bits.clone();
      for (var index = 0; index < merged.length; index++) {
        if (conflict[index]) merged[index] = sawError ? Value.ERROR : Value.UNKNOWN;
      }
      return merged;
    }
  }
}
