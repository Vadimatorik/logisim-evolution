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
import java.awt.Graphics2D;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x257: quad 2-line to 1-line data selector with 3-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT257.pdf">Nexperia 74HC257</a>
 * and <a href="https://www.ti.com/lit/ds/symlink/sn74hc257.pdf">TI SN74HC257</a> function tables.
 * A low output enable passes the selected source without inversion. A high output enable releases
 * every output. The common select input chooses source 1 when high and source 0 when low. Pin
 * names follow Nexperia; TI's SN74HC257 calls the same pins A/B, A, B, Y and G. Nanosecond delays
 * are not modeled.
 *
 * <p>An unknown or error input changes an output only when the accepted substitutions disagree. An
 * error on such an input makes the disagreed output an error; an unknown input makes it unknown. A
 * high output enable releases the outputs even if a data input is an error. A released output is
 * {@link Value#UNKNOWN}, which is how this library represents high impedance.
 */
public class Ttl74257 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74257";

  public static final int DELAY = 1;

  public static final byte SELECT = 1;
  public static final byte L1_I0 = 2;
  public static final byte L1_I1 = 3;
  public static final byte L1_Y = 4;
  public static final byte L2_I0 = 5;
  public static final byte L2_I1 = 6;
  public static final byte L2_Y = 7;
  public static final byte GND = 8;
  public static final byte L3_Y = 9;
  public static final byte L3_I1 = 10;
  public static final byte L3_I0 = 11;
  public static final byte L4_Y = 12;
  public static final byte L4_I1 = 13;
  public static final byte L4_I0 = 14;
  public static final byte OE = 15;
  public static final byte VCC = 16;

  /** Each row is source 0, source 1, and the output of one multiplexer. */
  private static final byte[][] CHANNELS = {
    {L1_I0, L1_I1, L1_Y},
    {L2_I0, L2_I1, L2_Y},
    {L3_I0, L3_I1, L3_Y},
    {L4_I0, L4_I1, L4_Y}
  };

  private static final byte[] OUTPUTS = {L1_Y, L2_Y, L3_Y, L4_Y};

  private static final String[] PORT_NAMES = {
    "S (Select)",
    "1I0",
    "1I1",
    "1Y",
    "2I0",
    "2I1",
    "2Y",
    "3Y",
    "3I1",
    "3I0",
    "4Y",
    "4I1",
    "4I0",
    "nOE (Output enable, active low)"
  };

  /** Square section between the I1 column and the Y column. */
  private static final int BLOCK_WIDTH = 14;

  /** Tall enough for a two-line label and a four-pixel gap beside the inputs. */
  private static final int BLOCK_HEIGHT = 16;

  /** Four pixels below the top pin line, clear of the Vcc label. */
  private static final int UPPER_TOP = PIN_HEIGHT + 4;

  /** Lower row. Its bottom stays six pixels above the bottom pin line. */
  private static final int LOWER_TOP = 31;

  /** Equal inset of the four inputs from both corners of the face. */
  private static final int INPUT_INSET = 2;

  private static final int INPUT_STEP = 4;

  /** Under the lower row: four pixels inside the package edge and two below the blocks. */
  private static final int SELECT_Y = 49;

  /** Between the rows, two pixels clear of both. */
  private static final int ENABLE_Y = 29;

  /** Riser that continues to the upper-right section. Section 2 taps this riser. */
  private static final int FAR_SELECT_X = 96;

  /** Enable riser beside the select riser, six pixels short of column 110. */
  private static final int FAR_ENABLE_X = 104;

  private static final Section[] SECTIONS = {
    new Section(L1_I0, L1_I1, L1_Y, false, 1),
    new Section(L2_I0, L2_I1, L2_Y, false, 2),
    new Section(L4_I0, L4_I1, L4_Y, true, 4),
    new Section(L3_I0, L3_I1, L3_Y, true, 3)
  };

  /** Creates a 74257 quad 2-line to 1-line data selector with 3-state outputs. */
  public Ttl74257() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74257HdlGenerator());
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
    drawStructure(g, x, y, height);
    g.setFont(previousFont);
  }

  /**
   * Four 2-to-1 sections. A low select passes I0 and a high select passes I1, without inversion. A
   * low output enable drives every output; a high enable releases them. Every input enters the left
   * face and Y leaves the right face. Sections 1 and 2 sit on the lower pins, clear of the package
   * edge; sections 4 and 3 sit on the upper pins.
   */
  private static void drawStructure(Graphics g, int x, int y, int height) {
    final var pinTop = y + PIN_HEIGHT;
    final var pinBottom = y + height - PIN_HEIGHT;
    for (final var section : SECTIONS) {
      drawSection(g, x, y, pinTop, pinBottom, section);
    }
    drawSelect(g, x, y, pinBottom);
    drawEnable(g, x, y, pinTop);
  }

  /** I0, I1, select, and enable on the left. Y on the right, off every input axis. */
  private static void drawSection(
      Graphics g, int x, int y, int pinTop, int pinBottom, Section section) {
    final var top = y + (section.upper ? UPPER_TOP : LOWER_TOP);
    final var left = pinX(x, section.source1) + 4;
    final var right = left + BLOCK_WIDTH;
    final var source1Y = top + inputOffset(section.upper, 0);
    final var source0Y = top + inputOffset(section.upper, 1);
    final var pinY = section.upper ? pinTop : pinBottom;

    g.drawRect(left, top, BLOCK_WIDTH, BLOCK_HEIGHT);
    drawLabel(g, "MUX\n" + section.number(), left + BLOCK_WIDTH / 2, top + BLOCK_HEIGHT / 2);
    // The nearer source stops first, so the farther horizontal clears it by four pixels.
    wire(g, pinX(x, section.source1), pinY, pinX(x, section.source1), source1Y, left, source1Y);
    wire(g, pinX(x, section.source0), pinY, pinX(x, section.source0), source0Y, left, source0Y);
    final var outY = top + BLOCK_HEIGHT / 2;
    wire(g, right, outY, pinX(x, section.output), outY, pinX(x, section.output), pinY);
  }

  /**
   * Select rises on its own pin and taps sections 1 and 4 directly. The riser that continues to the
   * upper-right section also feeds section 2, so that input has no extra corner.
   */
  private static void drawSelect(Graphics g, int x, int y, int pinBottom) {
    final var selectX = pinX(x, SELECT);
    final var upperY = y + UPPER_TOP + inputOffset(true, 2);
    final var lowerY = y + LOWER_TOP + inputOffset(false, 2);
    final var railY = y + SELECT_Y;
    final var farX = x + FAR_SELECT_X;

    wire(g, selectX, pinBottom, selectX, upperY);
    wire(g, selectX, lowerY, pinX(x, L1_I1) + 4, lowerY);
    wire(g, selectX, upperY, pinX(x, L4_I1) + 4, upperY);
    wire(g, selectX, railY, farX, railY);
    wire(g, farX, railY, farX, upperY, pinX(x, L3_I1) + 4, upperY);
    wire(g, farX, lowerY, pinX(x, L2_I1) + 4, lowerY);
    drawNode(g, selectX, railY);
    drawNode(g, selectX, lowerY);
    drawNode(g, farX, lowerY);
  }

  /**
   * Enable meets the rail between the rows. Sections 1 and 4 take a short rise from that rail, the
   * same shape, instead of a second line from the pin. Sections 2 and 3 use the riser at the rail's
   * end. The bubble is only on this input.
   */
  private static void drawEnable(Graphics g, int x, int y, int pinTop) {
    final var enableX = pinX(x, OE);
    final var upperY = y + UPPER_TOP + inputOffset(true, 3);
    final var lowerY = y + LOWER_TOP + inputOffset(false, 3);
    final var railY = y + ENABLE_Y;
    final var farX = x + FAR_ENABLE_X;
    final var section1Tap = pinX(x, L1_I1);
    final var section4Tap = pinX(x, L4_I1);

    wire(g, enableX, pinTop, enableX, railY, farX, railY);
    wire(g, section1Tap, railY, section1Tap, lowerY, section1Tap + 2, lowerY);
    wire(g, section4Tap, railY, section4Tap, upperY, section4Tap + 2, upperY);
    wire(g, farX, upperY, farX, lowerY);
    wire(g, farX, upperY, pinX(x, L3_I1) + 2, upperY);
    wire(g, farX, lowerY, pinX(x, L2_I1) + 2, lowerY);
    drawBubble(g, section4Tap + 3, upperY);
    drawBubble(g, section1Tap + 3, lowerY);
    drawBubble(g, pinX(x, L3_I1) + 3, upperY);
    drawBubble(g, pinX(x, L2_I1) + 3, lowerY);
    drawNode(g, section1Tap, railY);
    drawNode(g, section4Tap, railY);
    drawNode(g, farX, railY);
  }

  /**
   * Centers each line on its real string bounds. Integer width truncates a fractional advance and
   * shifts the bold label to the right.
   */
  private static void drawLabel(Graphics g, String text, int centerX, int centerY) {
    final var g2 = (Graphics2D) g;
    final var font = g2.getFont();
    final var context = g2.getFontRenderContext();
    final var lines = text.split("\n", -1);
    final var metrics = font.getLineMetrics(lines[0], context);
    final var ascent = (int) Math.ceil(metrics.getAscent());
    final var lineHeight =
        ascent + (int) Math.ceil(metrics.getDescent()) + (int) Math.ceil(metrics.getLeading());
    final var top = centerY - (lineHeight * lines.length) / 2;
    for (var i = 0; i < lines.length; i++) {
      final var bounds = font.getStringBounds(lines[i], context);
      final var drawX = (float) (centerX - bounds.getX() - bounds.getWidth() / 2.0);
      g2.drawString(lines[i], drawX, top + ascent + i * lineHeight);
    }
  }

  /** Input {@code index} from the outer corner: 0 is I1, then I0, select, and enable. */
  private static int inputOffset(boolean upper, int index) {
    final var fromOuter = INPUT_INSET + index * INPUT_STEP;
    return upper ? fromOuter : BLOCK_HEIGHT - fromOuter;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var outputEnable = state.getPortValue(pinNrToPortNr(OE));
    final var select = state.getPortValue(pinNrToPortNr(SELECT));
    for (final var channel : CHANNELS) {
      final var source0 = state.getPortValue(pinNrToPortNr(channel[0]));
      final var source1 = state.getPortValue(pinNrToPortNr(channel[1]));
      state.setPort(
          pinNrToPortNr(channel[2]), channelOutput(outputEnable, select, source0, source1), DELAY);
    }
  }

  /**
   * Resolves one multiplexer output, including unknown and error substitutions.
   *
   * <p>A definitely high output enable releases the pin and does not consult the data inputs.
   */
  private static Value channelOutput(
      Value outputEnable, Value select, Value source0, Value source1) {
    if (outputEnable == Value.TRUE) {
      return Value.UNKNOWN;
    }
    final var driven = selected(select, source0, source1);
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

  /** Returns the source chosen by select, or the value both sources share when select is not binary. */
  private static Value selected(Value select, Value source0, Value source1) {
    if (select == Value.TRUE) {
      return source1;
    }
    if (select == Value.FALSE) {
      return source0;
    }
    if (source0 == source1) {
      return source0;
    }
    if (select == Value.ERROR || source0 == Value.ERROR || source1 == Value.ERROR) {
      return Value.ERROR;
    }
    return Value.UNKNOWN;
  }

  private static int pinX(int originX, int pin) {
    if (pin <= GND) {
      return originX + (pin - 1) * 20 + 10;
    }
    return originX + (VCC - pin) * 20 + 10;
  }

  /** Orthogonal wire. A run that starts at a package pin keeps a visible stub before the bend. */
  private static void wire(Graphics g, int... points) {
    for (var i = 2; i < points.length; i += 2) {
      g.drawLine(points[i - 2], points[i - 1], points[i], points[i + 1]);
    }
  }

  /** Active-low mark. Even 2×2 oval, center on the wire axis. */
  private static void drawBubble(Graphics g, int centerX, int centerY) {
    g.drawOval(centerX - 1, centerY - 1, 2, 2);
  }

  /** Junction of a rail and a tap. The oval center is the common point. */
  private static void drawNode(Graphics g, int centerX, int centerY) {
    g.fillOval(centerX - 1, centerY - 1, 2, 2);
  }

  /** One selector. {@code upper} is true when its data pins are on the top edge. */
  private record Section(byte source0, byte source1, byte output, boolean upper, int number) {}
}
