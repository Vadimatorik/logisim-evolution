/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.std.plexers.PlexersLibrary;
import java.util.Map;

/**
 * TTL 74x249: BCD-to-seven-segment decoder/driver with active-high outputs.
 *
 * <p>The digital function follows the Texas Instruments SN54249/SN74249 and SN74LS249 data sheet
 * (types SN54246 through SN54249, March 1974, revised December 1983). There is no separate 74HC249
 * data sheet; this model is the same function table. Numerals 6 and 9 keep their tails, unlike the
 * 7447. Segment outputs are active high. The real device's open-collector pins are represented as
 * ordinary logic levels.
 *
 * <p>Blanking has the data-sheet priority. A low {@code BI} turns every segment off. With {@code
 * BI/RBO} high, a low {@code LT} turns every segment on. A low {@code RBI} blanks only a BCD zero
 * and pulls {@code RBO} low. {@code BI/RBO} is wire-AND logic with a pull-up: the model drives it
 * low only for that ripple-blank response and otherwise releases it. An unknown level is not an
 * active low.
 */
public class Ttl74249 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74249";

  public static final byte B = 1;
  public static final byte C = 2;
  public static final byte LT = 3;
  public static final byte BI_RBO = 4;
  public static final byte RBI = 5;
  public static final byte D = 6;
  public static final byte A = 7;
  public static final byte GND = 8;
  public static final byte SEG_E = 9;
  public static final byte SEG_D = 10;
  public static final byte SEG_C = 11;
  public static final byte SEG_B = 12;
  public static final byte SEG_A = 13;
  public static final byte SEG_G = 14;
  public static final byte SEG_F = 15;
  public static final byte VCC = 16;

  /** Segment pins in mask order, bit 0 = {@code a} through bit 6 = {@code g}. */
  private static final byte[] SEGMENT_PINS = {SEG_A, SEG_B, SEG_C, SEG_D, SEG_E, SEG_F, SEG_G};

  private static final byte[] BCD_PINS = {A, B, C, D};

  /**
   * Active-high glyphs for BCD 0 through 15. Bit 0 is segment {@code a}. Codes 6 and 9 include the
   * tails ({@code 0x7D} and {@code 0x6F}); the tailless 7448 patterns would be {@code 0x7C} and
   * {@code 0x67}.
   */
  private static final int[] GLYPHS = {
    0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07,
    0x7F, 0x6F, 0x58, 0x4C, 0x62, 0x69, 0x78, 0x00
  };

  private static final int ALL_SEGMENTS = 0x7F;

  private static final byte[] OUTPUTS = {SEG_E, SEG_D, SEG_C, SEG_B, SEG_A, SEG_G, SEG_F};

  private static final String[] PORT_NAMES = {
    "B BCD input",
    "C BCD input",
    "LT Lamp test input",
    "BI/RBO Blanking input or ripple-blanking output",
    "RBI Ripple-blanking input",
    "D BCD input",
    "A BCD input",
    "e Segment output",
    "d Segment output",
    "c Segment output",
    "b Segment output",
    "a Segment output",
    "g Segment output",
    "f Segment output"
  };

  /** Creates a 74249 BCD-to-seven-segment decoder. */
  public Ttl74249() {
    super(_ID, (byte) 16, OUTPUTS, new byte[] {}, new byte[] {BI_RBO}, PORT_NAMES,
        new Ttl74249HdlGenerator());
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Power pins are omitted from the port list, so this mapping covers the signal pins only.
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
          "B", "C", "LT", "BI/RBO", "RBI", "D", "A", null,
          "e", "d", "c", "b", "a", "g", "f", null
        });
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    super.configureNewInstance(instance);
    instance.getComponent().setPullPorts(Map.of((int) pinNrToPortNr(BI_RBO), Value.TRUE));
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var bcd = bcdValue(state);
    final var blanking = isLow(state, BI_RBO);
    final var lampTest = isLow(state, LT);
    final var blankZero = isLow(state, RBI) && bcd == 0;
    final int mask;
    if (blanking) {
      mask = 0;
    } else if (lampTest) {
      mask = ALL_SEGMENTS;
    } else if (bcd < 0) {
      mask = -1;
    } else if (blankZero) {
      mask = 0;
    } else {
      mask = GLYPHS[bcd];
    }
    for (var bit = 0; bit < SEGMENT_PINS.length; bit++) {
      final var segment = mask < 0 ? Value.UNKNOWN : bitValue(mask, bit);
      state.setPort(pinNrToPortNr(SEGMENT_PINS[bit]), segment, PlexersLibrary.DELAY);
    }
    // Lamp test leaves RBO released. An externally forced BI is not fought with a high drive.
    final var rippleBlanking = blankZero && !lampTest ? Value.FALSE : Value.UNKNOWN;
    state.setPort(pinNrToPortNr(BI_RBO), rippleBlanking, PlexersLibrary.DELAY);
  }

  /** Returns the BCD code, or -1 when any BCD input is not a firm 0 or 1. */
  private static int bcdValue(InstanceState state) {
    var code = 0;
    for (var bit = 0; bit < BCD_PINS.length; bit++) {
      final var value = state.getPortValue(pinNrToPortNr(BCD_PINS[bit]));
      if (value == Value.TRUE) {
        code |= 1 << bit;
      } else if (value != Value.FALSE) {
        return -1;
      }
    }
    return code;
  }

  private static boolean isLow(InstanceState state, byte dsPinNr) {
    return state.getPortValue(pinNrToPortNr(dsPinNr)) == Value.FALSE;
  }

  private static Value bitValue(int mask, int bit) {
    return (mask & (1 << bit)) != 0 ? Value.TRUE : Value.FALSE;
  }
}
