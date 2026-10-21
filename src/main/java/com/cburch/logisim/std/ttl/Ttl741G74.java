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
 * TTL 74LVC1G74: single positive-edge D flip-flop with asynchronous set and reset.
 *
 * <p>The library id is {@code 741G74}. There is no 74HC1G74 data sheet; simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74LVC1G74.pdf">Nexperia 74LVC1G74</a>
 * Rev. 18 function table for the standard 8-pin packages (TSSOP8, VSSOP8 and XSON8). The
 * <a href="https://www.ti.com/lit/ds/symlink/sn74lvc1g74.pdf">TI SN74LVC1G74</a> Rev. G table
 * agrees. The rotated Nexperia SOT902-2 pin order is not used.
 *
 * <p>{@code SD} and {@code RD} are active low and override the clock. Both low forces {@code Q}
 * and {@code nQ} high. That pair does not stay when one input is released: the input that remains
 * low sets the outputs at once. With both inputs high, {@code D} is stored on the rising edge of
 * {@code CP} and {@code nQ} is its complement. A high level or a falling edge leaves the stored
 * pair unchanged, including the both-high pair. Nanosecond delays are not modeled.
 *
 * <p>An unknown or error input changes an output only when the two substitutions disagree. An
 * error on such an input makes the disagreed output an error; an unknown input makes it unknown.
 */
