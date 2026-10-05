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

/**
 * VHDL and Verilog for the open-collector buffers of a 7435.
 *
 * <p>A low input drives a strong 0. Any other input releases the output ({@code 'Z'} / {@code
 * 1'bz}), matching the simulator's released high. The port order is the hex-buffer order shared
 * with the 7434.
 */
public class Ttl7435HdlGenerator extends AbstractGateHdlGenerator {

  /** Creates an HDL generator for the six open-collector buffers. */
  public Ttl7435HdlGenerator() {
    super(true);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    for (var gate = 0; gate < 6; gate++) {
      contents.addRemarkBlock(String.format("Here gate %d is described", gate));
      if (Hdl.isVhdl()) {
        contents.add("gateO{{1}} <= '0' when gateA{{1}} = '0' else 'Z';", gate);
      } else {
        contents.add("assign gateO{{1}} = (gateA{{1}} == 1'b0) ? 1'b0 : 1'bz;", gate);
      }
    }
    return contents.empty();
  }
}
