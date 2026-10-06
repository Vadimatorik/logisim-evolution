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
 * TTL 74x15: triple 3-input AND gate with open-collector outputs.
 *
 * <p>Model based on the
 * <a href="https://www.electronicoscaldas.com/datasheet/DM74LS15_National.pdf">DM74LS15 datasheet</a>
 * and the
 * <a href="https://hallaweb.jlab.org/tech/Detectors/public_html/manuals/chip_specs/M-Z/motorola/74ls15.pdf">SN74LS15
 * datasheet</a>. The DIP pinout matches the 7411. Each gate releases its output only when every
 * input is high and drives the output low when any input is low. A released output is {@link
 * Value#UNKNOWN}, so a pull-up in the circuit can raise it. An unknown or error input leaves the
 * output in error, unless another input is low: a low input drives the output low regardless of
 * the other inputs. There is no HDL model: an open-collector high-impedance output is not a
 * push-pull gate network.
 */
public class Ttl7415 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7415";

  public static final int PORT_INDEX_1A = 0;
  public static final int PORT_INDEX_1B = 1;
  public static final int PORT_INDEX_2A = 2;
  public static final int PORT_INDEX_2B = 3;
  public static final int PORT_INDEX_2C = 4;
  public static final int PORT_INDEX_2Y = 5;
  public static final int PORT_INDEX_3Y = 6;
  public static final int PORT_INDEX_3A = 7;
  public static final int PORT_INDEX_3B = 8;
  public static final int PORT_INDEX_3C = 9;
  public static final int PORT_INDEX_1Y = 10;
  public static final int PORT_INDEX_1C = 11;

  private static final int DELAY = 1;
  private static final byte PIN_COUNT = 14;
  private static final byte[] OUTPUT_PINS = {6, 8, 12};
  private static final String[] PORT_NAMES = {
    "1A", "1B", "2A", "2B", "2C", "2Y", "3Y", "3A", "3B", "3C", "1Y", "1C"
  };
  private static final int[][] GATES = {
    {PORT_INDEX_1A, PORT_INDEX_1B, PORT_INDEX_1C, PORT_INDEX_1Y},
    {PORT_INDEX_2A, PORT_INDEX_2B, PORT_INDEX_2C, PORT_INDEX_2Y},
    {PORT_INDEX_3A, PORT_INDEX_3B, PORT_INDEX_3C, PORT_INDEX_3Y}
  };

  public Ttl7415() {
    super(_ID, PIN_COUNT, OUTPUT_PINS, null, null, PORT_NAMES, false, DEFAULT_HEIGHT, null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    final var g = painter.getGraphics();
    Drawgates.paintAnd(g, x + 45, y + 20, 10, 10, false);
    Drawgates.paintAnd(g, x + 125, y + 20, 10, 10, false);
    Drawgates.paintAnd(g, x + 105, y + 40, 10, 10, false);
    Drawgates.paintOpenCollector(g, x + 45, y + 20);
    Drawgates.paintOpenCollector(g, x + 125, y + 20);
    Drawgates.paintOpenCollector(g, x + 105, y + 40);

    var xpos = new int[] {x + 45, x + 50, x + 50};
    var ypos = new int[] {y + 20, y + 20, y + AbstractTtlGate.PIN_HEIGHT};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 125, x + 130, x + 130};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 105, x + 110, x + 110};
    ypos = new int[] {y + 40, y + 40, y + height - AbstractTtlGate.PIN_HEIGHT};
    g.drawPolyline(xpos, ypos, 3);

    xpos = new int[] {x + 30, x + 30, x + 35};
    ypos = new int[] {y + AbstractTtlGate.PIN_HEIGHT, y + 17, y + 17};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 10, x + 10, x + 35};
    ypos = new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 20, y + 20};
    g.drawPolyline(xpos, ypos, 3);
    xpos = new int[] {x + 30, x + 30, x + 35};
    ypos = new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 23, y + 23};
    g.drawPolyline(xpos, ypos, 3);

    for (var i = 0; i < 3; i++) {
      xpos = new int[] {x + 70 + i * 20, x + 70 + i * 20, x + 115};
      ypos = new int[] {y + AbstractTtlGate.PIN_HEIGHT, y + 23 - i * 3, y + 23 - i * 3};
      g.drawPolyline(xpos, ypos, 3);
      xpos = new int[] {x + 50 + i * 20, x + 50 + i * 20, x + 95};
      ypos = new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 37 + i * 3, y + 37 + i * 3};
      g.drawPolyline(xpos, ypos, 3);
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    for (final var gate : GATES) {
      final var and =
          state
              .getPortValue(gate[0])
              .and(state.getPortValue(gate[1]))
              .and(state.getPortValue(gate[2]));
      state.setPort(gate[3], and == Value.TRUE ? Value.UNKNOWN : and, DELAY);
    }
  }
}
