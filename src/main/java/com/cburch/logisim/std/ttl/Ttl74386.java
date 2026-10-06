/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x386: quad 2-input exclusive-OR gate.
 *
 * <p>Simulation follows the Renesas HD74HC386 data sheet, Rev.2.00, 30 March 2006. Each output is
 * the exclusive OR of its two inputs. The package is 14 pins with GND on pin 7 and VCC on pin 14.
 * Gates 2 and 3 are not in the same pin order as the 7486: pin 4 is 2Y, pins 5 and 6 are 2A and
 * 2B, pin 8 is 3A, pin 9 is 3B and pin 10 is 3Y. CMOS input thresholds are not modeled.
 */
public class Ttl74386 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74386";

  public static final int PORT_INDEX_1A = 0;
  public static final int PORT_INDEX_1B = 1;
  public static final int PORT_INDEX_1Y = 2;
  public static final int PORT_INDEX_2Y = 3;
  public static final int PORT_INDEX_2A = 4;
  public static final int PORT_INDEX_2B = 5;
  public static final int PORT_INDEX_3A = 6;
  public static final int PORT_INDEX_3B = 7;
  public static final int PORT_INDEX_3Y = 8;
  public static final int PORT_INDEX_4Y = 9;
  public static final int PORT_INDEX_4A = 10;
  public static final int PORT_INDEX_4B = 11;

  private static final int DELAY = 1;
  private static final byte[] OUTPUT_PINS = {3, 4, 10, 11};
  private static final String[] PORT_NAMES = {
    "1A", "1B", "1Y", "2Y", "2A", "2B", "3A", "3B", "3Y", "4Y", "4A", "4B"
  };
  private static final int[][] GATES = {
    {PORT_INDEX_1A, PORT_INDEX_1B, PORT_INDEX_1Y},
    {PORT_INDEX_2A, PORT_INDEX_2B, PORT_INDEX_2Y},
    {PORT_INDEX_3A, PORT_INDEX_3B, PORT_INDEX_3Y},
    {PORT_INDEX_4A, PORT_INDEX_4B, PORT_INDEX_4Y}
  };

  /** Creates a 74386 quad 2-input exclusive-OR gate. */
  public Ttl74386() {
    super(_ID, (byte) 14, OUTPUT_PINS, PORT_NAMES, new Ttl74386HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, true, false);
    Drawgates.paintPortNamesByPin(
        painter,
        x,
        y,
        height,
        new String[] {
          "1A", "1B", "1Y", "2Y", "2A", "2B", null, "3A", "3B", "3Y", "4Y", "4A", "4B", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    for (final var gate : GATES) {
      state.setPort(gate[2], state.getPortValue(gate[0]).xor(state.getPortValue(gate[1])), DELAY);
    }
  }
}
