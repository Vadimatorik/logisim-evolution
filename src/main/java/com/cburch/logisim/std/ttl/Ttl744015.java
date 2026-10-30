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
import java.awt.Font;
import java.awt.Graphics;

/**
 * TTL 74x4015: dual 4-bit static shift register.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/cd74hc4015.pdf">CD74HC4015</a> and NXP 74HC/HCT4015
 * data sheets. Each half is an independent serial-in, parallel-out register. On the low-to-high
 * transition of {@code nCP}, {@code nD} enters {@code nQ0} and the previous bits move toward
 * {@code nQ3}. A high {@code nMR} asynchronously clears that half and overrides the clock and
 * data. DIP-16 and SO16 use the same pin numbers. Nanosecond delays are not modeled.
 */
public class Ttl744015 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744015";

  public static final int PORT_INDEX_2CP = 0;
  public static final int PORT_INDEX_2Q3 = 1;
  public static final int PORT_INDEX_1Q2 = 2;
  public static final int PORT_INDEX_1Q1 = 3;
  public static final int PORT_INDEX_1Q0 = 4;
  public static final int PORT_INDEX_1MR = 5;
  public static final int PORT_INDEX_1D = 6;
  public static final int PORT_INDEX_1CP = 7;
  public static final int PORT_INDEX_1Q3 = 8;
  public static final int PORT_INDEX_2Q2 = 9;
  public static final int PORT_INDEX_2Q1 = 10;
  public static final int PORT_INDEX_2Q0 = 11;
  public static final int PORT_INDEX_2MR = 12;
  public static final int PORT_INDEX_2D = 13;

  private static final int DELAY = 4;
  private static final int STAGES = 4;
  private static final BitWidth WIDTH = BitWidth.create(STAGES);
  private static final byte[] OUTPUT_PORTS = {2, 3, 4, 5, 10, 11, 12, 13};
  private static final String[] PORT_NAMES = {
    "2CP", "2Q3", "1Q2", "1Q1", "1Q0", "1MR", "1D",
    "1CP", "1Q3", "2Q2", "2Q1", "2Q0", "2MR", "2D"
  };
  private static final int[] REGISTER1_OUTPUTS = {
    PORT_INDEX_1Q0, PORT_INDEX_1Q1, PORT_INDEX_1Q2, PORT_INDEX_1Q3
  };
  private static final int[] REGISTER2_OUTPUTS = {
    PORT_INDEX_2Q0, PORT_INDEX_2Q1, PORT_INDEX_2Q2, PORT_INDEX_2Q3
  };

  /** Creates a 744015 dual 4-bit static shift register. */
  public Ttl744015() {
    super(_ID, (byte) 16, OUTPUT_PORTS, PORT_NAMES, new Ttl744015HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 8));
    g.drawRect(x + 25, y + 28, 10, 10);
    g.drawRect(x + 45, y + 28, 10, 10);
    g.drawRect(x + 65, y + 28, 10, 10);
    g.drawRect(x + 85, y + 28, 10, 10);
    g.drawRect(x + 125, y + 12, 10, 10);
    g.drawRect(x + 105, y + 12, 10, 10);
    g.drawRect(x + 85, y + 12, 10, 10);
    g.drawRect(x + 65, y + 12, 10, 10);
    g.drawLine(x + 30, y + height - 22, x + 30, y + height - 7);
    g.drawLine(x + 50, y + height - 22, x + 50, y + height - 7);
    g.drawLine(x + 70, y + height - 22, x + 70, y + height - 7);
    g.drawLine(x + 90, y + height - 22, x + 90, y + height - 7);
    g.drawLine(x + 130, y + 12, x + 130, y + 7);
    g.drawLine(x + 110, y + 12, x + 110, y + 7);
    g.drawLine(x + 90, y + 12, x + 90, y + 7);
    g.drawLine(x + 70, y + 12, x + 70, y + 7);
    g.drawLine(x + 10, y + height - 7, x + 10, y + height - 19);
    g.drawLine(x + 10, y + height - 19, x + 27, y + height - 19);
    g.drawLine(x + 27, y + height - 19, x + 27, y + height - 22);
    g.drawLine(x + 110, y + height - 7, x + 110, y + height - 19);
    g.drawLine(x + 110, y + height - 19, x + 47, y + height - 19);
    g.drawLine(x + 47, y + height - 19, x + 47, y + height - 22);
    g.drawLine(x + 130, y + height - 7, x + 130, y + height - 19);
    g.drawLine(x + 130, y + height - 19, x + 67, y + height - 19);
    g.drawLine(x + 67, y + height - 19, x + 67, y + height - 22);
    g.drawLine(x + 150, y + 7, x + 150, y + 25);
    g.drawLine(x + 150, y + 25, x + 87, y + 25);
    g.drawLine(x + 87, y + 25, x + 87, y + 28);
    g.drawLine(x + 50, y + 7, x + 50, y + 9);
    g.drawLine(x + 50, y + 9, x + 127, y + 9);
    g.drawLine(x + 127, y + 9, x + 127, y + 12);
    g.drawLine(x + 30, y + 7, x + 30, y + 9);
    g.drawLine(x + 30, y + 9, x + 107, y + 9);
    g.drawLine(x + 107, y + 9, x + 107, y + 12);
    GraphicsUtil.drawCenteredText(g, "3", x + 30, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "2", x + 50, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "1", x + 70, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "0", x + 90, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "3", x + 130, y + 17);
    GraphicsUtil.drawCenteredText(g, "2", x + 110, y + 17);
    GraphicsUtil.drawCenteredText(g, "1", x + 90, y + 17);
    GraphicsUtil.drawCenteredText(g, "0", x + 70, y + 17);
    g.setFont(previousFont);
  }



  private static TtlRegisterData getStateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WIDTH, 2);
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);
    final var shift1 =
        data.updateClock(state.getPortValue(PORT_INDEX_1CP), 0, StdAttr.TRIG_RISING);
    final var shift2 =
        data.updateClock(state.getPortValue(PORT_INDEX_2CP), 1, StdAttr.TRIG_RISING);
    final var next1 =
        nextRegister(
            data.getValue(0),
            state.getPortValue(PORT_INDEX_1MR),
            state.getPortValue(PORT_INDEX_1D),
            shift1);
    final var next2 =
        nextRegister(
            data.getValue(1),
            state.getPortValue(PORT_INDEX_2MR),
            state.getPortValue(PORT_INDEX_2D),
            shift2);
    data.setValue(0, next1);
    data.setValue(1, next2);
    writeRegister(state, REGISTER1_OUTPUTS, next1);
    writeRegister(state, REGISTER2_OUTPUTS, next2);
  }

  /**
   * A high reset clears the register. Otherwise a clock edge shifts {@code data} into Q0 and moves
   * the previous bits toward Q3. Any other reset level, including unknown and error, leaves the
   * register in place until a rising edge.
   */
  private static Value nextRegister(Value current, Value reset, Value data, boolean clocked) {
    if (reset == Value.TRUE) return Value.createKnown(WIDTH, 0);
    if (!clocked) return current;
    final var bits = current.getAll();
    return Value.create(new Value[] {bit(data), bits[0], bits[1], bits[2]});
  }

  private static Value bit(Value value) {
    if (value == Value.TRUE
        || value == Value.FALSE
        || value == Value.UNKNOWN
        || value == Value.ERROR) {
      return value;
    }
    return Value.ERROR;
  }

  private static void writeRegister(InstanceState state, int[] ports, Value word) {
    for (var stage = 0; stage < STAGES; stage++) {
      state.setPort(ports[stage], word.get(stage), DELAY);
    }
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_2CP, PORT_INDEX_1CP};
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
