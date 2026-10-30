/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

/**
 * TTL 74HC564: octal positive-edge D flip-flop with inverting three-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://media.digikey.com/pdf/Data%20Sheets/NXP%20PDFs/74HC564.pdf">NXP 74HC564</a>
 * data sheet. Each flip-flop stores its D input on the rising edge of CP. A low nOE drives the
 * complement of the stored bit; the data sheet names those pins Q0–Q7. A high nOE, or any level
 * that is not a solid low, releases every output. Output enable does not change the register, and
 * the register still loads while the outputs are released. High impedance is reported as unknown.
 * Nanosecond delays are not modeled. The DIP-20 pinout matches the 74HC574: inputs D0–D7 are
 * opposite the inverting outputs.
 */
public class Ttl74564 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74564";

  public static final int PORT_INDEX_nOE = 0;
  public static final int PORT_INDEX_D0 = 1;
  public static final int PORT_INDEX_D1 = 2;
  public static final int PORT_INDEX_D2 = 3;
  public static final int PORT_INDEX_D3 = 4;
  public static final int PORT_INDEX_D4 = 5;
  public static final int PORT_INDEX_D5 = 6;
  public static final int PORT_INDEX_D6 = 7;
  public static final int PORT_INDEX_D7 = 8;
  public static final int PORT_INDEX_CP = 9;
  public static final int PORT_INDEX_nQ7 = 10;
  public static final int PORT_INDEX_nQ6 = 11;
  public static final int PORT_INDEX_nQ5 = 12;
  public static final int PORT_INDEX_nQ4 = 13;
  public static final int PORT_INDEX_nQ3 = 14;
  public static final int PORT_INDEX_nQ2 = 15;
  public static final int PORT_INDEX_nQ1 = 16;
  public static final int PORT_INDEX_nQ0 = 17;
  /** Port index of GND when the explicit power pins are shown. */
  public static final int PORT_INDEX_GND = 18;
  /** Port index of VCC when the explicit power pins are shown. */
  public static final int PORT_INDEX_VCC = 19;

  private static final int DELAY = 1;
  private static final int BITS = 8;
  /** Data port of each bit. Bit 0 is D0. */
  private static final int[] DATA = {
    PORT_INDEX_D0,
    PORT_INDEX_D1,
    PORT_INDEX_D2,
    PORT_INDEX_D3,
    PORT_INDEX_D4,
    PORT_INDEX_D5,
    PORT_INDEX_D6,
    PORT_INDEX_D7
  };
  /** Inverting output of each bit. Bit 0 is nQ0, the complement of the bit stored from D0. */
  private static final int[] OUTPUTS = {
    PORT_INDEX_nQ0,
    PORT_INDEX_nQ1,
    PORT_INDEX_nQ2,
    PORT_INDEX_nQ3,
    PORT_INDEX_nQ4,
    PORT_INDEX_nQ5,
    PORT_INDEX_nQ6,
    PORT_INDEX_nQ7
  };
  private static final byte[] OUTPUT_PINS = {12, 13, 14, 15, 16, 17, 18, 19};
  private static final String[] PORT_NAMES = {
    "nOE (output enable, active low)",
    "D0",
    "D1",
    "D2",
    "D3",
    "D4",
    "D5",
    "D6",
    "D7",
    "CP",
    "nQ7 (datasheet Q7, inverted)",
    "nQ6 (datasheet Q6, inverted)",
    "nQ5 (datasheet Q5, inverted)",
    "nQ4 (datasheet Q4, inverted)",
    "nQ3 (datasheet Q3, inverted)",
    "nQ2 (datasheet Q2, inverted)",
    "nQ1 (datasheet Q1, inverted)",
    "nQ0 (datasheet Q0, inverted)"
  };

  /** Creates a 74564 octal positive-edge D flip-flop with inverting three-state outputs. */
  public Ttl74564() {
    super(_ID, (byte) 20, OUTPUT_PINS, PORT_NAMES, new Ttl74564HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 8));
    g.drawRect(x + 165, y + 12, 10, 10);
    g.drawRect(x + 145, y + 12, 10, 10);
    g.drawRect(x + 125, y + 12, 10, 10);
    g.drawRect(x + 105, y + 12, 10, 10);
    g.drawRect(x + 85, y + 12, 10, 10);
    g.drawRect(x + 65, y + 12, 10, 10);
    g.drawRect(x + 45, y + 12, 10, 10);
    g.drawRect(x + 25, y + 12, 10, 10);
    g.drawLine(x + 170, y + 12, x + 170, y + 7);
    g.drawLine(x + 150, y + 12, x + 150, y + 7);
    g.drawLine(x + 130, y + 12, x + 130, y + 7);
    g.drawLine(x + 110, y + 12, x + 110, y + 7);
    g.drawLine(x + 90, y + 12, x + 90, y + 7);
    g.drawLine(x + 70, y + 12, x + 70, y + 7);
    g.drawLine(x + 50, y + 12, x + 50, y + 7);
    g.drawLine(x + 30, y + 12, x + 30, y + 7);
    g.drawLine(x + 25, y + 24, x + 175, y + 24);
    g.drawLine(x + 10, y + height - 7, x + 10, y + height - 9);
    g.drawLine(x + 10, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 24);
    g.drawLine(x + 21, y + 24, x + 25, y + 24);
    g.drawLine(x + 167, y + 24, x + 167, y + 22);
    g.drawLine(x + 147, y + 24, x + 147, y + 22);
    g.drawLine(x + 127, y + 24, x + 127, y + 22);
    g.drawLine(x + 107, y + 24, x + 107, y + 22);
    g.drawLine(x + 87, y + 24, x + 87, y + 22);
    g.drawLine(x + 67, y + 24, x + 67, y + 22);
    g.drawLine(x + 47, y + 24, x + 47, y + 22);
    g.drawLine(x + 27, y + 24, x + 27, y + 22);
    g.drawLine(x + 25, y + 25, x + 175, y + 25);
    g.drawLine(x + 190, y + 7, x + 190, y + 9);
    g.drawLine(x + 190, y + 9, x + 21, y + 9);
    g.drawLine(x + 21, y + 9, x + 21, y + 25);
    g.drawLine(x + 21, y + 25, x + 25, y + 25);
    g.drawLine(x + 170, y + 25, x + 170, y + 22);
    g.drawLine(x + 150, y + 25, x + 150, y + 22);
    g.drawLine(x + 130, y + 25, x + 130, y + 22);
    g.drawLine(x + 110, y + 25, x + 110, y + 22);
    g.drawLine(x + 90, y + 25, x + 90, y + 22);
    g.drawLine(x + 70, y + 25, x + 70, y + 22);
    g.drawLine(x + 50, y + 25, x + 50, y + 22);
    g.drawLine(x + 30, y + 25, x + 30, y + 22);
    g.drawLine(x + 30, y + height - 7, x + 30, y + 25);
    g.drawLine(x + 30, y + 25, x + 27, y + 25);
    g.drawLine(x + 27, y + 25, x + 27, y + 22);
    g.drawLine(x + 50, y + height - 7, x + 50, y + 25);
    g.drawLine(x + 50, y + 25, x + 47, y + 25);
    g.drawLine(x + 47, y + 25, x + 47, y + 22);
    g.drawLine(x + 70, y + height - 7, x + 70, y + 25);
    g.drawLine(x + 70, y + 25, x + 67, y + 25);
    g.drawLine(x + 67, y + 25, x + 67, y + 22);
    g.drawLine(x + 90, y + height - 7, x + 90, y + 25);
    g.drawLine(x + 90, y + 25, x + 87, y + 25);
    g.drawLine(x + 87, y + 25, x + 87, y + 22);
    g.drawLine(x + 110, y + height - 7, x + 110, y + 25);
    g.drawLine(x + 110, y + 25, x + 107, y + 25);
    g.drawLine(x + 107, y + 25, x + 107, y + 22);
    g.drawLine(x + 130, y + height - 7, x + 130, y + 25);
    g.drawLine(x + 130, y + 25, x + 127, y + 25);
    g.drawLine(x + 127, y + 25, x + 127, y + 22);
    g.drawLine(x + 150, y + height - 7, x + 150, y + 25);
    g.drawLine(x + 150, y + 25, x + 147, y + 25);
    g.drawLine(x + 147, y + 25, x + 147, y + 22);
    g.drawLine(x + 170, y + height - 7, x + 170, y + 25);
    g.drawLine(x + 170, y + 25, x + 167, y + 25);
    g.drawLine(x + 167, y + 25, x + 167, y + 22);
    g.drawOval(x + 19, y + 22, 4, 4);
    GraphicsUtil.drawCenteredText(g, "7", x + 170, y + 17);
    GraphicsUtil.drawCenteredText(g, "6", x + 150, y + 17);
    GraphicsUtil.drawCenteredText(g, "5", x + 130, y + 17);
    GraphicsUtil.drawCenteredText(g, "4", x + 110, y + 17);
    GraphicsUtil.drawCenteredText(g, "3", x + 90, y + 17);
    GraphicsUtil.drawCenteredText(g, "2", x + 70, y + 17);
    GraphicsUtil.drawCenteredText(g, "1", x + 50, y + 17);
    GraphicsUtil.drawCenteredText(g, "0", x + 30, y + 17);
    g.setFont(previousFont);
  }



  /** Draws the stored flip-flop bits. The pins show the complement of these bits. */

  @Override
  public void propagateTtl(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.ONE, BITS);
      state.setData(data);
    }
    if (data.updateClock(state.getPortValue(PORT_INDEX_CP), StdAttr.TRIG_RISING)) {
      for (var bit = 0; bit < BITS; bit++) {
        data.setValue(bit, state.getPortValue(DATA[bit]));
      }
    }
    final var enabled = state.getPortValue(PORT_INDEX_nOE) == Value.FALSE;
    for (var bit = 0; bit < BITS; bit++) {
      final var stored = data.getValue(bit);
      state.setPort(OUTPUTS[bit], enabled ? invert(stored) : Value.UNKNOWN, DELAY);
    }
  }

  /**
   * Complements a stored bit. A one-bit {@link Value#not()} turns unknown into error, but an
   * undefined input stays undefined at the inverting output.
   */
  private static Value invert(Value bit) {
    if (bit == Value.TRUE) return Value.FALSE;
    if (bit == Value.FALSE) return Value.TRUE;
    return bit;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_CP};
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
