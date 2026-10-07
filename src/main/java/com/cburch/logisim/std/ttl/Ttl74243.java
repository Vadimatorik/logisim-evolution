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
 * TTL 74HC243: quad non-inverting bus transceiver with three-state outputs.
 *
 * <p>Simulation follows the Philips 74HC243 Rev. 03 (12 November 2004) and
 * <a href="https://www.ti.com/lit/ds/symlink/cd74hc243.pdf">TI CD74HC243</a> function tables.
 * Both enables low drive each B pin from the paired A pin. Both enables high drive each A pin
 * from the paired B pin. Different enable levels, or a control that is neither high nor low,
 * release both buses. High impedance is reported as unknown. The part is the non-inverting
 * counterpart of the 74HC242. Pin names follow Philips: {@code OEA} is active low only as one
 * half of that pair, not as an enable for the A outputs. Pins 2 and 12 are not connected.
 */
public class Ttl74243 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74243";

  public static final int PORT_INDEX_OEA = 0;
  public static final int PORT_INDEX_A0 = 1;
  public static final int PORT_INDEX_A1 = 2;
  public static final int PORT_INDEX_A2 = 3;
  public static final int PORT_INDEX_A3 = 4;
  public static final int PORT_INDEX_B3 = 5;
  public static final int PORT_INDEX_B2 = 6;
  public static final int PORT_INDEX_B1 = 7;
  public static final int PORT_INDEX_B0 = 8;
  public static final int PORT_INDEX_OEB = 9;
  /** Port index of GND when the explicit power pins are shown. */
  public static final int PORT_INDEX_GND = 10;
  /** Port index of VCC when the explicit power pins are shown. */
  public static final int PORT_INDEX_VCC = 11;

  private static final int DELAY = 1;
  private static final int BITS = 4;
  /** A-bus port of each bit. Bit 0 is A0, which shares a channel with B0. */
  private static final int[] BUS_A = {
    PORT_INDEX_A0, PORT_INDEX_A1, PORT_INDEX_A2, PORT_INDEX_A3
  };
  /** B-bus port of each bit. Bit 0 is B0, on physical pin 11. */
  private static final int[] BUS_B = {
    PORT_INDEX_B0, PORT_INDEX_B1, PORT_INDEX_B2, PORT_INDEX_B3
  };
  private static final byte[] UNUSED_PINS = {2, 12};
  private static final byte[] INOUT_PINS = {3, 4, 5, 6, 8, 9, 10, 11};
  private static final String[] PORT_NAMES = {
    "OEA (output enable for B, active low)",
    "A0",
    "A1",
    "A2",
    "A3",
    "B3",
    "B2",
    "B1",
    "B0",
    "OEB (output enable for A)"
  };

  /** Creates a 74243 quad non-inverting bus transceiver. */
  public Ttl74243() {
    super(
        _ID,
        (byte) 14,
        new byte[] {},
        UNUSED_PINS,
        INOUT_PINS,
        PORT_NAMES,
        new Ttl74243HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    Drawgates.paintPortNamesByPin(
        painter,
        x,
        y,
        height,
        new String[] {
          "OEA", null, "A0", "A1", "A2", "A3", null,
          "B3", "B2", "B1", "B0", null, "OEB", null
        });
  }

  /**
   * Releases both buses when the exposed supply pins are not a valid ground and positive rail.
   * The base implementation only clears output ports, and these data pins are bidirectional.
   */
  @Override
  public void propagate(InstanceState state) {
    if (exposedPowerIsInvalid(state)) {
      releaseBothBuses(state);
      return;
    }
    propagateTtl(state);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var fromA = new Value[BITS];
    final var fromB = new Value[BITS];
    for (var bit = 0; bit < BITS; bit++) {
      fromA[bit] = state.getPortValue(BUS_A[bit]);
      fromB[bit] = state.getPortValue(BUS_B[bit]);
    }
    final var outputEnableA = state.getPortValue(PORT_INDEX_OEA);
    final var outputEnableB = state.getPortValue(PORT_INDEX_OEB);
    if (outputEnableA == Value.FALSE && outputEnableB == Value.FALSE) {
      drive(state, BUS_B, fromA);
      release(state, BUS_A);
    } else if (outputEnableA == Value.TRUE && outputEnableB == Value.TRUE) {
      drive(state, BUS_A, fromB);
      release(state, BUS_B);
    } else {
      releaseBothBuses(state);
    }
  }

  private static boolean exposedPowerIsInvalid(InstanceState state) {
    if (!state.getAttributeValue(TtlLibrary.VCC_GND)) return false;
    return state.getPortValue(PORT_INDEX_GND) != Value.FALSE
        || state.getPortValue(PORT_INDEX_VCC) != Value.TRUE;
  }

  /** Drives {@code ports} from {@code sources}. A source that is not 0 or 1 becomes an error. */
  private static void drive(InstanceState state, int[] ports, Value[] sources) {
    for (var bit = 0; bit < BITS; bit++) {
      final var source = sources[bit];
      state.setPort(ports[bit], source.isFullyDefined() ? source : Value.ERROR, DELAY);
    }
  }

  private static void releaseBothBuses(InstanceState state) {
    release(state, BUS_A);
    release(state, BUS_B);
  }

  private static void release(InstanceState state, int[] ports) {
    for (final var port : ports) {
      state.setPort(port, Value.UNKNOWN, DELAY);
    }
  }
}
