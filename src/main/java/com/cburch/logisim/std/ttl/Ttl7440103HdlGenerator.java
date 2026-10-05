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

/** VHDL and Verilog for the 7440103 down counter, including asynchronous reset and preset. */
public class Ttl7440103HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the 7440103. Open active-low controls stay high. */
  public Ttl7440103HdlGenerator() {
    super();
    myWires.addWire("s_data", 8).addWire("s_next", 8).addRegister("s_count", 8);
    // MR, PL, PE and TE pull high, so an open counter neither resets, presets, nor counts.
    // Jam inputs pull low.
    myPorts
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl7440103.pinNrToPortNr(Ttl7440103.CP), true)
        .add(Port.INPUT, "MR", 1, Ttl7440103.pinNrToPortNr(Ttl7440103.MR), false)
        .add(Port.INPUT, "TE", 1, Ttl7440103.pinNrToPortNr(Ttl7440103.TE), false)
        .add(Port.INPUT, "P0", 1, Ttl7440103.pinNrToPortNr(Ttl7440103.P0), true)
        .add(Port.INPUT, "P1", 1, Ttl7440103.pinNrToPortNr(Ttl7440103.P1), true)
        .add(Port.INPUT, "P2", 1, Ttl7440103.pinNrToPortNr(Ttl7440103.P2), true)
        .add(Port.INPUT, "P3", 1, Ttl7440103.pinNrToPortNr(Ttl7440103.P3), true)
        .add(Port.INPUT, "PL", 1, Ttl7440103.pinNrToPortNr(Ttl7440103.PL), false)
        .add(Port.INPUT, "P4", 1, Ttl7440103.pinNrToPortNr(Ttl7440103.P4), true)
        .add(Port.INPUT, "P5", 1, Ttl7440103.pinNrToPortNr(Ttl7440103.P5), true)
        .add(Port.INPUT, "P6", 1, Ttl7440103.pinNrToPortNr(Ttl7440103.P6), true)
        .add(Port.INPUT, "P7", 1, Ttl7440103.pinNrToPortNr(Ttl7440103.P7), true)
        .add(Port.OUTPUT, "TC", 1, Ttl7440103.pinNrToPortNr(Ttl7440103.TC))
        .add(Port.INPUT, "PE", 1, Ttl7440103.pinNrToPortNr(Ttl7440103.PE), false);
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
        s_data <= P7 & P6 & P5 & P4 & P3 & P2 & P1 & P0;
        s_next <= s_count {{when}} {{tick}} = '0' {{else}}
                  s_data {{when}} PE = '0' {{else}}
                  std_logic_vector(unsigned(s_count) - 1) {{when}} TE = '0' {{else}}
                  s_count;
        TC <= '0' {{when}} TE = '0' {{and}} s_count = "00000000" {{else}} '1';

        count : {{process}}({{CLK}}, MR, PL, s_data) {{is}}
        {{begin}}
           {{if}} (MR = '0') {{then}}
              s_count <= ({{others}} => '1');
           {{elsif}} (PL = '0') {{then}}
              s_count <= s_data;
           {{elsif}} (rising_edge({{CLK}})) {{then}}
              s_count <= s_next;
           {{end}} {{if}};
        {{end}} {{process}} count;
        """);
  }

  private static void addVerilog(LineBuffer contents) {
    contents.add(
        """
        assign s_data = {P7, P6, P5, P4, P3, P2, P1, P0};
        assign s_next = ({{tick}} == 0) ? s_count :
                        (PE == 0) ? s_data :
                        (TE == 0) ? (s_count - 8'b00000001) :
                        s_count;
        assign TC = ((TE == 0) && (s_count == 8'b00000000)) ? 1'b0 : 1'b1;

        always @(posedge {{CLK}} or negedge MR or negedge PL or s_data)
        begin
           if (MR == 0) s_count <= 8'b11111111;
           else if (PL == 0) s_count <= s_data;
           else s_count <= s_next;
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
