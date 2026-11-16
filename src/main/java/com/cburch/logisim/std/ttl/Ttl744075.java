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

import java.awt.Graphics;

/**
 * TTL 74x4075: triple 3-input OR gate.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT4075.pdf">Nexperia
 * 74HC4075/74HCT4075</a> data sheet, Rev. 6, 21 March 2024. Each output is the OR of its three
 * inputs. The 74HCT4075 uses the same function table and pinout; CMOS and TTL input thresholds are
 * not modeled. The package is 14 pins with GND on pin 7 and VCC on pin 14. This pinout is not the
 * same as the 7410, 7411, or 7427.
 */
public class Ttl744075 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744075";

  public static final int PORT_INDEX_2A = 0;
  public static final int PORT_INDEX_2B = 1;
  public static final int PORT_INDEX_1A = 2;
  public static final int PORT_INDEX_1B = 3;
  public static final int PORT_INDEX_1C = 4;
  public static final int PORT_INDEX_1Y = 5;
  public static final int PORT_INDEX_2C = 6;
  public static final int PORT_INDEX_2Y = 7;
  public static final int PORT_INDEX_3Y = 8;
  public static final int PORT_INDEX_3A = 9;
  public static final int PORT_INDEX_3B = 10;
  public static final int PORT_INDEX_3C = 11;

  private static final int DELAY = 1;
  private static final byte[] OUTPUT_PINS = {6, 9, 10};
  private static final String[] PORT_NAMES = {
    "2A", "2B", "1A", "1B", "1C", "1Y", "2C", "2Y", "3Y", "3A", "3B", "3C"
  };

  /** Creates a 744075 triple 3-input OR gate. */
  public Ttl744075() {
    super(_ID, (byte) 14, OUTPUT_PINS, PORT_NAMES, new Ttl744075HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    final var g = painter.getGraphics();
    final var top = y + PIN_HEIGHT;
    final var bottom = y + height - PIN_HEIGHT;
    // Gate 3 and gate 2 sit 4px below the top edge. Gate 1 stops 4px above the
    // bottom edge. Gate 2 faces left; all three inputs use the right edge.
    Drawgates.paintOr(g, x + 88, y + 18, 16, 15, false, false);
    Drawgates.paintOr(g, x + 108, y + 41, 16, 15, false, false);
    Drawgates.paintOr(g, x + 112, y + 18, 16, 15, false, true);

    wire(g, x + 70, top, x + 70, y + 14, x + 72, y + 14);
    wire(g, x + 50, top, x + 50, y + 20, x + 72, y + 20);
    wire(g, x + 30, top, x + 30, y + 24, x + 72, y + 24);
    wire(g, x + 90, top, x + 90, y + 18, x + 88, y + 18);

    wire(g, x + 50, bottom, x + 50, y + 39, x + 92, y + 39);
    wire(g, x + 70, bottom, x + 70, y + 43, x + 92, y + 43);
    wire(g, x + 90, bottom, x + 90, y + 47, x + 92, y + 47);
    wire(g, x + 110, bottom, x + 110, y + 41, x + 108, y + 41);

    wire(g, x + 110, top, x + 110, y + 18, x + 112, y + 18);
    wire(g, x + 130, top, x + 130, y + 14, x + 128, y + 14);
    wire(g, x + 10, bottom, x + 10, y + 28, x + 130, y + 28, x + 130, y + 24, x + 128, y + 24);
    wire(g, x + 30, bottom, x + 30, y + 32, x + 134, y + 32, x + 134, y + 20, x + 128, y + 20);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    state.setPort(PORT_INDEX_1Y, or3(state, PORT_INDEX_1A, PORT_INDEX_1B, PORT_INDEX_1C), DELAY);
    state.setPort(PORT_INDEX_2Y, or3(state, PORT_INDEX_2A, PORT_INDEX_2B, PORT_INDEX_2C), DELAY);
    state.setPort(PORT_INDEX_3Y, or3(state, PORT_INDEX_3A, PORT_INDEX_3B, PORT_INDEX_3C), DELAY);
  }

  private static Value or3(InstanceState state, int portA, int portB, int portC) {
    return state.getPortValue(portA).or(state.getPortValue(portB)).or(state.getPortValue(portC));
  }

  /** Orthogonal wire through the given points. The first point is the package pin. */
  private static void wire(Graphics g, int... points) {
    for (var i = 2; i < points.length; i += 2) {
      g.drawLine(points[i - 2], points[i - 1], points[i], points[i + 1]);
    }
  }
}
