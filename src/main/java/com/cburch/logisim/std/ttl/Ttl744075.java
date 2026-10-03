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

/**
 * TTL 74x4075: triple 3-input OR gate.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT4075.pdf">Nexperia
 * 74HC4075/74HCT4075</a> data sheet, Rev. 6, 21 March 2024. Each output is the OR of its three
 * inputs. The 74HCT4075 uses the same function table and pinout; CMOS and TTL input thresholds are
 * not modeled. The package is 14 pins with GND on pin 7 and VCC on pin 14. This pinout is not the
 * same as the 7410, 7411, or 7427.
 */
public class Ttl744075 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744075";

  public static final int PORT_INDEX_2A = 0;
  public static final int PORT_INDEX_2B = 1;
  public static final int PORT_INDEX_1A = 2;
  public static final int PORT_INDEX_1B = 3;
  public static final int PORT_INDEX_1C = 4;
  public static final int PORT_INDEX_1Y = 5;
  public static final int PORT_INDEX_2C = 6;
  public static final int PORT_INDEX_2Y = 7;
  public static final int PORT_INDEX_3Y = 8;
  public static final int PORT_INDEX_3A = 9;
  public static final int PORT_INDEX_3B = 10;
  public static final int PORT_INDEX_3C = 11;

  private static final int DELAY = 1;
  private static final byte[] OUTPUT_PINS = {6, 9, 10};
  private static final String[] PORT_NAMES = {
    "2A", "2B", "1A", "1B", "1C", "1Y", "2C", "2Y", "3Y", "3A", "3B", "3C"
  };

  /** Creates a 744075 triple 3-input OR gate. */
  public Ttl744075() {
    super(_ID, (byte) 14, OUTPUT_PINS, PORT_NAMES, new Ttl744075HdlGenerator());
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
          "2A", "2B", "1A", "1B", "1C", "1Y", null,
          "2C", "2Y", "3Y", "3A", "3B", "3C", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    state.setPort(PORT_INDEX_1Y, or3(state, PORT_INDEX_1A, PORT_INDEX_1B, PORT_INDEX_1C), DELAY);
    state.setPort(PORT_INDEX_2Y, or3(state, PORT_INDEX_2A, PORT_INDEX_2B, PORT_INDEX_2C), DELAY);
    state.setPort(PORT_INDEX_3Y, or3(state, PORT_INDEX_3A, PORT_INDEX_3B, PORT_INDEX_3C), DELAY);
  }

  private static Value or3(InstanceState state, int portA, int portB, int portC) {
    return state.getPortValue(portA).or(state.getPortValue(portB)).or(state.getPortValue(portC));
  }
}
