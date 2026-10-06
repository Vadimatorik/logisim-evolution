/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x237: 3-to-8 line decoder/demultiplexer with address latches.
 *
 * <p>Model based on the <a href="https://assets.nexperia.com/documents/data-sheet/74HC237.pdf">74HC237
 * datasheet</a> function table. While nLE is low the address latch is transparent. The low-to-high
 * transition of nLE keeps the address that was already stored, and later address changes are
 * ignored until nLE is low again. The outputs are active high and mutually exclusive. They leave
 * the low level only when nE1 is low and E2 is high. Those two enable pins act on the outputs
 * directly and are not stored in the latch.
 *
 * <p>An address bit is copied only when it reads exactly high or low. Any other level makes the
 * stored address undefined, and an enabled device then drives every output to an error. An enable
 * pin is active only at its exact active level, so an unconnected device keeps every output low.
 */
public class Ttl74237 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74237";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  public static final byte A0 = 1;
  public static final byte A1 = 2;
  public static final byte A2 = 3;
  public static final byte LE = 4;
  public static final byte E1 = 5;
  public static final byte E2 = 6;

  public static final byte Y7 = 7;
  public static final byte Y6 = 9;
  public static final byte Y5 = 10;
  public static final byte Y4 = 11;
  public static final byte Y3 = 12;
  public static final byte Y2 = 13;
  public static final byte Y1 = 14;
  public static final byte Y0 = 15;

  public static final byte GND = 8;
  public static final byte VCC = 16;

  /** Address inputs, with the least significant bit first. */
  private static final byte[] ADDRESS = {A0, A1, A2};

  /** Outputs in the order of the address they decode. */
  private static final byte[] OUTPUTS = {Y0, Y1, Y2, Y3, Y4, Y5, Y6, Y7};

  private static final String[] PORT_NAMES = {
    "A0 Address (LSB)",
    "A1 Address",
    "A2 Address (MSB)",
    "nLE Latch enable (active low)",
    "nE1 Output enable (active low)",
    "E2 Output enable (active high)",
    "Y7",
    "Y6",
    "Y5",
    "Y4",
    "Y3",
    "Y2",
    "Y1",
    "Y0"
  };

  /** Creates a 74237 3-to-8 line decoder/demultiplexer with address latches. */
  public Ttl74237() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74237HdlGenerator());
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
          "A0", "A1", "A2", "nLE", "nE1", "E2", "Y7", null,
          "Y6", "Y5", "Y4", "Y3", "Y2", "Y1", "Y0", null
        });
  }

  private static Value definedLevel(InstanceState state, byte dsPinNr) {
    final var value = state.getPortValue(pinNrToPortNr(dsPinNr));
    if (value == Value.TRUE || value == Value.FALSE) {
      return value;
    }
    return Value.ERROR;
  }

  private static boolean isEnabled(InstanceState state) {
    return state.getPortValue(pinNrToPortNr(E1)) == Value.FALSE
        && state.getPortValue(pinNrToPortNr(E2)) == Value.TRUE;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.create(ADDRESS.length));
      state.setData(data);
    }

    // Sample only while nLE is low. A propagation that already sees nLE high therefore keeps
    // the address stored on the previous propagation, which is the value present before the edge.
    if (state.getPortValue(pinNrToPortNr(LE)) == Value.FALSE) {
      final var bits = new Value[ADDRESS.length];
      for (var index = 0; index < ADDRESS.length; index++) {
        bits[index] = definedLevel(state, ADDRESS[index]);
      }
      data.setValue(Value.create(bits));
    }

    final var address = data.getValue();
    final var enabled = isEnabled(state);
    final var code = address.isFullyDefined() ? (int) address.toLongValue() : -1;
    for (var index = 0; index < OUTPUTS.length; index++) {
      final Value output;
      if (!enabled) {
        output = Value.FALSE;
      } else if (code < 0) {
        output = Value.ERROR;
      } else {
        output = index == code ? Value.TRUE : Value.FALSE;
      }
      state.setPort(pinNrToPortNr(OUTPUTS[index]), output, DELAY);
    }
  }
}
