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
 * VHDL and Verilog for one 74HC90. {@code CKA} toggles {@code QA} on its falling edge. {@code CKB}
 * advances the divide-by-five section on its falling edge. Both {@code R9} inputs set the outputs
 * to 9, and that mode overrides a reset from both {@code R0} inputs.
 */
public class Ttl7490HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates a generator for the decade ripple counter. */
  public Ttl7490HdlGenerator() {
    super();
    myWires
        .addRegister("stateA", 1)
        .addRegister("stateB", 3);
    // The width of a clock port selects its tick name, as in Ttl7476: 1 is tick, 2 is tick2.
    myPorts
        .add(Port.CLOCK, HdlPorts.getClockName(2), 2, Ttl7490.PORT_INDEX_CKB)
        .add(Port.INPUT, "R01", 1, Ttl7490.PORT_INDEX_R0_1)
        .add(Port.INPUT, "R02", 1, Ttl7490.PORT_INDEX_R0_2)
        .add(Port.INPUT, "R91", 1, Ttl7490.PORT_INDEX_R9_1)
        .add(Port.INPUT, "R92", 1, Ttl7490.PORT_INDEX_R9_2)
        .add(Port.OUTPUT, "QC", 1, Ttl7490.PORT_INDEX_QC)
        .add(Port.OUTPUT, "QB", 1, Ttl7490.PORT_INDEX_QB)
        .add(Port.OUTPUT, "QD", 1, Ttl7490.PORT_INDEX_QD)
        .add(Port.OUTPUT, "QA", 1, Ttl7490.PORT_INDEX_QA)
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, Ttl7490.PORT_INDEX_CKA);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer()
            .pair("CLKA", HdlPorts.getClockName(1))
            .pair("CLKB", HdlPorts.getClockName(2))
            .pair("tickA", HdlPorts.getTickName(1))
            .pair("tickB", HdlPorts.getTickName(2));
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
          QA <= stateA;
          QB <= stateB(0);
          QC <= stateB(1);
          QD <= stateB(2);

          nextB <= "001" {{when}} stateB = "000" {{else}}
                   "010" {{when}} stateB = "001" {{else}}
                   "011" {{when}} stateB = "010" {{else}}
                   "100" {{when}} stateB = "011" {{else}}
                   "000";

          divideByTwo : {{process}}({{CLKA}}, R01, R02, R91, R92) {{is}}
             {{begin}}
                {{if}} (R91 = '1' {{and}} R92 = '1') {{then}} stateA <= '1';
                {{elsif}} (R01 = '1' {{and}} R02 = '1') {{then}} stateA <= '0';
                {{elsif}} (falling_edge({{CLKA}})) {{then}}
                   {{if}} ({{tickA}} = '1') {{then}} stateA <= {{not}} stateA;
                   {{end}} {{if}};
                {{end}} {{if}};
             {{end}} {{process}} divideByTwo;

          divideByFive : {{process}}({{CLKB}}, R01, R02, R91, R92) {{is}}
             {{begin}}
                {{if}} (R91 = '1' {{and}} R92 = '1') {{then}} stateB <= "100";
                {{elsif}} (R01 = '1' {{and}} R02 = '1') {{then}} stateB <= "000";
                {{elsif}} (falling_edge({{CLKB}})) {{then}}
                   {{if}} ({{tickB}} = '1') {{then}} stateB <= nextB;
                   {{end}} {{if}};
                {{end}} {{if}};
             {{end}} {{process}} divideByFive;
          """);
    } else {
      contents.add("""
          assign QA = stateA;
          assign QB = stateB[0];
          assign QC = stateB[1];
          assign QD = stateB[2];

          assign nextB = (stateB == 3'b000) ? 3'b001 :
                         (stateB == 3'b001) ? 3'b010 :
                         (stateB == 3'b010) ? 3'b011 :
                         (stateB == 3'b011) ? 3'b100 :
                                              3'b000;

          always @(negedge {{CLKA}} or posedge R01 or posedge R02
                   or posedge R91 or negedge R91 or posedge R92 or negedge R92)
          begin
             if (R91 == 1 && R92 == 1) stateA <= 1;
             else if (R01 == 1 && R02 == 1) stateA <= 0;
             else if ({{tickA}} == 1) stateA <= ~stateA;
          end

          always @(negedge {{CLKB}} or posedge R01 or posedge R02
                   or posedge R91 or negedge R91 or posedge R92 or negedge R92)
          begin
             if (R91 == 1 && R92 == 1) stateB <= 3'b100;
             else if (R01 == 1 && R02 == 1) stateB <= 0;
             else if ({{tickB}} == 1) stateB <= nextB;
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
