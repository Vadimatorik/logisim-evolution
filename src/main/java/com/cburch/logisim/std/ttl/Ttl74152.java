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
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x152: 1-of-8-line data selector/multiplexer.
 *
 * <p>Model based on the <a
 * href="https://www.renesas.com/en/document/dst/hd74hc152-datasheet">HD74HC152 datasheet</a>
 * (Renesas REJ03D0576). The single output {@code W} follows the selected data input. It is not the
 * complement, unlike the {@code W} output of the 74151, and there is no enable input.
 *
 * <p>Select input {@code A} is the least significant bit and {@code C} is the most significant, so
 * the address {@code CBA} chooses {@code D0} through {@code D7}. An error on a select bit makes
 * {@code W} an error. Any other non-boolean select bit makes {@code W} unknown. A known address
 * copies the selected input, including unknown and error values.
 */
public class Ttl74152 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading. Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74152";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  // Inputs
  public static final byte D4 = 1;
  public static final byte D3 = 2;
  public static final byte D2 = 3;
  public static final byte D1 = 4;
  public static final byte D0 = 5;

  public static final byte C = 8;
  public static final byte B = 9;
  public static final byte A = 10;

  public static final byte D7 = 11;
  public static final byte D6 = 12;
  public static final byte D5 = 13;

  // Output
  public static final byte W = 6;

  // Power supply
  public static final byte GND = 7;
  public static final byte VCC = 14;

  /** Data inputs ordered by the address that selects them. */
  private static final byte[] DATA = new byte[] {D0, D1, D2, D3, D4, D5, D6, D7};

  private static final byte[] OUTPUTS = new byte[] {W};

  private static final String[] PORT_NAMES = {
    "D4 Data input 4",
    "D3 Data input 3",
    "D2 Data input 2",
    "D1 Data input 1",
    "D0 Data input 0",
    "W Multiplexer output",
    "C Select input (MSB)",
    "B Select input",
    "A Select input (LSB)",
    "D7 Data input 7",
    "D6 Data input 6",
    "D5 Data input 5"
  };

  /** Creates a 74152 8-line to 1-line data selector. */
  public Ttl74152() {
    super(_ID, (byte) 14, OUTPUTS, PORT_NAMES, new Ttl74152HdlGenerator());
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
    g.drawRect(x + 74, y + 16, 12, 28);
    g.drawLine(x + 10, y + height - 7, x + 10, y + 19);
    g.drawLine(x + 10, y + 19, x + 74, y + 19);
    g.drawLine(x + 30, y + height - 7, x + 30, y + 22);
    g.drawLine(x + 30, y + 22, x + 74, y + 22);
    g.drawLine(x + 50, y + height - 7, x + 50, y + 25);
    g.drawLine(x + 50, y + 25, x + 74, y + 25);
    g.drawLine(x + 70, y + height - 7, x + 70, y + 28);
    g.drawLine(x + 70, y + 28, x + 74, y + 28);
    g.drawLine(x + 70, y + 7, x + 70, y + height - 29);
    g.drawLine(x + 70, y + height - 29, x + 74, y + height - 29);
    g.drawLine(x + 50, y + 7, x + 50, y + height - 26);
    g.drawLine(x + 50, y + height - 26, x + 74, y + height - 26);
    g.drawLine(x + 30, y + 7, x + 30, y + height - 23);
    g.drawLine(x + 30, y + height - 23, x + 74, y + height - 23);
    g.drawLine(x + 90, y + height - 7, x + 90, y + 19);
    g.drawLine(x + 90, y + 19, x + 86, y + 19);
    g.drawLine(x + 86, y + height - 30, x + 110, y + height - 30);
    g.drawLine(x + 110, y + height - 30, x + 110, y + height - 7);
    g.drawLine(x + 130, y + 7, x + 130, y + 13);
    g.drawLine(x + 130, y + 13, x + 78, y + 13);
    g.drawLine(x + 78, y + 13, x + 78, y + 16);
    g.drawLine(x + 110, y + 7, x + 110, y + 13);
    g.drawLine(x + 110, y + 13, x + 81, y + 13);
    g.drawLine(x + 81, y + 13, x + 81, y + 16);
    g.drawLine(x + 90, y + 7, x + 90, y + 13);
    g.drawLine(x + 90, y + 13, x + 84, y + 13);
    g.drawLine(x + 84, y + 13, x + 84, y + 16);
    GraphicsUtil.drawCenteredText(g, "M", x + 80, y + height - 30);
    g.setFont(previousFont);
  }



  private static Value pinValue(InstanceState state, byte dsPinNr) {
    return state.getPortValue(pinNrToPortNr(dsPinNr));
  }

  /**
   * Resolves {@code W} from the select inputs.
   *
   * @return the selected data value, or unknown/error when the address is not a clean binary code
   */
  private static Value selectedData(InstanceState state) {
    final var bits = new Value[] {pinValue(state, C), pinValue(state, B), pinValue(state, A)};
    var address = 0;
    var unknown = false;
    for (var index = 0; index < bits.length; index++) {
      final var bit = bits[index];
      if (bit == Value.ERROR) {
        return Value.ERROR;
      }
      if (bit == Value.TRUE) {
        address |= 1 << (bits.length - 1 - index);
      } else if (bit != Value.FALSE) {
        unknown = true;
      }
    }
    if (unknown) {
      return Value.UNKNOWN;
    }
    return pinValue(state, DATA[address]);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    state.setPort(pinNrToPortNr(W), selectedData(state), DELAY);
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
