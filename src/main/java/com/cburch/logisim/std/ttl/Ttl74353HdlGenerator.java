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
 * VHDL and Verilog for one 74x353. Each output is the inverted selected source while its strobe is
 * low, and high-impedance while that strobe is high. The FPGA netlist rejects three-state drivers,
 * so this text is not synthesized.
 */
public class Ttl74353HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the two inverting 3-state multiplexers. */
  public Ttl74353HdlGenerator() {
    super();
    myPorts
        .add(Port.INPUT, "G1", 1, Ttl74353.pinNrToPortNr(Ttl74353.G1))
        .add(Port.INPUT, "B", 1, Ttl74353.pinNrToPortNr(Ttl74353.B))
        .add(Port.INPUT, "C3_1", 1, Ttl74353.pinNrToPortNr(Ttl74353.C1_3))
        .add(Port.INPUT, "C2_1", 1, Ttl74353.pinNrToPortNr(Ttl74353.C1_2))
        .add(Port.INPUT, "C1_1", 1, Ttl74353.pinNrToPortNr(Ttl74353.C1_1))
        .add(Port.INPUT, "C0_1", 1, Ttl74353.pinNrToPortNr(Ttl74353.C1_0))
        .add(Port.OUTPUT, "Y1", 1, Ttl74353.pinNrToPortNr(Ttl74353.Y1))
        .add(Port.OUTPUT, "Y2", 1, Ttl74353.pinNrToPortNr(Ttl74353.Y2))
        .add(Port.INPUT, "C0_2", 1, Ttl74353.pinNrToPortNr(Ttl74353.C2_0))
        .add(Port.INPUT, "C1_2", 1, Ttl74353.pinNrToPortNr(Ttl74353.C2_1))
        .add(Port.INPUT, "C2_2", 1, Ttl74353.pinNrToPortNr(Ttl74353.C2_2))
        .add(Port.INPUT, "C3_2", 1, Ttl74353.pinNrToPortNr(Ttl74353.C2_3))
        .add(Port.INPUT, "A", 1, Ttl74353.pinNrToPortNr(Ttl74353.A))
        .add(Port.INPUT, "G2", 1, Ttl74353.pinNrToPortNr(Ttl74353.G2));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          Y1 <= 'Z' {{when}} G1 = '1' {{else}}
                not C3_1 {{when}} B = '1' {{and}} A = '1' {{else}}
                not C2_1 {{when}} B = '1' {{and}} A = '0' {{else}}
                not C1_1 {{when}} B = '0' {{and}} A = '1' {{else}}
                not C0_1;
          Y2 <= 'Z' {{when}} G2 = '1' {{else}}
                not C3_2 {{when}} B = '1' {{and}} A = '1' {{else}}
                not C2_2 {{when}} B = '1' {{and}} A = '0' {{else}}
                not C1_2 {{when}} B = '0' {{and}} A = '1' {{else}}
                not C0_2;
          """);
    } else {
      contents.add(
          """
          assign Y1 = (G1 == 1) ? 1'bZ :
                      ({B, A} == 2'b11) ? ~C3_1 :
                      ({B, A} == 2'b10) ? ~C2_1 :
                      ({B, A} == 2'b01) ? ~C1_1 :
                                           ~C0_1;
          assign Y2 = (G2 == 1) ? 1'bZ :
                      ({B, A} == 2'b11) ? ~C3_2 :
                      ({B, A} == 2'b10) ? ~C2_2 :
                      ({B, A} == 2'b01) ? ~C1_2 :
                                           ~C0_2;
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
