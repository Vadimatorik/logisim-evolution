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
 * TTL 74x16: hex inverter with open-collector outputs.
 *
 * <p>Simulation follows the Texas Instruments SN7416 function table (SDLS031A, December 2001).
 * Each inverter performs Y = not A in positive logic. A high input pulls the open-collector output
 * low, and a low input releases it. The released state is {@link Value#UNKNOWN} so an external
 * pull resistor can raise it. An unknown input releases the output as well, because the sink and
 * release results disagree. An error input produces an error output.
 *
 * <p>The 15 V output rating that distinguishes the 7416 from the 7406 is not modeled. There is no
 * separate 74HC16 function table; a 74HC06 with this pinout has the same digital behavior.
 */
public class Ttl7416 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7416";

  public static final int DELAY = 1;

  public static final int PORT_1A = 0;
  public static final int PORT_1Y = 1;
  public static final int PORT_2A = 2;
  public static final int PORT_2Y = 3;
  public static final int PORT_3A = 4;
  public static final int PORT_3Y = 5;
  public static final int PORT_4Y = 6;
  public static final int PORT_4A = 7;
  public static final int PORT_5Y = 8;
  public static final int PORT_5A = 9;
  public static final int PORT_6Y = 10;
  public static final int PORT_6A = 11;

  private static final byte[] OUTPUTS = {2, 4, 6, 8, 10, 12};

  private static final String[] PORT_NAMES = {
    "1A", "1Y", "2A", "2Y", "3A", "3Y", "4Y", "4A", "5Y", "5A", "6Y", "6A"
  };

  private static final int[] INPUT_PORTS = {
    PORT_1A, PORT_2A, PORT_3A, PORT_4A, PORT_5A, PORT_6A
  };

  private static final int[] OUTPUT_PORTS = {
    PORT_1Y, PORT_2Y, PORT_3Y, PORT_4Y, PORT_5Y, PORT_6Y
  };

  /** Creates a 7416 hex open-collector inverter. */
  public Ttl7416() {
    super(
        _ID,
        (byte) 14,
        OUTPUTS,
        null,
        null,
        PORT_NAMES,
        true,
        DEFAULT_HEIGHT,
        new Ttl7416HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    final var portWidth = 12;
    final var portHeight = 6;
    final var yOutput = y + (up ? 20 : 40);
    Drawgates.paintNot(g, x + 26, yOutput, portWidth, portHeight);
    Drawgates.paintOpenCollector(g, x + 30, yOutput);
    Drawgates.paintOutputgate(g, x + 30, y, x + 26, yOutput, up, height);
    Drawgates.paintSingleInputgate(g, x + 10, y, x + 26 - portWidth, yOutput, up, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    for (var gate = 0; gate < INPUT_PORTS.length; gate++) {
      state.setPort(OUTPUT_PORTS[gate], openCollector(state.getPortValue(INPUT_PORTS[gate])), DELAY);
    }
  }

  /**
   * Maps one input onto an open-collector output.
   *
   * @param input inverter input
   * @return low when the input is high, error when the input is an error, otherwise released
   */
  private static Value openCollector(Value input) {
    if (input == Value.TRUE) return Value.FALSE;
    if (input == Value.ERROR) return Value.ERROR;
    return Value.UNKNOWN;
  }
}
