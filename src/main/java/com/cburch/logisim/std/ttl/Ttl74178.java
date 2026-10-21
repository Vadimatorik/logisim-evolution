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
 * TTL 74x178: 4-bit parallel-access shift register.
 *
 * <p>Simulation follows the Texas Instruments SN54178/SN74178 data sheet (TTL Data Book Vol. 2,
 * 1985, pages 3-701 to 3-702). There is no separate 74HC178 data sheet. The register changes on
 * the high-to-low clock transition. Shift high moves the word from QA toward QD and copies the
 * serial input into QA, whether or not load is high. Shift low and load high copies A, B, C and D
 * into QA, QB, QC and QD. Both controls low hold the word, so a free-running clock does not change
 * it. The 74179 clear input and complementary QD output are not part of this device. Nanosecond
 * delays are not modeled.
 *
 * <p>An unknown or error input changes an output only when the two substitutions disagree. An error
 * on such an input makes the disagreed output an error; an unknown input makes it unknown.
 */
public class Ttl74178 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must be a unique string among all tools.
   */
  public static final String _ID = "74178";

  public static final int DELAY = 1;

  public static final byte B = 1;
  public static final byte A = 2;
  public static final byte SER = 3;
  public static final byte QA = 4;
  public static final byte CLK = 5;
  public static final byte QB = 6;
  public static final byte GND = 7;
  public static final byte QC = 8;
  public static final byte LOAD = 9;
  public static final byte QD = 10;
  public static final byte SHIFT = 11;
  public static final byte D = 12;
  public static final byte C = 13;
  public static final byte VCC = 14;

  private static final int WIDTH = 4;
  private static final byte[] OUTPUT_PINS = {QA, QB, QC, QD};
  private static final String[] PORT_NAMES = {
    "B",
    "A",
    "SER (Serial input)",
    "QA",
    "CLK (Clock)",
    "QB",
    "QC",
    "LOAD",
    "QD",
    "SHIFT",
    "D",
    "C"
  };
  private static final String[] PIN_NAMES = {
    "B", "A", "SER", "QA", "CLK", "QB", null, "QC", "LOAD", "QD", "SHIFT", "D", "C", null
  };

  private static final int BIT_SHIFT = 0;
  private static final int BIT_LOAD = 1;
  private static final int BIT_SER = 2;
  private static final int BIT_DATA = 3;
  private static final int BIT_STORED = 7;
  private static final int SOURCE_BITS = 11;

  /** Creates a 74178 4-bit parallel-access shift register. */
  public Ttl74178() {
    super(_ID, (byte) 14, OUTPUT_PINS, PORT_NAMES, new Ttl74178HdlGenerator());
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
    g.drawRect((x + 14), (y + height - 28), 12, 12);
    g.drawLine((x + 70), (y + height - 7), (x + 70), (y + height - 13));
    g.drawLine((x + 70), (y + height - 13), (x + 20), (y + height - 13));
    g.drawLine((x + 20), (y + height - 13), (x + 20), ((y + height - 28)) + 12);
    g.drawLine((x + 14), (((y + height - 28)) + 6) - 3, ((x + 14)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 14), (((y + height - 28)) + 6) + 3, ((x + 14)) + 4, ((y + height - 28)) + 6);
    structureDot(g, (x + 20), ((y + height - 28)) + 6, stored, 0, 0);
    g.drawRect((x + 42), (y + height - 28), 12, 12);
    g.drawLine((x + 110), (y + height - 7), (x + 110), (y + height - 13));
    g.drawLine((x + 110), (y + height - 13), (x + 48), (y + height - 13));
    g.drawLine((x + 48), (y + height - 13), (x + 48), ((y + height - 28)) + 12);
    g.drawLine((x + 42), (((y + height - 28)) + 6) - 3, ((x + 42)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 42), (((y + height - 28)) + 6) + 3, ((x + 42)) + 4, ((y + height - 28)) + 6);
    g.drawRect((x + 70), (y + height - 28), 12, 12);
    g.drawLine((x + 130), (y + 7), (x + 130), (y + 13));
    g.drawLine((x + 130), (y + 13), (x + 76), (y + 13));
    g.drawLine((x + 76), (y + 13), (x + 76), (y + height - 28));
    g.drawLine((x + 70), (((y + height - 28)) + 6) - 3, ((x + 70)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 70), (((y + height - 28)) + 6) + 3, ((x + 70)) + 4, ((y + height - 28)) + 6);
    g.drawRect((x + 98), (y + height - 28), 12, 12);
    g.drawLine((x + 90), (y + 7), (x + 90), (y + 13));
    g.drawLine((x + 90), (y + 13), (x + 104), (y + 13));
    g.drawLine((x + 104), (y + 13), (x + 104), (y + height - 28));
    g.drawLine((x + 98), (((y + height - 28)) + 6) - 3, ((x + 98)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 98), (((y + height - 28)) + 6) + 3, ((x + 98)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 26), ((y + height - 28)) + 6, (x + 38), ((y + height - 28)) + 6);
    g.drawLine((x + 54), ((y + height - 28)) + 6, (x + 66), ((y + height - 28)) + 6);
    g.drawLine((x + 82), ((y + height - 28)) + 6, (x + 94), ((y + height - 28)) + 6);
    g.drawLine((x + 90), (y + height - 7), (x + 90), (y + height - 13));
    g.drawLine((x + 90), (y + height - 13), (x + 9), (y + height - 13));
    g.drawLine((x + 9), (y + height - 13), (x + 9), ((y + height - 28)) + 6);
    g.drawLine((x + 50), (y + height - 7), (x + 50), (y + height - 13));
    g.drawLine((x + 50), (y + height - 13), (x + 62), (y + height - 13));
    g.drawLine((x + 62), (y + height - 13), (x + 62), (y + 20));
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getState(state);
    final var triggered = data.updateClock(input(state, CLK), StdAttr.TRIG_FALLING);
    final var word =
        resolve(
            data.getValue(),
            triggered,
            new Value[] {
              input(state, SHIFT),
              input(state, LOAD),
              input(state, SER),
              input(state, A),
              input(state, B),
              input(state, C),
              input(state, D)
            });
    data.setValue(word);
    state.setPort(pinNrToPortNr(QA), word.get(0), DELAY);
    state.setPort(pinNrToPortNr(QB), word.get(1), DELAY);
    state.setPort(pinNrToPortNr(QC), word.get(2), DELAY);
    state.setPort(pinNrToPortNr(QD), word.get(3), DELAY);
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
   * Without a falling edge the register holds. Shift high moves QA toward QD and loads the serial
   * input into QA. Shift low and load high copies the parallel inputs. Both controls low hold.
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

  /** Next QD..QA word. Bit 0 of the result is QA. */
  static int nextWord(int mask, boolean triggered) {
    final var stored = (mask >> BIT_STORED) & 0xF;
    if (!triggered) {
      return stored;
    }
    if (bitHigh(mask, BIT_SHIFT)) {
      final var serial = bitHigh(mask, BIT_SER) ? 1 : 0;
      return serial | ((stored & 0x7) << 1);
    }
    if (bitHigh(mask, BIT_LOAD)) {
      return (mask >> BIT_DATA) & 0xF;
    }
    return stored;
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
    return new int[] {pinNrToPortNr(CLK)};
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