public class Ttl741G74 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "741G74";

  public static final int DELAY = 1;

  /** Rising-edge clock. Logical port of datasheet pin 1. */
  public static final int PORT_CP = 0;

  /** Data input. Logical port of datasheet pin 2. */
  public static final int PORT_D = 1;

  /** Complement output. Logical port of datasheet pin 3. */
  public static final int PORT_NQ = 2;

  /** True output. Logical port of datasheet pin 5. */
  public static final int PORT_Q = 3;

  /** Asynchronous reset, active low. Logical port of datasheet pin 6. */
  public static final int PORT_RD = 4;

  /** Asynchronous set, active low. Logical port of datasheet pin 7. */
  public static final int PORT_SD = 5;

  /** Port index of GND when the explicit power pins are shown. Datasheet pin 4. */
  public static final int PORT_GND = 6;

  /** Port index of VCC when the explicit power pins are shown. Datasheet pin 8. */
  public static final int PORT_VCC = 7;

  private static final BitWidth WIDTH = BitWidth.create(2);
  private static final int Q_BIT = 0;
  private static final int NQ_BIT = 1;
  /** Q high and nQ low. Bit 0 is Q. */
  private static final int SET_STATE = 0b01;
  /** Q low and nQ high. */
  private static final int CLEAR_STATE = 0b10;
  /** Both outputs high, the result of asserting set and reset together. */
  private static final int BOTH_HIGH = 0b11;

  private static final byte NQ_PIN = 3;
  private static final byte Q_PIN = 5;
  private static final byte[] OUTPUT_PINS = {NQ_PIN, Q_PIN};
  private static final String[] PORT_NAMES = {
    "CP (clock, rising edge)",
    "D",
    "nQ",
    "Q",
    "RD (reset, active low)",
    "SD (set, active low)"
  };
  private static final String[] PIN_NAMES = {"CP", "D", "nQ", null, "Q", "RD", "SD", null};

  private static final int BIT_SD = 0;
  private static final int BIT_RD = 1;
  private static final int BIT_D = 2;
  private static final int BIT_Q = 3;
  private static final int BIT_NQ = 4;
  private static final int SOURCE_COUNT = 5;

  /** Creates a 741G74 single D flip-flop. */
  public Ttl741G74() {
    super(_ID, (byte) 8, OUTPUT_PINS, PORT_NAMES, new Ttl741G74HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var rawState = painter.getData();
    final TtlRegisterData stored = rawState instanceof TtlRegisterData ? (TtlRegisterData) rawState : null;
    g.drawRect((x + 14), (y + 18), 52, 24);
    GraphicsUtil.drawCenteredText(g, "D", (x + 22), (y + 30));
    GraphicsUtil.drawCenteredText(g, "Q", (x + 58), (y + 26));
    GraphicsUtil.drawCenteredText(g, "Q", (x + 58), (y + 36));
    structureBubble(g, (x + 58), (y + 32));
    structureDot(g, (x + 40), (y + 30), stored, 0, 0);
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + height - 13));
    g.drawLine((x + 10), (y + height - 13), (x + 25), (y + height - 13));
    g.drawLine((x + 25), (y + height - 13), (x + 25), (y + 42));
    g.drawLine((x + 25) - 3, (y + 42), (x + 25), ((y + 42)) - 4);
    g.drawLine((x + 25) + 3, (y + 42), (x + 25), ((y + 42)) - 4);
    g.drawLine((x + 30), (y + height - 7), (x + 30), (y + 42));
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 18));
    structureBubble(g, (x + 50), ((y + 18)) - 5);
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 18));
    structureBubble(g, (x + 30), ((y + 18)) - 5);
    g.drawLine((x + 70), (y + 7), (x + 70), (y + 13));
    g.drawLine((x + 70), (y + 13), (x + 40), (y + 13));
    g.drawLine((x + 40), (y + 13), (x + 40), (y + 18));
    g.drawLine((x + 50), (y + height - 7), (x + 50), (y + 42));
    structureBubble(g, (x + 50), ((y + 42)) + 5);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getState(state);
    final var triggered = data.updateClock(state.getPortValue(PORT_CP), StdAttr.TRIG_RISING);
    final var next =
        resolve(
            data.getValue(),
            triggered,
            state.getPortValue(PORT_SD),
            state.getPortValue(PORT_RD),
            state.getPortValue(PORT_D));
    data.setValue(next);
    state.setPort(PORT_Q, next.get(Q_BIT), DELAY);
    state.setPort(PORT_NQ, next.get(NQ_BIT), DELAY);
  }

  private static TtlRegisterData getState(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WIDTH);
      state.setData(data);
    }
    return data;
  }

  /**
   * Set wins over reset only in the sense that both asserted forces both outputs high. Either
   * asserted input wins over the clock. A rising edge is used only while both inputs are high.
   */
  private static Value resolve(Value stored, boolean triggered, Value sd, Value rd, Value data) {
    final var sources =
        new Value[] {sd, rd, data, stored.get(Q_BIT), stored.get(NQ_BIT)};
    final var choice = new Choice();
    for (var mask = 0; mask < (1 << SOURCE_COUNT); mask++) {
      if (!accepts(sources, mask)) {
        continue;
      }
      choice.accept(definedNext(mask, triggered), errorIn(sources));
    }
    return choice.value();
  }

  private static int definedNext(int mask, boolean triggered) {
    final var sdHigh = bitHigh(mask, BIT_SD);
    final var rdHigh = bitHigh(mask, BIT_RD);
    if (!sdHigh && !rdHigh) {
      return BOTH_HIGH;
    }
    if (!sdHigh) {
      return SET_STATE;
    }
    if (!rdHigh) {
      return CLEAR_STATE;
    }
    if (triggered) {
      return bitHigh(mask, BIT_D) ? SET_STATE : CLEAR_STATE;
    }
    final var q = bitHigh(mask, BIT_Q) ? 1 : 0;
    final var nq = bitHigh(mask, BIT_NQ) ? 2 : 0;
    return q | nq;
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
    return new int[] {PORT_CP};
  }

  /** Merges every accepted substitution. Disagreements become unknown, or error if one was seen. */
  private static final class Choice {
    private final Value[] bits = new Value[WIDTH.getWidth()];
    private final boolean[] conflict = new boolean[WIDTH.getWidth()];
    private boolean sawError;
    private boolean any;

    private void accept(int value, boolean error) {
      sawError |= error;
      if (!any) {
        any = true;
        for (var index = 0; index < bits.length; index++) {
          bits[index] = bit(value, index);
        }
        return;
      }
      for (var index = 0; index < bits.length; index++) {
        if (bits[index] != bit(value, index)) {
          conflict[index] = true;
        }
      }
    }

    private Value value() {
      if (!any) {
        return Value.createUnknown(WIDTH);
      }
      for (var index = 0; index < bits.length; index++) {
        if (conflict[index]) {
          bits[index] = sawError ? Value.ERROR : Value.UNKNOWN;
        }
      }
      return Value.create(bits);
    }

    private static Value bit(int value, int index) {
      return ((value >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
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
