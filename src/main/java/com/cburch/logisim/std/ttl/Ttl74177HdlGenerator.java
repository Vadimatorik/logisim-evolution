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

/** VHDL and Verilog for the presettable binary counter/latch of a 74177. */
public class Ttl74177HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the 74177. Open active-low controls stay high. */
  public Ttl74177HdlGenerator() {
    super();
    myWires
        .addRegister("s_qa", 1)
        .addRegister("s_div8", 3)
        .addWire("s_next8", 3);
    // The width of a clock port selects its tick name, as in Ttl7476: 1 is tick, 2 is tick2.
    myPorts
        .add(Port.INPUT, "nLOAD", 1, Ttl74177.pinNrToPortNr(Ttl74177.LOAD), false)
        .add(Port.OUTPUT, "QC", 1, Ttl74177.pinNrToPortNr(Ttl74177.QC))
        .add(Port.INPUT, "C", 1, Ttl74177.pinNrToPortNr(Ttl74177.C), true)
        .add(Port.INPUT, "A", 1, Ttl74177.pinNrToPortNr(Ttl74177.A), true)
        .add(Port.OUTPUT, "QA", 1, Ttl74177.pinNrToPortNr(Ttl74177.QA))
        .add(Port.CLOCK, HdlPorts.getClockName(2), 2, Ttl74177.pinNrToPortNr(Ttl74177.CLK2), true)
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, Ttl74177.pinNrToPortNr(Ttl74177.CLK1), true)
        .add(Port.OUTPUT, "QB", 1, Ttl74177.pinNrToPortNr(Ttl74177.QB))
        .add(Port.INPUT, "B", 1, Ttl74177.pinNrToPortNr(Ttl74177.B), true)
        .add(Port.INPUT, "D", 1, Ttl74177.pinNrToPortNr(Ttl74177.D), true)
        .add(Port.OUTPUT, "QD", 1, Ttl74177.pinNrToPortNr(Ttl74177.QD))
        .add(Port.INPUT, "nCLR", 1, Ttl74177.pinNrToPortNr(Ttl74177.CLR), false);
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
      contents.addVhdlKeywords().add("""
          QA <= s_qa;
          QB <= s_div8(0);
          QC <= s_div8(1);
          QD <= s_div8(2);

          s_next8 <= "001" {{when}} s_div8 = "000" {{else}}
                     "010" {{when}} s_div8 = "001" {{else}}
                     "011" {{when}} s_div8 = "010" {{else}}
                     "100" {{when}} s_div8 = "011" {{else}}
                     "101" {{when}} s_div8 = "100" {{else}}
                     "110" {{when}} s_div8 = "101" {{else}}
                     "111" {{when}} s_div8 = "110" {{else}}
                     "000";

          div2 : {{process}} ({{CLK1}}, nCLR, nLOAD, A) {{is}}
             {{begin}}
                {{if}} (nCLR = '0') {{then}} s_qa <= '0';
                {{elsif}} (nLOAD = '0') {{then}} s_qa <= A;
                {{elsif}} (falling_edge({{CLK1}})) {{then}}
                   {{if}} ({{tick1}} = '1') {{then}} s_qa <= {{not}} s_qa; {{end}} {{if}};
                {{end}} {{if}};
             {{end}} {{process}} div2;

          div8 : {{process}} ({{CLK2}}, nCLR, nLOAD, B, C, D) {{is}}
             {{begin}}
                {{if}} (nCLR = '0') {{then}} s_div8 <= "000";
                {{elsif}} (nLOAD = '0') {{then}} s_div8 <= D & C & B;
                {{elsif}} (falling_edge({{CLK2}})) {{then}}
                   {{if}} ({{tick2}} = '1') {{then}} s_div8 <= s_next8; {{end}} {{if}};
                {{end}} {{if}};
             {{end}} {{process}} div8;
          """);
    } else {
      contents.add("""
          assign QA = s_qa;
          assign QB = s_div8[0];
          assign QC = s_div8[1];
          assign QD = s_div8[2];
          assign s_next8 = (s_div8 == 3'b000) ? 3'b001 :
                           (s_div8 == 3'b001) ? 3'b010 :
                           (s_div8 == 3'b010) ? 3'b011 :
                           (s_div8 == 3'b011) ? 3'b100 :
                           (s_div8 == 3'b100) ? 3'b101 :
                           (s_div8 == 3'b101) ? 3'b110 :
                           (s_div8 == 3'b110) ? 3'b111 : 3'b000;

          always @(negedge {{CLK1}} or negedge nCLR or nLOAD or A)
          begin
             if (nCLR == 0) s_qa <= 0;
             else if (nLOAD == 0) s_qa <= A;
             else if ({{tick1}} == 1) s_qa <= ~s_qa;
          end

          always @(negedge {{CLK2}} or negedge nCLR or nLOAD or B or C or D)
          begin
             if (nCLR == 0) s_div8 <= 3'b000;
             else if (nLOAD == 0) s_div8 <= {D, C, B};
             else if ({{tick2}} == 1) s_div8 <= s_next8;
          end
          """);
    }
    return contents;
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) {
      return false;
    }
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
