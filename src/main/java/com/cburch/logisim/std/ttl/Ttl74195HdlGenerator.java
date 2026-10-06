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

/** VHDL and Verilog for the synchronous load and JK shift of a 74195. */
public class Ttl74195HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the 74195. */
  public Ttl74195HdlGenerator() {
    super();
    myWires
        .addWire("curState", 4)
        .addWire("nextState", 4)
        .addWire("serial", 1);
    myPorts
        .add(Port.INPUT, "MR", 1, Ttl74195.pinNrToPortNr(Ttl74195.MR))
        .add(Port.INPUT, "J", 1, Ttl74195.pinNrToPortNr(Ttl74195.J))
        .add(Port.INPUT, "K", 1, Ttl74195.pinNrToPortNr(Ttl74195.K))
        .add(Port.INPUT, "D0", 1, Ttl74195.pinNrToPortNr(Ttl74195.D0))
        .add(Port.INPUT, "D1", 1, Ttl74195.pinNrToPortNr(Ttl74195.D1))
        .add(Port.INPUT, "D2", 1, Ttl74195.pinNrToPortNr(Ttl74195.D2))
        .add(Port.INPUT, "D3", 1, Ttl74195.pinNrToPortNr(Ttl74195.D3))
        .add(Port.INPUT, "PE", 1, Ttl74195.pinNrToPortNr(Ttl74195.PE))
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl74195.pinNrToPortNr(Ttl74195.CP))
        .add(Port.OUTPUT, "nQ3", 1, Ttl74195.pinNrToPortNr(Ttl74195.NQ3))
        .add(Port.OUTPUT, "Q3", 1, Ttl74195.pinNrToPortNr(Ttl74195.Q3))
        .add(Port.OUTPUT, "Q2", 1, Ttl74195.pinNrToPortNr(Ttl74195.Q2))
        .add(Port.OUTPUT, "Q1", 1, Ttl74195.pinNrToPortNr(Ttl74195.Q1))
        .add(Port.OUTPUT, "Q0", 1, Ttl74195.pinNrToPortNr(Ttl74195.Q0));
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
        serial <= (J {{and}} {{not}}(curState(0))) {{or}} (K {{and}} curState(0));
        nextState <= curState {{when}} {{tick}} = '0' {{else}}
                     D3&D2&D1&D0 {{when}} PE = '0' {{else}}
                     curState(2 {{downto}} 0) & serial;

        dffs : {{process}}({{CLK}}, MR) {{is}}
           {{begin}}
              {{if}} (MR = '0') {{then}} curState <= "0000";
              {{elsif}} (rising_edge({{CLK}})) {{then}}
                 curState <= nextState;
              {{end}} {{if}};
           {{end}} {{process}} dffs;

        nQ3 <= {{not}}(curState(3));
        Q3  <= curState(3);
        Q2  <= curState(2);
        Q1  <= curState(1);
        Q0  <= curState(0);
        """;
  }

  private static String verilog() {
    return """
        assign serial    = (J & ~curState[0]) | (K & curState[0]);
        assign nextState = ({{tick}} == 0) ? curState :
                           (PE == 0) ? {D3, D2, D1, D0} :
                           {curState[2:0], serial};
        assign nQ3       = ~curState[3];
        assign Q3        = curState[3];
        assign Q2        = curState[2];
        assign Q1        = curState[1];
        assign Q0        = curState[0];

        always @(posedge {{CLK}} or negedge MR)
        begin
           if (~MR) curState <= 0;
           else curState <= nextState;
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
