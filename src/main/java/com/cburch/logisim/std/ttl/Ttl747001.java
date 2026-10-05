/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

/**
 * TTL 74x7001: quad 2-input AND gate with Schmitt-trigger inputs.
 *
 * <p>Model based on the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74hc7001.pdf">TI SN74HC7001 datasheet</a> (Rev. F).
 * Each output is the AND of its two inputs, and the DIP-14 pinout matches the 7408.
 * Schmitt-trigger thresholds and hysteresis are analog and are not modeled; a valid digital input
 * is treated as a logic level, as with the existing 7414 and 7424 models. Drawing and HDL
 * generation are inherited from {@link Ttl7408}.
 */
public class Ttl747001 extends Ttl7408 {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "747001";

  public static final int PORT_INDEX_1A = 0;
  public static final int PORT_INDEX_1B = 1;
  public static final int PORT_INDEX_1Y = 2;
  public static final int PORT_INDEX_2A = 3;
  public static final int PORT_INDEX_2B = 4;
  public static final int PORT_INDEX_2Y = 5;
  public static final int PORT_INDEX_3Y = 6;
  public static final int PORT_INDEX_3A = 7;
  public static final int PORT_INDEX_3B = 8;
  public static final int PORT_INDEX_4Y = 9;
  public static final int PORT_INDEX_4A = 10;
  public static final int PORT_INDEX_4B = 11;

  public Ttl747001() {
    super(_ID);
  }
}
