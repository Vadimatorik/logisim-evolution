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
 * TTL 74HC645: octal non-inverting bus transceiver with three-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74hc645.pdf">TI SN74HC645</a> and
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT645.pdf">Nexperia 74HC645</a>
 * data sheets. A low {@code nOE} and a high {@code DIR} drive each B pin with the paired A pin. A
 * low {@code nOE} and a low {@code DIR} drive each A pin with the paired B pin. A high {@code nOE},
 * or a control that is neither high nor low, releases both buses. High impedance is reported as
 * unknown. The part is the non-inverting counterpart of the 74HC640 and matches the function of
 * the 74HC245. Pin names follow the TI numbering, where A1 is Nexperia's A0.
 */
public class Ttl74645 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74645";

  public static final int PORT_INDEX_DIR = 0;
  public static final int PORT_INDEX_A1 = 1;
  public static final int PORT_INDEX_A2 = 2;
  public static final int PORT_INDEX_A3 = 3;
  public static final int PORT_INDEX_A4 = 4;
  public static final int PORT_INDEX_A5 = 5;
  public static final int PORT_INDEX_A6 = 6;
  public static final int PORT_INDEX_A7 = 7;
  public static final int PORT_INDEX_A8 = 8;
  public static final int PORT_INDEX_B8 = 9;
  public static final int PORT_INDEX_B7 = 10;
  public static final int PORT_INDEX_B6 = 11;
  public static final int PORT_INDEX_B5 = 12;
  public static final int PORT_INDEX_B4 = 13;
  public static final int PORT_INDEX_B3 = 14;
  public static final int PORT_INDEX_B2 = 15;
  public static final int PORT_INDEX_B1 = 16;
  public static final int PORT_INDEX_nOE = 17;
  /** Port index of GND when the explicit power pins are shown. */
  public static final int PORT_INDEX_GND = 18;
  /** Port index of VCC when the explicit power pins are shown. */
  public static final int PORT_INDEX_VCC = 19;

  private static final int DELAY = 1;
  private static final int BITS = 8;
  /** A-bus port of each bit. Bit 0 is A1, which shares a channel with B1. */
  private static final int[] BUS_A = {
    PORT_INDEX_A1,
    PORT_INDEX_A2,
    PORT_INDEX_A3,
    PORT_INDEX_A4,
    PORT_INDEX_A5,
    PORT_INDEX_A6,
    PORT_INDEX_A7,
    PORT_INDEX_A8
  };
  /** B-bus port of each bit. Bit 0 is B1, on physical pin 18. */
  private static final int[] BUS_B = {
    PORT_INDEX_B1,
    PORT_INDEX_B2,
    PORT_INDEX_B3,
    PORT_INDEX_B4,
    PORT_INDEX_B5,
    PORT_INDEX_B6,
    PORT_INDEX_B7,
    PORT_INDEX_B8
  };
  private static final byte[] INOUT_PINS = {
    2, 3, 4, 5, 6, 7, 8, 9, 11, 12, 13, 14, 15, 16, 17, 18
  };
  private static final String[] PORT_NAMES = {
    "DIR (direction, HIGH sends A to B)",
    "A1",
    "A2",
    "A3",
    "A4",
    "A5",
    "A6",
    "A7",
    "A8",
    "B8",
    "B7",
    "B6",
    "B5",
    "B4",
    "B3",
    "B2",
    "B1",
    "nOE (output enable, active LOW)"
  };

  /** Creates a 74645 octal non-inverting bus transceiver. */
  public Ttl74645() {
    super(_ID, (byte) 20, new byte[] {}, new byte[] {}, INOUT_PINS, PORT_NAMES,
        new Ttl74645HdlGenerator());
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
          "DIR", "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", null,
          "B8", "B7", "B6", "B5", "B4", "B3", "B2", "B1", "nOE", null
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
    final var enabled = state.getPortValue(PORT_INDEX_nOE) == Value.FALSE;
    final var direction = state.getPortValue(PORT_INDEX_DIR);
    if (enabled && direction == Value.TRUE) {
      drive(state, BUS_B, fromA);
      release(state, BUS_A);
    } else if (enabled && direction == Value.FALSE) {
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
