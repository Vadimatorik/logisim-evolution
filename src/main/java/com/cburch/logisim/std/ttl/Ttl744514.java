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
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Arrays;

/**
 * TTL 74x4514: 4-to-16 line decoder/demultiplexer with input latches.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT4514.pdf">Nexperia
 * 74HC4514/74HCT4514</a> and
 * <a href="https://www.ti.com/lit/ds/symlink/cd74hc4514.pdf">TI CD74HC4514</a> data sheets. The two
 * agree on the function table and the 24-pin numbers. Nexperia names the outputs {@code Q0} to
 * {@code Q15}; TI names the same pins {@code Y0} to {@code Y15}. This model uses the Nexperia
 * names. While {@code LE} is high the latched address follows {@code A3..A0}. Any level that is
 * not a solid high, including unknown and error, holds that address. While {@code nE} is low the
 * output selected by the latched address is high and the others are low. A high {@code nE} forces
 * every output low and does not change the latch. The data sheets call this enable {@code E}; the
 * model names it {@code nE} because it is active low. Using {@code nE} as data covers the
 * demultiplexer function. Nanosecond delays are not modeled.
 *
 * <p>An unknown or error value is replaced by every binary value it could still be. Outputs that
 * agree in every substitution keep that level. Outputs that disagree become unknown, or error when
 * one of the substituted values was an error. A known high {@code nE} still forces every output
 * low.
 */
