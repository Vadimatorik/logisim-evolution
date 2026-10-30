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
 * TTL 74HC4511: BCD to 7-segment latch/decoder/driver.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT4511.pdf">Nexperia 74HC4511</a>
 * function table. Segment outputs are active high. While {@code LE} is low the latch is
 * transparent; while {@code LE} is high it keeps the last BCD code. {@code LT} low forces every
 * segment high, and otherwise {@code BI} low forces every segment low. Neither input changes the
 * latch. Codes 10 through 15 blank the display. Nanosecond delays are not modeled, and a control
 * that is not exactly high or low does not write the latch.
 */
public class Ttl744511 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "744511";

  public static final int DELAY = 1;

  public static final byte B = 1;
  public static final byte C = 2;
  /** Lamp test, active low. */
  public static final byte LT = 3;
  /** Blanking input, active low. */
  public static final byte BI = 4;
  /** Latch enable, active low. */
  public static final byte LE = 5;
  public static final byte D = 6;
  public static final byte A = 7;
  public static final byte GND = 8;
  public static final byte SEGE = 9;
  public static final byte SEGD = 10;
  public static final byte SEGC = 11;
  public static final byte SEGB = 12;
  public static final byte SEGA = 13;
  public static final byte SEGG = 14;
  public static final byte SEGF = 15;
  public static final byte VCC = 16;

  private static final int BITS = 4;
  private static final int SEGMENTS = 7;
  /** Glyph bit 0 is segment a and bit 6 is segment g. Codes 10..15 are blank. */
  private static final int[] GLYPH = {
    0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7C, 0x07,
    0x7F, 0x67, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
  };
  private static final byte[] DATA = {A, B, C, D};
  private static final byte[] OUTPUTS = {SEGA, SEGB, SEGC, SEGD, SEGE, SEGF, SEGG};
  private static final String[] PORT_NAMES = {
    "B",
    "C",
    "LT (lamp test, active low)",
    "BI (blanking, active low)",
    "LE (latch enable, active low)",
    "D",
    "A",
    "e",
    "d",
    "c",
    "b",
    "a",
    "g",
    "f"
  };

  /** Creates a 744511 BCD to 7-segment latch/decoder/driver. */
  public Ttl744511() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl744511HdlGenerator());
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
    g.drawRect(x + 65, y + 12, 10, 10);
    g.drawRect(x + 85, y + 12, 10, 10);
    g.drawRect(x + 105, y + 12, 10, 10);
    g.drawRect(x + 125, y + 12, 10, 10);
    g.drawRect(x + 145, y + 12, 10, 10);
    g.drawRect(x + 25, y + 12, 10, 10);
    g.drawRect(x + 45, y + 12, 10, 10);
    g.drawLine(x + 70, y + 12, x + 70, y + 7);
    g.drawLine(x + 90, y + 12, x + 90, y + 7);
    g.drawLine(x + 110, y + 12, x + 110, y + 7);
    g.drawLine(x + 130, y + 12, x + 130, y + 7);
    g.drawLine(x + 150, y + 12, x + 150, y + 7);
    g.drawLine(x + 30, y + 12, x + 30, y + 7);
    g.drawLine(x + 50, y + 12, x + 50, y + 7);
    g.drawLine(x + 25, y + 24, x + 155, y + 24);
    g.drawLine(x + 10, y + height - 7, x + 10, y + height - 9);
    g.drawLine(x + 10, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 24);
    g.drawLine(x + 21, y + 24, x + 25, y + 24);
    g.drawLine(x + 67, y + 24, x + 67, y + 22);
    g.drawLine(x + 87, y + 24, x + 87, y + 22);
    g.drawLine(x + 107, y + 24, x + 107, y + 22);
    g.drawLine(x + 127, y + 24, x + 127, y + 22);
    g.drawLine(x + 147, y + 24, x + 147, y + 22);
    g.drawLine(x + 27, y + 24, x + 27, y + 22);
    g.drawLine(x + 47, y + 24, x + 47, y + 22);
    g.drawLine(x + 25, y + 24, x + 155, y + 24);
    g.drawLine(x + 30, y + height - 7, x + 30, y + height - 9);
    g.drawLine(x + 30, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 24);
    g.drawLine(x + 21, y + 24, x + 25, y + 24);
    g.drawLine(x + 70, y + 24, x + 70, y + 22);
    g.drawLine(x + 90, y + 24, x + 90, y + 22);
    g.drawLine(x + 110, y + 24, x + 110, y + 22);
    g.drawLine(x + 130, y + 24, x + 130, y + 22);
    g.drawLine(x + 150, y + 24, x + 150, y + 22);
    g.drawLine(x + 30, y + 24, x + 30, y + 22);
    g.drawLine(x + 50, y + 24, x + 50, y + 22);
    g.drawLine(x + 25, y + 24, x + 155, y + 24);
    g.drawLine(x + 50, y + height - 7, x + 50, y + height - 9);
    g.drawLine(x + 50, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 24);
    g.drawLine(x + 21, y + 24, x + 25, y + 24);
    g.drawLine(x + 73, y + 24, x + 73, y + 22);
    g.drawLine(x + 93, y + 24, x + 93, y + 22);
    g.drawLine(x + 113, y + 24, x + 113, y + 22);
    g.drawLine(x + 133, y + 24, x + 133, y + 22);
    g.drawLine(x + 153, y + 24, x + 153, y + 22);
    g.drawLine(x + 33, y + 24, x + 33, y + 22);
    g.drawLine(x + 53, y + 24, x + 53, y + 22);
    g.drawLine(x + 25, y + 25, x + 155, y + 25);
    g.drawLine(x + 70, y + height - 7, x + 70, y + height - 9);
    g.drawLine(x + 70, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 25);
    g.drawLine(x + 21, y + 25, x + 25, y + 25);
    g.drawLine(x + 67, y + 25, x + 67, y + 22);
    g.drawLine(x + 87, y + 25, x + 87, y + 22);
    g.drawLine(x + 107, y + 25, x + 107, y + 22);
    g.drawLine(x + 127, y + 25, x + 127, y + 22);
    g.drawLine(x + 147, y + 25, x + 147, y + 22);
    g.drawLine(x + 27, y + 25, x + 27, y + 22);
    g.drawLine(x + 47, y + 25, x + 47, y + 22);
    g.drawLine(x + 25, y + 25, x + 155, y + 25);
    g.drawLine(x + 90, y + height - 7, x + 90, y + height - 9);
    g.drawLine(x + 90, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 25);
    g.drawLine(x + 21, y + 25, x + 25, y + 25);
    g.drawLine(x + 70, y + 25, x + 70, y + 22);
    g.drawLine(x + 90, y + 25, x + 90, y + 22);
    g.drawLine(x + 110, y + 25, x + 110, y + 22);
    g.drawLine(x + 130, y + 25, x + 130, y + 22);
    g.drawLine(x + 150, y + 25, x + 150, y + 22);
    g.drawLine(x + 30, y + 25, x + 30, y + 22);
    g.drawLine(x + 50, y + 25, x + 50, y + 22);
    g.drawLine(x + 25, y + 25, x + 155, y + 25);
    g.drawLine(x + 110, y + height - 7, x + 110, y + height - 9);
    g.drawLine(x + 110, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 25);
    g.drawLine(x + 21, y + 25, x + 25, y + 25);
    g.drawLine(x + 73, y + 25, x + 73, y + 22);
    g.drawLine(x + 93, y + 25, x + 93, y + 22);
    g.drawLine(x + 113, y + 25, x + 113, y + 22);
    g.drawLine(x + 133, y + 25, x + 133, y + 22);
    g.drawLine(x + 153, y + 25, x + 153, y + 22);
    g.drawLine(x + 33, y + 25, x + 33, y + 22);
    g.drawLine(x + 53, y + 25, x + 53, y + 22);
    g.drawLine(x + 25, y + 25, x + 155, y + 25);
    g.drawLine(x + 130, y + height - 7, x + 130, y + height - 9);
    g.drawLine(x + 130, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 25);
    g.drawLine(x + 21, y + 25, x + 25, y + 25);
    g.drawLine(x + 67, y + 25, x + 67, y + 22);
    g.drawLine(x + 87, y + 25, x + 87, y + 22);
    g.drawLine(x + 107, y + 25, x + 107, y + 22);
    g.drawLine(x + 127, y + 25, x + 127, y + 22);
    g.drawLine(x + 147, y + 25, x + 147, y + 22);
    g.drawLine(x + 27, y + 25, x + 27, y + 22);
    g.drawLine(x + 47, y + 25, x + 47, y + 22);
    g.drawOval(x + 19, y + 22, 4, 4);
    g.drawOval(x + 19, y + 23, 4, 4);
    g.drawOval(x + 19, y + 23, 4, 4);
    GraphicsUtil.drawCenteredText(g, "a", x + 70, y + 17);
    GraphicsUtil.drawCenteredText(g, "b", x + 90, y + 17);
    GraphicsUtil.drawCenteredText(g, "c", x + 110, y + 17);
    GraphicsUtil.drawCenteredText(g, "d", x + 130, y + 17);
    GraphicsUtil.drawCenteredText(g, "e", x + 150, y + 17);
    GraphicsUtil.drawCenteredText(g, "f", x + 30, y + 17);
    GraphicsUtil.drawCenteredText(g, "g", x + 50, y + 17);
    g.setFont(previousFont);
  }



  @Override
  public void propagateTtl(InstanceState state) {
    final var data = stateData(state);
    if (input(state, LE) == Value.FALSE) {
      for (var index = 0; index < BITS; index++) {
        data.setValue(index, input(state, DATA[index]));
      }
    }
    final var bits = new Value[BITS];
    for (var index = 0; index < BITS; index++) {
      bits[index] = data.getValue(index);
    }
    final var lampTest = input(state, LT);
    final var blanking = input(state, BI);
    for (var segment = 0; segment < SEGMENTS; segment++) {
      final var decoded = decodeSegment(segment, bits);
      state.setPort(
          pinNrToPortNr(OUTPUTS[segment]), applyControls(lampTest, blanking, decoded), DELAY);
    }
  }

  private static TtlRegisterData stateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.ONE, BITS);
      state.setData(data);
    }
    return data;
  }

  private static Value decodeSegment(int segment, Value[] bits) {
    Value agreed = null;
    var conflict = false;
    var conflictFromError = false;
    for (var code = 0; code < GLYPH.length; code++) {
      if (!accepts(bits, code)) continue;
      final var level = ((GLYPH[code] >> segment) & 1) == 1 ? Value.TRUE : Value.FALSE;
      if (agreed == null) {
        agreed = level;
      } else if (agreed != level) {
        conflict = true;
        if (hasError(bits)) conflictFromError = true;
      }
    }
    if (agreed == null) return Value.ERROR;
    if (!conflict) return agreed;
    return conflictFromError ? Value.ERROR : Value.UNKNOWN;
  }

  private static boolean accepts(Value[] bits, int code) {
    for (var place = 0; place < bits.length; place++) {
      final var bit = bits[place];
      final var high = ((code >> place) & 1) == 1;
      if (bit == Value.TRUE && !high) return false;
      if (bit == Value.FALSE && high) return false;
    }
    return true;
  }

  private static boolean hasError(Value[] bits) {
    for (final var bit : bits) {
      if (bit != Value.TRUE && bit != Value.FALSE && bit.isErrorValue()) return true;
    }
    return false;
  }

  private static Value applyControls(Value lampTest, Value blanking, Value decoded) {
    if (lampTest == Value.FALSE) return Value.TRUE;
    final var blanked = applyBlanking(blanking, decoded);
    if (lampTest == Value.TRUE) return blanked;
    return disagree(lampTest, Value.TRUE, blanked);
  }

  private static Value applyBlanking(Value blanking, Value decoded) {
    if (blanking == Value.FALSE) return Value.FALSE;
    if (blanking == Value.TRUE) return decoded;
    return disagree(blanking, Value.FALSE, decoded);
  }

  /**
   * Combines a forced level with the decoded level when a control is not exactly high or low. An
   * error on that control produces an error where the two levels differ; any other disagreement is
   * unknown.
   */
  private static Value disagree(Value selector, Value forced, Value other) {
    if (forced == other) return forced;
    if (selector == Value.ERROR || other == Value.ERROR) return Value.ERROR;
    return Value.UNKNOWN;
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
