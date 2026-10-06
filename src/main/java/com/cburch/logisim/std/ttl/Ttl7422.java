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
 * TTL 74x22: dual 4-input NAND gate with open-collector outputs.
 *
 * <p>Model based on the
 * <a href="http://www.cs.trinity.edu/~knickels/class/data_sheets/m7400ls/sn74ls22rev5.pdf">SN74LS22
 * datasheet</a>. Each gate drives its output low only when all four inputs are high and releases
 * the output for every other determined combination. A released output is {@link Value#UNKNOWN},
 * so a pull-up in the circuit can raise it. An unknown or error input leaves the output in error,
 * unless another input is low: a low input releases the output regardless of the remaining inputs.
 * Input thresholds are not modeled. There is no HDL model: an open-collector high-impedance output
 * is not a push-pull gate network.
 */
public class Ttl7422 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7422";

  public static final int PORT_INDEX_1A = 0;
  public static final int PORT_INDEX_1B = 1;
  public static final int PORT_INDEX_1C = 2;
  public static final int PORT_INDEX_1D = 3;
  public static final int PORT_INDEX_1Y = 4;
  public static final int PORT_INDEX_2Y = 5;
  public static final int PORT_INDEX_2A = 6;
  public static final int PORT_INDEX_2B = 7;
  public static final int PORT_INDEX_2C = 8;
  public static final int PORT_INDEX_2D = 9;

  private static final int DELAY = 1;
  private static final byte PIN_COUNT = 14;
  private static final byte[] OUTPUT_PINS = {6, 8};
  private static final byte[] UNUSED_PINS = {3, 11};
  private static final String[] PORT_NAMES = {
    "1A", "1B", "1C", "1D", "1Y", "2Y", "2A", "2B", "2C", "2D"
  };
  private static final int[][] GATES = {
    {PORT_INDEX_1A, PORT_INDEX_1B, PORT_INDEX_1C, PORT_INDEX_1D, PORT_INDEX_1Y},
    {PORT_INDEX_2A, PORT_INDEX_2B, PORT_INDEX_2C, PORT_INDEX_2D, PORT_INDEX_2Y}
  };

  public Ttl7422() {
    super(_ID, PIN_COUNT, OUTPUT_PINS, UNUSED_PINS, null, PORT_NAMES, false, DEFAULT_HEIGHT, null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    final var g = painter.getGraphics();
    Drawgates.paintAnd(g, x + 117, y + 20, 10, 10, true);
    Drawgates.paintAnd(g, x + 97, y + 40, 10, 10, true);
    Drawgates.paintOpenCollector(g, x + 122, y + 20);
    Drawgates.paintOpenCollector(g, x + 102, y + 40);
    g.drawLine(x + 128, y + 20, x + 130, y + 20);
    g.drawLine(x + 130, y + AbstractTtlGate.PIN_HEIGHT, x + 130, y + 20);
    g.drawLine(x + 108, y + 40, x + 110, y + 40);
    g.drawLine(x + 110, y + height - AbstractTtlGate.PIN_HEIGHT, x + 110, y + 40);
    for (var i = 0; i < 5; i++) {
      if (i != 2) {
        g.drawLine(
            x + 10 + i * 20,
            y + height - AbstractTtlGate.PIN_HEIGHT,
            x + 10 + i * 20,
            y + 36 + i * 2);
        g.drawLine(x + 10 + i * 20, y + 36 + i * 2, x + 87, y + 36 + i * 2);
        g.drawLine(
            x + 30 + i * 20, y + AbstractTtlGate.PIN_HEIGHT, x + 30 + i * 20, y + 24 - i * 2);
        g.drawLine(x + 30 + i * 20, y + 24 - i * 2, x + 107, y + 24 - i * 2);
      }
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    for (final var gate : GATES) {
      state.setPort(
          gate[4],
          openCollectorNand(
              state.getPortValue(gate[0]),
              state.getPortValue(gate[1]),
              state.getPortValue(gate[2]),
              state.getPortValue(gate[3])),
          DELAY);
    }
  }

  /**
   * Drives low only when every input is high. A low input releases the pin; any other unsettled
   * combination leaves the output in error.
   */
  private static Value openCollectorNand(Value inputA, Value inputB, Value inputC, Value inputD) {
    if (inputA == Value.FALSE
        || inputB == Value.FALSE
        || inputC == Value.FALSE
        || inputD == Value.FALSE) {
      return Value.UNKNOWN;
    }
    if (inputA == Value.TRUE
        && inputB == Value.TRUE
        && inputC == Value.TRUE
        && inputD == Value.TRUE) {
      return Value.FALSE;
    }
    return Value.ERROR;
  }
}
