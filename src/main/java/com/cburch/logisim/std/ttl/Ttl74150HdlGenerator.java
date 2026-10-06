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

/** VHDL and Verilog generator for the 74x150 16-line to 1-line data selector. */
public class Ttl74150HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator whose unconnected strobe is pulled high, so the output stays high. */
  public Ttl74150HdlGenerator() {
    super();
    myWires
        .addWire("s_select", 4)
        .addWire("s_data", 16);
    // The strobe is active low, so an unconnected strobe disables the selector.
    myPorts
        .add(Port.INPUT, "E0", 1, Ttl74150.pinNrToPortNr(Ttl74150.E0))
        .add(Port.INPUT, "E1", 1, Ttl74150.pinNrToPortNr(Ttl74150.E1))
        .add(Port.INPUT, "E2", 1, Ttl74150.pinNrToPortNr(Ttl74150.E2))
        .add(Port.INPUT, "E3", 1, Ttl74150.pinNrToPortNr(Ttl74150.E3))
        .add(Port.INPUT, "E4", 1, Ttl74150.pinNrToPortNr(Ttl74150.E4))
        .add(Port.INPUT, "E5", 1, Ttl74150.pinNrToPortNr(Ttl74150.E5))
        .add(Port.INPUT, "E6", 1, Ttl74150.pinNrToPortNr(Ttl74150.E6))
        .add(Port.INPUT, "E7", 1, Ttl74150.pinNrToPortNr(Ttl74150.E7))
        .add(Port.INPUT, "E8", 1, Ttl74150.pinNrToPortNr(Ttl74150.E8))
        .add(Port.INPUT, "E9", 1, Ttl74150.pinNrToPortNr(Ttl74150.E9))
        .add(Port.INPUT, "E10", 1, Ttl74150.pinNrToPortNr(Ttl74150.E10))
        .add(Port.INPUT, "E11", 1, Ttl74150.pinNrToPortNr(Ttl74150.E11))
        .add(Port.INPUT, "E12", 1, Ttl74150.pinNrToPortNr(Ttl74150.E12))
        .add(Port.INPUT, "E13", 1, Ttl74150.pinNrToPortNr(Ttl74150.E13))
        .add(Port.INPUT, "E14", 1, Ttl74150.pinNrToPortNr(Ttl74150.E14))
        .add(Port.INPUT, "E15", 1, Ttl74150.pinNrToPortNr(Ttl74150.E15))
        .add(Port.INPUT, "nG", 1, Ttl74150.pinNrToPortNr(Ttl74150.G), false)
        .add(Port.INPUT, "A", 1, Ttl74150.pinNrToPortNr(Ttl74150.A))
        .add(Port.INPUT, "B", 1, Ttl74150.pinNrToPortNr(Ttl74150.B))
        .add(Port.INPUT, "C", 1, Ttl74150.pinNrToPortNr(Ttl74150.C))
        .add(Port.INPUT, "D", 1, Ttl74150.pinNrToPortNr(Ttl74150.D))
        .add(Port.OUTPUT, "W", 1, Ttl74150.pinNrToPortNr(Ttl74150.W));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          W <= '1' {{when}} nG = '1' {{else}}
               {{not}} s_data(to_integer(unsigned(s_select)));

          s_select <= D & C & B & A;
          s_data   <= E15 & E14 & E13 & E12 & E11 & E10 & E9 & E8 &
                      E7 & E6 & E5 & E4 & E3 & E2 & E1 & E0;
          """);
    } else {
      contents.add(
          """
          assign W = (nG == 1) ? 1'b1 : ~s_data[s_select];

          assign s_select = {D, C, B, A};
          assign s_data   = {E15, E14, E13, E12, E11, E10, E9, E8,
                             E7, E6, E5, E4, E3, E2, E1, E0};
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
