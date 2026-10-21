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
import java.awt.Graphics;

/**
 * TTL 74x197: presettable 4-bit binary ripple counter.
 *
 * <p>Simulation follows the Motorola SN74LS197 and Fairchild 74197/74LS197 data sheets. No 74HC197
 * data sheet from Nexperia, TI, or ON was found; a pin-compatible HC part uses this function
 * table. {@code MR} low asynchronously clears every output and overrides load and both clocks.
 * Otherwise {@code PL} low copies {@code P0} to {@code P3} onto {@code Q0} to {@code Q3} and keeps
 * copying while it stays low. Otherwise {@code CP0} toggles {@code Q0} on a high-to-low edge, and
 * {@code CP1} advances {@code Q1}, {@code Q2} and {@code Q3} through 0 to 7 on a high-to-low edge.
 * {@code Q1} is the least significant bit of that section. Connecting {@code Q0} to {@code CP1}
 * counts binary 0 through 15. VCC is pin 14 and GND is pin 7. Ripple delay inside the
 * divide-by-eight section is not modeled.
 */
public class Ttl74197 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74197";

  public static final int PORT_INDEX_PL = 0;
  public static final int PORT_INDEX_Q2 = 1;
  public static final int PORT_INDEX_P2 = 2;
  public static final int PORT_INDEX_P0 = 3;
  public static final int PORT_INDEX_Q0 = 4;
  public static final int PORT_INDEX_CP1 = 5;
  public static final int PORT_INDEX_CP0 = 6;
  public static final int PORT_INDEX_Q1 = 7;
  public static final int PORT_INDEX_P1 = 8;
  public static final int PORT_INDEX_P3 = 9;
  public static final int PORT_INDEX_Q3 = 10;
  public static final int PORT_INDEX_MR = 11;

  private static final int BIT_Q0 = 0;
  private static final int BIT_Q1 = 1;
  private static final int BIT_Q2 = 2;
  private static final int BIT_Q3 = 3;
  private static final int BITS = 4;
  private static final int SECTION_MASK = 0x7;
  private static final int DELAY = 4;
  private static final int CLOCK_0 = 0;
  private static final int CLOCK_1 = 1;
  private static final BitWidth WIDTH = BitWidth.create(BITS);
  private static final byte[] OUTPUT_PORTS = {2, 5, 9, 12};
  private static final String[] PORT_NAMES = {
    "PL (Parallel load, active low)",
    "Q2",
    "P2",
    "P0",
    "Q0",
    "CP1 (Clock, divide-by-8)",
    "CP0 (Clock, divide-by-2)",
    "Q1",
    "P1",
    "P3",
    "Q3",
    "MR (Master reset, active low)"
  };

  /** Creates a 74197 presettable binary ripple counter. */
  public Ttl74197() {
    super(_ID, (byte) 14, OUTPUT_PORTS, PORT_NAMES, null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var rawState = painter.getData();
    final TtlRegisterData stored = rawState instanceof TtlRegisterData ? (TtlRegisterData) rawState : null;
    g.drawRect((x + 84), (y + height - 28), 12, 14);
    g.drawLine((x + 90), ((y + height - 28)) + 14, (x + 90), (y + height - 7));
    g.drawLine((x + 84), (((y + height - 28)) + 7) - 3, ((x + 84)) + 4, ((y + height - 28)) + 7);
    g.drawLine((x + 84), (((y + height - 28)) + 7) + 3, ((x + 84)) + 4, ((y + height - 28)) + 7);
    structureDot(g, (x + 90), ((y + height - 28)) + 7, stored, 0, 0);
    g.drawLine((x + 130), (y + 7), (x + 130), (y + 13));
    g.drawLine((x + 130), (y + 13), (x + 79), (y + 13));
    g.drawLine((x + 79), (y + 13), (x + 79), ((y + height - 28)) + 7);
    structureBubble(g, (x + 79), ((y + height - 28)) + 7);
    g.drawLine((x + 90), (y + height - 32), (x + 90), (y + height - 32));
    g.drawLine((x + 90), (y + height - 32), (x + 90), ((y + height - 28)) + 0);
    g.drawLine((x + 90), (y + height - 32), (x + 90), (y + height - 28));
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 13));
    g.drawLine((x + 30), (y + 13), (x + 90), (y + 13));
    g.drawLine((x + 90), (y + 13), (x + 90), (y + height - 32));
    structureBubble(g, (x + 30), (y + 7));
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + 30));
    structureBubble(g, (x + 10), (y + height - 7));
    GraphicsUtil.drawCenteredText(g, "LD", (x + 10), (y + 30));
    g.drawLine((x + 70), (y + height - 7), (x + 70), (y + 30));
    GraphicsUtil.drawCenteredText(g, "P0", (x + 70), (y + 30));
    g.drawRect((x + 14), (y + 11), 12, 12);
    g.drawLine((x + 110), (y + 7), (x + 110), (y + 13));
    g.drawLine((x + 110), (y + 13), (x + 20), (y + 13));
    g.drawLine((x + 20), (y + 13), (x + 20), (y + 11));
    g.drawLine((x + 14), (((y + 11)) + 6) - 3, ((x + 14)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 14), (((y + 11)) + 6) + 3, ((x + 14)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 51), (y + 11), 12, 12);
    g.drawLine((x + 30), (y + height - 7), (x + 30), (y + height - 13));
    g.drawLine((x + 30), (y + height - 13), (x + 57), (y + height - 13));
    g.drawLine((x + 57), (y + height - 13), (x + 57), ((y + 11)) + 12);
    g.drawLine((x + 51), (((y + 11)) + 6) - 3, ((x + 51)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 51), (((y + 11)) + 6) + 3, ((x + 51)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 88), (y + 11), 12, 12);
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 13));
    g.drawLine((x + 50), (y + 13), (x + 94), (y + 13));
    g.drawLine((x + 94), (y + 13), (x + 94), (y + 11));
    g.drawLine((x + 88), (((y + 11)) + 6) - 3, ((x + 88)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 88), (((y + 11)) + 6) + 3, ((x + 88)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 26), ((y + 11)) + 6, (x + 47), ((y + 11)) + 6);
    g.drawLine((x + 63), ((y + 11)) + 6, (x + 84), ((y + 11)) + 6);
    g.drawLine((x + 110), (y + height - 7), (x + 110), (y + height - 13));
    g.drawLine((x + 110), (y + height - 13), (x + 9), (y + height - 13));
    g.drawLine((x + 9), (y + height - 13), (x + 9), ((y + 11)) + 6);
    structureBubble(g, (x + 9), ((y + 11)) + 6);
    g.drawLine((x + 20), (y + 27), (x + 94), (y + 27));
    g.drawLine((x + 20), (y + 27), (x + 20), (y + 11));
    g.drawLine((x + 20), (y + 27), (x + 20), ((y + 11)) + 14);
    g.drawLine((x + 57), (y + 27), (x + 57), (y + 11));
    g.drawLine((x + 57), (y + 27), (x + 57), ((y + 11)) + 14);
    g.drawLine((x + 94), (y + 27), (x + 94), (y + 11));
    g.drawLine((x + 94), (y + 27), (x + 94), ((y + 11)) + 14);
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 13));
    g.drawLine((x + 30), (y + 13), (x + 20), (y + 13));
    g.drawLine((x + 20), (y + 13), (x + 20), (y + 27));
    structureBubble(g, (x + 30), (y + 7));
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + 30));
    structureBubble(g, (x + 10), (y + height - 7));
    GraphicsUtil.drawCenteredText(g, "LD", (x + 10), (y + 30));
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
    final var clock0 =
        data.updateClock(state.getPortValue(PORT_INDEX_CP0), CLOCK_0, StdAttr.TRIG_FALLING);
    final var clock1 =
        data.updateClock(state.getPortValue(PORT_INDEX_CP1), CLOCK_1, StdAttr.TRIG_FALLING);
    var value = data.getValue();
    if (state.getPortValue(PORT_INDEX_MR) == Value.FALSE) {
      value = Value.createKnown(WIDTH, 0);
    } else if (state.getPortValue(PORT_INDEX_PL) == Value.FALSE) {
      value = parallelLoad(state);
    } else {
      if (clock0) value = toggleQ0(value);
      if (clock1) value = advanceDivideByEight(value);
    }
    data.setValue(value);
    state.setPort(PORT_INDEX_Q0, value.get(BIT_Q0), DELAY);
    state.setPort(PORT_INDEX_Q1, value.get(BIT_Q1), DELAY);
    state.setPort(PORT_INDEX_Q2, value.get(BIT_Q2), DELAY);
    state.setPort(PORT_INDEX_Q3, value.get(BIT_Q3), DELAY);
  }

  private static Value parallelLoad(InstanceState state) {
    return Value.create(
        new Value[] {
          state.getPortValue(PORT_INDEX_P0),
          state.getPortValue(PORT_INDEX_P1),
          state.getPortValue(PORT_INDEX_P2),
          state.getPortValue(PORT_INDEX_P3)
        });
  }

  private static Value toggleQ0(Value value) {
    return value.set(BIT_Q0, toggled(value.get(BIT_Q0)));
  }

  private static Value toggled(Value bit) {
    if (bit == Value.TRUE) return Value.FALSE;
    if (bit == Value.FALSE) return Value.TRUE;
    if (bit == Value.ERROR) return Value.ERROR;
    return Value.UNKNOWN;
  }

  /**
   * Advances {@code Q1}, {@code Q2} and {@code Q3}. An error in the section makes the next section
   * entirely error. A section that is not fully defined becomes unknown. A defined code increments
   * modulo 8.
   */
  private static Value advanceDivideByEight(Value value) {
    if (sectionContains(value, Value.ERROR)) return setSection(value, Value.ERROR);
    if (!sectionKnown(value)) return setSection(value, Value.UNKNOWN);
    final var next = (sectionCode(value) + 1) & SECTION_MASK;
    return value
        .set(BIT_Q1, bit(next, 0))
        .set(BIT_Q2, bit(next, 1))
        .set(BIT_Q3, bit(next, 2));
  }

  private static boolean sectionContains(Value value, Value bit) {
    return value.get(BIT_Q1) == bit || value.get(BIT_Q2) == bit || value.get(BIT_Q3) == bit;
  }

  private static boolean sectionKnown(Value value) {
    return isKnown(value.get(BIT_Q1)) && isKnown(value.get(BIT_Q2)) && isKnown(value.get(BIT_Q3));
  }

  private static boolean isKnown(Value bit) {
    return bit == Value.TRUE || bit == Value.FALSE;
  }

  private static int sectionCode(Value value) {
    return (value.get(BIT_Q3) == Value.TRUE ? 4 : 0)
        + (value.get(BIT_Q2) == Value.TRUE ? 2 : 0)
        + (value.get(BIT_Q1) == Value.TRUE ? 1 : 0);
  }

  private static Value setSection(Value value, Value bit) {
    return value.set(BIT_Q1, bit).set(BIT_Q2, bit).set(BIT_Q3, bit);
  }

  private static Value bit(int code, int shift) {
    return ((code >> shift) & 1) == 1 ? Value.TRUE : Value.FALSE;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_CP1, PORT_INDEX_CP0};
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
