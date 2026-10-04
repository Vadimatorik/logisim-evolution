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

/** VHDL and Verilog for the synchronous load and JK shift of a 74199. */
public class Ttl74199HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the 74199. Open active-low controls stay high. */
  public Ttl74199HdlGenerator() {
    super();
    myWires.addWire("curState", 8).addWire("nextState", 8).addWire("serial", 1);
    // MR, CE and PE pull high, so an open register neither resets, shifts, nor loads.
    // J, K and the parallel data pull low.
    myPorts
        .add(Port.INPUT, "J", 1, Ttl74199.pinNrToPortNr(Ttl74199.J), true)
        .add(Port.INPUT, "K", 1, Ttl74199.pinNrToPortNr(Ttl74199.K), true)
        .add(Port.INPUT, "D0", 1, Ttl74199.pinNrToPortNr(Ttl74199.D0), true)
        .add(Port.OUTPUT, "Q0", 1, Ttl74199.pinNrToPortNr(Ttl74199.Q0))
        .add(Port.INPUT, "D1", 1, Ttl74199.pinNrToPortNr(Ttl74199.D1), true)
        .add(Port.OUTPUT, "Q1", 1, Ttl74199.pinNrToPortNr(Ttl74199.Q1))
        .add(Port.INPUT, "D2", 1, Ttl74199.pinNrToPortNr(Ttl74199.D2), true)
        .add(Port.OUTPUT, "Q2", 1, Ttl74199.pinNrToPortNr(Ttl74199.Q2))
        .add(Port.INPUT, "D3", 1, Ttl74199.pinNrToPortNr(Ttl74199.D3), true)
        .add(Port.OUTPUT, "Q3", 1, Ttl74199.pinNrToPortNr(Ttl74199.Q3))
        .add(Port.INPUT, "CE", 1, Ttl74199.pinNrToPortNr(Ttl74199.CE), false)
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl74199.pinNrToPortNr(Ttl74199.CP), true)
        .add(Port.INPUT, "MR", 1, Ttl74199.pinNrToPortNr(Ttl74199.MR), false)
        .add(Port.OUTPUT, "Q4", 1, Ttl74199.pinNrToPortNr(Ttl74199.Q4))
        .add(Port.INPUT, "D4", 1, Ttl74199.pinNrToPortNr(Ttl74199.D4), true)
        .add(Port.OUTPUT, "Q5", 1, Ttl74199.pinNrToPortNr(Ttl74199.Q5))
        .add(Port.INPUT, "D5", 1, Ttl74199.pinNrToPortNr(Ttl74199.D5), true)
        .add(Port.OUTPUT, "Q6", 1, Ttl74199.pinNrToPortNr(Ttl74199.Q6))
        .add(Port.INPUT, "D6", 1, Ttl74199.pinNrToPortNr(Ttl74199.D6), true)
        .add(Port.OUTPUT, "Q7", 1, Ttl74199.pinNrToPortNr(Ttl74199.Q7))
        .add(Port.INPUT, "D7", 1, Ttl74199.pinNrToPortNr(Ttl74199.D7), true)
        .add(Port.INPUT, "PE", 1, Ttl74199.pinNrToPortNr(Ttl74199.PE), false);
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
                     curState {{when}} CE = '1' {{else}}
                     D7&D6&D5&D4&D3&D2&D1&D0 {{when}} PE = '0' {{else}}
                     curState(6 {{downto}} 0) & serial;

        dffs : {{process}}({{CLK}}, MR) {{is}}
           {{begin}}
              {{if}} (MR = '0') {{then}} curState <= "00000000";
              {{elsif}} (rising_edge({{CLK}})) {{then}}
                 curState <= nextState;
              {{end}} {{if}};
           {{end}} {{process}} dffs;

        Q0 <= curState(0);
        Q1 <= curState(1);
        Q2 <= curState(2);
        Q3 <= curState(3);
        Q4 <= curState(4);
        Q5 <= curState(5);
        Q6 <= curState(6);
        Q7 <= curState(7);
        """;
  }

  private static String verilog() {
    return """
        assign serial    = (J & ~curState[0]) | (K & curState[0]);
        assign nextState = ({{tick}} == 0) ? curState :
                           (CE == 1) ? curState :
                           (PE == 0) ? {D7, D6, D5, D4, D3, D2, D1, D0} :
                           {curState[6:0], serial};
        assign Q0 = curState[0];
        assign Q1 = curState[1];
        assign Q2 = curState[2];
        assign Q3 = curState[3];
        assign Q4 = curState[4];
        assign Q5 = curState[5];
        assign Q6 = curState[6];
        assign Q7 = curState[7];

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
