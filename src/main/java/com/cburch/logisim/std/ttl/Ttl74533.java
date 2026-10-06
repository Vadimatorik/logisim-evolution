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
 * TTL 74x533: octal inverting transparent latch with 3-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/cd74hc533.pdf">CD74HC533</a> function table. While
 * {@code LE} is high the latch is transparent and each stored bit is the complement of {@code Dn}.
 * {@code LE} falling keeps the complement that was present before the fall. {@code OE} high
 * releases every output and does not change the latch. Nanosecond delays are not modeled. A
 * released pin is {@link Value#UNKNOWN}, as on the other three-state TTL models.
 *
 * <p>An unknown or error on {@code LE} or {@code Dn} changes a stored bit only when the two
 * substitutions disagree. An error on such an input makes the disagreed bit an error; an unknown
 * input makes it unknown. {@code OE} low drives the stored bit. {@code OE} high or unknown releases
 * the pin. An error on {@code OE} makes the pin an error.
 *
 * <p>There is no HDL generator. The FPGA design-rule check rejects three-state drivers, and a model
 * that kept driving while {@code OE} was high would not match the latch.
 */
public class Ttl74533 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74533";

  public static final int DELAY = 1;

  /** Output enable, active low. High releases Q0 to Q7. */
  public static final byte OE = 1;

  public static final byte Q0 = 2;
  public static final byte D0 = 3;
  public static final byte D1 = 4;
  public static final byte Q1 = 5;
  public static final byte Q2 = 6;
  public static final byte D2 = 7;
  public static final byte D3 = 8;
  public static final byte Q3 = 9;
  public static final byte GND = 10;

  /** Latch enable, active high. Level-sensitive, not an edge clock. */
  public static final byte LE = 11;

  public static final byte Q4 = 12;
  public static final byte D4 = 13;
  public static final byte D5 = 14;
  public static final byte Q5 = 15;
  public static final byte Q6 = 16;
  public static final byte D6 = 17;
  public static final byte D7 = 18;
  public static final byte Q7 = 19;
  public static final byte VCC = 20;

  private static final int WIDTH = 8;
  private static final BitWidth LATCH_WIDTH = BitWidth.create(WIDTH);
  private static final byte[] OUTPUT_PINS = {Q0, Q1, Q2, Q3, Q4, Q5, Q6, Q7};
  private static final byte[] DATA_PINS = {D0, D1, D2, D3, D4, D5, D6, D7};
  private static final String[] PORT_NAMES = {
    "OE (output enable, active low)",
    "Q0",
    "D0",
    "D1",
    "Q1",
    "Q2",
    "D2",
    "D3",
    "Q3",
    "LE (latch enable, active high)",
    "Q4",
    "D4",
    "D5",
    "Q5",
    "Q6",
    "D6",
    "D7",
    "Q7"
  };
  private static final String[] PIN_NAMES = {
    "OE", "Q0", "D0", "D1", "Q1", "Q2", "D2", "D3", "Q3", null,
    "LE", "Q4", "D4", "D5", "Q5", "Q6", "D6", "D7", "Q7", null
  };

  /** Creates a 74533 octal inverting transparent latch. */
  public Ttl74533() {
    super(_ID, (byte) 20, OUTPUT_PINS, PORT_NAMES, null);
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
    drawLatch(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private static void drawLatch(Graphics2D gfx, int x, int y, int height, TtlRegisterData data) {
    if (data == null) {
      return;
    }
    final var value = data.getValue();
    for (var bit = WIDTH - 1; bit >= 0; bit--) {
      final var shown = value.get(bit);
      final var originX = x + 44 + (WIDTH - 1 - bit) * 14;
      gfx.setColor(shown.getColor());
      gfx.fillOval(originX, y + height / 2 - 4, 8, 8);
      gfx.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(gfx, shown.toDisplayString(), originX + 4, y + height / 2);
    }
    gfx.setColor(Color.BLACK);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getState(state);
    final var inputs = new Value[WIDTH];
    for (var bit = 0; bit < WIDTH; bit++) {
      inputs[bit] = input(state, DATA_PINS[bit]);
    }
    final var latched = resolveLatch(data.getValue(), input(state, LE), inputs);
    data.setValue(latched);
    final var outputEnable = input(state, OE);
    for (var bit = 0; bit < WIDTH; bit++) {
      state.setPort(pinNrToPortNr(OUTPUT_PINS[bit]), drive(latched.get(bit), outputEnable), DELAY);
    }
  }

  private static TtlRegisterData getState(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(LATCH_WIDTH);
      state.setData(data);
    }
    return data;
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }

  /**
   * Stored bit is the complement of {@code Dn} while {@code LE} is high, and the previous
   * complement while {@code LE} is low.
   */
  private static Value resolveLatch(Value stored, Value latchEnable, Value[] data) {
    final var bits = new Value[WIDTH];
    for (var bit = 0; bit < WIDTH; bit++) {
      bits[bit] = resolveBit(stored.get(bit), latchEnable, data[bit]);
    }
    return Value.create(bits);
  }

  private static Value resolveBit(Value storedBit, Value latchEnable, Value dataBit) {
    if (latchEnable == Value.FALSE) {
      return storedBit;
    }
    final var transparent = invert(dataBit);
    if (latchEnable == Value.TRUE) {
      return transparent;
    }
    if (storedBit == transparent) {
      return storedBit;
    }
    if (latchEnable == Value.ERROR || storedBit == Value.ERROR || dataBit == Value.ERROR) {
      return Value.ERROR;
    }
    return Value.UNKNOWN;
  }

  /** {@link Value#not()} turns unknown into error, so a released data bit stays unknown. */
  private static Value invert(Value bit) {
    if (bit == Value.TRUE) {
      return Value.FALSE;
    }
    if (bit == Value.FALSE) {
      return Value.TRUE;
    }
    if (bit == Value.ERROR) {
      return Value.ERROR;
    }
    return Value.UNKNOWN;
  }

  /** Active-low output enable. Anything other than a solid low releases the pin. */
  private static Value drive(Value latched, Value outputEnable) {
    if (outputEnable == Value.FALSE) {
      return latched;
    }
    if (outputEnable == Value.ERROR) {
      return Value.ERROR;
    }
    return Value.UNKNOWN;
  }
}
