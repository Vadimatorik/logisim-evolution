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
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * TTL 74HC646: octal bus transceiver and register with three-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT646_CNV.pdf">Nexperia
 * 74HC/HCT646</a> function table. Pin names are Nexperia's: {@code CPAB}, {@code CPBA} and
 * {@code A0} to {@code A7}. TI's SN74HC646 uses {@code CLKAB}, {@code CLKBA} and {@code A1} to
 * {@code A8} for the same pins.
 *
 * <p>A low {@code nOE} and a high {@code DIR} drive B from A. A low {@code nOE} and a low {@code
 * DIR} drive A from B. {@code SAB} and {@code SBA} choose the stored register instead of the live
 * opposite bus. A high {@code nOE} releases both buses. A control that is neither high nor low, or
 * an unknown select on the active direction, releases both buses so the model does not fight the
 * wire. High impedance is reported as unknown.
 *
 * <p>Each register stores the value on its own pins at the rising edge of its clock, whether or
 * not that bus is driving. The sampled output is the value computed from the registers before the
 * edge, so a clock does not capture the word it has just written. There is no HDL model: the FPGA
 * design-rule check rejects three-state drivers.
 */
public class Ttl74646 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74646";

  public static final int PORT_INDEX_CPAB = 0;
  public static final int PORT_INDEX_SAB = 1;
  public static final int PORT_INDEX_DIR = 2;
  public static final int PORT_INDEX_A0 = 3;
  public static final int PORT_INDEX_A1 = 4;
  public static final int PORT_INDEX_A2 = 5;
  public static final int PORT_INDEX_A3 = 6;
  public static final int PORT_INDEX_A4 = 7;
  public static final int PORT_INDEX_A5 = 8;
  public static final int PORT_INDEX_A6 = 9;
  public static final int PORT_INDEX_A7 = 10;
  public static final int PORT_INDEX_B7 = 11;
  public static final int PORT_INDEX_B6 = 12;
  public static final int PORT_INDEX_B5 = 13;
  public static final int PORT_INDEX_B4 = 14;
  public static final int PORT_INDEX_B3 = 15;
  public static final int PORT_INDEX_B2 = 16;
  public static final int PORT_INDEX_B1 = 17;
  public static final int PORT_INDEX_B0 = 18;
  public static final int PORT_INDEX_nOE = 19;
  public static final int PORT_INDEX_SBA = 20;
  public static final int PORT_INDEX_CPBA = 21;
  /** Port index of GND when the explicit power pins are shown. */
  public static final int PORT_INDEX_GND = 22;
  /** Port index of VCC when the explicit power pins are shown. */
  public static final int PORT_INDEX_VCC = 23;

  private static final int DELAY = 1;
  private static final int BITS = 8;
  private static final int REGISTER_A = 0;
  private static final int REGISTER_B = 1;
  private static final int CLOCK_AB = 0;
  private static final int CLOCK_BA = 1;
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
  /** B-bus port of each bit. Bit 0 is B0, on physical pin 20. */
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
  private static final byte[] INOUT_PINS = {
    4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15, 16, 17, 18, 19, 20
  };
  private static final String[] PORT_NAMES = {
    "CPAB (A-to-B clock, rising edge)",
    "SAB (HIGH selects stored A for the B bus)",
    "DIR (HIGH sends A to B)",
    "A0",
    "A1",
    "A2",
    "A3",
    "A4",
    "A5",
    "A6",
    "A7",
    "B7",
    "B6",
    "B5",
    "B4",
    "B3",
    "B2",
    "B1",
    "B0",
    "nOE (output enable, active LOW)",
    "SBA (HIGH selects stored B for the A bus)",
    "CPBA (B-to-A clock, rising edge)"
  };
  private static final String[] PIN_NAMES = {
    "CPAB", "SAB", "DIR", "A0", "A1", "A2", "A3", "A4", "A5", "A6", "A7", null,
    "B7", "B6", "B5", "B4", "B3", "B2", "B1", "B0", "nOE", "SBA", "CPBA", null
  };

  /** Creates a 74646 octal bus transceiver and register. */
  public Ttl74646() {
    super(_ID, (byte) 24, new byte[] {}, new byte[] {}, INOUT_PINS, PORT_NAMES, null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var gfx = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, false, false);
    Drawgates.paintPortNamesByPin(painter, x, y, height, PIN_NAMES);
    drawRegisters(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private static void drawRegisters(Graphics2D gfx, int x, int y, int height, TtlRegisterData data) {
    if (data == null) return;
    drawWord(gfx, "A", data.getValue(REGISTER_A), x, y + height / 2 - 10);
    drawWord(gfx, "B", data.getValue(REGISTER_B), x, y + height / 2 + 2);
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
    final var outputEnable = state.getPortValue(PORT_INDEX_nOE);
    final var direction = state.getPortValue(PORT_INDEX_DIR);
    final var selectAb = state.getPortValue(PORT_INDEX_SAB);
    final var selectBa = state.getPortValue(PORT_INDEX_SBA);
    final var data = registers(state);
    final var driveA = drivesA(outputEnable, direction, selectBa);
    final var driveB = drivesB(outputEnable, direction, selectAb);
    final var sampleA =
        driveA ? selected(selectBa, data.getValue(REGISTER_B), externalB) : externalA;
    final var sampleB =
        driveB ? selected(selectAb, data.getValue(REGISTER_A), externalA) : externalB;
    final var captureA =
        data.updateClock(state.getPortValue(PORT_INDEX_CPAB), CLOCK_AB, StdAttr.TRIG_RISING);
    final var captureB =
        data.updateClock(state.getPortValue(PORT_INDEX_CPBA), CLOCK_BA, StdAttr.TRIG_RISING);
    if (captureA) data.setValue(REGISTER_A, Value.create(sampleA));
    if (captureB) data.setValue(REGISTER_B, Value.create(sampleB));
    if (driveA) drive(state, BUS_A, selected(selectBa, data.getValue(REGISTER_B), externalB));
    else release(state, BUS_A);
    if (driveB) drive(state, BUS_B, selected(selectAb, data.getValue(REGISTER_A), externalA));
    else release(state, BUS_B);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_CPAB, PORT_INDEX_CPBA};
  }

  private static boolean exposedPowerIsInvalid(InstanceState state) {
    if (!state.getAttributeValue(TtlLibrary.VCC_GND)) return false;
    return state.getPortValue(PORT_INDEX_GND) != Value.FALSE
        || state.getPortValue(PORT_INDEX_VCC) != Value.TRUE;
  }

  /** B drives A only when the enable, direction and B-to-A select are all defined levels. */
  private static boolean drivesA(Value outputEnable, Value direction, Value selectBa) {
    return outputEnable == Value.FALSE && direction == Value.FALSE && isLevel(selectBa);
  }

  /** A drives B only when the enable, direction and A-to-B select are all defined levels. */
  private static boolean drivesB(Value outputEnable, Value direction, Value selectAb) {
    return outputEnable == Value.FALSE && direction == Value.TRUE && isLevel(selectAb);
  }

  private static boolean isLevel(Value value) {
    return value == Value.TRUE || value == Value.FALSE;
  }

  /**
   * Chooses stored or live bits. A live bit that is not 0 or 1 becomes an error. Stored bits are
   * kept, including unknown power-up state.
   */
  private static Value[] selected(Value select, Value stored, Value[] live) {
    final var result = new Value[BITS];
    for (var bit = 0; bit < BITS; bit++) {
      result[bit] = select == Value.TRUE ? stored.get(bit) : copiedBit(live[bit]);
    }
    return result;
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

  /** A source that is not 0 or 1 becomes an error on the driven bus. */
  private static Value copiedBit(Value source) {
    return source == Value.TRUE || source == Value.FALSE ? source : Value.ERROR;
  }

  private static TtlRegisterData registers(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WIDTH, 2);
      state.setData(data);
    }
    return data;
  }

  private static void drive(InstanceState state, int[] ports, Value[] bits) {
    for (var bit = 0; bit < BITS; bit++) {
      state.setPort(ports[bit], bits[bit], DELAY);
    }
  }

  private static void release(InstanceState state, int[] ports) {
    for (final var port : ports) {
      state.setPort(port, Value.UNKNOWN, DELAY);
    }
  }
}
