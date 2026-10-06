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
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import java.util.Map;

/**
 * TTL 74x48: BCD to 7-segment decoder/driver with active-high outputs.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74ls48.pdf">SN74LS48</a> function table, which the
 * pin-compatible 74HC48 uses as well. A high output lights the segment on a common-cathode
 * display. The same pins as {@link Ttl7447} are active low there. Digits 6 and 9 have no tails:
 * segment {@code a} stays off on 6 and segment {@code d} stays off on 9.
 *
 * <p>{@code BI} low blanks every segment and overrides {@code LT}. With {@code BI/RBO} released,
 * {@code LT} low lights every segment. {@code RBI} low blanks only code 0 and drives {@code BI/RBO}
 * low; otherwise that pin is released and reads high through its pull-up. An unknown or error bit
 * changes a segment only when the codes still allowed disagree. An error on such a bit makes that
 * disagreement an error; an unknown bit makes it unknown.
 */
public class Ttl7448 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "7448";

  public static final int DELAY = 1;

  public static final byte B = 1;
  public static final byte C = 2;
  /** Lamp test, active low. */
  public static final byte LT = 3;
  /** Blanking input and ripple-blanking output. */
  public static final byte BI_RBO = 4;
  /** Ripple blanking input, active low. */
  public static final byte RBI = 5;
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
  /**
   * Glyph bit 0 is segment a and bit 6 is segment g. A set bit lights the segment. Digit 6 is
   * {@code 0x7C} and digit 9 is {@code 0x67}, both without a tail. Code 15 is blank.
   */
  private static final int[] GLYPH = {
    0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7C, 0x07,
    0x7F, 0x67, 0x58, 0x4C, 0x62, 0x69, 0x78, 0x00
  };
  private static final byte[] DATA = {A, B, C, D};
  private static final byte[] OUTPUTS = {SEGE, SEGD, SEGC, SEGB, SEGA, SEGG, SEGF};
  private static final byte[] SEGMENT_PINS = {SEGA, SEGB, SEGC, SEGD, SEGE, SEGF, SEGG};
  private static final String[] PORT_NAMES = {
    "B",
    "C",
    "LT (lamp test, active LOW)",
    "BI/RBO (blanking input / ripple blanking output)",
    "RBI (ripple blanking, active LOW)",
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

  /** Creates a 7448 BCD to 7-segment decoder/driver. */
  public Ttl7448() {
    super(
        _ID,
        (byte) 16,
        OUTPUTS,
        new byte[] {},
        new byte[] {BI_RBO},
        PORT_NAMES,
        new Ttl7448HdlGenerator());
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
          "B", "C", "LT", "BI/RBO", "RBI", "D", "A", null,
          "e", "d", "c", "b", "a", "g", "f", null
        });
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    super.configureNewInstance(instance);
    instance.getComponent().setPullPorts(Map.of((int) pinNrToPortNr(BI_RBO), Value.TRUE));
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var bits = new Value[BITS];
    for (var index = 0; index < BITS; index++) {
      bits[index] = input(state, DATA[index]);
    }
    final var lampTest = input(state, LT);
    final var blanking = input(state, BI_RBO);
    final var rippleBlanking = input(state, RBI);
    for (var segment = 0; segment < SEGMENTS; segment++) {
      final var decoded = decodeSegment(segment, bits, rippleBlanking);
      state.setPort(
          pinNrToPortNr(SEGMENT_PINS[segment]),
          applyControls(lampTest, blanking, decoded),
          DELAY);
    }
    final var blankZero = rippleBlanking == Value.FALSE
        && lampTest != Value.FALSE
        && exactCode(bits) == 0;
    state.setPort(pinNrToPortNr(BI_RBO), blankZero ? Value.FALSE : Value.UNKNOWN, DELAY);
  }

  private static Value decodeSegment(int segment, Value[] bits, Value rippleBlanking) {
    Value agreed = null;
    var conflict = false;
    var conflictFromError = false;
    final var fromError = hasError(bits, rippleBlanking);
    for (var code = 0; code < GLYPH.length; code++) {
      if (!accepts(bits, code)) continue;
      if (code == 0 && rippleBlanking != Value.TRUE) {
        final var merged = merge(agreed, Value.FALSE, conflict, conflictFromError, fromError);
        agreed = merged.level;
        conflict = merged.conflict;
        conflictFromError = merged.fromError;
      }
      if (code == 0 && rippleBlanking == Value.FALSE) continue;
      final var on = ((GLYPH[code] >> segment) & 1) == 1;
      final var level = on ? Value.TRUE : Value.FALSE;
      final var merged = merge(agreed, level, conflict, conflictFromError, fromError);
      agreed = merged.level;
      conflict = merged.conflict;
      conflictFromError = merged.fromError;
    }
    if (agreed == null) return Value.ERROR;
    if (!conflict) return agreed;
    return conflictFromError ? Value.ERROR : Value.UNKNOWN;
  }

  private static Agreement merge(
      Value agreed, Value level, boolean conflict, boolean conflictFromError, boolean fromError) {
    if (agreed == null) return new Agreement(level, conflict, conflictFromError);
    if (agreed == level) return new Agreement(agreed, conflict, conflictFromError);
    return new Agreement(agreed, true, conflictFromError || fromError);
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

  private static boolean hasError(Value[] bits, Value rippleBlanking) {
    if (rippleBlanking != Value.TRUE
        && rippleBlanking != Value.FALSE
        && rippleBlanking.isErrorValue()) {
      return true;
    }
    for (final var bit : bits) {
      if (bit != Value.TRUE && bit != Value.FALSE && bit.isErrorValue()) return true;
    }
    return false;
  }

  /** Returns the BCD code when every bit is a definite level, otherwise -1. */
  private static int exactCode(Value[] bits) {
    var code = 0;
    for (var place = 0; place < bits.length; place++) {
      if (bits[place] == Value.TRUE) code |= 1 << place;
      else if (bits[place] != Value.FALSE) return -1;
    }
    return code;
  }

  private static Value applyControls(Value lampTest, Value blanking, Value decoded) {
    if (blanking == Value.FALSE) return Value.FALSE;
    final var lit = applyLamp(lampTest, decoded);
    if (blanking == Value.TRUE) return lit;
    return disagree(blanking, Value.FALSE, lit);
  }

  private static Value applyLamp(Value lampTest, Value decoded) {
    if (lampTest == Value.FALSE) return Value.TRUE;
    if (lampTest == Value.TRUE) return decoded;
    return disagree(lampTest, Value.TRUE, decoded);
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

  private record Agreement(Value level, boolean conflict, boolean fromError) {}
}
