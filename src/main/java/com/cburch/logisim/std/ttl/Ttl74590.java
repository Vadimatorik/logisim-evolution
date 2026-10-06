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
 * TTL 74x590: 8-bit binary counter with output register and 3-state outputs.
 *
 * <p>Simulation follows the Nexperia 74HC590 function table (Rev. 5, 17 January 2024) and the TI
 * SN74HC590A timing sequence. {@code MRC} low asynchronously clears the counter and leaves the
 * register alone. The counter advances on the rising edge of {@code CPC} while {@code MRC} is high
 * and {@code CE} is low. A rising {@code CPR} stores the counter into the register; when both
 * clocks rise together the register keeps the value from before that increment. {@code RCO} is
 * active low while the counter is 255, and {@code CE} does not gate it. {@code OE} high releases
 * {@code Q0} to {@code Q7}. Nanosecond delays are not modeled.
 *
 * <p>An unknown or error input changes an output only when the two substitutions disagree. An error
 * on such an input makes the disagreed output an error; an unknown input makes it unknown.
 */
public class Ttl74590 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74590";

  public static final int DELAY = 1;

  public static final byte Q1 = 1;
  public static final byte Q2 = 2;
  public static final byte Q3 = 3;
  public static final byte Q4 = 4;
  public static final byte Q5 = 5;
  public static final byte Q6 = 6;
  public static final byte Q7 = 7;
  public static final byte GND = 8;

  /** Ripple carry, active low while the counter holds 255. */
  public static final byte RCO = 9;

  /** Asynchronous counter clear, active low. Does not clear the output register. */
  public static final byte MRC = 10;

  /** Rising-edge counter clock. */
  public static final byte CPC = 11;

  /** Count enable, active low. Sampled on the rising edge of {@link #CPC}. */
  public static final byte CE = 12;

  /** Rising-edge register clock. Stores the counter before a simultaneous increment. */
  public static final byte CPR = 13;

  /** Output enable, active low. High releases the register outputs. */
  public static final byte OE = 14;

  public static final byte Q0 = 15;
  public static final byte VCC = 16;

  private static final int WIDTH = 8;
  private static final int COUNT_MASK = 0xFF;
  private static final BitWidth BIT_WIDTH = BitWidth.create(WIDTH);
  private static final int COUNTER_WORD = 0;
  private static final int REGISTER_WORD = 1;
  private static final int CLOCK_CPC = 0;
  private static final int CLOCK_CPR = 1;
  private static final byte[] OUTPUT_PINS = {Q1, Q2, Q3, Q4, Q5, Q6, Q7, RCO, Q0};
  private static final byte[] Q_PINS = {Q0, Q1, Q2, Q3, Q4, Q5, Q6, Q7};
  private static final String[] PORT_NAMES = {
    "Q1",
    "Q2",
    "Q3",
    "Q4",
    "Q5",
    "Q6",
    "Q7",
    "RCO (ripple carry, active low)",
    "MRC (master reset counter, active low)",
    "CPC (counter clock)",
    "CE (count enable, active low)",
    "CPR (register clock)",
    "OE (output enable, active low)",
    "Q0"
  };
  private static final String[] PIN_NAMES = {
    "Q1", "Q2", "Q3", "Q4", "Q5", "Q6", "Q7", null,
    "RCO", "MRC", "CPC", "CE", "CPR", "OE", "Q0", null
  };

  /** Creates a 74590 8-bit binary counter with an output register. */
  public Ttl74590() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl74590HdlGenerator());
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
    drawRegister(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private static void drawRegister(Graphics2D gfx, int x, int y, int height, TtlRegisterData data) {
    if (data == null) {
      return;
    }
    final var value = data.getValue(REGISTER_WORD);
    for (var bit = WIDTH - 1; bit >= 0; bit--) {
      final var shown = value.get(bit);
      final var originX = x + 20 + (WIDTH - 1 - bit) * 16;
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
    final var counterClock = data.updateClock(input(state, CPC), CLOCK_CPC, StdAttr.TRIG_RISING);
    final var registerClock = data.updateClock(input(state, CPR), CLOCK_CPR, StdAttr.TRIG_RISING);
    final var stored = data.getValue(COUNTER_WORD);
    final var counted = resolveCounter(stored, counterClock, input(state, MRC), input(state, CE));
    final var captured = resolveCounter(stored, false, input(state, MRC), Value.TRUE);
    final var register = registerClock ? captured : data.getValue(REGISTER_WORD);
    data.setValue(COUNTER_WORD, counted);
    data.setValue(REGISTER_WORD, register);
    driveRegister(state, register, input(state, OE));
    state.setPort(pinNrToPortNr(RCO), resolveCarry(counted), DELAY);
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

  private static void driveRegister(InstanceState state, Value register, Value outputEnable) {
    for (var bit = 0; bit < WIDTH; bit++) {
      state.setPort(pinNrToPortNr(Q_PINS[bit]), resolveOutput(register.get(bit), outputEnable), DELAY);
    }
  }

  /**
   * Reset wins over the clock. A rising counter clock increments only while count enable is low.
   * Uncertain controls are expanded, and bits that do not change stay defined.
   */
  private static Value resolveCounter(Value stored, boolean counterClock, Value reset, Value enable) {
    final var choice = new Choice();
    for (final var resetHigh : levels(reset)) {
      if (resetHigh == 0) {
        choice.accept(0, reset == Value.ERROR);
        continue;
      }
      expand(stored, 0, 0, false, (code, sawError) -> {
        final var controlError = sawError || reset == Value.ERROR;
        if (!counterClock) {
          choice.accept(code, controlError);
          return;
        }
        for (final var enableHigh : levels(enable)) {
          final var next = enableHigh == 0 ? (code + 1) & COUNT_MASK : code;
          choice.accept(next, controlError || enable == Value.ERROR);
        }
      });
    }
    return choice.value();
  }

  /** Carry is low only when every counter bit is high. A known low bit forces carry high. */
  private static Value resolveCarry(Value counter) {
    var sawError = false;
    var sawUnknown = false;
    for (var bit = 0; bit < WIDTH; bit++) {
      final var value = counter.get(bit);
      if (value == Value.FALSE) {
        return Value.TRUE;
      }
      sawError |= value == Value.ERROR;
      sawUnknown |= value != Value.TRUE && value != Value.ERROR;
    }
    if (sawError) {
      return Value.ERROR;
    }
    return sawUnknown ? Value.UNKNOWN : Value.FALSE;
  }

  /**
   * A high output enable releases the pin. An uncertain enable disagrees with a driven level, so
   * the pin becomes unknown, or error when the enable itself is an error.
   */
  private static Value resolveOutput(Value bit, Value outputEnable) {
    if (outputEnable == Value.FALSE) {
      return bit;
    }
    if (outputEnable == Value.TRUE || bit == Value.UNKNOWN) {
      return Value.UNKNOWN;
    }
    return outputEnable == Value.ERROR || bit == Value.ERROR ? Value.ERROR : Value.UNKNOWN;
  }

  private static void expand(Value word, int index, int code, boolean sawError, CodeConsumer out) {
    if (index == WIDTH) {
      out.accept(code, sawError);
      return;
    }
    final var bit = word.get(index);
    if (bit == Value.FALSE) {
      expand(word, index + 1, code, sawError, out);
    } else if (bit == Value.TRUE) {
      expand(word, index + 1, code | (1 << index), sawError, out);
    } else {
      final var error = sawError || bit == Value.ERROR;
      expand(word, index + 1, code, error, out);
      expand(word, index + 1, code | (1 << index), error, out);
    }
  }

  private static int[] levels(Value value) {
    if (value == Value.FALSE) {
      return new int[] {0};
    }
    if (value == Value.TRUE) {
      return new int[] {1};
    }
    return new int[] {0, 1};
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {pinNrToPortNr(CPC), pinNrToPortNr(CPR)};
  }

  private interface CodeConsumer {
    void accept(int code, boolean sawError);
  }

  /** Merges every accepted substitution. Disagreements become unknown, or error if one was seen. */
  private static final class Choice {
    private final Value[] bits = new Value[WIDTH];
    private final boolean[] conflict = new boolean[WIDTH];
    private boolean sawError;
    private boolean any;

    private void accept(int value, boolean error) {
      sawError |= error;
      if (!any) {
        any = true;
        for (var index = 0; index < WIDTH; index++) {
          bits[index] = ((value >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
        }
        return;
      }
      for (var index = 0; index < WIDTH; index++) {
        final var bit = ((value >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
        if (bits[index] != bit) {
          conflict[index] = true;
        }
      }
    }

    private Value value() {
      if (!any) {
        return Value.createUnknown(BIT_WIDTH);
      }
      for (var index = 0; index < WIDTH; index++) {
        if (conflict[index]) {
          bits[index] = sawError ? Value.ERROR : Value.UNKNOWN;
        }
      }
      return Value.create(bits);
    }
  }
}
