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
 * TTL 74x126: quad bus buffer with three-state outputs and active-high enable.
 *
 * <p>Model based on the Nexperia 74HC126 function table. A high output-enable makes the output
 * follow the data input. A low, unknown or error enable releases the output into high-impedance,
 * which the simulator represents as {@link Value#UNKNOWN}.
 */
public class Ttl74126 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74126";

  public static final int PORT_INDEX_1OE = 0;
  public static final int PORT_INDEX_1A = 1;
  public static final int PORT_INDEX_1Y = 2;
  public static final int PORT_INDEX_2OE = 3;
  public static final int PORT_INDEX_2A = 4;
  public static final int PORT_INDEX_2Y = 5;
  public static final int PORT_INDEX_3Y = 6;
  public static final int PORT_INDEX_3A = 7;
  public static final int PORT_INDEX_3OE = 8;
  public static final int PORT_INDEX_4Y = 9;
  public static final int PORT_INDEX_4A = 10;
  public static final int PORT_INDEX_4OE = 11;

  private static final int DELAY = 1;

  /** Each row is output-enable, data input, data output, in port-index order. */
  private static final int[][] CHANNELS = {
    {PORT_INDEX_1OE, PORT_INDEX_1A, PORT_INDEX_1Y},
    {PORT_INDEX_2OE, PORT_INDEX_2A, PORT_INDEX_2Y},
    {PORT_INDEX_3OE, PORT_INDEX_3A, PORT_INDEX_3Y},
    {PORT_INDEX_4OE, PORT_INDEX_4A, PORT_INDEX_4Y},
  };

  private static final String[] PORT_NAMES = {
    "1OE", "1A", "1Y", "2OE", "2A", "2Y", "3Y", "3A", "3OE", "4Y", "4A", "4OE",
  };

  /** Creates a 74126 quad bus buffer with active-high three-state outputs. */
  public Ttl74126() {
    super(
        _ID,
        (byte) 14,
        new byte[] {3, 6, 8, 11},
        null,
        null,
        PORT_NAMES,
        true,
        DEFAULT_HEIGHT,
        null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    final var portwidth = 15;
    final var portheight = 8;
    final var youtput = y + (up ? 20 : 40);
    Drawgates.paintBuffer(g, x + 50, youtput, portwidth, portheight);
    Drawgates.paintOutputgate(g, x + 50, y, x + 45, youtput, up, height);
    Drawgates.paintSingleInputgate(g, x + 30, y, x + 35, youtput, up, height);
    // Active-high enable: the control wire meets the buffer with no inversion bubble.
    final var enableY = up ? youtput + 2 : youtput - 2;
    Drawgates.paintSingleInputgate(g, x + 10, y, x + 41, enableY, up, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    for (final var channel : CHANNELS) {
      final var output =
          state.getPortValue(channel[0]) == Value.TRUE
              ? state.getPortValue(channel[1])
              : Value.UNKNOWN;
      state.setPort(channel[2], output, DELAY);
    }
  }
}
