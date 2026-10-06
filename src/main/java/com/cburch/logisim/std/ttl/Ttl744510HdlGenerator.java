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

/** VHDL and Verilog for the asynchronous reset, asynchronous load and BCD count of a 744510. */
public class Ttl744510HdlGenerator extends AbstractHdlGeneratorFactory {

  /**
   * Creates an HDL generator for the 744510. Open count-enable stays high, so the counter holds.
   */
  public Ttl744510HdlGenerator() {
    super();
    myWires
        .addWire("s_data", 4)
        .addWire("s_up", 4)
        .addWire("s_down", 4)
        .addRegister("s_count", 4);
    // MR and PL pull low, so an open counter neither resets nor loads. CE pulls high, so an open
    // counter holds. UP/DN pulls high. Data pins pull low.
    myPorts
        .add(Port.INPUT, "PL", 1, Ttl744510.pinNrToPortNr(Ttl744510.PL), true)
        .add(Port.OUTPUT, "Q3", 1, Ttl744510.pinNrToPortNr(Ttl744510.Q3))
        .add(Port.INPUT, "D3", 1, Ttl744510.pinNrToPortNr(Ttl744510.D3), true)
        .add(Port.INPUT, "D0", 1, Ttl744510.pinNrToPortNr(Ttl744510.D0), true)
        .add(Port.INPUT, "CE", 1, Ttl744510.pinNrToPortNr(Ttl744510.CE), false)
        .add(Port.OUTPUT, "Q0", 1, Ttl744510.pinNrToPortNr(Ttl744510.Q0))
        .add(Port.OUTPUT, "TC", 1, Ttl744510.pinNrToPortNr(Ttl744510.TC))
        .add(Port.INPUT, "MR", 1, Ttl744510.pinNrToPortNr(Ttl744510.MR), true)
        .add(Port.INPUT, "UP_DN", 1, Ttl744510.pinNrToPortNr(Ttl744510.UPDN), false)
        .add(Port.OUTPUT, "Q1", 1, Ttl744510.pinNrToPortNr(Ttl744510.Q1))
        .add(Port.INPUT, "D1", 1, Ttl744510.pinNrToPortNr(Ttl744510.D1), true)
        .add(Port.INPUT, "D2", 1, Ttl744510.pinNrToPortNr(Ttl744510.D2), true)
        .add(Port.OUTPUT, "Q2", 1, Ttl744510.pinNrToPortNr(Ttl744510.Q2))
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl744510.pinNrToPortNr(Ttl744510.CP), true);
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
    addVhdlSelect(contents, "s_up", Ttl744510.NEXT_UP);
    addVhdlSelect(contents, "s_down", Ttl744510.NEXT_DOWN);
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
                    s_count <= s_up;
                 {{else}}
                    s_count <= s_down;
                 {{end}} {{if}};
              {{end}} {{if}};
           {{end}} {{if}};
        {{end}} {{process}} count;

        TC <= '0' {{when}} CE = '0' {{and}} UP_DN = '1'
                  {{and}} s_count(0) = '1' {{and}} s_count(3) = '1' {{else}}
              '0' {{when}} CE = '0' {{and}} UP_DN = '0' {{and}} s_count = "0000" {{else}}
              '1';
        Q0 <= s_count(0);
        Q1 <= s_count(1);
        Q2 <= s_count(2);
        Q3 <= s_count(3);
        """);
  }

  private static void addVerilog(LineBuffer contents) {
    addVerilogSelect(contents, "s_up", Ttl744510.NEXT_UP);
    addVerilogSelect(contents, "s_down", Ttl744510.NEXT_DOWN);
    contents.add(
        """
        assign s_data = {D3, D2, D1, D0};
        assign Q0 = s_count[0];
        assign Q1 = s_count[1];
        assign Q2 = s_count[2];
        assign Q3 = s_count[3];
        assign TC = ((CE == 0) && (UP_DN == 1) && (s_count[0] == 1) && (s_count[3] == 1)) ? 1'b0 :
                    ((CE == 0) && (UP_DN == 0) && (s_count == 4'b0000)) ? 1'b0 : 1'b1;

        always @(posedge {{CLK}})
        begin
           if (({{tick}} == 1) && (MR == 0) && (PL == 0) && (CE == 0))
              s_count <= (UP_DN == 1) ? s_up : s_down;
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

  private static void addVhdlSelect(LineBuffer contents, String target, int[] table) {
    for (var code = 0; code < table.length; code++) {
      final var next = bits(table[code]);
      final var current = bits(code);
      if (code == 0) {
        contents.add(
            target + " <= \"" + next + "\" {{when}} s_count = \"" + current + "\" {{else}}");
      } else if (code == table.length - 1) {
        contents.add("        \"" + next + "\";");
      } else {
        contents.add("        \"" + next + "\" {{when}} s_count = \"" + current + "\" {{else}}");
      }
    }
  }

  private static void addVerilogSelect(LineBuffer contents, String target, int[] table) {
    for (var code = 0; code < table.length; code++) {
      final var next = bits(table[code]);
      final var current = bits(code);
      if (code == 0) {
        contents.add("assign " + target + " = (s_count == 4'b" + current + ") ? 4'b" + next + " :");
      } else if (code == table.length - 1) {
        contents.add("         4'b" + next + ";");
      } else {
        contents.add("         (s_count == 4'b" + current + ") ? 4'b" + next + " :");
      }
    }
  }

  private static String bits(int code) {
    return String.format("%4s", Integer.toBinaryString(code & 0xF)).replace(' ', '0');
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) {
      return false;
    }
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
