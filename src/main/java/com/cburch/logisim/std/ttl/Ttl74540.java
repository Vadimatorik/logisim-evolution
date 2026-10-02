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
import java.awt.Graphics2D;

/**
 * TTL 74x540: octal inverting buffers and line drivers with three-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74hc540.pdf">SN74HC540</a> and Nexperia 74HC/HCT540
 * data sheets. Both output enables are active low. When each reads exactly low, every output is
 * the complement of its input. Any other enable level, including high, unknown and error, releases
 * every output. DIP-20 and SO20 use the same pin numbers. Texas Instruments names the channels
 * {@code A1} to {@code A8}; Nexperia names the same pins {@code A0} to {@code A7}. Nanosecond
 * delays are not modeled.
 */
public class Ttl74540 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74540";

  public static final int DELAY = 1;

  // Inputs
  public static final byte OE1 = 1;
  public static final byte A1 = 2;
  public static final byte A2 = 3;
  public static final byte A3 = 4;
  public static final byte A4 = 5;
  public static final byte A5 = 6;
  public static final byte A6 = 7;
  public static final byte A7 = 8;
  public static final byte A8 = 9;

  // Outputs
  public static final byte Y8 = 11;
  public static final byte Y7 = 12;
  public static final byte Y6 = 13;
  public static final byte Y5 = 14;
  public static final byte Y4 = 15;
  public static final byte Y3 = 16;
  public static final byte Y2 = 17;
  public static final byte Y1 = 18;
  public static final byte OE2 = 19;

  // Power supply
  public static final byte GND = 10;
  public static final byte VCC = 20;

  private static final byte[] INPUTS = {A1, A2, A3, A4, A5, A6, A7, A8};
  private static final byte[] OUTPUTS = {Y1, Y2, Y3, Y4, Y5, Y6, Y7, Y8};
  private static final byte[] OUTPUT_PINS = {Y8, Y7, Y6, Y5, Y4, Y3, Y2, Y1};
  private static final String[] PORT_NAMES = {
    "nOE1", "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8",
    "Y8", "Y7", "Y6", "Y5", "Y4", "Y3", "Y2", "Y1", "nOE2"
  };

  /** Creates a 74540 octal inverting buffer with three-state outputs. */
  public Ttl74540() {
    super(_ID, (byte) 20, OUTPUT_PINS, PORT_NAMES, new Ttl74540HdlGenerator());
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Power pins are omitted from the port list.
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    return (byte) ((dsPinNr <= GND) ? dsPinNr - 1 : dsPinNr - 2);
  }

  /**
   * Draws one inverting buffer, including the wire from its input pin and the shared enable.
   *
   * @param graphics the graphics context
   * @param x the x coordinate of the tip of the triangle
   * @param y the y coordinate of the tip of the triangle
   * @param top the y coordinate of the top edge of the device
   * @param bottom the y coordinate of the bottom edge of the device
   */
  private void drawBuffer(Graphics2D graphics, int x, int y, int top, int bottom) {
    final var g = (Graphics2D) graphics.create();

    g.drawPolyline(
        new int[] {x - 10, x - 10, x, x},
        new int[] {bottom - AbstractTtlGate.PIN_HEIGHT, y + 20, y + 20, y + 10},
        4);
    g.drawPolyline(
        new int[] {x, x + 5, x - 5, x},
        new int[] {y, y + 10, y + 10, y},
        4);
    g.drawOval(x - 2, y - 5, 4, 4);
    g.drawPolyline(
        new int[] {x + 10, x + 10, x, x},
        new int[] {top + AbstractTtlGate.PIN_HEIGHT, y - 9, y - 9, y - 5},
        4);
    g.drawPolyline(
        new int[] {x - 10, x - 10, x - 3},
        new int[] {y + 15, y + 5, y + 5},
        3);
    g.dispose();
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);

    final var g = (Graphics2D) painter.getGraphics();
    for (var i = 0; i < 8; i++) {
      drawBuffer(g, x + 40 + i * 20, y + 25, y, y + height);
    }

    g.drawPolyline(
        new int[] {x + 10, x + 10, x + 13},
        new int[] {y + height - AbstractTtlGate.PIN_HEIGHT, y + 43, y + 43},
        3);
    g.drawOval(x + 13, y + 42, 2, 2);

    g.drawPolyline(
        new int[] {x + 30, x + 30, x + 10, x + 10, x + 13},
        new int[] {y + AbstractTtlGate.PIN_HEIGHT, y + 20, y + 20, y + 37, y + 37},
        5);
    g.drawOval(x + 13, y + 36, 2, 2);

    g.drawPolyline(
        new int[] {x + 22, x + 15, x + 15, x + 22},
        new int[] {y + 35, y + 35, y + 45, y + 45},
        4);
    g.drawArc(x + 17, y + 35, 10, 10, 270, 180);
    g.drawLine(x + 27, y + 40, x + 170, y + 40);
  }

  /**
   * Inverts a driven level. {@link Value#not()} turns an unknown bit into an error, which would
   * report a conflict on a floating input. An unknown input stays unknown, and an error stays an
   * error.
   */
  private static Value inverted(Value value) {
    if (value == Value.TRUE) {
      return Value.FALSE;
    }
    if (value == Value.FALSE) {
      return Value.TRUE;
    }
    return value;
  }

  private static boolean outputsEnabled(InstanceState state) {
    return state.getPortValue(pinNrToPortNr(OE1)) == Value.FALSE
        && state.getPortValue(pinNrToPortNr(OE2)) == Value.FALSE;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var enabled = outputsEnabled(state);
    for (var channel = 0; channel < INPUTS.length; channel++) {
      final var output = enabled ? inverted(state.getPortValue(pinNrToPortNr(INPUTS[channel]))) : Value.UNKNOWN;
      state.setPort(pinNrToPortNr(OUTPUTS[channel]), output, DELAY);
    }
  }
}
