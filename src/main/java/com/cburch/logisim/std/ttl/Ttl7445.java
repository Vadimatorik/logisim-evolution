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
 * TTL 74x45: BCD-to-decimal decoder/driver with open-collector outputs.
 *
 * <p>Simulation follows the <a href="https://www.ti.com/lit/ds/symlink/sn7445.pdf">SN5445/SN7445</a>
 * function table (SDLS110, March 1988). There is no 74HC45 data sheet; the HC family uses the
 * push-pull 74HC42 for this pinout. For a valid BCD code the selected output sinks, and the other
 * nine are released. Codes 10 to 15 release every output. A released open-collector output is
 * {@link Value#UNKNOWN}, matching the other open-collector TTL models. An error on any input is
 * copied to every output; any other non-binary input releases every output, because none can be
 * shown to be sinking.
 */
public class Ttl7445 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "7445";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  // Outputs, active low and open collector
  public static final byte O0 = 1;
  public static final byte O1 = 2;
  public static final byte O2 = 3;
  public static final byte O3 = 4;
  public static final byte O4 = 5;
  public static final byte O5 = 6;
  public static final byte O6 = 7;
  public static final byte O7 = 9;
  public static final byte O8 = 10;
  public static final byte O9 = 11;

  // Inputs, A is the least significant bit
  public static final byte D = 12;
  public static final byte C = 13;
  public static final byte B = 14;
  public static final byte A = 15;

  // Power supply
  public static final byte GND = 8;
  public static final byte VCC = 16;

  private static final int CODE_UNKNOWN = -1;
  private static final int CODE_ERROR = -2;

  /** Outputs ordered by the BCD code that pulls them low. */
  private static final byte[] OUTPUTS = {O0, O1, O2, O3, O4, O5, O6, O7, O8, O9};

  /** Inputs ordered from the least significant bit. */
  private static final byte[] INPUTS = {A, B, C, D};

  private static final String[] PORT_NAMES = {
    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
    "D", "C", "B", "A"
  };

  /** Creates a 7445 BCD-to-decimal decoder/driver. */
  public Ttl7445() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl7445HdlGenerator());
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
          "0", "1", "2", "3", "4", "5", "6", null,
          "7", "8", "9", "D", "C", "B", "A", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var code = inputCode(state);
    for (var index = 0; index < OUTPUTS.length; index++) {
      final var value =
          code == CODE_ERROR ? Value.ERROR : code == index ? Value.FALSE : Value.UNKNOWN;
      state.setPort(pinNrToPortNr(OUTPUTS[index]), value, DELAY);
    }
  }

  /**
   * Reads the BCD inputs.
   *
   * @return the code 0 to 9, {@link #CODE_UNKNOWN} when the code is above 9 or an input is not a
   *     binary value, or {@link #CODE_ERROR} when any input is an error
   */
  private static int inputCode(InstanceState state) {
    final var bits = new Value[INPUTS.length];
    for (var index = 0; index < INPUTS.length; index++) {
      bits[index] = state.getPortValue(pinNrToPortNr(INPUTS[index]));
      if (bits[index].isErrorValue()) {
        return CODE_ERROR;
      }
    }
    var code = 0;
    for (var index = 0; index < bits.length; index++) {
      if (bits[index] != Value.TRUE && bits[index] != Value.FALSE) {
        return CODE_UNKNOWN;
      }
      if (bits[index] == Value.TRUE) {
        code |= 1 << index;
      }
    }
    return code <= 9 ? code : CODE_UNKNOWN;
  }
}
