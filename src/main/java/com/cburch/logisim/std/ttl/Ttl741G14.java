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
 * TTL 74x1G14: single inverter with a Schmitt-trigger input.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT1G14.pdf">74HC1G14; 74HCT1G14</a>
 * function table (Nexperia, Rev. 8, 5 December 2023). Pin 1 is not connected. Pin 2 is input
 * {@code A}, pin 4 is output {@code Y}, pin 3 is GND and pin 5 is VCC. The same pinout is used by
 * the TSSOP5 (SOT353-1) and SC-74A (SOT753) packages. A low input produces a high output, and a
 * high input produces a low output.
 *
 * <p>The Schmitt-trigger thresholds and hysteresis are analog. This model is a digital inverter,
 * the same simplification used for the 7414.
 */
public class Ttl741G14 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "741G14";

  public static final int PORT_INDEX_A = 0;
  public static final int PORT_INDEX_Y = 1;

  private static final byte[] OUTPUT_PORTS = {4};
  private static final byte[] UNUSED_PINS = {1};
  private static final String[] PORT_NAMES = {"A", "Y"};
  private static final int DELAY = 1;

  /** Creates a 741G14 single inverting Schmitt trigger. */
  public Ttl741G14() {
    super(
        _ID,
        (byte) 5,
        OUTPUT_PORTS,
        UNUSED_PINS,
        PORT_NAMES,
        (byte) 5,
        (byte) 3,
        (byte) 3,
        new Ttl741G14HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    final var portWidth = 12;
    final var portHeight = 6;
    final var yOutput = y + height / 2;
    final var xPin = x + 30;
    final var xGate = xPin + 16;
    Drawgates.paintNot(g, xGate, yOutput, portWidth, portHeight);
    Drawgates.paintSingleInputgate(g, xPin, y, xGate - portWidth, yOutput, false, height);
    Drawgates.paintOutputgate(g, xPin, y, xGate, yOutput, true, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    state.setPort(PORT_INDEX_Y, state.getPortValue(PORT_INDEX_A).not(), DELAY);
  }
}
