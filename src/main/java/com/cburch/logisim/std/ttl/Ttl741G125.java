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
 * TTL 74x1G125: single bus buffer with a 3-state output.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT1G125.pdf">74HC1G125;
 * 74HCT1G125</a>
 * function table (Nexperia, Rev. 8, 27 November 2023). Pin 1 is active-low {@code OE}, pin 2 is
 * input {@code A}, pin 4 is output {@code Y}, pin 3 is GND and pin 5 is VCC. TSSOP5 and SC-74A use
 * the same pin numbers. A low {@code OE} copies {@code A} to {@code Y}. A high {@code OE} releases
 * {@code Y}. This library has no separate high-impedance value, so a released output is
 * {@link Value#UNKNOWN}, as on the 74125.
 *
 * <p>An unknown or error {@code OE} changes {@code Y} only when the enabled and released results
 * disagree. An error on such an input makes the disagreed output an error; an unknown input makes
 * it unknown. Nanosecond delays and the HC/HCT input thresholds are not modeled.
 *
 * <p>There is no HDL model. The FPGA netlist rejects three-state drivers, and the other
 * three-state TTL components in this library do not generate HDL either.
 *
 * <p>The drawn package is the library's two-row body, not the SOT lead arrangement. Pins 1 and 2
 * are on the pin-1 side. Pins 5, 4 and 3 are on the opposite side, left to right.
 */
public class Ttl741G125 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "741G125";

  public static final int DELAY = 1;

  /** Active-low output enable. */
  public static final byte OE = 1;

  public static final byte A = 2;
  public static final byte GND = 3;
  public static final byte Y = 4;
  public static final byte VCC = 5;

  public static final int PORT_INDEX_OE = 0;
  public static final int PORT_INDEX_A = 1;
  public static final int PORT_INDEX_Y = 2;

  private static final byte[] OUTPUT_PORTS = {Y};
  private static final String[] PORT_NAMES = {
    "nOE (output enable, active LOW)", "A", "Y"
  };

  /** Creates a 741G125 single bus buffer with a 3-state output. */
  public Ttl741G125() {
    super(_ID, (byte) 5, OUTPUT_PORTS, null, PORT_NAMES, VCC, GND, null);
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
    return (byte) (dsPinNr < GND ? dsPinNr - 1 : dsPinNr - 2);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, true, false);
    Drawgates.paintPortNamesByPin(
        painter, x, y, height, new String[] {"nOE", "A", null, "Y", null});
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var outputEnable = state.getPortValue(pinNrToPortNr(OE));
    final var data = state.getPortValue(pinNrToPortNr(A));
    state.setPort(pinNrToPortNr(Y), releasedOutput(outputEnable, data), DELAY);
  }

  /**
   * Enabled output copies the data pin. A high enable releases the pin. An undefined enable keeps
   * a value only when both interpretations produce it.
   */
  private static Value releasedOutput(Value outputEnable, Value data) {
    if (outputEnable == Value.FALSE) {
      return data;
    }
    if (outputEnable == Value.TRUE) {
      return Value.UNKNOWN;
    }
    if (data == Value.UNKNOWN || data == Value.NIL) {
      return Value.UNKNOWN;
    }
    if (outputEnable == Value.ERROR || data == Value.ERROR) {
      return Value.ERROR;
    }
    return Value.UNKNOWN;
  }
}
