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
 * TTL 74x40103: 8-bit synchronous binary down counter.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC40103.pdf">Nexperia 74HC40103</a>
 * data sheet, Rev. 6 (26 March 2024). {@code MR} low asynchronously loads 255. Otherwise {@code PL}
 * low asynchronously loads {@code P7} to {@code P0}. Otherwise a rising {@code CP} samples
 * {@code PE}: low loads the jam inputs regardless of {@code TE}, and high counts down when
 * {@code TE} is low. The counter wraps from 0 to 255. {@code TC} is active low and combinational:
 * it is low only while the code is 0 and {@code TE} is low. Nanosecond delays are not modeled.
 * This is not the CD40103, whose pinout and clock edge differ.
 *
 * <p>An unknown or error input changes a result only when the two substitutions disagree. An error
 * on such an input makes the disagreed result an error; an unknown input makes it unknown.
 */
public class Ttl7440103 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "7440103";

  public static final int DELAY = 1;

  /** Rising-edge clock. */
  public static final byte CP = 1;

  /** Asynchronous master reset, active low. Clears the counter to 255. */
  public static final byte MR = 2;

  /** Terminal enable, active low. High inhibits counting and forces {@link #TC} high. */
  public static final byte TE = 3;

  public static final byte P0 = 4;
  public static final byte P1 = 5;
  public static final byte P2 = 6;
  public static final byte P3 = 7;
  public static final byte GND = 8;

  /** Asynchronous preset, active low. Forces {@code P7} to {@code P0} into the counter. */
  public static final byte PL = 9;

  public static final byte P4 = 10;
  public static final byte P5 = 11;
  public static final byte P6 = 12;
  public static final byte P7 = 13;

  /** Terminal count, active low. Low only at code 0 while {@link #TE} is low. */
  public static final byte TC = 14;

  /** Synchronous preset, active low. Sampled on the rising edge of {@link #CP}. */
  public static final byte PE = 15;

  public static final byte VCC = 16;

  private static final int WIDTH = 8;
  private static final BitWidth BIT_WIDTH = BitWidth.create(WIDTH);
  private static final Value MAXIMUM = Value.createKnown(BIT_WIDTH, 0xFF);
  private static final byte[] OUTPUT_PINS = {TC};
  private static final byte[] PRESET_PINS = {P0, P1, P2, P3, P4, P5, P6, P7};
  private static final String[] PORT_NAMES = {
    "CP (clock)",
    "MR (asynchronous reset, active low)",
    "TE (terminal enable, active low)",
    "P0",
    "P1",
    "P2",
    "P3",
    "PL (asynchronous preset, active low)",
    "P4",
    "P5",
    "P6",
    "P7",
    "TC (terminal count, active low)",
    "PE (synchronous preset, active low)"
  };
  private static final String[] PIN_NAMES = {
    "CP", "MR", "TE", "P0", "P1", "P2", "P3", null,
    "PL", "P4", "P5", "P6", "P7", "TC", "PE", null
  };

  /** Creates a 7440103 8-bit synchronous binary down counter. */
  public Ttl7440103() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl7440103HdlGenerator());
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
    drawCount(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private static void drawCount(Graphics2D gfx, int x, int y, int height, TtlRegisterData data) {
    if (data == null) {
      return;
    }
    final var value = data.getValue();
    for (var bit = WIDTH - 1; bit >= 0; bit--) {
      final var shown = value.get(bit);
      final var originX = x + 24 + (WIDTH - 1 - bit) * 16;
      gfx.setColor(shown.getColor());
      gfx.fillOval(originX, y + height / 2 - 6, 12, 12);
      gfx.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(gfx, shown.toDisplayString(), originX + 6, y + height / 2);
    }
    gfx.setColor(Color.BLACK);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getState(state);
    final var triggered = data.updateClock(input(state, CP), StdAttr.TRIG_RISING);
    final var outcome =
        resolve(
            data.getValue(),
            triggered,
            input(state, MR),
            input(state, PL),
            input(state, PE),
            input(state, TE),
            preset(state));
    data.setValue(outcome.count);
    state.setPort(pinNrToPortNr(TC), outcome.terminal, DELAY);
  }

  private static TtlRegisterData getState(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BIT_WIDTH);
      state.setData(data);
    }
    return data;
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }

  private static Value preset(InstanceState state) {
    final var bits = new Value[WIDTH];
    for (var index = 0; index < PRESET_PINS.length; index++) {
      bits[index] = input(state, PRESET_PINS[index]);
    }
    return Value.create(bits);
  }

  /**
   * Master reset wins over asynchronous preset, and that preset wins over the clock. Synchronous
   * preset wins over counting. {@code TC} stays tied to the count that the same substitution
   * produced.
   */
  private static Outcome resolve(
      Value stored, boolean triggered, Value reset, Value asyncPreset, Value syncPreset, Value enable, Value data) {
    if (reset == Value.FALSE) {
      return at(MAXIMUM, enable);
    }
    if (reset != Value.TRUE) {
      return merge(
          at(MAXIMUM, enable),
          resolve(stored, triggered, Value.TRUE, asyncPreset, syncPreset, enable, data),
          reset == Value.ERROR);
    }
    if (asyncPreset == Value.FALSE) {
      return at(data, enable);
    }
    if (asyncPreset != Value.TRUE) {
      return merge(
          at(data, enable),
          resolve(stored, triggered, Value.TRUE, Value.TRUE, syncPreset, enable, data),
          asyncPreset == Value.ERROR);
    }
    if (!triggered) {
      return at(stored, enable);
    }
    if (syncPreset == Value.FALSE) {
      return at(data, enable);
    }
    if (syncPreset != Value.TRUE) {
      return merge(at(data, enable), countOrHold(stored, enable), syncPreset == Value.ERROR);
    }
    return countOrHold(stored, enable);
  }

  /** Low {@code TE} decrements. High {@code TE} holds. An ambiguous enable tries both. */
  private static Outcome countOrHold(Value stored, Value enable) {
    if (enable == Value.FALSE) {
      return at(decrement(stored), Value.FALSE);
    }
    if (enable == Value.TRUE) {
      return at(stored, Value.TRUE);
    }
    return merge(
        at(decrement(stored), Value.FALSE), at(stored, Value.TRUE), enable == Value.ERROR);
  }

  /** Count and the terminal output for one concrete or already-resolved word. */
  private static Outcome at(Value count, Value enable) {
    if (enable == Value.FALSE || enable == Value.TRUE) {
      return new Outcome(count, terminalOf(count, enable));
    }
    return merge(
        new Outcome(count, terminalOf(count, Value.FALSE)),
        new Outcome(count, terminalOf(count, Value.TRUE)),
        enable == Value.ERROR);
  }

  /**
   * Active-low terminal count. A definite 1 anywhere means the code is not 0, so {@code TC} is
   * high even when {@code TE} is low.
   */
  private static Value terminalOf(Value count, Value enable) {
    var ambiguous = false;
    var sawError = false;
    for (var bit = 0; bit < WIDTH; bit++) {
      final var value = count.get(bit);
      if (value == Value.TRUE) {
        return Value.TRUE;
      }
      if (value == Value.FALSE) {
        continue;
      }
      ambiguous = true;
      if (value == Value.ERROR) {
        sawError = true;
      }
    }
    if (!ambiguous) {
      return enable == Value.FALSE ? Value.FALSE : Value.TRUE;
    }
    if (enable == Value.TRUE) {
      return Value.TRUE;
    }
    return sawError ? Value.ERROR : Value.UNKNOWN;
  }

  private static Value decrement(Value stored) {
    final var bits = new Value[WIDTH];
    var borrow = Value.TRUE;
    for (var bit = 0; bit < WIDTH; bit++) {
      final var value = stored.get(bit);
      bits[bit] = bitXor(value, borrow);
      borrow = bitAnd(bitNot(value), borrow);
    }
    return Value.create(bits);
  }

  private static Outcome merge(Outcome left, Outcome right, boolean error) {
    return new Outcome(merge(left.count, right.count, error), mergeBit(left.terminal, right.terminal, error));
  }

  private static Value merge(Value left, Value right, boolean error) {
    final var bits = new Value[WIDTH];
    for (var bit = 0; bit < WIDTH; bit++) {
      bits[bit] = mergeBit(left.get(bit), right.get(bit), error);
    }
    return Value.create(bits);
  }

  private static Value mergeBit(Value left, Value right, boolean error) {
    if (left == right) {
      return left;
    }
    if (error || left == Value.ERROR || right == Value.ERROR) {
      return Value.ERROR;
    }
    return Value.UNKNOWN;
  }

  private static Value bitXor(Value left, Value right) {
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

  private static Value bitAnd(Value left, Value right) {
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

  private static Value bitNot(Value value) {
    if (value == Value.TRUE) {
      return Value.FALSE;
    }
    if (value == Value.FALSE) {
      return Value.TRUE;
    }
    if (value == Value.ERROR) {
      return Value.ERROR;
    }
    return Value.UNKNOWN;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {pinNrToPortNr(CP)};
  }

  /** One accepted count together with the terminal output of that same count. */
  private static final class Outcome {
    private final Value count;
    private final Value terminal;

    private Outcome(Value count, Value terminal) {
      this.count = count;
      this.terminal = terminal;
    }
  }
}
