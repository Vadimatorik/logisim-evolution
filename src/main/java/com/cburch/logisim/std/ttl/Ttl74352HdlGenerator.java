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

/** VHDL and Verilog generator for the 74x352 inverting dual 4-line to 1-line data selector. */
public class Ttl74352HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator whose unconnected inputs are pulled high. */
  public Ttl74352HdlGenerator() {
    super();
    // A pulled-high strobe inhibits the section, so an unconnected device holds both outputs high.
    myPorts
        .add(Port.INPUT, "nE1", 1, 0, false)
        .add(Port.INPUT, "s1", 1, 1, false)
        .add(Port.INPUT, "d1_3", 1, 2, false)
        .add(Port.INPUT, "d1_2", 1, 3, false)
        .add(Port.INPUT, "d1_1", 1, 4, false)
        .add(Port.INPUT, "d1_0", 1, 5, false)
        .add(Port.OUTPUT, "nY1", 1, 6)
        .add(Port.OUTPUT, "nY2", 1, 7)
        .add(Port.INPUT, "d2_0", 1, 8, false)
        .add(Port.INPUT, "d2_1", 1, 9, false)
        .add(Port.INPUT, "d2_2", 1, 10, false)
        .add(Port.INPUT, "d2_3", 1, 11, false)
        .add(Port.INPUT, "s0", 1, 12, false)
        .add(Port.INPUT, "nE2", 1, 13, false);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          nY1 <= '1' {{when}} nE1 = '1' {{else}}
                 {{not}} d1_0 {{when}} s1 = '0' {{and}} s0 = '0' {{else}}
                 {{not}} d1_1 {{when}} s1 = '0' {{and}} s0 = '1' {{else}}
                 {{not}} d1_2 {{when}} s1 = '1' {{and}} s0 = '0' {{else}}
                 {{not}} d1_3;

          nY2 <= '1' {{when}} nE2 = '1' {{else}}
                 {{not}} d2_0 {{when}} s1 = '0' {{and}} s0 = '0' {{else}}
                 {{not}} d2_1 {{when}} s1 = '0' {{and}} s0 = '1' {{else}}
                 {{not}} d2_2 {{when}} s1 = '1' {{and}} s0 = '0' {{else}}
                 {{not}} d2_3;
          """);
    } else {
      contents.add(
          """
          assign nY1 = (nE1 == 1) ? 1'b1 :
                       (s1 == 0 && s0 == 0) ? ~d1_0 :
                       (s1 == 0 && s0 == 1) ? ~d1_1 :
                       (s1 == 1 && s0 == 0) ? ~d1_2 :
                                              ~d1_3;

          assign nY2 = (nE2 == 1) ? 1'b1 :
                       (s1 == 0 && s0 == 0) ? ~d2_0 :
                       (s1 == 0 && s0 == 1) ? ~d2_1 :
                       (s1 == 1 && s0 == 0) ? ~d2_2 :
                                              ~d2_3;
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
