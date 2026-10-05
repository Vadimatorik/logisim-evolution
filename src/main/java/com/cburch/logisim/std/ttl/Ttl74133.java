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
 * TTL 74x133: single 13-input NAND gate.
 *
 * <p>Model based on the <a href="https://media.digikey.com/pdf/Data%20Sheets/ST%20Microelectronics%20PDFS/M74HC133.pdf">ST
 * M74HC133 datasheet</a>. Philips 74HC133 uses the same DIP-16 pinout and the same function. Y is low only when every
 * data input is high. The output is a standard push-pull driver.
 *
 * <p>A low input forces Y high. When no input is low, an error input makes Y an error, an unknown input makes Y unknown,
 * and only an all-high input vector makes Y low.
 */
public class Ttl74133 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74133";

  public static final int DELAY = 1;

  public static final byte A = 1;
  public static final byte B = 2;
  public static final byte C = 3;
  public static final byte D = 4;
  public static final byte E = 5;
  public static final byte F = 6;
  public static final byte G = 7;
  public static final byte H = 10;
  public static final byte I = 11;
  public static final byte J = 12;
  public static final byte K = 13;
  public static final byte L = 14;
  public static final byte M = 15;

  public static final byte Y = 9;

  public static final byte GND = 8;
  public static final byte VCC = 16;

  /** Data inputs in datasheet order, A through M. */
  private static final byte[] INPUTS = {A, B, C, D, E, F, G, H, I, J, K, L, M};

  private static final byte[] OUTPUTS = {Y};

  private static final String[] PORT_NAMES = {
    "A data input",
    "B data input",
    "C data input",
    "D data input",
    "E data input",
    "F data input",
    "G data input",
    "Y data output",
    "H data input",
    "I data input",
    "J data input",
    "K data input",
    "L data input",
    "M data input"
  };

  /** Creates a 74133 single 13-input NAND gate. */
  public Ttl74133() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74133HdlGenerator());
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
          "A", "B", "C", "D", "E", "F", "G", null,
          "Y", "H", "I", "J", "K", "L", "M", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    state.setPort(pinNrToPortNr(Y), nand(state), DELAY);
  }

  /**
   * NAND of the thirteen data inputs.
   *
   * <p>A low input dominates. Otherwise an error outranks an unknown level, and a fully high vector produces a low
   * output.
   */
  private static Value nand(InstanceState state) {
    var output = Value.FALSE;
    for (final var input : INPUTS) {
      final var value = state.getPortValue(pinNrToPortNr(input));
      if (value == Value.FALSE) {
        return Value.TRUE;
      }
      if (value == Value.ERROR) {
        output = Value.ERROR;
      } else if (value != Value.TRUE && output != Value.ERROR) {
        output = Value.UNKNOWN;
      }
    }
    return output;
  }
}
