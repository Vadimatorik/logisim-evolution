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
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * TTL 74HC543: octal registered transceiver with three-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74f543.pdf">TI SN74F543</a> function table. NXP's
 * 74F543 and the 74HC/HCT543 use the same DIP-24 pinout and the same latch rules. Pin names here
 * are {@code A0} to {@code A7}. TI numbers the same pins {@code A1} to {@code A8}.
 *
 * <p>Each direction has its own transparent latch. The A-to-B latch follows A while {@code nCEAB}
 * and {@code nLEAB} are both low, and keeps that word when either input rises. {@code nCEBA} and
 * {@code nLEBA} do the same for the B-to-A latch. B is driven only while {@code nCEAB} and {@code
 * nOEAB} are both low; A is driven only while {@code nCEBA} and {@code nOEBA} are both low. A
 * control that is neither high nor low releases that direction and leaves its latch unchanged.
 * High impedance is reported as unknown. A live bit that is not 0 or 1 becomes an error on the
 * driven bus.
 *
 * <p>The latch enables are level-sensitive, so they are not Logisim clocks. If both latches are
 * transparent and both buses are driven, the latches are left unchanged: each would otherwise copy
 * the other on every propagation. A stored word is still driven. There is no external driver in
 * that loop, matching the limit noted for the 74652 real-time paths.
 */
