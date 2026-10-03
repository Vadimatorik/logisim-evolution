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
 * TTL 74x280: 9-bit odd/even parity generator/checker.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT280.pdf">Nexperia 74HC280</a>
 * function table. {@code PE} is high when an even number of data inputs are high, including none.
 * {@code PO} is high when that number is odd. Pin 3 is not connected. Nanosecond delays are not
 * modeled.
 *
 * <p>An input that is not exactly high or low makes both outputs unknown. An error on any input
 * makes both outputs an error.
 */
public class Ttl74280 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74280";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  public static final byte I6 = 1;
  public static final byte I7 = 2;
  public static final byte NC = 3;
  public static final byte I8 = 4;
  public static final byte PE = 5;
  public static final byte PO = 6;

  public static final byte GND = 7;

  public static final byte I0 = 8;
  public static final byte I1 = 9;
  public static final byte I2 = 10;
  public static final byte I3 = 11;
  public static final byte I4 = 12;
  public static final byte I5 = 13;

  public static final byte VCC = 14;

  /** Data inputs in bit order, so bit 0 of a word is {@link #I0}. */
  private static final byte[] INPUTS = new byte[] {I0, I1, I2, I3, I4, I5, I6, I7, I8};

  private static final byte[] OUTPUTS = new byte[] {PE, PO};

  private static final byte[] UNUSED = new byte[] {NC};

  private static final String[] PORT_NAMES = {
    "I6 Data input 6",
    "I7 Data input 7",
    "I8 Data input 8",
    "PE Even parity output",
    "PO Odd parity output",
    "I0 Data input 0",
    "I1 Data input 1",
    "I2 Data input 2",
    "I3 Data input 3",
    "I4 Data input 4",
    "I5 Data input 5"
  };

  /** Creates a 74280 9-bit odd/even parity generator/checker. */
  public Ttl74280() {
    super(_ID, (byte) 14, OUTPUTS, UNUSED, PORT_NAMES, new Ttl74280HdlGenerator());
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>The unconnected pin and the power pins are omitted from the port list.
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    var skipped = 0;
    if (dsPinNr > NC) {
      skipped++;
    }
    if (dsPinNr > GND) {
      skipped++;
    }
    return (byte) (dsPinNr - 1 - skipped);
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
          "I6", "I7", null, "I8", "PE", "PO", null,
          "I0", "I1", "I2", "I3", "I4", "I5", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var odd = false;
    var unknown = false;
    for (final var pin : INPUTS) {
      final var value = state.getPortValue(pinNrToPortNr(pin));
      if (value == Value.ERROR) {
        setBoth(state, Value.ERROR);
        return;
      }
      if (value == Value.TRUE) {
        odd = !odd;
      } else if (value != Value.FALSE) {
        unknown = true;
      }
    }
    if (unknown) {
      setBoth(state, Value.UNKNOWN);
      return;
    }
    state.setPort(pinNrToPortNr(PO), odd ? Value.TRUE : Value.FALSE, DELAY);
    state.setPort(pinNrToPortNr(PE), odd ? Value.FALSE : Value.TRUE, DELAY);
  }

  private static void setBoth(InstanceState state, Value value) {
    state.setPort(pinNrToPortNr(PE), value, DELAY);
    state.setPort(pinNrToPortNr(PO), value, DELAY);
  }
}
