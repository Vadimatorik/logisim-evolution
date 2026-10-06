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
 * TTL 74x1G00: single 2-input NAND gate.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT1G00.pdf">74HC1G00; 74HCT1G00</a>
 * function table (Nexperia, Rev. 8, 20 June 2024). Pin 1 is input {@code B}, pin 2 is input
 * {@code A}, pin 4 is output {@code Y}, pin 3 is GND and pin 5 is VCC. The same pinout is used by
 * the TSSOP5 (SOT353-1), SC-74A (SOT753) and XSON5 (SOT8065-1) packages. The output is low only
 * when both inputs are high.
 *
 * <p>HC and HCT input thresholds are analog and are not distinguished. A low input forces the
 * output high. Any other undefined input makes the output an error, as with the other TTL gates.
 */
public class Ttl741G00 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "741G00";

  public static final int PORT_INDEX_B = 0;
  public static final int PORT_INDEX_A = 1;
  public static final int PORT_INDEX_Y = 2;

  private static final byte[] OUTPUT_PORTS = {4};
  private static final String[] PORT_NAMES = {"B", "A", "Y"};
  private static final int DELAY = 1;

  /** Creates a 741G00 single 2-input NAND gate. */
  public Ttl741G00() {
    super(
        _ID,
        (byte) 5,
        OUTPUT_PORTS,
        null,
        PORT_NAMES,
        (byte) 5,
        (byte) 3,
        (byte) 3,
        new Ttl741G00HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    final var portWidth = 14;
    final var portHeight = 10;
    final var yOutput = y + height / 2;
    final var xAnd = x + 22;
    Drawgates.paintAnd(g, xAnd, yOutput, portWidth, portHeight, true);
    Drawgates.paintOutputgate(g, x + 30, y, xAnd + 4, yOutput, true, height);
    Drawgates.paintDoubleInputgate(
        g, x + 30, y, xAnd - portWidth, yOutput, portHeight, false, false, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var product =
        state.getPortValue(PORT_INDEX_A).and(state.getPortValue(PORT_INDEX_B));
    state.setPort(PORT_INDEX_Y, product.not(), DELAY);
  }
}
