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
 * TTL 74x375: quad bistable transparent latch.
 *
 * <p>Simulation follows the ST M74HC375 data sheet. The device is pin and function compatible
 * with the 74LS375. Latches 1 and 2 are transparent while {@code G1•2} is high, and latches 3 and
 * 4 while {@code G3•4} is high. A low enable holds the value that was present at {@code D} when
 * the enable fell. {@code nQ} is the complement of {@code Q}. This is not the 74x75 pinout: VCC
 * is pin 16 and GND is pin 8.
 *
 * <p>An enable that is neither high nor low keeps a stored bit only when the data input is the
 * same fully defined value. Any other combination makes that bit an error, because the output
 * could either hold or follow. A high enable copies the data input unchanged, including unknown
 * and error values. The complement of an unknown or error bit stays unknown or error.
 */
public class Ttl74375 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74375";

  public static final int DELAY = 1;

  public static final byte D1 = 1;
  public static final byte NQ1 = 2;
  public static final byte Q1 = 3;
  public static final byte G12 = 4;
  public static final byte Q2 = 5;
  public static final byte NQ2 = 6;
  public static final byte D2 = 7;
  public static final byte GND = 8;
  public static final byte D3 = 9;
  public static final byte NQ3 = 10;
  public static final byte Q3 = 11;
  public static final byte G34 = 12;
  public static final byte Q4 = 13;
  public static final byte NQ4 = 14;
  public static final byte D4 = 15;
  public static final byte VCC = 16;

  private static final byte[] OUTPUTS = {NQ1, Q1, Q2, NQ2, NQ3, Q3, Q4, NQ4};

  private static final String[] PORT_NAMES = {
    "1D Data input 1",
    "n1Q Complementary output 1",
    "1Q Latch output 1",
    "G12 Latch enable 1 and 2 (active high)",
    "2Q Latch output 2",
    "n2Q Complementary output 2",
    "2D Data input 2",
    "3D Data input 3",
    "n3Q Complementary output 3",
    "3Q Latch output 3",
    "G34 Latch enable 3 and 4 (active high)",
    "4Q Latch output 4",
    "n4Q Complementary output 4",
    "4D Data input 4"
  };

  private static final Latch[] LATCHES = {
    new Latch(0, D1, Q1, NQ1, G12),
    new Latch(1, D2, Q2, NQ2, G12),
    new Latch(2, D3, Q3, NQ3, G34),
    new Latch(3, D4, Q4, NQ4, G34)
  };

  /** Creates a 74375 quad bistable transparent latch. */
  public Ttl74375() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74375HdlGenerator());
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
    Drawgates.paintPortNamesByPin(
        painter,
        x,
        y,
        height,
        new String[] {
          "1D", "n1Q", "1Q", "G12", "2Q", "n2Q", "2D", null,
          "3D", "n3Q", "3Q", "G34", "4Q", "n4Q", "4D", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = stateData(state);
    for (final var latch : LATCHES) {
      final var stored = data.getValue(latch.bit);
      final var next =
          nextValue(port(state, latch.enablePin), port(state, latch.dataPin), stored);
      data.setValue(latch.bit, next);
      state.setPort(pinNrToPortNr(latch.outputPin), next, DELAY);
      state.setPort(pinNrToPortNr(latch.complementPin), complement(next), DELAY);
    }
  }

  /**
   * Next stored bit. A high enable is transparent. A low enable holds. Any other enable level
   * holds only when the data input repeats the stored defined bit.
   */
  static Value nextValue(Value enable, Value data, Value stored) {
    if (enable == Value.TRUE) return data;
    if (enable == Value.FALSE) return stored;
    if ((data == Value.TRUE || data == Value.FALSE) && data == stored) return stored;
    return Value.ERROR;
  }

  /** Complement that leaves unknown and error bits unchanged. */
  static Value complement(Value value) {
    if (value == Value.TRUE) return Value.FALSE;
    if (value == Value.FALSE) return Value.TRUE;
    return value;
  }

  private static Value port(InstanceState state, byte dsPinNr) {
    return state.getPortValue(pinNrToPortNr(dsPinNr));
  }

  private static TtlRegisterData stateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.ONE, LATCHES.length);
      state.setData(data);
    }
    return data;
  }

  private record Latch(int bit, byte dataPin, byte outputPin, byte complementPin, byte enablePin) {}
}
