/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x4049: hex inverting high-to-low level shifter.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC4049.pdf">Nexperia 74HC4049</a>
 * function table. Each of the six channels is an independent inverter: a low input produces a high
 * output, and a high input produces a low output. An unknown or error input produces an error on
 * its own output, which is how a one-bit {@code Value.not()} is defined. Other channels are left
 * unchanged.
 *
 * <p>Supply is pin 1 and ground is pin 8. Pins 13 and 16 are not connected. Inputs that tolerate
 * voltages above VCC, and the analog high-to-low level shift, are not modeled. The output delay is
 * one simulator tick.
 */
public class Ttl744049 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744049";

  /** Output delay in simulator ticks. Nanosecond delays from the data sheet are not modeled. */
  public static final int DELAY = 1;

  /** Supply. This package does not put VCC on the last pin. */
  public static final byte VCC = 1;

  public static final byte Y1 = 2;
  public static final byte A1 = 3;
  public static final byte Y2 = 4;
  public static final byte A2 = 5;
  public static final byte Y3 = 6;
  public static final byte A3 = 7;
  public static final byte GND = 8;
  public static final byte A4 = 9;
  public static final byte Y4 = 10;
  public static final byte A5 = 11;
  public static final byte Y5 = 12;

  /** Not connected. */
  public static final byte NC13 = 13;

  public static final byte A6 = 14;
  public static final byte Y6 = 15;

  /** Not connected. Pin 16 is not a supply pin on this device. */
  public static final byte NC16 = 16;

  public static final int PORT_Y1 = pinNrToPortNr(Y1);
  public static final int PORT_A1 = pinNrToPortNr(A1);
  public static final int PORT_Y2 = pinNrToPortNr(Y2);
  public static final int PORT_A2 = pinNrToPortNr(A2);
  public static final int PORT_Y3 = pinNrToPortNr(Y3);
  public static final int PORT_A3 = pinNrToPortNr(A3);
  public static final int PORT_A4 = pinNrToPortNr(A4);
  public static final int PORT_Y4 = pinNrToPortNr(Y4);
  public static final int PORT_A5 = pinNrToPortNr(A5);
  public static final int PORT_Y5 = pinNrToPortNr(Y5);
  public static final int PORT_A6 = pinNrToPortNr(A6);
  public static final int PORT_Y6 = pinNrToPortNr(Y6);

  private static final byte[] OUTPUT_PINS = {Y1, Y2, Y3, Y4, Y5, Y6};
  private static final byte[] UNUSED_PINS = {NC13, NC16};
  private static final int[] INPUT_PORTS = {PORT_A1, PORT_A2, PORT_A3, PORT_A4, PORT_A5, PORT_A6};
  private static final int[] OUTPUT_PORTS = {PORT_Y1, PORT_Y2, PORT_Y3, PORT_Y4, PORT_Y5, PORT_Y6};
  private static final String[] PORT_NAMES = {
    "1Y (inverting output)",
    "1A (input)",
    "2Y (inverting output)",
    "2A (input)",
    "3Y (inverting output)",
    "3A (input)",
    "4A (input)",
    "4Y (inverting output)",
    "5A (input)",
    "5Y (inverting output)",
    "6A (input)",
    "6Y (inverting output)"
  };
  private static final String[] PIN_NAMES = {
    null, "1Y", "1A", "2Y", "2A", "3Y", "3A", null,
    "4A", "4Y", "5A", "5Y", null, "6A", "6Y", null
  };

  /** Creates a 744049 hex inverting high-to-low level shifter. */
  public Ttl744049() {
    super(
        _ID,
        (byte) 16,
        OUTPUT_PINS,
        UNUSED_PINS,
        PORT_NAMES,
        VCC,
        GND,
        new Ttl744049HdlGenerator());
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Supply, ground and unconnected pins are omitted from the port list.
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    var skipped = 0;
    for (var pin = 1; pin < dsPinNr; pin++) {
      if (pin == VCC || pin == GND || pin == NC13 || pin == NC16) skipped++;
    }
    return (byte) (dsPinNr - 1 - skipped);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    Drawgates.paintPortNamesByPin(painter, x, y, height, PIN_NAMES);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    for (var gate = 0; gate < INPUT_PORTS.length; gate++) {
      state.setPort(OUTPUT_PORTS[gate], state.getPortValue(INPUT_PORTS[gate]).not(), DELAY);
    }
  }
}
