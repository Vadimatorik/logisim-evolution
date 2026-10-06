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
 * TTL 74x35: hex noninverting buffer with open-collector outputs.
 *
 * <p>Simulation follows the Texas Instruments SN74ALS35A function table and DIP-14 pinout
 * (SDAS011C, December 1994). The 74HC35 entry in the 1988 TI High-Speed CMOS Logic Data Book
 * describes the same buffers. Each gate performs Y = A. A low input drives the output low; a high
 * input releases it. Logisim represents that released level as {@link Value#UNKNOWN}, so an
 * external pull-up can raise the wire. Unknown and error inputs are not treated as low.
 */
public class Ttl7435 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7435";

  public static final int DELAY = 1;

  public static final byte A1 = 1;
  public static final byte Y1 = 2;
  public static final byte A2 = 3;
  public static final byte Y2 = 4;
  public static final byte A3 = 5;
  public static final byte Y3 = 6;
  public static final byte GND = 7;
  public static final byte Y4 = 8;
  public static final byte A4 = 9;
  public static final byte Y5 = 10;
  public static final byte A5 = 11;
  public static final byte Y6 = 12;
  public static final byte A6 = 13;
  public static final byte VCC = 14;

  private static final byte[] OUTPUTS = {Y1, Y2, Y3, Y4, Y5, Y6};

  private static final String[] PORT_NAMES = {
    "1A", "1Y", "2A", "2Y", "3A", "3Y", "4Y", "4A", "5Y", "5A", "6Y", "6A"
  };

  /** Creates a 7435 hex noninverting buffer with open-collector outputs. */
  public Ttl7435() {
    super(
        _ID,
        (byte) 14,
        OUTPUTS,
        null,
        null,
        PORT_NAMES,
        true,
        DEFAULT_HEIGHT,
        new Ttl7435HdlGenerator());
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
  public void paintInternal(
      InstancePainter painter, int x, int y, int height, boolean isUpOriented) {
    final var g = painter.getGraphics();
    final var portWidth = 16;
    final var portHeight = 6;
    final var yOutput = y + (isUpOriented ? 20 : 40);
    Drawgates.paintBuffer(g, x + 30, yOutput, portWidth, portHeight);
    Drawgates.paintOpenCollector(g, x + 26, yOutput);
    Drawgates.paintOutputgate(g, x + 30, y, x + 26, yOutput, isUpOriented, height);
    Drawgates.paintSingleInputgate(g, x + 10, y, x + 30 - portWidth, yOutput, isUpOriented, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    for (byte output = Y1; output <= Y3; output += 2) {
      setOutput(state, (byte) (output - 1), output);
    }
    for (byte output = Y4; output <= Y6; output += 2) {
      setOutput(state, (byte) (output + 1), output);
    }
  }

  private static void setOutput(InstanceState state, byte inputPin, byte outputPin) {
    state.setPort(pinNrToPortNr(outputPin), openCollector(input(state, inputPin)), DELAY);
  }

  private static Value input(InstanceState state, byte dsPinNr) {
    return state.getPortValue(pinNrToPortNr(dsPinNr));
  }

  /**
   * Drives an open-collector output low only for a definite low input. A high input releases the
   * pin, and unknown or error inputs are passed through instead of being forced low.
   */
  private static Value openCollector(Value input) {
    if (input == Value.FALSE) return Value.FALSE;
    if (input == Value.ERROR) return Value.ERROR;
    return Value.UNKNOWN;
  }
}
