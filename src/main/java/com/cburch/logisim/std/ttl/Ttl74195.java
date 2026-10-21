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
 * TTL 74x195: 4-bit parallel-access shift register.
 *
 * <p>Simulation follows the Philips 74HC/HCT195 and TI CD74HC195 data sheets. A low master reset
 * clears every stage at once. Parallel load and the shift are both synchronous: they happen on the
 * rising edge of the clock. While parallel enable is low, that edge copies D0 to D3 into Q0 to Q3.
 * While it is high, the edge shifts Q0 toward Q3. The first stage is a JK input, and the K pin is
 * active low, so tying J and K together makes a D input. The complementary output is the inverse
 * of Q3. Nanosecond delays are not modeled.
 *
 * <p>An unknown or error input changes an output only when the two substitutions disagree. An error
 * on such an input makes the disagreed output an error; an unknown input makes it unknown.
 */
public class Ttl74195 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74195";

  public static final int DELAY = 1;

  public static final byte MR = 1;
  public static final byte J = 2;
  public static final byte K = 3;
  public static final byte D0 = 4;
  public static final byte D1 = 5;
  public static final byte D2 = 6;
  public static final byte D3 = 7;
  public static final byte GND = 8;
  public static final byte PE = 9;
  public static final byte CP = 10;
  public static final byte NQ3 = 11;
  public static final byte Q3 = 12;
  public static final byte Q2 = 13;
  public static final byte Q1 = 14;
  public static final byte Q0 = 15;
  public static final byte VCC = 16;

  private static final int WIDTH = 4;
  private static final byte[] OUTPUT_PINS = {NQ3, Q3, Q2, Q1, Q0};
  private static final String[] PORT_NAMES = {
    "MR (Master reset, active low)",
    "J (First stage J)",
    "K (First stage K, active low)",
    "D0",
    "D1",
    "D2",
    "D3",
    "PE (Parallel enable, active low)",
    "CP (Clock)",
    "nQ3",
    "Q3",
    "Q2",
    "Q1",
    "Q0"
  };
  private static final String[] PIN_NAMES = {
    "MR", "J", "K", "D0", "D1", "D2", "D3", null,
    "PE", "CP", "nQ3", "Q3", "Q2", "Q1", "Q0", null
  };

  private static final int BIT_RESET_HIGH = 0;
  private static final int BIT_SHIFT = 1;
  private static final int BIT_J = 2;
  private static final int BIT_K = 3;
  private static final int BIT_DATA = 4;
  private static final int BIT_STORED = 8;
  private static final int SOURCE_BITS = 12;

  /** Creates a 74195 4-bit parallel-access shift register. */
  public Ttl74195() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl74195HdlGenerator());
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
    final var rawState = painter.getData();
    final TtlRegisterData stored = rawState instanceof TtlRegisterData ? (TtlRegisterData) rawState : null;
    g.drawRect((x + 24), (y + 11), 12, 14);
    g.drawLine((x + 30), (y + 11), (x + 30), (y + 7));
    g.drawLine((x + 24), (((y + 11)) + 7) - 3, ((x + 24)) + 4, ((y + 11)) + 7);
    g.drawLine((x + 24), (((y + 11)) + 7) + 3, ((x + 24)) + 4, ((y + 11)) + 7);
    structureDot(g, (x + 30), ((y + 11)) + 7, stored, 0, 0);
    g.drawRect((x + 44), (y + 11), 12, 14);
    g.drawLine((x + 50), (y + 11), (x + 50), (y + 7));
    g.drawLine((x + 44), (((y + 11)) + 7) - 3, ((x + 44)) + 4, ((y + 11)) + 7);
    g.drawLine((x + 44), (((y + 11)) + 7) + 3, ((x + 44)) + 4, ((y + 11)) + 7);
    g.drawRect((x + 64), (y + 11), 12, 14);
    g.drawLine((x + 70), (y + 11), (x + 70), (y + 7));
    g.drawLine((x + 64), (((y + 11)) + 7) - 3, ((x + 64)) + 4, ((y + 11)) + 7);
    g.drawLine((x + 64), (((y + 11)) + 7) + 3, ((x + 64)) + 4, ((y + 11)) + 7);
    g.drawRect((x + 84), (y + 11), 12, 14);
    g.drawLine((x + 90), (y + 11), (x + 90), (y + 7));
    g.drawLine((x + 84), (((y + 11)) + 7) - 3, ((x + 84)) + 4, ((y + 11)) + 7);
    g.drawLine((x + 84), (((y + 11)) + 7) + 3, ((x + 84)) + 4, ((y + 11)) + 7);
    g.drawLine((x + 36), ((y + 11)) + 7, (x + 40), ((y + 11)) + 7);
    g.drawLine((x + 56), ((y + 11)) + 7, (x + 60), ((y + 11)) + 7);
    g.drawLine((x + 76), ((y + 11)) + 7, (x + 80), ((y + 11)) + 7);
    g.drawLine((x + 130), (y + 7), (x + 130), (y + 13));
    g.drawLine((x + 130), (y + 13), (x + 19), (y + 13));
    g.drawLine((x + 19), (y + 13), (x + 19), ((y + 11)) + 7);
    g.drawLine((x + 30), (y + 27), (x + 90), (y + 27));
    g.drawLine((x + 30), (y + 27), (x + 30), (y + 11));
    g.drawLine((x + 30), (y + 27), (x + 30), ((y + 11)) + 14);
    g.drawLine((x + 50), (y + 27), (x + 50), (y + 11));
    g.drawLine((x + 50), (y + 27), (x + 50), ((y + 11)) + 14);
    g.drawLine((x + 70), (y + 27), (x + 70), (y + 11));
    g.drawLine((x + 70), (y + 27), (x + 70), ((y + 11)) + 14);
    g.drawLine((x + 90), (y + 27), (x + 90), (y + 11));
    g.drawLine((x + 90), (y + 27), (x + 90), ((y + 11)) + 14);
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + height - 13));
    g.drawLine((x + 10), (y + height - 13), (x + 30), (y + height - 13));
    g.drawLine((x + 30), (y + height - 13), (x + 30), (y + 27));
    structureBubble(g, (x + 10), (y + height - 7));
    g.drawLine((x + 30), (y + height - 7), (x + 30), (y + height - 13));
    g.drawLine((x + 30), (y + height - 13), (x + 22), (y + height - 13));
    g.drawLine((x + 22), (y + height - 13), (x + 22), (y + 20));
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getState(state);
    final var triggered = data.updateClock(input(state, CP), StdAttr.TRIG_RISING);
    final var word =
        resolve(
            data.getValue(),
            triggered,
            new Value[] {
              input(state, MR),
              input(state, PE),
              input(state, J),
              input(state, K),
              input(state, D0),
              input(state, D1),
              input(state, D2),
              input(state, D3)
            });
    data.setValue(word);
    state.setPort(pinNrToPortNr(Q0), word.get(0), DELAY);
    state.setPort(pinNrToPortNr(Q1), word.get(1), DELAY);
    state.setPort(pinNrToPortNr(Q2), word.get(2), DELAY);
    state.setPort(pinNrToPortNr(Q3), word.get(3), DELAY);
    state.setPort(pinNrToPortNr(NQ3), word.get(3).not(), DELAY);
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
   * A low master reset wins over a rising edge. Without an edge the register holds. A low parallel
   * enable loads D0 to D3; a high one shifts toward Q3 and applies the JK function to Q0.
   */
  private static Value resolve(Value stored, boolean triggered, Value[] controls) {
    final var sources = new Value[SOURCE_BITS];
    System.arraycopy(controls, 0, sources, 0, controls.length);
    for (var bit = 0; bit < WIDTH; bit++) {
      sources[BIT_STORED + bit] = stored.get(bit);
    }
    final var choice = new Choice(WIDTH);
    for (var mask = 0; mask < (1 << SOURCE_BITS); mask++) {
      if (!accepts(sources, mask)) {
        continue;
      }
      choice.accept(nextWord(mask, triggered), errorIn(sources));
    }
    return choice.value();
  }

  /** Next Q3..Q0 word. Bit 0 of the result is Q0. */
  static int nextWord(int mask, boolean triggered) {
    if (!bitHigh(mask, BIT_RESET_HIGH)) {
      return 0;
    }
    final var stored = (mask >> BIT_STORED) & 0xF;
    if (!triggered) {
      return stored;
    }
    if (!bitHigh(mask, BIT_SHIFT)) {
      return (mask >> BIT_DATA) & 0xF;
    }
    final var serial = serialBit(bitHigh(mask, BIT_J), bitHigh(mask, BIT_K), (stored & 1) != 0);
    return (serial ? 1 : 0) | ((stored & 0x7) << 1);
  }

  /**
   * First-stage JK function for the pin levels. Both high sets Q0, both low clears it, J high with
   * K low toggles, and J low with K high retains.
   */
  static boolean serialBit(boolean jHigh, boolean kHigh, boolean q0) {
    return (jHigh && !q0) || (kHigh && q0);
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

  private static boolean errorIn(Value[] sources) {
    for (final var source : sources) {
      if (source == Value.ERROR) {
        return true;
      }
    }
    return false;
  }

  private static boolean bitHigh(int mask, int bit) {
    return (mask & (1 << bit)) != 0;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {pinNrToPortNr(CP)};
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

    private void accept(int value, boolean error) {
      sawError |= error;
      if (!any) {
        any = true;
        for (var index = 0; index < bits.length; index++) {
          bits[index] = ((value >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
        }
        return;
      }
      for (var index = 0; index < bits.length; index++) {
        final var bit = ((value >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
        if (bits[index] != bit) {
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
