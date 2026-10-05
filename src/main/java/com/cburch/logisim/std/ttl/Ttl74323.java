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

/**
 * TTL 74x323: 8-bit universal shift/storage register with 3-state outputs and synchronous clear.
 *
 * <p>Simulation follows the Hitachi HD74HC323 and ST M54/74HC323 function tables. The pinout matches
 * the 74x299. {@code nCLR} low clears every stage on the rising edge of {@code CP} and overrides
 * load and shift. {@code S1 S0} selects hold ({@code 00}), shift right ({@code 01}, {@code SR}
 * enters {@code Q0}), shift left ({@code 10}, {@code SL} enters {@code Q7}), or parallel load
 * ({@code 11}). There is no separate sign-extend mode.
 *
 * <p>The I/O pins are high-impedance when either output enable is high, and also while both mode
 * selects are high so the bus can be driven into the register. {@code Q0} and {@code Q7} stay
 * driven. An unknown or error input changes a result only when the accepted substitutions disagree.
 * An error among those inputs makes the disagreed bit an error.
 */
public class Ttl74323 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74323";

  public static final int DELAY = 1;

  public static final byte S0 = 1;
  public static final byte nOE1 = 2;
  public static final byte nOE2 = 3;
  public static final byte IO6 = 4;
  public static final byte IO4 = 5;
  public static final byte IO2 = 6;
  public static final byte IO0 = 7;
  public static final byte Q0 = 8;
  /** Synchronous clear, active low. */
  public static final byte nCLR = 9;

  public static final byte GND = 10;
  /** Serial input for a shift toward {@code Q7}. */
  public static final byte SR = 11;

  public static final byte CP = 12;
  public static final byte IO1 = 13;
  public static final byte IO3 = 14;
  public static final byte IO5 = 15;
  public static final byte IO7 = 16;
  public static final byte Q7 = 17;
  /** Serial input for a shift toward {@code Q0}. */
  public static final byte SL = 18;

  public static final byte S1 = 19;
  public static final byte VCC = 20;

  private static final int WIDTH = 8;
  private static final BitWidth WORD = BitWidth.create(WIDTH);
  private static final Value ZERO = Value.createKnown(WORD, 0);
  /** I/O pins in bit order. Bit 0 is {@code Q0}. */
  private static final byte[] IO_PINS = {IO0, IO1, IO2, IO3, IO4, IO5, IO6, IO7};

  private static final byte[] OUTPUT_PINS = {Q0, Q7};
  private static final String[] PORT_NAMES = {
    "S0 (mode select)",
    "nOE1 (output enable, active LOW)",
    "nOE2 (output enable, active LOW)",
    "IO6",
    "IO4",
    "IO2",
    "IO0",
    "Q0 (serial output)",
    "nCLR (synchronous clear, active LOW)",
    "SR (shift-right serial input)",
    "CP (clock)",
    "IO1",
    "IO3",
    "IO5",
    "IO7",
    "Q7 (serial output)",
    "SL (shift-left serial input)",
    "S1 (mode select)"
  };
  private static final String[] PIN_NAMES = {
    "S0", "nOE1", "nOE2", "IO6", "IO4", "IO2", "IO0", "Q0", "nCLR", null,
    "SR", "CP", "IO1", "IO3", "IO5", "IO7", "Q7", "SL", "S1", null
  };

  /** Creates a 74323 8-bit universal shift/storage register. */
  public Ttl74323() {
    super(_ID, (byte) 20, OUTPUT_PINS, null, IO_PINS, PORT_NAMES, new Ttl74323HdlGenerator());
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
    Drawgates.paintPortNamesByPin(painter, x, y, height, PIN_NAMES);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getState(state);
    final var triggered = data.updateClock(input(state, CP), StdAttr.TRIG_RISING);
    final var io = new Value[WIDTH];
    for (var bit = 0; bit < WIDTH; bit++) {
      io[bit] = input(state, IO_PINS[bit]);
    }
    final var next =
        resolveNext(
            data.getValue(),
            triggered,
            input(state, nCLR),
            input(state, S1),
            input(state, S0),
            input(state, SR),
            input(state, SL),
            io);
    data.setValue(next);
    drive(state, next, input(state, nOE1), input(state, nOE2), input(state, S0), input(state, S1));
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {pinNrToPortNr(CP)};
  }

  private static TtlRegisterData getState(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WORD);
      state.setData(data);
    }
    return data;
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }

  /**
   * Clear wins over every mode. Mode, serial data and the bus are sampled only on a rising edge;
   * without one the stored word is kept, unknown controls included.
   */
  private static Value resolveNext(
      Value stored,
      boolean triggered,
      Value nClr,
      Value s1,
      Value s0,
      Value sr,
      Value sl,
      Value[] io) {
    if (!triggered) {
      return stored;
    }
    if (nClr == Value.FALSE) {
      return ZERO;
    }
    final var active = resolveMode(stored, s1, s0, sr, sl, io);
    if (nClr == Value.TRUE) {
      return active;
    }
    return merge(ZERO, active, nClr == Value.ERROR);
  }

  private static Value resolveMode(Value stored, Value s1, Value s0, Value sr, Value sl, Value[] io) {
    final var s1Known = s1 == Value.TRUE || s1 == Value.FALSE;
    final var s0Known = s0 == Value.TRUE || s0 == Value.FALSE;
    if (s1Known && s0Known) {
      return applyMode(stored, s1 == Value.TRUE, s0 == Value.TRUE, sr, sl, io);
    }
    Value merged = null;
    final var selectError = s1 == Value.ERROR || s0 == Value.ERROR;
    for (var s1Bit = 0; s1Bit <= 1; s1Bit++) {
      if (s1Known && ((s1 == Value.TRUE) != (s1Bit == 1))) {
        continue;
      }
      for (var s0Bit = 0; s0Bit <= 1; s0Bit++) {
        if (s0Known && ((s0 == Value.TRUE) != (s0Bit == 1))) {
          continue;
        }
        final var next = applyMode(stored, s1Bit == 1, s0Bit == 1, sr, sl, io);
        merged = merged == null ? next : merge(merged, next, selectError);
      }
    }
    return merged == null ? Value.createUnknown(WORD) : merged;
  }

  /** {@code s1 s0}: 00 hold, 01 shift right, 10 shift left, 11 load. */
  private static Value applyMode(
      Value stored, boolean s1, boolean s0, Value sr, Value sl, Value[] io) {
    if (s1 && s0) {
      return pack(io);
    }
    if (!s1 && s0) {
      return shiftRight(stored, sr);
    }
    if (s1) {
      return shiftLeft(stored, sl);
    }
    return stored;
  }

  private static Value shiftRight(Value stored, Value serial) {
    final var bits = new Value[WIDTH];
    bits[0] = level(serial);
    for (var bit = 1; bit < WIDTH; bit++) {
      bits[bit] = stored.get(bit - 1);
    }
    return Value.create(bits);
  }

  private static Value shiftLeft(Value stored, Value serial) {
    final var bits = new Value[WIDTH];
    bits[WIDTH - 1] = level(serial);
    for (var bit = 0; bit < WIDTH - 1; bit++) {
      bits[bit] = stored.get(bit + 1);
    }
    return Value.create(bits);
  }

  private static Value pack(Value[] io) {
    final var bits = new Value[WIDTH];
    for (var bit = 0; bit < WIDTH; bit++) {
      bits[bit] = level(io[bit]);
    }
    return Value.create(bits);
  }

  private static void drive(
      InstanceState state, Value word, Value nOe1, Value nOe2, Value s0, Value s1) {
    state.setPort(pinNrToPortNr(Q0), level(word.get(0)), DELAY);
    state.setPort(pinNrToPortNr(Q7), level(word.get(WIDTH - 1)), DELAY);
    final var enabled = outputEnabled(nOe1, nOe2, s0, s1);
    for (var bit = 0; bit < WIDTH; bit++) {
      state.setPort(pinNrToPortNr(IO_PINS[bit]), driveBit(word.get(bit), enabled), DELAY);
    }
  }

  /**
   * The bus is driven only when both enables are low and the register is not loading. A high
   * enable or a parallel-load mode releases it.
   */
  private static Value outputEnabled(Value nOe1, Value nOe2, Value s0, Value s1) {
    final var released = eitherHigh(nOe1, nOe2);
    final var loading = bothHigh(s0, s1);
    if (released == Value.TRUE || loading == Value.TRUE) {
      return Value.FALSE;
    }
    if (released == Value.FALSE && loading == Value.FALSE) {
      return Value.TRUE;
    }
    if (released == Value.ERROR || loading == Value.ERROR) {
      return Value.ERROR;
    }
    return Value.UNKNOWN;
  }

  private static Value driveBit(Value bit, Value enabled) {
    if (enabled == Value.TRUE) {
      return level(bit);
    }
    if (enabled == Value.FALSE) {
      return Value.UNKNOWN;
    }
    if (level(bit) == Value.UNKNOWN) {
      return Value.UNKNOWN;
    }
    return enabled == Value.ERROR ? Value.ERROR : Value.UNKNOWN;
  }

  private static Value eitherHigh(Value left, Value right) {
    if (left == Value.TRUE || right == Value.TRUE) {
      return Value.TRUE;
    }
    if (left == Value.FALSE && right == Value.FALSE) {
      return Value.FALSE;
    }
    if (left == Value.ERROR || right == Value.ERROR) {
      return Value.ERROR;
    }
    return Value.UNKNOWN;
  }

  private static Value bothHigh(Value left, Value right) {
    if (left == Value.FALSE || right == Value.FALSE) {
      return Value.FALSE;
    }
    if (left == Value.TRUE && right == Value.TRUE) {
      return Value.TRUE;
    }
    if (left == Value.ERROR || right == Value.ERROR) {
      return Value.ERROR;
    }
    return Value.UNKNOWN;
  }

  /** Keeps a concrete bit. Anything else, including a floating bus, is unknown. */
  private static Value level(Value value) {
    if (value == Value.TRUE || value == Value.FALSE || value == Value.ERROR) {
      return value;
    }
    return Value.UNKNOWN;
  }

  private static Value merge(Value left, Value right, boolean controlError) {
    final var bits = new Value[WIDTH];
    for (var bit = 0; bit < WIDTH; bit++) {
      final var a = left.get(bit);
      final var b = right.get(bit);
      if (a == b) {
        bits[bit] = a;
      } else {
        bits[bit] =
            (controlError || a == Value.ERROR || b == Value.ERROR) ? Value.ERROR : Value.UNKNOWN;
      }
    }
    return Value.create(bits);
  }
}
