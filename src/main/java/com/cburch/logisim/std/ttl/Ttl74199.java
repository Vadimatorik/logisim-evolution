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
 * TTL 74x199: 8-bit parallel-access shift register.
 *
 * <p>Simulation follows the Philips 74F199 function table, which is the same function as the
 * 74LS199 and 74S199. A low master reset clears every stage at once. Parallel load and the shift
 * are synchronous: they happen on the rising edge of the clock, and only when clock enable is low.
 * A high clock enable holds the register. While parallel enable is low, that edge copies D0 to D7
 * into Q0 to Q7. While it is high, the edge shifts Q0 toward Q7. The first stage is a JK input,
 * and the K pin is active low, so tying J and K together makes a D input. Nanosecond delays are
 * not modeled.
 *
 * <p>An unknown or error input changes an output only when the two substitutions disagree. An error
 * on such an input makes the disagreed output an error; an unknown input makes it unknown.
 */
public class Ttl74199 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74199";

  public static final int DELAY = 1;

  public static final byte J = 1;
  public static final byte K = 2;
  public static final byte D0 = 3;
  public static final byte Q0 = 4;
  public static final byte D1 = 5;
  public static final byte Q1 = 6;
  public static final byte D2 = 7;
  public static final byte Q2 = 8;
  public static final byte D3 = 9;
  public static final byte Q3 = 10;
  public static final byte CE = 11;
  public static final byte GND = 12;
  public static final byte CP = 13;
  public static final byte MR = 14;
  public static final byte Q4 = 15;
  public static final byte D4 = 16;
  public static final byte Q5 = 17;
  public static final byte D5 = 18;
  public static final byte Q6 = 19;
  public static final byte D6 = 20;
  public static final byte Q7 = 21;
  public static final byte D7 = 22;
  public static final byte PE = 23;
  public static final byte VCC = 24;

  private static final int WIDTH = 8;
  private static final byte[] DATA_PINS = {D0, D1, D2, D3, D4, D5, D6, D7};
  private static final byte[] OUTPUT_PINS = {Q0, Q1, Q2, Q3, Q4, Q5, Q6, Q7};
  private static final String[] PORT_NAMES = {
    "J (First stage J)",
    "K (First stage K, active LOW)",
    "D0",
    "Q0",
    "D1",
    "Q1",
    "D2",
    "Q2",
    "D3",
    "Q3",
    "CE (Clock enable, active LOW)",
    "CP (Clock)",
    "MR (Master reset, active LOW)",
    "Q4",
    "D4",
    "Q5",
    "D5",
    "Q6",
    "D6",
    "Q7",
    "D7",
    "PE (Parallel enable, active LOW)"
  };
  private static final String[] PIN_NAMES = {
    "J", "K", "D0", "Q0", "D1", "Q1", "D2", "Q2", "D3", "Q3", "CE", null,
    "CP", "MR", "Q4", "D4", "Q5", "D5", "Q6", "D6", "Q7", "D7", "PE", null
  };

  /** Creates a 74199 8-bit parallel-access shift register. */
  public Ttl74199() {
    super(_ID, (byte) 24, OUTPUT_PINS, PORT_NAMES, new Ttl74199HdlGenerator());
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
            input(state, CE),
            input(state, PE),
            input(state, J),
            input(state, K),
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
   * A low master reset wins over a rising edge. Without an edge, or with clock enable high, the
   * register holds. A low parallel enable loads D0 to D7; a high one shifts toward Q7 and applies
   * the JK function to Q0.
   */
  private static Value resolve(
      Value stored,
      boolean triggered,
      Value masterReset,
      Value clockEnable,
      Value parallelEnable,
      Value jPin,
      Value kPin,
      Value[] parallel) {
    final var sawError =
        anyError(stored, masterReset, clockEnable, parallelEnable, jPin, kPin, parallel);
    if (masterReset == Value.FALSE) {
      return Value.createKnown(BitWidth.create(WIDTH), 0);
    }
    final var running =
        runningWord(stored, triggered, clockEnable, parallelEnable, jPin, kPin, parallel, sawError);
    if (masterReset == Value.TRUE) {
      return running;
    }
    return merge(Value.createKnown(BitWidth.create(WIDTH), 0), running, sawError);
  }

  private static Value runningWord(
      Value stored,
      boolean triggered,
      Value clockEnable,
      Value parallelEnable,
      Value jPin,
      Value kPin,
      Value[] parallel,
      boolean sawError) {
    if (!triggered) {
      return hold(stored, sawError);
    }
    Value merged = null;
    for (final var ceHigh : bothIfUncertain(clockEnable)) {
      for (final var peHigh : bothIfUncertain(parallelEnable)) {
        final Value next;
        if (ceHigh) {
          next = hold(stored, sawError);
        } else if (!peHigh) {
          next = load(parallel, sawError);
        } else {
          next = shift(stored, jPin, kPin, sawError);
        }
        merged = merge(merged, next, sawError);
      }
    }
    return merged;
  }

  /** Next Q7..Q0 word. Bit 0 of the result is Q0. */
  private static Value shift(Value stored, Value jPin, Value kPin, boolean sawError) {
    final var bits = new Value[WIDTH];
    bits[0] = serialBit(jPin, kPin, stored.get(0), sawError);
    for (var bit = 0; bit < WIDTH - 1; bit++) {
      bits[bit + 1] = driven(stored.get(bit), sawError);
    }
    return Value.create(bits);
  }

  /**
   * First-stage JK function for the pin levels. Both high sets Q0, both low clears it, J high with
   * K low toggles, and J low with K high retains.
   */
  private static Value serialBit(Value jPin, Value kPin, Value q0, boolean sawError) {
    Value merged = null;
    for (final var jHigh : bothIfUncertain(jPin)) {
      for (final var kHigh : bothIfUncertain(kPin)) {
        for (final var q0High : bothIfUncertain(q0)) {
          final var high = (jHigh && !q0High) || (kHigh && q0High);
          merged = mergeBit(merged, high ? Value.TRUE : Value.FALSE, sawError);
        }
      }
    }
    return merged;
  }

  private static Value hold(Value stored, boolean sawError) {
    final var bits = new Value[WIDTH];
    for (var bit = 0; bit < WIDTH; bit++) {
      bits[bit] = driven(stored.get(bit), sawError);
    }
    return Value.create(bits);
  }

  private static Value load(Value[] parallel, boolean sawError) {
    final var bits = new Value[WIDTH];
    for (var bit = 0; bit < WIDTH; bit++) {
      bits[bit] = driven(parallel[bit], sawError);
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
      Value clockEnable,
      Value parallelEnable,
      Value jPin,
      Value kPin,
      Value[] parallel) {
    if (masterReset == Value.ERROR
        || clockEnable == Value.ERROR
        || parallelEnable == Value.ERROR
        || jPin == Value.ERROR
        || kPin == Value.ERROR) {
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
      bits[index] = mergeBit(left.get(index), right.get(index), sawError);
    }
    return Value.create(bits);
  }

  private static Value mergeBit(Value left, Value right, boolean sawError) {
    if (left == null) {
      return right;
    }
    if (left == right) {
      return left;
    }
    return sawError ? Value.ERROR : Value.UNKNOWN;
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
