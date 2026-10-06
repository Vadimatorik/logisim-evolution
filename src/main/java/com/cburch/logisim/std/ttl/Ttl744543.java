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

/**
 * TTL 74x4543: BCD to 7-segment latch/decoder/driver for liquid-crystal displays.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/gpn/CD74HC4543">CD74HC4543</a> function table (TI SCHS217).
 * {@code LD} high makes the latch transparent, and {@code LD} low holds the BCD code that was
 * applied while {@code LD} was high. {@code BI} and {@code PH} are not stored: blanking forces
 * every segment to follow {@code PH}, and a high {@code PH} inverts the decoded pattern. Codes 10
 * to 15 are blank. Nexperia's HEF4543B names pin 1 {@code LE} and describes it as active-low, but
 * its function table uses this same polarity.
 */
public class Ttl744543 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744543";

  public static final int DELAY = 1;

  public static final byte LD = 1;
  public static final byte D2 = 2;
  public static final byte D1 = 3;
  public static final byte D3 = 4;
  public static final byte D0 = 5;
  public static final byte PH = 6;
  public static final byte BI = 7;

  public static final byte QA = 9;
  public static final byte QB = 10;
  public static final byte QC = 11;
  public static final byte QD = 12;
  public static final byte QE = 13;
  public static final byte QG = 14;
  public static final byte QF = 15;

  public static final byte GND = 8;
  public static final byte VCC = 16;

  /** Segment bits in datasheet order a, b, c, d, e, f, g. Bit 6 is segment a. */
  static final int[] GLYPHS = {
    0b1111110,
    0b0110000,
    0b1101101,
    0b1111001,
    0b0110011,
    0b1011011,
    0b1011111,
    0b1110000,
    0b1111111,
    0b1111011
  };

  private static final BitWidth WIDTH = BitWidth.create(4);
  private static final byte[] OUTPUTS = {QA, QB, QC, QD, QE, QG, QF};
  /** Segment outputs in order a, b, c, d, e, f, g. Pin 14 is g and pin 15 is f. */
  private static final byte[] SEGMENT_PINS = {QA, QB, QC, QD, QE, QF, QG};

  private static final String[] PORT_NAMES = {
    "LD Latch disable (high = follow)",
    "D2 BCD weight 4",
    "D1 BCD weight 2",
    "D3 BCD weight 8",
    "D0 BCD weight 1",
    "PH Phase",
    "BI Blanking (high = blank)",
    "a Segment",
    "b Segment",
    "c Segment",
    "d Segment",
    "e Segment",
    "g Segment",
    "f Segment"
  };

  /** Creates a 744543 BCD to 7-segment latch/decoder/driver. */
  public Ttl744543() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl744543HdlGenerator());
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

  /** Datasheet glyph as seven characters, {@code a} first and {@code g} last. */
  static String glyphBits(int code) {
    final var glyph = code >= 0 && code < GLYPHS.length ? GLYPHS[code] : 0;
    final var bits = new StringBuilder(7);
    for (var segment = 6; segment >= 0; segment--) {
      bits.append(((glyph >> segment) & 1) == 1 ? '1' : '0');
    }
    return bits.toString();
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
          "LD", "D2", "D1", "D3", "D0", "PH", "BI", null,
          "a", "b", "c", "d", "e", "g", "f", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var stored = register(state);
    final var code =
        nextCode(
            stored.getValue(),
            input(state, LD),
            Value.create(
                new Value[] {
                  input(state, D0), input(state, D1), input(state, D2), input(state, D3)
                }));
    stored.setValue(code);
    final var phase = input(state, PH);
    final var blank = input(state, BI);
    for (var segment = 0; segment < SEGMENT_PINS.length; segment++) {
      state.setPort(
          pinNrToPortNr(SEGMENT_PINS[segment]), segmentValue(code, phase, blank, segment), DELAY);
    }
  }

  /**
   * {@code LD} is a level-sensitive latch enable, not an edge clock.
   */
  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return false;
  }

  private static TtlRegisterData register(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WIDTH);
      state.setData(data);
    }
    return data;
  }

  private static Value input(InstanceState state, byte dsPinNr) {
    return state.getPortValue(pinNrToPortNr(dsPinNr));
  }

  /**
   * Captures {@code incoming} while {@code load} is high and holds {@code stored} while it is low.
   * An undefined load leaves the code unchanged when both words match. Otherwise an error on the
   * load or either word makes the code error, and a merely unknown load makes it unknown.
   */
  private static Value nextCode(Value stored, Value load, Value incoming) {
    if (load == Value.TRUE) return incoming;
    if (load == Value.FALSE) return stored;
    if (incoming.equals(stored)) return stored;
    if (load == Value.ERROR || hasError(incoming) || hasError(stored)) {
      return Value.createError(WIDTH);
    }
    return Value.createUnknown(WIDTH);
  }

  private static boolean hasError(Value word) {
    for (var index = 0; index < word.getWidth(); index++) {
      if (word.get(index) == Value.ERROR) return true;
    }
    return false;
  }

  /**
   * Decodes one segment. Blanking outputs follow {@code phase}. A high phase inverts the glyph.
   * Unknown inputs leave a segment unknown only when some legal substitution would change it. An
   * error input does the same with {@link Value#ERROR}.
   */
  private static Value segmentValue(Value code, Value phase, Value blank, int segment) {
    final var bits =
        new Value[] {code.get(0), code.get(1), code.get(2), code.get(3), phase, blank};
    return resolve(bits, segment, 0);
  }

  private static Value resolve(Value[] bits, int segment, int index) {
    if (index == bits.length) return concreteSegment(bits, segment);
    final var current = bits[index];
    if (current == Value.TRUE || current == Value.FALSE) {
      return resolve(bits, segment, index + 1);
    }
    bits[index] = Value.FALSE;
    final var low = resolve(bits, segment, index + 1);
    bits[index] = Value.TRUE;
    final var high = resolve(bits, segment, index + 1);
    bits[index] = current;
    if (low.equals(high)) return low;
    if (current == Value.ERROR || low == Value.ERROR || high == Value.ERROR) return Value.ERROR;
    return Value.UNKNOWN;
  }

  private static Value concreteSegment(Value[] bits, int segment) {
    final var code =
        (bit(bits[3]) << 3) | (bit(bits[2]) << 2) | (bit(bits[1]) << 1) | bit(bits[0]);
    final var on = (glyphBits(code).charAt(segment) == '1') != (bits[4] == Value.TRUE);
    final var driven = bits[5] == Value.TRUE ? bits[4] == Value.TRUE : on;
    return driven ? Value.TRUE : Value.FALSE;
  }

  private static int bit(Value value) {
    return value == Value.TRUE ? 1 : 0;
  }
}
