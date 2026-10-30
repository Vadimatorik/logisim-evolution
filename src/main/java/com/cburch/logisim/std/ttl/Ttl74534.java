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
 * TTL 74HC534: octal positive-edge D flip-flop with inverting three-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/cd74hc534.pdf">TI CD74HC534</a> data sheet. The part
 * is the inverting twin of the 74HC374: the DIP-20 pinout matches, and each output is the
 * complement of the stored bit. Each flip-flop stores its D input on the rising edge of CLK. A low
 * nOE drives that complement; the data sheet names those pins 1Q–8Q (Nexperia Q0–Q7). A high nOE,
 * or any level that is not a solid low, releases every output. Output enable does not change the
 * register, and the register still loads while the outputs are released. High impedance is
 * reported as unknown. Nanosecond delays are not modeled. Pin names follow the TI numbering used
 * by the 74273 and 74377 in this library, where nQ1 is Nexperia's Q0 and D1 is Nexperia's D0.
 */
public class Ttl74534 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74534";

  public static final int PORT_INDEX_nOE = 0;
  public static final int PORT_INDEX_nQ1 = 1;
  public static final int PORT_INDEX_D1 = 2;
  public static final int PORT_INDEX_D2 = 3;
  public static final int PORT_INDEX_nQ2 = 4;
  public static final int PORT_INDEX_nQ3 = 5;
  public static final int PORT_INDEX_D3 = 6;
  public static final int PORT_INDEX_D4 = 7;
  public static final int PORT_INDEX_nQ4 = 8;
  public static final int PORT_INDEX_CLK = 9;
  public static final int PORT_INDEX_nQ5 = 10;
  public static final int PORT_INDEX_D5 = 11;
  public static final int PORT_INDEX_D6 = 12;
  public static final int PORT_INDEX_nQ6 = 13;
  public static final int PORT_INDEX_nQ7 = 14;
  public static final int PORT_INDEX_D7 = 15;
  public static final int PORT_INDEX_D8 = 16;
  public static final int PORT_INDEX_nQ8 = 17;
  /** Port index of GND when the explicit power pins are shown. */
  public static final int PORT_INDEX_GND = 18;
  /** Port index of VCC when the explicit power pins are shown. */
  public static final int PORT_INDEX_VCC = 19;

  private static final int DELAY = 1;
  private static final int BITS = 8;
  /** Data port of each bit. Bit 0 is D1, which is Nexperia's D0. */
  private static final int[] DATA = {
    PORT_INDEX_D1,
    PORT_INDEX_D2,
    PORT_INDEX_D3,
    PORT_INDEX_D4,
    PORT_INDEX_D5,
    PORT_INDEX_D6,
    PORT_INDEX_D7,
    PORT_INDEX_D8
  };
  /** Inverting output of each bit. Bit 0 is nQ1, the complement of the bit stored from D1. */
  private static final int[] OUTPUTS = {
    PORT_INDEX_nQ1,
    PORT_INDEX_nQ2,
    PORT_INDEX_nQ3,
    PORT_INDEX_nQ4,
    PORT_INDEX_nQ5,
    PORT_INDEX_nQ6,
    PORT_INDEX_nQ7,
    PORT_INDEX_nQ8
  };
  private static final byte[] OUTPUT_PINS = {2, 5, 6, 9, 12, 15, 16, 19};
  private static final String[] PORT_NAMES = {
    "nOE (output enable, active low)",
    "nQ1 (datasheet 1Q, inverted)",
    "D1",
    "D2",
    "nQ2 (datasheet 2Q, inverted)",
    "nQ3 (datasheet 3Q, inverted)",
    "D3",
    "D4",
    "nQ4 (datasheet 4Q, inverted)",
    "CLK",
    "nQ5 (datasheet 5Q, inverted)",
    "D5",
    "D6",
    "nQ6 (datasheet 6Q, inverted)",
    "nQ7 (datasheet 7Q, inverted)",
    "D7",
    "D8",
    "nQ8 (datasheet 8Q, inverted)"
  };

  /** Creates a 74534 octal positive-edge D flip-flop with inverting three-state outputs. */
  public Ttl74534() {
    super(_ID, (byte) 20, OUTPUT_PINS, PORT_NAMES, new Ttl74534HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 8));
    g.drawRect(x + 25, y + 28, 10, 10);
    g.drawRect(x + 85, y + 28, 10, 10);
    g.drawRect(x + 105, y + 28, 10, 10);
    g.drawRect(x + 165, y + 28, 10, 10);
    g.drawRect(x + 165, y + 12, 10, 10);
    g.drawRect(x + 105, y + 12, 10, 10);
    g.drawRect(x + 85, y + 12, 10, 10);
    g.drawRect(x + 25, y + 12, 10, 10);
    g.drawLine(x + 30, y + height - 22, x + 30, y + height - 7);
    g.drawLine(x + 90, y + height - 22, x + 90, y + height - 7);
    g.drawLine(x + 110, y + height - 22, x + 110, y + height - 7);
    g.drawLine(x + 170, y + height - 22, x + 170, y + height - 7);
    g.drawLine(x + 170, y + 12, x + 170, y + 7);
    g.drawLine(x + 110, y + 12, x + 110, y + 7);
    g.drawLine(x + 90, y + 12, x + 90, y + 7);
    g.drawLine(x + 30, y + 12, x + 30, y + 7);
    g.drawLine(x + 25, y + 24, x + 175, y + 24);
    g.drawLine(x + 10, y + height - 7, x + 10, y + height - 9);
    g.drawLine(x + 10, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 24);
    g.drawLine(x + 21, y + 24, x + 25, y + 24);
    g.drawLine(x + 27, y + 24, x + 27, y + 28);
    g.drawLine(x + 87, y + 24, x + 87, y + 28);
    g.drawLine(x + 107, y + 24, x + 107, y + 28);
    g.drawLine(x + 167, y + 24, x + 167, y + 28);
    g.drawLine(x + 167, y + 24, x + 167, y + 22);
    g.drawLine(x + 107, y + 24, x + 107, y + 22);
    g.drawLine(x + 87, y + 24, x + 87, y + 22);
    g.drawLine(x + 27, y + 24, x + 27, y + 22);
    g.drawLine(x + 25, y + 25, x + 175, y + 25);
    g.drawLine(x + 190, y + 7, x + 190, y + 9);
    g.drawLine(x + 190, y + 9, x + 21, y + 9);
    g.drawLine(x + 21, y + 9, x + 21, y + 25);
    g.drawLine(x + 21, y + 25, x + 25, y + 25);
    g.drawLine(x + 30, y + 25, x + 30, y + 28);
    g.drawLine(x + 90, y + 25, x + 90, y + 28);
    g.drawLine(x + 110, y + 25, x + 110, y + 28);
    g.drawLine(x + 170, y + 25, x + 170, y + 28);
    g.drawLine(x + 170, y + 25, x + 170, y + 22);
    g.drawLine(x + 110, y + 25, x + 110, y + 22);
    g.drawLine(x + 90, y + 25, x + 90, y + 22);
    g.drawLine(x + 30, y + 25, x + 30, y + 22);
    g.drawLine(x + 50, y + height - 7, x + 50, y + height - 19);
    g.drawLine(x + 50, y + height - 19, x + 27, y + height - 19);
    g.drawLine(x + 27, y + height - 19, x + 27, y + height - 22);
    g.drawLine(x + 70, y + height - 7, x + 70, y + height - 19);
    g.drawLine(x + 70, y + height - 19, x + 87, y + height - 19);
    g.drawLine(x + 87, y + height - 19, x + 87, y + height - 22);
    g.drawLine(x + 130, y + height - 7, x + 130, y + height - 19);
    g.drawLine(x + 130, y + height - 19, x + 107, y + height - 19);
    g.drawLine(x + 107, y + height - 19, x + 107, y + height - 22);
    g.drawLine(x + 150, y + height - 7, x + 150, y + height - 19);
    g.drawLine(x + 150, y + height - 19, x + 167, y + height - 19);
    g.drawLine(x + 167, y + height - 19, x + 167, y + height - 22);
    g.drawLine(x + 150, y + 7, x + 150, y + 9);
    g.drawLine(x + 150, y + 9, x + 167, y + 9);
    g.drawLine(x + 167, y + 9, x + 167, y + 12);
    g.drawLine(x + 130, y + 7, x + 130, y + 9);
    g.drawLine(x + 130, y + 9, x + 107, y + 9);
    g.drawLine(x + 107, y + 9, x + 107, y + 12);
    g.drawLine(x + 70, y + 7, x + 70, y + 9);
    g.drawLine(x + 70, y + 9, x + 87, y + 9);
    g.drawLine(x + 87, y + 9, x + 87, y + 12);
    g.drawLine(x + 50, y + 7, x + 50, y + 9);
    g.drawLine(x + 50, y + 9, x + 27, y + 9);
    g.drawLine(x + 27, y + 9, x + 27, y + 12);
    g.drawOval(x + 19, y + 22, 4, 4);
    GraphicsUtil.drawCenteredText(g, "1", x + 30, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "2", x + 90, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "3", x + 110, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "4", x + 170, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "5", x + 170, y + 17);
    GraphicsUtil.drawCenteredText(g, "6", x + 110, y + 17);
    GraphicsUtil.drawCenteredText(g, "7", x + 90, y + 17);
    GraphicsUtil.drawCenteredText(g, "8", x + 30, y + 17);
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
    if (data.updateClock(state.getPortValue(PORT_INDEX_CLK), StdAttr.TRIG_RISING)) {
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
    return new int[] {PORT_INDEX_CLK};
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
