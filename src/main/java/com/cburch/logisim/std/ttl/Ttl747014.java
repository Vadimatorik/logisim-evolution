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
 * TTL 74HC7014: hex non-inverting precision Schmitt-trigger.
 *
 * <p>The digital model follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC7014.pdf">74HC7014</a>
 * function table (Rev. 6, 2 April 2024): each output copies its input. The Schmitt-trigger window
 * between {@code 0.55 × VCC} and {@code 0.65 × VCC} is analog and is not simulated. Pinning matches
 * the hex buffer: inputs on pins 1, 3, 5, 9, 11 and 13, outputs on pins 2, 4, 6, 8, 10 and 12.
 */
public class Ttl747014 extends Ttl7434 {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "747014";

  public Ttl747014() {
    super(_ID);
  }
}