public class Ttl74543 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74543";

  public static final int PORT_INDEX_nLEBA = 0;
  public static final int PORT_INDEX_nOEBA = 1;
  public static final int PORT_INDEX_A0 = 2;
  public static final int PORT_INDEX_A1 = 3;
  public static final int PORT_INDEX_A2 = 4;
  public static final int PORT_INDEX_A3 = 5;
  public static final int PORT_INDEX_A4 = 6;
  public static final int PORT_INDEX_A5 = 7;
  public static final int PORT_INDEX_A6 = 8;
  public static final int PORT_INDEX_A7 = 9;
  public static final int PORT_INDEX_nCEAB = 10;
  public static final int PORT_INDEX_nOEAB = 11;
  public static final int PORT_INDEX_nLEAB = 12;
  public static final int PORT_INDEX_B7 = 13;
  public static final int PORT_INDEX_B6 = 14;
  public static final int PORT_INDEX_B5 = 15;
  public static final int PORT_INDEX_B4 = 16;
  public static final int PORT_INDEX_B3 = 17;
  public static final int PORT_INDEX_B2 = 18;
  public static final int PORT_INDEX_B1 = 19;
  public static final int PORT_INDEX_B0 = 20;
  public static final int PORT_INDEX_nCEBA = 21;
  /** Port index of GND when the explicit power pins are shown. */
  public static final int PORT_INDEX_GND = 22;
  /** Port index of VCC when the explicit power pins are shown. */
  public static final int PORT_INDEX_VCC = 23;

  private static final int DELAY = 1;
  private static final int BITS = 8;
  private static final int LATCH_AB = 0;
  private static final int LATCH_BA = 1;
  private static final BitWidth WIDTH = BitWidth.create(BITS);
  /** A-bus port of each bit. Bit 0 is A0. */
  private static final int[] BUS_A = {
    PORT_INDEX_A0,
    PORT_INDEX_A1,
    PORT_INDEX_A2,
    PORT_INDEX_A3,
    PORT_INDEX_A4,
    PORT_INDEX_A5,
    PORT_INDEX_A6,
    PORT_INDEX_A7
  };
  /** B-bus port of each bit. Bit 0 is B0, on physical pin 22. */
  private static final int[] BUS_B = {
    PORT_INDEX_B0,
    PORT_INDEX_B1,
    PORT_INDEX_B2,
    PORT_INDEX_B3,
    PORT_INDEX_B4,
    PORT_INDEX_B5,
    PORT_INDEX_B6,
    PORT_INDEX_B7
  };
  private static final byte[] INOUT_PINS = {3, 4, 5, 6, 7, 8, 9, 10, 15, 16, 17, 18, 19, 20, 21, 22};
  private static final String[] PORT_NAMES = {
    "nLEBA (B-to-A latch enable, active low)",
    "nOEBA (B-to-A output enable, active low)",
    "A0",
    "A1",
    "A2",
    "A3",
    "A4",
    "A5",
    "A6",
    "A7",
    "nCEAB (A-to-B enable, active low)",
    "nOEAB (A-to-B output enable, active low)",
    "nLEAB (A-to-B latch enable, active low)",
    "B7",
    "B6",
    "B5",
    "B4",
    "B3",
    "B2",
    "B1",
    "B0",
    "nCEBA (B-to-A enable, active low)"
  };
  private static final String[] PIN_NAMES = {
    "nLEBA", "nOEBA", "A0", "A1", "A2", "A3", "A4", "A5", "A6", "A7", "nCEAB", null,
    "nOEAB", "nLEAB", "B7", "B6", "B5", "B4", "B3", "B2", "B1", "B0", "nCEBA", null
  };

  /** Creates a 74543 octal registered transceiver. */
  public Ttl74543() {
    super(_ID, (byte) 24, new byte[] {}, new byte[] {}, INOUT_PINS, PORT_NAMES, new Ttl74543HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var gfx = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, false, false);
    Drawgates.paintPortNamesByPin(painter, x, y, height, PIN_NAMES);
    drawLatches(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private static void drawLatches(Graphics2D gfx, int x, int y, int height, TtlRegisterData data) {
    if (data == null) return;
    drawWord(gfx, "AB", data.getValue(LATCH_AB), x, y + height / 2 - 10);
    drawWord(gfx, "BA", data.getValue(LATCH_BA), x, y + height / 2 + 2);
    gfx.setColor(Color.BLACK);
  }

  private static void drawWord(Graphics2D gfx, String name, Value word, int x, int originY) {
    gfx.setColor(Color.BLACK);
    GraphicsUtil.drawCenteredText(gfx, name, x + 48, originY + 4);
    for (var bit = BITS - 1; bit >= 0; bit--) {
      final var shown = word.get(bit);
      final var dotX = x + 64 + (BITS - 1 - bit) * 14;
      gfx.setColor(shown.getColor());
      gfx.fillOval(dotX, originY, 8, 8);
      gfx.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(gfx, shown.toDisplayString(), dotX + 4, originY + 4);
    }
  }

  /**
   * Releases both buses when the exposed supply pins are not a valid ground and positive rail.
   * The base implementation only clears output ports, and these data pins are bidirectional.
   */
  @Override
  public void propagate(InstanceState state) {
    if (exposedPowerIsInvalid(state)) {
      release(state, BUS_A);
      release(state, BUS_B);
      return;
    }
    propagateTtl(state);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var externalA = readBus(state, BUS_A);
    final var externalB = readBus(state, BUS_B);
    final var nCeAb = state.getPortValue(PORT_INDEX_nCEAB);
    final var nLeAb = state.getPortValue(PORT_INDEX_nLEAB);
    final var nOeAb = state.getPortValue(PORT_INDEX_nOEAB);
    final var nCeBa = state.getPortValue(PORT_INDEX_nCEBA);
    final var nLeBa = state.getPortValue(PORT_INDEX_nLEBA);
    final var nOeBa = state.getPortValue(PORT_INDEX_nOEBA);
    final var data = latches(state);
    final var storedAb = data.getValue(LATCH_AB);
    final var storedBa = data.getValue(LATCH_BA);
    final var openAb = isLow(nCeAb) && isLow(nLeAb);
    final var openBa = isLow(nCeBa) && isLow(nLeBa);
    final var driveB = isLow(nCeAb) && isLow(nOeAb) && isLevel(nLeAb);
    final var driveA = isLow(nCeBa) && isLow(nOeBa) && isLevel(nLeBa);
    updateLatches(data, storedAb, storedBa, externalA, externalB, openAb, openBa, driveA, driveB);
    if (driveB) drive(state, BUS_B, data.getValue(LATCH_AB));
    else release(state, BUS_B);
    if (driveA) drive(state, BUS_A, data.getValue(LATCH_BA));
    else release(state, BUS_A);
  }

  /**
   * Copies an open latch from its bus. A bus this chip is driving contributes the opposite latch
   * instead of the value just written, and both directions are held when that would swap the two
   * words.
   */
  private static void updateLatches(
      TtlRegisterData data,
      Value storedAb,
      Value storedBa,
      Value[] externalA,
      Value[] externalB,
      boolean openAb,
      boolean openBa,
      boolean driveA,
      boolean driveB) {
    if (openAb && openBa && driveA && driveB) return;
    if (openAb && !driveA) data.setValue(LATCH_AB, Value.create(copied(externalA)));
    if (openBa && !driveB) data.setValue(LATCH_BA, Value.create(copied(externalB)));
    if (openAb && driveA) {
      final var source = openBa && !driveB ? data.getValue(LATCH_BA) : storedBa;
      data.setValue(LATCH_AB, source);
    }
    if (openBa && driveB) {
      final var source = openAb && !driveA ? data.getValue(LATCH_AB) : storedAb;
      data.setValue(LATCH_BA, source);
    }
  }

  private static boolean exposedPowerIsInvalid(InstanceState state) {
    if (!state.getAttributeValue(TtlLibrary.VCC_GND)) return false;
    return state.getPortValue(PORT_INDEX_GND) != Value.FALSE
        || state.getPortValue(PORT_INDEX_VCC) != Value.TRUE;
  }

  private static boolean isLow(Value value) {
    return value == Value.FALSE;
  }

  private static boolean isLevel(Value value) {
    return value == Value.TRUE || value == Value.FALSE;
  }

  private static Value[] readBus(InstanceState state, int[] ports) {
    final var bits = new Value[BITS];
    for (var bit = 0; bit < BITS; bit++) {
      bits[bit] = storedBit(state.getPortValue(ports[bit]));
    }
    return bits;
  }

  /** Keeps a single logic level, unknown or error. Anything else is an error. */
  private static Value storedBit(Value source) {
    if (source == Value.TRUE
        || source == Value.FALSE
        || source == Value.UNKNOWN
        || source == Value.ERROR) {
      return source;
    }
    return Value.ERROR;
  }

  /** A live source that is not 0 or 1 becomes an error on the driven bus. */
  private static Value[] copied(Value[] live) {
    final var result = new Value[BITS];
    for (var bit = 0; bit < BITS; bit++) {
      result[bit] = live[bit] == Value.TRUE || live[bit] == Value.FALSE ? live[bit] : Value.ERROR;
    }
    return result;
  }

  private static TtlRegisterData latches(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WIDTH, 2);
      state.setData(data);
    }
    return data;
  }

  private static void drive(InstanceState state, int[] ports, Value word) {
    for (var bit = 0; bit < BITS; bit++) {
      state.setPort(ports[bit], word.get(bit), DELAY);
    }
  }

  private static void release(InstanceState state, int[] ports) {
    for (final var port : ports) {
      state.setPort(port, Value.UNKNOWN, DELAY);
    }
  }
}
