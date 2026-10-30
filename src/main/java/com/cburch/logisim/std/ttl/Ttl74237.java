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
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x237: 3-to-8 line decoder/demultiplexer with address latches.
 *
 * <p>Model based on the <a href="https://assets.nexperia.com/documents/data-sheet/74HC237.pdf">74HC237
 * datasheet</a> function table. While nLE is low the address latch is transparent. The low-to-high
 * transition of nLE keeps the address that was already stored, and later address changes are
 * ignored until nLE is low again. The outputs are active high and mutually exclusive. They leave
 * the low level only when nE1 is low and E2 is high. Those two enable pins act on the outputs
 * directly and are not stored in the latch.
 *
 * <p>An address bit is copied only when it reads exactly high or low. Any other level makes the
 * stored address undefined, and an enabled device then drives every output to an error. An enable
 * pin is active only at its exact active level, so an unconnected device keeps every output low.
 */
public class Ttl74237 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74237";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  public static final byte A0 = 1;
  public static final byte A1 = 2;
  public static final byte A2 = 3;
  public static final byte LE = 4;
  public static final byte E1 = 5;
  public static final byte E2 = 6;

  public static final byte Y7 = 7;
  public static final byte Y6 = 9;
  public static final byte Y5 = 10;
  public static final byte Y4 = 11;
  public static final byte Y3 = 12;
  public static final byte Y2 = 13;
  public static final byte Y1 = 14;
  public static final byte Y0 = 15;

  public static final byte GND = 8;
  public static final byte VCC = 16;

  /** Address inputs, with the least significant bit first. */
  private static final byte[] ADDRESS = {A0, A1, A2};

  /** Outputs in the order of the address they decode. */
  private static final byte[] OUTPUTS = {Y0, Y1, Y2, Y3, Y4, Y5, Y6, Y7};

  private static final String[] PORT_NAMES = {
    "A0 Address (LSB)",
    "A1 Address",
    "A2 Address (MSB)",
    "nLE Latch enable (active low)",
    "nE1 Output enable (active low)",
    "E2 Output enable (active high)",
    "Y7",
    "Y6",
    "Y5",
    "Y4",
    "Y3",
    "Y2",
    "Y1",
    "Y0"
  };

  /** Creates a 74237 3-to-8 line decoder/demultiplexer with address latches. */
  public Ttl74237() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74237HdlGenerator());
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
    g.drawRect(x + 25, y + 12, 10, 10);
    g.drawRect(x + 45, y + 12, 10, 10);
    g.drawRect(x + 65, y + 12, 10, 10);
    g.drawRect(x + 85, y + 12, 10, 10);
    g.drawRect(x + 105, y + 12, 10, 10);
    g.drawRect(x + 125, y + 12, 10, 10);
    g.drawRect(x + 145, y + 12, 10, 10);
    g.drawRect(x + 125, y + 28, 10, 10);
    g.drawLine(x + 30, y + 12, x + 30, y + 7);
    g.drawLine(x + 50, y + 12, x + 50, y + 7);
    g.drawLine(x + 70, y + 12, x + 70, y + 7);
    g.drawLine(x + 90, y + 12, x + 90, y + 7);
    g.drawLine(x + 110, y + 12, x + 110, y + 7);
    g.drawLine(x + 130, y + 12, x + 130, y + 7);
    g.drawLine(x + 150, y + 12, x + 150, y + 7);
    g.drawLine(x + 130, y + height - 22, x + 130, y + height - 7);
    g.drawLine(x + 25, y + 24, x + 155, y + 24);
    g.drawLine(x + 10, y + height - 7, x + 10, y + height - 9);
    g.drawLine(x + 10, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 24);
    g.drawLine(x + 21, y + 24, x + 25, y + 24);
    g.drawLine(x + 27, y + 24, x + 27, y + 22);
    g.drawLine(x + 47, y + 24, x + 47, y + 22);
    g.drawLine(x + 67, y + 24, x + 67, y + 22);
    g.drawLine(x + 87, y + 24, x + 87, y + 22);
    g.drawLine(x + 107, y + 24, x + 107, y + 22);
    g.drawLine(x + 127, y + 24, x + 127, y + 22);
    g.drawLine(x + 147, y + 24, x + 147, y + 22);
    g.drawLine(x + 127, y + 24, x + 127, y + 28);
    g.drawLine(x + 25, y + 24, x + 155, y + 24);
    g.drawLine(x + 30, y + height - 7, x + 30, y + height - 9);
    g.drawLine(x + 30, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 24);
    g.drawLine(x + 21, y + 24, x + 25, y + 24);
    g.drawLine(x + 30, y + 24, x + 30, y + 22);
    g.drawLine(x + 50, y + 24, x + 50, y + 22);
    g.drawLine(x + 70, y + 24, x + 70, y + 22);
    g.drawLine(x + 90, y + 24, x + 90, y + 22);
    g.drawLine(x + 110, y + 24, x + 110, y + 22);
    g.drawLine(x + 130, y + 24, x + 130, y + 22);
    g.drawLine(x + 150, y + 24, x + 150, y + 22);
    g.drawLine(x + 130, y + 24, x + 130, y + 28);
    g.drawLine(x + 25, y + 25, x + 155, y + 25);
    g.drawLine(x + 50, y + height - 7, x + 50, y + height - 9);
    g.drawLine(x + 50, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 25);
    g.drawLine(x + 21, y + 25, x + 25, y + 25);
    g.drawLine(x + 33, y + 25, x + 33, y + 22);
    g.drawLine(x + 53, y + 25, x + 53, y + 22);
    g.drawLine(x + 73, y + 25, x + 73, y + 22);
    g.drawLine(x + 93, y + 25, x + 93, y + 22);
    g.drawLine(x + 113, y + 25, x + 113, y + 22);
    g.drawLine(x + 133, y + 25, x + 133, y + 22);
    g.drawLine(x + 153, y + 25, x + 153, y + 22);
    g.drawLine(x + 133, y + 25, x + 133, y + 28);
    g.drawLine(x + 25, y + 25, x + 155, y + 25);
    g.drawLine(x + 70, y + height - 7, x + 70, y + height - 9);
    g.drawLine(x + 70, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 25);
    g.drawLine(x + 21, y + 25, x + 25, y + 25);
    g.drawLine(x + 27, y + 25, x + 27, y + 22);
    g.drawLine(x + 47, y + 25, x + 47, y + 22);
    g.drawLine(x + 67, y + 25, x + 67, y + 22);
    g.drawLine(x + 87, y + 25, x + 87, y + 22);
    g.drawLine(x + 107, y + 25, x + 107, y + 22);
    g.drawLine(x + 127, y + 25, x + 127, y + 22);
    g.drawLine(x + 147, y + 25, x + 147, y + 22);
    g.drawLine(x + 127, y + 25, x + 127, y + 28);
    g.drawLine(x + 25, y + 25, x + 155, y + 25);
    g.drawLine(x + 90, y + height - 7, x + 90, y + height - 9);
    g.drawLine(x + 90, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 25);
    g.drawLine(x + 21, y + 25, x + 25, y + 25);
    g.drawLine(x + 30, y + 25, x + 30, y + 22);
    g.drawLine(x + 50, y + 25, x + 50, y + 22);
    g.drawLine(x + 70, y + 25, x + 70, y + 22);
    g.drawLine(x + 90, y + 25, x + 90, y + 22);
    g.drawLine(x + 110, y + 25, x + 110, y + 22);
    g.drawLine(x + 130, y + 25, x + 130, y + 22);
    g.drawLine(x + 150, y + 25, x + 150, y + 22);
    g.drawLine(x + 130, y + 25, x + 130, y + 28);
    g.drawLine(x + 110, y + height - 7, x + 110, y + 25);
    g.drawLine(x + 110, y + 25, x + 67, y + 25);
    g.drawLine(x + 67, y + 25, x + 67, y + 22);
    g.drawOval(x + 19, y + 23, 4, 4);
    g.drawOval(x + 19, y + 23, 4, 4);
    GraphicsUtil.drawCenteredText(g, "0", x + 30, y + 17);
    GraphicsUtil.drawCenteredText(g, "1", x + 50, y + 17);
    GraphicsUtil.drawCenteredText(g, "2", x + 70, y + 17);
    GraphicsUtil.drawCenteredText(g, "3", x + 90, y + 17);
    GraphicsUtil.drawCenteredText(g, "4", x + 110, y + 17);
    GraphicsUtil.drawCenteredText(g, "5", x + 130, y + 17);
    GraphicsUtil.drawCenteredText(g, "6", x + 150, y + 17);
    GraphicsUtil.drawCenteredText(g, "7", x + 130, y + height - 27);
    g.setFont(previousFont);
  }



  private static Value definedLevel(InstanceState state, byte dsPinNr) {
    final var value = state.getPortValue(pinNrToPortNr(dsPinNr));
    if (value == Value.TRUE || value == Value.FALSE) {
      return value;
    }
    return Value.ERROR;
  }

  private static boolean isEnabled(InstanceState state) {
    return state.getPortValue(pinNrToPortNr(E1)) == Value.FALSE
        && state.getPortValue(pinNrToPortNr(E2)) == Value.TRUE;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.create(ADDRESS.length));
      state.setData(data);
    }

    // Sample only while nLE is low. A propagation that already sees nLE high therefore keeps
    // the address stored on the previous propagation, which is the value present before the edge.
    if (state.getPortValue(pinNrToPortNr(LE)) == Value.FALSE) {
      final var bits = new Value[ADDRESS.length];
      for (var index = 0; index < ADDRESS.length; index++) {
        bits[index] = definedLevel(state, ADDRESS[index]);
      }
      data.setValue(Value.create(bits));
    }

    final var address = data.getValue();
    final var enabled = isEnabled(state);
    final var code = address.isFullyDefined() ? (int) address.toLongValue() : -1;
    for (var index = 0; index < OUTPUTS.length; index++) {
      final Value output;
      if (!enabled) {
        output = Value.FALSE;
      } else if (code < 0) {
        output = Value.ERROR;
      } else {
        output = index == code ? Value.TRUE : Value.FALSE;
      }
      state.setPort(pinNrToPortNr(OUTPUTS[index]), output, DELAY);
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
