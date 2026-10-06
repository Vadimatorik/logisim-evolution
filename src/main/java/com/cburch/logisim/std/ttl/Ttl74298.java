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
 * TTL 74x298: quad 2-input multiplexer with storage.
 *
 * <p>Simulation follows the Hitachi HD74HC298 and the pin-compatible ON SN74LS298. Word select low
 * presents word 1 (A1, B1, C1, D1) to the flip-flops, and word select high presents word 2 (A2,
 * B2, C2, D2). The selected word is stored on the high-to-low clock transition. There is no
 * reset. Nanosecond delays are not modeled.
 *
 * <p>An unknown word select leaves a bit unchanged in value when both of its sources already
 * agree on a defined level. An error on the word select, or on the source that can still be
 * chosen, makes that bit an error.
 */
public class Ttl74298 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must be a unique string among all tools.
   */
  public static final String _ID = "74298";

  public static final int DELAY = 8;

  public static final byte B2 = 1;
  public static final byte A2 = 2;
  public static final byte A1 = 3;
  public static final byte B1 = 4;
  public static final byte C2 = 5;
  public static final byte D2 = 6;
  public static final byte D1 = 7;
  public static final byte GND = 8;
  public static final byte C1 = 9;
  public static final byte WS = 10;
  public static final byte CLK = 11;
  public static final byte QD = 12;
  public static final byte QC = 13;
  public static final byte QB = 14;
  public static final byte QA = 15;
  public static final byte VCC = 16;

  private static final int WIDTH = 4;
  private static final byte[] OUTPUT_PINS = {QD, QC, QB, QA};
  private static final byte[] WORD1 = {A1, B1, C1, D1};
  private static final byte[] WORD2 = {A2, B2, C2, D2};
  private static final byte[] OUTPUTS = {QA, QB, QC, QD};
  private static final String[] PORT_NAMES = {
    "B2 Word 2 data",
    "A2 Word 2 data",
    "A1 Word 1 data",
    "B1 Word 1 data",
    "C2 Word 2 data",
    "D2 Word 2 data",
    "D1 Word 1 data",
    "C1 Word 1 data",
    "WS Word select",
    "CLK Clock (falling edge)",
    "QD",
    "QC",
    "QB",
    "QA"
  };
  private static final String[] PIN_NAMES = {
    "B2", "A2", "A1", "B1", "C2", "D2", "D1", null,
    "C1", "WS", "CLK", "QD", "QC", "QB", "QA", null
  };

  /** Creates a 74298 quad 2-input multiplexer with storage. */
  public Ttl74298() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl74298HdlGenerator());
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
    super.paintBase(painter, true, false);
    Drawgates.paintPortNamesByPin(painter, x, y, height, PIN_NAMES);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getState(state);
    if (data.updateClock(input(state, CLK), StdAttr.TRIG_FALLING)) {
      final var stored = new Value[WIDTH];
      final var select = input(state, WS);
      for (var bit = 0; bit < WIDTH; bit++) {
        stored[bit] = selected(select, input(state, WORD1[bit]), input(state, WORD2[bit]));
      }
      data.setValue(Value.create(stored));
    }
    final var word = data.getValue();
    for (var bit = 0; bit < WIDTH; bit++) {
      state.setPort(pinNrToPortNr(OUTPUTS[bit]), word.get(bit), DELAY);
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
   * Chooses one source bit. A defined select takes that source. An unknown select keeps a defined
   * level only when both sources already carry it. An error on the select, or on a source that
   * the unknown select might still choose, makes the result an error.
   */
  private static Value selected(Value select, Value word1, Value word2) {
    if (select == Value.FALSE) {
      return level(word1);
    }
    if (select == Value.TRUE) {
      return level(word2);
    }
    if (select != Value.ERROR && word1 == word2 && defined(word1)) {
      return word1;
    }
    if (select == Value.ERROR || word1 == Value.ERROR || word2 == Value.ERROR) {
      return Value.ERROR;
    }
    return Value.UNKNOWN;
  }

  private static Value level(Value value) {
    return defined(value) || value == Value.ERROR ? value : Value.UNKNOWN;
  }

  private static boolean defined(Value value) {
    return value == Value.TRUE || value == Value.FALSE;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {pinNrToPortNr(CLK)};
  }
}
