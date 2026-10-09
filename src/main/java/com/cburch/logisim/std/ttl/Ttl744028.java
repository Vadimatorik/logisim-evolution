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
 * TTL 74x4028: BCD-to-decimal decoder with active-high outputs.
 *
 * <p>Model based on the
 * <a href="https://assets.nexperia.com/documents/data-sheet/HEF4028B.pdf">Nexperia HEF4028B</a>
 * pinout and the Toshiba TC74HC4028 / ST M74HC4028 function table. Those 74HC parts are pin and
 * function compatible with 4028B. A BCD code on {@code A3 A2 A1 A0} drives the matching {@code Yn}
 * high and the other outputs low. {@code A0} on pin 10 is the least significant bit. Codes 10 to
 * 15 hold every output low, which is also how {@code A3} acts as an active-low enable for a 3-to-8
 * decoder. Nanosecond delays are not modeled.
 *
 * <p>An unknown or error value on any address bit makes every output unknown.
 */
public class Ttl744028 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "744028";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet.
  public static final byte Y4 = 1;
  public static final byte Y2 = 2;
  public static final byte Y0 = 3;
  public static final byte Y7 = 4;
  public static final byte Y9 = 5;
  public static final byte Y5 = 6;
  public static final byte Y6 = 7;
  public static final byte GND = 8;
  public static final byte Y8 = 9;
  public static final byte A0 = 10;
  public static final byte A3 = 11;
  public static final byte A2 = 12;
  public static final byte A1 = 13;
  public static final byte Y1 = 14;
  public static final byte Y3 = 15;
  public static final byte VCC = 16;

  /** Outputs ordered by the BCD code that selects them. */
  private static final byte[] OUTPUTS = {Y0, Y1, Y2, Y3, Y4, Y5, Y6, Y7, Y8, Y9};

  private static final byte[] OUTPUT_PINS = {Y4, Y2, Y0, Y7, Y9, Y5, Y6, Y8, Y1, Y3};

  private static final String[] PORT_NAMES = {
    "Y4",
    "Y2",
    "Y0",
    "Y7",
    "Y9",
    "Y5",
    "Y6",
    "Y8",
    "A0 Address (LSB)",
    "A3 Address (MSB)",
    "A2 Address",
    "A1 Address",
    "Y1",
    "Y3"
  };

  private static final byte[] ADDRESS = {A0, A1, A2, A3};

  /** Creates a 744028 BCD-to-decimal decoder. */
  public Ttl744028() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl744028HdlGenerator());
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
          "Y4", "Y2", "Y0", "Y7", "Y9", "Y5", "Y6", null,
          "Y8", "A0", "A3", "A2", "A1", "Y1", "Y3", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
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
    setOutputs(state, Value.FALSE, address <= 9 ? address : -1);
  }

  private static boolean isBit(Value value) {
    return value == Value.TRUE || value == Value.FALSE;
  }

  /**
   * Drives every output to {@code inactive}, or high when {@code selected} names that output.
   * {@code selected} of {@code -1} leaves every output at {@code inactive}.
   */
  private static void setOutputs(InstanceState state, Value inactive, int selected) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      final var value = index == selected ? Value.TRUE : inactive;
      state.setPort(pinNrToPortNr(OUTPUTS[index]), value, DELAY);
    }
  }
}
