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
 * TTL 74x45: BCD-to-decimal decoder/driver with open-collector outputs.
 *
 * <p>Simulation follows the <a href="https://www.ti.com/lit/ds/symlink/sn7445.pdf">SN5445/SN7445</a>
 * function table (SDLS110, March 1988). There is no 74HC45 data sheet; the HC family uses the
 * push-pull 74HC42 for this pinout. For a valid BCD code the selected output sinks, and the other
 * nine are released. Codes 10 to 15 release every output. A released open-collector output is
 * {@link Value#UNKNOWN}, matching the other open-collector TTL models. An error on any input is
 * copied to every output; any other non-binary input releases every output, because none can be
 * shown to be sinking.
 */
public class Ttl7445 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "7445";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  // Outputs, active low and open collector
  public static final byte O0 = 1;
  public static final byte O1 = 2;
  public static final byte O2 = 3;
  public static final byte O3 = 4;
  public static final byte O4 = 5;
  public static final byte O5 = 6;
  public static final byte O6 = 7;
  public static final byte O7 = 9;
  public static final byte O8 = 10;
  public static final byte O9 = 11;

  // Inputs, A is the least significant bit
  public static final byte D = 12;
  public static final byte C = 13;
  public static final byte B = 14;
  public static final byte A = 15;

  // Power supply
  public static final byte GND = 8;
  public static final byte VCC = 16;

  private static final int CODE_UNKNOWN = -1;
  private static final int CODE_ERROR = -2;

  /** Outputs ordered by the BCD code that pulls them low. */
  private static final byte[] OUTPUTS = {O0, O1, O2, O3, O4, O5, O6, O7, O8, O9};

  /** Inputs ordered from the least significant bit. */
  private static final byte[] INPUTS = {A, B, C, D};

  private static final String[] PORT_NAMES = {
    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
    "D", "C", "B", "A"
  };

  /** Creates a 7445 BCD-to-decimal decoder/driver. */
  public Ttl7445() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl7445HdlGenerator());
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
    g.drawRect(x + 145, y + 12, 10, 10);
    g.drawRect(x + 125, y + 12, 10, 10);
    g.drawRect(x + 105, y + 12, 10, 10);
    g.drawLine(x + 10, y + height - 22, x + 10, y + height - 7);
    g.drawLine(x + 30, y + height - 22, x + 30, y + height - 7);
    g.drawLine(x + 50, y + height - 22, x + 50, y + height - 7);
    g.drawLine(x + 70, y + height - 22, x + 70, y + height - 7);
    g.drawLine(x + 90, y + height - 22, x + 90, y + height - 7);
    g.drawLine(x + 110, y + height - 22, x + 110, y + height - 7);
    g.drawLine(x + 130, y + height - 22, x + 130, y + height - 7);
    g.drawLine(x + 150, y + 12, x + 150, y + 7);
    g.drawLine(x + 130, y + 12, x + 130, y + 7);
    g.drawLine(x + 110, y + 12, x + 110, y + 7);
    g.drawLine(x + 5, y + 24, x + 155, y + 24);
    g.drawLine(x + 90, y + 7, x + 90, y + 9);
    g.drawLine(x + 90, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 24);
    g.drawLine(x + 1, y + 24, x + 5, y + 24);
    g.drawLine(x + 7, y + 24, x + 7, y + 28);
    g.drawLine(x + 27, y + 24, x + 27, y + 28);
    g.drawLine(x + 47, y + 24, x + 47, y + 28);
    g.drawLine(x + 67, y + 24, x + 67, y + 28);
    g.drawLine(x + 87, y + 24, x + 87, y + 28);
    g.drawLine(x + 107, y + 24, x + 107, y + 28);
    g.drawLine(x + 127, y + 24, x + 127, y + 28);
    g.drawLine(x + 147, y + 24, x + 147, y + 22);
    g.drawLine(x + 127, y + 24, x + 127, y + 22);
    g.drawLine(x + 107, y + 24, x + 107, y + 22);
    g.drawLine(x + 5, y + 24, x + 155, y + 24);
    g.drawLine(x + 70, y + 7, x + 70, y + 9);
    g.drawLine(x + 70, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 24);
    g.drawLine(x + 1, y + 24, x + 5, y + 24);
    g.drawLine(x + 10, y + 24, x + 10, y + 28);
    g.drawLine(x + 30, y + 24, x + 30, y + 28);
    g.drawLine(x + 50, y + 24, x + 50, y + 28);
    g.drawLine(x + 70, y + 24, x + 70, y + 28);
    g.drawLine(x + 90, y + 24, x + 90, y + 28);
    g.drawLine(x + 110, y + 24, x + 110, y + 28);
    g.drawLine(x + 130, y + 24, x + 130, y + 28);
    g.drawLine(x + 150, y + 24, x + 150, y + 22);
    g.drawLine(x + 130, y + 24, x + 130, y + 22);
    g.drawLine(x + 110, y + 24, x + 110, y + 22);
    g.drawLine(x + 5, y + 25, x + 155, y + 25);
    g.drawLine(x + 50, y + 7, x + 50, y + 9);
    g.drawLine(x + 50, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 25);
    g.drawLine(x + 1, y + 25, x + 5, y + 25);
    g.drawLine(x + 13, y + 25, x + 13, y + 28);
    g.drawLine(x + 33, y + 25, x + 33, y + 28);
    g.drawLine(x + 53, y + 25, x + 53, y + 28);
    g.drawLine(x + 73, y + 25, x + 73, y + 28);
    g.drawLine(x + 93, y + 25, x + 93, y + 28);
    g.drawLine(x + 113, y + 25, x + 113, y + 28);
    g.drawLine(x + 133, y + 25, x + 133, y + 28);
    g.drawLine(x + 153, y + 25, x + 153, y + 22);
    g.drawLine(x + 133, y + 25, x + 133, y + 22);
    g.drawLine(x + 113, y + 25, x + 113, y + 22);
    g.drawLine(x + 5, y + 25, x + 155, y + 25);
    g.drawLine(x + 30, y + 7, x + 30, y + 9);
    g.drawLine(x + 30, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 25);
    g.drawLine(x + 1, y + 25, x + 5, y + 25);
    g.drawLine(x + 7, y + 25, x + 7, y + 28);
    g.drawLine(x + 27, y + 25, x + 27, y + 28);
    g.drawLine(x + 47, y + 25, x + 47, y + 28);
    g.drawLine(x + 67, y + 25, x + 67, y + 28);
    g.drawLine(x + 87, y + 25, x + 87, y + 28);
    g.drawLine(x + 107, y + 25, x + 107, y + 28);
    g.drawLine(x + 127, y + 25, x + 127, y + 28);
    g.drawLine(x + 147, y + 25, x + 147, y + 22);
    g.drawLine(x + 127, y + 25, x + 127, y + 22);
    g.drawLine(x + 107, y + 25, x + 107, y + 22);
    GraphicsUtil.drawCenteredText(g, "0", x + 10, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "1", x + 30, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "2", x + 50, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "3", x + 70, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "4", x + 90, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "5", x + 110, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "6", x + 130, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "7", x + 150, y + 17);
    GraphicsUtil.drawCenteredText(g, "8", x + 130, y + 17);
    GraphicsUtil.drawCenteredText(g, "9", x + 110, y + 17);
    g.setFont(previousFont);
  }



  @Override
  public void propagateTtl(InstanceState state) {
    final var code = inputCode(state);
    for (var index = 0; index < OUTPUTS.length; index++) {
      final var value =
          code == CODE_ERROR ? Value.ERROR : code == index ? Value.FALSE : Value.UNKNOWN;
      state.setPort(pinNrToPortNr(OUTPUTS[index]), value, DELAY);
    }
  }

  /**
   * Reads the BCD inputs.
   *
   * @return the code 0 to 9, {@link #CODE_UNKNOWN} when the code is above 9 or an input is not a
   *     binary value, or {@link #CODE_ERROR} when any input is an error
   */
  private static int inputCode(InstanceState state) {
    final var bits = new Value[INPUTS.length];
    for (var index = 0; index < INPUTS.length; index++) {
      bits[index] = state.getPortValue(pinNrToPortNr(INPUTS[index]));
      if (bits[index].isErrorValue()) {
        return CODE_ERROR;
      }
    }
    var code = 0;
    for (var index = 0; index < bits.length; index++) {
      if (bits[index] != Value.TRUE && bits[index] != Value.FALSE) {
        return CODE_UNKNOWN;
      }
      if (bits[index] == Value.TRUE) {
        code |= 1 << index;
      }
    }
    return code <= 9 ? code : CODE_UNKNOWN;
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
