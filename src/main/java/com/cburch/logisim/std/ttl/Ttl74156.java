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
 * TTL 74x156: dual 2-line to 4-line decoder/demultiplexer with open-collector outputs.
 *
 * <p>The model follows the Hitachi HD74HC156 and Philips 74HC/HCT156, which share the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74ls156.pdf">SN74LS156</a> function table. Each
 * half pulls one active-low output down and leaves the other three released. Section 1 is enabled
 * by 1G low and 1C high. Section 2 is enabled by 2G low and 2C low. A and B are the shared
 * address, with A the least significant bit.
 *
 * <p>An output is driven only when the function table selects it, and then only to
 * {@link Value#FALSE}. Every other output is {@link Value#UNKNOWN}, the released open-collector
 * level. A pull-up resistor in the circuit makes that level read high. The Inno IN74HC156
 * function table matches this logic but writes the released outputs as H; this model keeps the
 * open-drain behavior of the 156 rather than a push-pull 155 clone.
 *
 * <p>The HDL generator emits the same function as strong 0 and 1, which is what the outputs look
 * like with pull-ups. It does not emit high-impedance.
 */
public class Ttl74156 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74156";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  public static final byte C1 = 1;
  public static final byte G1 = 2;
  public static final byte B = 3;
  public static final byte Y1_3 = 4;
  public static final byte Y1_2 = 5;
  public static final byte Y1_1 = 6;
  public static final byte Y1_0 = 7;

  public static final byte Y2_0 = 9;
  public static final byte Y2_1 = 10;
  public static final byte Y2_2 = 11;
  public static final byte Y2_3 = 12;
  public static final byte A = 13;
  public static final byte G2 = 14;
  public static final byte C2 = 15;

  // Power supply
  public static final byte GND = 8;
  public static final byte VCC = 16;

  /** Section 1 outputs ordered from 1Y0 to 1Y3. */
  private static final byte[] SECTION1 = new byte[] {Y1_0, Y1_1, Y1_2, Y1_3};

  /** Section 2 outputs ordered from 2Y0 to 2Y3. */
  private static final byte[] SECTION2 = new byte[] {Y2_0, Y2_1, Y2_2, Y2_3};

  private static final byte[] OUTPUTS = new byte[] {
    Y1_3, Y1_2, Y1_1, Y1_0, Y2_0, Y2_1, Y2_2, Y2_3
  };

  private static final String[] PORT_NAMES = {
    "1C Enable (active high)",
    "1nG Enable (active low)",
    "B Address (MSB)",
    "1nY3 Open-collector output",
    "1nY2 Open-collector output",
    "1nY1 Open-collector output",
    "1nY0 Open-collector output",
    "2nY0 Open-collector output",
    "2nY1 Open-collector output",
    "2nY2 Open-collector output",
    "2nY3 Open-collector output",
    "A Address (LSB)",
    "2nG Enable (active low)",
    "2nC Enable (active low)"
  };

  /** Creates a 74156 dual 2-line to 4-line decoder/demultiplexer. */
  public Ttl74156() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74156HdlGenerator());
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
    g.drawRect((x + 12), (y + 18), 136, 24);
    GraphicsUtil.drawCenteredText(g, "2-4", (x + 80), (y + 30));
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + 42));
    g.drawLine((x + 10), (y + 42), (x + 12), (y + 42));
    g.drawLine((x + 30), (y + height - 7), (x + 30), (y + 42));
    structureBubble(g, (x + 30), (y + 46));
    g.drawLine((x + 50), (y + height - 7), (x + 50), (y + 42));
    g.drawLine((x + 70), (y + 7), (x + 70), (y + 18));
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 18));
    structureBubble(g, (x + 50), (y + 14));
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 18));
    structureBubble(g, (x + 30), (y + 14));
    g.drawLine((x + 130), (y + height - 7), (x + 130), (y + 42));
    structureBubble(g, (x + 130), (y + 46));
    g.drawLine((x + 110), (y + height - 7), (x + 110), (y + 42));
    structureBubble(g, (x + 110), (y + 46));
    g.drawLine((x + 90), (y + height - 7), (x + 90), (y + 42));
    structureBubble(g, (x + 90), (y + 46));
    g.drawLine((x + 70), (y + height - 7), (x + 70), (y + 42));
    structureBubble(g, (x + 70), (y + 46));
    g.drawLine((x + 150), (y + 7), (x + 150), (y + 18));
    structureBubble(g, (x + 150), (y + 14));
    g.drawLine((x + 130), (y + 7), (x + 130), (y + 18));
    structureBubble(g, (x + 130), (y + 14));
    g.drawLine((x + 110), (y + 7), (x + 110), (y + 18));
    structureBubble(g, (x + 110), (y + 14));
    g.drawLine((x + 90), (y + 7), (x + 90), (y + 18));
    structureBubble(g, (x + 90), (y + 14));
  }

  private static boolean isLow(InstanceState state, byte dsPinNr) {
    return state.getPortValue(pinNrToPortNr(dsPinNr)) == Value.FALSE;
  }

  private static boolean isHigh(InstanceState state, byte dsPinNr) {
    return state.getPortValue(pinNrToPortNr(dsPinNr)) == Value.TRUE;
  }

  /**
   * Returns the shared address, or -1 when A or B is not a firm logic level.
   *
   * <p>An unknown address must not pull any output down.
   */
  private static int address(InstanceState state) {
    if (!isLow(state, A) && !isHigh(state, A)) {
      return -1;
    }
    if (!isLow(state, B) && !isHigh(state, B)) {
      return -1;
    }
    return (isHigh(state, A) ? 1 : 0) + (isHigh(state, B) ? 2 : 0);
  }

  private static void propagateSection(InstanceState state, boolean enabled, byte[] outputs) {
    final var selected = enabled ? address(state) : -1;
    for (var index = 0; index < outputs.length; index++) {
      final var pullLow = selected == index;
      state.setPort(
          pinNrToPortNr(outputs[index]), pullLow ? Value.FALSE : Value.UNKNOWN, DELAY);
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    propagateSection(state, isLow(state, G1) && isHigh(state, C1), SECTION1);
    propagateSection(state, isLow(state, G2) && isLow(state, C2), SECTION2);
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
