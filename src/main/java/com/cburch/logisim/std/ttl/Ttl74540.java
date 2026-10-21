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
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Graphics;

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
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    g.drawLine((x + 30), (y + height - 7), (x + 30), (y + 30));
    g.drawLine((x + 30), (y + 30), (x + 34), (y + 30));
    g.drawLine((x + 34), (y + 24), (x + 46), (y + 30));
    g.drawLine((x + 34), (y + 36), (x + 46), (y + 30));
    g.drawLine((x + 34), (y + 24), (x + 34), (y + 36));
    structureBubble(g, (x + 48), (y + 30));
    g.drawLine((x + 52), (y + 30), (x + 50), (y + 30));
    g.drawLine((x + 50), (y + 30), (x + 50), (y + 7));
    g.drawLine((x + 50), (y + height - 7), (x + 50), (y + 30));
    g.drawLine((x + 50), (y + 30), (x + 54), (y + 30));
    g.drawLine((x + 54), (y + 24), (x + 66), (y + 30));
    g.drawLine((x + 54), (y + 36), (x + 66), (y + 30));
    g.drawLine((x + 54), (y + 24), (x + 54), (y + 36));
    structureBubble(g, (x + 68), (y + 30));
    g.drawLine((x + 72), (y + 30), (x + 70), (y + 30));
    g.drawLine((x + 70), (y + 30), (x + 70), (y + 7));
    g.drawLine((x + 70), (y + height - 7), (x + 70), (y + 30));
    g.drawLine((x + 70), (y + 30), (x + 74), (y + 30));
    g.drawLine((x + 74), (y + 24), (x + 86), (y + 30));
    g.drawLine((x + 74), (y + 36), (x + 86), (y + 30));
    g.drawLine((x + 74), (y + 24), (x + 74), (y + 36));
    structureBubble(g, (x + 88), (y + 30));
    g.drawLine((x + 92), (y + 30), (x + 90), (y + 30));
    g.drawLine((x + 90), (y + 30), (x + 90), (y + 7));
    g.drawLine((x + 90), (y + height - 7), (x + 90), (y + 30));
    g.drawLine((x + 90), (y + 30), (x + 94), (y + 30));
    g.drawLine((x + 94), (y + 24), (x + 106), (y + 30));
    g.drawLine((x + 94), (y + 36), (x + 106), (y + 30));
    g.drawLine((x + 94), (y + 24), (x + 94), (y + 36));
    structureBubble(g, (x + 108), (y + 30));
    g.drawLine((x + 112), (y + 30), (x + 110), (y + 30));
    g.drawLine((x + 110), (y + 30), (x + 110), (y + 7));
    g.drawLine((x + 110), (y + height - 7), (x + 110), (y + 30));
    g.drawLine((x + 110), (y + 30), (x + 114), (y + 30));
    g.drawLine((x + 114), (y + 24), (x + 126), (y + 30));
    g.drawLine((x + 114), (y + 36), (x + 126), (y + 30));
    g.drawLine((x + 114), (y + 24), (x + 114), (y + 36));
    structureBubble(g, (x + 128), (y + 30));
    g.drawLine((x + 132), (y + 30), (x + 130), (y + 30));
    g.drawLine((x + 130), (y + 30), (x + 130), (y + 7));
    g.drawLine((x + 130), (y + height - 7), (x + 130), (y + 30));
    g.drawLine((x + 130), (y + 30), (x + 134), (y + 30));
    g.drawLine((x + 134), (y + 24), (x + 146), (y + 30));
    g.drawLine((x + 134), (y + 36), (x + 146), (y + 30));
    g.drawLine((x + 134), (y + 24), (x + 134), (y + 36));
    structureBubble(g, (x + 148), (y + 30));
    g.drawLine((x + 152), (y + 30), (x + 150), (y + 30));
    g.drawLine((x + 150), (y + 30), (x + 150), (y + 7));
    g.drawLine((x + 150), (y + height - 7), (x + 150), (y + 30));
    g.drawLine((x + 150), (y + 30), (x + 154), (y + 30));
    g.drawLine((x + 154), (y + 24), (x + 166), (y + 30));
    g.drawLine((x + 154), (y + 36), (x + 166), (y + 30));
    g.drawLine((x + 154), (y + 24), (x + 154), (y + 36));
    structureBubble(g, (x + 168), (y + 30));
    g.drawLine((x + 172), (y + 30), (x + 170), (y + 30));
    g.drawLine((x + 170), (y + 30), (x + 170), (y + 7));
    g.drawLine((x + 170), (y + height - 7), (x + 170), (y + 30));
    g.drawLine((x + 170), (y + 30), (x + 174), (y + 30));
    g.drawLine((x + 174), (y + 24), (x + 186), (y + 30));
    g.drawLine((x + 174), (y + 36), (x + 186), (y + 30));
    g.drawLine((x + 174), (y + 24), (x + 174), (y + 36));
    structureBubble(g, (x + 188), (y + 30));
    g.drawLine((x + 192), (y + 30), (x + 190), (y + 30));
    g.drawLine((x + 190), (y + 30), (x + 190), (y + 7));
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + 22));
    structureBubble(g, (x + 10), (y + height - 7));
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 22));
    structureBubble(g, (x + 30), (y + 7));
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

  private static int structureX(int originX, int pins, int pin) {
    if (pin <= pins / 2) {
      return originX + (pin - 1) * 20 + 10;
    }
    return originX + (pins - pin) * 20 + 10;
  }

  private static int structureY(int originY, int bodyHeight, int pins, int pin) {
    if (pin <= pins / 2) {
      return originY + bodyHeight - AbstractTtlGate.PIN_HEIGHT;
    }
    return originY + AbstractTtlGate.PIN_HEIGHT;
  }

  private static void structureBubble(Graphics g, int centerX, int centerY) {
    g.drawOval(centerX - 2, centerY - 2, 4, 4);
  }

  private static void structureDot(
      Graphics g, int centerX, int centerY, TtlRegisterData data, int word, int bit) {
    if (data == null) {
      return;
    }
    final var value = data.getValue(word);
    if (bit < 0 || bit >= value.getWidth()) {
      return;
    }
    final var bitValue = value.get(bit);
    g.setColor(bitValue.getColor());
    g.fillOval(centerX - 3, centerY - 3, 6, 6);
    g.setColor(Color.WHITE);
    GraphicsUtil.drawCenteredText(g, bitValue.toDisplayString(), centerX, centerY);
    g.setColor(Color.BLACK);
  }

}
