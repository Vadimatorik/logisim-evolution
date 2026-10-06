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

public class Ttl74174HdlGenerator extends AbstractHdlGeneratorFactory {

  public Ttl74174HdlGenerator() {
    super();
    myWires
        .addWire("curState", 6)
        .addWire("nextState", 6);
    myPorts
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl74174.PORT_INDEX_CLK)
        .add(Port.INPUT, "nCLR", 1, Ttl74174.PORT_INDEX_nCLR)
        .add(Port.INPUT, "D1", 1, Ttl74174.PORT_INDEX_D1)
        .add(Port.INPUT, "D2", 1, Ttl74174.PORT_INDEX_D2)
        .add(Port.INPUT, "D3", 1, Ttl74174.PORT_INDEX_D3)
        .add(Port.INPUT, "D4", 1, Ttl74174.PORT_INDEX_D4)
        .add(Port.INPUT, "D5", 1, Ttl74174.PORT_INDEX_D5)
        .add(Port.INPUT, "D6", 1, Ttl74174.PORT_INDEX_D6)
        .add(Port.OUTPUT, "Q1", 1, Ttl74174.PORT_INDEX_Q1)
        .add(Port.OUTPUT, "Q2", 1, Ttl74174.PORT_INDEX_Q2)
        .add(Port.OUTPUT, "Q3", 1, Ttl74174.PORT_INDEX_Q3)
        .add(Port.OUTPUT, "Q4", 1, Ttl74174.PORT_INDEX_Q4)
        .add(Port.OUTPUT, "Q5", 1, Ttl74174.PORT_INDEX_Q5)
        .add(Port.OUTPUT, "Q6", 1, Ttl74174.PORT_INDEX_Q6);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
        .pair("CLK", HdlPorts.CLOCK)
        .pair("tick", HdlPorts.TICK);
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
            nextState <= curState {{when}} {{tick}} = '0' {{else}}
                         D6&D5&D4&D3&D2&D1;

            dffs : {{process}}({{CLK}}, nCLR) {{is}}
               {{begin}}
                  {{if}} (nCLR = '0') {{then}} curState <= "000000";
                  {{elsif}} (rising_edge({{CLK}})) {{then}}
                     curState <= nextState;
                  {{end}} {{if}};
               {{end}} {{process}} dffs;

            Q1 <= curState(0);
            Q2 <= curState(1);
            Q3 <= curState(2);
            Q4 <= curState(3);
            Q5 <= curState(4);
            Q6 <= curState(5);
            """);
    } else {
      contents.add("""
          assign nextState = tick == 0 ? curState : {D6, D5, D4, D3, D2, D1};
          assign Q1        = curState[0];
          assign Q2        = curState[1];
          assign Q3        = curState[2];
          assign Q4        = curState[3];
          assign Q5        = curState[4];
          assign Q6        = curState[5];

          always @(posedge {{CLK}} or negedge nCLR)
          begin
             if (~nCLR) curState <= 0;
             else curState <= nextState;
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
