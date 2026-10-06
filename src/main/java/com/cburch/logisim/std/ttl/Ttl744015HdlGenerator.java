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

/** VHDL and Verilog for the two independent 4-bit registers of a 744015. */
public class Ttl744015HdlGenerator extends AbstractHdlGeneratorFactory {

  public Ttl744015HdlGenerator() {
    super();
    myWires
        .addRegister("state1", 4)
        .addRegister("state2", 4)
        .addWire("next1", 4)
        .addWire("next2", 4);
    myPorts
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, Ttl744015.PORT_INDEX_1CP)
        .add(Port.CLOCK, HdlPorts.getClockName(2), 2, Ttl744015.PORT_INDEX_2CP)
        .add(Port.INPUT, "D1", 1, Ttl744015.PORT_INDEX_1D)
        .add(Port.INPUT, "MR1", 1, Ttl744015.PORT_INDEX_1MR)
        .add(Port.INPUT, "D2", 1, Ttl744015.PORT_INDEX_2D)
        .add(Port.INPUT, "MR2", 1, Ttl744015.PORT_INDEX_2MR)
        .add(Port.OUTPUT, "Q10", 1, Ttl744015.PORT_INDEX_1Q0)
        .add(Port.OUTPUT, "Q11", 1, Ttl744015.PORT_INDEX_1Q1)
        .add(Port.OUTPUT, "Q12", 1, Ttl744015.PORT_INDEX_1Q2)
        .add(Port.OUTPUT, "Q13", 1, Ttl744015.PORT_INDEX_1Q3)
        .add(Port.OUTPUT, "Q20", 1, Ttl744015.PORT_INDEX_2Q0)
        .add(Port.OUTPUT, "Q21", 1, Ttl744015.PORT_INDEX_2Q1)
        .add(Port.OUTPUT, "Q22", 1, Ttl744015.PORT_INDEX_2Q2)
        .add(Port.OUTPUT, "Q23", 1, Ttl744015.PORT_INDEX_2Q3);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer()
            .pair("CLK1", HdlPorts.getClockName(1))
            .pair("CLK2", HdlPorts.getClockName(2))
            .pair("tick1", HdlPorts.getTickName(1))
            .pair("tick2", HdlPorts.getTickName(2));
    if (Hdl.isVhdl()) {
      contents
          .empty()
          .addVhdlKeywords()
          .add(
              """
              Q10 <= state1(0);
              Q11 <= state1(1);
              Q12 <= state1(2);
              Q13 <= state1(3);
              Q20 <= state2(0);
              Q21 <= state2(1);
              Q22 <= state2(2);
              Q23 <= state2(3);

              next1 <= state1(2 {{downto}} 0) & D1;
              next2 <= state2(2 {{downto}} 0) & D2;

              ff1 : {{process}} ( {{CLK1}} , MR1 ) {{is}}
                 BEGIN
                    {{if}} (MR1 = '1') {{then}} state1 <= "0000";
                    {{elsif}} (rising_edge({{CLK1}})) {{then}}
                       {{if}} ({{tick1}} = '1') {{then}} state1 <= next1; {{end}} {{if}};
                    {{end}} {{if}};
                 {{end}} {{process}} ff1;

              ff2 : {{process}} ( {{CLK2}} , MR2 ) {{is}}
                 BEGIN
                    {{if}} (MR2 = '1') {{then}} state2 <= "0000";
                    {{elsif}} (rising_edge({{CLK2}})) {{then}}
                       {{if}} ({{tick2}} = '1') {{then}} state2 <= next2; {{end}} {{if}};
                    {{end}} {{if}};
                 {{end}} {{process}} ff2;
              """);
    } else {
      contents.add(
          """
          assign Q10 = state1[0];
          assign Q11 = state1[1];
          assign Q12 = state1[2];
          assign Q13 = state1[3];
          assign Q20 = state2[0];
          assign Q21 = state2[1];
          assign Q22 = state2[2];
          assign Q23 = state2[3];

          assign next1 = {state1[2:0], D1};
          assign next2 = {state2[2:0], D2};

          always @(posedge {{CLK1}} or posedge MR1)
          begin
             if (MR1 == 1) state1 <= 4'b0000;
             else if ({{tick1}} == 1) state1 <= next1;
          end

          always @(posedge {{CLK2}} or posedge MR2)
          begin
             if (MR2 == 1) state2 <= 4'b0000;
             else if ({{tick2}} == 1) state2 <= next2;
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
