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
import java.awt.Font;
import java.awt.Graphics;

/**
 * TTL 74x593: 8-bit binary counter with an input register and 3-state I/O.
 *
 * <p>Simulation follows the ST M54/M74HC593, which is pin-compatible with the 74LS593. The logic
 * diagram loads the bus into the input register on the rising edge of {@code RCK} when {@code
 * RCKEN} is low, and {@code CLOAD} copies that register into the counter. {@code CCLR} clears only
 * the counter and wins when both load and clear are low. The counter advances on the rising edge
 * of {@code CCK} when {@code CCKEN} is high or {@code CCKEN}-bar is low. The bus is driven only
 * when {@code G} is high and {@code G}-bar is low. {@code RCO} is low only while the count is
 * {@code 0xFF}. An unknown clock is not a rising edge. Nanosecond delays are not modeled.
 *
 * <p>An unknown or error level changes a result only when the two substitutions disagree. An error
 * among those inputs makes the disagreed bits an error; an unknown input makes them unknown.
 */
public class Ttl74593 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74593";

  public static final int DELAY = 1;

  /** Bus bit 0, datasheet pin A/QA. */
  public static final byte QA = 1;

  /** Bus bit 1, datasheet pin B/QB. */
  public static final byte QB = 2;

  /** Bus bit 2, datasheet pin C/QC. */
  public static final byte QC = 3;

  /** Bus bit 3, datasheet pin D/QD. */
  public static final byte QD = 4;

  /** Bus bit 4, datasheet pin E/QE. */
  public static final byte QE = 5;

  /** Bus bit 5, datasheet pin F/QF. */
  public static final byte QF = 6;

  /** Bus bit 6, datasheet pin G/QG. */
  public static final byte QG = 7;

  /** Bus bit 7, datasheet pin H/QH. */
  public static final byte QH = 8;

  /** Asynchronous counter load, active low. Copies the register, not the bus pins. */
  public static final byte NCLOAD = 9;

  public static final byte GND = 10;

  /** Ripple carry, active low. Low only while the counter is 0xFF. */
  public static final byte NRCO = 11;

  /** Asynchronous counter clear, active low. Does not clear the input register. */
  public static final byte NCCLR = 12;

  /** Counter clock. The counter advances on the rising edge. */
  public static final byte CCK = 13;

  /** Counter clock enable, active low. Either this pin or {@link #CCKEN} can allow a count. */
  public static final byte NCCKEN = 14;

  /** Counter clock enable, active high. */
  public static final byte CCKEN = 15;

  /** Register clock. The register loads on the rising edge. */
  public static final byte RCK = 16;

  /** Register clock enable, active low. */
  public static final byte NRCKEN = 17;

  /** Output enable, active low. The bus is driven only with this pin low and {@link #G} high. */
  public static final byte NG = 18;

  /** Output enable, active high. */
  public static final byte G = 19;

  public static final byte VCC = 20;

  private static final int WIDTH = 8;
  private static final BitWidth BUS = BitWidth.create(WIDTH);
  private static final int REGISTER = 0;
  private static final int COUNTER = 1;
  private static final int CCK_CLOCK = 0;
  private static final int RCK_CLOCK = 1;
  private static final int HIDDEN_PORT_COUNT = 18;
  private static final byte[] BUS_PINS = {QA, QB, QC, QD, QE, QF, QG, QH};
  private static final byte[] OUTPUT_PINS = {NRCO};
  private static final String[] PORT_NAMES = {
    "A/QA",
    "B/QB",
    "C/QC",
    "D/QD",
    "E/QE",
    "F/QF",
    "G/QG",
    "H/QH",
    "CLOAD (counter load, active low)",
    "RCO (ripple carry, active low)",
    "CCLR (counter clear, active low)",
    "CCK (counter clock)",
    "CCKEN (counter clock enable, active low)",
    "CCKEN (counter clock enable, active high)",
    "RCK (register clock)",
    "RCKEN (register clock enable, active low)",
    "G (output enable, active low)",
    "G (output enable, active high)"
  };
  private static final String[] PIN_NAMES = {
    "QA", "QB", "QC", "QD", "QE", "QF", "QG", "QH", "CLD", null,
    "RCO", "CLR", "CCK", "nCE", "CE", "RCK", "nRE", "nG", "G", null
  };

  /** Creates a 74593 8-bit counter with an input register and 3-state bus. */
  public Ttl74593() {
    super(
        _ID,
        (byte) 20,
        OUTPUT_PINS,
        null,
        BUS_PINS,
        PORT_NAMES,
        new Ttl74593HdlGenerator());
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
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 8));
    g.drawRect(x + 5, y + 28, 10, 10);
    g.drawRect(x + 25, y + 28, 10, 10);
    g.drawRect(x + 45, y + 28, 10, 10);
    g.drawRect(x + 65, y + 28, 10, 10);
    g.drawRect(x + 85, y + 28, 10, 10);
    g.drawRect(x + 105, y + 28, 10, 10);
    g.drawRect(x + 125, y + 28, 10, 10);
    g.drawRect(x + 145, y + 28, 10, 10);
    g.drawLine(x + 10, y + height - 22, x + 10, y + height - 7);
    g.drawLine(x + 30, y + height - 22, x + 30, y + height - 7);
    g.drawLine(x + 50, y + height - 22, x + 50, y + height - 7);
    g.drawLine(x + 70, y + height - 22, x + 70, y + height - 7);
    g.drawLine(x + 90, y + height - 22, x + 90, y + height - 7);
    g.drawLine(x + 110, y + height - 22, x + 110, y + height - 7);
    g.drawLine(x + 130, y + height - 22, x + 130, y + height - 7);
    g.drawLine(x + 150, y + height - 22, x + 150, y + height - 7);
    g.drawLine(x + 5, y + 24, x + 155, y + 24);
    g.drawLine(x + 170, y + height - 7, x + 170, y + height - 9);
    g.drawLine(x + 170, y + height - 9, x + 1, y + height - 9);
    g.drawLine(x + 1, y + height - 9, x + 1, y + 24);
    g.drawLine(x + 1, y + 24, x + 5, y + 24);
    g.drawLine(x + 7, y + 24, x + 7, y + 28);
    g.drawLine(x + 27, y + 24, x + 27, y + 28);
    g.drawLine(x + 47, y + 24, x + 47, y + 28);
    g.drawLine(x + 67, y + 24, x + 67, y + 28);
    g.drawLine(x + 87, y + 24, x + 87, y + 28);
    g.drawLine(x + 107, y + 24, x + 107, y + 28);
    g.drawLine(x + 127, y + 24, x + 127, y + 28);
    g.drawLine(x + 147, y + 24, x + 147, y + 28);
    g.drawLine(x + 5, y + 24, x + 155, y + 24);
    g.drawLine(x + 170, y + 7, x + 170, y + 9);
    g.drawLine(x + 170, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 24);
    g.drawLine(x + 1, y + 24, x + 5, y + 24);
    g.drawLine(x + 10, y + 24, x + 10, y + 28);
    g.drawLine(x + 30, y + 24, x + 30, y + 28);
    g.drawLine(x + 50, y + 24, x + 50, y + 28);
    g.drawLine(x + 70, y + 24, x + 70, y + 28);
    g.drawLine(x + 90, y + 24, x + 90, y + 28);
    g.drawLine(x + 110, y + 24, x + 110, y + 28);
    g.drawLine(x + 130, y + 24, x + 130, y + 28);
    g.drawLine(x + 150, y + 24, x + 150, y + 28);
    g.drawLine(x + 5, y + 24, x + 155, y + 24);
    g.drawLine(x + 150, y + 7, x + 150, y + 9);
    g.drawLine(x + 150, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 24);
    g.drawLine(x + 1, y + 24, x + 5, y + 24);
    g.drawLine(x + 13, y + 24, x + 13, y + 28);
    g.drawLine(x + 33, y + 24, x + 33, y + 28);
    g.drawLine(x + 53, y + 24, x + 53, y + 28);
    g.drawLine(x + 73, y + 24, x + 73, y + 28);
    g.drawLine(x + 93, y + 24, x + 93, y + 28);
    g.drawLine(x + 113, y + 24, x + 113, y + 28);
    g.drawLine(x + 133, y + 24, x + 133, y + 28);
    g.drawLine(x + 153, y + 24, x + 153, y + 28);
    g.drawLine(x + 5, y + 24, x + 155, y + 24);
    g.drawLine(x + 130, y + 7, x + 130, y + 9);
    g.drawLine(x + 130, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 24);
    g.drawLine(x + 1, y + 24, x + 5, y + 24);
    g.drawLine(x + 7, y + 24, x + 7, y + 28);
    g.drawLine(x + 27, y + 24, x + 27, y + 28);
    g.drawLine(x + 47, y + 24, x + 47, y + 28);
    g.drawLine(x + 67, y + 24, x + 67, y + 28);
    g.drawLine(x + 87, y + 24, x + 87, y + 28);
    g.drawLine(x + 107, y + 24, x + 107, y + 28);
    g.drawLine(x + 127, y + 24, x + 127, y + 28);
    g.drawLine(x + 147, y + 24, x + 147, y + 28);
    g.drawLine(x + 5, y + 25, x + 155, y + 25);
    g.drawLine(x + 110, y + 7, x + 110, y + 9);
    g.drawLine(x + 110, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 25);
    g.drawLine(x + 1, y + 25, x + 5, y + 25);
    g.drawLine(x + 10, y + 25, x + 10, y + 28);
    g.drawLine(x + 30, y + 25, x + 30, y + 28);
    g.drawLine(x + 50, y + 25, x + 50, y + 28);
    g.drawLine(x + 70, y + 25, x + 70, y + 28);
    g.drawLine(x + 90, y + 25, x + 90, y + 28);
    g.drawLine(x + 110, y + 25, x + 110, y + 28);
    g.drawLine(x + 130, y + 25, x + 130, y + 28);
    g.drawLine(x + 150, y + 25, x + 150, y + 28);
    g.drawLine(x + 5, y + 25, x + 155, y + 25);
    g.drawLine(x + 90, y + 7, x + 90, y + 9);
    g.drawLine(x + 90, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 25);
    g.drawLine(x + 1, y + 25, x + 5, y + 25);
    g.drawLine(x + 13, y + 25, x + 13, y + 28);
    g.drawLine(x + 33, y + 25, x + 33, y + 28);
    g.drawLine(x + 53, y + 25, x + 53, y + 28);
    g.drawLine(x + 73, y + 25, x + 73, y + 28);
    g.drawLine(x + 93, y + 25, x + 93, y + 28);
    g.drawLine(x + 113, y + 25, x + 113, y + 28);
    g.drawLine(x + 133, y + 25, x + 133, y + 28);
    g.drawLine(x + 153, y + 25, x + 153, y + 28);
    g.drawLine(x + 5, y + 25, x + 155, y + 25);
    g.drawLine(x + 70, y + 7, x + 70, y + 9);
    g.drawLine(x + 70, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 25);
    g.drawLine(x + 1, y + 25, x + 5, y + 25);
    g.drawLine(x + 7, y + 25, x + 7, y + 28);
    g.drawLine(x + 27, y + 25, x + 27, y + 28);
    g.drawLine(x + 47, y + 25, x + 47, y + 28);
    g.drawLine(x + 67, y + 25, x + 67, y + 28);
    g.drawLine(x + 87, y + 25, x + 87, y + 28);
    g.drawLine(x + 107, y + 25, x + 107, y + 28);
    g.drawLine(x + 127, y + 25, x + 127, y + 28);
    g.drawLine(x + 147, y + 25, x + 147, y + 28);
    g.drawLine(x + 5, y + 25, x + 155, y + 25);
    g.drawLine(x + 50, y + 7, x + 50, y + 9);
    g.drawLine(x + 50, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 25);
    g.drawLine(x + 1, y + 25, x + 5, y + 25);
    g.drawLine(x + 10, y + 25, x + 10, y + 28);
    g.drawLine(x + 30, y + 25, x + 30, y + 28);
    g.drawLine(x + 50, y + 25, x + 50, y + 28);
    g.drawLine(x + 70, y + 25, x + 70, y + 28);
    g.drawLine(x + 90, y + 25, x + 90, y + 28);
    g.drawLine(x + 110, y + 25, x + 110, y + 28);
    g.drawLine(x + 130, y + 25, x + 130, y + 28);
    g.drawLine(x + 150, y + 25, x + 150, y + 28);
    g.drawLine(x + 5, y + 25, x + 155, y + 25);
    g.drawLine(x + 30, y + 7, x + 30, y + 9);
    g.drawLine(x + 30, y + 9, x + 1, y + 9);
    g.drawLine(x + 1, y + 9, x + 1, y + 25);
    g.drawLine(x + 1, y + 25, x + 5, y + 25);
    g.drawLine(x + 13, y + 25, x + 13, y + 28);
    g.drawLine(x + 33, y + 25, x + 33, y + 28);
    g.drawLine(x + 53, y + 25, x + 53, y + 28);
    g.drawLine(x + 73, y + 25, x + 73, y + 28);
    g.drawLine(x + 93, y + 25, x + 93, y + 28);
    g.drawLine(x + 113, y + 25, x + 113, y + 28);
    g.drawLine(x + 133, y + 25, x + 133, y + 28);
    g.drawLine(x + 153, y + 25, x + 153, y + 28);
    g.drawOval(x + -1, y + 22, 4, 4);
    g.drawOval(x + -1, y + 22, 4, 4);
    g.drawOval(x + -1, y + 22, 4, 4);
    g.drawOval(x + -1, y + 23, 4, 4);
    g.drawOval(x + -1, y + 23, 4, 4);
    GraphicsUtil.drawCenteredText(g, "A", x + 10, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "B", x + 30, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "C", x + 50, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "D", x + 70, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "E", x + 90, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "F", x + 110, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "G", x + 130, y + height - 27);
    GraphicsUtil.drawCenteredText(g, "H", x + 150, y + height - 27);
    g.setFont(previousFont);
  }




  @Override
  public void propagate(InstanceState state) {
    if (powerIsBad(state)) {
      releaseOutputs(state);
      return;
    }
    propagateTtl(state);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = stateOf(state);
    final var bus = readBus(state);
    final var cckRise = data.updateClock(input(state, CCK), CCK_CLOCK, StdAttr.TRIG_RISING);
    final var rckRise = data.updateClock(input(state, RCK), RCK_CLOCK, StdAttr.TRIG_RISING);
    final var register = nextRegister(data.getValue(REGISTER), bus, input(state, NRCKEN), rckRise);
    final var controls =
        new Value[] {
          input(state, NCLOAD), input(state, NCCLR), input(state, NCCKEN), input(state, CCKEN)
        };
    final var counter = nextCounter(data.getValue(COUNTER), register, controls, cckRise);
    data.setValue(REGISTER, register);
    data.setValue(COUNTER, counter);
    state.setPort(pinNrToPortNr(NRCO), rippleCarry(counter), DELAY);
    driveBus(state, counter, input(state, NG), input(state, G));
  }

  /**
   * Carry is low only when every counter bit is high. One low bit forces it high. Anything else
   * unresolved becomes unknown, or error when an error bit is involved.
   */
  static Value rippleCarry(Value count) {
    var unknown = false;
    var error = false;
    for (var bit = 0; bit < WIDTH; bit++) {
      final var value = count.get(bit);
      if (value == Value.FALSE) {
        return Value.TRUE;
      }
      if (value == Value.ERROR) {
        error = true;
      } else if (value != Value.TRUE) {
        unknown = true;
      }
    }
    if (!unknown && !error) {
      return Value.FALSE;
    }
    return error ? Value.ERROR : Value.UNKNOWN;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {pinNrToPortNr(CCK), pinNrToPortNr(RCK)};
  }

  private static boolean powerIsBad(InstanceState state) {
    if (!state.getAttributeValue(TtlLibrary.VCC_GND)) {
      return false;
    }
    final var gnd = state.getPortValue(HIDDEN_PORT_COUNT);
    final var vcc = state.getPortValue(HIDDEN_PORT_COUNT + 1);
    return gnd != Value.FALSE || vcc != Value.TRUE;
  }

  private static void releaseOutputs(InstanceState state) {
    for (final var pin : BUS_PINS) {
      state.setPort(pinNrToPortNr(pin), Value.UNKNOWN, DELAY);
    }
    state.setPort(pinNrToPortNr(NRCO), Value.UNKNOWN, DELAY);
  }

  private static TtlRegisterData stateOf(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BUS, 2);
      state.setData(data);
    }
    return data;
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }

  private static Value readBus(InstanceState state) {
    final var bits = new Value[WIDTH];
    for (var index = 0; index < WIDTH; index++) {
      bits[index] = input(state, BUS_PINS[index]);
    }
    return Value.create(bits);
  }

  private static void driveBus(InstanceState state, Value counter, Value gBar, Value g) {
    final var sources = new Value[] {gBar, g};
    final var error = hasError(sources);
    for (var bit = 0; bit < WIDTH; bit++) {
      final var choice = new Choice(1);
      for (var mask = 0; mask < 4; mask++) {
        if (!accepts(sources, mask)) {
          continue;
        }
        final var enabled = bitHigh(mask, 1) && !bitHigh(mask, 0);
        final var driven = enabled ? counter.get(bit) : Value.UNKNOWN;
        choice.accept(Value.create(new Value[] {driven}), error);
      }
      state.setPort(pinNrToPortNr(BUS_PINS[bit]), choice.value().get(0), DELAY);
    }
  }

  /**
   * {@code RCKEN} low stores the bus. A high enable keeps the register. The level is sampled only
   * on a rising {@code RCK}.
   */
  private static Value nextRegister(Value stored, Value bus, Value rcken, boolean rckRise) {
    if (!rckRise) {
      return stored;
    }
    final var sources = new Value[] {rcken};
    final var choice = new Choice(WIDTH);
    final var error = hasError(sources) || hasError(stored) || hasError(bus);
    for (var mask = 0; mask < 2; mask++) {
      if (!accepts(sources, mask)) {
        continue;
      }
      choice.accept(bitHigh(mask, 0) ? stored : bus, error);
    }
    return choice.value();
  }

  /**
   * Clear wins over load, and load wins over a count. Controls are index 0 {@code CLOAD}, 1 {@code
   * CCLR}, 2 {@code CCKEN}-bar and 3 {@code CCKEN}.
   */
  private static Value nextCounter(Value stored, Value loaded, Value[] controls, boolean cckRise) {
    final var choice = new Choice(WIDTH);
    final var error = hasError(controls);
    for (var mask = 0; mask < 16; mask++) {
      if (!accepts(controls, mask)) {
        continue;
      }
      final Value candidate;
      if (!bitHigh(mask, 1)) {
        candidate = Value.createKnown(BUS, 0);
      } else if (!bitHigh(mask, 0)) {
        candidate = loaded;
      } else if (cckRise && (bitHigh(mask, 3) || !bitHigh(mask, 2))) {
        candidate = increment(stored);
      } else {
        candidate = stored;
      }
      choice.accept(candidate, error);
    }
    return choice.value();
  }

  private static Value increment(Value current) {
    var carry = Value.TRUE;
    final var bits = new Value[WIDTH];
    for (var index = 0; index < WIDTH; index++) {
      final var bit = current.get(index);
      bits[index] = xorBit(bit, carry);
      carry = andBit(bit, carry);
    }
    return Value.create(bits);
  }

  private static Value andBit(Value left, Value right) {
    if (left == Value.FALSE || right == Value.FALSE) {
      return Value.FALSE;
    }
    if (left == Value.TRUE && right == Value.TRUE) {
      return Value.TRUE;
    }
    if (left == Value.ERROR || right == Value.ERROR) {
      return Value.ERROR;
    }
    return Value.UNKNOWN;
  }

  private static Value xorBit(Value left, Value right) {
    if (left == Value.ERROR || right == Value.ERROR) {
      return Value.ERROR;
    }
    if (left != Value.TRUE && left != Value.FALSE) {
      return Value.UNKNOWN;
    }
    if (right != Value.TRUE && right != Value.FALSE) {
      return Value.UNKNOWN;
    }
    return left == right ? Value.FALSE : Value.TRUE;
  }

  private static boolean accepts(Value[] sources, int mask) {
    for (var index = 0; index < sources.length; index++) {
      final var actual = sources[index];
      final var high = bitHigh(mask, index);
      if (actual == Value.TRUE && !high) {
        return false;
      }
      if (actual == Value.FALSE && high) {
        return false;
      }
    }
    return true;
  }

  private static boolean hasError(Value word) {
    for (var index = 0; index < word.getWidth(); index++) {
      if (word.get(index) == Value.ERROR) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasError(Value[] sources) {
    for (final var source : sources) {
      if (hasError(source)) {
        return true;
      }
    }
    return false;
  }

  private static boolean bitHigh(int mask, int bit) {
    return (mask & (1 << bit)) != 0;
  }

  /** Merges every accepted substitution. Disagreements become unknown, or error if one was seen. */
  private static final class Choice {
    private final Value[] bits;
    private final boolean[] conflict;
    private boolean sawError;
    private boolean any;

    private Choice(int width) {
      bits = new Value[width];
      conflict = new boolean[width];
    }

    private void accept(Value word, boolean error) {
      sawError |= error;
      if (!any) {
        any = true;
        for (var index = 0; index < bits.length; index++) {
          bits[index] = word.get(index);
        }
        return;
      }
      for (var index = 0; index < bits.length; index++) {
        if (bits[index] != word.get(index)) {
          conflict[index] = true;
        }
      }
    }

    private Value value() {
      if (!any) {
        return Value.createUnknown(BitWidth.create(bits.length));
      }
      for (var index = 0; index < bits.length; index++) {
        if (conflict[index]) {
          bits[index] = sawError ? Value.ERROR : Value.UNKNOWN;
        }
      }
      return Value.create(bits);
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
