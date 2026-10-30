/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x4028: BCD-to-decimal decoder with active-high outputs.
 *
 * <p>Model based on the
 * <a href="https://assets.nexperia.com/documents/data-sheet/HEF4028B.pdf">Nexperia HEF4028B</a>
 * pinout and the Toshiba TC74HC4028 / ST M74HC4028 function table. Those 74HC parts are pin and
 * function compatible with 4028B. A BCD code on {@code A3 A2 A1 A0} drives the matching {@code Yn}
 * high and the other outputs low. {@code A0} on pin 10 is the least significant bit. Codes 10 to
 * 15 hold every output low, which is also how {@code A3} acts as an active-low enable for a 3-to-8
 * decoder. Nanosecond delays are not modeled.
 *
 * <p>An unknown or error value on any address bit makes every output unknown.
 */
public class Ttl744028 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "744028";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet.
  public static final byte Y4 = 1;
  public static final byte Y2 = 2;
  public static final byte Y0 = 3;
  public static final byte Y7 = 4;
  public static final byte Y9 = 5;
  public static final byte Y5 = 6;
  public static final byte Y6 = 7;
  public static final byte GND = 8;
  public static final byte Y8 = 9;
  public static final byte A0 = 10;
  public static final byte A3 = 11;
  public static final byte A2 = 12;
  public static final byte A1 = 13;
  public static final byte Y1 = 14;
  public static final byte Y3 = 15;
  public static final byte VCC = 16;

  /** Outputs ordered by the BCD code that selects them. */
  private static final byte[] OUTPUTS = {Y0, Y1, Y2, Y3, Y4, Y5, Y6, Y7, Y8, Y9};

  private static final byte[] OUTPUT_PINS = {Y4, Y2, Y0, Y7, Y9, Y5, Y6, Y8, Y1, Y3};

  private static final String[] PORT_NAMES = {
    "Y4",
    "Y2",
    "Y0",
    "Y7",
    "Y9",
    "Y5",
    "Y6",
    "Y8",
    "A0 Address (LSB)",
    "A3 Address (MSB)",
    "A2 Address",
    "A1 Address",
    "Y1",
    "Y3"
  };

  private static final byte[] ADDRESS = {A0, A1, A2, A3};

  /** Creates a 744028 BCD-to-decimal decoder. */
  public Ttl744028() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl744028HdlGenerator());
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

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 8));
    g.drawRect(x + 45, y + 28, 10, 10);
    g.drawRect(x + 45, y + 12, 10, 10);
    g.drawRect(x + 25, y + 28, 10, 10);
    g.drawRect(x + 25, y + 12, 10, 10);
    g.drawRect(x + 5, y + 28, 10, 10);
    g.drawRect(x + 105, y + 28, 10, 10);
    g.drawRect(x + 125, y + 28, 10, 10);
    g.drawRect(x + 65, y + 28, 10, 10);
    g.drawRect(x + 145, y + 12, 10, 10);
    g.drawRect(x + 85, y + 28, 10, 10);
    g.drawLine(x + 50, y + height - 22, x + 50, y + height - 7);
    g.drawLine(x + 50, y + 12, x + 50, y + 7);
    g.drawLine(x + 30, y + height - 22, x + 30, y + height - 7);
    g.drawLine(x + 30, y + 12, x + 30, y + 7);
    g.drawLine(x + 10, y + height - 22, x + 10, y + height - 7);
    g.drawLine(x + 110, y + height - 22, x + 110, y + height - 7);
    g.drawLine(x + 130, y + height - 22, x + 130, y + height - 7);
    g.drawLine(x + 70, y + height - 22, x + 70, y + height - 7);
    g.drawLine(x + 150, y + 12, x + 150, y + 7);
    g.drawLine(x + 90, y + height - 22, x + 90, y + height - 7);
    g.drawLine(x + 5, y + 24, x + 155, y + 24);
    g.drawLine(x + 130, y + 7, x + 130, y + 9);
    g.drawLine(x + 130, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 24);
    g.drawLine(x + 1, y + 24, x + 5, y + 24);
    g.drawLine(x + 47, y + 24, x + 47, y + 28);
    g.drawLine(x + 47, y + 24, x + 47, y + 22);
    g.drawLine(x + 27, y + 24, x + 27, y + 28);
    g.drawLine(x + 27, y + 24, x + 27, y + 22);
    g.drawLine(x + 7, y + 24, x + 7, y + 28);
    g.drawLine(x + 107, y + 24, x + 107, y + 28);
    g.drawLine(x + 127, y + 24, x + 127, y + 28);
    g.drawLine(x + 67, y + 24, x + 67, y + 28);
    g.drawLine(x + 147, y + 24, x + 147, y + 22);
    g.drawLine(x + 87, y + 24, x + 87, y + 28);
    g.drawLine(x + 5, y + 24, x + 155, y + 24);
    g.drawLine(x + 110, y + 7, x + 110, y + 9);
    g.drawLine(x + 110, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 24);
    g.drawLine(x + 1, y + 24, x + 5, y + 24);
    g.drawLine(x + 50, y + 24, x + 50, y + 28);
    g.drawLine(x + 50, y + 24, x + 50, y + 22);
    g.drawLine(x + 30, y + 24, x + 30, y + 28);
    g.drawLine(x + 30, y + 24, x + 30, y + 22);
    g.drawLine(x + 10, y + 24, x + 10, y + 28);
    g.drawLine(x + 110, y + 24, x + 110, y + 28);
    g.drawLine(x + 130, y + 24, x + 130, y + 28);
    g.drawLine(x + 70, y + 24, x + 70, y + 28);
    g.drawLine(x + 150, y + 24, x + 150, y + 22);
    g.drawLine(x + 90, y + 24, x + 90, y + 28);
    g.drawLine(x + 5, y + 25, x + 155, y + 25);
    g.drawLine(x + 90, y + 7, x + 90, y + 9);
    g.drawLine(x + 90, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 25);
    g.drawLine(x + 1, y + 25, x + 5, y + 25);
    g.drawLine(x + 53, y + 25, x + 53, y + 28);
    g.drawLine(x + 53, y + 25, x + 53, y + 22);
    g.drawLine(x + 33, y + 25, x + 33, y + 28);
    g.drawLine(x + 33, y + 25, x + 33, y + 22);
    g.drawLine(x + 13, y + 25, x + 13, y + 28);
    g.drawLine(x + 113, y + 25, x + 113, y + 28);
    g.drawLine(x + 133, y + 25, x + 133, y + 28);
    g.drawLine(x + 73, y + 25, x + 73, y + 28);
    g.drawLine(x + 153, y + 25, x + 153, y + 22);
    g.drawLine(x + 93, y + 25, x + 93, y + 28);
    g.drawLine(x + 5, y + 25, x + 155, y + 25);
    g.drawLine(x + 70, y + 7, x + 70, y + 9);
    g.drawLine(x + 70, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 25);
    g.drawLine(x + 1, y + 25, x + 5, y + 25);
    g.drawLine(x + 47, y + 25, x + 47, y + 28);
    g.drawLine(x + 47, y + 25, x + 47, y + 22);
    g.drawLine(x + 27, y + 25, x + 27, y + 28);
    g.drawLine(x + 27, y + 25, x + 27, y + 22);
    g.drawLine(x + 7, y + 25, x + 7, y + 28);
    g.drawLine(x + 107, y + 25, x + 107, y + 28);
    g.drawLine(x + 127, y + 25, x + 127, y + 28);
    g.drawLine(x + 67, y + 25, x + 67, y + 28);
    g.drawLine(x + 147, y + 25, x + 147, y + 22);
    g.drawLine(x + 87, y + 25, x + 87, y + 28);
    GraphicsUtil.drawCenteredText(g, "0", x + 50, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "1", x + 50, y + 17);
    GraphicsUtil.drawCenteredText(g, "2", x + 30, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "3", x + 30, y + 17);
    GraphicsUtil.drawCenteredText(g, "4", x + 10, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "5", x + 110, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "6", x + 130, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "7", x + 70, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "8", x + 150, y + 17);
    GraphicsUtil.drawCenteredText(g, "9", x + 90, y + height - 27);
    g.setFont(previousFont);
  }



  @Override
  public void propagateTtl(InstanceState state) {
    var address = 0;
    for (var bit = 0; bit < ADDRESS.length; bit++) {
      final var value = state.getPortValue(pinNrToPortNr(ADDRESS[bit]));
      if (!isBit(value)) {
        setOutputs(state, Value.UNKNOWN, -1);
        return;
      }
      if (value == Value.TRUE) {
        address += 1 << bit;
      }
    }
    setOutputs(state, Value.FALSE, address <= 9 ? address : -1);
  }

  private static boolean isBit(Value value) {
    return value == Value.TRUE || value == Value.FALSE;
  }

  /**
   * Drives every output to {@code inactive}, or high when {@code selected} names that output.
   * {@code selected} of {@code -1} leaves every output at {@code inactive}.
   */
  private static void setOutputs(InstanceState state, Value inactive, int selected) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      final var value = index == selected ? Value.TRUE : inactive;
      state.setPort(pinNrToPortNr(OUTPUTS[index]), value, DELAY);
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
