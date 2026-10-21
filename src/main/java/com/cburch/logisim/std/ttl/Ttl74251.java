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
import java.awt.Graphics;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x251: 8-line to 1-line data selector with three-state outputs.
 *
 * <p>Simulation follows the Nexperia 74HC251 and TI CD74HC251 data sheets. The pinout matches
 * the TI SN74HC251, which names the same pins D0-D7, A/B/C, Y/W and G. Pin names in this model
 * are the Nexperia names. A low nOE routes the input selected by S2, S1 and S0 to Y and its
 * complement to nY. Any other level on nOE, including unknown and error, releases both outputs.
 * A select bit that is neither high nor low makes both outputs unknown while enabled. The
 * selected input is copied unchanged, so an unknown input stays unknown on both Y and nY.
 * High impedance is reported as {@link Value#UNKNOWN}. Nanosecond delays are not modeled.
 *
 * @see <a href="https://www.ti.com/lit/ds/symlink/cd74hct251.pdf">TI CD74HC251</a>
 * @see <a href="https://www.ti.com/lit/ds/symlink/sn74hc251.pdf">TI SN74HC251</a>
 * @see <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT251.pdf">Nexperia</a>
 */
public class Ttl74251 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74251";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet.

  // Inputs
  public static final byte I3 = 1;
  public static final byte I2 = 2;
  public static final byte I1 = 3;
  public static final byte I0 = 4;

  public static final byte NOE = 7;

  public static final byte S2 = 9;
  public static final byte S1 = 10;
  public static final byte S0 = 11;

  public static final byte I7 = 12;
  public static final byte I6 = 13;
  public static final byte I5 = 14;
  public static final byte I4 = 15;

  // Outputs
  public static final byte Y = 5;
  public static final byte NY = 6;

  // Power supply
  public static final byte GND = 8;
  public static final byte VCC = 16;

  /** Data inputs ordered by the address that selects them. */
  private static final byte[] DATA = {I0, I1, I2, I3, I4, I5, I6, I7};

  /** Select inputs, with S0 as the least significant bit. */
  private static final byte[] SELECT = {S0, S1, S2};

  private static final byte[] OUTPUTS = {Y, NY};

  private static final String[] PORT_NAMES = {
    "I3 Data input 3",
    "I2 Data input 2",
    "I1 Data input 1",
    "I0 Data input 0",
    "Y Multiplexer output",
    "nY Complementary multiplexer output",
    "nOE Output enable (active low)",
    "S2 Select input (MSB)",
    "S1 Select input",
    "S0 Select input (LSB)",
    "I7 Data input 7",
    "I6 Data input 6",
    "I5 Data input 5",
    "I4 Data input 4"
  };

  /** Creates a 74251 8-line to 1-line data selector with three-state outputs. */
  public Ttl74251() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74251HdlGenerator());
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
    g.drawLine((x + 10), (y + 34), (x + 50), (y + 18));
    g.drawLine((x + 90), (y + 34), (x + 50), (y + 18));
    g.drawLine((x + 10), (y + 34), (x + 90), (y + 34));
    g.drawLine((x + 50), (y + 18), (x + 90), (y + height - 7));
    g.drawLine((x + 70), (y + height - 7), (x + 70), (y + 34));
    g.drawLine((x + 50), (y + height - 7), (x + 50), (y + 34));
    g.drawLine((x + 30), (y + height - 7), (x + 30), (y + 34));
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + 34));
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 34));
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 34));
    g.drawLine((x + 70), (y + 7), (x + 70), (y + 34));
    g.drawLine((x + 90), (y + 7), (x + 90), (y + 34));
    g.drawLine((x + 110), (y + 7), (x + 110), (y + 16));
    GraphicsUtil.drawCenteredText(g, "S", (x + 110), (y + 16));
    g.drawLine((x + 130), (y + 7), (x + 130), (y + 16));
    GraphicsUtil.drawCenteredText(g, "S", (x + 130), (y + 16));
    g.drawLine((x + 150), (y + 7), (x + 150), (y + 16));
    GraphicsUtil.drawCenteredText(g, "S", (x + 150), (y + 16));
    g.drawLine((x + 130), (y + height - 7), (x + 130), (y + 40));
    structureBubble(g, (x + 130), (y + height - 7));
  }

  private static Value portValue(InstanceState state, byte dsPinNr) {
    return state.getPortValue(pinNrToPortNr(dsPinNr));
  }

  private static void setPort(InstanceState state, byte dsPinNr, Value value) {
    state.setPort(pinNrToPortNr(dsPinNr), value, DELAY);
  }

  /** Complements a bit without turning an unknown level into an error. */
  private static Value complement(Value value) {
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

  private static void releaseOutputs(InstanceState state) {
    setPort(state, Y, Value.UNKNOWN);
    setPort(state, NY, Value.UNKNOWN);
  }

  /**
   * Reads the select inputs.
   *
   * @return the address 0..7, or -1 when any select bit is not a solid high or low
   */
  private static int selectedAddress(InstanceState state) {
    var address = 0;
    for (var bit = 0; bit < SELECT.length; bit++) {
      final var level = portValue(state, SELECT[bit]);
      if (level != Value.TRUE && level != Value.FALSE) {
        return -1;
      }
      if (level == Value.TRUE) {
        address |= 1 << bit;
      }
    }
    return address;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    if (portValue(state, NOE) != Value.FALSE) {
      releaseOutputs(state);
      return;
    }
    final var address = selectedAddress(state);
    if (address < 0) {
      releaseOutputs(state);
      return;
    }
    final var selected = portValue(state, DATA[address]);
    setPort(state, Y, selected);
    setPort(state, NY, complement(selected));
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
