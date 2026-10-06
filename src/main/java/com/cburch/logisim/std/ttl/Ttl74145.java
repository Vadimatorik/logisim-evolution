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
 * TTL 74x145: BCD-to-decimal decoder/driver with open-collector outputs.
 *
 * <p>Model based on the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74ls145.pdf">SN74LS145 datasheet</a>. The 74HC145
 * uses the same DIP-16 pinout and function table: four active-high BCD inputs select one of ten
 * active-low outputs, and input codes above nine turn every output off. A is the least significant
 * bit.
 *
 * <p>A selected output is driven {@link Value#FALSE}. Every other output is released
 * ({@link Value#UNKNOWN}), so a pull-up in the circuit can raise it and outputs can be wire-ORed.
 * An unknown or error input leaves every output in error, because the decoder can no longer tell
 * which transistor should sink. Input thresholds of the HC and HCT variants are not modeled. There
 * is no HDL model: an open-drain high-impedance output is not a push-pull gate network.
 */
public class Ttl74145 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74145";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  // Outputs
  public static final byte Q0 = 1;
  public static final byte Q1 = 2;
  public static final byte Q2 = 3;
  public static final byte Q3 = 4;
  public static final byte Q4 = 5;
  public static final byte Q5 = 6;
  public static final byte Q6 = 7;
  public static final byte Q7 = 9;
  public static final byte Q8 = 10;
  public static final byte Q9 = 11;

  // Inputs. A is the least significant bit.
  public static final byte D = 12;
  public static final byte C = 13;
  public static final byte B = 14;
  public static final byte A = 15;

  // Power supply
  public static final byte GND = 8;
  public static final byte VCC = 16;

  /** Address inputs ordered from the least significant bit. */
  private static final byte[] ADDRESS = {A, B, C, D};

  private static final byte[] OUTPUTS = {Q0, Q1, Q2, Q3, Q4, Q5, Q6, Q7, Q8, Q9};

  private static final String[] PORT_NAMES = {
    "Q0 Output 0 (open-drain, active low)",
    "Q1 Output 1 (open-drain, active low)",
    "Q2 Output 2 (open-drain, active low)",
    "Q3 Output 3 (open-drain, active low)",
    "Q4 Output 4 (open-drain, active low)",
    "Q5 Output 5 (open-drain, active low)",
    "Q6 Output 6 (open-drain, active low)",
    "Q7 Output 7 (open-drain, active low)",
    "Q8 Output 8 (open-drain, active low)",
    "Q9 Output 9 (open-drain, active low)",
    "D BCD input (MSB)",
    "C BCD input",
    "B BCD input",
    "A BCD input (LSB)"
  };

  /** Creates a 74145 BCD-to-decimal decoder/driver. */
  public Ttl74145() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, null);
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
          "Q0", "Q1", "Q2", "Q3", "Q4", "Q5", "Q6", null,
          "Q7", "Q8", "Q9", "D", "C", "B", "A", null
        });
  }

  private static void setOutputs(InstanceState state, Value released, int sinkingCode) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      final var value = index == sinkingCode ? Value.FALSE : released;
      state.setPort(pinNrToPortNr(OUTPUTS[index]), value, DELAY);
    }
  }

  /**
   * Reads the BCD address.
   *
   * @return the code 0..15, or -1 when any address input is not a clean logic level
   */
  private static int addressCode(InstanceState state) {
    var code = 0;
    for (var bit = 0; bit < ADDRESS.length; bit++) {
      final var level = state.getPortValue(pinNrToPortNr(ADDRESS[bit]));
      if (level == Value.TRUE) {
        code |= 1 << bit;
      } else if (level != Value.FALSE) {
        return -1;
      }
    }
    return code;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var code = addressCode(state);
    if (code < 0) {
      setOutputs(state, Value.ERROR, -1);
      return;
    }
    setOutputs(state, Value.UNKNOWN, code);
  }
}
