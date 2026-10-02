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
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Arrays;

/**
 * TTL 74x137: 3-line to 8-line decoder/demultiplexer with address latches.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC137.pdf">74HC137</a> data sheet.
 * While latch enable is low the address is transparent. A low-to-high transition keeps the address
 * that was already stored, so an address presented in the same step as that edge is not captured.
 * Further address changes are ignored while latch enable stays high. The outputs are active low
 * and mutually exclusive. All of them stay high unless enable 1 is low and enable 2 is high.
 * Enables do not change the stored address. Nanosecond delays are not modeled.
 *
 * <p>An unknown or error input changes an output only when the two substitutions disagree. An
 * error on such an input makes the disagreed output an error; an unknown input makes it unknown.
 */
public class Ttl74137 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74137";

  public static final int DELAY = 1;

  public static final byte A0 = 1;
  public static final byte A1 = 2;
  public static final byte A2 = 3;
  public static final byte LE = 4;
  public static final byte E1 = 5;
  public static final byte E2 = 6;
  public static final byte Y7 = 7;
  public static final byte GND = 8;
  public static final byte Y6 = 9;
  public static final byte Y5 = 10;
  public static final byte Y4 = 11;
  public static final byte Y3 = 12;
  public static final byte Y2 = 13;
  public static final byte Y1 = 14;
  public static final byte Y0 = 15;
  public static final byte VCC = 16;

  private static final int ADDRESS_WIDTH = 3;
  private static final int OUTPUT_COUNT = 8;
  private static final int BIT_LE = 0;
  private static final int BIT_E1 = 1;
  private static final int BIT_E2 = 2;
  private static final int BIT_A0 = 3;
  private static final int BIT_A1 = 4;
  private static final int BIT_A2 = 5;
  private static final int INPUT_COMBINATIONS = 1 << (BIT_A2 + 1);

  private static final byte[] OUTPUT_PINS = {Y0, Y1, Y2, Y3, Y4, Y5, Y6, Y7};
  private static final String[] PORT_NAMES = {
    "A0",
    "A1",
    "A2",
    "nLE Latch enable (active LOW)",
    "nE1 Output enable (active LOW)",
    "E2 Output enable (active HIGH)",
    "nY7",
    "nY6",
    "nY5",
    "nY4",
    "nY3",
    "nY2",
    "nY1",
    "nY0"
  };
  private static final String[] PIN_NAMES = {
    "A0", "A1", "A2", "nLE", "nE1", "E2", "nY7", null,
    "nY6", "nY5", "nY4", "nY3", "nY2", "nY1", "nY0", null
  };

  /** Creates a 74137 3-to-8 decoder with address latches. */
  public Ttl74137() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl74137HdlGenerator());
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
    drawAddress(gfx, x, y, (AddressLatch) painter.getData());
  }

  private static void drawAddress(Graphics2D gfx, int x, int y, AddressLatch data) {
    if (data == null) return;
    final var address = data.address;
    GraphicsUtil.drawCenteredText(gfx, "addr", x + 80, y + 20);
    drawBit(gfx, x + 64, y + 34, address.get(2));
    drawBit(gfx, x + 80, y + 34, address.get(1));
    drawBit(gfx, x + 96, y + 34, address.get(0));
  }

  private static void drawBit(Graphics2D gfx, int x, int y, Value bit) {
    gfx.setColor(bit.getColor());
    gfx.fillOval(x - 4, y - 4, 8, 8);
    gfx.setColor(Color.WHITE);
    GraphicsUtil.drawCenteredText(gfx, bit.toDisplayString(), x, y);
    gfx.setColor(Color.BLACK);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getLatch(state);
    final var latchEnable = state.getPortValue(pinNrToPortNr(LE));
    final var enable1 = state.getPortValue(pinNrToPortNr(E1));
    final var enable2 = state.getPortValue(pinNrToPortNr(E2));
    final var address0 = state.getPortValue(pinNrToPortNr(A0));
    final var address1 = state.getPortValue(pinNrToPortNr(A1));
    final var address2 = state.getPortValue(pinNrToPortNr(A2));
    final var outputs =
        decodedOutputs(data.address, latchEnable, enable1, enable2, address0, address1, address2);
    if (latchEnable == Value.FALSE) {
      data.address = Value.create(new Value[] {address0, address1, address2});
    }
    state.setPort(pinNrToPortNr(Y0), outputs[0], DELAY);
    state.setPort(pinNrToPortNr(Y1), outputs[1], DELAY);
    state.setPort(pinNrToPortNr(Y2), outputs[2], DELAY);
    state.setPort(pinNrToPortNr(Y3), outputs[3], DELAY);
    state.setPort(pinNrToPortNr(Y4), outputs[4], DELAY);
    state.setPort(pinNrToPortNr(Y5), outputs[5], DELAY);
    state.setPort(pinNrToPortNr(Y6), outputs[6], DELAY);
    state.setPort(pinNrToPortNr(Y7), outputs[7], DELAY);
  }

  private static AddressLatch getLatch(InstanceState state) {
    var data = (AddressLatch) state.getData();
    if (data == null) {
      data = AddressLatch.create();
      state.setData(data);
    }
    return data;
  }

  /**
   * Decodes every binary substitution of the unknown or error inputs. Latch enable selects the
   * address source: the pins while it is low, and the stored address while it is high.
   */
  private static Value[] decodedOutputs(
      Value stored,
      Value latchEnable,
      Value enable1,
      Value enable2,
      Value address0,
      Value address1,
      Value address2) {
    final var storedBits = new Value[] {stored.get(0), stored.get(1), stored.get(2)};
    final var pins = new Value[] {address0, address1, address2};
    Value[] merged = null;
    final var conflict = new boolean[OUTPUT_COUNT];
    var sawError = false;
    for (var mask = 0; mask < INPUT_COMBINATIONS; mask++) {
      final var latchHigh = bitHigh(mask, BIT_LE);
      final var enable1High = bitHigh(mask, BIT_E1);
      final var enable2High = bitHigh(mask, BIT_E2);
      if (!accepts(latchEnable, latchHigh)
          || !accepts(enable1, enable1High)
          || !accepts(enable2, enable2High)) {
        continue;
      }
      final var source = latchHigh ? storedBits : pins;
      final var bit0 = bitHigh(mask, BIT_A0);
      final var bit1 = bitHigh(mask, BIT_A1);
      final var bit2 = bitHigh(mask, BIT_A2);
      if (!accepts(source[0], bit0) || !accepts(source[1], bit1) || !accepts(source[2], bit2)) {
        continue;
      }
      sawError |=
          isError(latchEnable)
              || isError(enable1)
              || isError(enable2)
              || isError(source[0])
              || isError(source[1])
              || isError(source[2]);
      final var code = (bit0 ? 1 : 0) | (bit1 ? (1 << 1) : 0) | (bit2 ? (1 << 2) : 0);
      final var enabled = !enable1High && enable2High;
      final var next = decode(code, enabled);
      if (merged == null) {
        merged = next;
      } else {
        markConflicts(conflict, merged, next);
      }
    }
    return resolve(merged, conflict, sawError);
  }

  private static boolean bitHigh(int mask, int bit) {
    return (mask & (1 << bit)) != 0;
  }

  private static boolean accepts(Value bit, boolean high) {
    if (bit == Value.TRUE) return high;
    if (bit == Value.FALSE) return !high;
    return true;
  }

  private static boolean isError(Value bit) {
    return bit == Value.ERROR;
  }

  private static Value[] decode(int code, boolean enabled) {
    final var outputs = new Value[OUTPUT_COUNT];
    Arrays.fill(outputs, Value.TRUE);
    if (enabled) outputs[code] = Value.FALSE;
    return outputs;
  }

  private static void markConflicts(boolean[] conflict, Value[] current, Value[] next) {
    for (var index = 0; index < OUTPUT_COUNT; index++) {
      if (current[index] != next[index]) conflict[index] = true;
    }
  }

  private static Value[] resolve(Value[] merged, boolean[] conflict, boolean sawError) {
    final var result = merged == null ? new Value[OUTPUT_COUNT] : merged;
    if (merged == null) Arrays.fill(result, Value.UNKNOWN);
    for (var index = 0; index < OUTPUT_COUNT; index++) {
      if (conflict[index]) result[index] = sawError ? Value.ERROR : Value.UNKNOWN;
    }
    return result;
  }

  /** Three stored address bits. Bit 0 is A0. */
  private static final class AddressLatch implements InstanceData, Cloneable {
    private Value address;

    private AddressLatch(Value address) {
      this.address = address;
    }

    static AddressLatch create() {
      final var width = BitWidth.create(ADDRESS_WIDTH);
      final var startup =
          AppPreferences.Memory_Startup_Unknown.get()
              ? Value.createUnknown(width)
              : Value.createKnown(width, 0);
      return new AddressLatch(startup);
    }

    @Override
    public AddressLatch clone() {
      try {
        return (AddressLatch) super.clone();
      } catch (CloneNotSupportedException ex) {
        return new AddressLatch(address);
      }
    }
  }
}
