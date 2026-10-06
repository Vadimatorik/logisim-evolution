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

/**
 * TTL 74575: octal positive-edge D flip-flop with synchronous clear and three-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74als575a.pdf">TI SN74ALS575A</a> function table in
 * SDAS165B. {@code nCLR} is active low and synchronous: it writes zero only on the rising edge of
 * {@code CLK}, and that clear wins over {@code D}. A low {@code nOE} drives the stored bits onto
 * {@code Q}. Any other {@code nOE} level releases the outputs. Output enable does not change the
 * register, so a rising edge still loads while the outputs are released. High impedance is
 * reported as unknown. Nanosecond delays are not modeled.
 *
 * <p>The DW and NT packages have the same 24-pin map. Pins 11, 13 and 23 are not connected. An
 * unknown or error input changes a stored bit only when the clear and load substitutions disagree.
 * An error on such an input makes the disagreed bit an error; an unknown input makes it unknown.
 */
public class Ttl74575 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74575";

  public static final int PORT_INDEX_nCLR = 0;
  public static final int PORT_INDEX_nOE = 1;
  public static final int PORT_INDEX_D1 = 2;
  public static final int PORT_INDEX_D2 = 3;
  public static final int PORT_INDEX_D3 = 4;
  public static final int PORT_INDEX_D4 = 5;
  public static final int PORT_INDEX_D5 = 6;
  public static final int PORT_INDEX_D6 = 7;
  public static final int PORT_INDEX_D7 = 8;
  public static final int PORT_INDEX_D8 = 9;
  public static final int PORT_INDEX_CLK = 10;
  public static final int PORT_INDEX_Q8 = 11;
  public static final int PORT_INDEX_Q7 = 12;
  public static final int PORT_INDEX_Q6 = 13;
  public static final int PORT_INDEX_Q5 = 14;
  public static final int PORT_INDEX_Q4 = 15;
  public static final int PORT_INDEX_Q3 = 16;
  public static final int PORT_INDEX_Q2 = 17;
  public static final int PORT_INDEX_Q1 = 18;
  /** Port index of GND when the explicit power pins are shown. */
  public static final int PORT_INDEX_GND = 19;
  /** Port index of VCC when the explicit power pins are shown. */
  public static final int PORT_INDEX_VCC = 20;

  private static final int DELAY = 1;
  private static final int BITS = 8;
  /** Data port of each bit. Bit 0 is D1. */
  private static final int[] DATA = {
    PORT_INDEX_D1,
    PORT_INDEX_D2,
    PORT_INDEX_D3,
    PORT_INDEX_D4,
    PORT_INDEX_D5,
    PORT_INDEX_D6,
    PORT_INDEX_D7,
    PORT_INDEX_D8
  };
  /** Output port of each bit. Bit 0 is Q1. */
  private static final int[] OUTPUTS = {
    PORT_INDEX_Q1,
    PORT_INDEX_Q2,
    PORT_INDEX_Q3,
    PORT_INDEX_Q4,
    PORT_INDEX_Q5,
    PORT_INDEX_Q6,
    PORT_INDEX_Q7,
    PORT_INDEX_Q8
  };
  private static final byte[] OUTPUT_PINS = {15, 16, 17, 18, 19, 20, 21, 22};
  private static final byte[] UNUSED_PINS = {11, 13, 23};
  private static final String[] PORT_NAMES = {
    "nCLR (synchronous clear, active low)",
    "nOE (output enable, active low)",
    "D1",
    "D2",
    "D3",
    "D4",
    "D5",
    "D6",
    "D7",
    "D8",
    "CLK",
    "Q8",
    "Q7",
    "Q6",
    "Q5",
    "Q4",
    "Q3",
    "Q2",
    "Q1"
  };

  /** Creates a 74575 octal D flip-flop with synchronous clear and three-state outputs. */
  public Ttl74575() {
    super(_ID, (byte) 24, OUTPUT_PINS, UNUSED_PINS, PORT_NAMES, new Ttl74575HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, true, false);
    Drawgates.paintPortNamesByPin(
        painter,
        x,
        y,
        height,
        new String[] {
          "nCLR", "nOE", "1D", "2D", "3D", "4D", "5D", "6D", "7D", "8D", null, null,
          null, "CLK", "8Q", "7Q", "6Q", "5Q", "4Q", "3Q", "2Q", "1Q", null, null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.ONE, BITS);
      state.setData(data);
    }
    if (data.updateClock(state.getPortValue(PORT_INDEX_CLK), StdAttr.TRIG_RISING)) {
      final var clear = state.getPortValue(PORT_INDEX_nCLR);
      for (var bit = 0; bit < BITS; bit++) {
        data.setValue(bit, storedBit(clear, state.getPortValue(DATA[bit])));
      }
    }
    final var outputEnable = state.getPortValue(PORT_INDEX_nOE);
    for (var bit = 0; bit < BITS; bit++) {
      state.setPort(OUTPUTS[bit], drivenBit(outputEnable, data.getValue(bit)), DELAY);
    }
  }

  /**
   * A low clear forces zero. A high clear loads the data bit. Otherwise the two substitutions must
   * agree, so an unknown clear still writes zero when the data bit is already zero.
   */
  private static Value storedBit(Value clear, Value data) {
    if (clear == Value.FALSE) return Value.FALSE;
    if (clear == Value.TRUE) return data;
    if (data == Value.FALSE) return Value.FALSE;
    return clear == Value.ERROR || data == Value.ERROR ? Value.ERROR : Value.UNKNOWN;
  }

  /**
   * A low output enable drives the stored bit. A high output enable is high impedance. An unknown
   * or error enable is high impedance when that matches the stored bit, and unknown or error when
   * it does not.
   */
  private static Value drivenBit(Value outputEnable, Value stored) {
    if (outputEnable == Value.FALSE) return stored;
    if (outputEnable == Value.TRUE || stored == Value.UNKNOWN) return Value.UNKNOWN;
    return outputEnable == Value.ERROR || stored == Value.ERROR ? Value.ERROR : Value.UNKNOWN;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_CLK};
  }
}
