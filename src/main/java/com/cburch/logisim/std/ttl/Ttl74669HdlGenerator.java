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

/** HDL for the synchronous 4-bit up/down counter in {@link Ttl74669}. */
public class Ttl74669HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates the port and signal map for a binary 74669 model. */
  public Ttl74669HdlGenerator() {
    super();
    myWires
        .addRegister("curState", 4)
        .addWire("nextState", 4)
        .addWire("loaded", 4)
        .addWire("upCount", 4)
        .addWire("downCount", 4)
        .addWire("counting", 1);
    myPorts
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl74669.PORT_INDEX_CK)
        .add(Port.INPUT, "UD", 1, Ttl74669.PORT_INDEX_UD)
        .add(Port.INPUT, "A", 1, Ttl74669.PORT_INDEX_A)
        .add(Port.INPUT, "B", 1, Ttl74669.PORT_INDEX_B)
        .add(Port.INPUT, "C", 1, Ttl74669.PORT_INDEX_C)
        .add(Port.INPUT, "D", 1, Ttl74669.PORT_INDEX_D)
        .add(Port.INPUT, "ENP", 1, Ttl74669.PORT_INDEX_ENP)
        .add(Port.INPUT, "LOAD", 1, Ttl74669.PORT_INDEX_LOAD)
        .add(Port.INPUT, "ENT", 1, Ttl74669.PORT_INDEX_ENT)
        .add(Port.OUTPUT, "QA", 1, Ttl74669.PORT_INDEX_QA)
        .add(Port.OUTPUT, "QB", 1, Ttl74669.PORT_INDEX_QB)
        .add(Port.OUTPUT, "QC", 1, Ttl74669.PORT_INDEX_QC)
        .add(Port.OUTPUT, "QD", 1, Ttl74669.PORT_INDEX_QD)
        .add(Port.OUTPUT, "RCO", 1, Ttl74669.PORT_INDEX_RCO);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist netlist, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer().pair("CLK", HdlPorts.CLOCK).pair("tick", HdlPorts.TICK);
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          loaded <= D&C&B&A;
          upCount <= std_logic_vector(unsigned(curState) + 1);
          downCount <= std_logic_vector(unsigned(curState) - 1);
          counting <= {{not}}(ENP) {{and}} {{not}}(ENT) {{and}} LOAD;

          nextState <= curState {{when}} {{tick}} = '0' {{else}}
                       loaded {{when}} LOAD = '0' {{else}}
                       curState {{when}} counting = '0' {{else}}
                       upCount {{when}} UD = '1' {{else}}
                       downCount;

          dffs : {{process}}({{CLK}}) {{is}}
          {{begin}}
             {{if}} (rising_edge({{CLK}})) {{then}}
                curState <= nextState;
             {{end}} {{if}};
          {{end}} {{process}} dffs;

          QA <= curState(0);
          QB <= curState(1);
          QC <= curState(2);
          QD <= curState(3);
          RCO <= '0' {{when}} ENT = '0' {{and}} UD = '1' {{and}} curState = "1111"
                 {{else}} '0' {{when}} ENT = '0' {{and}} UD = '0' {{and}} curState = "0000"
                 {{else}} '1';
          """);
    } else {
      contents.add("""
          assign loaded = {D, C, B, A};
          assign upCount = curState + 4'b0001;
          assign downCount = curState - 4'b0001;
          assign counting = ~ENP & ~ENT & LOAD;
          assign nextState = {{tick}} == 0 ? curState :
                             LOAD == 0 ? loaded :
                             counting == 0 ? curState :
                             UD == 1 ? upCount : downCount;
          assign QA = curState[0];
          assign QB = curState[1];
          assign QC = curState[2];
          assign QD = curState[3];
          assign RCO = (ENT == 0 && UD == 1 && curState == 4'b1111) ? 1'b0 :
                       (ENT == 0 && UD == 0 && curState == 4'b0000) ? 1'b0 : 1'b1;

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
