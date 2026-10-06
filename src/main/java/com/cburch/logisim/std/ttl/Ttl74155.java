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
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x155: dual 2-line to 4-line decoder/demultiplexer.
 *
 * <p>Simulation follows the Texas Instruments SN74LS155A function table. ST M74HC155 and Toshiba
 * TC74HC155A describe the 74HC155 as pin and function compatible with that device. Address inputs
 * {@code A} (least significant, pin 13) and {@code B} (pin 3) are shared. Section 1 is active when
 * {@code 1G} is low and {@code 1C} is high; the selected {@code 1Y} output is then low. Section 2
 * is active when {@code 2G} is high and {@code 2C} is low; the selected {@code 2Y} output is then
 * low. Every other output stays high. Outputs are push-pull, and propagation delay is one
 * simulator tick.
 *
 * <p>An unknown or error input changes an output only when the two substitutions disagree. An error
 * on such an input makes the disagreed output an error; an unknown input makes it unknown. A
 * strictly inactive strobe or data input therefore keeps its section high.
 */
public class Ttl74155 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74155";

  public static final int DELAY = 1;

  /** Section-1 data input. The selected output is the complement of this pin. */
  public static final byte C1 = 1;

  /** Section-1 strobe, active low. */
  public static final byte G1 = 2;

  /** Shared address input, most significant bit. */
  public static final byte B = 3;

  public static final byte Y1_3 = 4;
  public static final byte Y1_2 = 5;
  public static final byte Y1_1 = 6;
  public static final byte Y1_0 = 7;

  public static final byte GND = 8;

  public static final byte Y2_0 = 9;
  public static final byte Y2_1 = 10;
  public static final byte Y2_2 = 11;
  public static final byte Y2_3 = 12;

  /** Shared address input, least significant bit. */
  public static final byte A = 13;

  /** Section-2 strobe, active high. */
  public static final byte G2 = 14;

  /** Section-2 data input. The selected output follows this pin. */
  public static final byte C2 = 15;

  public static final byte VCC = 16;

  private static final byte[] SOURCES = {C1, G1, B, A, G2, C2};
  private static final int BIT_C1 = 0;
  private static final int BIT_G1 = 1;
  private static final int BIT_B = 2;
  private static final int BIT_A = 3;
  private static final int BIT_G2 = 4;
  private static final int BIT_C2 = 5;

  /** Output pins in the same order as the bits of {@link #levelsFor(int)}. */
  private static final byte[] RESULT_PINS = {Y1_0, Y1_1, Y1_2, Y1_3, Y2_0, Y2_1, Y2_2, Y2_3};

  private static final byte[] OUTPUTS = {Y1_3, Y1_2, Y1_1, Y1_0, Y2_0, Y2_1, Y2_2, Y2_3};

  private static final String[] PORT_NAMES = {
    "1C Data input (active high)",
    "1G Strobe (active low)",
    "B Address (MSB)",
    "1Y3",
    "1Y2",
    "1Y1",
    "1Y0",
    "2Y0",
    "2Y1",
    "2Y2",
    "2Y3",
    "A Address (LSB)",
    "2G Strobe (active high)",
    "2C Data input (active low)"
  };

  /** Creates a 74155 dual 2-line to 4-line decoder/demultiplexer. */
  public Ttl74155() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74155HdlGenerator());
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
          "1C", "1G", "B", "1Y3", "1Y2", "1Y1", "1Y0", null,
          "2Y0", "2Y1", "2Y2", "2Y3", "A", "2G", "2C", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var sources = new Value[SOURCES.length];
    for (var index = 0; index < SOURCES.length; index++) {
      sources[index] = state.getPortValue(pinNrToPortNr(SOURCES[index]));
    }
    final var choice = new Choice();
    for (var mask = 0; mask < (1 << sources.length); mask++) {
      if (!accepts(sources, mask)) {
        continue;
      }
      choice.accept(levelsFor(mask), errorIn(sources));
    }
    choice.drive(state);
  }

  /**
   * Active-low output levels for one fully binary input combination.
   *
   * <p>Bits 0 to 3 are {@code 1Y0} to {@code 1Y3}. Bits 4 to 7 are {@code 2Y0} to {@code 2Y3}. A
   * set bit is a high output.
   */
  private static int levelsFor(int mask) {
    final var address = (bitHigh(mask, BIT_B) ? 2 : 0) + (bitHigh(mask, BIT_A) ? 1 : 0);
    var levels = 0xFF;
    if (!bitHigh(mask, BIT_G1) && bitHigh(mask, BIT_C1)) {
      levels &= ~(1 << address);
    }
    if (bitHigh(mask, BIT_G2) && !bitHigh(mask, BIT_C2)) {
      levels &= ~(1 << (4 + address));
    }
    return levels;
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

  /** Merges every accepted substitution. Disagreements become unknown, or error if one was seen. */
  private static final class Choice {
    private final Value[] bits = new Value[RESULT_PINS.length];
    private final boolean[] conflict = new boolean[RESULT_PINS.length];
    private boolean sawError;
    private boolean any;

    private void accept(int levels, boolean error) {
      sawError |= error;
      if (!any) {
        any = true;
        for (var index = 0; index < bits.length; index++) {
          bits[index] = bitHigh(levels, index) ? Value.TRUE : Value.FALSE;
        }
        return;
      }
      for (var index = 0; index < bits.length; index++) {
        final var bit = bitHigh(levels, index) ? Value.TRUE : Value.FALSE;
        if (bits[index] != bit) {
          conflict[index] = true;
        }
      }
    }

    private void drive(InstanceState state) {
      for (var index = 0; index < RESULT_PINS.length; index++) {
        final Value value;
        if (!any) {
          value = Value.UNKNOWN;
        } else if (conflict[index]) {
          value = sawError ? Value.ERROR : Value.UNKNOWN;
        } else {
          value = bits[index];
        }
        state.setPort(pinNrToPortNr(RESULT_PINS[index]), value, DELAY);
      }
    }
  }
}
