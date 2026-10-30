/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x147: 10-line to 4-line BCD priority encoder.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/cd74hc147.pdf">TI CD74HC147</a> and
 * <a href="https://media.digikey.com/pdf/Data%20Sheets/NXP%20PDFs/74HC%28T%29147.pdf">NXP 74HC147</a>
 * function tables. Nine active-low inputs encode decimal 1 through 9. {@code nI9} has the highest
 * priority and {@code nI1} the lowest. The four active-low outputs carry the complement of that
 * BCD number. When every data input is high, the implied decimal zero forces every output high.
 * Pin 15 has no internal connection. Nexperia names the inputs {@code A0} to {@code A8}, where
 * {@code A0} is this model's {@code nI1} and {@code A8} is {@code nI9}.
 *
 * <p>A pin is treated as asserted only when it reads exactly {@link Value#FALSE}. Unknown and
 * error values are treated as the inactive high level, so an unconnected device encodes zero.
 */
public class Ttl74147 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74147";

  public static final int DELAY = 1;

  public static final byte I4 = 1;
  public static final byte I5 = 2;
  public static final byte I6 = 3;
  public static final byte I7 = 4;
  public static final byte I8 = 5;
  public static final byte Y2 = 6;
  public static final byte Y1 = 7;
  public static final byte GND = 8;
  public static final byte Y0 = 9;
  public static final byte I9 = 10;
  public static final byte I1 = 11;
  public static final byte I2 = 12;
  public static final byte I3 = 13;
  public static final byte Y3 = 14;
  public static final byte NC = 15;
  public static final byte VCC = 16;

  /** Data inputs ordered by the decimal number they encode, so the last entry wins. */
  private static final byte[] INPUTS = new byte[] {I1, I2, I3, I4, I5, I6, I7, I8, I9};

  private static final byte[] OUTPUTS = new byte[] {Y2, Y1, Y0, Y3};
  private static final byte[] UNUSED_PINS = new byte[] {NC};

  private static final String[] PORT_NAMES = {
    "nI4 / A3 data input 4",
    "nI5 / A4 data input 5",
    "nI6 / A5 data input 6",
    "nI7 / A6 data input 7",
    "nI8 / A7 data input 8",
    "nY2 code output",
    "nY1 code output",
    "nY0 code output (LSB)",
    "nI9 / A8 data input 9 (highest priority)",
    "nI1 / A0 data input 1 (lowest priority)",
    "nI2 / A1 data input 2",
    "nI3 / A2 data input 3",
    "nY3 code output (MSB)"
  };

  /** Creates a 74147 10-line to 4-line priority encoder. */
  public Ttl74147() {
    super(_ID, (byte) 16, OUTPUTS, UNUSED_PINS, PORT_NAMES, new Ttl74147HdlGenerator());
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Power pins and the unconnected pin 15 are omitted from the port list. Only data and code
   * pins may be passed here.
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    return (byte) (dsPinNr < GND ? dsPinNr - 1 : dsPinNr - 2);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 8));
    g.drawRect(x + 105, y + 28, 10, 10);
    g.drawRect(x + 125, y + 28, 10, 10);
    g.drawRect(x + 145, y + 12, 10, 10);
    g.drawRect(x + 45, y + 12, 10, 10);
    g.drawLine(x + 110, y + height - 22, x + 110, y + height - 7);
    g.drawLine(x + 130, y + height - 22, x + 130, y + height - 7);
    g.drawLine(x + 150, y + 12, x + 150, y + 7);
    g.drawLine(x + 50, y + 12, x + 50, y + 7);
    g.drawLine(x + 10, y + height - 7, x + 10, y + height - 19);
    g.drawLine(x + 10, y + height - 19, x + 107, y + height - 19);
    g.drawLine(x + 107, y + height - 19, x + 107, y + height - 22);
    g.drawLine(x + 30, y + height - 7, x + 30, y + height - 19);
    g.drawLine(x + 30, y + height - 19, x + 127, y + height - 19);
    g.drawLine(x + 127, y + height - 19, x + 127, y + height - 22);
    g.drawLine(x + 50, y + height - 7, x + 50, y + 25);
    g.drawLine(x + 50, y + 25, x + 147, y + 25);
    g.drawLine(x + 147, y + 25, x + 147, y + 22);
    g.drawLine(x + 70, y + height - 7, x + 70, y + 25);
    g.drawLine(x + 70, y + 25, x + 47, y + 25);
    g.drawLine(x + 47, y + 25, x + 47, y + 22);
    GraphicsUtil.drawCenteredText(g, "t", x + 110, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "t", x + 130, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "t", x + 150, y + 17);
    GraphicsUtil.drawCenteredText(g, "t", x + 50, y + 17);
    g.setFont(previousFont);
  }



  private static boolean isAsserted(InstanceState state, byte dsPinNr) {
    return state.getPortValue(pinNrToPortNr(dsPinNr)) == Value.FALSE;
  }

  private static void setAsserted(InstanceState state, byte dsPinNr, boolean asserted) {
    state.setPort(pinNrToPortNr(dsPinNr), asserted ? Value.FALSE : Value.TRUE, DELAY);
  }

  /**
   * Finds the decimal number of the highest priority asserted data input.
   *
   * @return the number to encode, or 0 when no input is asserted
   */
  private static int selectedInput(InstanceState state) {
    for (var index = INPUTS.length - 1; index >= 0; index--) {
      if (isAsserted(state, INPUTS[index])) {
        return index + 1;
      }
    }
    return 0;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var code = selectedInput(state);
    setAsserted(state, Y0, (code & 1) != 0);
    setAsserted(state, Y1, (code & 2) != 0);
    setAsserted(state, Y2, (code & 4) != 0);
    setAsserted(state, Y3, (code & 8) != 0);
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
