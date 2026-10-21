/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;

/**
 * TTL 74HC563: octal transparent D latch with inverting three-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74hc563.pdf">TI SN74HC563</a> and
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT563.pdf">Nexperia 74HC563</a>
 * data sheets. While {@code LE} is high the stored word follows the complement of {@code D}. Taking
 * {@code LE} low holds that complement. A high {@code nOE} puts every output in the high-impedance
 * state, which this model reports as unknown, and does not change the latch. The part is the
 * inverting-output counterpart of the 74HC573.
 */
public class Ttl74563 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74563";

  public static final int PORT_INDEX_nOE = 0;
  public static final int PORT_INDEX_D0 = 1;
  public static final int PORT_INDEX_D1 = 2;
  public static final int PORT_INDEX_D2 = 3;
  public static final int PORT_INDEX_D3 = 4;
  public static final int PORT_INDEX_D4 = 5;
  public static final int PORT_INDEX_D5 = 6;
  public static final int PORT_INDEX_D6 = 7;
  public static final int PORT_INDEX_D7 = 8;
  public static final int PORT_INDEX_LE = 9;
  public static final int PORT_INDEX_Q7 = 10;
  public static final int PORT_INDEX_Q6 = 11;
  public static final int PORT_INDEX_Q5 = 12;
  public static final int PORT_INDEX_Q4 = 13;
  public static final int PORT_INDEX_Q3 = 14;
  public static final int PORT_INDEX_Q2 = 15;
  public static final int PORT_INDEX_Q1 = 16;
  public static final int PORT_INDEX_Q0 = 17;

  private static final int DELAY = 1;
  private static final int BITS = 8;
  private static final BitWidth WIDTH = BitWidth.create(BITS);
  private static final byte[] OUTPUT_PINS = {12, 13, 14, 15, 16, 17, 18, 19};
  private static final String[] PORT_NAMES = {
    "nOE (output enable, active low)",
    "D0",
    "D1",
    "D2",
    "D3",
    "D4",
    "D5",
    "D6",
    "D7",
    "LE (latch enable, active high)",
    "Q7",
    "Q6",
    "Q5",
    "Q4",
    "Q3",
    "Q2",
    "Q1",
    "Q0"
  };
  /** Output port of each stored bit. Bit 0 is Q0, which is the complement of D0. */
  private static final int[] OUTPUTS = {
    PORT_INDEX_Q0,
    PORT_INDEX_Q1,
    PORT_INDEX_Q2,
    PORT_INDEX_Q3,
    PORT_INDEX_Q4,
    PORT_INDEX_Q5,
    PORT_INDEX_Q6,
    PORT_INDEX_Q7
  };

  /** Creates a 74563 octal inverting transparent latch. */
  public Ttl74563() {
    super(_ID, (byte) 20, OUTPUT_PINS, PORT_NAMES, new Ttl74563HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var rawState = painter.getData();
    final TtlRegisterData stored = rawState instanceof TtlRegisterData ? (TtlRegisterData) rawState : null;
    g.drawRect((x + 24), (y + 12), 12, 14);
    g.drawLine((x + 30), (y + 12), (x + 30), (y + 7));
    g.drawLine((x + 30), (y + height - 7), (x + 30), ((y + 12)) + 14);
    GraphicsUtil.drawCenteredText(g, "L", (x + 30), ((y + 12)) + 8);
    structureDot(g, (x + 30), ((y + 12)) + 7, stored, 0, 0);
    g.drawRect((x + 44), (y + 12), 12, 14);
    g.drawLine((x + 50), (y + 12), (x + 50), (y + 7));
    g.drawLine((x + 50), (y + height - 7), (x + 50), ((y + 12)) + 14);
    GraphicsUtil.drawCenteredText(g, "L", (x + 50), ((y + 12)) + 8);
    g.drawRect((x + 64), (y + 12), 12, 14);
    g.drawLine((x + 70), (y + 12), (x + 70), (y + 7));
    g.drawLine((x + 70), (y + height - 7), (x + 70), ((y + 12)) + 14);
    GraphicsUtil.drawCenteredText(g, "L", (x + 70), ((y + 12)) + 8);
    g.drawRect((x + 84), (y + 12), 12, 14);
    g.drawLine((x + 90), (y + 12), (x + 90), (y + 7));
    g.drawLine((x + 90), (y + height - 7), (x + 90), ((y + 12)) + 14);
    GraphicsUtil.drawCenteredText(g, "L", (x + 90), ((y + 12)) + 8);
    g.drawRect((x + 104), (y + 12), 12, 14);
    g.drawLine((x + 110), (y + 12), (x + 110), (y + 7));
    g.drawLine((x + 110), (y + height - 7), (x + 110), ((y + 12)) + 14);
    GraphicsUtil.drawCenteredText(g, "L", (x + 110), ((y + 12)) + 8);
    g.drawRect((x + 124), (y + 12), 12, 14);
    g.drawLine((x + 130), (y + 12), (x + 130), (y + 7));
    g.drawLine((x + 130), (y + height - 7), (x + 130), ((y + 12)) + 14);
    GraphicsUtil.drawCenteredText(g, "L", (x + 130), ((y + 12)) + 8);
    g.drawRect((x + 144), (y + 12), 12, 14);
    g.drawLine((x + 150), (y + 12), (x + 150), (y + 7));
    g.drawLine((x + 150), (y + height - 7), (x + 150), ((y + 12)) + 14);
    GraphicsUtil.drawCenteredText(g, "L", (x + 150), ((y + 12)) + 8);
    g.drawRect((x + 164), (y + 12), 12, 14);
    g.drawLine((x + 170), (y + 12), (x + 170), (y + 7));
    g.drawLine((x + 170), (y + height - 7), (x + 170), ((y + 12)) + 14);
    GraphicsUtil.drawCenteredText(g, "L", (x + 170), ((y + 12)) + 8);
    g.drawLine((x + 24), (y + 28), (x + 176), (y + 28));
    g.drawLine((x + 30), (y + 28), (x + 30), (y + 26));
    g.drawLine((x + 50), (y + 28), (x + 50), (y + 26));
    g.drawLine((x + 70), (y + 28), (x + 70), (y + 26));
    g.drawLine((x + 90), (y + 28), (x + 90), (y + 26));
    g.drawLine((x + 110), (y + 28), (x + 110), (y + 26));
    g.drawLine((x + 130), (y + 28), (x + 130), (y + 26));
    g.drawLine((x + 150), (y + 28), (x + 150), (y + 26));
    g.drawLine((x + 170), (y + 28), (x + 170), (y + 26));
    g.drawLine((x + 190), (y + 7), (x + 190), (y + 13));
    g.drawLine((x + 190), (y + 13), (x + 24), (y + 13));
    g.drawLine((x + 24), (y + 13), (x + 24), (y + 28));
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + 30));
    structureBubble(g, (x + 10), (y + height - 7));
  }

  private static TtlRegisterData getStateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WIDTH);
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);
    if (state.getPortValue(PORT_INDEX_LE) == Value.TRUE) {
      data.setValue(invertedInputs(state));
    }
    final var word = data.getValue();
    final var driving = state.getPortValue(PORT_INDEX_nOE) == Value.FALSE;
    for (var bit = 0; bit < BITS; bit++) {
      state.setPort(OUTPUTS[bit], driving ? word.get(bit) : Value.UNKNOWN, DELAY);
    }
  }

  /** Complements D0 through D7. A floating or erroneous input becomes an error bit. */
  private static Value invertedInputs(InstanceState state) {
    final var bits = new Value[BITS];
    for (var bit = 0; bit < BITS; bit++) {
      bits[bit] = state.getPortValue(PORT_INDEX_D0 + bit).not();
    }
    return Value.create(bits);
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
