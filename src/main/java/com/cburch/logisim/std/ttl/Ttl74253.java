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
 * TTL 74x253: dual 4-line to 1-line data selector with 3-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT253.pdf">Nexperia 74HC253</a>
 * and <a href="https://www.ti.com/lit/ds/symlink/sn74hc253.pdf">TI SN74HC253</a> function tables.
 * A low output enable passes the selected source without inversion. A high output enable releases
 * that multiplexer only. The two select inputs are shared: S1 is the MSB and S0 is the LSB. Pin
 * names follow Nexperia. TI calls the same pins 1G/2G, B/A and 1C0..2C3. Nanosecond delays are not
 * modeled.
 *
 * <p>An unknown or error input changes an output only when the accepted substitutions disagree. An
 * error on such an input makes the disagreed output an error; an unknown input makes it unknown. A
 * high output enable releases its output even if a data input is an error. A released output is
 * {@link Value#UNKNOWN}, which is how this library represents high impedance.
 */
public class Ttl74253 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74253";

  public static final int DELAY = 1;

  public static final byte OE1 = 1;
  public static final byte S1 = 2;
  public static final byte L1_I3 = 3;
  public static final byte L1_I2 = 4;
  public static final byte L1_I1 = 5;
  public static final byte L1_I0 = 6;
  public static final byte L1_Y = 7;
  public static final byte GND = 8;
  public static final byte L2_Y = 9;
  public static final byte L2_I0 = 10;
  public static final byte L2_I1 = 11;
  public static final byte L2_I2 = 12;
  public static final byte L2_I3 = 13;
  public static final byte S0 = 14;
  public static final byte OE2 = 15;
  public static final byte VCC = 16;

  /** Each row is I0, I1, I2, I3, the output, and the output enable of one multiplexer. */
  private static final byte[][] CHANNELS = {
    {L1_I0, L1_I1, L1_I2, L1_I3, L1_Y, OE1},
    {L2_I0, L2_I1, L2_I2, L2_I3, L2_Y, OE2}
  };

  private static final byte[] OUTPUTS = {L1_Y, L2_Y};

  private static final String[] PORT_NAMES = {
    "n1OE (Output enable, active low)",
    "S1 (Select, MSB)",
    "1I3",
    "1I2",
    "1I1",
    "1I0",
    "1Y",
    "2Y",
    "2I0",
    "2I1",
    "2I2",
    "2I3",
    "S0 (Select, LSB)",
    "n2OE (Output enable, active low)"
  };

  /** Creates a 74253 dual 4-line to 1-line data selector with 3-state outputs. */
  public Ttl74253() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74253HdlGenerator());
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
          "n1OE", "S1", "1I3", "1I2", "1I1", "1I0", "1Y", null,
          "2Y", "2I0", "2I1", "2I2", "2I3", "S0", "n2OE", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var s1 = state.getPortValue(pinNrToPortNr(S1));
    final var s0 = state.getPortValue(pinNrToPortNr(S0));
    for (final var channel : CHANNELS) {
      final var sources = new Value[4];
      for (var index = 0; index < sources.length; index++) {
        sources[index] = state.getPortValue(pinNrToPortNr(channel[index]));
      }
      state.setPort(
          pinNrToPortNr(channel[4]),
          channelOutput(state.getPortValue(pinNrToPortNr(channel[5])), s1, s0, sources),
          DELAY);
    }
  }

  /**
   * Resolves one multiplexer output, including unknown and error substitutions.
   *
   * <p>A definitely high output enable releases the pin and does not consult the data inputs.
   */
  private static Value channelOutput(Value outputEnable, Value s1, Value s0, Value[] sources) {
    if (outputEnable == Value.TRUE) {
      return Value.UNKNOWN;
    }
    final var driven = selected(s1, s0, sources);
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
   * Returns the source chosen by the select inputs.
   *
   * <p>When a select input is not binary, every source it could still address is a candidate. The
   * candidates' common value is the output. Disagreement is an error if a select input or a
   * candidate is an error, and unknown otherwise.
   */
  private static Value selected(Value s1, Value s0, Value[] sources) {
    if (s1 == Value.FALSE && s0 == Value.FALSE) {
      return sources[0];
    }
    if (s1 == Value.FALSE && s0 == Value.TRUE) {
      return sources[1];
    }
    if (s1 == Value.TRUE && s0 == Value.FALSE) {
      return sources[2];
    }
    if (s1 == Value.TRUE && s0 == Value.TRUE) {
      return sources[3];
    }

    final var possible = new boolean[] {
      s1 != Value.TRUE && s0 != Value.TRUE,
      s1 != Value.TRUE && s0 != Value.FALSE,
      s1 != Value.FALSE && s0 != Value.TRUE,
      s1 != Value.FALSE && s0 != Value.FALSE
    };
    Value agreed = null;
    var differ = false;
    var sawError = s1 == Value.ERROR || s0 == Value.ERROR;
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
}
