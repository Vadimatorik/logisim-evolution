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
 * TTL 74x4515: 4-to-16 line decoder/demultiplexer with input latches and inverting outputs.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC4515.pdf">Nexperia 74HC4515</a>
 * data sheet. While {@code LE} is high the latched address follows {@code A3..A0}. Any level that
 * is not a solid high, including unknown and error, holds that address. While {@code nE} is low the
 * output selected by the latched address is low and the others are high. A high {@code nE} forces
 * every output high and does not change the latch. The data sheet calls this enable {@code E}; the
 * model names it {@code nE} because it is active low. Using {@code nE} as data covers the
 * demultiplexer function. Nanosecond delays are not modeled.
 *
 * <p>An unknown or error value on {@code nE}, or in the latched address while the device is
 * enabled, makes every output unknown. A known high {@code nE} still forces every output high.
 */
public class Ttl744515 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "744515";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet.
  public static final byte LE = 1;
  public static final byte A0 = 2;
  public static final byte A1 = 3;
  public static final byte Q7 = 4;
  public static final byte Q6 = 5;
  public static final byte Q5 = 6;
  public static final byte Q4 = 7;
  public static final byte Q3 = 8;
  public static final byte Q1 = 9;
  public static final byte Q2 = 10;
  public static final byte Q0 = 11;
  public static final byte GND = 12;
  public static final byte Q13 = 13;
  public static final byte Q12 = 14;
  public static final byte Q15 = 15;
  public static final byte Q14 = 16;
  public static final byte Q9 = 17;
  public static final byte Q8 = 18;
  public static final byte Q11 = 19;
  public static final byte Q10 = 20;
  public static final byte A2 = 21;
  public static final byte A3 = 22;
  public static final byte NE = 23;
  public static final byte VCC = 24;

  private static final int BITS = 4;
  private static final BitWidth WIDTH = BitWidth.create(BITS);

  /** Outputs ordered by the address that selects them. */
  private static final byte[] OUTPUTS = {
    Q0, Q1, Q2, Q3, Q4, Q5, Q6, Q7, Q8, Q9, Q10, Q11, Q12, Q13, Q14, Q15
  };

  private static final byte[] OUTPUT_PINS = {
    Q7, Q6, Q5, Q4, Q3, Q1, Q2, Q0, Q13, Q12, Q15, Q14, Q9, Q8, Q11, Q10
  };

  private static final byte[] ADDRESS = {A0, A1, A2, A3};

  private static final String[] PORT_NAMES = {
    "LE Latch enable (active HIGH)",
    "A0 Address (LSB)",
    "A1 Address",
    "Q7",
    "Q6",
    "Q5",
    "Q4",
    "Q3",
    "Q1",
    "Q2",
    "Q0",
    "Q13",
    "Q12",
    "Q15",
    "Q14",
    "Q9",
    "Q8",
    "Q11",
    "Q10",
    "A2 Address",
    "A3 Address (MSB)",
    "nE Enable (active LOW)"
  };

  /** Creates a 744515 4-to-16 line decoder/demultiplexer with input latches. */
  public Ttl744515() {
    super(_ID, (byte) 24, OUTPUT_PINS, PORT_NAMES, new Ttl744515HdlGenerator());
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
    final var gfx = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, false, false);
    Drawgates.paintPortNamesByPin(
        painter,
        x,
        y,
        height,
        new String[] {
          "LE", "A0", "A1", "Q7", "Q6", "Q5", "Q4", "Q3", "Q1", "Q2", "Q0", null,
          "Q13", "Q12", "Q15", "Q14", "Q9", "Q8", "Q11", "Q10", "A2", "A3", "nE", null
        });
    drawState(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private void drawState(Graphics2D gfx, int x, int y, int height, TtlRegisterData state) {
    if (state == null) return;
    final var word = state.getValue();
    for (var bit = 0; bit < BITS; bit++) {
      final var value = word.get(BITS - 1 - bit);
      final var dotX = x + 96 + bit * 16;
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
    if (state.getPortValue(pinNrToPortNr(LE)) == Value.TRUE) {
      data.setValue(capturedAddress(state));
    }
    driveOutputs(state, data.getValue(), state.getPortValue(pinNrToPortNr(NE)));
  }

  /** Copies A0 through A3, including unknown and error bits. Bit 0 is A0. */
  private static Value capturedAddress(InstanceState state) {
    final var bits = new Value[BITS];
    for (var bit = 0; bit < ADDRESS.length; bit++) {
      bits[bit] = state.getPortValue(pinNrToPortNr(ADDRESS[bit]));
    }
    return Value.create(bits);
  }

  private static void driveOutputs(InstanceState state, Value address, Value enable) {
    if (enable != Value.TRUE && enable != Value.FALSE) {
      setOutputs(state, Value.UNKNOWN, -1);
      return;
    }
    if (enable != Value.FALSE) {
      setOutputs(state, Value.TRUE, -1);
      return;
    }
    if (!address.isFullyDefined()) {
      setOutputs(state, Value.UNKNOWN, -1);
      return;
    }
    setOutputs(state, Value.TRUE, (int) address.toLongValue());
  }

  /** Drives every output high, or unknown, except {@code selected} when it is in range. */
  private static void setOutputs(InstanceState state, Value inactive, int selected) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      final var value = index == selected ? Value.FALSE : inactive;
      state.setPort(pinNrToPortNr(OUTPUTS[index]), value, DELAY);
    }
  }
}
