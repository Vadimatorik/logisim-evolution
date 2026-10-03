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
 * TTL 74x90: decade ripple counter.
 *
 * <p>Simulation follows the
 * <a href="https://inno-ic.net/pd/1/27.pdf">IN74HC90</a> function table, which matches the
 * Fairchild DM74LS90 table. {@code CKA} toggles {@code QA} on a high-to-low edge. {@code CKB}
 * advances {@code QB}, {@code QC} and {@code QD} through 0, 1, 2, 3 and 4 on a high-to-low edge.
 * {@code QB} is the least significant bit of that section. Connecting {@code QA} to {@code CKB}
 * counts BCD 0 through 9. Connecting {@code QD} to {@code CKA} produces the bi-quinary sequence.
 * Both {@code R9} inputs high asynchronously force {@code QD QC QB QA} to 1001 and override the
 * clocks and the reset. Otherwise both {@code R0} inputs high clear every output. A single input
 * of either pair does nothing. Codes 5, 6 and 7 of the divide-by-five section are outside the
 * published table; the next {@code CKB} edge returns that section to 0. Ripple delay is not
 * modeled.
 */
public class Ttl7490 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7490";

  public static final int PORT_INDEX_CKB = 0;
  public static final int PORT_INDEX_R0_1 = 1;
  public static final int PORT_INDEX_R0_2 = 2;
  public static final int PORT_INDEX_R9_1 = 3;
  public static final int PORT_INDEX_R9_2 = 4;
  public static final int PORT_INDEX_QC = 5;
  public static final int PORT_INDEX_QB = 6;
  public static final int PORT_INDEX_QD = 7;
  public static final int PORT_INDEX_QA = 8;
  public static final int PORT_INDEX_CKA = 9;

  private static final int BIT_QA = 0;
  private static final int BIT_QB = 1;
  private static final int BIT_QC = 2;
  private static final int BIT_QD = 3;
  private static final int BITS = 4;
  private static final int SET_TO_NINE = 9;
  private static final int DIVIDE_BY_FIVE_TERMINAL = 4;
  private static final int DELAY = 4;
  private static final int CLOCK_A = 0;
  private static final int CLOCK_B = 1;
  private static final BitWidth WIDTH = BitWidth.create(BITS);
  private static final byte[] OUTPUT_PORTS = {8, 9, 11, 12};
  private static final byte[] UNUSED_PINS = {4, 13};
  private static final String[] PORT_NAMES = {
    "CKB (Clock B)",
    "R0(1) (Reset, active HIGH)",
    "R0(2) (Reset, active HIGH)",
    "R9(1) (Set to 9, active HIGH)",
    "R9(2) (Set to 9, active HIGH)",
    "QC",
    "QB",
    "QD",
    "QA",
    "CKA (Clock A)"
  };

  /** Creates a 7490 decade ripple counter. */
  public Ttl7490() {
    super(
        _ID,
        (byte) 14,
        OUTPUT_PORTS,
        UNUSED_PINS,
        PORT_NAMES,
        (byte) 5,
        (byte) 10,
        new Ttl7490HdlGenerator());
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
          "CKB", "R0(1)", "R0(2)", null, null, "R9(1)", "R9(2)",
          "QC", "QB", null, "QD", "QA", null, "CKA"
        });
    drawState(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private void drawState(Graphics2D gfx, int x, int y, int height, TtlRegisterData state) {
    if (state == null) return;
    for (var i = 0; i < BITS; i++) {
      final var bit = state.getValue().get(BIT_QD - i);
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
    final var clockA =
        data.updateClock(state.getPortValue(PORT_INDEX_CKA), CLOCK_A, StdAttr.TRIG_FALLING);
    final var clockB =
        data.updateClock(state.getPortValue(PORT_INDEX_CKB), CLOCK_B, StdAttr.TRIG_FALLING);
    var value = data.getValue();
    if (bothHigh(state, PORT_INDEX_R9_1, PORT_INDEX_R9_2)) {
      value = Value.createKnown(WIDTH, SET_TO_NINE);
    } else if (bothHigh(state, PORT_INDEX_R0_1, PORT_INDEX_R0_2)) {
      value = Value.createKnown(WIDTH, 0);
    } else {
      if (clockA) value = toggleQa(value);
      if (clockB) value = advanceDivideByFive(value);
    }
    data.setValue(value);
    state.setPort(PORT_INDEX_QA, value.get(BIT_QA), DELAY);
    state.setPort(PORT_INDEX_QB, value.get(BIT_QB), DELAY);
    state.setPort(PORT_INDEX_QC, value.get(BIT_QC), DELAY);
    state.setPort(PORT_INDEX_QD, value.get(BIT_QD), DELAY);
  }

  private static boolean bothHigh(InstanceState state, int firstPort, int secondPort) {
    return state.getPortValue(firstPort) == Value.TRUE
        && state.getPortValue(secondPort) == Value.TRUE;
  }

  private static Value toggleQa(Value value) {
    return value.set(BIT_QA, toggled(value.get(BIT_QA)));
  }

  private static Value toggled(Value bit) {
    if (bit == Value.TRUE) return Value.FALSE;
    if (bit == Value.FALSE) return Value.TRUE;
    if (bit == Value.ERROR) return Value.ERROR;
    return Value.UNKNOWN;
  }

  /**
   * Advances {@code QB}, {@code QC} and {@code QD}. An error in the section makes the next section
   * entirely error. A section that is not fully defined becomes unknown. A defined code below 4
   * increments, and every other defined code returns to 0.
   */
  private static Value advanceDivideByFive(Value value) {
    if (sectionContains(value, Value.ERROR)) return setDivideByFive(value, Value.ERROR);
    if (!divideByFiveKnown(value)) return setDivideByFive(value, Value.UNKNOWN);
    final var code = divideByFiveCode(value);
    final var next = code >= DIVIDE_BY_FIVE_TERMINAL ? 0 : code + 1;
    return value
        .set(BIT_QB, bit(next, 0))
        .set(BIT_QC, bit(next, 1))
        .set(BIT_QD, bit(next, 2));
  }

  private static boolean sectionContains(Value value, Value bit) {
    return value.get(BIT_QB) == bit || value.get(BIT_QC) == bit || value.get(BIT_QD) == bit;
  }

  private static boolean divideByFiveKnown(Value value) {
    return isKnown(value.get(BIT_QB)) && isKnown(value.get(BIT_QC)) && isKnown(value.get(BIT_QD));
  }

  private static boolean isKnown(Value bit) {
    return bit == Value.TRUE || bit == Value.FALSE;
  }

  private static int divideByFiveCode(Value value) {
    return (value.get(BIT_QD) == Value.TRUE ? 4 : 0)
        + (value.get(BIT_QC) == Value.TRUE ? 2 : 0)
        + (value.get(BIT_QB) == Value.TRUE ? 1 : 0);
  }

  private static Value setDivideByFive(Value value, Value bit) {
    return value.set(BIT_QB, bit).set(BIT_QC, bit).set(BIT_QD, bit);
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
    return new int[] {PORT_INDEX_CKB, PORT_INDEX_CKA};
  }
}
