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
 * TTL 74x490: dual 4-bit decade counter.
 *
 * <p>Simulation follows the Hitachi/Renesas HD74HC490 (ADE-205-507). Each half is an independent
 * BCD decade counter. The count advances on the high-to-low transition of its clock. {@code CLR}
 * high asynchronously clears that half. {@code SET9} high asynchronously forces BCD 9 ({@code QD
 * QC QB QA} = 1001). The HD74HC490 function table does not list both controls high; the
 * pin-compatible SN74LS490 gives set-to-9 priority over clear, matching the 7490 reset-to-9 gates,
 * so both high produces 9. An unknown control is not active. An error on either control makes that
 * half's outputs an error. Codes 10 to 15 are outside the specified sequence and return to 0 on the
 * next falling edge.
 */
public class Ttl74490 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74490";

  public static final int PORT_INDEX_1CLK = 0;
  public static final int PORT_INDEX_1CLR = 1;
  public static final int PORT_INDEX_1QA = 2;
  public static final int PORT_INDEX_1SET9 = 3;
  public static final int PORT_INDEX_1QB = 4;
  public static final int PORT_INDEX_1QC = 5;
  public static final int PORT_INDEX_1QD = 6;
  public static final int PORT_INDEX_2QD = 7;
  public static final int PORT_INDEX_2QC = 8;
  public static final int PORT_INDEX_2QB = 9;
  public static final int PORT_INDEX_2SET9 = 10;
  public static final int PORT_INDEX_2QA = 11;
  public static final int PORT_INDEX_2CLR = 12;
  public static final int PORT_INDEX_2CLK = 13;

  private static final int DELAY = 4;
  private static final int SET_TO_9 = 9;
  private static final BitWidth NIBBLE = BitWidth.create(4);
  private static final byte[] OUTPUT_PORTS = {3, 5, 6, 7, 9, 10, 11, 13};
  private static final String[] PORT_NAMES = {
    "1CLK (clock, falling edge)",
    "1CLR (clear, active high)",
    "1QA",
    "1SET9 (set to 9, active high)",
    "1QB",
    "1QC",
    "1QD",
    "2QD",
    "2QC",
    "2QB",
    "2SET9 (set to 9, active high)",
    "2QA",
    "2CLR (clear, active high)",
    "2CLK (clock, falling edge)"
  };

  /** Creates a 74490 dual 4-bit decade counter. */
  public Ttl74490() {
    super(_ID, (byte) 16, OUTPUT_PORTS, PORT_NAMES, new Ttl74490HdlGenerator());
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
          "1CK", "1CLR", "1QA", "1S9", "1QB", "1QC", "1QD", null,
          "2QD", "2QC", "2QB", "2S9", "2QA", "2CLR", "2CK", null
        });
    final var state = (TtlRegisterData) painter.getData();
    if (state == null) {
      return;
    }
    drawNibble(gfx, x + 24, y, height, state.getValue(0));
    drawNibble(gfx, x + 96, y, height, state.getValue(1));
    gfx.setColor(Color.BLACK);
  }

  private static void drawNibble(Graphics2D gfx, int x, int y, int height, Value nibble) {
    for (var i = 0; i < 4; i++) {
      final var bit = nibble.get(3 - i);
      final var originX = x + i * 10;
      gfx.setColor(bit.getColor());
      gfx.fillOval(originX, y + height / 2 - 4, 8, 8);
      gfx.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(gfx, bit.toDisplayString(), originX + 4, y + height / 2);
    }
  }

  private static TtlRegisterData getStateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(NIBBLE, 2);
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);
    propagateHalf(
        state,
        data,
        0,
        PORT_INDEX_1CLK,
        PORT_INDEX_1CLR,
        PORT_INDEX_1SET9,
        PORT_INDEX_1QA,
        PORT_INDEX_1QB,
        PORT_INDEX_1QC,
        PORT_INDEX_1QD);
    propagateHalf(
        state,
        data,
        1,
        PORT_INDEX_2CLK,
        PORT_INDEX_2CLR,
        PORT_INDEX_2SET9,
        PORT_INDEX_2QA,
        PORT_INDEX_2QB,
        PORT_INDEX_2QC,
        PORT_INDEX_2QD);
  }

  private static void propagateHalf(
      InstanceState state,
      TtlRegisterData data,
      int which,
      int clockPort,
      int clearPort,
      int setPort,
      int qa,
      int qb,
      int qc,
      int qd) {
    final var clocked =
        data.updateClock(state.getPortValue(clockPort), which, StdAttr.TRIG_FALLING);
    final var set = state.getPortValue(setPort);
    final var clear = state.getPortValue(clearPort);
    if (set == Value.ERROR || clear == Value.ERROR) {
      drive(state, Value.createError(NIBBLE), qa, qb, qc, qd);
      return;
    }
    final Value next;
    if (set == Value.TRUE) {
      next = Value.createKnown(NIBBLE, SET_TO_9);
    } else if (clear == Value.TRUE) {
      next = Value.createKnown(NIBBLE, 0);
    } else if (clocked) {
      next = nextCount(data.getValue(which));
    } else {
      next = data.getValue(which);
    }
    data.setValue(which, next);
    drive(state, next, qa, qb, qc, qd);
  }

  private static void drive(InstanceState state, Value value, int qa, int qb, int qc, int qd) {
    state.setPort(qa, value.get(0), DELAY);
    state.setPort(qb, value.get(1), DELAY);
    state.setPort(qc, value.get(2), DELAY);
    state.setPort(qd, value.get(3), DELAY);
  }

  /**
   * Advances one decade count. An error anywhere makes the next code entirely error. A code that is
   * only unknown stays unknown. Codes 0 to 8 increment. Code 9 and the unspecified codes 10 to 15
   * return to 0.
   */
  private static Value nextCount(Value current) {
    if (containsBit(current, Value.ERROR)) {
      return Value.createError(NIBBLE);
    }
    if (!current.isFullyDefined()) {
      return Value.createUnknown(NIBBLE);
    }
    final var code = current.toLongValue();
    return Value.createKnown(NIBBLE, code >= SET_TO_9 ? 0 : code + 1);
  }

  private static boolean containsBit(Value word, Value bit) {
    for (var i = 0; i < word.getWidth(); i++) {
      if (word.get(i) == bit) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_1CLK, PORT_INDEX_2CLK};
  }
}
