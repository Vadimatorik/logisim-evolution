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
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * TTL 74HC534: octal positive-edge D flip-flop with inverting three-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/cd74hc534.pdf">TI CD74HC534</a> data sheet. The part
 * is the inverting twin of the 74HC374: the DIP-20 pinout matches, and each output is the
 * complement of the stored bit. Each flip-flop stores its D input on the rising edge of CLK. A low
 * nOE drives that complement; the data sheet names those pins 1Q–8Q (Nexperia Q0–Q7). A high nOE,
 * or any level that is not a solid low, releases every output. Output enable does not change the
 * register, and the register still loads while the outputs are released. High impedance is
 * reported as unknown. Nanosecond delays are not modeled. Pin names follow the TI numbering used
 * by the 74273 and 74377 in this library, where nQ1 is Nexperia's Q0 and D1 is Nexperia's D0.
 */
public class Ttl74534 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74534";

  public static final int PORT_INDEX_nOE = 0;
  public static final int PORT_INDEX_nQ1 = 1;
  public static final int PORT_INDEX_D1 = 2;
  public static final int PORT_INDEX_D2 = 3;
  public static final int PORT_INDEX_nQ2 = 4;
  public static final int PORT_INDEX_nQ3 = 5;
  public static final int PORT_INDEX_D3 = 6;
  public static final int PORT_INDEX_D4 = 7;
  public static final int PORT_INDEX_nQ4 = 8;
  public static final int PORT_INDEX_CLK = 9;
  public static final int PORT_INDEX_nQ5 = 10;
  public static final int PORT_INDEX_D5 = 11;
  public static final int PORT_INDEX_D6 = 12;
  public static final int PORT_INDEX_nQ6 = 13;
  public static final int PORT_INDEX_nQ7 = 14;
  public static final int PORT_INDEX_D7 = 15;
  public static final int PORT_INDEX_D8 = 16;
  public static final int PORT_INDEX_nQ8 = 17;
  /** Port index of GND when the explicit power pins are shown. */
  public static final int PORT_INDEX_GND = 18;
  /** Port index of VCC when the explicit power pins are shown. */
  public static final int PORT_INDEX_VCC = 19;

  private static final int DELAY = 1;
  private static final int BITS = 8;
  /** Data port of each bit. Bit 0 is D1, which is Nexperia's D0. */
  private static final int[] DATA = {
    PORT_INDEX_D1,
    PORT_INDEX_D2,
    PORT_INDEX_D3,
    PORT_INDEX_D4,
    PORT_INDEX_D5,
    PORT_INDEX_D6,
    PORT_INDEX_D7,
    PORT_INDEX_D8
  };
  /** Inverting output of each bit. Bit 0 is nQ1, the complement of the bit stored from D1. */
  private static final int[] OUTPUTS = {
    PORT_INDEX_nQ1,
    PORT_INDEX_nQ2,
    PORT_INDEX_nQ3,
    PORT_INDEX_nQ4,
    PORT_INDEX_nQ5,
    PORT_INDEX_nQ6,
    PORT_INDEX_nQ7,
    PORT_INDEX_nQ8
  };
  private static final byte[] OUTPUT_PINS = {2, 5, 6, 9, 12, 15, 16, 19};
  private static final String[] PORT_NAMES = {
    "nOE (output enable, active LOW)",
    "nQ1 (datasheet 1Q, inverted)",
    "D1",
    "D2",
    "nQ2 (datasheet 2Q, inverted)",
    "nQ3 (datasheet 3Q, inverted)",
    "D3",
    "D4",
    "nQ4 (datasheet 4Q, inverted)",
    "CLK",
    "nQ5 (datasheet 5Q, inverted)",
    "D5",
    "D6",
    "nQ6 (datasheet 6Q, inverted)",
    "nQ7 (datasheet 7Q, inverted)",
    "D7",
    "D8",
    "nQ8 (datasheet 8Q, inverted)"
  };

  /** Creates a 74534 octal positive-edge D flip-flop with inverting three-state outputs. */
  public Ttl74534() {
    super(_ID, (byte) 20, OUTPUT_PINS, PORT_NAMES, new Ttl74534HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var gfx = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, true, false);
    Drawgates.paintPortNamesByPin(
        painter,
        x,
        y,
        height,
        new String[] {
          "nOE", "nQ1", "D1", "D2", "nQ2", "nQ3", "D3", "D4", "nQ4", null,
          "CLK", "nQ5", "D5", "D6", "nQ6", "nQ7", "D7", "D8", "nQ8", null
        });
    drawState(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  /** Draws the stored flip-flop bits. The pins show the complement of these bits. */
  private void drawState(Graphics2D gfx, int x, int y, int height, TtlRegisterData state) {
    if (state == null) return;
    for (var bit = 0; bit < BITS; bit++) {
      final var value = state.getValue(bit);
      final var originX = x + 28 + bit * 18;
      gfx.setColor(value.getColor());
      gfx.fillOval(originX, y + height / 2 - 4, 8, 8);
      gfx.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(gfx, value.toDisplayString(), originX + 4, y + height / 2);
    }
    gfx.setColor(Color.BLACK);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.ONE, BITS);
      state.setData(data);
    }
    if (data.updateClock(state.getPortValue(PORT_INDEX_CLK), StdAttr.TRIG_RISING)) {
      for (var bit = 0; bit < BITS; bit++) {
        data.setValue(bit, state.getPortValue(DATA[bit]));
      }
    }
    final var enabled = state.getPortValue(PORT_INDEX_nOE) == Value.FALSE;
    for (var bit = 0; bit < BITS; bit++) {
      final var stored = data.getValue(bit);
      state.setPort(OUTPUTS[bit], enabled ? invert(stored) : Value.UNKNOWN, DELAY);
    }
  }

  /**
   * Complements a stored bit. A one-bit {@link Value#not()} turns unknown into error, but an
   * undefined input stays undefined at the inverting output.
   */
  private static Value invert(Value bit) {
    if (bit == Value.TRUE) return Value.FALSE;
    if (bit == Value.FALSE) return Value.TRUE;
    return bit;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_CLK};
  }
}
