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
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.util.LineBuffer;

/**
 * TTL 74x09: quad 2-input AND gate with open-drain outputs.
 *
 * <p>Model based on the
 * <a href="https://media.digikey.com/pdf/Data%20Sheets/ST%20Microelectronics%20PDFS/M74HC09_Rev_2.pdf">M74HC09 datasheet</a>.
 * The function table is L/L = L, L/H = L, H/L = L, H/H = Z, and the pinout matches the 74 series
 * 09. A released output is {@link Value#UNKNOWN}, so a pull-up resistor makes the net high and
 * several outputs can be wired together. This is a digital functional model; propagation delay and
 * output current are not simulated.
 */
public class Ttl7409 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "7409";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  public static final byte A1 = 1;
  public static final byte B1 = 2;
  public static final byte Y1 = 3;

  public static final byte A2 = 4;
  public static final byte B2 = 5;
  public static final byte Y2 = 6;

  public static final byte GND = 7;

  public static final byte Y3 = 8;
  public static final byte A3 = 9;
  public static final byte B3 = 10;

  public static final byte Y4 = 11;
  public static final byte A4 = 12;
  public static final byte B4 = 13;

  public static final byte VCC = 14;

  private static final byte[] OUTPUTS = {Y1, Y2, Y3, Y4};

  private static final String[] PORT_NAMES = {
    "1A", "1B", "1Y", "2A", "2B", "2Y", "3Y", "3A", "3B", "4Y", "4A", "4B"
  };

  /**
   * HDL for one open-drain AND. A low product drives 0; a high product releases the pin. Shown
   * power pins are rejected by {@link AbstractGateHdlGenerator}.
   */
  static class OpenDrainAndHdlGenerator extends AbstractGateHdlGenerator {
    @Override
    public LineBuffer getLogicFunction(int index) {
      if (Hdl.isVhdl()) {
        // VHDL keywords replace {{and}} with a spaceless AND, so restore the operator pair.
        return LineBuffer.getHdlBuffer()
            .addVhdlKeywords()
            .pair("and", Hdl.andOperator())
            .add(
                "gateO{{1}} <= '0' {{when}} (gateA{{1}}{{and}}gateB{{1}}) = '0' {{else}} 'Z';",
                index);
      }
      return LineBuffer.getHdlBuffer()
          .add("{{assign}}gateO{{1}}{{=}}(gateA{{1}} & gateB{{1}}) ? 1'bZ : 1'b0;", index);
    }
  }

  /** Creates a 7409 quad 2-input AND gate with open-drain outputs. */
  public Ttl7409() {
    super(
        _ID,
        (byte) 14,
        OUTPUTS,
        null,
        null,
        PORT_NAMES,
        true,
        DEFAULT_HEIGHT,
        new OpenDrainAndHdlGenerator());
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Power pins are omitted from the port list. When the power-pin attribute is enabled, GND and
   * VCC are the last two ports and are not described by this conversion.
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    return (byte) ((dsPinNr <= GND) ? dsPinNr - 1 : dsPinNr - 2);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    final var portwidth = 15;
    final var portheight = 15;
    final var youtput = y + (up ? 20 : 40);
    Drawgates.paintAnd(g, x + 44, youtput, portwidth, portheight, false);
    // output line
    Drawgates.paintOutputgate(g, x + 50, y, x + 44, youtput, up, height);
    // output type
    Drawgates.paintOpenCollector(g, x + 48, youtput);
    // input lines
    Drawgates.paintDoubleInputgate(
        g, x + 30, y, x + 44 - portwidth, youtput, portheight, up, false, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    drive(state, A1, B1, Y1);
    drive(state, A2, B2, Y2);
    drive(state, A3, B3, Y3);
    drive(state, A4, B4, Y4);
  }

  /**
   * Drives one open-drain AND. A single low input forces the pin low. Both inputs high release it.
   * Any other combination, including unknown and error, is left as the error from {@link
   * Value#and}.
   */
  private static void drive(InstanceState state, byte inputA, byte inputB, byte output) {
    final var product =
        state.getPortValue(pinNrToPortNr(inputA)).and(state.getPortValue(pinNrToPortNr(inputB)));
    state.setPort(pinNrToPortNr(output), product == Value.TRUE ? Value.UNKNOWN : product, DELAY);
  }
}
