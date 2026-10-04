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

/** VHDL and Verilog for the synchronous load and bidirectional shift of a 74198. */
public class Ttl74198HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the 74198. */
  public Ttl74198HdlGenerator() {
    super();
    myWires.addWire("curState", 8).addWire("nextState", 8);
    myPorts
        .add(Port.INPUT, "S0", 1, Ttl74198.pinNrToPortNr(Ttl74198.S0))
        .add(Port.INPUT, "DSR", 1, Ttl74198.pinNrToPortNr(Ttl74198.DSR))
        .add(Port.INPUT, "D0", 1, Ttl74198.pinNrToPortNr(Ttl74198.D0))
        .add(Port.INPUT, "D1", 1, Ttl74198.pinNrToPortNr(Ttl74198.D1))
        .add(Port.INPUT, "D2", 1, Ttl74198.pinNrToPortNr(Ttl74198.D2))
        .add(Port.INPUT, "D3", 1, Ttl74198.pinNrToPortNr(Ttl74198.D3))
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl74198.pinNrToPortNr(Ttl74198.CP))
        .add(Port.INPUT, "MR", 1, Ttl74198.pinNrToPortNr(Ttl74198.MR))
        .add(Port.INPUT, "DSL", 1, Ttl74198.pinNrToPortNr(Ttl74198.DSL))
        .add(Port.INPUT, "D4", 1, Ttl74198.pinNrToPortNr(Ttl74198.D4))
        .add(Port.INPUT, "D5", 1, Ttl74198.pinNrToPortNr(Ttl74198.D5))
        .add(Port.INPUT, "D6", 1, Ttl74198.pinNrToPortNr(Ttl74198.D6))
        .add(Port.INPUT, "D7", 1, Ttl74198.pinNrToPortNr(Ttl74198.D7))
        .add(Port.INPUT, "S1", 1, Ttl74198.pinNrToPortNr(Ttl74198.S1))
        .add(Port.OUTPUT, "Q0", 1, Ttl74198.pinNrToPortNr(Ttl74198.Q0))
        .add(Port.OUTPUT, "Q1", 1, Ttl74198.pinNrToPortNr(Ttl74198.Q1))
        .add(Port.OUTPUT, "Q2", 1, Ttl74198.pinNrToPortNr(Ttl74198.Q2))
        .add(Port.OUTPUT, "Q3", 1, Ttl74198.pinNrToPortNr(Ttl74198.Q3))
        .add(Port.OUTPUT, "Q4", 1, Ttl74198.pinNrToPortNr(Ttl74198.Q4))
        .add(Port.OUTPUT, "Q5", 1, Ttl74198.pinNrToPortNr(Ttl74198.Q5))
        .add(Port.OUTPUT, "Q6", 1, Ttl74198.pinNrToPortNr(Ttl74198.Q6))
        .add(Port.OUTPUT, "Q7", 1, Ttl74198.pinNrToPortNr(Ttl74198.Q7));
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
                     D7&D6&D5&D4&D3&D2&D1&D0 {{when}} S1 = '1' {{and}} S0 = '1' {{else}}
                     curState(6 {{downto}} 0)&DSR {{when}} S1 = '0' {{and}} S0 = '1' {{else}}
                     DSL&curState(7 {{downto}} 1) {{when}} S1 = '1' {{and}} S0 = '0' {{else}}
                     curState;

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
        assign nextState = ({{tick}} == 0) ? curState :
                           (S1 == 1 && S0 == 1) ? {D7, D6, D5, D4, D3, D2, D1, D0} :
                           (S1 == 0 && S0 == 1) ? {curState[6:0], DSR} :
                           (S1 == 1 && S0 == 0) ? {DSL, curState[7:1]} :
                           curState;
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
