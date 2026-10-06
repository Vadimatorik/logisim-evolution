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

/** VHDL and Verilog for the falling-edge word select of a 74298. */
public class Ttl74298HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the 74298. */
  public Ttl74298HdlGenerator() {
    super();
    myWires.addRegister("curState", 4).addWire("nextState", 4);
    myPorts
        .add(Port.INPUT, "B2", 1, Ttl74298.pinNrToPortNr(Ttl74298.B2))
        .add(Port.INPUT, "A2", 1, Ttl74298.pinNrToPortNr(Ttl74298.A2))
        .add(Port.INPUT, "A1", 1, Ttl74298.pinNrToPortNr(Ttl74298.A1))
        .add(Port.INPUT, "B1", 1, Ttl74298.pinNrToPortNr(Ttl74298.B1))
        .add(Port.INPUT, "C2", 1, Ttl74298.pinNrToPortNr(Ttl74298.C2))
        .add(Port.INPUT, "D2", 1, Ttl74298.pinNrToPortNr(Ttl74298.D2))
        .add(Port.INPUT, "D1", 1, Ttl74298.pinNrToPortNr(Ttl74298.D1))
        .add(Port.INPUT, "C1", 1, Ttl74298.pinNrToPortNr(Ttl74298.C1))
        .add(Port.INPUT, "WS", 1, Ttl74298.pinNrToPortNr(Ttl74298.WS))
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl74298.pinNrToPortNr(Ttl74298.CLK))
        .add(Port.OUTPUT, "QD", 1, Ttl74298.pinNrToPortNr(Ttl74298.QD))
        .add(Port.OUTPUT, "QC", 1, Ttl74298.pinNrToPortNr(Ttl74298.QC))
        .add(Port.OUTPUT, "QB", 1, Ttl74298.pinNrToPortNr(Ttl74298.QB))
        .add(Port.OUTPUT, "QA", 1, Ttl74298.pinNrToPortNr(Ttl74298.QA));
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
                     D1&C1&B1&A1 {{when}} WS = '0' {{else}}
                     D2&C2&B2&A2;

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
                           (WS == 0) ? {D1, C1, B1, A1} :
                           {D2, C2, B2, A2};
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
