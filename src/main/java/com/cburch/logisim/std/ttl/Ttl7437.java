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
 * TTL 74x37: quad 2-input NAND buffer.
 *
 * <p>Model based on the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74ls37.pdf">Texas Instruments SN74LS37
 * datasheet</a>. Each output is the NAND of its two inputs, and the DIP-14 pinout matches the
 * 7400. The buffer designation is a higher output current in the bipolar families; drive strength
 * is not modeled. Drawing and HDL generation are inherited from {@link Ttl7400}.
 */
public class Ttl7437 extends Ttl7400 {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7437";

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

  public Ttl7437() {
    super(_ID);
  }
}
