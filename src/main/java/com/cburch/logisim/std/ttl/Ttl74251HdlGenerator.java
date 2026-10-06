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

/** VHDL and Verilog generator for the 74x251 data selector. */
public class Ttl74251HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates a generator that releases the outputs when nOE is left unconnected. */
  public Ttl74251HdlGenerator() {
    super();
    myWires
        .addWire("s_select", 3)
        .addWire("s_y", 1);
    // Data and select inputs pull low. nOE pulls high so an open enable stays disabled.
    myPorts
        .add(Port.INPUT, "I0", 1, Ttl74251.pinNrToPortNr(Ttl74251.I0))
        .add(Port.INPUT, "I1", 1, Ttl74251.pinNrToPortNr(Ttl74251.I1))
        .add(Port.INPUT, "I2", 1, Ttl74251.pinNrToPortNr(Ttl74251.I2))
        .add(Port.INPUT, "I3", 1, Ttl74251.pinNrToPortNr(Ttl74251.I3))
        .add(Port.INPUT, "I4", 1, Ttl74251.pinNrToPortNr(Ttl74251.I4))
        .add(Port.INPUT, "I5", 1, Ttl74251.pinNrToPortNr(Ttl74251.I5))
        .add(Port.INPUT, "I6", 1, Ttl74251.pinNrToPortNr(Ttl74251.I6))
        .add(Port.INPUT, "I7", 1, Ttl74251.pinNrToPortNr(Ttl74251.I7))
        .add(Port.INPUT, "S0", 1, Ttl74251.pinNrToPortNr(Ttl74251.S0))
        .add(Port.INPUT, "S1", 1, Ttl74251.pinNrToPortNr(Ttl74251.S1))
        .add(Port.INPUT, "S2", 1, Ttl74251.pinNrToPortNr(Ttl74251.S2))
        .add(Port.INPUT, "nOE", 1, Ttl74251.pinNrToPortNr(Ttl74251.NOE), false)
        .add(Port.OUTPUT, "Y", 1, Ttl74251.pinNrToPortNr(Ttl74251.Y))
        .add(Port.OUTPUT, "nY", 1, Ttl74251.pinNrToPortNr(Ttl74251.NY));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          s_select <= S2 & S1 & S0;
          s_y <= I0 {{when}} s_select = "000" {{else}}
                 I1 {{when}} s_select = "001" {{else}}
                 I2 {{when}} s_select = "010" {{else}}
                 I3 {{when}} s_select = "011" {{else}}
                 I4 {{when}} s_select = "100" {{else}}
                 I5 {{when}} s_select = "101" {{else}}
                 I6 {{when}} s_select = "110" {{else}}
                 I7;
          Y  <= s_y {{when}} nOE = '0' {{else}} 'Z';
          nY <= ({{not}} s_y) {{when}} nOE = '0' {{else}} 'Z';
          """);
    } else {
      contents.add(
          """
          assign s_select = {S2, S1, S0};
          assign s_y = (s_select == 3'b000) ? I0 :
                       (s_select == 3'b001) ? I1 :
                       (s_select == 3'b010) ? I2 :
                       (s_select == 3'b011) ? I3 :
                       (s_select == 3'b100) ? I4 :
                       (s_select == 3'b101) ? I5 :
                       (s_select == 3'b110) ? I6 :
                                              I7;
          assign Y  = (nOE == 0) ? s_y : 1'bz;
          assign nY = (nOE == 0) ? ~s_y : 1'bz;
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
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
