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
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x375: quad bistable transparent latch.
 *
 * <p>Simulation follows the ST M74HC375 data sheet. The device is pin and function compatible
 * with the 74LS375. Latches 1 and 2 are transparent while {@code G1•2} is high, and latches 3 and
 * 4 while {@code G3•4} is high. A low enable holds the value that was present at {@code D} when
 * the enable fell. {@code nQ} is the complement of {@code Q}. This is not the 74x75 pinout: VCC
 * is pin 16 and GND is pin 8.
 *
 * <p>An enable that is neither high nor low keeps a stored bit only when the data input is the
 * same fully defined value. Any other combination makes that bit an error, because the output
 * could either hold or follow. A high enable copies the data input unchanged, including unknown
 * and error values. The complement of an unknown or error bit stays unknown or error.
 */
public class Ttl74375 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74375";

  public static final int DELAY = 1;

  public static final byte D1 = 1;
  public static final byte NQ1 = 2;
  public static final byte Q1 = 3;
  public static final byte G12 = 4;
  public static final byte Q2 = 5;
  public static final byte NQ2 = 6;
  public static final byte D2 = 7;
  public static final byte GND = 8;
  public static final byte D3 = 9;
  public static final byte NQ3 = 10;
  public static final byte Q3 = 11;
  public static final byte G34 = 12;
  public static final byte Q4 = 13;
  public static final byte NQ4 = 14;
  public static final byte D4 = 15;
  public static final byte VCC = 16;

  private static final byte[] OUTPUTS = {NQ1, Q1, Q2, NQ2, NQ3, Q3, Q4, NQ4};

  private static final String[] PORT_NAMES = {
    "1D Data input 1",
    "n1Q Complementary output 1",
    "1Q Latch output 1",
    "G12 Latch enable 1 and 2 (active high)",
    "2Q Latch output 2",
    "n2Q Complementary output 2",
    "2D Data input 2",
    "3D Data input 3",
    "n3Q Complementary output 3",
    "3Q Latch output 3",
    "G34 Latch enable 3 and 4 (active high)",
    "4Q Latch output 4",
    "n4Q Complementary output 4",
    "4D Data input 4"
  };

  private static final Latch[] LATCHES = {
    new Latch(0, D1, Q1, NQ1, G12),
    new Latch(1, D2, Q2, NQ2, G12),
    new Latch(2, D3, Q3, NQ3, G34),
    new Latch(3, D4, Q4, NQ4, G34)
  };

  /** Creates a 74375 quad bistable transparent latch. */
  public Ttl74375() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74375HdlGenerator());
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
    g.drawRect(x + 25, y + 28, 10, 10);
    g.drawRect(x + 45, y + 28, 10, 10);
    g.drawRect(x + 85, y + 28, 10, 10);
    g.drawRect(x + 105, y + 28, 10, 10);
    g.drawRect(x + 125, y + 12, 10, 10);
    g.drawRect(x + 105, y + 12, 10, 10);
    g.drawRect(x + 65, y + 12, 10, 10);
    g.drawRect(x + 45, y + 12, 10, 10);
    g.drawLine(x + 30, y + height - 22, x + 30, y + height - 7);
    g.drawLine(x + 50, y + height - 22, x + 50, y + height - 7);
    g.drawLine(x + 90, y + height - 22, x + 90, y + height - 7);
    g.drawLine(x + 110, y + height - 22, x + 110, y + height - 7);
    g.drawLine(x + 130, y + 12, x + 130, y + 7);
    g.drawLine(x + 110, y + 12, x + 110, y + 7);
    g.drawLine(x + 70, y + 12, x + 70, y + 7);
    g.drawLine(x + 50, y + 12, x + 50, y + 7);
    g.drawLine(x + 25, y + 24, x + 135, y + 24);
    g.drawLine(x + 70, y + height - 7, x + 70, y + height - 9);
    g.drawLine(x + 70, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 24);
    g.drawLine(x + 21, y + 24, x + 25, y + 24);
    g.drawLine(x + 27, y + 24, x + 27, y + 28);
    g.drawLine(x + 47, y + 24, x + 47, y + 28);
    g.drawLine(x + 87, y + 24, x + 87, y + 28);
    g.drawLine(x + 107, y + 24, x + 107, y + 28);
    g.drawLine(x + 127, y + 24, x + 127, y + 22);
    g.drawLine(x + 107, y + 24, x + 107, y + 22);
    g.drawLine(x + 67, y + 24, x + 67, y + 22);
    g.drawLine(x + 47, y + 24, x + 47, y + 22);
    g.drawLine(x + 25, y + 25, x + 135, y + 25);
    g.drawLine(x + 90, y + 7, x + 90, y + 9);
    g.drawLine(x + 90, y + 9, x + 21, y + 9);
    g.drawLine(x + 21, y + 9, x + 21, y + 25);
    g.drawLine(x + 21, y + 25, x + 25, y + 25);
    g.drawLine(x + 30, y + 25, x + 30, y + 28);
    g.drawLine(x + 50, y + 25, x + 50, y + 28);
    g.drawLine(x + 90, y + 25, x + 90, y + 28);
    g.drawLine(x + 110, y + 25, x + 110, y + 28);
    g.drawLine(x + 130, y + 25, x + 130, y + 22);
    g.drawLine(x + 110, y + 25, x + 110, y + 22);
    g.drawLine(x + 70, y + 25, x + 70, y + 22);
    g.drawLine(x + 50, y + 25, x + 50, y + 22);
    g.drawLine(x + 10, y + height - 7, x + 10, y + height - 19);
    g.drawLine(x + 10, y + height - 19, x + 27, y + height - 19);
    g.drawLine(x + 27, y + height - 19, x + 27, y + height - 22);
    g.drawLine(x + 130, y + height - 7, x + 130, y + height - 19);
    g.drawLine(x + 130, y + height - 19, x + 87, y + height - 19);
    g.drawLine(x + 87, y + height - 19, x + 87, y + height - 22);
    g.drawLine(x + 150, y + 7, x + 150, y + 9);
    g.drawLine(x + 150, y + 9, x + 127, y + 9);
    g.drawLine(x + 127, y + 9, x + 127, y + 12);
    g.drawLine(x + 30, y + 7, x + 30, y + 9);
    g.drawLine(x + 30, y + 9, x + 67, y + 9);
    g.drawLine(x + 67, y + 9, x + 67, y + 12);
    GraphicsUtil.drawCenteredText(g, "1", x + 30, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "1", x + 50, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "2", x + 90, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "2", x + 110, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "3", x + 130, y + 17);
    GraphicsUtil.drawCenteredText(g, "3", x + 110, y + 17);
    GraphicsUtil.drawCenteredText(g, "4", x + 70, y + 17);
    GraphicsUtil.drawCenteredText(g, "4", x + 50, y + 17);
    g.setFont(previousFont);
  }



  @Override
  public void propagateTtl(InstanceState state) {
    final var data = stateData(state);
    for (final var latch : LATCHES) {
      final var stored = data.getValue(latch.bit);
      final var next =
          nextValue(port(state, latch.enablePin), port(state, latch.dataPin), stored);
      data.setValue(latch.bit, next);
      state.setPort(pinNrToPortNr(latch.outputPin), next, DELAY);
      state.setPort(pinNrToPortNr(latch.complementPin), complement(next), DELAY);
    }
  }

  /**
   * Next stored bit. A high enable is transparent. A low enable holds. Any other enable level
   * holds only when the data input repeats the stored defined bit.
   */
  static Value nextValue(Value enable, Value data, Value stored) {
    if (enable == Value.TRUE) return data;
    if (enable == Value.FALSE) return stored;
    if ((data == Value.TRUE || data == Value.FALSE) && data == stored) return stored;
    return Value.ERROR;
  }

  /** Complement that leaves unknown and error bits unchanged. */
  static Value complement(Value value) {
    if (value == Value.TRUE) return Value.FALSE;
    if (value == Value.FALSE) return Value.TRUE;
    return value;
  }

  private static Value port(InstanceState state, byte dsPinNr) {
    return state.getPortValue(pinNrToPortNr(dsPinNr));
  }

  private static TtlRegisterData stateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.ONE, LATCHES.length);
      state.setData(data);
    }
    return data;
  }

  private record Latch(int bit, byte dataPin, byte outputPin, byte complementPin, byte enablePin) {}

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
