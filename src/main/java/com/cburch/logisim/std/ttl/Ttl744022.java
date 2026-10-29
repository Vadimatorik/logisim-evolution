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
 * TTL 74x4022: 4-stage divide-by-8 Johnson counter with 8 decoded outputs.
 *
 * <p>Simulation follows the Philips HEF4022B function table and the
 * <a href="https://www.ti.com/lit/ds/symlink/cd4022b.pdf">TI CD4022B</a> terminal diagram. The
 * 74HC4022 uses the same pins. The counter advances on the low-to-high transition of {@code CP0}
 * while {@code CP1} is low, or on the high-to-low transition of {@code CP1} while {@code CP0} is
 * high. A high {@code MR} asynchronously selects decoded output 0 ({@code Q0} and {@code Q4-7}
 * high, {@code Q1} to {@code Q7} low). {@code Q4-7} is high for counts 0 to 3 and low for counts 4
 * to 7. TI calls {@code CP0} CLOCK, {@code CP1} CLOCK INHIBIT, {@code MR} RESET and {@code Q4-7}
 * CARRY OUT. Pins 6 and 9 are not connected. Nanosecond delays are not modeled. Illegal Johnson
 * codes cannot be entered from the pins, so the internal correction circuit is not simulated.
 */
public class Ttl744022 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744022";

  public static final int PORT_INDEX_Q1 = 0;
  public static final int PORT_INDEX_Q0 = 1;
  public static final int PORT_INDEX_Q2 = 2;
  public static final int PORT_INDEX_Q5 = 3;
  public static final int PORT_INDEX_Q6 = 4;
  public static final int PORT_INDEX_Q3 = 5;
  public static final int PORT_INDEX_Q7 = 6;
  public static final int PORT_INDEX_Q4 = 7;
  public static final int PORT_INDEX_Q4_7 = 8;
  public static final int PORT_INDEX_CP1 = 9;
  public static final int PORT_INDEX_CP0 = 10;
  public static final int PORT_INDEX_MR = 11;

  private static final int DELAY = 4;
  private static final int OCTAL = 8;
  private static final BitWidth WIDTH = BitWidth.create(3);
  private static final byte[] OUTPUT_PORTS = {1, 2, 3, 4, 5, 7, 10, 11, 12};
  private static final byte[] UNUSED_PINS = {6, 9};
  private static final String[] PORT_NAMES = {
    "Q1",
    "Q0",
    "Q2",
    "Q5",
    "Q6",
    "Q3",
    "Q7",
    "Q4",
    "Q4-7 (carry, active low)",
    "CP1 (clock, high-to-low)",
    "CP0 (clock, low-to-high)",
    "MR (master reset, active high)"
  };
  /** Decoded outputs Q0 to Q7, in count order. */
  private static final int[] DECODED_PORTS = {
    PORT_INDEX_Q0,
    PORT_INDEX_Q1,
    PORT_INDEX_Q2,
    PORT_INDEX_Q3,
    PORT_INDEX_Q4,
    PORT_INDEX_Q5,
    PORT_INDEX_Q6,
    PORT_INDEX_Q7
  };

  /** Creates a 744022 divide-by-8 Johnson counter. */
  public Ttl744022() {
    super(_ID, (byte) 16, OUTPUT_PORTS, UNUSED_PINS, PORT_NAMES, null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    g.drawRect((x + 12), (y + 18), 122, 24);
    GraphicsUtil.drawCenteredText(g, "JOH", (x + 73), (y + 30));
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 18));
    g.drawLine((x + 70), (y + 7), (x + 70), (y + 18));
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 18));
    g.drawLine((x + 30), (y + height - 7), (x + 30), (y + 42));
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + 42));
    g.drawLine((x + 10), (y + 42), (x + 12), (y + 42));
    g.drawLine((x + 50), (y + height - 7), (x + 50), (y + 42));
    g.drawLine((x + 130), (y + height - 7), (x + 130), (y + 42));
    g.drawLine((x + 110), (y + 7), (x + 110), (y + 18));
    g.drawLine((x + 70), (y + height - 7), (x + 70), (y + 42));
    g.drawLine((x + 90), (y + height - 7), (x + 90), (y + 42));
    g.drawLine((x + 130), (y + 7), (x + 130), (y + 18));
  }

  private static Color stateColor(Value value) {
    if (containsBit(value, Value.ERROR)) return Value.ERROR.getColor();
    if (!value.isFullyDefined()) return Value.UNKNOWN.getColor();
    return Value.TRUE.getColor();
  }

  private static TtlRegisterData getStateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WIDTH);
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);
    final var cp0 = state.getPortValue(PORT_INDEX_CP0);
    final var cp1 = state.getPortValue(PORT_INDEX_CP1);
    final var cp0Rising = data.updateClock(cp0, 0, StdAttr.TRIG_RISING);
    final var cp1Falling = data.updateClock(cp1, 1, StdAttr.TRIG_FALLING);
    if (state.getPortValue(PORT_INDEX_MR) == Value.TRUE) {
      data.setValue(Value.createKnown(WIDTH, 0));
    } else if (cp0Rising && cp1 == Value.FALSE) {
      data.setValue(nextCount(data.getValue()));
    } else if (cp1Falling && cp0 == Value.TRUE) {
      data.setValue(nextCount(data.getValue()));
    }
    publish(state, data.getValue());
  }

  /**
   * Advances one octal count. An error anywhere makes the next count entirely error. A count that
   * is only unknown, or a defined value outside 0 to 7, stays unknown. A legal count increments
   * modulo 8.
   */
  private static Value nextCount(Value current) {
    if (containsBit(current, Value.ERROR)) return Value.createError(WIDTH);
    if (!current.isFullyDefined()) return Value.createUnknown(WIDTH);
    final var count = current.toLongValue();
    if (count > 7) return Value.createUnknown(WIDTH);
    return Value.createKnown(WIDTH, (count + 1) % OCTAL);
  }

  private void publish(InstanceState state, Value count) {
    if (containsBit(count, Value.ERROR) || !count.isFullyDefined() || count.toLongValue() > 7) {
      final var bit = containsBit(count, Value.ERROR) ? Value.ERROR : Value.UNKNOWN;
      for (final var port : DECODED_PORTS) {
        state.setPort(port, bit, DELAY);
      }
      state.setPort(PORT_INDEX_Q4_7, bit, DELAY);
      return;
    }
    final var step = (int) count.toLongValue();
    for (var output = 0; output < DECODED_PORTS.length; output++) {
      state.setPort(DECODED_PORTS[output], output == step ? Value.TRUE : Value.FALSE, DELAY);
    }
    state.setPort(PORT_INDEX_Q4_7, step < 4 ? Value.TRUE : Value.FALSE, DELAY);
  }

  private static boolean containsBit(Value word, Value bit) {
    for (var i = 0; i < word.getWidth(); i++) {
      if (word.get(i) == bit) return true;
    }
    return false;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_CP0, PORT_INDEX_CP1};
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
