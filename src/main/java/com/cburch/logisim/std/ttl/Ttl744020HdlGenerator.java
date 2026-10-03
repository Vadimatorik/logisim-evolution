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

/** VHDL and Verilog generator for the 74x4020 14-stage binary ripple counter. */
public class Ttl744020HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for a falling-edge counter with an active-high reset. */
  public Ttl744020HdlGenerator() {
    super();
    myWires.addRegister("curState", 14);
    myPorts
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl744020.PORT_INDEX_CP)
        .add(Port.INPUT, "MR", 1, Ttl744020.PORT_INDEX_MR)
        .add(Port.OUTPUT, "Q1", 1, Ttl744020.PORT_INDEX_Q1)
        .add(Port.OUTPUT, "Q4", 1, Ttl744020.PORT_INDEX_Q4)
        .add(Port.OUTPUT, "Q5", 1, Ttl744020.PORT_INDEX_Q5)
        .add(Port.OUTPUT, "Q6", 1, Ttl744020.PORT_INDEX_Q6)
        .add(Port.OUTPUT, "Q7", 1, Ttl744020.PORT_INDEX_Q7)
        .add(Port.OUTPUT, "Q8", 1, Ttl744020.PORT_INDEX_Q8)
        .add(Port.OUTPUT, "Q9", 1, Ttl744020.PORT_INDEX_Q9)
        .add(Port.OUTPUT, "Q10", 1, Ttl744020.PORT_INDEX_Q10)
        .add(Port.OUTPUT, "Q11", 1, Ttl744020.PORT_INDEX_Q11)
        .add(Port.OUTPUT, "Q12", 1, Ttl744020.PORT_INDEX_Q12)
        .add(Port.OUTPUT, "Q13", 1, Ttl744020.PORT_INDEX_Q13)
        .add(Port.OUTPUT, "Q14", 1, Ttl744020.PORT_INDEX_Q14);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer().pair("CLK", HdlPorts.CLOCK).pair("tick", HdlPorts.TICK);
    if (Hdl.isVhdl()) {
      contents
          .empty()
          .addVhdlKeywords()
          .add(
              """
              Q1  <= curState(0);
              Q4  <= curState(3);
              Q5  <= curState(4);
              Q6  <= curState(5);
              Q7  <= curState(6);
              Q8  <= curState(7);
              Q9  <= curState(8);
              Q10 <= curState(9);
              Q11 <= curState(10);
              Q12 <= curState(11);
              Q13 <= curState(12);
              Q14 <= curState(13);

              counter : {{process}}({{CLK}}, MR) {{is}}
                 {{begin}}
                    {{if}} (MR = '1') {{then}}
                       curState <= ({{others}} => '0');
                    {{elsif}} (falling_edge({{CLK}})) {{then}}
                       {{if}} ({{tick}} = '1') {{then}}
                          curState <= std_logic_vector(unsigned(curState) + 1);
                       {{end}} {{if}};
                    {{end}} {{if}};
                 {{end}} {{process}} counter;
              """);
    } else {
      contents.add(
          """
          assign Q1  = curState[0];
          assign Q4  = curState[3];
          assign Q5  = curState[4];
          assign Q6  = curState[5];
          assign Q7  = curState[6];
          assign Q8  = curState[7];
          assign Q9  = curState[8];
          assign Q10 = curState[9];
          assign Q11 = curState[10];
          assign Q12 = curState[11];
          assign Q13 = curState[12];
          assign Q14 = curState[13];

          always @(negedge {{CLK}} or posedge MR)
          begin
             if (MR == 1) curState <= 0;
             else if ({{tick}} == 1) curState <= curState + 1;
          end
          """);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
