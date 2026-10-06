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

/** VHDL and Verilog generator for the 74x4024 7-stage binary ripple counter. */
public class Ttl744024HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for a falling-edge counter with an active-high reset. */
  public Ttl744024HdlGenerator() {
    super();
    myWires.addRegister("curState", 7);
    myPorts
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl744024.PORT_INDEX_CP)
        .add(Port.INPUT, "MR", 1, Ttl744024.PORT_INDEX_MR)
        .add(Port.OUTPUT, "Q1", 1, Ttl744024.PORT_INDEX_Q1)
        .add(Port.OUTPUT, "Q2", 1, Ttl744024.PORT_INDEX_Q2)
        .add(Port.OUTPUT, "Q3", 1, Ttl744024.PORT_INDEX_Q3)
        .add(Port.OUTPUT, "Q4", 1, Ttl744024.PORT_INDEX_Q4)
        .add(Port.OUTPUT, "Q5", 1, Ttl744024.PORT_INDEX_Q5)
        .add(Port.OUTPUT, "Q6", 1, Ttl744024.PORT_INDEX_Q6)
        .add(Port.OUTPUT, "Q7", 1, Ttl744024.PORT_INDEX_Q7);
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
              Q1 <= curState(0);
              Q2 <= curState(1);
              Q3 <= curState(2);
              Q4 <= curState(3);
              Q5 <= curState(4);
              Q6 <= curState(5);
              Q7 <= curState(6);

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
          assign Q1 = curState[0];
          assign Q2 = curState[1];
          assign Q3 = curState[2];
          assign Q4 = curState[3];
          assign Q5 = curState[4];
          assign Q6 = curState[5];
          assign Q7 = curState[6];

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
