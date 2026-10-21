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
 * TTL 74x293: 4-bit binary ripple counter.
 *
 * <p>The digital function follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74ls293.pdf">SN74LS293</a> and
 * <a href="https://cdn-reichelt.de/documents/datenblatt/A200/HD74LS293.pdf">HD74LS293</a> data
 * sheets.
 * Those parts are electrically and functionally the same counter as the 7493; only the terminals
 * are rearranged, with GND on pin 7 and VCC on pin 14. A high-to-low edge on {@code A} toggles
 * {@code QA}. A high-to-low edge on {@code B} advances {@code QB}, {@code QC} and {@code QD}.
 * {@code QA} is not tied to {@code B} inside the package. Both {@code R0(1)} and {@code R0(2)}
 * high asynchronously clear every stage and override the clocks. One reset input does not.
 * Pins 1, 2, 3 and 6 are not connected. This is not an electrical model of a 74HC293.
 */
public class Ttl74293 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74293";

  public static final int PORT_INDEX_QC = 0;
  public static final int PORT_INDEX_QB = 1;
  public static final int PORT_INDEX_QD = 2;
  public static final int PORT_INDEX_QA = 3;
  public static final int PORT_INDEX_A = 4;
  public static final int PORT_INDEX_B = 5;
  public static final int PORT_INDEX_R0_1 = 6;
  public static final int PORT_INDEX_R0_2 = 7;

  private static final byte[] OUTPUT_PORTS = {4, 5, 8, 9};
  private static final byte[] UNUSED_PINS = {1, 2, 3, 6};
  private static final String[] PORT_NAMES = {
    "QC",
    "QB",
    "QD",
    "QA",
    "A (Clock A, falling edge)",
    "B (Clock B, falling edge)",
    "R0(1) (Reset, active high)",
    "R0(2) (Reset, active high)"
  };
  private static final BitWidth WIDTH = BitWidth.create(4);

  /** Creates the 74293. Power stays on the conventional pins 14 and 7. */
  public Ttl74293() {
    super(_ID, (byte) 14, OUTPUT_PORTS, UNUSED_PINS, PORT_NAMES, new Ttl74293HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var rawState = painter.getData();
    final TtlRegisterData stored = rawState instanceof TtlRegisterData ? (TtlRegisterData) rawState : null;
    g.drawRect((x + 104), (y + 11), 12, 14);
    g.drawLine((x + 110), (y + 11), (x + 110), (y + 7));
    g.drawLine((x + 104), (((y + 11)) + 7) - 3, ((x + 104)) + 4, ((y + 11)) + 7);
    g.drawLine((x + 104), (((y + 11)) + 7) + 3, ((x + 104)) + 4, ((y + 11)) + 7);
    structureDot(g, (x + 110), ((y + 11)) + 7, stored, 0, 0);
    g.drawLine((x + 90), (y + 7), (x + 90), (y + 13));
    g.drawLine((x + 90), (y + 13), (x + 99), (y + 13));
    g.drawLine((x + 99), (y + 13), (x + 99), ((y + 11)) + 7);
    structureBubble(g, (x + 99), ((y + 11)) + 7);
    g.drawLine((x + 110), (y + 27), (x + 110), (y + 27));
    g.drawLine((x + 110), (y + 27), (x + 110), (y + 11));
    g.drawLine((x + 110), (y + 27), (x + 110), ((y + 11)) + 14);
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 13));
    g.drawLine((x + 50), (y + 13), (x + 110), (y + 13));
    g.drawLine((x + 110), (y + 13), (x + 110), (y + 27));
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 13));
    g.drawLine((x + 30), (y + 13), (x + 110), (y + 13));
    g.drawLine((x + 110), (y + 13), (x + 110), (y + 27));
    g.drawRect((x + 14), (y + height - 28), 12, 12);
    g.drawLine((x + 90), (y + height - 7), (x + 90), (y + height - 13));
    g.drawLine((x + 90), (y + height - 13), (x + 20), (y + height - 13));
    g.drawLine((x + 20), (y + height - 13), (x + 20), ((y + height - 28)) + 12);
    g.drawLine((x + 14), (((y + height - 28)) + 6) - 3, ((x + 14)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 14), (((y + height - 28)) + 6) + 3, ((x + 14)) + 4, ((y + height - 28)) + 6);
    structureDot(g, (x + 20), ((y + height - 28)) + 6, stored, 0, 1);
    g.drawRect((x + 51), (y + height - 28), 12, 12);
    g.drawLine((x + 70), (y + height - 7), (x + 70), (y + height - 13));
    g.drawLine((x + 70), (y + height - 13), (x + 57), (y + height - 13));
    g.drawLine((x + 57), (y + height - 13), (x + 57), ((y + height - 28)) + 12);
    g.drawLine((x + 51), (((y + height - 28)) + 6) - 3, ((x + 51)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 51), (((y + height - 28)) + 6) + 3, ((x + 51)) + 4, ((y + height - 28)) + 6);
    structureDot(g, (x + 57), ((y + height - 28)) + 6, stored, 0, 2);
    g.drawRect((x + 88), (y + height - 28), 12, 12);
    g.drawLine((x + 130), (y + 7), (x + 130), (y + 13));
    g.drawLine((x + 130), (y + 13), (x + 94), (y + 13));
    g.drawLine((x + 94), (y + 13), (x + 94), (y + height - 28));
    g.drawLine((x + 88), (((y + height - 28)) + 6) - 3, ((x + 88)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 88), (((y + height - 28)) + 6) + 3, ((x + 88)) + 4, ((y + height - 28)) + 6);
    structureDot(g, (x + 94), ((y + height - 28)) + 6, stored, 0, 3);
    g.drawLine((x + 26), ((y + height - 28)) + 6, (x + 47), ((y + height - 28)) + 6);
    g.drawLine((x + 63), ((y + height - 28)) + 6, (x + 84), ((y + height - 28)) + 6);
    g.drawLine((x + 70), (y + 7), (x + 70), (y + 13));
    g.drawLine((x + 70), (y + 13), (x + 9), (y + 13));
    g.drawLine((x + 9), (y + 13), (x + 9), ((y + height - 28)) + 6);
    structureBubble(g, (x + 9), ((y + height - 28)) + 6);
    g.drawLine((x + 20), (y + height - 32), (x + 94), (y + height - 32));
    g.drawLine((x + 20), (y + height - 32), (x + 20), ((y + height - 28)) + 0);
    g.drawLine((x + 20), (y + height - 32), (x + 20), (y + height - 28));
    g.drawLine((x + 57), (y + height - 32), (x + 57), ((y + height - 28)) + 0);
    g.drawLine((x + 57), (y + height - 32), (x + 57), (y + height - 28));
    g.drawLine((x + 94), (y + height - 32), (x + 94), ((y + height - 28)) + 0);
    g.drawLine((x + 94), (y + height - 32), (x + 94), (y + height - 28));
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 13));
    g.drawLine((x + 50), (y + 13), (x + 20), (y + 13));
    g.drawLine((x + 20), (y + 13), (x + 20), (y + height - 32));
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 13));
    g.drawLine((x + 30), (y + 13), (x + 20), (y + 13));
    g.drawLine((x + 20), (y + 13), (x + 20), (y + height - 32));
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
        data.updateClock(state.getPortValue(PORT_INDEX_A), 0, StdAttr.TRIG_FALLING);
    final var clockBTriggered =
        data.updateClock(state.getPortValue(PORT_INDEX_B), 1, StdAttr.TRIG_FALLING);
    final var values = data.getValue().getAll();

    if (state.getPortValue(PORT_INDEX_R0_1) == Value.TRUE
        && state.getPortValue(PORT_INDEX_R0_2) == Value.TRUE) {
      data.setValue(Value.createKnown(WIDTH, 0));
    } else {
      if (clockATriggered) values[0] = values[0].not();
      if (clockBTriggered) {
        var carry = Value.TRUE;
        for (var i = 1; i < values.length; i++) {
          final var oldValue = values[i];
          values[i] = oldValue.xor(carry);
          carry = oldValue.and(carry);
        }
      }
      data.setValue(Value.create(values));
    }

    state.setPort(PORT_INDEX_QA, data.getValue().get(0), 4);
    state.setPort(PORT_INDEX_QB, data.getValue().get(1), 4);
    state.setPort(PORT_INDEX_QC, data.getValue().get(2), 4);
    state.setPort(PORT_INDEX_QD, data.getValue().get(3), 4);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_B, PORT_INDEX_A};
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
