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
 * TTL 74x4078: 8-input NOR/OR gate.
 *
 * <p>Simulation follows the
 * <a href="https://www.st.com/resource/en/datasheet/cd00000322.pdf">M74HC4078</a> data sheet.
 * {@code Y} is the OR of {@code A} through {@code H}, and {@code X} is the NOR. Both outputs are
 * low and high respectively only when every input is low. A definite high on any input forces
 * {@code Y} high and {@code X} low; otherwise an unknown or error input makes both outputs an
 * error. Pins 6 and 8 are not connected.
 */
public class Ttl744078 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744078";

  public static final int DELAY = 1;

  public static final byte Y = 1;
  public static final byte A = 2;
  public static final byte B = 3;
  public static final byte C = 4;
  public static final byte D = 5;
  public static final byte E = 9;
  public static final byte F = 10;
  public static final byte G = 11;
  public static final byte H = 12;
  public static final byte X = 13;

  public static final byte GND = 7;
  public static final byte VCC = 14;

  private static final byte[] INPUTS = {A, B, C, D, E, F, G, H};
  private static final byte[] OUTPUTS = {Y, X};
  private static final byte[] UNUSED_PINS = {6, 8};
  private static final String[] PORT_NAMES = {"Y", "A", "B", "C", "D", "E", "F", "G", "H", "X"};

  /** Creates a 744078 8-input NOR/OR gate. */
  public Ttl744078() {
    super(_ID, (byte) 14, OUTPUTS, UNUSED_PINS, PORT_NAMES, new Ttl744078HdlGenerator());
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Pins 6 and 8 are not connected, and the power pins are omitted from the port list.
   *
   * @param dsPinNr datasheet pin number of a signal pin
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    return (byte) ((dsPinNr <= D) ? dsPinNr - 1 : dsPinNr - 4);
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
          "Y", "A", "B", "C", "D", "NC", null, "NC", "E", "F", "G", "H", "X", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var combined = Value.FALSE;
    for (final var input : INPUTS) {
      combined = combined.or(state.getPortValue(pinNrToPortNr(input)));
    }
    state.setPort(pinNrToPortNr(Y), combined, DELAY);
    state.setPort(pinNrToPortNr(X), combined.not(), DELAY);
  }
}
