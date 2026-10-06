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

/** VHDL and Verilog for the asynchronous reset, asynchronous load and binary count of a 744516. */
public class Ttl744516HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the 744516. Open count-enable stays high, so the counter holds. */
  public Ttl744516HdlGenerator() {
    super();
    myWires.addWire("s_count", 4).addWire("s_data", 4);
    myPorts
        .add(Port.INPUT, "PL", 1, Ttl744516.pinNrToPortNr(Ttl744516.PL), true)
        .add(Port.OUTPUT, "Q3", 1, Ttl744516.pinNrToPortNr(Ttl744516.Q3))
        .add(Port.INPUT, "D3", 1, Ttl744516.pinNrToPortNr(Ttl744516.D3), true)
        .add(Port.INPUT, "D0", 1, Ttl744516.pinNrToPortNr(Ttl744516.D0), true)
        .add(Port.INPUT, "CE", 1, Ttl744516.pinNrToPortNr(Ttl744516.CE), false)
        .add(Port.OUTPUT, "Q0", 1, Ttl744516.pinNrToPortNr(Ttl744516.Q0))
        .add(Port.OUTPUT, "TC", 1, Ttl744516.pinNrToPortNr(Ttl744516.TC))
        .add(Port.INPUT, "MR", 1, Ttl744516.pinNrToPortNr(Ttl744516.MR), true)
        .add(Port.INPUT, "UP_DN", 1, Ttl744516.pinNrToPortNr(Ttl744516.UPDN), false)
        .add(Port.OUTPUT, "Q1", 1, Ttl744516.pinNrToPortNr(Ttl744516.Q1))
        .add(Port.INPUT, "D1", 1, Ttl744516.pinNrToPortNr(Ttl744516.D1), true)
        .add(Port.INPUT, "D2", 1, Ttl744516.pinNrToPortNr(Ttl744516.D2), true)
        .add(Port.OUTPUT, "Q2", 1, Ttl744516.pinNrToPortNr(Ttl744516.Q2))
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl744516.pinNrToPortNr(Ttl744516.CP), true);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer().pair("CLK", HdlPorts.CLOCK).pair("tick", HdlPorts.TICK);
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords();
      addVhdl(contents);
    } else {
      addVerilog(contents);
    }
    return contents.empty();
  }

  private static void addVhdl(LineBuffer contents) {
    contents.add(
        """
        s_data <= D3 & D2 & D1 & D0;

        count : {{process}}({{CLK}}, MR, PL, D0, D1, D2, D3) {{is}}
        {{begin}}
           {{if}} (MR = '1') {{then}}
              s_count <= "0000";
           {{elsif}} (PL = '1') {{then}}
              s_count <= s_data;
           {{elsif}} (rising_edge({{CLK}})) {{then}}
              {{if}} ({{tick}} = '1' {{and}} CE = '0') {{then}}
                 {{if}} (UP_DN = '1') {{then}}
                    s_count <= std_logic_vector(unsigned(s_count) + 1);
                 {{else}}
                    s_count <= std_logic_vector(unsigned(s_count) - 1);
                 {{end}} {{if}};
              {{end}} {{if}};
           {{end}} {{if}};
        {{end}} {{process}} count;

        TC <= '0' {{when}} CE = '0' {{and}} UP_DN = '1' {{and}} s_count = "1111" {{else}}
              '0' {{when}} CE = '0' {{and}} UP_DN = '0' {{and}} s_count = "0000" {{else}}
              '1';
        Q0 <= s_count(0);
        Q1 <= s_count(1);
        Q2 <= s_count(2);
        Q3 <= s_count(3);
        """);
  }

  private static void addVerilog(LineBuffer contents) {
    contents.add(
        """
        assign s_data = {D3, D2, D1, D0};
        assign Q0 = s_count[0];
        assign Q1 = s_count[1];
        assign Q2 = s_count[2];
        assign Q3 = s_count[3];
        assign TC = ((CE == 0) && (UP_DN == 1) && (s_count == 4'b1111)) ? 1'b0 :
                    ((CE == 0) && (UP_DN == 0) && (s_count == 4'b0000)) ? 1'b0 : 1'b1;

        always @(posedge {{CLK}})
        begin
           if (({{tick}} == 1) && (MR == 0) && (PL == 0) && (CE == 0))
              s_count <= (UP_DN == 1) ? (s_count + 4'b0001) : (s_count - 4'b0001);
        end

        always @(MR or PL or D0 or D1 or D2 or D3)
        begin
           if (MR == 1)
              s_count <= 4'b0000;
           else if (PL == 1)
              s_count <= s_data;
        end
        """);
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) {
      return false;
    }
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
