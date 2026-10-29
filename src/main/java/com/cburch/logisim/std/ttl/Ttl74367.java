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
import java.awt.Graphics;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x367: hex non-inverting buffer/line driver with three-state outputs.
 *
 * <p>Model based on the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT367.pdf">Nexperia
 * 74HC367/74HCT367 datasheet</a>. {@code nOE1} and {@code nOE2} are active low and independent.
 * {@code nOE1} enables buffers 1 through 4. {@code nOE2} enables buffers 5 and 6. While an enable
 * is low, each output in its group follows its data input. A high level releases only that group.
 * The 74x365 is a different device: both of its enables are common to all six buffers.
 * High-impedance is represented as {@link Value#UNKNOWN}. Nanosecond delays are not modeled.
 *
 * <p>An unknown or error value on an enable releases only that enable's group. While a group is
 * enabled, an unknown or error data input is copied to that channel only.
 */
public class Ttl74367 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74367";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet.
  public static final byte OE1 = 1;
  public static final byte A1 = 2;
  public static final byte Y1 = 3;
  public static final byte A2 = 4;
  public static final byte Y2 = 5;
  public static final byte A3 = 6;
  public static final byte Y3 = 7;
  public static final byte GND = 8;
  public static final byte Y4 = 9;
  public static final byte A4 = 10;
  public static final byte Y5 = 11;
  public static final byte A5 = 12;
  public static final byte Y6 = 13;
  public static final byte A6 = 14;
  public static final byte OE2 = 15;
  public static final byte VCC = 16;

  /** Data inputs in channel order. */
  private static final byte[] INPUTS = {A1, A2, A3, A4, A5, A6};

  /** Data outputs in channel order. */
  private static final byte[] OUTPUTS = {Y1, Y2, Y3, Y4, Y5, Y6};

  /** Number of buffers controlled by {@code nOE1}. The rest belong to {@code nOE2}. */
  private static final int GROUP1_SIZE = 4;

  private static final String[] PORT_NAMES = {
    "nOE1 Output enable 1 (active low)",
    "1A",
    "1Y",
    "2A",
    "2Y",
    "3A",
    "3Y",
    "4Y",
    "4A",
    "5Y",
    "5A",
    "6Y",
    "6A",
    "nOE2 Output enable 2 (active low)"
  };

  /** Creates a 74367 hex buffer/line driver with three-state outputs. */
  public Ttl74367() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74367HdlGenerator());
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
    final var bodyFont = g.getFont();
    g.setFont(bodyFont.deriveFont(9f));
    g.drawRect((x + 12), (y + 18), 142, 24);
    GraphicsUtil.drawCenteredText(g, "BUF", (x + 83), (y + 30));
    g.setFont(bodyFont);
    final var top = y + PIN_HEIGHT;
    final var bottom = y + height - PIN_HEIGHT;
    structureBubble(g, (x + 10), (y + 42));
    g.drawLine((x + 10), bottom, (x + 10), (y + 44));
    g.drawLine((x + 30), bottom, (x + 30), (y + 42));
    g.drawLine((x + 50), bottom, (x + 50), (y + 42));
    g.drawLine((x + 70), bottom, (x + 70), (y + 42));
    g.drawLine((x + 90), bottom, (x + 90), (y + 42));
    g.drawLine((x + 110), bottom, (x + 110), (y + 42));
    g.drawLine((x + 130), bottom, (x + 130), (y + 42));
    g.drawLine((x + 150), top, (x + 150), (y + 18));
    g.drawLine((x + 130), top, (x + 130), (y + 18));
    g.drawLine((x + 110), top, (x + 110), (y + 18));
    g.drawLine((x + 90), top, (x + 90), (y + 18));
    g.drawLine((x + 70), top, (x + 70), (y + 18));
    g.drawLine((x + 50), top, (x + 50), (y + 18));
    structureBubble(g, (x + 30), (y + 18));
    g.drawLine((x + 30), top, (x + 30), (y + 16));
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var outputEnable1 = state.getPortValue(pinNrToPortNr(OE1));
    final var outputEnable2 = state.getPortValue(pinNrToPortNr(OE2));
    final var group1Enabled = outputEnable1 == Value.FALSE;
    final var group2Enabled = outputEnable2 == Value.FALSE;
    for (var index = 0; index < OUTPUTS.length; index++) {
      final var enabled = index < GROUP1_SIZE ? group1Enabled : group2Enabled;
      final var value =
          enabled ? state.getPortValue(pinNrToPortNr(INPUTS[index])) : Value.UNKNOWN;
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
