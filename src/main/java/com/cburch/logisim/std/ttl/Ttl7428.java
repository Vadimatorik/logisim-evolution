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
 * TTL 74x28: quad 2-input NOR buffer.
 *
 * <p>The digital function follows the Texas Instruments SN74LS28 table in
 * <a href="https://www.ti.com/lit/ds/symlink/sn74ls28.pdf">SDLS094</a>: each gate computes
 * Y = NOT (A OR B). The DIP-14 pinout matches the 7402, with outputs on pins 1, 4, 10 and 13.
 * The higher sink current of a NOR buffer is not part of this model.
 */
public class Ttl7428 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7428";

  public static final int DELAY = 1;

  /** Logical port of pin 1, output 1Y. Power pins are omitted from this numbering. */
  public static final int PORT_1Y = 0;

  /** Logical port of pin 2, input 1A. */
  public static final int PORT_1A = 1;

  /** Logical port of pin 3, input 1B. */
  public static final int PORT_1B = 2;

  /** Logical port of pin 4, output 2Y. */
  public static final int PORT_2Y = 3;

  /** Logical port of pin 5, input 2A. */
  public static final int PORT_2A = 4;

  /** Logical port of pin 6, input 2B. */
  public static final int PORT_2B = 5;

  /** Logical port of pin 8, input 3A. */
  public static final int PORT_3A = 6;

  /** Logical port of pin 9, input 3B. */
  public static final int PORT_3B = 7;

  /** Logical port of pin 10, output 3Y. */
  public static final int PORT_3Y = 8;

  /** Logical port of pin 11, input 4A. */
  public static final int PORT_4A = 9;

  /** Logical port of pin 12, input 4B. */
  public static final int PORT_4B = 10;

  /** Logical port of pin 13, output 4Y. */
  public static final int PORT_4Y = 11;

  private static final byte PIN_COUNT = 14;
  private static final byte[] OUTPUT_PINS = {1, 4, 10, 13};
  private static final String[] PORT_NAMES = {
    "1Y", "1A", "1B", "2Y", "2A", "2B", "3A", "3B", "3Y", "4A", "4B", "4Y"
  };

  /** Creates a 7428 quad 2-input NOR buffer. */
  public Ttl7428() {
    super(
        _ID,
        PIN_COUNT,
        OUTPUT_PINS,
        null,
        null,
        PORT_NAMES,
        true,
        DEFAULT_HEIGHT,
        new Ttl7428HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    final var portwidth = 18;
    final var portheight = 15;
    final var youtput = y + (up ? 20 : 40);
    Drawgates.paintOr(g, x + 20, youtput, portwidth - 4, portheight, true, true);
    Drawgates.paintOutputgate(g, x + 10, y, x + 16, youtput, up, height);
    Drawgates.paintDoubleInputgate(
        g, x + 50, y, x + 16 + portwidth, youtput, portheight, up, true, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    state.setPort(PORT_1Y, nor(state, PORT_1A, PORT_1B), DELAY);
    state.setPort(PORT_2Y, nor(state, PORT_2A, PORT_2B), DELAY);
    state.setPort(PORT_3Y, nor(state, PORT_3A, PORT_3B), DELAY);
    state.setPort(PORT_4Y, nor(state, PORT_4A, PORT_4B), DELAY);
  }

  private static Value nor(InstanceState state, int inputA, int inputB) {
    return state.getPortValue(inputA).or(state.getPortValue(inputB)).not();
  }
}
