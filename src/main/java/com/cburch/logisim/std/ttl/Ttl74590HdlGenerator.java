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

/** VHDL and Verilog for the counter, output register and 3-state pins of a 74590. */
public class Ttl74590HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the 74590. Open active-low controls stay high. */
  public Ttl74590HdlGenerator() {
    super();
    myWires
        .addWire("s_next", 8)
        .addWire("s_load", 8)
        .addRegister("s_count", 8)
        .addRegister("s_reg", 8);
    // The width of a clock port is its tick index, matching the 7474. Active-low controls pull
    // high, so an open device neither resets, nor counts, nor drives the bus. Clocks pull low.
    myPorts
        .add(Port.OUTPUT, "Q1", 1, Ttl74590.pinNrToPortNr(Ttl74590.Q1))
        .add(Port.OUTPUT, "Q2", 1, Ttl74590.pinNrToPortNr(Ttl74590.Q2))
        .add(Port.OUTPUT, "Q3", 1, Ttl74590.pinNrToPortNr(Ttl74590.Q3))
        .add(Port.OUTPUT, "Q4", 1, Ttl74590.pinNrToPortNr(Ttl74590.Q4))
        .add(Port.OUTPUT, "Q5", 1, Ttl74590.pinNrToPortNr(Ttl74590.Q5))
        .add(Port.OUTPUT, "Q6", 1, Ttl74590.pinNrToPortNr(Ttl74590.Q6))
        .add(Port.OUTPUT, "Q7", 1, Ttl74590.pinNrToPortNr(Ttl74590.Q7))
        .add(Port.OUTPUT, "RCO", 1, Ttl74590.pinNrToPortNr(Ttl74590.RCO))
        .add(Port.INPUT, "MRC", 1, Ttl74590.pinNrToPortNr(Ttl74590.MRC), false)
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, Ttl74590.pinNrToPortNr(Ttl74590.CPC), true)
        .add(Port.INPUT, "CE", 1, Ttl74590.pinNrToPortNr(Ttl74590.CE), false)
        .add(Port.CLOCK, HdlPorts.getClockName(2), 2, Ttl74590.pinNrToPortNr(Ttl74590.CPR), true)
        .add(Port.INPUT, "OE", 1, Ttl74590.pinNrToPortNr(Ttl74590.OE), false)
        .add(Port.OUTPUT, "Q0", 1, Ttl74590.pinNrToPortNr(Ttl74590.Q0));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer()
            .pair("CLK1", HdlPorts.getClockName(1))
            .pair("CLK2", HdlPorts.getClockName(2))
            .pair("tick1", HdlPorts.getTickName(1))
            .pair("tick2", HdlPorts.getTickName(2));
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords();
      addVhdl(contents);
    } else {
      addVerilog(contents);
    }
    return contents.empty();
  }

  private static void addVhdl(LineBuffer contents) {
    contents.add(
        """
        s_next <= s_count {{when}} {{tick1}} = '0' {{else}}
                  std_logic_vector(unsigned(s_count) + 1) {{when}} CE = '0' {{else}}
                  s_count;
        s_load <= s_count {{when}} {{tick2}} = '1' {{else}} s_reg;
        RCO <= '0' {{when}} s_count = "11111111" {{else}} '1';
        Q0 <= s_reg(0) {{when}} OE = '0' {{else}} 'Z';
        Q1 <= s_reg(1) {{when}} OE = '0' {{else}} 'Z';
        Q2 <= s_reg(2) {{when}} OE = '0' {{else}} 'Z';
        Q3 <= s_reg(3) {{when}} OE = '0' {{else}} 'Z';
        Q4 <= s_reg(4) {{when}} OE = '0' {{else}} 'Z';
        Q5 <= s_reg(5) {{when}} OE = '0' {{else}} 'Z';
        Q6 <= s_reg(6) {{when}} OE = '0' {{else}} 'Z';
        Q7 <= s_reg(7) {{when}} OE = '0' {{else}} 'Z';

        counter : {{process}}({{CLK1}}, MRC) {{is}}
        {{begin}}
           {{if}} (MRC = '0') {{then}}
              s_count <= "00000000";
           {{elsif}} (rising_edge({{CLK1}})) {{then}}
              s_count <= s_next;
           {{end}} {{if}};
        {{end}} {{process}} counter;

        storage : {{process}}({{CLK2}}) {{is}}
        {{begin}}
           {{if}} (rising_edge({{CLK2}})) {{then}}
              s_reg <= s_load;
           {{end}} {{if}};
        {{end}} {{process}} storage;
        """);
  }

  private static void addVerilog(LineBuffer contents) {
    contents.add(
        """
        assign s_next = ({{tick1}} == 0) ? s_count : (CE == 0) ? (s_count + 1) : s_count;
        assign s_load = ({{tick2}} == 1) ? s_count : s_reg;
        assign RCO = (s_count != 8'b11111111);
        assign Q0 = (OE == 0) ? s_reg[0] : 1'bZ;
        assign Q1 = (OE == 0) ? s_reg[1] : 1'bZ;
        assign Q2 = (OE == 0) ? s_reg[2] : 1'bZ;
        assign Q3 = (OE == 0) ? s_reg[3] : 1'bZ;
        assign Q4 = (OE == 0) ? s_reg[4] : 1'bZ;
        assign Q5 = (OE == 0) ? s_reg[5] : 1'bZ;
        assign Q6 = (OE == 0) ? s_reg[6] : 1'bZ;
        assign Q7 = (OE == 0) ? s_reg[7] : 1'bZ;

        always @(posedge {{CLK1}} or negedge MRC)
        begin
           if (MRC == 0) s_count <= 8'b00000000;
           else s_count <= s_next;
        end

        always @(posedge {{CLK2}})
        begin
           s_reg <= s_load;
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
