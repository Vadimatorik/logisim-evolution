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
import java.awt.Graphics;

/**
 * TTL 74x198: 8-bit bidirectional universal shift register.
 *
 * <p>Simulation follows the Philips 74HC/HCT198 function table, the 8-bit form of the 74HC194
 * table, on the 74LS198 DIP-24 pinout. A low master reset clears every stage at once. Parallel
 * load, shift right and shift left happen on the rising edge of the clock. Shift right enters
 * {@code DSR} at {@code Q0} and moves bits toward {@code Q7}. Shift left enters {@code DSL} at
 * {@code Q7} and moves bits toward {@code Q0}. Both mode inputs low hold the register. Nanosecond
 * delays are not modeled.
 *
 * <p>An unknown or error input changes an output only when the two substitutions disagree. An error
 * on such an input makes the disagreed output an error; an unknown input makes it unknown.
 */
public class Ttl74198 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74198";

  public static final int DELAY = 1;

  public static final byte S0 = 1;
  public static final byte DSR = 2;
  public static final byte D0 = 3;
  public static final byte Q0 = 4;
  public static final byte D1 = 5;
  public static final byte Q1 = 6;
  public static final byte D2 = 7;
  public static final byte Q2 = 8;
  public static final byte D3 = 9;
  public static final byte Q3 = 10;
  public static final byte CP = 11;
  public static final byte GND = 12;
  public static final byte MR = 13;
  public static final byte DSL = 14;
  public static final byte D4 = 15;
  public static final byte Q4 = 16;
  public static final byte D5 = 17;
  public static final byte Q5 = 18;
  public static final byte D6 = 19;
  public static final byte Q6 = 20;
  public static final byte D7 = 21;
  public static final byte Q7 = 22;
  public static final byte S1 = 23;
  public static final byte VCC = 24;

  private static final int WIDTH = 8;
  private static final int HOLD = 0;
  private static final int SHIFT_RIGHT = 1;
  private static final int SHIFT_LEFT = 2;
  private static final int LOAD = 3;

  private static final byte[] OUTPUT_PINS = {Q0, Q1, Q2, Q3, Q4, Q5, Q6, Q7};
  private static final byte[] DATA_PINS = {D0, D1, D2, D3, D4, D5, D6, D7};
  private static final String[] PORT_NAMES = {
    "S0 (mode select)",
    "DSR (shift-right serial input)",
    "D0",
    "Q0",
    "D1",
    "Q1",
    "D2",
    "Q2",
    "D3",
    "Q3",
    "CP (clock)",
    "MR (master reset, active low)",
    "DSL (shift-left serial input)",
    "D4",
    "Q4",
    "D5",
    "Q5",
    "D6",
    "Q6",
    "D7",
    "Q7",
    "S1 (mode select)"
  };
  private static final String[] PIN_NAMES = {
    "S0", "DSR", "D0", "Q0", "D1", "Q1", "D2", "Q2", "D3", "Q3", "CP", null,
    "MR", "DSL", "D4", "Q4", "D5", "Q5", "D6", "Q6", "D7", "Q7", "S1", null
  };

  /** Creates a 74198 8-bit bidirectional universal shift register. */
  public Ttl74198() {
    super(_ID, (byte) 24, OUTPUT_PINS, PORT_NAMES, new Ttl74198HdlGenerator());
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
    final var gfx = painter.getGraphics();
    super.paintBase(painter, false, false);
    Drawgates.paintPortNamesByPin(painter, x, y, height, PIN_NAMES);
    drawWord(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private static void drawWord(Graphics gfx, int x, int y, int height, TtlRegisterData data) {
    if (data == null) {
      return;
    }
    final var value = data.getValue();
    final var origin = x + ((24 * 10) - (WIDTH * 16)) / 2;
    for (var bit = WIDTH - 1; bit >= 0; bit--) {
      final var shown = value.get(bit);
      final var originX = origin + (WIDTH - 1 - bit) * 16;
      gfx.setColor(shown.getColor());
      gfx.fillOval(originX, y + height / 2 - 7, 14, 14);
      gfx.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(gfx, shown.toDisplayString(), originX + 7, y + height / 2);
    }
    gfx.setColor(Color.BLACK);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getState(state);
    final var triggered = data.updateClock(input(state, CP), StdAttr.TRIG_RISING);
    final var parallel = new Value[WIDTH];
    for (var bit = 0; bit < WIDTH; bit++) {
      parallel[bit] = input(state, DATA_PINS[bit]);
    }
    final var word =
        resolve(
            data.getValue(),
            triggered,
            input(state, MR),
            input(state, S0),
            input(state, S1),
            input(state, DSR),
            input(state, DSL),
            parallel);
    data.setValue(word);
    for (var bit = 0; bit < WIDTH; bit++) {
      state.setPort(pinNrToPortNr(OUTPUT_PINS[bit]), word.get(bit), DELAY);
    }
  }

  private static TtlRegisterData getState(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.create(WIDTH));
      state.setData(data);
    }
    return data;
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }

  /**
   * A low master reset wins over a rising edge. Without an edge the register holds. On an edge,
   * {@code S1 S0} selects hold ({@code 00}), shift right ({@code 01}), shift left ({@code 10}) or
   * parallel load ({@code 11}).
   */
  private static Value resolve(
      Value stored,
      boolean triggered,
      Value masterReset,
      Value s0,
      Value s1,
      Value shiftRight,
      Value shiftLeft,
      Value[] parallel) {
    final var sawError =
        anyError(stored, masterReset, s0, s1, shiftRight, shiftLeft, parallel);
    if (masterReset == Value.FALSE) {
      return Value.createKnown(BitWidth.create(WIDTH), 0);
    }
    final var running =
        runningWord(stored, triggered, s0, s1, shiftRight, shiftLeft, parallel, sawError);
    if (masterReset == Value.TRUE) {
      return running;
    }
    return merge(Value.createKnown(BitWidth.create(WIDTH), 0), running, sawError);
  }

  private static Value runningWord(
      Value stored,
      boolean triggered,
      Value s0,
      Value s1,
      Value shiftRight,
      Value shiftLeft,
      Value[] parallel,
      boolean sawError) {
    if (!triggered) {
      return modeWord(HOLD, stored, shiftRight, shiftLeft, parallel, sawError);
    }
    Value merged = null;
    for (final var s1High : bothIfUncertain(s1)) {
      for (final var s0High : bothIfUncertain(s0)) {
        final var mode = (s1High ? 2 : 0) + (s0High ? 1 : 0);
        merged =
            merge(
                merged,
                modeWord(mode, stored, shiftRight, shiftLeft, parallel, sawError),
                sawError);
      }
    }
    return merged;
  }

  /** Next Q7..Q0 word for one fully chosen mode. Bit 0 of the result is Q0. */
  private static Value modeWord(
      int mode,
      Value stored,
      Value shiftRight,
      Value shiftLeft,
      Value[] parallel,
      boolean sawError) {
    final var bits = new Value[WIDTH];
    switch (mode) {
      case LOAD:
        for (var bit = 0; bit < WIDTH; bit++) {
          bits[bit] = driven(parallel[bit], sawError);
        }
        break;
      case SHIFT_RIGHT:
        bits[0] = driven(shiftRight, sawError);
        for (var bit = 0; bit < WIDTH - 1; bit++) {
          bits[bit + 1] = driven(stored.get(bit), sawError);
        }
        break;
      case SHIFT_LEFT:
        bits[WIDTH - 1] = driven(shiftLeft, sawError);
        for (var bit = 0; bit < WIDTH - 1; bit++) {
          bits[bit] = driven(stored.get(bit + 1), sawError);
        }
        break;
      case HOLD:
      default:
        for (var bit = 0; bit < WIDTH; bit++) {
          bits[bit] = driven(stored.get(bit), sawError);
        }
        break;
    }
    return Value.create(bits);
  }

  private static Value driven(Value bit, boolean sawError) {
    if (bit == Value.TRUE || bit == Value.FALSE) {
      return bit;
    }
    return sawError ? Value.ERROR : Value.UNKNOWN;
  }

  private static boolean[] bothIfUncertain(Value value) {
    if (value == Value.FALSE) {
      return new boolean[] {false};
    }
    if (value == Value.TRUE) {
      return new boolean[] {true};
    }
    return new boolean[] {false, true};
  }

  private static boolean anyError(
      Value stored,
      Value masterReset,
      Value s0,
      Value s1,
      Value shiftRight,
      Value shiftLeft,
      Value[] parallel) {
    if (masterReset == Value.ERROR
        || s0 == Value.ERROR
        || s1 == Value.ERROR
        || shiftRight == Value.ERROR
        || shiftLeft == Value.ERROR) {
      return true;
    }
    for (var bit = 0; bit < WIDTH; bit++) {
      if (stored.get(bit) == Value.ERROR || parallel[bit] == Value.ERROR) {
        return true;
      }
    }
    return false;
  }

  private static Value merge(Value left, Value right, boolean sawError) {
    if (left == null) {
      return right;
    }
    final var bits = new Value[WIDTH];
    for (var index = 0; index < WIDTH; index++) {
      final var first = left.get(index);
      final var second = right.get(index);
      bits[index] = first == second ? first : (sawError ? Value.ERROR : Value.UNKNOWN);
    }
    return Value.create(bits);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {pinNrToPortNr(CP)};
  }
}
