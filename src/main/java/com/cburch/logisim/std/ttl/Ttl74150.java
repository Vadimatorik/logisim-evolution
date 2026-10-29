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
 * TTL 74x150: 16-line to 1-line data selector/multiplexer.
 *
 * <p>Model based on the National Semiconductor DM74150 datasheet (TL/F/6546, June 1989), which
 * matches the TI SN74150 function table. The 74HC150 uses the same digital behavior and DIP-24
 * pinout. The output is the complement of the selected data input. A high strobe forces that
 * output high.
 *
 * <p>A high strobe overrides the address and the data inputs, including unknown and error values.
 * When the strobe is low and the address is fully defined, an unknown selected input stays unknown
 * and an error stays an error. An undefined address or strobe produces an error when any involved
 * value is an error, and unknown otherwise.
 */
public class Ttl74150 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74150";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  // Data inputs
  public static final byte E7 = 1;
  public static final byte E6 = 2;
  public static final byte E5 = 3;
  public static final byte E4 = 4;
  public static final byte E3 = 5;
  public static final byte E2 = 6;
  public static final byte E1 = 7;
  public static final byte E0 = 8;
  public static final byte E15 = 16;
  public static final byte E14 = 17;
  public static final byte E13 = 18;
  public static final byte E12 = 19;
  public static final byte E11 = 20;
  public static final byte E10 = 21;
  public static final byte E9 = 22;
  public static final byte E8 = 23;

  /** Active-low strobe. A high level forces the output high. */
  public static final byte G = 9;

  /** Inverted multiplexer output. */
  public static final byte W = 10;

  // Select inputs. A is the least significant bit and D is the most significant bit.
  public static final byte D = 11;
  public static final byte C = 13;
  public static final byte B = 14;
  public static final byte A = 15;

  // Power supply
  public static final byte GND = 12;
  public static final byte VCC = 24;

  /** Data inputs ordered by the address that selects them. */
  private static final byte[] DATA = {
    E0, E1, E2, E3, E4, E5, E6, E7, E8, E9, E10, E11, E12, E13, E14, E15
  };

  /** Select inputs ordered from the least significant bit to the most significant bit. */
  private static final byte[] SELECT = {A, B, C, D};

  private static final byte[] OUTPUTS = {W};

  private static final String[] PORT_NAMES = {
    "E7 Data input 7",
    "E6 Data input 6",
    "E5 Data input 5",
    "E4 Data input 4",
    "E3 Data input 3",
    "E2 Data input 2",
    "E1 Data input 1",
    "E0 Data input 0",
    "nG Strobe input",
    "W Inverted output",
    "D Select input (MSB)",
    "C Select input",
    "B Select input",
    "A Select input (LSB)",
    "E15 Data input 15",
    "E14 Data input 14",
    "E13 Data input 13",
    "E12 Data input 12",
    "E11 Data input 11",
    "E10 Data input 10",
    "E9 Data input 9",
    "E8 Data input 8"
  };

  /** Creates a 74150 16-line to 1-line data selector. */
  public Ttl74150() {
    super(_ID, (byte) 24, OUTPUTS, PORT_NAMES, new Ttl74150HdlGenerator());
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
    final var top = y + 18;
    final var bottom = y + 42;
    g.drawRect(x + 6, top, 228, bottom - top);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 9));
    GraphicsUtil.drawCenteredText(g, "MUX", x + 120, y + 30);
    g.setFont(previousFont);
    for (var pin = 1; pin <= 24; pin++) {
      if (pin == GND || pin == 24) {
        continue;
      }
      final var pinX = pin <= 12 ? x + (pin - 1) * 20 + 10 : x + (24 - pin) * 20 + 10;
      final var pinY = pin <= 12 ? y + height - AbstractTtlGate.PIN_HEIGHT : y + AbstractTtlGate.PIN_HEIGHT;
      final var edge = pin <= 12 ? bottom : top;
      if (pin == G || pin == W) {
        final var bubbleY = pin <= 12 ? edge + 4 : edge - 4;
        final var stop = pin <= 12 ? bubbleY + 2 : bubbleY - 2;
        g.drawLine(pinX, pinY, pinX, stop);
        structureBubble(g, pinX, bubbleY);
      } else {
        g.drawLine(pinX, pinY, pinX, edge);
      }
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    state.setPort(pinNrToPortNr(W), outputValue(state), DELAY);
  }

  private static Value outputValue(InstanceState state) {
    final var strobe = input(state, G);
    if (strobe == Value.TRUE) {
      return Value.TRUE;
    }
    if (strobe == Value.ERROR) {
      return Value.ERROR;
    }
    final var address = address(state);
    if (strobe == Value.FALSE && address >= 0) {
      return invert(input(state, DATA[address]));
    }
    return containsError(state) ? Value.ERROR : Value.UNKNOWN;
  }

  /**
   * Decodes the select inputs.
   *
   * @return the selected data-input number, or -1 when any select input is not a binary level
   */
  private static int address(InstanceState state) {
    var address = 0;
    var weight = 1;
    for (final var pin : SELECT) {
      final var level = input(state, pin);
      if (level != Value.TRUE && level != Value.FALSE) {
        return -1;
      }
      if (level == Value.TRUE) {
        address += weight;
      }
      weight <<= 1;
    }
    return address;
  }

  private static boolean containsError(InstanceState state) {
    for (final var pin : SELECT) {
      if (input(state, pin) == Value.ERROR) {
        return true;
      }
    }
    for (final var pin : DATA) {
      if (input(state, pin) == Value.ERROR) {
        return true;
      }
    }
    return false;
  }

  private static Value invert(Value value) {
    if (value == Value.TRUE) {
      return Value.FALSE;
    }
    if (value == Value.FALSE) {
      return Value.TRUE;
    }
    if (value == Value.UNKNOWN) {
      return Value.UNKNOWN;
    }
    return Value.ERROR;
  }

  private static Value input(InstanceState state, byte dsPinNr) {
    return state.getPortValue(pinNrToPortNr(dsPinNr));
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
