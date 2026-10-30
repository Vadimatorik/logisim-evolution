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
 * TTL 74x154: 4-line to 16-line decoder/demultiplexer with active-low outputs.
 *
 * <p>Model based on the
 * <a href="https://www.ti.com/lit/ds/symlink/cd74hc154.pdf">TI CD74HC154</a> and
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT154.pdf">Nexperia
 * 74HC154/74HCT154</a> data sheets. While both enables are low, the output selected by
 * {@code A3 A2 A1 A0} is low and the others are high. A high on either enable forces every output
 * high and the address is ignored. Using one enable as data covers the demultiplexer function.
 * Nanosecond delays are not modeled.
 *
 * <p>Pin 18 is {@code E1} in the TI data sheet and {@code E0} in the Nexperia data sheet. Pin 19 is
 * {@code E2} and {@code E1} respectively. Both are active low. This model names them {@code nE1}
 * and {@code nE2} after the TI pin numbers. {@code A0} on pin 23 is the least significant address
 * bit.
 *
 * <p>An unknown or error value on an enable, or on an address bit while the device is enabled,
 * makes every output unknown. A known disable still forces every output high.
 */
public class Ttl74154 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74154";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet.
  public static final byte Y0 = 1;
  public static final byte Y1 = 2;
  public static final byte Y2 = 3;
  public static final byte Y3 = 4;
  public static final byte Y4 = 5;
  public static final byte Y5 = 6;
  public static final byte Y6 = 7;
  public static final byte Y7 = 8;
  public static final byte Y8 = 9;
  public static final byte Y9 = 10;
  public static final byte Y10 = 11;
  public static final byte GND = 12;
  public static final byte Y11 = 13;
  public static final byte Y12 = 14;
  public static final byte Y13 = 15;
  public static final byte Y14 = 16;
  public static final byte Y15 = 17;
  public static final byte NE1 = 18;
  public static final byte NE2 = 19;
  public static final byte A3 = 20;
  public static final byte A2 = 21;
  public static final byte A1 = 22;
  public static final byte A0 = 23;
  public static final byte VCC = 24;

  /** Outputs ordered by the address that selects them. */
  private static final byte[] OUTPUTS = {
    Y0, Y1, Y2, Y3, Y4, Y5, Y6, Y7, Y8, Y9, Y10, Y11, Y12, Y13, Y14, Y15
  };

  private static final byte[] OUTPUT_PINS = {
    Y0, Y1, Y2, Y3, Y4, Y5, Y6, Y7, Y8, Y9, Y10, Y11, Y12, Y13, Y14, Y15
  };

  private static final String[] PORT_NAMES = {
    "Y0",
    "Y1",
    "Y2",
    "Y3",
    "Y4",
    "Y5",
    "Y6",
    "Y7",
    "Y8",
    "Y9",
    "Y10",
    "Y11",
    "Y12",
    "Y13",
    "Y14",
    "Y15",
    "nE1 Enable (active low)",
    "nE2 Enable (active low)",
    "A3 Address (MSB)",
    "A2 Address",
    "A1 Address",
    "A0 Address (LSB)"
  };

  private static final byte[] ADDRESS = {A0, A1, A2, A3};

  /** Creates a 74154 4-line to 16-line decoder/demultiplexer. */
  public Ttl74154() {
    super(_ID, (byte) 24, OUTPUT_PINS, PORT_NAMES, new Ttl74154HdlGenerator());
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
    g.drawRect(x + 5, y + 28, 10, 10);
    g.drawRect(x + 25, y + 28, 10, 10);
    g.drawRect(x + 45, y + 28, 10, 10);
    g.drawRect(x + 65, y + 28, 10, 10);
    g.drawRect(x + 85, y + 28, 10, 10);
    g.drawRect(x + 105, y + 28, 10, 10);
    g.drawRect(x + 125, y + 28, 10, 10);
    g.drawRect(x + 145, y + 28, 10, 10);
    g.drawRect(x + 165, y + 28, 10, 10);
    g.drawRect(x + 185, y + 28, 10, 10);
    g.drawRect(x + 205, y + 28, 10, 10);
    g.drawRect(x + 225, y + 12, 10, 10);
    g.drawRect(x + 205, y + 12, 10, 10);
    g.drawRect(x + 185, y + 12, 10, 10);
    g.drawRect(x + 165, y + 12, 10, 10);
    g.drawRect(x + 145, y + 12, 10, 10);
    g.drawLine(x + 10, y + height - 22, x + 10, y + height - 7);
    g.drawLine(x + 30, y + height - 22, x + 30, y + height - 7);
    g.drawLine(x + 50, y + height - 22, x + 50, y + height - 7);
    g.drawLine(x + 70, y + height - 22, x + 70, y + height - 7);
    g.drawLine(x + 90, y + height - 22, x + 90, y + height - 7);
    g.drawLine(x + 110, y + height - 22, x + 110, y + height - 7);
    g.drawLine(x + 130, y + height - 22, x + 130, y + height - 7);
    g.drawLine(x + 150, y + height - 22, x + 150, y + height - 7);
    g.drawLine(x + 170, y + height - 22, x + 170, y + height - 7);
    g.drawLine(x + 190, y + height - 22, x + 190, y + height - 7);
    g.drawLine(x + 210, y + height - 22, x + 210, y + height - 7);
    g.drawLine(x + 230, y + 12, x + 230, y + 7);
    g.drawLine(x + 210, y + 12, x + 210, y + 7);
    g.drawLine(x + 190, y + 12, x + 190, y + 7);
    g.drawLine(x + 170, y + 12, x + 170, y + 7);
    g.drawLine(x + 150, y + 12, x + 150, y + 7);
    g.drawLine(x + 5, y + 24, x + 235, y + 24);
    g.drawLine(x + 130, y + 7, x + 130, y + 9);
    g.drawLine(x + 130, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 24);
    g.drawLine(x + 1, y + 24, x + 5, y + 24);
    g.drawLine(x + 7, y + 24, x + 7, y + 28);
    g.drawLine(x + 27, y + 24, x + 27, y + 28);
    g.drawLine(x + 47, y + 24, x + 47, y + 28);
    g.drawLine(x + 67, y + 24, x + 67, y + 28);
    g.drawLine(x + 87, y + 24, x + 87, y + 28);
    g.drawLine(x + 107, y + 24, x + 107, y + 28);
    g.drawLine(x + 127, y + 24, x + 127, y + 28);
    g.drawLine(x + 147, y + 24, x + 147, y + 28);
    g.drawLine(x + 167, y + 24, x + 167, y + 28);
    g.drawLine(x + 187, y + 24, x + 187, y + 28);
    g.drawLine(x + 207, y + 24, x + 207, y + 28);
    g.drawLine(x + 227, y + 24, x + 227, y + 22);
    g.drawLine(x + 207, y + 24, x + 207, y + 22);
    g.drawLine(x + 187, y + 24, x + 187, y + 22);
    g.drawLine(x + 167, y + 24, x + 167, y + 22);
    g.drawLine(x + 147, y + 24, x + 147, y + 22);
    g.drawLine(x + 5, y + 24, x + 235, y + 24);
    g.drawLine(x + 110, y + 7, x + 110, y + 9);
    g.drawLine(x + 110, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 24);
    g.drawLine(x + 1, y + 24, x + 5, y + 24);
    g.drawLine(x + 10, y + 24, x + 10, y + 28);
    g.drawLine(x + 30, y + 24, x + 30, y + 28);
    g.drawLine(x + 50, y + 24, x + 50, y + 28);
    g.drawLine(x + 70, y + 24, x + 70, y + 28);
    g.drawLine(x + 90, y + 24, x + 90, y + 28);
    g.drawLine(x + 110, y + 24, x + 110, y + 28);
    g.drawLine(x + 130, y + 24, x + 130, y + 28);
    g.drawLine(x + 150, y + 24, x + 150, y + 28);
    g.drawLine(x + 170, y + 24, x + 170, y + 28);
    g.drawLine(x + 190, y + 24, x + 190, y + 28);
    g.drawLine(x + 210, y + 24, x + 210, y + 28);
    g.drawLine(x + 230, y + 24, x + 230, y + 22);
    g.drawLine(x + 210, y + 24, x + 210, y + 22);
    g.drawLine(x + 190, y + 24, x + 190, y + 22);
    g.drawLine(x + 170, y + 24, x + 170, y + 22);
    g.drawLine(x + 150, y + 24, x + 150, y + 22);
    g.drawLine(x + 5, y + 24, x + 235, y + 24);
    g.drawLine(x + 90, y + 7, x + 90, y + 9);
    g.drawLine(x + 90, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 24);
    g.drawLine(x + 1, y + 24, x + 5, y + 24);
    g.drawLine(x + 13, y + 24, x + 13, y + 28);
    g.drawLine(x + 33, y + 24, x + 33, y + 28);
    g.drawLine(x + 53, y + 24, x + 53, y + 28);
    g.drawLine(x + 73, y + 24, x + 73, y + 28);
    g.drawLine(x + 93, y + 24, x + 93, y + 28);
    g.drawLine(x + 113, y + 24, x + 113, y + 28);
    g.drawLine(x + 133, y + 24, x + 133, y + 28);
    g.drawLine(x + 153, y + 24, x + 153, y + 28);
    g.drawLine(x + 173, y + 24, x + 173, y + 28);
    g.drawLine(x + 193, y + 24, x + 193, y + 28);
    g.drawLine(x + 213, y + 24, x + 213, y + 28);
    g.drawLine(x + 233, y + 24, x + 233, y + 22);
    g.drawLine(x + 213, y + 24, x + 213, y + 22);
    g.drawLine(x + 193, y + 24, x + 193, y + 22);
    g.drawLine(x + 173, y + 24, x + 173, y + 22);
    g.drawLine(x + 153, y + 24, x + 153, y + 22);
    g.drawLine(x + 5, y + 25, x + 235, y + 25);
    g.drawLine(x + 70, y + 7, x + 70, y + 9);
    g.drawLine(x + 70, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 25);
    g.drawLine(x + 1, y + 25, x + 5, y + 25);
    g.drawLine(x + 7, y + 25, x + 7, y + 28);
    g.drawLine(x + 27, y + 25, x + 27, y + 28);
    g.drawLine(x + 47, y + 25, x + 47, y + 28);
    g.drawLine(x + 67, y + 25, x + 67, y + 28);
    g.drawLine(x + 87, y + 25, x + 87, y + 28);
    g.drawLine(x + 107, y + 25, x + 107, y + 28);
    g.drawLine(x + 127, y + 25, x + 127, y + 28);
    g.drawLine(x + 147, y + 25, x + 147, y + 28);
    g.drawLine(x + 167, y + 25, x + 167, y + 28);
    g.drawLine(x + 187, y + 25, x + 187, y + 28);
    g.drawLine(x + 207, y + 25, x + 207, y + 28);
    g.drawLine(x + 227, y + 25, x + 227, y + 22);
    g.drawLine(x + 207, y + 25, x + 207, y + 22);
    g.drawLine(x + 187, y + 25, x + 187, y + 22);
    g.drawLine(x + 167, y + 25, x + 167, y + 22);
    g.drawLine(x + 147, y + 25, x + 147, y + 22);
    g.drawLine(x + 5, y + 25, x + 235, y + 25);
    g.drawLine(x + 50, y + 7, x + 50, y + 9);
    g.drawLine(x + 50, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 25);
    g.drawLine(x + 1, y + 25, x + 5, y + 25);
    g.drawLine(x + 10, y + 25, x + 10, y + 28);
    g.drawLine(x + 30, y + 25, x + 30, y + 28);
    g.drawLine(x + 50, y + 25, x + 50, y + 28);
    g.drawLine(x + 70, y + 25, x + 70, y + 28);
    g.drawLine(x + 90, y + 25, x + 90, y + 28);
    g.drawLine(x + 110, y + 25, x + 110, y + 28);
    g.drawLine(x + 130, y + 25, x + 130, y + 28);
    g.drawLine(x + 150, y + 25, x + 150, y + 28);
    g.drawLine(x + 170, y + 25, x + 170, y + 28);
    g.drawLine(x + 190, y + 25, x + 190, y + 28);
    g.drawLine(x + 210, y + 25, x + 210, y + 28);
    g.drawLine(x + 230, y + 25, x + 230, y + 22);
    g.drawLine(x + 210, y + 25, x + 210, y + 22);
    g.drawLine(x + 190, y + 25, x + 190, y + 22);
    g.drawLine(x + 170, y + 25, x + 170, y + 22);
    g.drawLine(x + 150, y + 25, x + 150, y + 22);
    g.drawLine(x + 5, y + 25, x + 235, y + 25);
    g.drawLine(x + 30, y + 7, x + 30, y + 9);
    g.drawLine(x + 30, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 25);
    g.drawLine(x + 1, y + 25, x + 5, y + 25);
    g.drawLine(x + 13, y + 25, x + 13, y + 28);
    g.drawLine(x + 33, y + 25, x + 33, y + 28);
    g.drawLine(x + 53, y + 25, x + 53, y + 28);
    g.drawLine(x + 73, y + 25, x + 73, y + 28);
    g.drawLine(x + 93, y + 25, x + 93, y + 28);
    g.drawLine(x + 113, y + 25, x + 113, y + 28);
    g.drawLine(x + 133, y + 25, x + 133, y + 28);
    g.drawLine(x + 153, y + 25, x + 153, y + 28);
    g.drawLine(x + 173, y + 25, x + 173, y + 28);
    g.drawLine(x + 193, y + 25, x + 193, y + 28);
    g.drawLine(x + 213, y + 25, x + 213, y + 28);
    g.drawLine(x + 233, y + 25, x + 233, y + 22);
    g.drawLine(x + 213, y + 25, x + 213, y + 22);
    g.drawLine(x + 193, y + 25, x + 193, y + 22);
    g.drawLine(x + 173, y + 25, x + 173, y + 22);
    g.drawLine(x + 153, y + 25, x + 153, y + 22);
    g.drawOval(x + -1, y + 22, 4, 4);
    g.drawOval(x + -1, y + 22, 4, 4);
    GraphicsUtil.drawCenteredText(g, "0", x + 10, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "1", x + 30, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "2", x + 50, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "3", x + 70, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "4", x + 90, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "5", x + 110, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "6", x + 130, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "7", x + 150, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "8", x + 170, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "9", x + 190, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "0", x + 210, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "1", x + 230, y + 17);
    GraphicsUtil.drawCenteredText(g, "2", x + 210, y + 17);
    GraphicsUtil.drawCenteredText(g, "3", x + 190, y + 17);
    GraphicsUtil.drawCenteredText(g, "4", x + 170, y + 17);
    GraphicsUtil.drawCenteredText(g, "5", x + 150, y + 17);
    g.setFont(previousFont);
  }



  @Override
  public void propagateTtl(InstanceState state) {
    final var nEnable1 = state.getPortValue(pinNrToPortNr(NE1));
    final var nEnable2 = state.getPortValue(pinNrToPortNr(NE2));
    if (!isBit(nEnable1) || !isBit(nEnable2)) {
      setOutputs(state, Value.UNKNOWN, -1);
      return;
    }
    if (nEnable1 != Value.FALSE || nEnable2 != Value.FALSE) {
      setOutputs(state, Value.TRUE, -1);
      return;
    }

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
    setOutputs(state, Value.TRUE, address);
  }

  private static boolean isBit(Value value) {
    return value == Value.TRUE || value == Value.FALSE;
  }

  /** Drives every output high, or unknown, except {@code selected} when it is in range. */
  private static void setOutputs(InstanceState state, Value inactive, int selected) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      final var value = index == selected ? Value.FALSE : inactive;
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
