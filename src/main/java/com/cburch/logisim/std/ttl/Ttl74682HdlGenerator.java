/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

/** VHDL and Verilog generator for the 74x682 magnitude comparator. */
public class Ttl74682HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator whose unconnected inputs are pulled high. */
  public Ttl74682HdlGenerator() {
    super();
    myWires.addWire("oppP", 8).addWire("oppQ", 8);
    // Floating data bits read as one. The Q inputs also have pull-ups on the physical device.
    myPorts
        .add(Port.OUTPUT, "nPGTQ", 1, 0)
        .add(Port.INPUT, "P0", 1, 1, false)
        .add(Port.INPUT, "Q0", 1, 2, false)
        .add(Port.INPUT, "P1", 1, 3, false)
        .add(Port.INPUT, "Q1", 1, 4, false)
        .add(Port.INPUT, "P2", 1, 5, false)
        .add(Port.INPUT, "Q2", 1, 6, false)
        .add(Port.INPUT, "P3", 1, 7, false)
        .add(Port.INPUT, "Q3", 1, 8, false)
        .add(Port.INPUT, "P4", 1, 9, false)
        .add(Port.INPUT, "Q4", 1, 10, false)
        .add(Port.INPUT, "P5", 1, 11, false)
        .add(Port.INPUT, "Q5", 1, 12, false)
        .add(Port.INPUT, "P6", 1, 13, false)
        .add(Port.INPUT, "Q6", 1, 14, false)
        .add(Port.INPUT, "P7", 1, 15, false)
        .add(Port.INPUT, "Q7", 1, 16, false)
        .add(Port.OUTPUT, "nPQ", 1, 17);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          oppP   <= P7&P6&P5&P4&P3&P2&P1&P0;
          oppQ   <= Q7&Q6&Q5&Q4&Q3&Q2&Q1&Q0;
          nPQ    <= '0' {{when}} unsigned(oppP) = unsigned(oppQ) {{else}} '1';
          nPGTQ  <= '0' {{when}} unsigned(oppP) > unsigned(oppQ) {{else}} '1';
          """);
    } else {
      contents.add(
          """
          assign oppP   = {P7, P6, P5, P4, P3, P2, P1, P0};
          assign oppQ   = {Q7, Q6, Q5, Q4, Q3, Q2, Q1, Q0};
          assign nPQ    = (oppP == oppQ) ? 0 : 1;
          assign nPGTQ  = (oppP > oppQ) ? 0 : 1;
          """);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) {
      return false;
    }
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
