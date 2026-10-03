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
 * TTL 74x682: 8-bit magnitude comparator.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74hc682.pdf">TI SN74HC682</a> function table. The
 * DIP-20 pinout matches that data sheet and the Hitachi HD74HC682. The device compares unsigned
 * words P and Q. Both outputs are active low and push-pull: nP=Q is low only when the words are
 * equal, and nP&gt;Q is low only when P is greater. Both stay high when P is less, so an external
 * NAND of the two outputs produces an active-low P&lt;Q. There is no enable input.
 *
 * <p>The 100 kΩ pull-ups on the Q inputs are not modelled. A bit counts only when it reads exactly
 * {@link Value#TRUE} or {@link Value#FALSE}. Any unknown or error level makes both outputs unknown,
 * so the device does not report a false P&lt;Q. An unconnected device therefore drives both outputs
 * unknown.
 */
public class Ttl74682 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74682";

  public static final int DELAY = 1;

  public static final byte PGTQ = 1;
  public static final byte P0 = 2;
  public static final byte Q0 = 3;
  public static final byte P1 = 4;
  public static final byte Q1 = 5;
  public static final byte P2 = 6;
  public static final byte Q2 = 7;
  public static final byte P3 = 8;
  public static final byte Q3 = 9;
  public static final byte P4 = 11;
  public static final byte Q4 = 12;
  public static final byte P5 = 13;
  public static final byte Q5 = 14;
  public static final byte P6 = 15;
  public static final byte Q6 = 16;
  public static final byte P7 = 17;
  public static final byte Q7 = 18;
  public static final byte PQ = 19;

  public static final byte GND = 10;
  public static final byte VCC = 20;

  /** Word P inputs, bit 0 first. */
  private static final byte[] WORD_P = {P0, P1, P2, P3, P4, P5, P6, P7};

  /** Word Q inputs, bit 0 first. */
  private static final byte[] WORD_Q = {Q0, Q1, Q2, Q3, Q4, Q5, Q6, Q7};

  private static final byte[] OUTPUTS = {PGTQ, PQ};

  private static final String[] PORT_NAMES = {
    "nP>Q greater (active LOW)",
    "P0",
    "Q0",
    "P1",
    "Q1",
    "P2",
    "Q2",
    "P3",
    "Q3",
    "P4",
    "Q4",
    "P5",
    "Q5",
    "P6",
    "Q6",
    "P7",
    "Q7",
    "nP=Q equal (active LOW)"
  };

  /** Creates a 74682 8-bit magnitude comparator. */
  public Ttl74682() {
    super(_ID, (byte) 20, OUTPUTS, PORT_NAMES, new Ttl74682HdlGenerator());
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
          "nP>Q", "P0", "Q0", "P1", "Q1", "P2", "Q2", "P3", "Q3", null,
          "P4", "Q4", "P5", "Q5", "P6", "Q6", "P7", "Q7", "nP=Q", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var wordP = knownWord(state, WORD_P);
    final var wordQ = knownWord(state, WORD_Q);
    if (wordP < 0 || wordQ < 0) {
      setUnknown(state, PGTQ);
      setUnknown(state, PQ);
      return;
    }
    setLow(state, PQ, wordP == wordQ);
    setLow(state, PGTQ, wordP > wordQ);
  }

  /** Returns the unsigned word, or -1 when any pin is not a solid high or low. */
  private static int knownWord(InstanceState state, byte[] pins) {
    var word = 0;
    for (var bit = 0; bit < pins.length; bit++) {
      final var value = state.getPortValue(pinNrToPortNr(pins[bit]));
      if (value != Value.TRUE && value != Value.FALSE) {
        return -1;
      }
      if (value == Value.TRUE) {
        word |= 1 << bit;
      }
    }
    return word;
  }

  private static void setLow(InstanceState state, byte dsPinNr, boolean low) {
    state.setPort(pinNrToPortNr(dsPinNr), low ? Value.FALSE : Value.TRUE, DELAY);
  }

  private static void setUnknown(InstanceState state, byte dsPinNr) {
    state.setPort(pinNrToPortNr(dsPinNr), Value.UNKNOWN, DELAY);
  }
}
