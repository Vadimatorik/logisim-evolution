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
import com.cburch.logisim.util.GraphicsUtil;

import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

import java.awt.Graphics;

/**
 * TTL 74x386: quad 2-input exclusive-OR gate.
 *
 * <p>Simulation follows the Renesas HD74HC386 data sheet, Rev.2.00, 30 March 2006. Each output is
 * the exclusive OR of its two inputs. The package is 14 pins with GND on pin 7 and VCC on pin 14.
 * Gates 2 and 3 are not in the same pin order as the 7486: pin 4 is 2Y, pins 5 and 6 are 2A and
 * 2B, pin 8 is 3A, pin 9 is 3B and pin 10 is 3Y. CMOS input thresholds are not modeled.
 */
public class Ttl74386 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74386";

  public static final int PORT_INDEX_1A = 0;
  public static final int PORT_INDEX_1B = 1;
  public static final int PORT_INDEX_1Y = 2;
  public static final int PORT_INDEX_2Y = 3;
  public static final int PORT_INDEX_2A = 4;
  public static final int PORT_INDEX_2B = 5;
  public static final int PORT_INDEX_3A = 6;
  public static final int PORT_INDEX_3B = 7;
  public static final int PORT_INDEX_3Y = 8;
  public static final int PORT_INDEX_4Y = 9;
  public static final int PORT_INDEX_4A = 10;
  public static final int PORT_INDEX_4B = 11;

  private static final int DELAY = 1;
  private static final byte[] OUTPUT_PINS = {3, 4, 10, 11};
  private static final String[] PORT_NAMES = {
    "1A", "1B", "1Y", "2Y", "2A", "2B", "3A", "3B", "3Y", "4Y", "4A", "4B"
  };
  private static final int[][] GATES = {
    {PORT_INDEX_1A, PORT_INDEX_1B, PORT_INDEX_1Y},
    {PORT_INDEX_2A, PORT_INDEX_2B, PORT_INDEX_2Y},
    {PORT_INDEX_3A, PORT_INDEX_3B, PORT_INDEX_3Y},
    {PORT_INDEX_4A, PORT_INDEX_4B, PORT_INDEX_4Y}
  };

  /** Creates a 74386 quad 2-input exclusive-OR gate. */
  public Ttl74386() {
    super(_ID, (byte) 14, OUTPUT_PINS, PORT_NAMES, new Ttl74386HdlGenerator());
  }

    @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var bodyFont = g.getFont();
    g.setFont(bodyFont.deriveFont(9f));
    g.drawRect((x + 12), (y + 18), 122, 24);
    GraphicsUtil.drawCenteredText(g, "BUF", (x + 73), (y + 30));
    g.setFont(bodyFont);
    final var top = y + PIN_HEIGHT;
    final var bottom = y + height - PIN_HEIGHT;
    g.drawLine((x + 10), bottom, (x + 10), (y + 42));
    g.drawLine((x + 30), bottom, (x + 30), (y + 42));
    g.drawLine((x + 50), bottom, (x + 50), (y + 42));
    g.drawLine((x + 70), bottom, (x + 70), (y + 42));
    g.drawLine((x + 90), bottom, (x + 90), (y + 42));
    g.drawLine((x + 110), bottom, (x + 110), (y + 42));
    g.drawLine((x + 130), top, (x + 130), (y + 18));
    g.drawLine((x + 110), top, (x + 110), (y + 18));
    g.drawLine((x + 90), top, (x + 90), (y + 18));
    g.drawLine((x + 70), top, (x + 70), (y + 18));
    g.drawLine((x + 50), top, (x + 50), (y + 18));
    g.drawLine((x + 30), top, (x + 30), (y + 18));
  }

  @Override
  public void propagateTtl(InstanceState state) {
    for (final var gate : GATES) {
      state.setPort(gate[2], state.getPortValue(gate[0]).xor(state.getPortValue(gate[1])), DELAY);
    }
  }

  /**
   * Draws one right-facing XOR and wires it to pin columns that all lie on the same package side.
   */
  private static void paintXorToPin(
      Graphics g, int outX, int outY, int pinY, int[] inputX, int outputPinX) {
    final int width = 16;
    final int height = 12;
    Drawgates.paintXor(g, outX, outY, width, height, false);
    g.drawPolyline(new int[] {outX, outputPinX, outputPinX}, new int[] {outY, outY, pinY}, 3);
    final int inputEdge = outX - width;
    for (var i = 0; i < inputX.length; i++) {
      final int entry = outY - height / 4 + i * (height / 2);
      g.drawPolyline(new int[] {inputX[i], inputX[i], inputEdge}, new int[] {pinY, entry, entry}, 3);
    }
  }

}
