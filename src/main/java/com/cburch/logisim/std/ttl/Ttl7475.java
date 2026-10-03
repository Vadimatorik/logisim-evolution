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

/**
 * TTL 74HC75: quad bistable transparent latch.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC75.pdf">Nexperia 74HC75</a>
 * function table. {@code LE12} makes latches 1 and 2 transparent, and {@code LE34} makes latches 3
 * and 4 transparent. While an enable is high, {@code nQ} follows {@code nD} and the complementary
 * output is its inverse. A low enable holds the pair. The pairs are independent. Nanosecond delays
 * are not modeled.
 *
 * <p>The pinout is the 74HC75 package: {@code VCC} is pin 5 and {@code GND} is pin 12. It is not
 * the 7475 or 74LS75 pinout, where power sits on pins 16 and 8.
 *
 * <p>An enable that is not exactly high or low does not write. The published level is then the
 * transparent and stored results combined: a 0/1 conflict is an error, and any other disagreement
 * is unknown.
 */
public class Ttl7475 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "7475";

  public static final int DELAY = 1;

  public static final byte Q1N = 1;
  public static final byte D1 = 2;
  public static final byte D2 = 3;
  public static final byte LE34 = 4;
  public static final byte VCC = 5;
  public static final byte D3 = 6;
  public static final byte D4 = 7;
  public static final byte Q4N = 8;
  public static final byte Q4 = 9;
  public static final byte Q3 = 10;
  public static final byte Q3N = 11;
  public static final byte GND = 12;
  public static final byte LE12 = 13;
  public static final byte Q2N = 14;
  public static final byte Q2 = 15;
  public static final byte Q1 = 16;

  private static final byte[] DATA = {D1, D2, D3, D4};
  private static final byte[] OUTPUT = {Q1, Q2, Q3, Q4};
  private static final byte[] COMPLEMENT = {Q1N, Q2N, Q3N, Q4N};
  private static final byte[] ENABLE = {LE12, LE12, LE34, LE34};
  private static final byte[] OUTPUTS = {Q1N, Q4N, Q4, Q3, Q3N, Q2N, Q2, Q1};
  private static final String[] PORT_NAMES = {
    "1Q complementary",
    "1D data",
    "2D data",
    "LE34 latch enable for latches 3 and 4 (active HIGH)",
    "3D data",
    "4D data",
    "4Q complementary",
    "4Q",
    "3Q",
    "3Q complementary",
    "LE12 latch enable for latches 1 and 2 (active HIGH)",
    "2Q complementary",
    "2Q",
    "1Q"
  };

  /** Creates a 7475 quad bistable transparent latch with the 74HC75 pinout. */
  public Ttl7475() {
    super(_ID, (byte) 16, OUTPUTS, null, PORT_NAMES, VCC, GND, new Ttl7475HdlGenerator());
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Power pins are omitted from the port list. Pin 5 is {@code VCC} and pin 12 is {@code GND}.
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    if (dsPinNr < VCC) return (byte) (dsPinNr - 1);
    if (dsPinNr < GND) return (byte) (dsPinNr - 2);
    return (byte) (dsPinNr - 3);
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
          "1Qn", "1D", "2D", "LE34", null, "3D", "4D", "4Qn",
          "4Q", "3Q", "3Qn", null, "LE12", "2Qn", "2Q", "1Q"
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = stateData(state);
    for (var index = 0; index < DATA.length; index++) {
      if (input(state, ENABLE[index]) == Value.TRUE) {
        data.setValue(index, input(state, DATA[index]));
      }
    }
    for (var index = 0; index < DATA.length; index++) {
      final var stored = data.getValue(index);
      final var enable = input(state, ENABLE[index]);
      final var level = definedEnable(enable) ? stored : combine(input(state, DATA[index]), stored);
      publish(state, OUTPUT[index], level);
      publish(state, COMPLEMENT[index], complement(level));
    }
  }

  private static TtlRegisterData stateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.ONE, DATA.length);
      state.setData(data);
    }
    return data;
  }

  private static boolean definedEnable(Value enable) {
    return enable == Value.TRUE || enable == Value.FALSE;
  }

  private static Value combine(Value transparent, Value stored) {
    if (transparent == stored) return transparent;
    if (transparent == Value.ERROR || stored == Value.ERROR) return Value.ERROR;
    if (transparent.isFullyDefined() && stored.isFullyDefined()) return Value.ERROR;
    return Value.UNKNOWN;
  }

  /** Inverts a stored bit. An unknown latch stays unknown on both outputs. */
  private static Value complement(Value value) {
    if (value == Value.TRUE) return Value.FALSE;
    if (value == Value.FALSE) return Value.TRUE;
    if (value == Value.ERROR) return Value.ERROR;
    return Value.UNKNOWN;
  }

  private static void publish(InstanceState state, byte pin, Value value) {
    state.setPort(pinNrToPortNr(pin), value, DELAY);
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }
}
