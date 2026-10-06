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
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

import java.awt.Graphics;

/**
 * TTL 74x260: dual 5-input NOR gate.
 *
 * <p>Each output is high only when all five of its inputs are low. A high level on any input
 * forces that output low, even if another input is unknown or an error. With no high input, an
 * error wins over an unknown value, and an unknown value wins over low. Nanosecond delays are not
 * modeled.
 *
 * <p>Pin groups follow the Philips 74F260 logic symbol, which is the same function as 74HC/HCT260.
 * Gate 1 uses pins 1, 2, 3, 12 and 13, and its output is pin 5. Gate 2 uses pins 4, 8, 9, 10 and
 * 11, and its output is pin 6. Philips names pin 4 {@code 2E} and pin 8 {@code 2A}. Texas
 * Instruments SN74F260 (SDFS012A) agrees on those pin groups but names pin 4 {@code 2A} and pin 8
 * {@code 2B}. The NOR inputs are interchangeable, so the tooltips follow the Philips names. The
 * 74HC260 datasheet itself could not be retrieved while this model was written.
 */
public class Ttl74260 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74260";

  public static final int DELAY = 1;

  // IC pin indices as specified by the Philips 74F260 logic symbol.
  public static final byte A1 = 1;
  public static final byte B1 = 2;
  public static final byte C1 = 3;
  public static final byte E2 = 4;
  public static final byte Y1 = 5;
  public static final byte Y2 = 6;
  public static final byte GND = 7;
  public static final byte A2 = 8;
  public static final byte B2 = 9;
  public static final byte C2 = 10;
  public static final byte D2 = 11;
  public static final byte D1 = 12;
  public static final byte E1 = 13;
  public static final byte VCC = 14;

  private static final byte[] OUTPUTS = {Y1, Y2};
  private static final byte[] GATE1 = {A1, B1, C1, D1, E1};
  private static final byte[] GATE2 = {A2, B2, C2, D2, E2};

  private static final String[] PORT_NAMES = {
    "1A", "1B", "1C", "2E", "1Y", "2Y", "2A", "2B", "2C", "2D", "1D", "1E"
  };

  /** Creates a 74260 dual 5-input NOR gate. */
  public Ttl74260() {
    super(_ID, (byte) 14, OUTPUTS, PORT_NAMES, new Ttl74260HdlGenerator());
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Power pins are omitted from the port list. Pins 1 to 6 therefore keep a zero-based index,
   * and pins 8 to 13 shift back by one.
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    return (byte) (dsPinNr <= Y2 ? dsPinNr - 1 : dsPinNr - 2);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    final var g = painter.getGraphics();
    final int bottom = y + height - PIN_HEIGHT;
    final int top = y + PIN_HEIGHT;
    // Gate 1 output is pin 5. Inputs 1A to 1C are bottom pins 1 to 3; 1D and 1E are top pins 12
    // and 13.
    paintNorToPin(g, x + 78, y + 42, x + 90, bottom);
    wireInputs(g, new int[] {x + 10, x + 30, x + 50}, bottom, x + 62, y + 36);
    wireInputs(g, new int[] {x + 50, x + 30}, top, x + 62, y + 44);
    // Gate 2 output is pin 6. 2E is bottom pin 4 and 2A to 2D are top pins 8 to 11.
    paintNorToPin(g, x + 104, y + 22, x + 110, bottom);
    wireInputs(g, new int[] {x + 70}, bottom, x + 88, y + 28);
    wireInputs(g, new int[] {x + 130, x + 110, x + 90, x + 70}, top, x + 88, y + 14);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    state.setPort(pinNrToPortNr(Y1), nor(state, GATE1), DELAY);
    state.setPort(pinNrToPortNr(Y2), nor(state, GATE2), DELAY);
  }

  private static Value nor(InstanceState state, byte[] inputs) {
    var sawError = false;
    var sawUnknown = false;
    for (final var pin : inputs) {
      final var value = state.getPortValue(pinNrToPortNr(pin));
      if (value == Value.TRUE) {
        return Value.FALSE;
      }
      if (value == Value.ERROR) {
        sawError = true;
      } else if (value == Value.UNKNOWN) {
        sawUnknown = true;
      } else if (value != Value.FALSE) {
        sawError = true;
      }
    }
    if (sawError) {
      return Value.ERROR;
    }
    if (sawUnknown) {
      return Value.UNKNOWN;
    }
    return Value.TRUE;
  }

  /** Draws one right-facing NOR whose output runs to {@code outputPinX} on the bottom side. */
  private static void paintNorToPin(
      Graphics g, int outX, int outY, int outputPinX, int outputPinY) {
    Drawgates.paintOr(g, outX, outY, 16, 14, true, false);
    g.drawPolyline(
        new int[] {outX + 4, outputPinX, outputPinX}, new int[] {outY, outY, outputPinY}, 3);
  }

  /** Fans the given pin columns into the input side of a gate. */
  private static void wireInputs(Graphics g, int[] pinX, int pinY, int inputEdge, int entryY) {
    for (var i = 0; i < pinX.length; i++) {
      final int entry = entryY + i * 3;
      g.drawPolyline(new int[] {pinX[i], pinX[i], inputEdge}, new int[] {pinY, entry, entry}, 3);
    }
  }

}
