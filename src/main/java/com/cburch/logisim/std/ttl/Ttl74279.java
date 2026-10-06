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
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74HC279: quad S-R latch.
 *
 * <p>The digital function follows the ST M74HC279 and Toshiba TC74HC279 truth tables, which match
 * the Fairchild DM74LS279 table. Every set and reset input is active low. Latches 1 and 3 each
 * have two set inputs: the latch sets when either one is low. While set and reset are both low,
 * {@code Q} is high. Releasing both in the same step is the cross-coupled NAND race described by
 * Fairchild and TI, so {@code Q} becomes unknown. Releasing one input before the other follows the
 * remaining active input. Nanosecond delays are not modeled. The Toshiba prose that says both-low
 * produces a low output disagrees with its own table and is not used.
 *
 * <p>A set input that is exactly low forces {@code Q} high even when reset is unknown or an error.
 * Any other input that is not exactly high or low is replaced by both binary values. Identical
 * results pass through. A 0/1 conflict is an error when any of those inputs is an error, and
 * unknown otherwise.
 */
public class Ttl74279 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74279";

  public static final int DELAY = 1;

  public static final byte R1 = 1;
  public static final byte S1A = 2;
  public static final byte S1B = 3;
  public static final byte Q1 = 4;
  public static final byte R2 = 5;
  public static final byte S2 = 6;
  public static final byte Q2 = 7;
  public static final byte GND = 8;
  public static final byte Q3 = 9;
  public static final byte R3 = 10;
  public static final byte S3A = 11;
  public static final byte S3B = 12;
  public static final byte Q4 = 13;
  public static final byte R4 = 14;
  public static final byte S4 = 15;
  public static final byte VCC = 16;

  private static final byte[] OUTPUTS = {Q1, Q2, Q3, Q4};
  private static final Latch[] LATCHES = {
    new Latch(new byte[] {S1A, S1B}, R1, Q1),
    new Latch(new byte[] {S2}, R2, Q2),
    new Latch(new byte[] {S3A, S3B}, R3, Q3),
    new Latch(new byte[] {S4}, R4, Q4)
  };
  private static final String[] PORT_NAMES = {
    "1R reset (active low)",
    "1S1 set (active low)",
    "1S2 set (active low)",
    "1Q",
    "2R reset (active low)",
    "2S set (active low)",
    "2Q",
    "3Q",
    "3R reset (active low)",
    "3S1 set (active low)",
    "3S2 set (active low)",
    "4Q",
    "4R reset (active low)",
    "4S set (active low)"
  };

  /** Creates a 74279 quad S-R latch. */
  public Ttl74279() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74279HdlGenerator());
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
    super.paintBase(painter, true, false);
    Drawgates.paintPortNamesByPin(
        painter,
        x,
        y,
        height,
        new String[] {
          "1R", "1S1", "1S2", "1Q", "2R", "2S", "2Q", null,
          "3Q", "3R", "3S1", "3S2", "4Q", "4R", "4S", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = latchState(state);
    for (var index = 0; index < LATCHES.length; index++) {
      final var latch = LATCHES[index];
      final var sets = values(state, latch.sets);
      final var reset = input(state, latch.reset);
      final var next =
          nextLevel(data.level(index), data.bothAsserted(index), sets, reset);
      data.setLevel(index, next);
      data.setBothAsserted(index, bothAsserted(sets, reset));
      state.setPort(pinNrToPortNr(latch.output), next, DELAY);
    }
  }

  private static LatchState latchState(InstanceState state) {
    var data = (LatchState) state.getData();
    if (data == null) {
      data = new LatchState();
      state.setData(data);
    }
    return data;
  }

  private static Value nextLevel(Value stored, boolean wasBoth, Value[] sets, Value reset) {
    if (anyEqual(sets, Value.FALSE)) {
      return Value.TRUE;
    }
    if (allBinary(sets, reset)) {
      return evalBinary(stored, wasBoth, sets, reset);
    }
    return enumeratedLevel(stored, wasBoth, sets, reset);
  }

  /**
   * Evaluates one fully defined input combination. A low set input forces the output high. A low
   * reset otherwise clears it. Leaving the both-low state for the both-high state is a race.
   */
  private static Value evalBinary(Value stored, boolean wasBoth, Value[] sets, Value reset) {
    if (anyEqual(sets, Value.FALSE)) {
      return Value.TRUE;
    }
    if (reset == Value.FALSE) {
      return Value.FALSE;
    }
    if (wasBoth) {
      return Value.UNKNOWN;
    }
    return stored;
  }

  private static Value enumeratedLevel(Value stored, boolean wasBoth, Value[] sets, Value reset) {
    final var pins = new Value[sets.length + 1];
    System.arraycopy(sets, 0, pins, 0, sets.length);
    pins[sets.length] = reset;
    return enumerate(pins, 0, stored, wasBoth, anyEqual(pins, Value.ERROR), null);
  }

  private static Value enumerate(
      Value[] pins,
      int index,
      Value stored,
      boolean wasBoth,
      boolean sawError,
      Value combined) {
    if (index == pins.length) {
      final var sets = new Value[pins.length - 1];
      System.arraycopy(pins, 0, sets, 0, sets.length);
      return merge(combined, evalBinary(stored, wasBoth, sets, pins[sets.length]), sawError);
    }
    final var original = pins[index];
    if (original == Value.TRUE || original == Value.FALSE) {
      return enumerate(pins, index + 1, stored, wasBoth, sawError, combined);
    }
    pins[index] = Value.FALSE;
    final var withLow = enumerate(pins, index + 1, stored, wasBoth, sawError, combined);
    pins[index] = Value.TRUE;
    final var withHigh = enumerate(pins, index + 1, stored, wasBoth, sawError, withLow);
    pins[index] = original;
    return withHigh;
  }

  private static Value merge(Value current, Value next, boolean sawError) {
    if (current == null || current == next) {
      return next;
    }
    if (current == Value.ERROR || next == Value.ERROR) {
      return Value.ERROR;
    }
    if (current.isFullyDefined() && next.isFullyDefined()) {
      return sawError ? Value.ERROR : Value.UNKNOWN;
    }
    return Value.UNKNOWN;
  }

  private static boolean bothAsserted(Value[] sets, Value reset) {
    return reset == Value.FALSE && anyEqual(sets, Value.FALSE);
  }

  private static boolean allBinary(Value[] sets, Value reset) {
    if (reset != Value.TRUE && reset != Value.FALSE) {
      return false;
    }
    for (final var set : sets) {
      if (set != Value.TRUE && set != Value.FALSE) {
        return false;
      }
    }
    return true;
  }

  private static boolean anyEqual(Value[] values, Value expected) {
    for (final var value : values) {
      if (value == expected) {
        return true;
      }
    }
    return false;
  }

  private static Value[] values(InstanceState state, byte[] pins) {
    final var values = new Value[pins.length];
    for (var index = 0; index < pins.length; index++) {
      values[index] = input(state, pins[index]);
    }
    return values;
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }

  private static final class Latch {
    private final byte[] sets;
    private final byte reset;
    private final byte output;

    private Latch(byte[] sets, byte reset, byte output) {
      this.sets = sets;
      this.reset = reset;
      this.output = output;
    }
  }

  /**
   * Per-instance latch levels and whether each latch is currently in the both-low state. The
   * levels come from {@link TtlRegisterData}, so the memory-startup preference supplies the value
   * held before the first set or reset.
   */
  private static final class LatchState implements InstanceData {
    private static final int LATCHES = 4;

    private final TtlRegisterData outputs = new TtlRegisterData(BitWidth.ONE, LATCHES);
    private final boolean[] bothAsserted = new boolean[LATCHES];

    private Value level(int index) {
      return outputs.getValue(index);
    }

    private void setLevel(int index, Value value) {
      outputs.setValue(index, value);
    }

    private boolean bothAsserted(int index) {
      return bothAsserted[index];
    }

    private void setBothAsserted(int index, boolean value) {
      bothAsserted[index] = value;
    }

    @Override
    public LatchState clone() {
      final var copy = new LatchState();
      for (var index = 0; index < LATCHES; index++) {
        copy.setLevel(index, level(index));
        copy.setBothAsserted(index, bothAsserted(index));
      }
      return copy;
    }
  }
}
