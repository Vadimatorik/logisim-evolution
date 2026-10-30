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
 * TTL 74HC75: quad bistable transparent latch.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC75.pdf">Nexperia 74HC75</a>
 * function table. {@code LE12} makes latches 1 and 2 transparent, and {@code LE34} makes latches 3
 * and 4 transparent. While an enable is high, {@code nQ} follows {@code nD} and the complementary
 * output is its inverse. A low enable holds the pair. The pairs are independent. Nanosecond delays
 * are not modeled.
 *
 * <p>The pinout is the 74HC75 package: {@code VCC} is pin 5 and {@code GND} is pin 12. It is not
 * the 7475 or 74LS75 pinout, where power sits on pins 16 and 8.
 *
 * <p>An enable that is not exactly high or low does not write. The published level is then the
 * transparent and stored results combined: a 0/1 conflict is an error, and any other disagreement
 * is unknown.
 */
public class Ttl7475 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "7475";

  public static final int DELAY = 1;

  public static final byte Q1N = 1;
  public static final byte D1 = 2;
  public static final byte D2 = 3;
  public static final byte LE34 = 4;
  public static final byte VCC = 5;
  public static final byte D3 = 6;
  public static final byte D4 = 7;
  public static final byte Q4N = 8;
  public static final byte Q4 = 9;
  public static final byte Q3 = 10;
  public static final byte Q3N = 11;
  public static final byte GND = 12;
  public static final byte LE12 = 13;
  public static final byte Q2N = 14;
  public static final byte Q2 = 15;
  public static final byte Q1 = 16;

  private static final byte[] DATA = {D1, D2, D3, D4};
  private static final byte[] OUTPUT = {Q1, Q2, Q3, Q4};
  private static final byte[] COMPLEMENT = {Q1N, Q2N, Q3N, Q4N};
  private static final byte[] ENABLE = {LE12, LE12, LE34, LE34};
  private static final byte[] OUTPUTS = {Q1N, Q4N, Q4, Q3, Q3N, Q2N, Q2, Q1};
  private static final String[] PORT_NAMES = {
    "1Q complementary",
    "1D data",
    "2D data",
    "LE34 latch enable for latches 3 and 4 (active high)",
    "3D data",
    "4D data",
    "4Q complementary",
    "4Q",
    "3Q",
    "3Q complementary",
    "LE12 latch enable for latches 1 and 2 (active high)",
    "2Q complementary",
    "2Q",
    "1Q"
  };

  /** Creates a 7475 quad bistable transparent latch with the 74HC75 pinout. */
  public Ttl7475() {
    super(_ID, (byte) 16, OUTPUTS, null, PORT_NAMES, VCC, GND, new Ttl7475HdlGenerator());
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Power pins are omitted from the port list. Pin 5 is {@code VCC} and pin 12 is {@code GND}.
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    if (dsPinNr < VCC) return (byte) (dsPinNr - 1);
    if (dsPinNr < GND) return (byte) (dsPinNr - 2);
    return (byte) (dsPinNr - 3);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 8));
    g.drawRect(x + 5, y + 28, 10, 10);
    g.drawRect(x + 145, y + 28, 10, 10);
    g.drawRect(x + 145, y + 12, 10, 10);
    g.drawRect(x + 125, y + 12, 10, 10);
    g.drawRect(x + 105, y + 12, 10, 10);
    g.drawRect(x + 45, y + 12, 10, 10);
    g.drawRect(x + 25, y + 12, 10, 10);
    g.drawRect(x + 5, y + 12, 10, 10);
    g.drawLine(x + 10, y + height - 22, x + 10, y + height - 7);
    g.drawLine(x + 150, y + height - 22, x + 150, y + height - 7);
    g.drawLine(x + 150, y + 12, x + 150, y + 7);
    g.drawLine(x + 130, y + 12, x + 130, y + 7);
    g.drawLine(x + 110, y + 12, x + 110, y + 7);
    g.drawLine(x + 50, y + 12, x + 50, y + 7);
    g.drawLine(x + 30, y + 12, x + 30, y + 7);
    g.drawLine(x + 10, y + 12, x + 10, y + 7);
    g.drawLine(x + 5, y + 24, x + 155, y + 24);
    g.drawLine(x + 70, y + height - 7, x + 70, y + height - 9);
    g.drawLine(x + 70, y + height - 9, x + 1, y + height - 9);
    g.drawLine(x + 1, y + height - 9, x + 1, y + 24);
    g.drawLine(x + 1, y + 24, x + 5, y + 24);
    g.drawLine(x + 7, y + 24, x + 7, y + 28);
    g.drawLine(x + 147, y + 24, x + 147, y + 28);
    g.drawLine(x + 147, y + 24, x + 147, y + 22);
    g.drawLine(x + 127, y + 24, x + 127, y + 22);
    g.drawLine(x + 107, y + 24, x + 107, y + 22);
    g.drawLine(x + 47, y + 24, x + 47, y + 22);
    g.drawLine(x + 27, y + 24, x + 27, y + 22);
    g.drawLine(x + 7, y + 24, x + 7, y + 22);
    g.drawLine(x + 5, y + 25, x + 155, y + 25);
    g.drawLine(x + 70, y + 7, x + 70, y + 9);
    g.drawLine(x + 70, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 25);
    g.drawLine(x + 1, y + 25, x + 5, y + 25);
    g.drawLine(x + 10, y + 25, x + 10, y + 28);
    g.drawLine(x + 150, y + 25, x + 150, y + 28);
    g.drawLine(x + 150, y + 25, x + 150, y + 22);
    g.drawLine(x + 130, y + 25, x + 130, y + 22);
    g.drawLine(x + 110, y + 25, x + 110, y + 22);
    g.drawLine(x + 50, y + 25, x + 50, y + 22);
    g.drawLine(x + 30, y + 25, x + 30, y + 22);
    g.drawLine(x + 10, y + 25, x + 10, y + 22);
    g.drawLine(x + 30, y + height - 7, x + 30, y + height - 19);
    g.drawLine(x + 30, y + height - 19, x + 7, y + height - 19);
    g.drawLine(x + 7, y + height - 19, x + 7, y + height - 22);
    g.drawLine(x + 50, y + height - 7, x + 50, y + 25);
    g.drawLine(x + 50, y + 25, x + 47, y + 25);
    g.drawLine(x + 47, y + 25, x + 47, y + 22);
    g.drawLine(x + 110, y + height - 7, x + 110, y + 25);
    g.drawLine(x + 110, y + 25, x + 127, y + 25);
    g.drawLine(x + 127, y + 25, x + 127, y + 22);
    g.drawLine(x + 130, y + height - 7, x + 130, y + height - 19);
    g.drawLine(x + 130, y + height - 19, x + 147, y + height - 19);
    g.drawLine(x + 147, y + height - 19, x + 147, y + height - 22);
    GraphicsUtil.drawCenteredText(g, "y", x + 10, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "y", x + 150, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "Q", x + 150, y + 17);
    GraphicsUtil.drawCenteredText(g, "Q", x + 130, y + 17);
    GraphicsUtil.drawCenteredText(g, "y", x + 110, y + 17);
    GraphicsUtil.drawCenteredText(g, "y", x + 50, y + 17);
    GraphicsUtil.drawCenteredText(g, "Q", x + 30, y + 17);
    GraphicsUtil.drawCenteredText(g, "Q", x + 10, y + 17);
    g.setFont(previousFont);
  }



  @Override
  public void propagateTtl(InstanceState state) {
    final var data = stateData(state);
    for (var index = 0; index < DATA.length; index++) {
      if (input(state, ENABLE[index]) == Value.TRUE) {
        data.setValue(index, input(state, DATA[index]));
      }
    }
    for (var index = 0; index < DATA.length; index++) {
      final var stored = data.getValue(index);
      final var enable = input(state, ENABLE[index]);
      final var level = definedEnable(enable) ? stored : combine(input(state, DATA[index]), stored);
      publish(state, OUTPUT[index], level);
      publish(state, COMPLEMENT[index], complement(level));
    }
  }

  private static TtlRegisterData stateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.ONE, DATA.length);
      state.setData(data);
    }
    return data;
  }

  private static boolean definedEnable(Value enable) {
    return enable == Value.TRUE || enable == Value.FALSE;
  }

  private static Value combine(Value transparent, Value stored) {
    if (transparent == stored) return transparent;
    if (transparent == Value.ERROR || stored == Value.ERROR) return Value.ERROR;
    if (transparent.isFullyDefined() && stored.isFullyDefined()) return Value.ERROR;
    return Value.UNKNOWN;
  }

  /** Inverts a stored bit. An unknown latch stays unknown on both outputs. */
  private static Value complement(Value value) {
    if (value == Value.TRUE) return Value.FALSE;
    if (value == Value.FALSE) return Value.TRUE;
    if (value == Value.ERROR) return Value.ERROR;
    return Value.UNKNOWN;
  }

  private static void publish(InstanceState state, byte pin, Value value) {
    state.setPort(pinNrToPortNr(pin), value, DELAY);
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
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
