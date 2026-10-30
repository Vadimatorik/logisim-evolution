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
 * TTL 74HC645: octal non-inverting bus transceiver with three-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74hc645.pdf">TI SN74HC645</a> and
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT645.pdf">Nexperia 74HC645</a>
 * data sheets. A low {@code nOE} and a high {@code DIR} drive each B pin with the paired A pin. A
 * low {@code nOE} and a low {@code DIR} drive each A pin with the paired B pin. A high {@code nOE},
 * or a control that is neither high nor low, releases both buses. High impedance is reported as
 * unknown. The part is the non-inverting counterpart of the 74HC640 and matches the function of
 * the 74HC245. Pin names follow the TI numbering, where A1 is Nexperia's A0.
 */
public class Ttl74645 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74645";

  public static final int PORT_INDEX_DIR = 0;
  public static final int PORT_INDEX_A1 = 1;
  public static final int PORT_INDEX_A2 = 2;
  public static final int PORT_INDEX_A3 = 3;
  public static final int PORT_INDEX_A4 = 4;
  public static final int PORT_INDEX_A5 = 5;
  public static final int PORT_INDEX_A6 = 6;
  public static final int PORT_INDEX_A7 = 7;
  public static final int PORT_INDEX_A8 = 8;
  public static final int PORT_INDEX_B8 = 9;
  public static final int PORT_INDEX_B7 = 10;
  public static final int PORT_INDEX_B6 = 11;
  public static final int PORT_INDEX_B5 = 12;
  public static final int PORT_INDEX_B4 = 13;
  public static final int PORT_INDEX_B3 = 14;
  public static final int PORT_INDEX_B2 = 15;
  public static final int PORT_INDEX_B1 = 16;
  public static final int PORT_INDEX_nOE = 17;
  /** Port index of GND when the explicit power pins are shown. */
  public static final int PORT_INDEX_GND = 18;
  /** Port index of VCC when the explicit power pins are shown. */
  public static final int PORT_INDEX_VCC = 19;

  private static final int DELAY = 1;
  private static final int BITS = 8;
  /** A-bus port of each bit. Bit 0 is A1, which shares a channel with B1. */
  private static final int[] BUS_A = {
    PORT_INDEX_A1,
    PORT_INDEX_A2,
    PORT_INDEX_A3,
    PORT_INDEX_A4,
    PORT_INDEX_A5,
    PORT_INDEX_A6,
    PORT_INDEX_A7,
    PORT_INDEX_A8
  };
  /** B-bus port of each bit. Bit 0 is B1, on physical pin 18. */
  private static final int[] BUS_B = {
    PORT_INDEX_B1,
    PORT_INDEX_B2,
    PORT_INDEX_B3,
    PORT_INDEX_B4,
    PORT_INDEX_B5,
    PORT_INDEX_B6,
    PORT_INDEX_B7,
    PORT_INDEX_B8
  };
  private static final byte[] INOUT_PINS = {
    2, 3, 4, 5, 6, 7, 8, 9, 11, 12, 13, 14, 15, 16, 17, 18
  };
  private static final String[] PORT_NAMES = {
    "DIR (direction, high sends A to B)",
    "A1",
    "A2",
    "A3",
    "A4",
    "A5",
    "A6",
    "A7",
    "A8",
    "B8",
    "B7",
    "B6",
    "B5",
    "B4",
    "B3",
    "B2",
    "B1",
    "nOE (output enable, active low)"
  };

  /** Creates a 74645 octal non-inverting bus transceiver. */
  public Ttl74645() {
    super(_ID, (byte) 20, new byte[] {}, new byte[] {}, INOUT_PINS, PORT_NAMES,
        new Ttl74645HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 8));
    g.drawRect(x + 185, y + 12, 10, 10);
    g.drawRect(x + 165, y + 12, 10, 10);
    g.drawRect(x + 145, y + 12, 10, 10);
    g.drawRect(x + 125, y + 12, 10, 10);
    g.drawRect(x + 105, y + 12, 10, 10);
    g.drawRect(x + 85, y + 12, 10, 10);
    g.drawRect(x + 65, y + 12, 10, 10);
    g.drawRect(x + 45, y + 12, 10, 10);
    g.drawRect(x + 25, y + 12, 10, 10);
    g.drawLine(x + 190, y + 12, x + 190, y + 7);
    g.drawLine(x + 170, y + 12, x + 170, y + 7);
    g.drawLine(x + 150, y + 12, x + 150, y + 7);
    g.drawLine(x + 130, y + 12, x + 130, y + 7);
    g.drawLine(x + 110, y + 12, x + 110, y + 7);
    g.drawLine(x + 90, y + 12, x + 90, y + 7);
    g.drawLine(x + 70, y + 12, x + 70, y + 7);
    g.drawLine(x + 50, y + 12, x + 50, y + 7);
    g.drawLine(x + 30, y + 12, x + 30, y + 7);
    g.drawLine(x + 10, y + height - 7, x + 10, y + 25);
    g.drawLine(x + 10, y + 25, x + 187, y + 25);
    g.drawLine(x + 187, y + 25, x + 187, y + 22);
    g.drawLine(x + 30, y + height - 7, x + 30, y + 25);
    g.drawLine(x + 30, y + 25, x + 47, y + 25);
    g.drawLine(x + 47, y + 25, x + 47, y + 22);
    g.drawLine(x + 50, y + height - 7, x + 50, y + 25);
    g.drawLine(x + 50, y + 25, x + 67, y + 25);
    g.drawLine(x + 67, y + 25, x + 67, y + 22);
    g.drawLine(x + 70, y + height - 7, x + 70, y + 25);
    g.drawLine(x + 70, y + 25, x + 87, y + 25);
    g.drawLine(x + 87, y + 25, x + 87, y + 22);
    g.drawLine(x + 90, y + height - 7, x + 90, y + 25);
    g.drawLine(x + 90, y + 25, x + 107, y + 25);
    g.drawLine(x + 107, y + 25, x + 107, y + 22);
    g.drawLine(x + 110, y + height - 7, x + 110, y + 25);
    g.drawLine(x + 110, y + 25, x + 127, y + 25);
    g.drawLine(x + 127, y + 25, x + 127, y + 22);
    g.drawLine(x + 130, y + height - 7, x + 130, y + 25);
    g.drawLine(x + 130, y + 25, x + 147, y + 25);
    g.drawLine(x + 147, y + 25, x + 147, y + 22);
    g.drawLine(x + 150, y + height - 7, x + 150, y + 25);
    g.drawLine(x + 150, y + 25, x + 167, y + 25);
    g.drawLine(x + 167, y + 25, x + 167, y + 22);
    g.drawLine(x + 170, y + height - 7, x + 170, y + 25);
    g.drawLine(x + 170, y + 25, x + 187, y + 25);
    g.drawLine(x + 187, y + 25, x + 187, y + 22);
    g.drawOval(x + 28, y + 8, 4, 4);
    GraphicsUtil.drawCenteredText(g, "8", x + 190, y + 17);
    GraphicsUtil.drawCenteredText(g, "7", x + 170, y + 17);
    GraphicsUtil.drawCenteredText(g, "6", x + 150, y + 17);
    GraphicsUtil.drawCenteredText(g, "5", x + 130, y + 17);
    GraphicsUtil.drawCenteredText(g, "4", x + 110, y + 17);
    GraphicsUtil.drawCenteredText(g, "3", x + 90, y + 17);
    GraphicsUtil.drawCenteredText(g, "2", x + 70, y + 17);
    GraphicsUtil.drawCenteredText(g, "1", x + 50, y + 17);
    GraphicsUtil.drawCenteredText(g, "E", x + 30, y + 17);
    g.setFont(previousFont);
  }



  /**
   * Releases both buses when the exposed supply pins are not a valid ground and positive rail.
   * The base implementation only clears output ports, and these data pins are bidirectional.
   */
  @Override
  public void propagate(InstanceState state) {
    if (exposedPowerIsInvalid(state)) {
      releaseBothBuses(state);
      return;
    }
    propagateTtl(state);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var fromA = new Value[BITS];
    final var fromB = new Value[BITS];
    for (var bit = 0; bit < BITS; bit++) {
      fromA[bit] = state.getPortValue(BUS_A[bit]);
      fromB[bit] = state.getPortValue(BUS_B[bit]);
    }
    final var enabled = state.getPortValue(PORT_INDEX_nOE) == Value.FALSE;
    final var direction = state.getPortValue(PORT_INDEX_DIR);
    if (enabled && direction == Value.TRUE) {
      drive(state, BUS_B, fromA);
      release(state, BUS_A);
    } else if (enabled && direction == Value.FALSE) {
      drive(state, BUS_A, fromB);
      release(state, BUS_B);
    } else {
      releaseBothBuses(state);
    }
  }

  private static boolean exposedPowerIsInvalid(InstanceState state) {
    if (!state.getAttributeValue(TtlLibrary.VCC_GND)) return false;
    return state.getPortValue(PORT_INDEX_GND) != Value.FALSE
        || state.getPortValue(PORT_INDEX_VCC) != Value.TRUE;
  }

  /** Drives {@code ports} from {@code sources}. A source that is not 0 or 1 becomes an error. */
  private static void drive(InstanceState state, int[] ports, Value[] sources) {
    for (var bit = 0; bit < BITS; bit++) {
      final var source = sources[bit];
      state.setPort(ports[bit], source.isFullyDefined() ? source : Value.ERROR, DELAY);
    }
  }

  private static void releaseBothBuses(InstanceState state) {
    release(state, BUS_A);
    release(state, BUS_B);
  }

  private static void release(InstanceState state, int[] ports) {
    for (final var port : ports) {
      state.setPort(port, Value.UNKNOWN, DELAY);
    }
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
