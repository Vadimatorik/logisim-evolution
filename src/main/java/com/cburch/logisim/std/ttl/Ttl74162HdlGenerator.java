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

/** VHDL and Verilog for the synchronous BCD counter and synchronous reset of a 74162. */
public class Ttl74162HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the 74162. Open active-low controls stay high. */
  public Ttl74162HdlGenerator() {
    super();
    myWires
        .addWire("s_counted", 4)
        .addWire("s_next", 4)
        .addWire("s_data", 4)
        .addRegister("s_count", 4);
    // MR and PE pull high, so an open counter neither resets nor loads. The count enables pull
    // low, so an open counter holds. Data pins pull low.
    myPorts
        .add(Port.INPUT, "MR", 1, Ttl74162.pinNrToPortNr(Ttl74162.MR), false)
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl74162.pinNrToPortNr(Ttl74162.CP), true)
        .add(Port.INPUT, "D0", 1, Ttl74162.pinNrToPortNr(Ttl74162.D0), true)
        .add(Port.INPUT, "D1", 1, Ttl74162.pinNrToPortNr(Ttl74162.D1), true)
        .add(Port.INPUT, "D2", 1, Ttl74162.pinNrToPortNr(Ttl74162.D2), true)
        .add(Port.INPUT, "D3", 1, Ttl74162.pinNrToPortNr(Ttl74162.D3), true)
        .add(Port.INPUT, "CEP", 1, Ttl74162.pinNrToPortNr(Ttl74162.CEP), true)
        .add(Port.INPUT, "PE", 1, Ttl74162.pinNrToPortNr(Ttl74162.PE), false)
        .add(Port.INPUT, "CET", 1, Ttl74162.pinNrToPortNr(Ttl74162.CET), true)
        .add(Port.OUTPUT, "Q3", 1, Ttl74162.pinNrToPortNr(Ttl74162.Q3))
        .add(Port.OUTPUT, "Q2", 1, Ttl74162.pinNrToPortNr(Ttl74162.Q2))
        .add(Port.OUTPUT, "Q1", 1, Ttl74162.pinNrToPortNr(Ttl74162.Q1))
        .add(Port.OUTPUT, "Q0", 1, Ttl74162.pinNrToPortNr(Ttl74162.Q0))
        .add(Port.OUTPUT, "TC", 1, Ttl74162.pinNrToPortNr(Ttl74162.TC));
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
    addVhdlSelect(contents);
    contents.add(
        """
        s_data <= D3 & D2 & D1 & D0;
        s_next <= s_count {{when}} {{tick}} = '0' {{else}}
                  "0000" {{when}} MR = '0' {{else}}
                  s_data {{when}} PE = '0' {{else}}
                  s_counted {{when}} CEP = '1' {{and}} CET = '1' {{else}}
                  s_count;
        TC <= '1' {{when}} CET = '1' {{and}} s_count = "1001" {{else}} '0';
        Q0 <= s_count(0);
        Q1 <= s_count(1);
        Q2 <= s_count(2);
        Q3 <= s_count(3);

        count : {{process}}({{CLK}}) {{is}}
        {{begin}}
           {{if}} (rising_edge({{CLK}})) {{then}}
              s_count <= s_next;
           {{end}} {{if}};
        {{end}} {{process}} count;
        """);
  }

  private static void addVerilog(LineBuffer contents) {
    contents.add("assign s_counted = " + verilogSelect() + ";");
    contents.add(
        """
        assign s_data = {D3, D2, D1, D0};
        assign s_next = ({{tick}} == 0) ? s_count :
                        (MR == 0) ? 4'b0000 :
                        (PE == 0) ? s_data :
                        ((CEP == 1) && (CET == 1)) ? s_counted :
                        s_count;
        assign TC = (CET == 1) && (s_count == 4'b1001);
        assign Q0 = s_count[0];
        assign Q1 = s_count[1];
        assign Q2 = s_count[2];
        assign Q3 = s_count[3];

        always @(posedge {{CLK}})
        begin
           s_count <= s_next;
        end
        """);
  }

  private static void addVhdlSelect(LineBuffer contents) {
    final var table = Ttl74162.NEXT_COUNT;
    for (var code = 0; code < table.length; code++) {
      final var next = bits(table[code]);
      final var current = bits(code);
      if (code == 0) {
        contents.add(
            "s_counted <= \"" + next + "\" {{when}} s_count = \"" + current + "\" {{else}}");
      } else if (code == table.length - 1) {
        contents.add("             \"" + next + "\";");
      } else {
        contents.add(
            "             \"" + next + "\" {{when}} s_count = \"" + current + "\" {{else}}");
      }
    }
  }

  private static String verilogSelect() {
    final var table = Ttl74162.NEXT_COUNT;
    final var text = new StringBuilder();
    for (var code = 0; code < table.length; code++) {
      text.append("(s_count == 4'b")
          .append(bits(code))
          .append(") ? 4'b")
          .append(bits(table[code]))
          .append(" : ");
    }
    return text.append("4'b0000").toString();
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
