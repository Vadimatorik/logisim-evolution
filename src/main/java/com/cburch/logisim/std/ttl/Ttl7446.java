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
 * TTL 7446: BCD to 7-segment decoder/driver.
 *
 * <p>Simulation follows the function table shared by the
 * <a href="https://media.digikey.com/pdf/Data%20Sheets/Fairchild%20PDFs/DM7446A,%20DM7447A.pdf">DM7446A</a>
 * and TI SN7446A/SN74LS46 data sheets. Segment outputs are active low. A low {@code BI} blanks
 * every segment. Otherwise a low {@code LT} turns every segment on. Otherwise a low {@code RBI}
 * blanks a BCD zero and drives {@code RBO} low. Codes 10 through 15 keep the non-decimal patterns
 * from that table. The 30 V open-collector rating is not modeled; segment pins are logic levels,
 * the same way as the 7447. {@code BI/RBO} is an open-collector pin with a pull-up. There is no
 * separate 74HC46 data sheet; a 74HC46 in the same DIP-16 pinout follows this table.
 */
public class Ttl7446 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7446";

  public static final int PORT_INDEX_B = 0;
  public static final int PORT_INDEX_C = 1;
  public static final int PORT_INDEX_LT = 2;
  public static final int PORT_INDEX_BI_RBO = 3;
  public static final int PORT_INDEX_RBI = 4;
  public static final int PORT_INDEX_D = 5;
  public static final int PORT_INDEX_A = 6;
  public static final int PORT_INDEX_QE = 7;
  public static final int PORT_INDEX_QD = 8;
  public static final int PORT_INDEX_QC = 9;
  public static final int PORT_INDEX_QB = 10;
  public static final int PORT_INDEX_QA = 11;
  public static final int PORT_INDEX_QG = 12;
  public static final int PORT_INDEX_QF = 13;

  public Ttl7446() {
    super(
        _ID,
        (byte) 16,
        new byte[] {9, 10, 11, 12, 13, 14, 15},
        new byte[] {},
        new byte[] {4},
        new String[] {"B", "C", "LT", "BI/RBO", "RBI", "D", "A", "e", "d", "c", "b", "a", "g", "f"},
        new Ttl7447HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, true, false);
    Drawgates.paintPortNames(painter, x, y, height, super.portNames);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    super.configureNewInstance(instance);
    instance.getComponent().setPullPorts(Map.of(PORT_INDEX_BI_RBO, Value.TRUE));
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var inputValue =
        DisplayDecoder.getdecval(
            state, false, 0, PORT_INDEX_A, PORT_INDEX_B, PORT_INDEX_C, PORT_INDEX_D);
    final var blankZero = state.getPortValue(PORT_INDEX_RBI) == Value.FALSE && inputValue == 0;
    DisplayDecoder.computeDisplayDecoderOutputs(
        state,
        inputValue,
        PORT_INDEX_QA,
        PORT_INDEX_QB,
        PORT_INDEX_QC,
        PORT_INDEX_QD,
        PORT_INDEX_QE,
        PORT_INDEX_QF,
        PORT_INDEX_QG,
        PORT_INDEX_LT,
        PORT_INDEX_BI_RBO,
        PORT_INDEX_RBI);
    final var rippleBlank = blankZero && state.getPortValue(PORT_INDEX_LT) != Value.FALSE;
    state.setPort(
        PORT_INDEX_BI_RBO, rippleBlank ? Value.FALSE : Value.UNKNOWN, PlexersLibrary.DELAY);
  }
}
