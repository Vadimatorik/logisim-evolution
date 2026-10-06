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

/**
 * VHDL and Verilog for one 74HC290. {@code CP0} toggles {@code Q0} on its falling edge. {@code
 * CP1} advances the divide-by-five section on its falling edge. Both {@code MS} inputs set the
 * outputs to 9, and that mode overrides a reset from both {@code MR} inputs.
 */
public class Ttl74290HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates a generator for the decade ripple counter. */
  public Ttl74290HdlGenerator() {
    super();
    myWires
        .addRegister("stateA", 1)
        .addRegister("stateB", 3)
        .addWire("nextB", 3);
    // The width of a clock port selects its tick name, as in Ttl7476: 1 is tick, 2 is tick2.
    myPorts
        .add(Port.CLOCK, HdlPorts.getClockName(2), 2, Ttl74290.PORT_INDEX_CP1)
        .add(Port.INPUT, "MR1", 1, Ttl74290.PORT_INDEX_MR1)
        .add(Port.INPUT, "MR2", 1, Ttl74290.PORT_INDEX_MR2)
        .add(Port.INPUT, "MS1", 1, Ttl74290.PORT_INDEX_MS1)
        .add(Port.INPUT, "MS2", 1, Ttl74290.PORT_INDEX_MS2)
        .add(Port.OUTPUT, "Q2", 1, Ttl74290.PORT_INDEX_Q2)
        .add(Port.OUTPUT, "Q1", 1, Ttl74290.PORT_INDEX_Q1)
        .add(Port.OUTPUT, "Q3", 1, Ttl74290.PORT_INDEX_Q3)
        .add(Port.OUTPUT, "Q0", 1, Ttl74290.PORT_INDEX_Q0)
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, Ttl74290.PORT_INDEX_CP0);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer()
            .pair("CLK0", HdlPorts.getClockName(1))
            .pair("CLK1", HdlPorts.getClockName(2))
            .pair("tick0", HdlPorts.getTickName(1))
            .pair("tick1", HdlPorts.getTickName(2));
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
          Q0 <= stateA;
          Q1 <= stateB(0);
          Q2 <= stateB(1);
          Q3 <= stateB(2);

          nextB <= "001" {{when}} stateB = "000" {{else}}
                   "010" {{when}} stateB = "001" {{else}}
                   "011" {{when}} stateB = "010" {{else}}
                   "100" {{when}} stateB = "011" {{else}}
                   "000";

          divideByTwo : {{process}}({{CLK0}}, MR1, MR2, MS1, MS2) {{is}}
             {{begin}}
                {{if}} (MS1 = '1' {{and}} MS2 = '1') {{then}} stateA <= '1';
                {{elsif}} (MR1 = '1' {{and}} MR2 = '1') {{then}} stateA <= '0';
                {{elsif}} (falling_edge({{CLK0}})) {{then}}
                   {{if}} ({{tick0}} = '1') {{then}} stateA <= {{not}} stateA;
                   {{end}} {{if}};
                {{end}} {{if}};
             {{end}} {{process}} divideByTwo;

          divideByFive : {{process}}({{CLK1}}, MR1, MR2, MS1, MS2) {{is}}
             {{begin}}
                {{if}} (MS1 = '1' {{and}} MS2 = '1') {{then}} stateB <= "100";
                {{elsif}} (MR1 = '1' {{and}} MR2 = '1') {{then}} stateB <= "000";
                {{elsif}} (falling_edge({{CLK1}})) {{then}}
                   {{if}} ({{tick1}} = '1') {{then}} stateB <= nextB;
                   {{end}} {{if}};
                {{end}} {{if}};
             {{end}} {{process}} divideByFive;
          """);
    } else {
      contents.add("""
          assign Q0 = stateA;
          assign Q1 = stateB[0];
          assign Q2 = stateB[1];
          assign Q3 = stateB[2];

          assign nextB = (stateB == 3'b000) ? 3'b001 :
                         (stateB == 3'b001) ? 3'b010 :
                         (stateB == 3'b010) ? 3'b011 :
                         (stateB == 3'b011) ? 3'b100 :
                                              3'b000;

          always @(negedge {{CLK0}} or posedge MR1 or posedge MR2
                   or posedge MS1 or negedge MS1 or posedge MS2 or negedge MS2)
          begin
             if (MS1 == 1 && MS2 == 1) stateA <= 1;
             else if (MR1 == 1 && MR2 == 1) stateA <= 0;
             else if ({{tick0}} == 1) stateA <= ~stateA;
          end

          always @(negedge {{CLK1}} or posedge MR1 or posedge MR2
                   or posedge MS1 or negedge MS1 or posedge MS2 or negedge MS2)
          begin
             if (MS1 == 1 && MS2 == 1) stateB <= 3'b100;
             else if (MR1 == 1 && MR2 == 1) stateB <= 0;
             else if ({{tick1}} == 1) stateB <= nextB;
          end
          """);
    }
    return contents;
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
