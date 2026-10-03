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

/**
 * VHDL and Verilog for one 74HC258. Each output is the inverted selected source while {@code OE}
 * is low, and high-impedance while {@code OE} is high. The FPGA netlist rejects three-state
 * drivers, so this text is not synthesized.
 */
public class Ttl74258HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates a generator for the quad inverting selector. */
  public Ttl74258HdlGenerator() {
    super();
    myPorts
        .add(Port.INPUT, "S", 1, Ttl74258.pinNrToPortNr(Ttl74258.S))
        .add(Port.INPUT, "OE", 1, Ttl74258.pinNrToPortNr(Ttl74258.OE));
    for (var channel = 0; channel < Ttl74258.Y.length; channel++) {
      final var index = channel + 1;
      myPorts
          .add(Port.INPUT, "I0_" + index, 1, Ttl74258.pinNrToPortNr(Ttl74258.I0[channel]))
          .add(Port.INPUT, "I1_" + index, 1, Ttl74258.pinNrToPortNr(Ttl74258.I1[channel]))
          .add(Port.OUTPUT, "Y" + index, 1, Ttl74258.pinNrToPortNr(Ttl74258.Y[channel]));
    }
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords();
      for (var channel = 1; channel <= Ttl74258.Y.length; channel++) {
        contents.add(
            "Y{{1}} <= 'Z' {{when}} OE = '1' {{else}}"
                + " {{not}} I1_{{1}} {{when}} S = '1' {{else}} {{not}} I0_{{1}};",
            channel);
      }
    } else {
      for (var channel = 1; channel <= Ttl74258.Y.length; channel++) {
        contents.add(
            "assign Y{{1}} = (OE == 1) ? 1'bZ : (S == 1) ? ~I1_{{1}} : ~I0_{{1}};", channel);
      }
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) {
      return false;
    }
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
