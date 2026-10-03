/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74HC258: quad 2-line to 1-line data selector with three-state inverted outputs.
 *
 * <p>Simulation follows the Nexperia/NXP 74HC258 function table. A low {@code S} selects {@code
 * nI0} and a high {@code S} selects {@code nI1}. The selected bit is inverted onto {@code nY}. A
 * high {@code OE} turns every output off. TI SN74LS258 uses the same DIP-16 pins and calls the data
 * inputs {@code A}/{@code B} and the output enable {@code G}.
 *
 * <p>Logisim has no separate high-impedance value, so a released output is {@link Value#UNKNOWN},
 * as it is for the 74125. {@link Value#not()} turns an unknown bit into an error, so inversion is
 * done here: true and false swap, an error stays an error, and every other value becomes unknown.
 * An output enable that is not exactly low does not pass an input error through. An unknown select
 * still has a result when both inverted sources agree; if either possible result is an error, the
 * output is an error. Nanosecond delays are not modeled. FPGA download rejects this component
 * because its outputs are three-state.
 */
public class Ttl74258 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74258";

  public static final int DELAY = 1;

  public static final byte S = 1;
  public static final byte I0_1 = 2;
  public static final byte I1_1 = 3;
  public static final byte Y1 = 4;
  public static final byte I0_2 = 5;
  public static final byte I1_2 = 6;
  public static final byte Y2 = 7;
  public static final byte GND = 8;
  public static final byte Y3 = 9;
  public static final byte I1_3 = 10;
  public static final byte I0_3 = 11;
  public static final byte Y4 = 12;
  public static final byte I1_4 = 13;
  public static final byte I0_4 = 14;
  /** Output enable, active low. TI calls this pin G. */
  public static final byte OE = 15;
  public static final byte VCC = 16;

  static final byte[] I0 = {I0_1, I0_2, I0_3, I0_4};
  static final byte[] I1 = {I1_1, I1_2, I1_3, I1_4};
  static final byte[] Y = {Y1, Y2, Y3, Y4};

  private static final byte[] OUTPUTS = {Y1, Y2, Y3, Y4};
  private static final String[] PORT_NAMES = {
    "S select (TI SELECT)",
    "1I0 / 1A data from source 0",
    "1I1 / 1B data from source 1",
    "1Y inverted output",
    "2I0 / 2A data from source 0",
    "2I1 / 2B data from source 1",
    "2Y inverted output",
    "3Y inverted output",
    "3I1 / 3B data from source 1",
    "3I0 / 3A data from source 0",
    "4Y inverted output",
    "4I1 / 4B data from source 1",
    "4I0 / 4A data from source 0",
    "OE / G (output enable, active LOW)"
  };

  /** Creates a 74258 quad inverting selector with three-state outputs. */
  public Ttl74258() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74258HdlGenerator());
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
  public boolean hasThreeStateDrivers(AttributeSet attrs) {
    return true;
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
          "S", "1I0", "1I1", "1Y", "2I0", "2I1", "2Y", null,
          "3Y", "3I1", "3I0", "4Y", "4I1", "4I0", "OE", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var outputEnable = input(state, OE);
    if (outputEnable == Value.TRUE) {
      publish(state, Value.UNKNOWN);
      return;
    }
    if (outputEnable != Value.FALSE) {
      publish(state, outputEnable == Value.ERROR ? Value.ERROR : Value.UNKNOWN);
      return;
    }
    final var select = input(state, S);
    for (var channel = 0; channel < Y.length; channel++) {
      final var result =
          channelOutput(select, input(state, I0[channel]), input(state, I1[channel]));
      state.setPort(pinNrToPortNr(Y[channel]), result, DELAY);
    }
  }

  private static Value channelOutput(Value select, Value source0, Value source1) {
    final var fromSource0 = invert(source0);
    final var fromSource1 = invert(source1);
    if (select == Value.FALSE) return fromSource0;
    if (select == Value.TRUE) return fromSource1;
    if (select == Value.ERROR) return Value.ERROR;
    if (fromSource0 == fromSource1) return fromSource0;
    if (fromSource0 == Value.ERROR || fromSource1 == Value.ERROR) return Value.ERROR;
    return Value.UNKNOWN;
  }

  private static Value invert(Value value) {
    if (value == Value.TRUE) return Value.FALSE;
    if (value == Value.FALSE) return Value.TRUE;
    if (value == Value.ERROR) return Value.ERROR;
    return Value.UNKNOWN;
  }

  private static void publish(InstanceState state, Value value) {
    for (final var output : Y) {
      state.setPort(pinNrToPortNr(output), value, DELAY);
    }
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }
}
