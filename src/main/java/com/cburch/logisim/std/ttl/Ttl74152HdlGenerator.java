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

/** VHDL and Verilog generator for the 74x152 8-line to 1-line data selector. */
public class Ttl74152HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator whose unconnected inputs are pulled low. */
  public Ttl74152HdlGenerator() {
    super();
    myWires.addWire("s_select", 3);
    // Unconnected inputs pull low, so an open device selects D0 at a low level.
    myPorts
        .add(Port.INPUT, "D4", 1, Ttl74152.pinNrToPortNr(Ttl74152.D4), true)
        .add(Port.INPUT, "D3", 1, Ttl74152.pinNrToPortNr(Ttl74152.D3), true)
        .add(Port.INPUT, "D2", 1, Ttl74152.pinNrToPortNr(Ttl74152.D2), true)
        .add(Port.INPUT, "D1", 1, Ttl74152.pinNrToPortNr(Ttl74152.D1), true)
        .add(Port.INPUT, "D0", 1, Ttl74152.pinNrToPortNr(Ttl74152.D0), true)
        .add(Port.OUTPUT, "W", 1, Ttl74152.pinNrToPortNr(Ttl74152.W))
        .add(Port.INPUT, "C", 1, Ttl74152.pinNrToPortNr(Ttl74152.C), true)
        .add(Port.INPUT, "B", 1, Ttl74152.pinNrToPortNr(Ttl74152.B), true)
        .add(Port.INPUT, "A", 1, Ttl74152.pinNrToPortNr(Ttl74152.A), true)
        .add(Port.INPUT, "D7", 1, Ttl74152.pinNrToPortNr(Ttl74152.D7), true)
        .add(Port.INPUT, "D6", 1, Ttl74152.pinNrToPortNr(Ttl74152.D6), true)
        .add(Port.INPUT, "D5", 1, Ttl74152.pinNrToPortNr(Ttl74152.D5), true);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          W <= D0 {{when}} s_select = "000" {{else}}
               D1 {{when}} s_select = "001" {{else}}
               D2 {{when}} s_select = "010" {{else}}
               D3 {{when}} s_select = "011" {{else}}
               D4 {{when}} s_select = "100" {{else}}
               D5 {{when}} s_select = "101" {{else}}
               D6 {{when}} s_select = "110" {{else}}
               D7;

          s_select <= C & B & A;
          """);
    } else {
      contents.add(
          """
          assign W = (s_select == 3'b000) ? D0 :
                     (s_select == 3'b001) ? D1 :
                     (s_select == 3'b010) ? D2 :
                     (s_select == 3'b011) ? D3 :
                     (s_select == 3'b100) ? D4 :
                     (s_select == 3'b101) ? D5 :
                     (s_select == 3'b110) ? D6 :
                                            D7;

          assign s_select = {C, B, A};
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
