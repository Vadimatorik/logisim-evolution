/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x154: 4-line to 16-line decoder/demultiplexer with active-low outputs.
 *
 * <p>Model based on the
 * <a href="https://www.ti.com/lit/ds/symlink/cd74hc154.pdf">TI CD74HC154</a> and
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT154.pdf">Nexperia
 * 74HC154/74HCT154</a> data sheets. While both enables are low, the output selected by
 * {@code A3 A2 A1 A0} is low and the others are high. A high on either enable forces every output
 * high and the address is ignored. Using one enable as data covers the demultiplexer function.
 * Nanosecond delays are not modeled.
 *
 * <p>Pin 18 is {@code E1} in the TI data sheet and {@code E0} in the Nexperia data sheet. Pin 19 is
 * {@code E2} and {@code E1} respectively. Both are active low. This model names them {@code nE1}
 * and {@code nE2} after the TI pin numbers. {@code A0} on pin 23 is the least significant address
 * bit.
 *
 * <p>An unknown or error value on an enable, or on an address bit while the device is enabled,
 * makes every output unknown. A known disable still forces every output high.
 */
public class Ttl74154 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74154";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet.
  public static final byte Y0 = 1;
  public static final byte Y1 = 2;
  public static final byte Y2 = 3;
  public static final byte Y3 = 4;
  public static final byte Y4 = 5;
  public static final byte Y5 = 6;
  public static final byte Y6 = 7;
  public static final byte Y7 = 8;
  public static final byte Y8 = 9;
  public static final byte Y9 = 10;
  public static final byte Y10 = 11;
  public static final byte GND = 12;
  public static final byte Y11 = 13;
  public static final byte Y12 = 14;
  public static final byte Y13 = 15;
  public static final byte Y14 = 16;
  public static final byte Y15 = 17;
  public static final byte NE1 = 18;
  public static final byte NE2 = 19;
  public static final byte A3 = 20;
  public static final byte A2 = 21;
  public static final byte A1 = 22;
  public static final byte A0 = 23;
  public static final byte VCC = 24;

  /** Outputs ordered by the address that selects them. */
  private static final byte[] OUTPUTS = {
    Y0, Y1, Y2, Y3, Y4, Y5, Y6, Y7, Y8, Y9, Y10, Y11, Y12, Y13, Y14, Y15
  };

  private static final byte[] OUTPUT_PINS = {
    Y0, Y1, Y2, Y3, Y4, Y5, Y6, Y7, Y8, Y9, Y10, Y11, Y12, Y13, Y14, Y15
  };

  private static final String[] PORT_NAMES = {
    "Y0",
    "Y1",
    "Y2",
    "Y3",
    "Y4",
    "Y5",
    "Y6",
    "Y7",
    "Y8",
    "Y9",
    "Y10",
    "Y11",
    "Y12",
    "Y13",
    "Y14",
    "Y15",
    "nE1 Enable (active low)",
    "nE2 Enable (active low)",
    "A3 Address (MSB)",
    "A2 Address",
    "A1 Address",
    "A0 Address (LSB)"
  };

  private static final byte[] ADDRESS = {A0, A1, A2, A3};

  /** Creates a 74154 4-line to 16-line decoder/demultiplexer. */
  public Ttl74154() {
    super(_ID, (byte) 24, OUTPUT_PINS, PORT_NAMES, new Ttl74154HdlGenerator());
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
    g.drawRect((x + 12), (y + 18), 216, 24);
    GraphicsUtil.drawCenteredText(g, "4-16", (x + 120), (y + 30));
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 18));
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 18));
    g.drawLine((x + 70), (y + 7), (x + 70), (y + 18));
    g.drawLine((x + 90), (y + 7), (x + 90), (y + 18));
    g.drawLine((x + 130), (y + 7), (x + 130), (y + 18));
    structureBubble(g, (x + 130), (y + 14));
    g.drawLine((x + 110), (y + 7), (x + 110), (y + 18));
    structureBubble(g, (x + 110), (y + 14));
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + height - 13));
    g.drawLine((x + 10), (y + height - 13), (x + 16), (y + height - 13));
    g.drawLine((x + 16), (y + height - 13), (x + 16), (y + 42));
    structureBubble(g, (x + 16), (y + 46));
    g.drawLine((x + 30), (y + height - 7), (x + 30), (y + 42));
    structureBubble(g, (x + 30), (y + 46));
    g.drawLine((x + 50), (y + height - 7), (x + 50), (y + 42));
    structureBubble(g, (x + 50), (y + 46));
    g.drawLine((x + 70), (y + height - 7), (x + 70), (y + 42));
    structureBubble(g, (x + 70), (y + 46));
    g.drawLine((x + 90), (y + height - 7), (x + 90), (y + 42));
    structureBubble(g, (x + 90), (y + 46));
    g.drawLine((x + 110), (y + height - 7), (x + 110), (y + 42));
    structureBubble(g, (x + 110), (y + 46));
    g.drawLine((x + 130), (y + height - 7), (x + 130), (y + 42));
    structureBubble(g, (x + 130), (y + 46));
    g.drawLine((x + 150), (y + height - 7), (x + 150), (y + 42));
    structureBubble(g, (x + 150), (y + 46));
    g.drawLine((x + 170), (y + height - 7), (x + 170), (y + 42));
    structureBubble(g, (x + 170), (y + 46));
    g.drawLine((x + 190), (y + height - 7), (x + 190), (y + 42));
    structureBubble(g, (x + 190), (y + 46));
    g.drawLine((x + 210), (y + height - 7), (x + 210), (y + 42));
    structureBubble(g, (x + 210), (y + 46));
    g.drawLine((x + 230), (y + 7), (x + 230), (y + 13));
    g.drawLine((x + 230), (y + 13), (x + 224), (y + 13));
    g.drawLine((x + 224), (y + 13), (x + 224), (y + 18));
    structureBubble(g, (x + 224), (y + 14));
    g.drawLine((x + 210), (y + 7), (x + 210), (y + 18));
    structureBubble(g, (x + 210), (y + 14));
    g.drawLine((x + 190), (y + 7), (x + 190), (y + 18));
    structureBubble(g, (x + 190), (y + 14));
    g.drawLine((x + 170), (y + 7), (x + 170), (y + 18));
    structureBubble(g, (x + 170), (y + 14));
    g.drawLine((x + 150), (y + 7), (x + 150), (y + 18));
    structureBubble(g, (x + 150), (y + 14));
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var nEnable1 = state.getPortValue(pinNrToPortNr(NE1));
    final var nEnable2 = state.getPortValue(pinNrToPortNr(NE2));
    if (!isBit(nEnable1) || !isBit(nEnable2)) {
      setOutputs(state, Value.UNKNOWN, -1);
      return;
    }
    if (nEnable1 != Value.FALSE || nEnable2 != Value.FALSE) {
      setOutputs(state, Value.TRUE, -1);
      return;
    }

    var address = 0;
    for (var bit = 0; bit < ADDRESS.length; bit++) {
      final var value = state.getPortValue(pinNrToPortNr(ADDRESS[bit]));
      if (!isBit(value)) {
        setOutputs(state, Value.UNKNOWN, -1);
        return;
      }
      if (value == Value.TRUE) {
        address += 1 << bit;
      }
    }
    setOutputs(state, Value.TRUE, address);
  }

  private static boolean isBit(Value value) {
    return value == Value.TRUE || value == Value.FALSE;
  }

  /** Drives every output high, or unknown, except {@code selected} when it is in range. */
  private static void setOutputs(InstanceState state, Value inactive, int selected) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      final var value = index == selected ? Value.FALSE : inactive;
      state.setPort(pinNrToPortNr(OUTPUTS[index]), value, DELAY);
    }
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