public class Ttl744514 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "744514";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet.
  public static final byte LE = 1;
  public static final byte A0 = 2;
  public static final byte A1 = 3;
  public static final byte Q7 = 4;
  public static final byte Q6 = 5;
  public static final byte Q5 = 6;
  public static final byte Q4 = 7;
  public static final byte Q3 = 8;
  public static final byte Q1 = 9;
  public static final byte Q2 = 10;
  public static final byte Q0 = 11;
  public static final byte GND = 12;
  public static final byte Q13 = 13;
  public static final byte Q12 = 14;
  public static final byte Q15 = 15;
  public static final byte Q14 = 16;
  public static final byte Q9 = 17;
  public static final byte Q8 = 18;
  public static final byte Q11 = 19;
  public static final byte Q10 = 20;
  public static final byte A2 = 21;
  public static final byte A3 = 22;
  public static final byte NE = 23;
  public static final byte VCC = 24;

  private static final int BITS = 4;
  private static final int OUTPUTS_COUNT = 16;
  private static final BitWidth WIDTH = BitWidth.create(BITS);
  private static final int BIT_LE = 4;
  private static final int BIT_ENABLE = 5;
  private static final int SUBSTITUTIONS = 1 << (BITS + 2);

  /** Outputs ordered by the address that selects them. */
  private static final byte[] OUTPUTS = {
    Q0, Q1, Q2, Q3, Q4, Q5, Q6, Q7, Q8, Q9, Q10, Q11, Q12, Q13, Q14, Q15
  };

  private static final byte[] OUTPUT_PINS = {
    Q7, Q6, Q5, Q4, Q3, Q1, Q2, Q0, Q13, Q12, Q15, Q14, Q9, Q8, Q11, Q10
  };

  private static final byte[] ADDRESS = {A0, A1, A2, A3};

  private static final String[] PORT_NAMES = {
    "LE Latch enable (active HIGH)",
    "A0 Address (LSB)",
    "A1 Address",
    "Q7",
    "Q6",
    "Q5",
    "Q4",
    "Q3",
    "Q1",
    "Q2",
    "Q0",
    "Q13",
    "Q12",
    "Q15",
    "Q14",
    "Q9",
    "Q8",
    "Q11",
    "Q10",
    "A2 Address",
    "A3 Address (MSB)",
    "nE Enable (active LOW)"
  };

  /** Creates a 744514 4-to-16 line decoder/demultiplexer with input latches. */
  public Ttl744514() {
    super(_ID, (byte) 24, OUTPUT_PINS, PORT_NAMES, new Ttl744514HdlGenerator());
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
    Drawgates.paintPortNamesByPin(
        painter,
        x,
        y,
        height,
        new String[] {
          "LE", "A0", "A1", "Q7", "Q6", "Q5", "Q4", "Q3", "Q1", "Q2", "Q0", null,
          "Q13", "Q12", "Q15", "Q14", "Q9", "Q8", "Q11", "Q10", "A2", "A3", "nE", null
        });
    drawState(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private void drawState(Graphics2D gfx, int x, int y, int height, TtlRegisterData state) {
    if (state == null) return;
    final var word = state.getValue();
    for (var bit = 0; bit < BITS; bit++) {
      final var value = word.get(BITS - 1 - bit);
      final var dotX = x + 96 + bit * 16;
      gfx.setColor(value.getColor());
      gfx.fillOval(dotX, y + height / 2 - 4, 8, 8);
      gfx.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(gfx, value.toDisplayString(), dotX + 4, y + height / 2);
    }
    gfx.setColor(Color.BLACK);
  }

  private static TtlRegisterData getStateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WIDTH);
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);
    final var latchEnable = state.getPortValue(pinNrToPortNr(LE));
    final var pins = capturedAddress(state);
    if (latchEnable == Value.TRUE) {
      data.setValue(pins);
    }
    final var outputs =
        decodedOutputs(
            data.getValue(), latchEnable, state.getPortValue(pinNrToPortNr(NE)), pins);
    for (var index = 0; index < OUTPUTS.length; index++) {
      state.setPort(pinNrToPortNr(OUTPUTS[index]), outputs[index], DELAY);
    }
  }

  /** Copies A0 through A3, including unknown and error bits. Bit 0 is A0. */
  private static Value capturedAddress(InstanceState state) {
    final var bits = new Value[BITS];
    for (var bit = 0; bit < ADDRESS.length; bit++) {
      bits[bit] = state.getPortValue(pinNrToPortNr(ADDRESS[bit]));
    }
    return Value.create(bits);
  }

  /**
   * Decodes every binary substitution of the unknown or error inputs. Latch enable selects the
   * address source: the pins while it is high, and the stored address while it is low.
   */
  private static Value[] decodedOutputs(
      Value stored, Value latchEnable, Value enable, Value pins) {
    final var storedBits = bitsOf(stored);
    final var pinBits = bitsOf(pins);
    Value[] merged = null;
    final var conflict = new boolean[OUTPUTS_COUNT];
    var sawError = false;
    for (var mask = 0; mask < SUBSTITUTIONS; mask++) {
      final var latchHigh = bitHigh(mask, BIT_LE);
      final var enableHigh = bitHigh(mask, BIT_ENABLE);
      if (!accepts(latchEnable, latchHigh) || !accepts(enable, enableHigh)) {
        continue;
      }
      final var source = latchHigh ? pinBits : storedBits;
      final var code = acceptedCode(mask, source);
      if (code < 0) {
        continue;
      }
      sawError |= isError(latchEnable) || isError(enable) || errorIn(source);
      final var next = decode(code, !enableHigh);
      if (merged == null) {
        merged = next;
      } else {
        markConflicts(conflict, merged, next);
      }
    }
    return resolve(merged, conflict, sawError);
  }

  private static Value[] bitsOf(Value word) {
    final var bits = new Value[BITS];
    for (var bit = 0; bit < BITS; bit++) {
      bits[bit] = word.get(bit);
    }
    return bits;
  }

  /** Returns the address in {@code mask}, or {@code -1} when {@code source} rejects a bit. */
  private static int acceptedCode(int mask, Value[] source) {
    var code = 0;
    for (var bit = 0; bit < BITS; bit++) {
      final var high = bitHigh(mask, bit);
      if (!accepts(source[bit], high)) {
        return -1;
      }
      if (high) {
        code |= 1 << bit;
      }
    }
    return code;
  }

  private static boolean bitHigh(int mask, int bit) {
    return (mask & (1 << bit)) != 0;
  }

  private static boolean accepts(Value bit, boolean high) {
    if (bit == Value.TRUE) return high;
    if (bit == Value.FALSE) return !high;
    return true;
  }

  private static boolean isError(Value bit) {
    return bit == Value.ERROR;
  }

  private static boolean errorIn(Value[] bits) {
    for (final var bit : bits) {
      if (bit == Value.ERROR) return true;
    }
    return false;
  }

  private static Value[] decode(int code, boolean enabled) {
    final var outputs = new Value[OUTPUTS_COUNT];
    Arrays.fill(outputs, Value.FALSE);
    if (enabled) outputs[code] = Value.TRUE;
    return outputs;
  }

  private static void markConflicts(boolean[] conflict, Value[] current, Value[] next) {
    for (var index = 0; index < OUTPUTS_COUNT; index++) {
      if (current[index] != next[index]) conflict[index] = true;
    }
  }

  private static Value[] resolve(Value[] merged, boolean[] conflict, boolean sawError) {
    final var result = merged == null ? new Value[OUTPUTS_COUNT] : merged;
    if (merged == null) Arrays.fill(result, Value.UNKNOWN);
    for (var index = 0; index < OUTPUTS_COUNT; index++) {
      if (conflict[index]) result[index] = sawError ? Value.ERROR : Value.UNKNOWN;
    }
    return result;
  }
}
