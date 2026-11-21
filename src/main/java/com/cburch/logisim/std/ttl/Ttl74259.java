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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74HC259: 8-bit addressable latch.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT259.pdf">Nexperia 74HC259</a>
 * and <a href="https://www.ti.com/lit/ds/symlink/sn74hc259.pdf">TI SN74HC259</a> function tables.
 * Nexperia names the controls {@code LE} and {@code MR}; TI names the same pins {@code G} and
 * {@code CLR}. Both are active low. Nanosecond delays are not modeled.
 *
 * <p>{@code MR} low and {@code LE} high clears every latch. Both low make the addressed output
 * follow {@code D} and clear every other latch. {@code MR} high and {@code LE} low make only the
 * addressed latch follow {@code D}. Both high hold the latches. A control that is not exactly high
 * or low does not write. An address bit that is not exactly high or low makes each latch that
 * might still be selected combine the selected and unselected results: a 0/1 conflict is an error,
 * and any other disagreement is unknown.
 */
public class Ttl74259 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74259";

  public static final int DELAY = 1;

  public static final byte A0 = 1;
  public static final byte A1 = 2;
  public static final byte A2 = 3;
  public static final byte Q0 = 4;
  public static final byte Q1 = 5;
  public static final byte Q2 = 6;
  public static final byte Q3 = 7;
  public static final byte GND = 8;
  public static final byte Q4 = 9;
  public static final byte Q5 = 10;
  public static final byte Q6 = 11;
  public static final byte Q7 = 12;
  public static final byte D = 13;
  /** Latch enable, active low. TI calls this pin G. */
  public static final byte LE = 14;
  /** Conditional reset, active low. TI calls this pin CLR. */
  public static final byte MR = 15;
  public static final byte VCC = 16;

  private static final int BITS = 8;
  private static final byte[] ADDRESS = {A0, A1, A2};
  private static final byte[] OUTPUTS = {Q0, Q1, Q2, Q3, Q4, Q5, Q6, Q7};
  private static final String[] PORT_NAMES = {
    "A0 address",
    "A1 address",
    "A2 address",
    "Q0",
    "Q1",
    "Q2",
    "Q3",
    "Q4",
    "Q5",
    "Q6",
    "Q7",
    "D data",
    "LE / G (latch enable, active low)",
    "MR / CLR (conditional reset, active low)"
  };

  /** Creates a 74259 8-bit addressable latch. */
  public Ttl74259() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74259HdlGenerator());
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
    g.setFont(new Font(Font.DIALOG, Font.BOLD, 5));
    drawSchematic(g, x, y, height);
    g.setFont(previousFont);
  }

  /**
   * Address S0–S2 enters the bottom of the decoder. The select bus leaves its top. Pin 14, D and
   * active-low CLR enter the top of the latch; Q leaves the bottom, opposite those inputs, and one
   * bus reaches both pin rows. Address and data inverters stay inside the blocks.
   */
  private static void drawSchematic(Graphics g, int x, int y, int height) {
    final var pinTop = y + PIN_HEIGHT;
    final var pinBottom = y + height - PIN_HEIGHT;
    g.drawRect(x + 10, y + 32, 24, 12);
    g.drawRect(x + 38, y + 32, 40, 12);
    label(g, "DEC", x + 22, y + 38);
    label(g, "LATCH", x + 58, y + 38);

    // Address bus into the decoder bottom. The stub stops one pixel outside the rail.
    bus(g, x + 9.5f, y + 48, x + 50.5f, y + 48);
    wire(g, x + 10, pinBottom, x + 10, y + 48);
    wire(g, x + 30, pinBottom, x + 30, y + 48);
    wire(g, x + 50, pinBottom, x + 50, y + 48);
    bus(g, x + 22, y + 44, x + 22, y + 47);

    // Pin 14 runs along the top into the latch. Active low, so the bubble is on that block.
    wire(g, x + 50, pinTop, x + 50, y + 12, x + 62, y + 12, x + 62, y + 30);
    bubble(g, x + 62, y + 31);

    // Select bus leaves the decoder top and enters the latch top.
    bus(g, x + 22, y + 32, x + 22, y + 28, x + 46, y + 28, x + 46, y + 32);

    // Pin 13 drops straight into the latch on its own column.
    wire(g, x + 70, pinTop, x + 70, y + 32);

    // CLR is active low and drops left of pin 14, so the two wires do not cross.
    wire(g, x + 30, pinTop, x + 30, y + 24, x + 54, y + 24, x + 54, y + 30);
    bubble(g, x + 54, y + 31);

    // Q leaves the latch bottom opposite the inputs and feeds both pin rows.
    bus(g, x + 58, y + 44, x + 58, y + 47);
    bus(g, x + 57, y + 48, x + 137, y + 48);
    bus(g, x + 136, y + 13, x + 136, y + 47);
    bus(g, x + 89.5f, y + 12, x + 150.5f, y + 12);
    for (var column : new int[] {90, 110, 130, 150}) {
      wire(g, x + column, pinTop, x + column, y + 12);
    }
    for (var column : new int[] {70, 90, 110, 130}) {
      wire(g, x + column, pinBottom, x + column, y + 48);
    }
  }

  private static void label(Graphics g, String text, int cx, int cy) {
    GraphicsUtil.drawText(g, text, cx, cy, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER_OVERALL);
  }

  private static void bubble(Graphics g, int cx, int cy) {
    g.drawOval(cx - 1, cy - 1, 2, 2);
  }

  private static void wire(Graphics g, int... points) {
    for (var i = 2; i < points.length; i += 2) {
      g.drawLine(points[i - 2], points[i - 1], points[i], points[i + 1]);
    }
  }

  /** Thick bus. A corner is one polyline so the butt caps do not leave a gap. */
  private static void bus(Graphics g, float... points) {
    if ((points.length < 4) || (points.length % 2 != 0)) return;
    if (!(g instanceof Graphics2D g2)) {
      for (var i = 2; i < points.length; i += 2) {
        g.drawLine(
            Math.round(points[i - 2]),
            Math.round(points[i - 1]),
            Math.round(points[i]),
            Math.round(points[i + 1]));
      }
      return;
    }
    final var previous = g2.getStroke();
    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
    final var path = new Path2D.Float();
    path.moveTo(points[0], points[1]);
    for (var i = 2; i < points.length; i += 2) {
      path.lineTo(points[i], points[i + 1]);
    }
    g2.draw(path);
    g2.setStroke(previous);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = stateData(state);
    final var masterReset = input(state, MR);
    final var latchEnable = input(state, LE);
    if (masterReset == Value.FALSE && latchEnable == Value.TRUE) {
      clearAll(data);
    } else if (masterReset == Value.FALSE && latchEnable == Value.FALSE) {
      writeTransparent(state, data, true);
    } else if (masterReset == Value.TRUE && latchEnable == Value.FALSE) {
      writeTransparent(state, data, false);
    }
    publish(state, data);
  }

  private static TtlRegisterData stateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.ONE, BITS);
      state.setData(data);
    }
    return data;
  }

  private static void clearAll(TtlRegisterData data) {
    for (var index = 0; index < BITS; index++) {
      data.setValue(index, Value.FALSE);
    }
  }

  /**
   * Writes the transparent mode. In demultiplexer mode every unselected latch is cleared. In
   * addressable-latch mode an unselected latch keeps the value it already stores.
   */
  private static void writeTransparent(InstanceState state, TtlRegisterData data, boolean clearUnselected) {
    final var dataIn = input(state, D);
    for (var index = 0; index < BITS; index++) {
      final var idle = clearUnselected ? Value.FALSE : data.getValue(index);
      data.setValue(index, levelFor(state, index, dataIn, idle));
    }
  }

  private static Value levelFor(InstanceState state, int index, Value dataIn, Value idle) {
    if (addressExcludes(state, index)) return idle;
    if (addressSelects(state, index)) return dataIn;
    return combine(dataIn, idle);
  }

  private static boolean addressExcludes(InstanceState state, int index) {
    for (var place = 0; place < ADDRESS.length; place++) {
      final var bit = input(state, ADDRESS[place]);
      final var expectedHigh = ((index >> place) & 1) == 1;
      if (expectedHigh && bit == Value.FALSE) return true;
      if (!expectedHigh && bit == Value.TRUE) return true;
    }
    return false;
  }

  private static boolean addressSelects(InstanceState state, int index) {
    if (addressExcludes(state, index)) return false;
    for (final var pin : ADDRESS) {
      final var bit = input(state, pin);
      if (bit != Value.TRUE && bit != Value.FALSE) return false;
    }
    return true;
  }

  private static Value combine(Value whenSelected, Value whenIdle) {
    if (whenSelected == whenIdle) return whenSelected;
    if (whenSelected == Value.ERROR || whenIdle == Value.ERROR) return Value.ERROR;
    if (whenSelected.isFullyDefined() && whenIdle.isFullyDefined()) return Value.ERROR;
    return Value.UNKNOWN;
  }

  private static void publish(InstanceState state, TtlRegisterData data) {
    for (var index = 0; index < BITS; index++) {
      state.setPort(pinNrToPortNr(OUTPUTS[index]), data.getValue(index), DELAY);
    }
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }

}
