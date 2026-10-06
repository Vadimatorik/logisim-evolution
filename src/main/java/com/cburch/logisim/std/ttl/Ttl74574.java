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
import com.cburch.logisim.instance.StdAttr;

/**
 * TTL 74HC574: octal positive-edge D flip-flop with three-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74hc574.pdf">TI SN74HC574</a> and
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT574.pdf">Nexperia 74HC574</a>
 * data sheets. The function matches the 74374: each flip-flop stores its D input on the rising
 * edge of CLK, and a low nOE drives the stored bit onto Q. A high nOE, or any level that is not a
 * solid low, releases every Q. Output enable does not change the register, and the register still
 * loads while the outputs are released. High impedance is reported as unknown. The DIP-20 pinout
 * is flow-through: inputs D1–D8 are opposite outputs Q1–Q8. Pin names follow the TI numbering used
 * by the 74273 and 74377 in this library, where Q1 is Nexperia's Q0 and D1 is Nexperia's D0.
 */
public class Ttl74574 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74574";

  public static final int PORT_INDEX_nOE = 0;
  public static final int PORT_INDEX_D1 = 1;
  public static final int PORT_INDEX_D2 = 2;
  public static final int PORT_INDEX_D3 = 3;
  public static final int PORT_INDEX_D4 = 4;
  public static final int PORT_INDEX_D5 = 5;
  public static final int PORT_INDEX_D6 = 6;
  public static final int PORT_INDEX_D7 = 7;
  public static final int PORT_INDEX_D8 = 8;
  public static final int PORT_INDEX_CLK = 9;
  public static final int PORT_INDEX_Q8 = 10;
  public static final int PORT_INDEX_Q7 = 11;
  public static final int PORT_INDEX_Q6 = 12;
  public static final int PORT_INDEX_Q5 = 13;
  public static final int PORT_INDEX_Q4 = 14;
  public static final int PORT_INDEX_Q3 = 15;
  public static final int PORT_INDEX_Q2 = 16;
  public static final int PORT_INDEX_Q1 = 17;
  /** Port index of GND when the explicit power pins are shown. */
  public static final int PORT_INDEX_GND = 18;
  /** Port index of VCC when the explicit power pins are shown. */
  public static final int PORT_INDEX_VCC = 19;

  private static final int DELAY = 1;
  private static final int BITS = 8;
  /** Data port of each bit. Bit 0 is D1, which is Nexperia's D0. */
  private static final int[] DATA = {
    PORT_INDEX_D1,
    PORT_INDEX_D2,
    PORT_INDEX_D3,
    PORT_INDEX_D4,
    PORT_INDEX_D5,
    PORT_INDEX_D6,
    PORT_INDEX_D7,
    PORT_INDEX_D8
  };
  /** Output port of each bit. Bit 0 is Q1, which is Nexperia's Q0. */
  private static final int[] OUTPUTS = {
    PORT_INDEX_Q1,
    PORT_INDEX_Q2,
    PORT_INDEX_Q3,
    PORT_INDEX_Q4,
    PORT_INDEX_Q5,
    PORT_INDEX_Q6,
    PORT_INDEX_Q7,
    PORT_INDEX_Q8
  };
  private static final byte[] OUTPUT_PINS = {12, 13, 14, 15, 16, 17, 18, 19};
  private static final String[] PORT_NAMES = {
    "nOE (output enable, active low)",
    "D1",
    "D2",
    "D3",
    "D4",
    "D5",
    "D6",
    "D7",
    "D8",
    "CLK",
    "Q8",
    "Q7",
    "Q6",
    "Q5",
    "Q4",
    "Q3",
    "Q2",
    "Q1"
  };

  /** Creates a 74574 octal positive-edge D flip-flop with three-state outputs. */
  public Ttl74574() {
    super(_ID, (byte) 20, OUTPUT_PINS, PORT_NAMES, new Ttl74574HdlGenerator());
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
          "nOE", "D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8", null,
          "CLK", "Q8", "Q7", "Q6", "Q5", "Q4", "Q3", "Q2", "Q1", null
        });
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.ONE, BITS);
      state.setData(data);
    }
    if (data.updateClock(state.getPortValue(PORT_INDEX_CLK), StdAttr.TRIG_RISING)) {
      for (var bit = 0; bit < BITS; bit++) {
        data.setValue(bit, state.getPortValue(DATA[bit]));
      }
    }
    final var enabled = state.getPortValue(PORT_INDEX_nOE) == Value.FALSE;
    for (var bit = 0; bit < BITS; bit++) {
      state.setPort(OUTPUTS[bit], enabled ? data.getValue(bit) : Value.UNKNOWN, DELAY);
    }
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_CLK};
  }
}
