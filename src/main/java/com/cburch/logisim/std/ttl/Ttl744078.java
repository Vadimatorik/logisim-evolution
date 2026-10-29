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
 * TTL 74x4078: 8-input NOR/OR gate.
 *
 * <p>Simulation follows the
 * <a href="https://www.st.com/resource/en/datasheet/cd00000322.pdf">M74HC4078</a> data sheet.
 * {@code Y} is the OR of {@code A} through {@code H}, and {@code X} is the NOR. Both outputs are
 * low and high respectively only when every input is low. A definite high on any input forces
 * {@code Y} high and {@code X} low; otherwise an unknown or error input makes both outputs an
 * error. Pins 6 and 8 are not connected.
 */
public class Ttl744078 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744078";

  public static final int DELAY = 1;

  public static final byte Y = 1;
  public static final byte A = 2;
  public static final byte B = 3;
  public static final byte C = 4;
  public static final byte D = 5;
  public static final byte E = 9;
  public static final byte F = 10;
  public static final byte G = 11;
  public static final byte H = 12;
  public static final byte X = 13;

  public static final byte GND = 7;
  public static final byte VCC = 14;

  private static final byte[] INPUTS = {A, B, C, D, E, F, G, H};
  private static final byte[] OUTPUTS = {Y, X};
  private static final byte[] UNUSED_PINS = {6, 8};
  private static final String[] PORT_NAMES = {"Y", "A", "B", "C", "D", "E", "F", "G", "H", "X"};

  /** Creates a 744078 8-input NOR/OR gate. */
  public Ttl744078() {
    super(_ID, (byte) 14, OUTPUTS, UNUSED_PINS, PORT_NAMES, new Ttl744078HdlGenerator());
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Pins 6 and 8 are not connected, and the power pins are omitted from the port list.
   *
   * @param dsPinNr datasheet pin number of a signal pin
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    return (byte) ((dsPinNr <= D) ? dsPinNr - 1 : dsPinNr - 4);
  }

    @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var bodyFont = g.getFont();
    g.setFont(bodyFont.deriveFont(9f));
    g.drawRect((x + 12), (y + 18), 122, 24);
    GraphicsUtil.drawCenteredText(g, "REG", (x + 73), (y + 30));
    g.setFont(bodyFont);
    final var top = y + PIN_HEIGHT;
    final var bottom = y + height - PIN_HEIGHT;
    g.drawLine((x + 10), bottom, (x + 10), (y + 42));
    g.drawLine((x + 30), bottom, (x + 30), (y + 42));
    g.drawLine((x + 50), bottom, (x + 50), (y + 42));
    g.drawLine((x + 70), bottom, (x + 70), (y + 42));
    g.drawLine((x + 90), bottom, (x + 90), (y + 42));
    g.drawLine((x + 110), bottom, (x + 110), (y + 42));
    g.drawLine((x + 130), top, (x + 130), (y + 18));
    g.drawLine((x + 110), top, (x + 110), (y + 18));
    g.drawLine((x + 90), top, (x + 90), (y + 18));
    g.drawLine((x + 70), top, (x + 70), (y + 18));
    g.drawLine((x + 50), top, (x + 50), (y + 18));
    g.drawLine((x + 30), top, (x + 30), (y + 18));
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var combined = Value.FALSE;
    for (final var input : INPUTS) {
      combined = combined.or(state.getPortValue(pinNrToPortNr(input)));
    }
    state.setPort(pinNrToPortNr(Y), combined, DELAY);
    state.setPort(pinNrToPortNr(X), combined.not(), DELAY);
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
