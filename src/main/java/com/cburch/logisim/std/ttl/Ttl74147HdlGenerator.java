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

/** VHDL and Verilog generator for the 74x147 priority encoder. */
public class Ttl74147HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator whose unconnected inputs are pulled high. */
  public Ttl74147HdlGenerator() {
    super();
    myWires.addWire("s_code", 4);
    // Unconnected inputs are pulled to one, as every input of this device is active low.
    myPorts
        .add(Port.INPUT, "nI1", 1, 9, false)
        .add(Port.INPUT, "nI2", 1, 10, false)
        .add(Port.INPUT, "nI3", 1, 11, false)
        .add(Port.INPUT, "nI4", 1, 0, false)
        .add(Port.INPUT, "nI5", 1, 1, false)
        .add(Port.INPUT, "nI6", 1, 2, false)
        .add(Port.INPUT, "nI7", 1, 3, false)
        .add(Port.INPUT, "nI8", 1, 4, false)
        .add(Port.INPUT, "nI9", 1, 8, false)
        .add(Port.OUTPUT, "nY0", 1, 7)
        .add(Port.OUTPUT, "nY1", 1, 6)
        .add(Port.OUTPUT, "nY2", 1, 5)
        .add(Port.OUTPUT, "nY3", 1, 12);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          nY3 <= {{not}} s_code(3);
          nY2 <= {{not}} s_code(2);
          nY1 <= {{not}} s_code(1);
          nY0 <= {{not}} s_code(0);

          s_code <= "1001" {{when}} nI9 = '0' {{else}}
                    "1000" {{when}} nI8 = '0' {{else}}
                    "0111" {{when}} nI7 = '0' {{else}}
                    "0110" {{when}} nI6 = '0' {{else}}
                    "0101" {{when}} nI5 = '0' {{else}}
                    "0100" {{when}} nI4 = '0' {{else}}
                    "0011" {{when}} nI3 = '0' {{else}}
                    "0010" {{when}} nI2 = '0' {{else}}
                    "0001" {{when}} nI1 = '0' {{else}}
                    "0000";
          """);
    } else {
      contents.add(
          """
          assign nY3 = ~s_code[3];
          assign nY2 = ~s_code[2];
          assign nY1 = ~s_code[1];
          assign nY0 = ~s_code[0];

          assign s_code = (nI9 == 0) ? 4'b1001 :
                          (nI8 == 0) ? 4'b1000 :
                          (nI7 == 0) ? 4'b0111 :
                          (nI6 == 0) ? 4'b0110 :
                          (nI5 == 0) ? 4'b0101 :
                          (nI4 == 0) ? 4'b0100 :
                          (nI3 == 0) ? 4'b0011 :
                          (nI2 == 0) ? 4'b0010 :
                          (nI1 == 0) ? 4'b0001 :
                                       4'b0000;
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
