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
 * TTL 7492: divide-by-twelve counter.
 *
 * <p>Simulation follows the ON Semiconductor SN74LS92 function table in
 * <a href="https://www.futurlec.com/Datasheet/74ls/74LS92.pdf">SN54/74LS90, LS92 and LS93</a>
 * (the same table as the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74ls92.pdf">TI SN74LS92</a> data sheet). {@code CKA}
 * ({@code CP0}, pin 14) clocks the divide-by-two stage {@code QA} ({@code Q0}, pin 12). {@code CKB}
 * ({@code CP1}, pin 1) clocks the divide-by-six stage {@code QB}/{@code QC}/{@code QD} ({@code Q1}
 * pin 11, {@code Q2} pin 9, {@code Q3} pin 8). Both clocks respond to the high-to-low edge. The
 * stages are not connected inside the package: a divide-by-twelve count ties {@code QA} to {@code
 * CKB} outside the chip. {@code R0(1)} and {@code R0(2)} ({@code MR1} pin 6 and {@code MR2} pin 7)
 * asynchronously clear every stage when both are high.
 */
public class Ttl7492 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7492";

  public static final int PORT_INDEX_CKB = 0;
  public static final int PORT_INDEX_R0_1 = 1;
  public static final int PORT_INDEX_R0_2 = 2;
  public static final int PORT_INDEX_QD = 3;
  public static final int PORT_INDEX_QC = 4;
  public static final int PORT_INDEX_QB = 5;
  public static final int PORT_INDEX_QA = 6;
  public static final int PORT_INDEX_CKA = 7;

  private static final int BIT_QA = 0;
  private static final int BIT_QB = 1;
  private static final int BIT_QC = 2;
  private static final int BIT_QD = 3;
  private static final int DELAY = 4;
  private static final byte[] OUTPUT_PORTS = {8, 9, 11, 12};
  private static final byte[] UNUSED_PINS = {2, 3, 4, 13};
  private static final String[] PORT_NAMES = {
    "CKB (Clock B)",
    "R0(1) (Reset, active HIGH)",
    "R0(2) (Reset, active HIGH)",
    "QD",
    "QC",
    "QB",
    "QA",
    "CKA (Clock A)"
  };
  private static final BitWidth WIDTH = BitWidth.create(4);

  public Ttl7492() {
    super(
        _ID,
        (byte) 14,
        OUTPUT_PORTS,
        UNUSED_PINS,
        PORT_NAMES,
        (byte) 5,
        (byte) 10,
        null);
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
          "CKB", null, null, null, null, "R0(1)", "R0(2)",
          "QD", "QC", null, "QB", "QA", null, "CKA"
        });
    drawState(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private void drawState(Graphics2D gfx, int x, int y, int height, TtlRegisterData state) {
    if (state == null) return;
    for (var i = 0; i < 4; i++) {
      final var bit = state.getValue().get(3 - i);
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
    final var clockATriggered =
        data.updateClock(state.getPortValue(PORT_INDEX_CKA), 0, StdAttr.TRIG_FALLING);
    final var clockBTriggered =
        data.updateClock(state.getPortValue(PORT_INDEX_CKB), 1, StdAttr.TRIG_FALLING);
    final var values = data.getValue().getAll();

    if (state.getPortValue(PORT_INDEX_R0_1) == Value.TRUE
        && state.getPortValue(PORT_INDEX_R0_2) == Value.TRUE) {
      data.setValue(Value.createKnown(WIDTH, 0));
    } else {
      if (clockATriggered) values[BIT_QA] = invert(values[BIT_QA]);
      if (clockBTriggered) advanceDivideBySix(values);
      data.setValue(Value.create(values));
    }

    state.setPort(PORT_INDEX_QA, data.getValue().get(BIT_QA), DELAY);
    state.setPort(PORT_INDEX_QB, data.getValue().get(BIT_QB), DELAY);
    state.setPort(PORT_INDEX_QC, data.getValue().get(BIT_QC), DELAY);
    state.setPort(PORT_INDEX_QD, data.getValue().get(BIT_QD), DELAY);
  }

  /**
   * Advances {@code QB}, {@code QC} and {@code QD}. {@code QB} and {@code QC} are clocked together
   * by {@code CKB}. {@code QD} toggles when {@code QC} falls.
   */
  private static void advanceDivideBySix(Value[] values) {
    final var q1 = values[BIT_QB];
    final var q2 = values[BIT_QC];
    final var q1Next = jkWithKHigh(invert(q2), q1);
    final var q2Next = jkWithKHigh(q1, q2);
    values[BIT_QB] = q1Next;
    values[BIT_QC] = q2Next;
    values[BIT_QD] = nextQd(q2, q2Next, values[BIT_QD]);
  }

  /** Next state of a JK flip-flop whose K input is tied high. */
  private static Value jkWithKHigh(Value j, Value q) {
    if (isConflict(j) || isConflict(q)) return Value.ERROR;
    if (j == Value.FALSE) return Value.FALSE;
    if (j == Value.TRUE) return invert(q);
    if (q == Value.TRUE) return Value.FALSE;
    return Value.UNKNOWN;
  }

  /** {@code QD} toggles only on a known falling edge of {@code QC}. */
  private static Value nextQd(Value oldQc, Value newQc, Value qd) {
    if (oldQc == Value.FALSE || (oldQc == Value.TRUE && newQc == Value.TRUE)) return qd;
    if (oldQc == Value.TRUE && newQc == Value.FALSE) return invert(qd);
    if (isConflict(oldQc) || isConflict(newQc) || isConflict(qd)) return Value.ERROR;
    return Value.UNKNOWN;
  }

  private static Value invert(Value bit) {
    if (bit == Value.TRUE) return Value.FALSE;
    if (bit == Value.FALSE) return Value.TRUE;
    if (isConflict(bit)) return Value.ERROR;
    return Value.UNKNOWN;
  }

  private static boolean isConflict(Value bit) {
    return bit != Value.TRUE && bit != Value.FALSE && bit != Value.UNKNOWN;
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
