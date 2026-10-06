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
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.util.LineBuffer;

/** VHDL and Verilog for the open-collector buffers of a 7417. */
public class Ttl7417HdlGenerator extends AbstractGateHdlGenerator {

  /** Creates an HDL generator for the six single-input buffers. */
  public Ttl7417HdlGenerator() {
    super(true);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) contents.addVhdlKeywords();
    for (var gate = 0; gate < 6; gate++) {
      contents.addRemarkBlock(String.format("Here gate %d is described", gate));
      if (Hdl.isVhdl()) {
        contents.add(
            "gateO{{1}} <= 'Z' {{when}} gateA{{1}} = '1' {{else}}"
                + " '0' {{when}} gateA{{1}} = '0' {{else}} 'X';",
            gate);
      } else {
        contents.add(
            "assign gateO{{1}} = (gateA{{1}} == 1) ? 1'bZ : (gateA{{1}} == 0) ? 1'b0 : 1'bX;",
            gate);
      }
    }
    return contents;
  }
}
