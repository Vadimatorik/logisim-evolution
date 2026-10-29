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
import java.awt.Font;
import java.awt.Graphics;

/**
 * TTL 74HC652: octal bus transceivers and registers with three-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74hc652.pdf">TI SN74HC652</a> function table. Pin
 * names are TI's: {@code CLKAB}, {@code CLKBA} and {@code A1} to {@code A8}. Nexperia's 74HC652
 * uses {@code CPAB}, {@code CPBA} and {@code A0} to {@code A7} for the same pins.
 *
 * <p>A high {@code OEAB} drives B from A. A low {@code OEBA} drives A from B. Both directions can
 * be active together. {@code SAB} and {@code SBA} choose the stored register instead of the live
 * opposite bus. A control that is neither high nor low releases only the direction it belongs to.
 * High impedance is reported as unknown. A driven bit whose source is not 0 or 1 becomes an error.
 *
 * <p>Each register stores the value on its own pins at the rising edge of its clock, whether or
 * not that bus is driving. The sampled output is the value computed from the registers before the
 * edge, so a clock does not capture the word it has just written. The analog bus-hold that appears
 * when both real-time paths are enabled and every external driver releases is not modelled.
 */
public class Ttl74652 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be a unique string among all tools.
   */
  public static final String _ID = "74652";

  public static final int PORT_INDEX_CLKAB = 0;
  public static final int PORT_INDEX_SAB = 1;
  public static final int PORT_INDEX_OEAB = 2;
  public static final int PORT_INDEX_A1 = 3;
  public static final int PORT_INDEX_A2 = 4;
  public static final int PORT_INDEX_A3 = 5;
  public static final int PORT_INDEX_A4 = 6;
  public static final int PORT_INDEX_A5 = 7;
  public static final int PORT_INDEX_A6 = 8;
  public static final int PORT_INDEX_A7 = 9;
  public static final int PORT_INDEX_A8 = 10;
  public static final int PORT_INDEX_B8 = 11;
  public static final int PORT_INDEX_B7 = 12;
  public static final int PORT_INDEX_B6 = 13;
  public static final int PORT_INDEX_B5 = 14;
  public static final int PORT_INDEX_B4 = 15;
  public static final int PORT_INDEX_B3 = 16;
  public static final int PORT_INDEX_B2 = 17;
  public static final int PORT_INDEX_B1 = 18;
  public static final int PORT_INDEX_OEBA = 19;
  public static final int PORT_INDEX_SBA = 20;
  public static final int PORT_INDEX_CLKBA = 21;
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
  /** A-bus port of each bit. Bit 0 is A1. */
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
  /** B-bus port of each bit. Bit 0 is B1, on physical pin 20. */
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
    4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15, 16, 17, 18, 19, 20
  };
  private static final String[] PORT_NAMES = {
    "CLKAB (A-to-B clock, rising edge)",
    "SAB (HIGH selects stored A for the B bus)",
    "OEAB (HIGH enables A-to-B outputs)",
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
    "OEBA (LOW enables B-to-A outputs)",
    "SBA (HIGH selects stored B for the A bus)",
    "CLKBA (B-to-A clock, rising edge)"
  };
  private static final String[] PIN_NAMES = {
    "CLKAB", "SAB", "OEAB", "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", null,
    "B8", "B7", "B6", "B5", "B4", "B3", "B2", "B1", "OEBA", "SBA", "CLKBA", null
  };

  /** Creates a 74652 octal bus transceiver and registers. */
  public Ttl74652() {
    super(
        _ID, (byte) 24, new byte[] {}, new byte[] {}, INOUT_PINS, PORT_NAMES,
        new Ttl74652HdlGenerator());
  }

    public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var boxTop = y + 18;
    final var boxBottom = boxTop + 24;
    g.drawRect(x + 6, boxTop, 472, 24);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 9));
    GraphicsUtil.drawCenteredText(g, "BUF", x + 242, y + 30);
    g.setFont(previousFont);
    for (var pin = 1; pin <= 24; pin++) {
      if (pin == 12 || pin == 24) {
        continue;
      }
      final var pinX = pin <= 12 ? x + (pin - 1) * 20 + 10 : x + (24 - pin) * 20 + 10;
      final var pinY =
          pin <= 12
              ? y + height - AbstractTtlGate.PIN_HEIGHT
              : y + AbstractTtlGate.PIN_HEIGHT;
      final var edge = pin <= 12 ? boxBottom : boxTop;
      if (false) {
        final var bubbleY = pin <= 12 ? edge + 4 : edge - 4;
        g.drawLine(pinX, pinY, pinX, pin <= 12 ? bubbleY + 2 : bubbleY - 2);
        structureBubble(g, pinX, bubbleY);
      } else {
        g.drawLine(pinX, pinY, pinX, edge);
      }
    }
  }

  private static void drawRegisters(
      Graphics2D gfx, int x, int y, int height, TtlRegisterData data) {
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
    final var outputEnableAb = state.getPortValue(PORT_INDEX_OEAB);
    final var outputEnableBa = state.getPortValue(PORT_INDEX_OEBA);
    final var selectAb = state.getPortValue(PORT_INDEX_SAB);
    final var selectBa = state.getPortValue(PORT_INDEX_SBA);
    final var data = registers(state);
    final var driveA = drivesA(outputEnableBa, selectBa);
    final var driveB = drivesB(outputEnableAb, selectAb);
    final var sampleA =
        driveA ? selected(selectBa, data.getValue(REGISTER_B), externalB) : externalA;
    final var sampleB =
        driveB ? selected(selectAb, data.getValue(REGISTER_A), externalA) : externalB;
    final var captureA =
        data.updateClock(state.getPortValue(PORT_INDEX_CLKAB), CLOCK_AB, StdAttr.TRIG_RISING);
    final var captureB =
        data.updateClock(state.getPortValue(PORT_INDEX_CLKBA), CLOCK_BA, StdAttr.TRIG_RISING);
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
    return new int[] {PORT_INDEX_CLKAB, PORT_INDEX_CLKBA};
  }

  private static boolean exposedPowerIsInvalid(InstanceState state) {
    if (!state.getAttributeValue(TtlLibrary.VCC_GND)) return false;
    return state.getPortValue(PORT_INDEX_GND) != Value.FALSE
        || state.getPortValue(PORT_INDEX_VCC) != Value.TRUE;
  }

  /** A drives from B only when OEBA is low and the B-to-A select is a defined level. */
  private static boolean drivesA(Value outputEnableBa, Value selectBa) {
    return outputEnableBa == Value.FALSE && isLevel(selectBa);
  }

  /** B drives from A only when OEAB is high and the A-to-B select is a defined level. */
  private static boolean drivesB(Value outputEnableAb, Value selectAb) {
    return outputEnableAb == Value.TRUE && isLevel(selectAb);
  }

  private static boolean isLevel(Value value) {
    return value == Value.TRUE || value == Value.FALSE;
  }

  /**
   * Chooses stored or live bits for a driven bus. A bit that is not 0 or 1 becomes an error.
   */
  private static Value[] selected(Value select, Value stored, Value[] live) {
    final var result = new Value[BITS];
    for (var bit = 0; bit < BITS; bit++) {
      final var raw = select == Value.TRUE ? stored.get(bit) : live[bit];
      result[bit] = raw == Value.TRUE || raw == Value.FALSE ? raw : Value.ERROR;
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

  private static int structureX(int originX, int pins, int pin) {
    if (pin <= pins / 2) {
      return originX + (pin - 1) * 20 + 10;
    }
    return originX + (pins - pin) * 20 + 10;
  }

  private static int structureY(int originY, int bodyHeight, int pins, int pin) {
    if (pin <= pins / 2) {
      return originY + bodyHeight - AbstractTtlGate.PIN_HEIGHT;
    }
    return originY + AbstractTtlGate.PIN_HEIGHT;
  }

  private static void structureBubble(Graphics g, int centerX, int centerY) {
    g.drawOval(centerX - 2, centerY - 2, 4, 4);
  }

  private static void structureDot(
      Graphics g, int centerX, int centerY, TtlRegisterData data, int word, int bit) {
    if (data == null) {
      return;
    }
    final var value = data.getValue(word);
    if (bit < 0 || bit >= value.getWidth()) {
      return;
    }
    final var bitValue = value.get(bit);
    g.setColor(bitValue.getColor());
    g.fillOval(centerX - 3, centerY - 3, 6, 6);
    g.setColor(Color.WHITE);
    GraphicsUtil.drawCenteredText(g, bitValue.toDisplayString(), centerX, centerY);
    g.setColor(Color.BLACK);
  }

}
