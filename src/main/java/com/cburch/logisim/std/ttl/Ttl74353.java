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
 * TTL 74x353: dual 4-line to 1-line data selector with 3-state inverted outputs.
 *
 * <p>Simulation follows the ST M54/M74HC353 function table. The same DIP-16 pins are used by
 * Hitachi HD74HC353 and 74LS353. A low output strobe inverts the selected source onto Y. A high
 * strobe releases that multiplexer only. The two select inputs are shared: B is the MSB and A is
 * the LSB. Pin names follow ST (1G/2G, B/A and 1C0..2C3). Nexperia 74HC253 uses the same pins
 * with non-inverted outputs. Nanosecond delays are not modeled.
 *
 * <p>An unknown or error input changes an output only when the accepted substitutions disagree. An
 * error on such an input makes the disagreed output an error; an unknown input makes it unknown.
 * Inversion swaps true and false, leaves an error as an error, and turns every other value
 * unknown. {@link Value#not()} is not used, because it turns an unknown bit into an error. A high
 * output strobe releases its output even if a data input is an error. A released output is {@link
 * Value#UNKNOWN}, which is how this library represents high impedance. FPGA download rejects this
 * component because its outputs are three-state.
 */
public class Ttl74353 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74353";

  public static final int DELAY = 1;

  /** Output strobe for the first multiplexer. High releases 1Y. ST calls this 1G. */
  public static final byte G1 = 1;

  /** Shared select, MSB. ST calls this B. */
  public static final byte B = 2;

  public static final byte C1_3 = 3;
  public static final byte C1_2 = 4;
  public static final byte C1_1 = 5;
  public static final byte C1_0 = 6;
  public static final byte Y1 = 7;
  public static final byte GND = 8;
  public static final byte Y2 = 9;
  public static final byte C2_0 = 10;
  public static final byte C2_1 = 11;
  public static final byte C2_2 = 12;
  public static final byte C2_3 = 13;

  /** Shared select, LSB. ST calls this A. */
  public static final byte A = 14;

  /** Output strobe for the second multiplexer. High releases 2Y. ST calls this 2G. */
  public static final byte G2 = 15;

  public static final byte VCC = 16;

  /** Each row is C0, C1, C2, C3, the output, and the output strobe of one multiplexer. */
  private static final byte[][] CHANNELS = {
    {C1_0, C1_1, C1_2, C1_3, Y1, G1},
    {C2_0, C2_1, C2_2, C2_3, Y2, G2}
  };

  private static final byte[] OUTPUTS = {Y1, Y2};

  private static final String[] PORT_NAMES = {
    "1G (Output strobe, active low)",
    "B (Select, MSB)",
    "1C3",
    "1C2",
    "1C1",
    "1C0",
    "1Y (inverted)",
    "2Y (inverted)",
    "2C0",
    "2C1",
    "2C2",
    "2C3",
    "A (Select, LSB)",
    "2G (Output strobe, active low)"
  };

  /** Creates a 74353 dual 4-line to 1-line data selector with 3-state inverted outputs. */
  public Ttl74353() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74353HdlGenerator());
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
          "1G", "B", "1C3", "1C2", "1C1", "1C0", "1Y", null,
          "2Y", "2C0", "2C1", "2C2", "2C3", "A", "2G", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var selectMsb = state.getPortValue(pinNrToPortNr(B));
    final var selectLsb = state.getPortValue(pinNrToPortNr(A));
    for (final var channel : CHANNELS) {
      final var sources = new Value[4];
      for (var index = 0; index < sources.length; index++) {
        sources[index] = state.getPortValue(pinNrToPortNr(channel[index]));
      }
      state.setPort(
          pinNrToPortNr(channel[4]),
          channelOutput(state.getPortValue(pinNrToPortNr(channel[5])), selectMsb, selectLsb, sources),
          DELAY);
    }
  }

  /**
   * Resolves one multiplexer output, including unknown and error substitutions.
   *
   * <p>A definitely high output strobe releases the pin and does not consult the data inputs. A
   * low strobe drives the inverted selected source.
   */
  private static Value channelOutput(Value outputEnable, Value selectMsb, Value selectLsb, Value[] sources) {
    if (outputEnable == Value.TRUE) {
      return Value.UNKNOWN;
    }
    final var driven = invert(selected(selectMsb, selectLsb, sources));
    if (outputEnable == Value.FALSE) {
      return driven;
    }
    if (driven == Value.UNKNOWN && outputEnable == Value.UNKNOWN) {
      return Value.UNKNOWN;
    }
    if (outputEnable == Value.ERROR || driven == Value.ERROR) {
      return Value.ERROR;
    }
    return Value.UNKNOWN;
  }

  /**
   * Returns the source chosen by the select inputs, before output inversion.
   *
   * <p>When a select input is not binary, every source it could still address is a candidate. The
   * candidates' common value is the result. Disagreement is an error if a select input or a
   * candidate is an error, and unknown otherwise.
   */
  private static Value selected(Value selectMsb, Value selectLsb, Value[] sources) {
    if (selectMsb == Value.FALSE && selectLsb == Value.FALSE) {
      return sources[0];
    }
    if (selectMsb == Value.FALSE && selectLsb == Value.TRUE) {
      return sources[1];
    }
    if (selectMsb == Value.TRUE && selectLsb == Value.FALSE) {
      return sources[2];
    }
    if (selectMsb == Value.TRUE && selectLsb == Value.TRUE) {
      return sources[3];
    }

    final var possible = new boolean[] {
      selectMsb != Value.TRUE && selectLsb != Value.TRUE,
      selectMsb != Value.TRUE && selectLsb != Value.FALSE,
      selectMsb != Value.FALSE && selectLsb != Value.TRUE,
      selectMsb != Value.FALSE && selectLsb != Value.FALSE
    };
    Value agreed = null;
    var differ = false;
    var sawError = selectMsb == Value.ERROR || selectLsb == Value.ERROR;
    for (var index = 0; index < sources.length; index++) {
      if (!possible[index]) {
        continue;
      }
      final var source = sources[index];
      if (source == Value.ERROR) {
        sawError = true;
      }
      if (agreed == null) {
        agreed = source;
      } else if (source != agreed) {
        differ = true;
      }
    }
    if (agreed == null || !differ) {
      return agreed == null ? Value.UNKNOWN : agreed;
    }
    return sawError ? Value.ERROR : Value.UNKNOWN;
  }

  /**
   * Inverts a driven level without turning unknown into error.
   *
   * @param value selected source
   * @return inverted source
   */
  private static Value invert(Value value) {
    if (value == Value.TRUE) return Value.FALSE;
    if (value == Value.FALSE) return Value.TRUE;
    if (value == Value.ERROR) return Value.ERROR;
    return Value.UNKNOWN;
  }
}
