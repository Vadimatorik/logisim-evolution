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
import java.awt.Font;
import com.cburch.logisim.instance.InstancePainter;

/**
 * TTL 74x37: quad 2-input NAND buffer.
 *
 * <p>Model based on the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74ls37.pdf">Texas Instruments SN74LS37
 * datasheet</a>. Each output is the NAND of its two inputs, and the DIP-14 pinout matches the
 * 7400. The buffer designation is a higher output current in the bipolar families; drive strength
 * is not modeled. Drawing and HDL generation are inherited from {@link Ttl7400}.
 */
public class Ttl7437 extends Ttl7400 {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7437";

  public static final int PORT_INDEX_1A = 0;
  public static final int PORT_INDEX_1B = 1;
  public static final int PORT_INDEX_1Y = 2;
  public static final int PORT_INDEX_2A = 3;
  public static final int PORT_INDEX_2B = 4;
  public static final int PORT_INDEX_2Y = 5;
  public static final int PORT_INDEX_3Y = 6;
  public static final int PORT_INDEX_3A = 7;
  public static final int PORT_INDEX_3B = 8;
  public static final int PORT_INDEX_4Y = 9;
  public static final int PORT_INDEX_4A = 10;
  public static final int PORT_INDEX_4B = 11;

  public Ttl7437() {
    super(_ID);
  }
  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 8));
    g.drawRect(x + 45, y + 28, 10, 10);
    g.drawRect(x + 105, y + 28, 10, 10);
    g.drawRect(x + 125, y + 12, 10, 10);
    g.drawRect(x + 65, y + 12, 10, 10);
    g.drawLine(x + 50, y + height - 22, x + 50, y + height - 7);
    g.drawLine(x + 110, y + height - 22, x + 110, y + height - 7);
    g.drawLine(x + 130, y + 12, x + 130, y + 7);
    g.drawLine(x + 70, y + 12, x + 70, y + 7);
    g.drawLine(x + 10, y + height - 7, x + 10, y + height - 19);
    g.drawLine(x + 10, y + height - 19, x + 47, y + height - 19);
    g.drawLine(x + 47, y + height - 19, x + 47, y + height - 22);
    g.drawLine(x + 30, y + height - 7, x + 30, y + height - 16);
    g.drawLine(x + 30, y + height - 16, x + 49, y + height - 16);
    g.drawLine(x + 49, y + height - 16, x + 49, y + height - 22);
    g.drawLine(x + 70, y + height - 7, x + 70, y + height - 19);
    g.drawLine(x + 70, y + height - 19, x + 107, y + height - 19);
    g.drawLine(x + 107, y + height - 19, x + 107, y + height - 22);
    g.drawLine(x + 90, y + height - 7, x + 90, y + height - 16);
    g.drawLine(x + 90, y + height - 16, x + 109, y + height - 16);
    g.drawLine(x + 109, y + height - 16, x + 109, y + height - 22);
    g.drawLine(x + 110, y + 7, x + 110, y + 9);
    g.drawLine(x + 110, y + 9, x + 127, y + 9);
    g.drawLine(x + 127, y + 9, x + 127, y + 12);
    g.drawLine(x + 90, y + 7, x + 90, y + 6);
    g.drawLine(x + 90, y + 6, x + 129, y + 6);
    g.drawLine(x + 129, y + 6, x + 129, y + 12);
    g.drawLine(x + 50, y + 7, x + 50, y + 9);
    g.drawLine(x + 50, y + 9, x + 67, y + 9);
    g.drawLine(x + 67, y + 9, x + 67, y + 12);
    g.drawLine(x + 30, y + 7, x + 30, y + 6);
    g.drawLine(x + 30, y + 6, x + 69, y + 6);
    g.drawLine(x + 69, y + 6, x + 69, y + 12);
    GraphicsUtil.drawCenteredText(g, "Y", x + 50, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "Y", x + 110, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "Y", x + 130, y + 17);
    GraphicsUtil.drawCenteredText(g, "Y", x + 70, y + 17);
    g.setFont(previousFont);
  }


}
