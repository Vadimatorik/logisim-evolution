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
 * TTL 74x592: 8-bit input register feeding an 8-bit binary counter.
 *
 * <p>Simulation follows the ST M74HC592 and Toshiba TC74HC592A function tables. {@code A} is the
 * least significant input and {@code H} is the most significant. A rising {@code RCK} stores {@code
 * A} through {@code H}. {@code CLOAD} low copies that register into the counter for as long as it
 * stays low, including a later rising {@code RCK}. {@code CCLR} low clears the counter
 * asynchronously and wins while {@code CLOAD} is also low. The counter advances on the rising edge
 * of {@code CCK} only while {@code CCLR} and {@code CLOAD} are high and {@code CCKEN} is low.
 * {@code QA} to {@code QH} are not brought out. {@code RCO} is the AND of those internal bits, so
 * it is high only at code 255. An unknown or error input changes a stored bit only when the two
 * substitutions disagree. Nanosecond delays are not modeled.
 */
public class Ttl74592 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74592";

  public static final int DELAY = 1;
  static final int WIDTH = 8;
  /** Input register word inside {@link TtlRegisterData}. */
  static final int REGISTER_WORD = 0;
  /** Counter word inside {@link TtlRegisterData}. */
  static final int COUNTER_WORD = 1;

  public static final byte A = 1;
  public static final byte B = 2;
  public static final byte C = 3;
  public static final byte D = 4;
  public static final byte E = 5;
  public static final byte F = 6;
  public static final byte G = 7;
  public static final byte GND = 8;
  public static final byte RCO = 9;
  public static final byte CCLR = 10;
  public static final byte CCK = 11;
  public static final byte CCKEN = 12;
  public static final byte RCK = 13;
  public static final byte CLOAD = 14;
  public static final byte H = 15;
  public static final byte VCC = 16;

  private static final int CLOCK_RCK = 0;
  private static final int CLOCK_CCK = 1;
  private static final BitWidth BIT_WIDTH = BitWidth.create(WIDTH);
  private static final byte[] OUTPUT_PINS = {RCO};
  private static final String[] PORT_NAMES = {
    "A (register input, LSB)",
    "B",
    "C",
    "D",
    "E",
    "F",
    "G",
    "RCO (ripple carry, high at 255)",
    "CCLR (counter clear, active low)",
    "CCK (counter clock)",
    "CCKEN (counter clock enable, active low)",
    "RCK (register clock)",
    "CLOAD (counter load, active low)",
    "H (register input, MSB)"
  };
  private static final String[] PIN_NAMES = {
    "A", "B", "C", "D", "E", "F", "G", null,
    "RCO", "CCLR", "CCK", "CCKEN", "RCK", "CLOAD", "H", null
  };

  /** Creates a 74592 8-bit counter with an input register. */
  public Ttl74592() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl74592HdlGenerator());
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
    final var gfx = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, false, false);
    Drawgates.paintPortNamesByPin(painter, x, y, height, PIN_NAMES);
    drawState(gfx, x, y, (TtlRegisterData) painter.getData());
  }

  private static void drawState(Graphics2D gfx, int x, int y, TtlRegisterData data) {
    if (data == null) {
      return;
    }
    gfx.setColor(Color.BLACK);
    GraphicsUtil.drawCenteredText(gfx, "R", x + 22, y + 22);
    GraphicsUtil.drawCenteredText(gfx, "C", x + 22, y + 36);
    drawWord(gfx, data.getValue(REGISTER_WORD), x + 30, y + 18);
    drawWord(gfx, data.getValue(COUNTER_WORD), x + 30, y + 32);
    gfx.setColor(Color.BLACK);
  }

  /** Draws one byte with the most significant bit on the left. */
  private static void drawWord(Graphics2D gfx, Value word, int originX, int originY) {
    for (var bit = WIDTH - 1; bit >= 0; bit--) {
      final var shown = word.get(bit);
      final var left = originX + (WIDTH - 1 - bit) * 14;
      gfx.setColor(shown.getColor());
      gfx.fillOval(left, originY, 8, 8);
      gfx.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(gfx, shown.toDisplayString(), left + 4, originY + 4);
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getState(state);
    final var registerClock =
        data.updateClock(input(state, RCK), CLOCK_RCK, StdAttr.TRIG_RISING);
    final var counterClock =
        data.updateClock(input(state, CCK), CLOCK_CCK, StdAttr.TRIG_RISING);
    final var register = registerClock ? dataWord(state) : data.getValue(REGISTER_WORD);
    final var counter =
        resolveCounter(
            data.getValue(COUNTER_WORD),
            register,
            counterClock,
            input(state, CCLR),
            input(state, CLOAD),
            input(state, CCKEN));
    data.setValue(REGISTER_WORD, register);
    data.setValue(COUNTER_WORD, counter);
    state.setPort(pinNrToPortNr(RCO), rippleCarry(counter), DELAY);
  }

  private static TtlRegisterData getState(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BIT_WIDTH, 2);
      state.setData(data);
    }
    return data;
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }

  private static Value dataWord(InstanceState state) {
    return Value.create(
        new Value[] {
          level(input(state, A)),
          level(input(state, B)),
          level(input(state, C)),
          level(input(state, D)),
          level(input(state, E)),
          level(input(state, F)),
          level(input(state, G)),
          level(input(state, H))
        });
  }

  /** Clear wins over load, and load wins over a count. The count is sampled on the rising edge. */
  private static Value resolveCounter(
      Value counter, Value register, boolean countEdge, Value cclr, Value cload, Value ccken) {
    if (cclr == Value.FALSE) {
      return Value.createKnown(BIT_WIDTH, 0);
    }
    final var whileClearInactive = resolveWithoutClear(counter, register, countEdge, cload, ccken);
    if (cclr == Value.TRUE) {
      return whileClearInactive;
    }
    return merge(Value.createKnown(BIT_WIDTH, 0), whileClearInactive, cclr == Value.ERROR);
  }

  private static Value resolveWithoutClear(
      Value counter, Value register, boolean countEdge, Value cload, Value ccken) {
    if (cload == Value.FALSE) {
      return register;
    }
    final var whileLoadInactive = resolveCountOrHold(counter, countEdge, ccken);
    if (cload == Value.TRUE) {
      return whileLoadInactive;
    }
    return merge(register, whileLoadInactive, cload == Value.ERROR);
  }

  private static Value resolveCountOrHold(Value counter, boolean countEdge, Value ccken) {
    if (!countEdge || ccken == Value.TRUE) {
      return counter;
    }
    final var counted = increment(counter);
    if (ccken == Value.FALSE) {
      return counted;
    }
    return merge(counted, counter, ccken == Value.ERROR);
  }

  private static Value increment(Value current) {
    final var bits = new Value[WIDTH];
    var carry = Value.TRUE;
    for (var bit = 0; bit < WIDTH; bit++) {
      final var value = current.get(bit);
      bits[bit] = xorBit(value, carry);
      carry = andBit(value, carry);
    }
    return Value.create(bits);
  }

  /** High only when every counter bit is high. A known low bit forces {@code RCO} low. */
  static Value rippleCarry(Value counter) {
    var sawUnknown = false;
    var sawError = false;
    for (var bit = 0; bit < WIDTH; bit++) {
      final var value = counter.get(bit);
      if (value == Value.FALSE) {
        return Value.FALSE;
      }
      if (value == Value.ERROR) {
        sawError = true;
      } else if (value != Value.TRUE) {
        sawUnknown = true;
      }
    }
    if (sawError) {
      return Value.ERROR;
    }
    if (sawUnknown) {
      return Value.UNKNOWN;
    }
    return Value.TRUE;
  }

  private static Value merge(Value left, Value right, boolean error) {
    final var bits = new Value[WIDTH];
    for (var bit = 0; bit < WIDTH; bit++) {
      final var leftBit = left.get(bit);
      final var rightBit = right.get(bit);
      if (leftBit == rightBit) {
        bits[bit] = leftBit;
      } else if (error || leftBit == Value.ERROR || rightBit == Value.ERROR) {
        bits[bit] = Value.ERROR;
      } else {
        bits[bit] = Value.UNKNOWN;
      }
    }
    return Value.create(bits);
  }

  private static Value level(Value value) {
    if (value == Value.TRUE || value == Value.FALSE || value == Value.ERROR) {
      return value;
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

  /** A known low input forces the result low. Otherwise an error wins over an unknown. */
  private static Value andBit(Value left, Value right) {
    if (left == Value.FALSE || right == Value.FALSE) {
      return Value.FALSE;
    }
    if (left == Value.ERROR || right == Value.ERROR) {
      return Value.ERROR;
    }
    if (left == Value.TRUE && right == Value.TRUE) {
      return Value.TRUE;
    }
    return Value.UNKNOWN;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {pinNrToPortNr(RCK), pinNrToPortNr(CCK)};
  }
}
