/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x238: 3-line to 8-line decoder/demultiplexer with active-high outputs.
 *
 * <p>Model based on the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT238.pdf">Nexperia
 * 74HC238/74HCT238 datasheet</a>. While {@code nE1} and {@code nE2} are low and {@code E3} is
 * high, the output selected by {@code A2 A1 A0} is high and the others are low. Otherwise every
 * output is low, and the address is ignored. Using one enable input as data covers the
 * demultiplexer function. Nanosecond delays are not modeled.
 *
 * <p>An unknown or error value on an enable, or on an address bit while the device is enabled,
 * makes every output unknown. A known disable still forces every output low.
 */
public class Ttl74238 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74238";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet.
  public static final byte A0 = 1;
  public static final byte A1 = 2;
  public static final byte A2 = 3;
  public static final byte NE1 = 4;
  public static final byte NE2 = 5;
  public static final byte E3 = 6;
  public static final byte Y7 = 7;
  public static final byte GND = 8;
  public static final byte Y6 = 9;
  public static final byte Y5 = 10;
  public static final byte Y4 = 11;
  public static final byte Y3 = 12;
  public static final byte Y2 = 13;
  public static final byte Y1 = 14;
  public static final byte Y0 = 15;
  public static final byte VCC = 16;

  /** Outputs ordered by the address that selects them. */
  private static final byte[] OUTPUTS = {Y0, Y1, Y2, Y3, Y4, Y5, Y6, Y7};

  private static final byte[] OUTPUT_PINS = {Y7, Y6, Y5, Y4, Y3, Y2, Y1, Y0};

  private static final String[] PORT_NAMES = {
    "A0 Address (LSB)",
    "A1 Address",
    "A2 Address (MSB)",
    "nE1 Enable (active low)",
    "nE2 Enable (active low)",
    "E3 Enable (active high)",
    "Y7",
    "Y6",
    "Y5",
    "Y4",
    "Y3",
    "Y2",
    "Y1",
    "Y0"
  };

  /** Creates a 74238 3-line to 8-line decoder/demultiplexer. */
  public Ttl74238() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl74238HdlGenerator());
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
    super.paintBase(painter, true, false);
    Drawgates.paintPortNamesByPin(
        painter,
        x,
        y,
        height,
        new String[] {
          "A0", "A1", "A2", "nE1", "nE2", "E3", "Y7", null,
          "Y6", "Y5", "Y4", "Y3", "Y2", "Y1", "Y0", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var nEnable1 = state.getPortValue(pinNrToPortNr(NE1));
    final var nEnable2 = state.getPortValue(pinNrToPortNr(NE2));
    final var enable3 = state.getPortValue(pinNrToPortNr(E3));
    if (!isBit(nEnable1) || !isBit(nEnable2) || !isBit(enable3)) {
      setOutputs(state, Value.UNKNOWN, -1);
      return;
    }
    final var enabled =
        nEnable1 == Value.FALSE && nEnable2 == Value.FALSE && enable3 == Value.TRUE;
    if (!enabled) {
      setOutputs(state, Value.FALSE, -1);
      return;
    }
    final var address0 = state.getPortValue(pinNrToPortNr(A0));
    final var address1 = state.getPortValue(pinNrToPortNr(A1));
    final var address2 = state.getPortValue(pinNrToPortNr(A2));
    if (!isBit(address0) || !isBit(address1) || !isBit(address2)) {
      setOutputs(state, Value.UNKNOWN, -1);
      return;
    }
    final var address =
        (address0 == Value.TRUE ? 1 : 0)
            + (address1 == Value.TRUE ? 2 : 0)
            + (address2 == Value.TRUE ? 4 : 0);
    setOutputs(state, Value.FALSE, address);
  }

  private static boolean isBit(Value value) {
    return value == Value.TRUE || value == Value.FALSE;
  }

  /** Drives every output low, or unknown, except {@code selected} when it is in range. */
  private static void setOutputs(InstanceState state, Value inactive, int selected) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      final var value = index == selected ? Value.TRUE : inactive;
      state.setPort(pinNrToPortNr(OUTPUTS[index]), value, DELAY);
    }
  }
}
