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

/** HDL for the synchronous 4-bit up/down counter in {@link Ttl74169}. */
public class Ttl74169HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates the port and signal map for a binary 74169 model. */
  public Ttl74169HdlGenerator() {
    super();
    myWires
        .addRegister("curState", 4)
        .addWire("nextState", 4)
        .addWire("loaded", 4)
        .addWire("upCount", 4)
        .addWire("downCount", 4)
        .addWire("counting", 1);
    myPorts
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl74169.PORT_INDEX_CP)
        .add(Port.INPUT, "UD", 1, Ttl74169.PORT_INDEX_UD)
        .add(Port.INPUT, "D0", 1, Ttl74169.PORT_INDEX_D0)
        .add(Port.INPUT, "D1", 1, Ttl74169.PORT_INDEX_D1)
        .add(Port.INPUT, "D2", 1, Ttl74169.PORT_INDEX_D2)
        .add(Port.INPUT, "D3", 1, Ttl74169.PORT_INDEX_D3)
        .add(Port.INPUT, "CEP", 1, Ttl74169.PORT_INDEX_CEP)
        .add(Port.INPUT, "PE", 1, Ttl74169.PORT_INDEX_PE)
        .add(Port.INPUT, "CET", 1, Ttl74169.PORT_INDEX_CET)
        .add(Port.OUTPUT, "Q0", 1, Ttl74169.PORT_INDEX_Q0)
        .add(Port.OUTPUT, "Q1", 1, Ttl74169.PORT_INDEX_Q1)
        .add(Port.OUTPUT, "Q2", 1, Ttl74169.PORT_INDEX_Q2)
        .add(Port.OUTPUT, "Q3", 1, Ttl74169.PORT_INDEX_Q3)
        .add(Port.OUTPUT, "TC", 1, Ttl74169.PORT_INDEX_TC);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist netlist, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer().pair("CLK", HdlPorts.CLOCK).pair("tick", HdlPorts.TICK);
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          loaded <= D3&D2&D1&D0;
          upCount <= std_logic_vector(unsigned(curState) + 1);
          downCount <= std_logic_vector(unsigned(curState) - 1);
          counting <= {{not}}(CEP) {{and}} {{not}}(CET) {{and}} PE;

          nextState <= curState {{when}} {{tick}} = '0' {{else}}
                       loaded {{when}} PE = '0' {{else}}
                       curState {{when}} counting = '0' {{else}}
                       upCount {{when}} UD = '1' {{else}}
                       downCount;

          dffs : {{process}}({{CLK}}) {{is}}
          {{begin}}
             {{if}} (rising_edge({{CLK}})) {{then}}
                curState <= nextState;
             {{end}} {{if}};
          {{end}} {{process}} dffs;

          Q0 <= curState(0);
          Q1 <= curState(1);
          Q2 <= curState(2);
          Q3 <= curState(3);
          TC <= '0' {{when}} CET = '0' {{and}} UD = '1' {{and}} curState = "1111"
                {{else}} '0' {{when}} CET = '0' {{and}} UD = '0' {{and}} curState = "0000"
                {{else}} '1';
          """);
    } else {
      contents.add("""
          assign loaded = {D3, D2, D1, D0};
          assign upCount = curState + 4'b0001;
          assign downCount = curState - 4'b0001;
          assign counting = ~CEP & ~CET & PE;
          assign nextState = {{tick}} == 0 ? curState :
                             PE == 0 ? loaded :
                             counting == 0 ? curState :
                             UD == 1 ? upCount : downCount;
          assign Q0 = curState[0];
          assign Q1 = curState[1];
          assign Q2 = curState[2];
          assign Q3 = curState[3];
          assign TC = (CET == 0 && UD == 1 && curState == 4'b1111) ? 1'b0 :
                      (CET == 0 && UD == 0 && curState == 4'b0000) ? 1'b0 : 1'b1;

          always @(posedge {{CLK}})
          begin
             curState <= nextState;
          end
          """);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
