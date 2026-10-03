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
 * TTL 74HC83: 4-bit binary full adder with fast carry.
 *
 * <p>Simulation follows the
 * <a href="https://www.renesas.com/en/document/dst/hd74hc83-datasheet">Renesas HD74HC83</a>
 * pinout and function. This is the classic 7483 package: VCC is pin 5 and GND is pin 12. The
 * 74283 already in this library performs the same addition on the rearranged 74HC283 pinout.
 *
 * <p>The sum is {@code C4,∑4..∑1 = A4..A1 + B4..B1 + C0}. Every operand is active high. A bit
 * whose inputs and incoming carry are all known produces a known sum and carry. An error on any of
 * those three makes that sum and every higher result an error. Any other non-boolean value makes
 * that sum and every higher result unknown. Lower bits stay as already computed. Nanosecond delays
 * are not modeled.
 */
public class Ttl7483 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "7483";

  public static final int DELAY = 1;

  public static final byte A4 = 1;
  public static final byte S3 = 2;
  public static final byte A3 = 3;
  public static final byte B3 = 4;
  public static final byte VCC = 5;
  public static final byte S2 = 6;
  public static final byte B2 = 7;
  public static final byte A2 = 8;
  public static final byte S1 = 9;
  public static final byte A1 = 10;
  public static final byte B1 = 11;
  public static final byte GND = 12;
  public static final byte C0 = 13;
  public static final byte C4 = 14;
  public static final byte S4 = 15;
  public static final byte B4 = 16;

  private static final byte[] A_BITS = {A1, A2, A3, A4};
  private static final byte[] B_BITS = {B1, B2, B3, B4};
  private static final byte[] SUM_BITS = {S1, S2, S3, S4};
  private static final byte[] OUTPUTS = {S3, S2, S1, C4, S4};

  private static final String[] PORT_NAMES = {
    "A4 operand A bit 4",
    "∑3 sum bit 3",
    "A3 operand A bit 3",
    "B3 operand B bit 3",
    "∑2 sum bit 2",
    "B2 operand B bit 2",
    "A2 operand A bit 2",
    "∑1 sum bit 1",
    "A1 operand A bit 1",
    "B1 operand B bit 1",
    "C0 carry input",
    "C4 carry output",
    "∑4 sum bit 4",
    "B4 operand B bit 4"
  };

  private static final String[] PIN_LABELS = {
    "A4", "∑3", "A3", "B3", null, "∑2", "B2", "A2",
    "∑1", "A1", "B1", null, "C0", "C4", "∑4", "B4"
  };

  /** Creates a 7483 4-bit binary full adder. */
  public Ttl7483() {
    super(_ID, (byte) 16, OUTPUTS, null, PORT_NAMES, VCC, GND, new Ttl7483HdlGenerator());
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Pins 5 (VCC) and 12 (GND) are omitted from the port list. Callers must not pass either
   * power pin.
   *
   * @param pin datasheet pin number
   * @return port number
   */
  static int pinToPort(byte pin) {
    var skipped = 0;
    if (pin > VCC) {
      skipped++;
    }
    if (pin > GND) {
      skipped++;
    }
    return pin - 1 - skipped;
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, true, false);
    Drawgates.paintPortNamesByPin(painter, x, y, height, PIN_LABELS);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var carry = state.getPortValue(pinToPort(C0));
    for (var bit = 0; bit < A_BITS.length; bit++) {
      final var a = state.getPortValue(pinToPort(A_BITS[bit]));
      final var b = state.getPortValue(pinToPort(B_BITS[bit]));
      if (isError(a) || isError(b) || isError(carry)) {
        carry = Value.ERROR;
        state.setPort(pinToPort(SUM_BITS[bit]), Value.ERROR, DELAY);
      } else if (!isKnown(a) || !isKnown(b) || !isKnown(carry)) {
        carry = Value.UNKNOWN;
        state.setPort(pinToPort(SUM_BITS[bit]), Value.UNKNOWN, DELAY);
      } else {
        final var ones = bit(a) + bit(b) + bit(carry);
        state.setPort(pinToPort(SUM_BITS[bit]), (ones & 1) == 0 ? Value.FALSE : Value.TRUE, DELAY);
        carry = ones >= 2 ? Value.TRUE : Value.FALSE;
      }
    }
    state.setPort(pinToPort(C4), carry, DELAY);
  }

  private static boolean isKnown(Value value) {
    return value == Value.TRUE || value == Value.FALSE;
  }

  private static boolean isError(Value value) {
    return value != null && value.isErrorValue();
  }

  private static int bit(Value value) {
    return value == Value.TRUE ? 1 : 0;
  }
}
