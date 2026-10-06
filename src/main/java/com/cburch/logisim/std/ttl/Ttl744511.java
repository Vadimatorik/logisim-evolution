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

/**
 * TTL 74HC4511: BCD to 7-segment latch/decoder/driver.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT4511.pdf">Nexperia 74HC4511</a>
 * function table. Segment outputs are active high. While {@code LE} is low the latch is
 * transparent; while {@code LE} is high it keeps the last BCD code. {@code LT} low forces every
 * segment high, and otherwise {@code BI} low forces every segment low. Neither input changes the
 * latch. Codes 10 through 15 blank the display. Nanosecond delays are not modeled, and a control
 * that is not exactly high or low does not write the latch.
 */
public class Ttl744511 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "744511";

  public static final int DELAY = 1;

  public static final byte B = 1;
  public static final byte C = 2;
  /** Lamp test, active low. */
  public static final byte LT = 3;
  /** Blanking input, active low. */
  public static final byte BI = 4;
  /** Latch enable, active low. */
  public static final byte LE = 5;
  public static final byte D = 6;
  public static final byte A = 7;
  public static final byte GND = 8;
  public static final byte SEGE = 9;
  public static final byte SEGD = 10;
  public static final byte SEGC = 11;
  public static final byte SEGB = 12;
  public static final byte SEGA = 13;
  public static final byte SEGG = 14;
  public static final byte SEGF = 15;
  public static final byte VCC = 16;

  private static final int BITS = 4;
  private static final int SEGMENTS = 7;
  /** Glyph bit 0 is segment a and bit 6 is segment g. Codes 10..15 are blank. */
  private static final int[] GLYPH = {
    0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7C, 0x07,
    0x7F, 0x67, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
  };
  private static final byte[] DATA = {A, B, C, D};
  private static final byte[] OUTPUTS = {SEGA, SEGB, SEGC, SEGD, SEGE, SEGF, SEGG};
  private static final String[] PORT_NAMES = {
    "B",
    "C",
    "LT (lamp test, active low)",
    "BI (blanking, active low)",
    "LE (latch enable, active low)",
    "D",
    "A",
    "e",
    "d",
    "c",
    "b",
    "a",
    "g",
    "f"
  };

  /** Creates a 744511 BCD to 7-segment latch/decoder/driver. */
  public Ttl744511() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl744511HdlGenerator());
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
          "B", "C", "LT", "BI", "LE", "D", "A", null,
          "e", "d", "c", "b", "a", "g", "f", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = stateData(state);
    if (input(state, LE) == Value.FALSE) {
      for (var index = 0; index < BITS; index++) {
        data.setValue(index, input(state, DATA[index]));
      }
    }
    final var bits = new Value[BITS];
    for (var index = 0; index < BITS; index++) {
      bits[index] = data.getValue(index);
    }
    final var lampTest = input(state, LT);
    final var blanking = input(state, BI);
    for (var segment = 0; segment < SEGMENTS; segment++) {
      final var decoded = decodeSegment(segment, bits);
      state.setPort(
          pinNrToPortNr(OUTPUTS[segment]), applyControls(lampTest, blanking, decoded), DELAY);
    }
  }

  private static TtlRegisterData stateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.ONE, BITS);
      state.setData(data);
    }
    return data;
  }

  private static Value decodeSegment(int segment, Value[] bits) {
    Value agreed = null;
    var conflict = false;
    var conflictFromError = false;
    for (var code = 0; code < GLYPH.length; code++) {
      if (!accepts(bits, code)) continue;
      final var level = ((GLYPH[code] >> segment) & 1) == 1 ? Value.TRUE : Value.FALSE;
      if (agreed == null) {
        agreed = level;
      } else if (agreed != level) {
        conflict = true;
        if (hasError(bits)) conflictFromError = true;
      }
    }
    if (agreed == null) return Value.ERROR;
    if (!conflict) return agreed;
    return conflictFromError ? Value.ERROR : Value.UNKNOWN;
  }

  private static boolean accepts(Value[] bits, int code) {
    for (var place = 0; place < bits.length; place++) {
      final var bit = bits[place];
      final var high = ((code >> place) & 1) == 1;
      if (bit == Value.TRUE && !high) return false;
      if (bit == Value.FALSE && high) return false;
    }
    return true;
  }

  private static boolean hasError(Value[] bits) {
    for (final var bit : bits) {
      if (bit != Value.TRUE && bit != Value.FALSE && bit.isErrorValue()) return true;
    }
    return false;
  }

  private static Value applyControls(Value lampTest, Value blanking, Value decoded) {
    if (lampTest == Value.FALSE) return Value.TRUE;
    final var blanked = applyBlanking(blanking, decoded);
    if (lampTest == Value.TRUE) return blanked;
    return disagree(lampTest, Value.TRUE, blanked);
  }

  private static Value applyBlanking(Value blanking, Value decoded) {
    if (blanking == Value.FALSE) return Value.FALSE;
    if (blanking == Value.TRUE) return decoded;
    return disagree(blanking, Value.FALSE, decoded);
  }

  /**
   * Combines a forced level with the decoded level when a control is not exactly high or low. An
   * error on that control produces an error where the two levels differ; any other disagreement is
   * unknown.
   */
  private static Value disagree(Value selector, Value forced, Value other) {
    if (forced == other) return forced;
    if (selector == Value.ERROR || other == Value.ERROR) return Value.ERROR;
    return Value.UNKNOWN;
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }
}
