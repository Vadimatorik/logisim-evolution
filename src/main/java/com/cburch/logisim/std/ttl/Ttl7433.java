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
 * TTL 74x33: quad 2-input NOR buffer with open-collector outputs.
 *
 * <p>Model based on the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74ls33.pdf">SN7433, SN74LS33 datasheet</a>.
 * The DIP pinout matches the 7402: outputs are on pins 1, 4, 10 and 13. Each gate drives low when
 * either input is high and releases the output only when both inputs are low. A released output is
 * {@link Value#UNKNOWN}, so a pull-up in the circuit can raise it. An unknown or error input
 * leaves the output in error, unless the other input is high: a high input drives the output low
 * regardless of the second input. There is no HDL model: an open-collector high-impedance output
 * is not a push-pull gate network.
 */
public class Ttl7433 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7433";

  public static final int PORT_INDEX_1Y = 0;
  public static final int PORT_INDEX_1A = 1;
  public static final int PORT_INDEX_1B = 2;
  public static final int PORT_INDEX_2Y = 3;
  public static final int PORT_INDEX_2A = 4;
  public static final int PORT_INDEX_2B = 5;
  public static final int PORT_INDEX_3A = 6;
  public static final int PORT_INDEX_3B = 7;
  public static final int PORT_INDEX_3Y = 8;
  public static final int PORT_INDEX_4A = 9;
  public static final int PORT_INDEX_4B = 10;
  public static final int PORT_INDEX_4Y = 11;

  private static final int DELAY = 1;
  private static final byte PIN_COUNT = 14;
  private static final byte[] OUTPUT_PINS = {1, 4, 10, 13};
  private static final String[] PORT_NAMES = {
    "1Y", "1A", "1B", "2Y", "2A", "2B", "3A", "3B", "3Y", "4A", "4B", "4Y"
  };
  private static final int[][] GATES = {
    {PORT_INDEX_1A, PORT_INDEX_1B, PORT_INDEX_1Y},
    {PORT_INDEX_2A, PORT_INDEX_2B, PORT_INDEX_2Y},
    {PORT_INDEX_3A, PORT_INDEX_3B, PORT_INDEX_3Y},
    {PORT_INDEX_4A, PORT_INDEX_4B, PORT_INDEX_4Y}
  };

  public Ttl7433() {
    super(_ID, PIN_COUNT, OUTPUT_PINS, null, null, PORT_NAMES, true, DEFAULT_HEIGHT, null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    final var portwidth = 18;
    final var portheight = 15;
    final var youtput = y + (up ? 20 : 40);
    Drawgates.paintOr(g, x + 20, youtput, portwidth - 4, portheight, true, true);
    Drawgates.paintOutputgate(g, x + 10, y, x + 16, youtput, up, height);
    Drawgates.paintOpenCollector(g, x + 10, youtput);
    Drawgates.paintDoubleInputgate(
        g, x + 50, y, x + 16 + portwidth, youtput, portheight, up, true, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    for (final var gate : GATES) {
      final var nor = state.getPortValue(gate[0]).or(state.getPortValue(gate[1])).not();
      state.setPort(gate[2], nor == Value.TRUE ? Value.UNKNOWN : nor, DELAY);
    }
  }
}
