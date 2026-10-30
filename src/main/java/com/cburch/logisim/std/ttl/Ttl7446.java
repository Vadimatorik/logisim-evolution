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
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.std.plexers.PlexersLibrary;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Map;

/**
 * TTL 7446: BCD to 7-segment decoder/driver.
 *
 * <p>Simulation follows the function table shared by the
 * <a href="https://media.digikey.com/pdf/Data%20Sheets/Fairchild%20PDFs/DM7446A,%20DM7447A.pdf">DM7446A</a>
 * and TI SN7446A/SN74LS46 data sheets. Segment outputs are active low. A low {@code BI} blanks
 * every segment. Otherwise a low {@code LT} turns every segment on. Otherwise a low {@code RBI}
 * blanks a BCD zero and drives {@code RBO} low. Codes 10 through 15 keep the non-decimal patterns
 * from that table. The 30 V open-collector rating is not modeled; segment pins are logic levels,
 * the same way as the 7447. {@code BI/RBO} is an open-collector pin with a pull-up. There is no
 * separate 74HC46 data sheet; a 74HC46 in the same DIP-16 pinout follows this table.
 */
public class Ttl7446 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7446";

  public static final int PORT_INDEX_B = 0;
  public static final int PORT_INDEX_C = 1;
  public static final int PORT_INDEX_LT = 2;
  public static final int PORT_INDEX_BI_RBO = 3;
  public static final int PORT_INDEX_RBI = 4;
  public static final int PORT_INDEX_D = 5;
  public static final int PORT_INDEX_A = 6;
  public static final int PORT_INDEX_QE = 7;
  public static final int PORT_INDEX_QD = 8;
  public static final int PORT_INDEX_QC = 9;
  public static final int PORT_INDEX_QB = 10;
  public static final int PORT_INDEX_QA = 11;
  public static final int PORT_INDEX_QG = 12;
  public static final int PORT_INDEX_QF = 13;

  public Ttl7446() {
    super(
        _ID,
        (byte) 16,
        new byte[] {9, 10, 11, 12, 13, 14, 15},
        new byte[] {},
        new byte[] {4},
        new String[] {"B", "C", "LT", "BI/RBO", "RBI", "D", "A", "e", "d", "c", "b", "a", "g", "f"},
        new Ttl7447HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 8));
    g.drawRect(x + 145, y + 12, 10, 10);
    g.drawRect(x + 125, y + 12, 10, 10);
    g.drawRect(x + 105, y + 12, 10, 10);
    g.drawRect(x + 85, y + 12, 10, 10);
    g.drawRect(x + 65, y + 12, 10, 10);
    g.drawRect(x + 45, y + 12, 10, 10);
    g.drawRect(x + 25, y + 12, 10, 10);
    g.drawLine(x + 150, y + 12, x + 150, y + 7);
    g.drawLine(x + 130, y + 12, x + 130, y + 7);
    g.drawLine(x + 110, y + 12, x + 110, y + 7);
    g.drawLine(x + 90, y + 12, x + 90, y + 7);
    g.drawLine(x + 70, y + 12, x + 70, y + 7);
    g.drawLine(x + 50, y + 12, x + 50, y + 7);
    g.drawLine(x + 30, y + 12, x + 30, y + 7);
    g.drawLine(x + 25, y + 24, x + 155, y + 24);
    g.drawLine(x + 10, y + height - 7, x + 10, y + height - 9);
    g.drawLine(x + 10, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 24);
    g.drawLine(x + 21, y + 24, x + 25, y + 24);
    g.drawLine(x + 147, y + 24, x + 147, y + 22);
    g.drawLine(x + 127, y + 24, x + 127, y + 22);
    g.drawLine(x + 107, y + 24, x + 107, y + 22);
    g.drawLine(x + 87, y + 24, x + 87, y + 22);
    g.drawLine(x + 67, y + 24, x + 67, y + 22);
    g.drawLine(x + 47, y + 24, x + 47, y + 22);
    g.drawLine(x + 27, y + 24, x + 27, y + 22);
    g.drawLine(x + 25, y + 24, x + 155, y + 24);
    g.drawLine(x + 30, y + height - 7, x + 30, y + height - 9);
    g.drawLine(x + 30, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 24);
    g.drawLine(x + 21, y + 24, x + 25, y + 24);
    g.drawLine(x + 150, y + 24, x + 150, y + 22);
    g.drawLine(x + 130, y + 24, x + 130, y + 22);
    g.drawLine(x + 110, y + 24, x + 110, y + 22);
    g.drawLine(x + 90, y + 24, x + 90, y + 22);
    g.drawLine(x + 70, y + 24, x + 70, y + 22);
    g.drawLine(x + 50, y + 24, x + 50, y + 22);
    g.drawLine(x + 30, y + 24, x + 30, y + 22);
    g.drawLine(x + 25, y + 24, x + 155, y + 24);
    g.drawLine(x + 50, y + height - 7, x + 50, y + height - 9);
    g.drawLine(x + 50, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 24);
    g.drawLine(x + 21, y + 24, x + 25, y + 24);
    g.drawLine(x + 153, y + 24, x + 153, y + 22);
    g.drawLine(x + 133, y + 24, x + 133, y + 22);
    g.drawLine(x + 113, y + 24, x + 113, y + 22);
    g.drawLine(x + 93, y + 24, x + 93, y + 22);
    g.drawLine(x + 73, y + 24, x + 73, y + 22);
    g.drawLine(x + 53, y + 24, x + 53, y + 22);
    g.drawLine(x + 33, y + 24, x + 33, y + 22);
    g.drawLine(x + 25, y + 25, x + 155, y + 25);
    g.drawLine(x + 90, y + height - 7, x + 90, y + height - 9);
    g.drawLine(x + 90, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 25);
    g.drawLine(x + 21, y + 25, x + 25, y + 25);
    g.drawLine(x + 147, y + 25, x + 147, y + 22);
    g.drawLine(x + 127, y + 25, x + 127, y + 22);
    g.drawLine(x + 107, y + 25, x + 107, y + 22);
    g.drawLine(x + 87, y + 25, x + 87, y + 22);
    g.drawLine(x + 67, y + 25, x + 67, y + 22);
    g.drawLine(x + 47, y + 25, x + 47, y + 22);
    g.drawLine(x + 27, y + 25, x + 27, y + 22);
    g.drawLine(x + 25, y + 25, x + 155, y + 25);
    g.drawLine(x + 110, y + height - 7, x + 110, y + height - 9);
    g.drawLine(x + 110, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 25);
    g.drawLine(x + 21, y + 25, x + 25, y + 25);
    g.drawLine(x + 150, y + 25, x + 150, y + 22);
    g.drawLine(x + 130, y + 25, x + 130, y + 22);
    g.drawLine(x + 110, y + 25, x + 110, y + 22);
    g.drawLine(x + 90, y + 25, x + 90, y + 22);
    g.drawLine(x + 70, y + 25, x + 70, y + 22);
    g.drawLine(x + 50, y + 25, x + 50, y + 22);
    g.drawLine(x + 30, y + 25, x + 30, y + 22);
    g.drawLine(x + 25, y + 25, x + 155, y + 25);
    g.drawLine(x + 130, y + height - 7, x + 130, y + height - 9);
    g.drawLine(x + 130, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 25);
    g.drawLine(x + 21, y + 25, x + 25, y + 25);
    g.drawLine(x + 153, y + 25, x + 153, y + 22);
    g.drawLine(x + 133, y + 25, x + 133, y + 22);
    g.drawLine(x + 113, y + 25, x + 113, y + 22);
    g.drawLine(x + 93, y + 25, x + 93, y + 22);
    g.drawLine(x + 73, y + 25, x + 73, y + 22);
    g.drawLine(x + 53, y + 25, x + 53, y + 22);
    g.drawLine(x + 33, y + 25, x + 33, y + 22);
    g.drawLine(x + 70, y + height - 7, x + 70, y + 25);
    g.drawLine(x + 70, y + 25, x + 147, y + 25);
    g.drawLine(x + 147, y + 25, x + 147, y + 22);
    GraphicsUtil.drawCenteredText(g, "e", x + 150, y + 17);
    GraphicsUtil.drawCenteredText(g, "d", x + 130, y + 17);
    GraphicsUtil.drawCenteredText(g, "c", x + 110, y + 17);
    GraphicsUtil.drawCenteredText(g, "b", x + 90, y + 17);
    GraphicsUtil.drawCenteredText(g, "a", x + 70, y + 17);
    GraphicsUtil.drawCenteredText(g, "g", x + 50, y + 17);
    GraphicsUtil.drawCenteredText(g, "f", x + 30, y + 17);
    g.setFont(previousFont);
  }



  @Override
  protected void configureNewInstance(Instance instance) {
    super.configureNewInstance(instance);
    instance.getComponent().setPullPorts(Map.of(PORT_INDEX_BI_RBO, Value.TRUE));
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var inputValue =
        DisplayDecoder.getdecval(
            state, false, 0, PORT_INDEX_A, PORT_INDEX_B, PORT_INDEX_C, PORT_INDEX_D);
    final var blankZero = state.getPortValue(PORT_INDEX_RBI) == Value.FALSE && inputValue == 0;
    DisplayDecoder.computeDisplayDecoderOutputs(
        state,
        inputValue,
        PORT_INDEX_QA,
        PORT_INDEX_QB,
        PORT_INDEX_QC,
        PORT_INDEX_QD,
        PORT_INDEX_QE,
        PORT_INDEX_QF,
        PORT_INDEX_QG,
        PORT_INDEX_LT,
        PORT_INDEX_BI_RBO,
        PORT_INDEX_RBI);
    final var rippleBlank = blankZero && state.getPortValue(PORT_INDEX_LT) != Value.FALSE;
    state.setPort(
        PORT_INDEX_BI_RBO, rippleBlank ? Value.FALSE : Value.UNKNOWN, PlexersLibrary.DELAY);
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
