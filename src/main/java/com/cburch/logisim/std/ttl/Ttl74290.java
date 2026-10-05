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
 * TTL 74x290: decade ripple counter.
 *
 * <p>Simulation follows the Philips/NXP 74HC/HCT290 product specification. {@code CP0} toggles
 * {@code Q0} on a high-to-low edge. {@code CP1} advances {@code Q1}, {@code Q2} and {@code Q3}
 * through 0, 1, 2, 3 and 4 on a high-to-low edge. {@code Q1} is the least significant bit of that
 * section. Connecting {@code Q0} to {@code CP1} counts BCD 0 through 9. Connecting {@code Q3} to
 * {@code CP0} produces the bi-quinary sequence. Both {@code MS} inputs high asynchronously force
 * {@code Q3 Q2 Q1 Q0} to 1001 and override the clocks and the reset. Otherwise both {@code MR}
 * inputs high clear every output. A single input of either pair does nothing. Codes 5, 6 and 7 of
 * the divide-by-five section are outside the published table; the next {@code CP1} edge returns
 * that section to 0. VCC is pin 5 and GND is pin 10. The corner-power SN74LS290, with VCC on pin
 * 14 and GND on pin 7, is a different package and is not this model. Ripple delay is not modeled.
 */
public class Ttl74290 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74290";

  public static final int PORT_INDEX_CP1 = 0;
  public static final int PORT_INDEX_MR1 = 1;
  public static final int PORT_INDEX_MR2 = 2;
  public static final int PORT_INDEX_MS1 = 3;
  public static final int PORT_INDEX_MS2 = 4;
  public static final int PORT_INDEX_Q2 = 5;
  public static final int PORT_INDEX_Q1 = 6;
  public static final int PORT_INDEX_Q3 = 7;
  public static final int PORT_INDEX_Q0 = 8;
  public static final int PORT_INDEX_CP0 = 9;

  private static final int BIT_Q0 = 0;
  private static final int BIT_Q1 = 1;
  private static final int BIT_Q2 = 2;
  private static final int BIT_Q3 = 3;
  private static final int BITS = 4;
  private static final int SET_TO_NINE = 9;
  private static final int DIVIDE_BY_FIVE_TERMINAL = 4;
  private static final int DELAY = 4;
  private static final int CLOCK_0 = 0;
  private static final int CLOCK_1 = 1;
  private static final BitWidth WIDTH = BitWidth.create(BITS);
  private static final byte[] OUTPUT_PORTS = {8, 9, 11, 12};
  private static final byte[] UNUSED_PINS = {4, 13};
  private static final String[] PORT_NAMES = {
    "CP1 (Clock, divide-by-5)",
    "MR1 (Master reset, active HIGH)",
    "MR2 (Master reset, active HIGH)",
    "MS1 (Master set to 9, active HIGH)",
    "MS2 (Master set to 9, active HIGH)",
    "Q2",
    "Q1",
    "Q3",
    "Q0",
    "CP0 (Clock, divide-by-2)"
  };

  /** Creates a 74290 decade ripple counter. */
  public Ttl74290() {
    super(
        _ID,
        (byte) 14,
        OUTPUT_PORTS,
        UNUSED_PINS,
        PORT_NAMES,
        (byte) 5,
        (byte) 10,
        new Ttl74290HdlGenerator());
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
          "CP1", "MR1", "MR2", null, null, "MS1", "MS2",
          "Q2", "Q1", null, "Q3", "Q0", null, "CP0"
        });
    drawState(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private void drawState(Graphics2D gfx, int x, int y, int height, TtlRegisterData state) {
    if (state == null) return;
    for (var i = 0; i < BITS; i++) {
      final var bit = state.getValue().get(BIT_Q3 - i);
      gfx.setColor(bit.getColor());
      gfx.fillOval(x + 52 + i * 10, y + height / 2 - 4, 8, 8);
      gfx.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(gfx, bit.toDisplayString(), x + 56 + i * 10, y + height / 2);
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
    final var clock0 =
        data.updateClock(state.getPortValue(PORT_INDEX_CP0), CLOCK_0, StdAttr.TRIG_FALLING);
    final var clock1 =
        data.updateClock(state.getPortValue(PORT_INDEX_CP1), CLOCK_1, StdAttr.TRIG_FALLING);
    var value = data.getValue();
    if (bothHigh(state, PORT_INDEX_MS1, PORT_INDEX_MS2)) {
      value = Value.createKnown(WIDTH, SET_TO_NINE);
    } else if (bothHigh(state, PORT_INDEX_MR1, PORT_INDEX_MR2)) {
      value = Value.createKnown(WIDTH, 0);
    } else {
      if (clock0) value = toggleQ0(value);
      if (clock1) value = advanceDivideByFive(value);
    }
    data.setValue(value);
    state.setPort(PORT_INDEX_Q0, value.get(BIT_Q0), DELAY);
    state.setPort(PORT_INDEX_Q1, value.get(BIT_Q1), DELAY);
    state.setPort(PORT_INDEX_Q2, value.get(BIT_Q2), DELAY);
    state.setPort(PORT_INDEX_Q3, value.get(BIT_Q3), DELAY);
  }

  private static boolean bothHigh(InstanceState state, int firstPort, int secondPort) {
    return state.getPortValue(firstPort) == Value.TRUE
        && state.getPortValue(secondPort) == Value.TRUE;
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
   * entirely error. A section that is not fully defined becomes unknown. A defined code below 4
   * increments, and every other defined code returns to 0.
   */
  private static Value advanceDivideByFive(Value value) {
    if (sectionContains(value, Value.ERROR)) return setDivideByFive(value, Value.ERROR);
    if (!divideByFiveKnown(value)) return setDivideByFive(value, Value.UNKNOWN);
    final var code = divideByFiveCode(value);
    final var next = code >= DIVIDE_BY_FIVE_TERMINAL ? 0 : code + 1;
    return value
        .set(BIT_Q1, bit(next, 0))
        .set(BIT_Q2, bit(next, 1))
        .set(BIT_Q3, bit(next, 2));
  }

  private static boolean sectionContains(Value value, Value bit) {
    return value.get(BIT_Q1) == bit || value.get(BIT_Q2) == bit || value.get(BIT_Q3) == bit;
  }

  private static boolean divideByFiveKnown(Value value) {
    return isKnown(value.get(BIT_Q1)) && isKnown(value.get(BIT_Q2)) && isKnown(value.get(BIT_Q3));
  }

  private static boolean isKnown(Value bit) {
    return bit == Value.TRUE || bit == Value.FALSE;
  }

  private static int divideByFiveCode(Value value) {
    return (value.get(BIT_Q3) == Value.TRUE ? 4 : 0)
        + (value.get(BIT_Q2) == Value.TRUE ? 2 : 0)
        + (value.get(BIT_Q1) == Value.TRUE ? 1 : 0);
  }

  private static Value setDivideByFive(Value value, Value bit) {
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
}
