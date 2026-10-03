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

/** VHDL and Verilog generator for the 74x253 dual 4-line to 1-line data selector. */
public class Ttl74253HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the two 3-state multiplexers. */
  public Ttl74253HdlGenerator() {
    super();
    myPorts
        .add(Port.INPUT, "n1OE", 1, Ttl74253.pinNrToPortNr(Ttl74253.OE1))
        .add(Port.INPUT, "S1", 1, Ttl74253.pinNrToPortNr(Ttl74253.S1))
        .add(Port.INPUT, "I3_1", 1, Ttl74253.pinNrToPortNr(Ttl74253.L1_I3))
        .add(Port.INPUT, "I2_1", 1, Ttl74253.pinNrToPortNr(Ttl74253.L1_I2))
        .add(Port.INPUT, "I1_1", 1, Ttl74253.pinNrToPortNr(Ttl74253.L1_I1))
        .add(Port.INPUT, "I0_1", 1, Ttl74253.pinNrToPortNr(Ttl74253.L1_I0))
        .add(Port.OUTPUT, "Y1", 1, Ttl74253.pinNrToPortNr(Ttl74253.L1_Y))
        .add(Port.OUTPUT, "Y2", 1, Ttl74253.pinNrToPortNr(Ttl74253.L2_Y))
        .add(Port.INPUT, "I0_2", 1, Ttl74253.pinNrToPortNr(Ttl74253.L2_I0))
        .add(Port.INPUT, "I1_2", 1, Ttl74253.pinNrToPortNr(Ttl74253.L2_I1))
        .add(Port.INPUT, "I2_2", 1, Ttl74253.pinNrToPortNr(Ttl74253.L2_I2))
        .add(Port.INPUT, "I3_2", 1, Ttl74253.pinNrToPortNr(Ttl74253.L2_I3))
        .add(Port.INPUT, "S0", 1, Ttl74253.pinNrToPortNr(Ttl74253.S0))
        .add(Port.INPUT, "n2OE", 1, Ttl74253.pinNrToPortNr(Ttl74253.OE2));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          Y1 <= 'Z' {{when}} n1OE = '1' {{else}}
                I3_1 {{when}} S1 = '1' {{and}} S0 = '1' {{else}}
                I2_1 {{when}} S1 = '1' {{and}} S0 = '0' {{else}}
                I1_1 {{when}} S1 = '0' {{and}} S0 = '1' {{else}}
                I0_1;
          Y2 <= 'Z' {{when}} n2OE = '1' {{else}}
                I3_2 {{when}} S1 = '1' {{and}} S0 = '1' {{else}}
                I2_2 {{when}} S1 = '1' {{and}} S0 = '0' {{else}}
                I1_2 {{when}} S1 = '0' {{and}} S0 = '1' {{else}}
                I0_2;
          """);
    } else {
      contents.add(
          """
          assign Y1 = (n1OE == 1) ? 1'bZ :
                      ({S1, S0} == 2'b11) ? I3_1 :
                      ({S1, S0} == 2'b10) ? I2_1 :
                      ({S1, S0} == 2'b01) ? I1_1 :
                                             I0_1;
          assign Y2 = (n2OE == 1) ? 1'bZ :
                      ({S1, S0} == 2'b11) ? I3_2 :
                      ({S1, S0} == 2'b10) ? I2_2 :
                      ({S1, S0} == 2'b01) ? I1_2 :
                                             I0_2;
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
