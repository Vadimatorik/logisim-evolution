/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;

/**
 * TTL 74x399: quad 2-port register.
 *
 * <p>Simulation follows the TI SN74LS399 and the pin-compatible Fairchild 74AC399 and 74F399.
 * Select low stores port 0 ({@code I0a}, {@code I0b}, {@code I0c}, {@code I0d}). Select high
 * stores port 1 ({@code I1a}, {@code I1b}, {@code I1c}, {@code I1d}). The selected word is stored
 * on the low-to-high clock transition. There is no reset, and the outputs are push-pull.
 * Nanosecond delays are not modeled.
 *
 * <p>An unknown select leaves a bit unchanged in value when both of its sources already agree on a
 * defined level. An error on the select, or on the source that can still be chosen, makes that bit
 * an error.
 */
public class Ttl74399 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must be a unique string among all tools.
   */
  public static final String _ID = "74399";

  public static final int DELAY = 1;

  public static final byte S = 1;
  public static final byte QA = 2;
  public static final byte I0A = 3;
  public static final byte I1A = 4;
  public static final byte I1B = 5;
  public static final byte I0B = 6;
  public static final byte QB = 7;
  public static final byte GND = 8;
  public static final byte CP = 9;
  public static final byte QC = 10;
  public static final byte I0C = 11;
  public static final byte I1C = 12;
  public static final byte I1D = 13;
  public static final byte I0D = 14;
  public static final byte QD = 15;
  public static final byte VCC = 16;

  private static final int WIDTH = 4;
  private static final byte[] OUTPUT_PINS = {QA, QB, QC, QD};
  private static final byte[] PORT0 = {I0A, I0B, I0C, I0D};
  private static final byte[] PORT1 = {I1A, I1B, I1C, I1D};
  private static final byte[] OUTPUTS = {QA, QB, QC, QD};
  private static final String[] PORT_NAMES = {
    "S Select",
    "Qa",
    "I0a Port 0 data",
    "I1a Port 1 data",
    "I1b Port 1 data",
    "I0b Port 0 data",
    "Qb",
    "CP Clock (rising edge)",
    "Qc",
    "I0c Port 0 data",
    "I1c Port 1 data",
    "I1d Port 1 data",
    "I0d Port 0 data",
    "Qd"
  };
  private static final String[] PIN_NAMES = {
    "S", "Qa", "I0a", "I1a", "I1b", "I0b", "Qb", null,
    "CP", "Qc", "I0c", "I1c", "I1d", "I0d", "Qd", null
  };

  /** Creates a 74399 quad 2-port register. */
  public Ttl74399() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl74399HdlGenerator());
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
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 8));
    g.drawRect(x + 25, y + 28, 10, 10);
    g.drawRect(x + 125, y + 28, 10, 10);
    g.drawRect(x + 125, y + 12, 10, 10);
    g.drawRect(x + 25, y + 12, 10, 10);
    g.drawLine(x + 30, y + height - 22, x + 30, y + height - 7);
    g.drawLine(x + 130, y + height - 22, x + 130, y + height - 7);
    g.drawLine(x + 130, y + 12, x + 130, y + 7);
    g.drawLine(x + 30, y + 12, x + 30, y + 7);
    g.drawLine(x + 25, y + 24, x + 135, y + 24);
    g.drawLine(x + 10, y + height - 7, x + 10, y + height - 9);
    g.drawLine(x + 10, y + height - 9, x + 21, y + height - 9);
    g.drawLine(x + 21, y + height - 9, x + 21, y + 24);
    g.drawLine(x + 21, y + 24, x + 25, y + 24);
    g.drawLine(x + 27, y + 24, x + 27, y + 28);
    g.drawLine(x + 127, y + 24, x + 127, y + 28);
    g.drawLine(x + 127, y + 24, x + 127, y + 22);
    g.drawLine(x + 27, y + 24, x + 27, y + 22);
    g.drawLine(x + 25, y + 25, x + 135, y + 25);
    g.drawLine(x + 150, y + 7, x + 150, y + 9);
    g.drawLine(x + 150, y + 9, x + 21, y + 9);
    g.drawLine(x + 21, y + 9, x + 21, y + 25);
    g.drawLine(x + 21, y + 25, x + 25, y + 25);
    g.drawLine(x + 30, y + 25, x + 30, y + 28);
    g.drawLine(x + 130, y + 25, x + 130, y + 28);
    g.drawLine(x + 130, y + 25, x + 130, y + 22);
    g.drawLine(x + 30, y + 25, x + 30, y + 22);
    g.drawLine(x + 50, y + height - 7, x + 50, y + height - 19);
    g.drawLine(x + 50, y + height - 19, x + 27, y + height - 19);
    g.drawLine(x + 27, y + height - 19, x + 27, y + height - 22);
    g.drawLine(x + 70, y + height - 7, x + 70, y + height - 19);
    g.drawLine(x + 70, y + height - 19, x + 127, y + height - 19);
    g.drawLine(x + 127, y + height - 19, x + 127, y + height - 22);
    g.drawLine(x + 90, y + height - 7, x + 90, y + 25);
    g.drawLine(x + 90, y + 25, x + 127, y + 25);
    g.drawLine(x + 127, y + 25, x + 127, y + 22);
    g.drawLine(x + 110, y + height - 7, x + 110, y + 25);
    g.drawLine(x + 110, y + 25, x + 27, y + 25);
    g.drawLine(x + 27, y + 25, x + 27, y + 22);
    GraphicsUtil.drawCenteredText(g, "a", x + 30, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "b", x + 130, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "c", x + 130, y + 17);
    GraphicsUtil.drawCenteredText(g, "d", x + 30, y + 17);
    g.setFont(previousFont);
  }



  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getState(state);
    if (data.updateClock(input(state, CP), StdAttr.TRIG_RISING)) {
      final var stored = new Value[WIDTH];
      final var select = input(state, S);
      for (var bit = 0; bit < WIDTH; bit++) {
        stored[bit] = selected(select, input(state, PORT0[bit]), input(state, PORT1[bit]));
      }
      data.setValue(Value.create(stored));
    }
    final var word = data.getValue();
    for (var bit = 0; bit < WIDTH; bit++) {
      state.setPort(pinNrToPortNr(OUTPUTS[bit]), word.get(bit), DELAY);
    }
  }

  private static TtlRegisterData getState(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.create(WIDTH));
      state.setData(data);
    }
    return data;
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }

  /**
   * Chooses one source bit. A defined select takes that source. An unknown select keeps a defined
   * level only when both sources already carry it. An error on the select, or on a source that the
   * unknown select might still choose, makes the result an error.
   */
  private static Value selected(Value select, Value port0, Value port1) {
    if (select == Value.FALSE) {
      return level(port0);
    }
    if (select == Value.TRUE) {
      return level(port1);
    }
    if (select != Value.ERROR && port0 == port1 && defined(port0)) {
      return port0;
    }
    if (select == Value.ERROR || port0 == Value.ERROR || port1 == Value.ERROR) {
      return Value.ERROR;
    }
    return Value.UNKNOWN;
  }

  private static Value level(Value value) {
    return defined(value) || value == Value.ERROR ? value : Value.UNKNOWN;
  }

  private static boolean defined(Value value) {
    return value == Value.TRUE || value == Value.FALSE;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {pinNrToPortNr(CP)};
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
