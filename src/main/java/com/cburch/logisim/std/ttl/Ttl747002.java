/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x7002: quad 2-input NOR gate with Schmitt-trigger inputs.
 *
 * <p>Digital behavior follows the Philips 74HC/HCT7002 product specification. The pinout matches
 * 74x02: pin 1 is 1Y, pin 2 is 1A, pin 3 is 1B, and pin 13 is 4Y. Each output is the NOR of its
 * two inputs. Schmitt-trigger hysteresis is electrical and is not simulated. Texas Instruments
 * SN74HC7002 uses a different pinout and is not this model.
 */
public class Ttl747002 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "747002";

  public static final int PORT_1Y = 0;
  public static final int PORT_1A = 1;
  public static final int PORT_1B = 2;
  public static final int PORT_2Y = 3;
  public static final int PORT_2A = 4;
  public static final int PORT_2B = 5;
  public static final int PORT_3A = 6;
  public static final int PORT_3B = 7;
  public static final int PORT_3Y = 8;
  public static final int PORT_4A = 9;
  public static final int PORT_4B = 10;
  public static final int PORT_4Y = 11;
  public static final int PORT_GND = 12;
  public static final int PORT_VCC = 13;

  private static final byte PIN_COUNT = 14;
  private static final byte[] OUTPUT_PINS = {1, 4, 10, 13};
  private static final String[] PORT_NAMES = {
    "1Y", "1A", "1B", "2Y", "2A", "2B", "3A", "3B", "3Y", "4A", "4B", "4Y"
  };

  public Ttl747002() {
    super(
        _ID,
        PIN_COUNT,
        OUTPUT_PINS,
        null,
        null,
        PORT_NAMES,
        true,
        DEFAULT_HEIGHT,
        new Ttl747002HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    final var portwidth = 18;
    final var portheight = 15;
    final var youtput = y + (up ? 20 : 40);
    Drawgates.paintOr(g, x + 20, youtput, portwidth - 4, portheight, true, true);
    Drawgates.paintOutputgate(g, x + 10, y, x + 16, youtput, up, height);
    Drawgates.paintDoubleInputgate(
        g, x + 50, y, x + 16 + portwidth, youtput, portheight, up, true, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    for (var i = 0; i < 6; i += 3) {
      state.setPort(i, state.getPortValue(i + 1).or(state.getPortValue(i + 2)).not(), 1);
    }
    for (var i = 8; i < 12; i += 3) {
      state.setPort(i, state.getPortValue(i - 1).or(state.getPortValue(i - 2)).not(), 1);
    }
  }
}
