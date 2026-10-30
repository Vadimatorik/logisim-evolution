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
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x253: dual 4-line to 1-line data selector with 3-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT253.pdf">Nexperia 74HC253</a>
 * and <a href="https://www.ti.com/lit/ds/symlink/sn74hc253.pdf">TI SN74HC253</a> function tables.
 * A low output enable passes the selected source without inversion. A high output enable releases
 * that multiplexer only. The two select inputs are shared: S1 is the MSB and S0 is the LSB. Pin
 * names follow Nexperia. TI calls the same pins 1G/2G, B/A and 1C0..2C3. Nanosecond delays are not
 * modeled.
 *
 * <p>An unknown or error input changes an output only when the accepted substitutions disagree. An
 * error on such an input makes the disagreed output an error; an unknown input makes it unknown. A
 * high output enable releases its output even if a data input is an error. A released output is
 * {@link Value#UNKNOWN}, which is how this library represents high impedance.
 */
public class Ttl74253 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74253";

  public static final int DELAY = 1;

  public static final byte OE1 = 1;
  public static final byte S1 = 2;
  public static final byte L1_I3 = 3;
  public static final byte L1_I2 = 4;
  public static final byte L1_I1 = 5;
  public static final byte L1_I0 = 6;
  public static final byte L1_Y = 7;
  public static final byte GND = 8;
  public static final byte L2_Y = 9;
  public static final byte L2_I0 = 10;
  public static final byte L2_I1 = 11;
  public static final byte L2_I2 = 12;
  public static final byte L2_I3 = 13;
  public static final byte S0 = 14;
  public static final byte OE2 = 15;
  public static final byte VCC = 16;

  /** Each row is I0, I1, I2, I3, the output, and the output enable of one multiplexer. */
  private static final byte[][] CHANNELS = {
    {L1_I0, L1_I1, L1_I2, L1_I3, L1_Y, OE1},
    {L2_I0, L2_I1, L2_I2, L2_I3, L2_Y, OE2}
  };

  private static final byte[] OUTPUTS = {L1_Y, L2_Y};

  private static final String[] PORT_NAMES = {
    "n1OE (Output enable, active low)",
    "S1 (Select, MSB)",
    "1I3",
    "1I2",
    "1I1",
    "1I0",
    "1Y",
    "2Y",
    "2I0",
    "2I1",
    "2I2",
    "2I3",
    "S0 (Select, LSB)",
    "n2OE (Output enable, active low)"
  };

  /** Creates a 74253 dual 4-line to 1-line data selector with 3-state outputs. */
  public Ttl74253() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74253HdlGenerator());
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
    g.drawRect(x + 125, y + 28, 10, 10);
    g.drawRect(x + 145, y + 12, 10, 10);
    g.drawLine(x + 130, y + height - 22, x + 130, y + height - 7);
    g.drawLine(x + 150, y + 12, x + 150, y + 7);
    g.drawLine(x + 125, y + 24, x + 155, y + 24);
    g.drawLine(x + 10, y + height - 7, x + 10, y + height - 9);
    g.drawLine(x + 10, y + height - 9, x + 121, y + height - 9);
    g.drawLine(x + 121, y + height - 9, x + 121, y + 24);
    g.drawLine(x + 121, y + 24, x + 125, y + 24);
    g.drawLine(x + 127, y + 24, x + 127, y + 28);
    g.drawLine(x + 147, y + 24, x + 147, y + 22);
    g.drawLine(x + 125, y + 24, x + 155, y + 24);
    g.drawLine(x + 30, y + height - 7, x + 30, y + height - 9);
    g.drawLine(x + 30, y + height - 9, x + 121, y + height - 9);
    g.drawLine(x + 121, y + height - 9, x + 121, y + 24);
    g.drawLine(x + 121, y + 24, x + 125, y + 24);
    g.drawLine(x + 130, y + 24, x + 130, y + 28);
    g.drawLine(x + 150, y + 24, x + 150, y + 22);
    g.drawLine(x + 125, y + 25, x + 155, y + 25);
    g.drawLine(x + 50, y + 7, x + 50, y + 9);
    g.drawLine(x + 50, y + 9, x + 121, y + 9);
    g.drawLine(x + 121, y + 9, x + 121, y + 25);
    g.drawLine(x + 121, y + 25, x + 125, y + 25);
    g.drawLine(x + 133, y + 25, x + 133, y + 28);
    g.drawLine(x + 153, y + 25, x + 153, y + 22);
    g.drawLine(x + 125, y + 25, x + 155, y + 25);
    g.drawLine(x + 30, y + 7, x + 30, y + 9);
    g.drawLine(x + 30, y + 9, x + 121, y + 9);
    g.drawLine(x + 121, y + 9, x + 121, y + 25);
    g.drawLine(x + 121, y + 25, x + 125, y + 25);
    g.drawLine(x + 127, y + 25, x + 127, y + 28);
    g.drawLine(x + 147, y + 25, x + 147, y + 22);
    g.drawLine(x + 50, y + height - 7, x + 50, y + height - 19);
    g.drawLine(x + 50, y + height - 19, x + 127, y + height - 19);
    g.drawLine(x + 127, y + height - 19, x + 127, y + height - 22);
    g.drawLine(x + 70, y + height - 7, x + 70, y + 25);
    g.drawLine(x + 70, y + 25, x + 147, y + 25);
    g.drawLine(x + 147, y + 25, x + 147, y + 22);
    g.drawOval(x + 119, y + 22, 4, 4);
    g.drawOval(x + 119, y + 23, 4, 4);
    GraphicsUtil.drawCenteredText(g, "Y", x + 130, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "Y", x + 150, y + 17);
    g.setFont(previousFont);
  }







  @Override
  public void propagateTtl(InstanceState state) {
    final var s1 = state.getPortValue(pinNrToPortNr(S1));
    final var s0 = state.getPortValue(pinNrToPortNr(S0));
    for (final var channel : CHANNELS) {
      final var sources = new Value[4];
      for (var index = 0; index < sources.length; index++) {
        sources[index] = state.getPortValue(pinNrToPortNr(channel[index]));
      }
      state.setPort(
          pinNrToPortNr(channel[4]),
          channelOutput(state.getPortValue(pinNrToPortNr(channel[5])), s1, s0, sources),
          DELAY);
    }
  }

  /**
   * Resolves one multiplexer output, including unknown and error substitutions.
   *
   * <p>A definitely high output enable releases the pin and does not consult the data inputs.
   */
  private static Value channelOutput(Value outputEnable, Value s1, Value s0, Value[] sources) {
    if (outputEnable == Value.TRUE) {
      return Value.UNKNOWN;
    }
    final var driven = selected(s1, s0, sources);
    if (outputEnable == Value.FALSE) {
      return driven;
    }
    if (driven == Value.UNKNOWN && outputEnable == Value.UNKNOWN) {
      return Value.UNKNOWN;
    }
    if (outputEnable == Value.ERROR || driven == Value.ERROR) {
      return Value.ERROR;
    }
    return Value.UNKNOWN;
  }

  /**
   * Returns the source chosen by the select inputs.
   *
   * <p>When a select input is not binary, every source it could still address is a candidate. The
   * candidates' common value is the output. Disagreement is an error if a select input or a
   * candidate is an error, and unknown otherwise.
   */
  private static Value selected(Value s1, Value s0, Value[] sources) {
    if (s1 == Value.FALSE && s0 == Value.FALSE) {
      return sources[0];
    }
    if (s1 == Value.FALSE && s0 == Value.TRUE) {
      return sources[1];
    }
    if (s1 == Value.TRUE && s0 == Value.FALSE) {
      return sources[2];
    }
    if (s1 == Value.TRUE && s0 == Value.TRUE) {
      return sources[3];
    }

    final var possible = new boolean[] {
      s1 != Value.TRUE && s0 != Value.TRUE,
      s1 != Value.TRUE && s0 != Value.FALSE,
      s1 != Value.FALSE && s0 != Value.TRUE,
      s1 != Value.FALSE && s0 != Value.FALSE
    };
    Value agreed = null;
    var differ = false;
    var sawError = s1 == Value.ERROR || s0 == Value.ERROR;
    for (var index = 0; index < sources.length; index++) {
      if (!possible[index]) {
        continue;
      }
      final var source = sources[index];
      if (source == Value.ERROR) {
        sawError = true;
      }
      if (agreed == null) {
        agreed = source;
      } else if (source != agreed) {
        differ = true;
      }
    }
    if (agreed == null || !differ) {
      return agreed == null ? Value.UNKNOWN : agreed;
    }
    return sawError ? Value.ERROR : Value.UNKNOWN;
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
