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

/** VHDL and Verilog for the synchronous load, shift and hold of a 74178. */
public class Ttl74178HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the 74178. */
  public Ttl74178HdlGenerator() {
    super();
    myWires.addRegister("curState", 4).addWire("nextState", 4);
    myPorts
        .add(Port.INPUT, "B", 1, Ttl74178.pinNrToPortNr(Ttl74178.B))
        .add(Port.INPUT, "A", 1, Ttl74178.pinNrToPortNr(Ttl74178.A))
        .add(Port.INPUT, "SER", 1, Ttl74178.pinNrToPortNr(Ttl74178.SER))
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl74178.pinNrToPortNr(Ttl74178.CLK))
        .add(Port.INPUT, "LOAD", 1, Ttl74178.pinNrToPortNr(Ttl74178.LOAD))
        .add(Port.INPUT, "SHIFT", 1, Ttl74178.pinNrToPortNr(Ttl74178.SHIFT))
        .add(Port.INPUT, "D", 1, Ttl74178.pinNrToPortNr(Ttl74178.D))
        .add(Port.INPUT, "C", 1, Ttl74178.pinNrToPortNr(Ttl74178.C))
        .add(Port.OUTPUT, "QA", 1, Ttl74178.pinNrToPortNr(Ttl74178.QA))
        .add(Port.OUTPUT, "QB", 1, Ttl74178.pinNrToPortNr(Ttl74178.QB))
        .add(Port.OUTPUT, "QC", 1, Ttl74178.pinNrToPortNr(Ttl74178.QC))
        .add(Port.OUTPUT, "QD", 1, Ttl74178.pinNrToPortNr(Ttl74178.QD));
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
                     curState(2 {{downto}} 0) & SER {{when}} SHIFT = '1' {{else}}
                     D&C&B&A {{when}} LOAD = '1' {{else}}
                     curState;

        dffs : {{process}}({{CLK}}) {{is}}
           {{begin}}
              {{if}} (falling_edge({{CLK}})) {{then}}
                 curState <= nextState;
              {{end}} {{if}};
           {{end}} {{process}} dffs;

        QD <= curState(3);
        QC <= curState(2);
        QB <= curState(1);
        QA <= curState(0);
        """;
  }

  private static String verilog() {
    return """
        assign nextState = ({{tick}} == 0) ? curState :
                           (SHIFT == 1) ? {curState[2:0], SER} :
                           (LOAD == 1) ? {D, C, B, A} :
                           curState;
        assign QD = curState[3];
        assign QC = curState[2];
        assign QB = curState[1];
        assign QA = curState[0];

        always @(negedge {{CLK}})
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
