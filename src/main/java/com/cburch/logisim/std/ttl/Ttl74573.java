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
import java.awt.Graphics2D;

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

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var gfx = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, false, false);
    Drawgates.paintPortNamesByPin(
        painter,
        x,
        y,
        height,
        new String[] {
          "nOE", "D0", "D1", "D2", "D3", "D4", "D5", "D6", "D7", null,
          "LE", "Q7", "Q6", "Q5", "Q4", "Q3", "Q2", "Q1", "Q0", null
        });
    drawState(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private void drawState(Graphics2D gfx, int x, int y, int height, TtlRegisterData state) {
    if (state == null) return;
    final var word = state.getValue();
    for (var bit = 0; bit < BITS; bit++) {
      final var value = word.get(BITS - 1 - bit);
      final var dotX = x + 36 + bit * 16;
      gfx.setColor(value.getColor());
      gfx.fillOval(dotX, y + height / 2 - 4, 8, 8);
      gfx.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(gfx, value.toDisplayString(), dotX + 4, y + height / 2);
    }
    gfx.setColor(Color.BLACK);
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
}
