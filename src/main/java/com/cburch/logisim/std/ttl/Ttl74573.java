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
 * TTL 74HC573: octal transparent D latch with three-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/cd74hc573.pdf">TI CD74HC573</a> and
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT573.pdf">Nexperia 74HC573</a>
 * data sheets. While {@code LE} is high the stored word follows {@code D}. Taking {@code LE} low
 * holds that word. A high {@code nOE}, or any level that is not a solid low, releases every
 * output, which this model reports as unknown, and does not change the latch. Port names follow
 * Nexperia: {@code D0} is TI {@code 1D} and {@code Q0} is TI {@code 1Q}. The part has the same
 * function as the 74HC373, with the inputs and outputs on opposite sides.
 */
public class Ttl74573 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74573";

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
  /** Output port of each stored bit. Bit 0 is Q0, which follows D0. */
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

  /** Creates a 74573 octal transparent latch. */
  public Ttl74573() {
    super(_ID, (byte) 20, OUTPUT_PINS, PORT_NAMES, new Ttl74573HdlGenerator());
  }

  private static final int STRUCTURE_PINS = 20;
  private static final int[] STRUCTURE_D_PINS = {2, 3, 4, 5, 6, 7, 8, 9};
  private static final int[] STRUCTURE_Q_PINS = {19, 18, 17, 16, 15, 14, 13, 12};

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var rawState = painter.getData();
    final TtlRegisterData stored = rawState instanceof TtlRegisterData ? (TtlRegisterData) rawState : null;
    final var boxTop = y + 12;
    final var boxBottom = boxTop + 14;
    final var labelY = boxTop + 8;
    var busLeft = Integer.MAX_VALUE;
    var busRight = Integer.MIN_VALUE;
    for (var bit = 0; bit < BITS; bit++) {
      final var dX = structureX(x, STRUCTURE_PINS, STRUCTURE_D_PINS[bit]);
      final var qX = structureX(x, STRUCTURE_PINS, STRUCTURE_Q_PINS[bit]);
      final var centerX = (dX + qX) / 2;
      final var left = centerX - 6;
      busLeft = Math.min(busLeft, left);
      busRight = Math.max(busRight, left + 12);
      g.drawRect(left, boxTop, 12, 14);
      GraphicsUtil.drawCenteredText(g, "L", centerX, labelY);
      structureDot(g, centerX, labelY, stored, 0, bit);
      final var dPinY = structureY(y, height, STRUCTURE_PINS, STRUCTURE_D_PINS[bit]);
      g.drawLine(dX, dPinY, dX, boxBottom);
      if (dX != centerX) {
        g.drawLine(dX, boxBottom, centerX, boxBottom);
      }
      final var qPinY = structureY(y, height, STRUCTURE_PINS, STRUCTURE_Q_PINS[bit]);
      g.drawLine(qX, qPinY, qX, boxTop);
      if (qX != centerX) {
        g.drawLine(centerX, boxTop, qX, boxTop);
      }
    }
    g.drawLine(busLeft, boxBottom, busRight, boxBottom);
    final var leX = structureX(x, STRUCTURE_PINS, 10);
    final var lePinY = structureY(y, height, STRUCTURE_PINS, 10);
    g.drawLine(leX, lePinY, leX, boxBottom);
    final var oeX = structureX(x, STRUCTURE_PINS, 1);
    final var oePinY = structureY(y, height, STRUCTURE_PINS, 1);
    g.drawLine(oeX, oePinY, oeX, boxTop);
    structureBubble(g, oeX, boxTop);
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
      data.setValue(capturedInputs(state));
    }
    final var word = data.getValue();
    final var driving = state.getPortValue(PORT_INDEX_nOE) == Value.FALSE;
    for (var bit = 0; bit < BITS; bit++) {
      state.setPort(OUTPUTS[bit], driving ? word.get(bit) : Value.UNKNOWN, DELAY);
    }
  }

  /** Copies D0 through D7, including unknown and error bits. */
  private static Value capturedInputs(InstanceState state) {
    final var bits = new Value[BITS];
    for (var bit = 0; bit < BITS; bit++) {
      bits[bit] = state.getPortValue(PORT_INDEX_D0 + bit);
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
