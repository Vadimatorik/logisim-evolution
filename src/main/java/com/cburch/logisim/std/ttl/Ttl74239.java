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
 * TTL 74x239: dual 2-line to 4-line decoder/demultiplexer with active-high outputs.
 *
 * <p>Pinout matches the 74x139. Each half decodes {@code A1 A0} onto {@code Y0} to {@code Y3}.
 * While {@code nE} is low, the selected output is high and the others are low. While {@code nE}
 * is high, every output of that half is low and the address is ignored. The two halves are
 * independent. Using an enable as data covers the demultiplexer function. Nanosecond delays are
 * not modeled.
 *
 * <p>An unknown or error value on an enable, or on an address bit while that half is enabled,
 * makes every output of that half unknown. A known disable still forces that half low.
 */
public class Ttl74239 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74239";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet.
  public static final byte NE1 = 1;
  public static final byte A0_1 = 2;
  public static final byte A1_1 = 3;
  public static final byte Y0_1 = 4;
  public static final byte Y1_1 = 5;
  public static final byte Y2_1 = 6;
  public static final byte Y3_1 = 7;
  public static final byte GND = 8;
  public static final byte Y3_2 = 9;
  public static final byte Y2_2 = 10;
  public static final byte Y1_2 = 11;
  public static final byte Y0_2 = 12;
  public static final byte A1_2 = 13;
  public static final byte A0_2 = 14;
  public static final byte NE2 = 15;
  public static final byte VCC = 16;

  /** Outputs of the first decoder, ordered by the address that selects them. */
  private static final byte[] OUTPUTS_1 = {Y0_1, Y1_1, Y2_1, Y3_1};

  /** Outputs of the second decoder, ordered by the address that selects them. */
  private static final byte[] OUTPUTS_2 = {Y0_2, Y1_2, Y2_2, Y3_2};

  private static final byte[] OUTPUT_PINS = {
    Y0_1, Y1_1, Y2_1, Y3_1, Y0_2, Y1_2, Y2_2, Y3_2
  };

  private static final String[] PORT_NAMES = {
    "1nE Enable (active low)",
    "1A0 Address (LSB)",
    "1A1 Address (MSB)",
    "1Y0",
    "1Y1",
    "1Y2",
    "1Y3",
    "2Y3",
    "2Y2",
    "2Y1",
    "2Y0",
    "2A1 Address (MSB)",
    "2A0 Address (LSB)",
    "2nE Enable (active low)"
  };

  /** Creates a 74239 dual 2-line to 4-line decoder/demultiplexer. */
  public Ttl74239() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl74239HdlGenerator());
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
          "1nE", "1A0", "1A1", "1Y0", "1Y1", "1Y2", "1Y3", null,
          "2Y3", "2Y2", "2Y1", "2Y0", "2A1", "2A0", "2nE", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    propagateHalf(state, NE1, A0_1, A1_1, OUTPUTS_1);
    propagateHalf(state, NE2, A0_2, A1_2, OUTPUTS_2);
  }

  private static void propagateHalf(
      InstanceState state, byte nEnablePin, byte address0Pin, byte address1Pin, byte[] outputs) {
    final var nEnable = state.getPortValue(pinNrToPortNr(nEnablePin));
    if (!isBit(nEnable)) {
      setOutputs(state, outputs, Value.UNKNOWN, -1);
      return;
    }
    if (nEnable != Value.FALSE) {
      setOutputs(state, outputs, Value.FALSE, -1);
      return;
    }
    final var address0 = state.getPortValue(pinNrToPortNr(address0Pin));
    final var address1 = state.getPortValue(pinNrToPortNr(address1Pin));
    if (!isBit(address0) || !isBit(address1)) {
      setOutputs(state, outputs, Value.UNKNOWN, -1);
      return;
    }
    final var address = (address0 == Value.TRUE ? 1 : 0) + (address1 == Value.TRUE ? 2 : 0);
    setOutputs(state, outputs, Value.FALSE, address);
  }

  private static boolean isBit(Value value) {
    return value == Value.TRUE || value == Value.FALSE;
  }

  /** Drives one half low, or unknown, except {@code selected} when it is in range. */
  private static void setOutputs(InstanceState state, byte[] outputs, Value inactive, int selected) {
    for (var index = 0; index < outputs.length; index++) {
      final var value = index == selected ? Value.TRUE : inactive;
      state.setPort(pinNrToPortNr(outputs[index]), value, DELAY);
    }
  }
}
