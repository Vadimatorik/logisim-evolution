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
 * TTL 74HC77: quad bistable transparent latch.
 *
 * <p>Simulation follows the ST M74HC77 function table, which is pin-compatible with the Motorola
 * SN54/74LS77. {@code LE12} makes latches 1 and 2 transparent, and {@code LE34} makes latches 3
 * and 4 transparent. While an enable is high, {@code nQ} follows {@code nD}. A low enable holds
 * the pair. The pairs are independent. There are no complementary outputs. Nanosecond delays are
 * not modeled.
 *
 * <p>The pinout is the 14-pin 74HC77 package: {@code VCC} is pin 4 and {@code GND} is pin 11.
 * Pins 7 and 10 are not connected. It is not the 74HC75 pinout.
 *
 * <p>An enable that is not exactly high or low does not write. The published level is then the
 * transparent and stored results combined: a 0/1 conflict is an error, and any other disagreement
 * is unknown.
 */
public class Ttl7477 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "7477";

  public static final int DELAY = 1;

  public static final byte D1 = 1;
  public static final byte D2 = 2;
  public static final byte LE34 = 3;
  public static final byte VCC = 4;
  public static final byte D3 = 5;
  public static final byte D4 = 6;
  static final byte NC7 = 7;
  public static final byte Q4 = 8;
  public static final byte Q3 = 9;
  static final byte NC10 = 10;
  public static final byte GND = 11;
  public static final byte LE12 = 12;
  public static final byte Q2 = 13;
  public static final byte Q1 = 14;

  private static final byte[] DATA = {D1, D2, D3, D4};
  private static final byte[] OUTPUT = {Q1, Q2, Q3, Q4};
  private static final byte[] ENABLE = {LE12, LE12, LE34, LE34};
  private static final byte[] OUTPUTS = {Q4, Q3, Q2, Q1};
  private static final byte[] UNUSED = {NC7, NC10};
  private static final String[] PORT_NAMES = {
    "1D data",
    "2D data",
    "LE34 latch enable for latches 3 and 4 (active HIGH)",
    "3D data",
    "4D data",
    "4Q",
    "3Q",
    "LE12 latch enable for latches 1 and 2 (active HIGH)",
    "2Q",
    "1Q"
  };

  /** Creates a 7477 quad bistable transparent latch with the 74HC77 pinout. */
  public Ttl7477() {
    super(_ID, (byte) 14, OUTPUTS, UNUSED, PORT_NAMES, VCC, GND, new Ttl7477HdlGenerator());
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Power and unconnected pins are omitted from the port list. Pin 4 is {@code VCC}, pin 11 is
   * {@code GND}, and pins 7 and 10 have no internal connection.
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    var skipped = 0;
    if (dsPinNr > VCC) skipped++;
    if (dsPinNr > NC7) skipped++;
    if (dsPinNr > NC10) skipped++;
    if (dsPinNr > GND) skipped++;
    return (byte) (dsPinNr - 1 - skipped);
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
          "1D", "2D", "LE34", null, "3D", "4D", null, "4Q", "3Q", null, null, "LE12", "2Q", "1Q"
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
      final var level =
          definedEnable(enable) ? stored : combine(input(state, DATA[index]), stored);
      publish(state, OUTPUT[index], level);
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

  private static void publish(InstanceState state, byte pin, Value value) {
    state.setPort(pinNrToPortNr(pin), value, DELAY);
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }
}
