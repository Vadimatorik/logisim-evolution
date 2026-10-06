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
import com.cburch.logisim.fpga.hdlgenerator.HdlPorts;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

/** VHDL and Verilog for the rising-edge port select of a 74399. */
public class Ttl74399HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the 74399. */
  public Ttl74399HdlGenerator() {
    super();
    myWires.addRegister("curState", 4).addWire("nextState", 4);
    myPorts
        .add(Port.INPUT, "S", 1, Ttl74399.pinNrToPortNr(Ttl74399.S))
        .add(Port.INPUT, "I0a", 1, Ttl74399.pinNrToPortNr(Ttl74399.I0A))
        .add(Port.INPUT, "I1a", 1, Ttl74399.pinNrToPortNr(Ttl74399.I1A))
        .add(Port.INPUT, "I1b", 1, Ttl74399.pinNrToPortNr(Ttl74399.I1B))
        .add(Port.INPUT, "I0b", 1, Ttl74399.pinNrToPortNr(Ttl74399.I0B))
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl74399.pinNrToPortNr(Ttl74399.CP))
        .add(Port.INPUT, "I0c", 1, Ttl74399.pinNrToPortNr(Ttl74399.I0C))
        .add(Port.INPUT, "I1c", 1, Ttl74399.pinNrToPortNr(Ttl74399.I1C))
        .add(Port.INPUT, "I1d", 1, Ttl74399.pinNrToPortNr(Ttl74399.I1D))
        .add(Port.INPUT, "I0d", 1, Ttl74399.pinNrToPortNr(Ttl74399.I0D))
        .add(Port.OUTPUT, "Qa", 1, Ttl74399.pinNrToPortNr(Ttl74399.QA))
        .add(Port.OUTPUT, "Qb", 1, Ttl74399.pinNrToPortNr(Ttl74399.QB))
        .add(Port.OUTPUT, "Qc", 1, Ttl74399.pinNrToPortNr(Ttl74399.QC))
        .add(Port.OUTPUT, "Qd", 1, Ttl74399.pinNrToPortNr(Ttl74399.QD));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer().pair("CLK", HdlPorts.CLOCK).pair("tick", HdlPorts.TICK);
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(vhdl());
    } else {
      contents.add(verilog());
    }
    return contents.empty();
  }

  private static String vhdl() {
    return """
        nextState <= curState {{when}} {{tick}} = '0' {{else}}
                     I0d&I0c&I0b&I0a {{when}} S = '0' {{else}}
                     I1d&I1c&I1b&I1a;

        dffs : {{process}}({{CLK}}) {{is}}
           {{begin}}
              {{if}} (rising_edge({{CLK}})) {{then}}
                 curState <= nextState;
              {{end}} {{if}};
           {{end}} {{process}} dffs;

        Qd <= curState(3);
        Qc <= curState(2);
        Qb <= curState(1);
        Qa <= curState(0);
        """;
  }

  private static String verilog() {
    return """
        assign nextState = ({{tick}} == 0) ? curState :
                           (S == 0) ? {I0d, I0c, I0b, I0a} :
                           {I1d, I1c, I1b, I1a};
        assign Qd = curState[3];
        assign Qc = curState[2];
        assign Qb = curState[1];
        assign Qa = curState[0];

        always @(posedge {{CLK}})
        begin
           curState <= nextState;
        end
        """;
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) {
      return false;
    }
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
