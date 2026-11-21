/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.Arrays;

/**
 * TTL 74x4094: 8-bit shift-and-store register with three-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/cd74hc4094.pdf">CD74HC4094</a> and Nexperia 74HC4094
 * data sheets. A rising {@code CP} shifts {@code D} into {@code QP0} and toward {@code QP7}.
 * {@code QS1} is that last shift-register stage. A falling {@code CP} copies {@code QS1} into
 * {@code QS2}, so the cascading output stays stable while the next package samples {@code CP}.
 * {@code STR} is a level: while it is high the storage register follows the shift register, and a
 * low {@code STR} holds storage. {@code OE} high drives storage onto {@code QP0}..{@code QP7};
 * any other level releases those pins. {@code QS1} and {@code QS2} stay driven. TI names the same
 * pins {@code STROBE}, {@code DATA} and {@code Q0}..{@code Q7}. On the package, {@code QP4}
 * to {@code QP7} are pins 14, 13, 12 and 11. Nanosecond delays are not modeled.
 */
public class Ttl744094 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744094";

  public static final int DELAY = 1;
  private static final int STAGES = 8;

  public static final byte STR = 1;
  public static final byte D = 2;
  public static final byte CP = 3;
  public static final byte QP0 = 4;
  public static final byte QP1 = 5;
  public static final byte QP2 = 6;
  public static final byte QP3 = 7;
  public static final byte GND = 8;
  public static final byte QS1 = 9;
  public static final byte QS2 = 10;
  public static final byte QP7 = 11;
  public static final byte QP6 = 12;
  public static final byte QP5 = 13;
  public static final byte QP4 = 14;
  public static final byte OE = 15;
  public static final byte VCC = 16;

  private static final byte[] PARALLEL = {QP0, QP1, QP2, QP3, QP4, QP5, QP6, QP7};
  private static final byte[] OUTPUT_PINS = {QP0, QP1, QP2, QP3, QS1, QS2, QP4, QP5, QP6, QP7};
  private static final String[] PORT_NAMES = {
    "STR (Strobe)",
    "D (Data)",
    "CP (Clock)",
    "QP0",
    "QP1",
    "QP2",
    "QP3",
    "QS1",
    "QS2",
    "QP7",
    "QP6",
    "QP5",
    "QP4",
    "OE (Output enable, active high)"
  };
  private static final String[] PIN_NAMES = {
    "STR", "D", "CP", "QP0", "QP1", "QP2", "QP3", null,
    "QS1", "QS2", "QP7", "QP6", "QP5", "QP4", "OE", null
  };

  /** Creates a 744094 shift-and-store register with three-state parallel outputs. */
  public Ttl744094() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl744094HdlGenerator());
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

  private static State getData(InstanceState state) {
    var data = (State) state.getData();
    if (data == null) {
      data = new State();
      state.setData(data);
    }
    return data;
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    drawStructure(g, x, y, height);
  }

  /**
   * Logic diagram of the 74HC4094. Rising CP clocks D through the shift register; QS1 leaves its
   * last stage. STR high lets storage follow that register. OE high, with no bubble, enables one
   * buffer on each stored bit. A second flip-flop copies QS1 into QS2 on the falling edge.
   */
  private static void drawStructure(Graphics g, int x, int y, int height) {
    final var top = y + PIN_HEIGHT;
    final var bottom = y + height - PIN_HEIGHT;
    final var previous = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 5));
    g.drawRect(x + 22, y + 35, 36, 10);
    g.drawRect(x + 9, y + 23, 30, 10);
    g.drawRect(x + 122, y + 17, 24, 10);
    // Dialog 5 is 8 px tall. V_CENTER_OVERALL puts that box on the block center.
    GraphicsUtil.drawText(
        g, "SHIFT", x + 40, y + 40, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER_OVERALL);
    GraphicsUtil.drawText(
        g, "STORE", x + 24, y + 28, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER_OVERALL);
    GraphicsUtil.drawText(
        g, "QS2", x + 134, y + 22, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER_OVERALL);
    g.setFont(previous);

    for (var px : new int[] {50, 70, 90, 110}) {
      drawBuffer(g, x + px, y + 11, x + px - 4, y + 15, x + px + 4, y + 15);
    }
    for (var px : new int[] {70, 90, 110, 130}) {
      drawBuffer(g, x + px - 4, y + 41, x + px + 4, y + 41, x + px, y + 45);
    }
    drawClockUp(g, x + 50, y + 45, 1);
    drawClockUp(g, x + 142, y + 27, 2);
    g.drawOval(x + 141, y + 27, 2, 2);

    wire(g, x + 10, bottom, x + 10, y + 33);
    wire(g, x + 30, bottom, x + 30, y + 45);
    wire(g, x + 50, bottom, x + 50, y + 45);
    wire(g, x + 50, y + 48, x + 142, y + 48, x + 142, y + 29);
    drawNode(g, x + 50, y + 48);
    wire(g, x + 44, y + 35, x + 44, y + 31, x + 150, y + 31, x + 150, top);
    wire(g, x + 126, y + 31, x + 126, y + 27);
    drawNode(g, x + 126, y + 31);
    wire(g, x + 130, y + 17, x + 130, top);
    wire(g, x + 30, top, x + 30, y + 13, x + 48, y + 13);
    wire(g, x + 42, y + 13, x + 42, y + 27, x + 118, y + 27, x + 118, y + 43);
    drawNode(g, x + 42, y + 13);
    for (var px : new int[] {70, 90}) {
      final var drop = px - 8;
      wire(g, x + drop, y + 13, x + drop, y + 43);
      wire(g, x + drop, y + 13, x + px - 2, y + 13);
      wire(g, x + drop, y + 43, x + px - 2, y + 43);
      drawNode(g, x + drop, y + 27);
    }
    wire(g, x + 102, y + 13, x + 102, y + 27);
    wire(g, x + 102, y + 13, x + 108, y + 13);
    drawNode(g, x + 102, y + 27);
    wire(g, x + 112, y + 43, x + 128, y + 43);
    drawNode(g, x + 118, y + 43);
    for (var px : new int[] {50, 70, 90, 110}) {
      wire(g, x + px, y + 11, x + px, top);
    }
    for (var px : new int[] {70, 90, 110, 130}) {
      wire(g, x + px, y + 45, x + px, bottom);
    }
    for (var px : new int[] {50, 70, 90, 110}) {
      wire(g, x + px, y + 19, x + px, y + 15);
    }
    for (var px : new int[] {70, 90, 110, 130}) {
      wire(g, x + px, y + 37, x + px, y + 41);
    }

    bus(g, x + 36, y + 33, x + 36, y + 35);
    bus(g, x + 23, y + 23, x + 23, y + 19, x + 110, y + 19, x + 110, y + 36);
    // Butt cap meets the outer face of the width-1 taps at pins 4 and 7.
    bus(g, x + 69.5, y + 37, x + 130.5, y + 37);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getData(state);
    final var clock = state.getPortValue(pinNrToPortNr(CP));
    final var rising = data.isRising(clock);
    final var falling = data.isFalling(clock);
    data.remember(clock);

    if (rising) {
      data.shiftIn(state.getPortValue(pinNrToPortNr(D)));
    }
    if (falling) {
      data.captureSerial();
    }
    if (state.getPortValue(pinNrToPortNr(STR)) == Value.TRUE) {
      data.followShift();
    }

    final var enabled = state.getPortValue(pinNrToPortNr(OE)) == Value.TRUE;
    for (var stage = 0; stage < STAGES; stage++) {
      final var output = enabled ? data.storage[stage] : Value.UNKNOWN;
      state.setPort(pinNrToPortNr(PARALLEL[stage]), output, DELAY);
    }
    state.setPort(pinNrToPortNr(QS1), data.shift[STAGES - 1], DELAY);
    state.setPort(pinNrToPortNr(QS2), data.qs2, DELAY);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {pinNrToPortNr(CP)};
  }

  /**
   * Shift register, storage register, delayed serial bit and the previous clock level.
   *
   * <p>Index 0 is {@code QP0}. Startup follows {@link AppPreferences#Memory_Startup_Unknown}.
   */
  private static final class State implements InstanceData {
    private Value[] shift = new Value[STAGES];
    private Value[] storage = new Value[STAGES];
    private Value qs2;
    private Value lastCp = Value.FALSE;

    private State() {
      final var initial =
          AppPreferences.Memory_Startup_Unknown.get() ? Value.UNKNOWN : Value.FALSE;
      Arrays.fill(shift, initial);
      Arrays.fill(storage, initial);
      qs2 = initial;
    }

    private boolean isRising(Value clock) {
      return lastCp == Value.FALSE && clock == Value.TRUE;
    }

    private boolean isFalling(Value clock) {
      return lastCp == Value.TRUE && clock == Value.FALSE;
    }

    private void remember(Value clock) {
      if (clock != null && clock != Value.NIL) {
        lastCp = clock;
      }
    }

    private void shiftIn(Value serial) {
      for (var stage = STAGES - 1; stage > 0; stage--) {
        shift[stage] = shift[stage - 1];
      }
      shift[0] = serial;
    }

    private void captureSerial() {
      qs2 = shift[STAGES - 1];
    }

    private void followShift() {
      storage = shift.clone();
    }

    @Override
    public State clone() {
      final var copy = new State();
      copy.shift = shift.clone();
      copy.storage = storage.clone();
      copy.qs2 = qs2;
      copy.lastCp = lastCp;
      return copy;
    }
  }

  /** Orthogonal wire through the given points. */
  private static void wire(Graphics g, int... points) {
    for (var i = 2; i < points.length; i += 2) {
      g.drawLine(points[i - 2], points[i - 1], points[i], points[i + 1]);
    }
  }

  /** Closed buffer triangle. Bevel join keeps the apex from mitering past the tip. */
  private static void drawBuffer(Graphics g, int x0, int y0, int x1, int y1, int x2, int y2) {
    final var g2 = (Graphics2D) g;
    final var previous = g2.getStroke();
    g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
    g.drawPolygon(new int[] {x0, x1, x2}, new int[] {y0, y1, y2}, 3);
    g2.setStroke(previous);
  }

  /** Clock mark on the bottom edge. The point is inside the block. */
  private static void drawClockUp(Graphics g, int centerX, int edgeY, int halfBase) {
    g.drawLine(centerX - halfBase, edgeY, centerX, edgeY - 3);
    g.drawLine(centerX + halfBase, edgeY, centerX, edgeY - 3);
  }

  /** Several signals of one kind. One path, so a corner does not leave a gap. */
  private static void bus(Graphics g, double... points) {
    final var path = new Path2D.Double();
    path.moveTo(points[0], points[1]);
    for (var i = 2; i < points.length; i += 2) {
      path.lineTo(points[i], points[i + 1]);
    }
    final var g2 = (Graphics2D) g;
    final var previous = g2.getStroke();
    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
    g2.draw(path);
    g2.setStroke(previous);
  }

  /** Junction of three branches. The oval center is the common point. */
  private static void drawNode(Graphics g, int centerX, int centerY) {
    g.fillOval(centerX - 1, centerY - 1, 2, 2);
  }

}
