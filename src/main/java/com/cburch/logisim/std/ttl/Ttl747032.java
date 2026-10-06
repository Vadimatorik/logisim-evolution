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
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x7032: quad 2-input OR gate with Schmitt-trigger inputs.
 *
 * <p>Simulation follows the TI SN74HC7032 function table (Rev. F, June 2019). Each gate performs
 * {@code Y = A + B} in positive logic. A high on either input forces the output high. When neither
 * input is high, an error input makes the output an error and an unknown input makes it unknown.
 * Schmitt-trigger hysteresis is an electrical property and is not modeled. The pinout matches the
 * 74x32.
 *
 * @see <a href="https://www.ti.com/lit/ds/symlink/sn74hc7032.pdf">SN74HC7032 datasheet</a>
 */
public class Ttl747032 extends Ttl7432 {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "747032";

  /** Logical port for physical pin 1, {@code 1A}. */
  public static final int PORT_1A = 0;

  /** Logical port for physical pin 2, {@code 1B}. */
  public static final int PORT_1B = 1;

  /** Logical port for physical pin 3, {@code 1Y}. */
  public static final int PORT_1Y = 2;

  /** Logical port for physical pin 4, {@code 2A}. */
  public static final int PORT_2A = 3;

  /** Logical port for physical pin 5, {@code 2B}. */
  public static final int PORT_2B = 4;

  /** Logical port for physical pin 6, {@code 2Y}. */
  public static final int PORT_2Y = 5;

  /** Logical port for physical pin 8, {@code 3Y}. */
  public static final int PORT_3Y = 6;

  /** Logical port for physical pin 9, {@code 3A}. */
  public static final int PORT_3A = 7;

  /** Logical port for physical pin 10, {@code 3B}. */
  public static final int PORT_3B = 8;

  /** Logical port for physical pin 11, {@code 4Y}. */
  public static final int PORT_4Y = 9;

  /** Logical port for physical pin 12, {@code 4A}. */
  public static final int PORT_4A = 10;

  /** Logical port for physical pin 13, {@code 4B}. */
  public static final int PORT_4B = 11;

  /** Logical port for physical pin 7 when power pins are shown. */
  public static final int PORT_GND = 12;

  /** Logical port for physical pin 14 when power pins are shown. */
  public static final int PORT_VCC = 13;

  public Ttl747032() {
    super(_ID);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    state.setPort(PORT_1Y, orGate(state.getPortValue(PORT_1A), state.getPortValue(PORT_1B)), 1);
    state.setPort(PORT_2Y, orGate(state.getPortValue(PORT_2A), state.getPortValue(PORT_2B)), 1);
    state.setPort(PORT_3Y, orGate(state.getPortValue(PORT_3A), state.getPortValue(PORT_3B)), 1);
    state.setPort(PORT_4Y, orGate(state.getPortValue(PORT_4A), state.getPortValue(PORT_4B)), 1);
  }

  /**
   * Positive-logic OR that keeps unknown and error distinct. A definite high masks the other
   * input. {@link Value#or(Value)} collapses a 1-bit unknown into an error, so it is not used.
   */
  private static Value orGate(Value left, Value right) {
    if (left == Value.TRUE || right == Value.TRUE) return Value.TRUE;
    if (left == Value.ERROR || right == Value.ERROR) return Value.ERROR;
    if (left == Value.UNKNOWN || right == Value.UNKNOWN) return Value.UNKNOWN;
    return Value.FALSE;
  }
}
