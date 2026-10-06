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

/** VHDL and Verilog for the two falling-edge BCD counters of a 74490. */
public class Ttl74490HdlGenerator extends AbstractHdlGeneratorFactory {

  /**
   * Creates an HDL generator for the 74490. The second clock's width is its index, which {@link
   * HdlPorts#getTickName(String)} uses to name {@code tick2}. That matches {@code Ttl7476}.
   */
  public Ttl74490HdlGenerator() {
    super();
    myWires
        .addWire("s_next1", 4)
        .addWire("s_next2", 4)
        .addRegister("s_count1", 4)
        .addRegister("s_count2", 4);
    myPorts
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, Ttl74490.PORT_INDEX_1CLK)
        .add(Port.INPUT, "CLR1", 1, Ttl74490.PORT_INDEX_1CLR)
        .add(Port.OUTPUT, "QA1", 1, Ttl74490.PORT_INDEX_1QA)
        .add(Port.INPUT, "SET91", 1, Ttl74490.PORT_INDEX_1SET9)
        .add(Port.OUTPUT, "QB1", 1, Ttl74490.PORT_INDEX_1QB)
        .add(Port.OUTPUT, "QC1", 1, Ttl74490.PORT_INDEX_1QC)
        .add(Port.OUTPUT, "QD1", 1, Ttl74490.PORT_INDEX_1QD)
        .add(Port.OUTPUT, "QD2", 1, Ttl74490.PORT_INDEX_2QD)
        .add(Port.OUTPUT, "QC2", 1, Ttl74490.PORT_INDEX_2QC)
        .add(Port.OUTPUT, "QB2", 1, Ttl74490.PORT_INDEX_2QB)
        .add(Port.INPUT, "SET92", 1, Ttl74490.PORT_INDEX_2SET9)
        .add(Port.OUTPUT, "QA2", 1, Ttl74490.PORT_INDEX_2QA)
        .add(Port.INPUT, "CLR2", 1, Ttl74490.PORT_INDEX_2CLR)
        .add(Port.CLOCK, HdlPorts.getClockName(2), 2, Ttl74490.PORT_INDEX_2CLK);
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
      addVhdlNext(contents, "s_next1", "s_count1");
      addVhdlNext(contents, "s_next2", "s_count2");
      contents.add(
          """
          QA1 <= s_count1(0);
          QB1 <= s_count1(1);
          QC1 <= s_count1(2);
          QD1 <= s_count1(3);
          QA2 <= s_count2(0);
          QB2 <= s_count2(1);
          QC2 <= s_count2(2);
          QD2 <= s_count2(3);

          count1 : {{process}}({{CLK1}}, SET91, CLR1) {{is}}
          {{begin}}
             {{if}} (SET91 = '1') {{then}} s_count1 <= "1001";
             {{elsif}} (CLR1 = '1') {{then}} s_count1 <= "0000";
             {{elsif}} (falling_edge({{CLK1}})) {{then}}
                {{if}} ({{tick1}} = '1') {{then}} s_count1 <= s_next1; {{end}} {{if}};
             {{end}} {{if}};
          {{end}} {{process}} count1;

          count2 : {{process}}({{CLK2}}, SET92, CLR2) {{is}}
          {{begin}}
             {{if}} (SET92 = '1') {{then}} s_count2 <= "1001";
             {{elsif}} (CLR2 = '1') {{then}} s_count2 <= "0000";
             {{elsif}} (falling_edge({{CLK2}})) {{then}}
                {{if}} ({{tick2}} = '1') {{then}} s_count2 <= s_next2; {{end}} {{if}};
             {{end}} {{if}};
          {{end}} {{process}} count2;
          """);
    } else {
      contents.empty();
      addVerilogNext(contents, "s_next1", "s_count1");
      addVerilogNext(contents, "s_next2", "s_count2");
      contents.add(
          """
          assign QA1 = s_count1[0];
          assign QB1 = s_count1[1];
          assign QC1 = s_count1[2];
          assign QD1 = s_count1[3];
          assign QA2 = s_count2[0];
          assign QB2 = s_count2[1];
          assign QC2 = s_count2[2];
          assign QD2 = s_count2[3];

          always @(negedge {{CLK1}} or posedge SET91 or posedge CLR1)
          begin
             if (SET91 == 1) s_count1 <= 4'b1001;
             else if (CLR1 == 1) s_count1 <= 4'b0000;
             else if ({{tick1}} == 1) s_count1 <= s_next1;
          end

          always @(negedge {{CLK2}} or posedge SET92 or posedge CLR2)
          begin
             if (SET92 == 1) s_count2 <= 4'b1001;
             else if (CLR2 == 1) s_count2 <= 4'b0000;
             else if ({{tick2}} == 1) s_count2 <= s_next2;
          end
          """);
    }
    return contents.empty();
  }

  private static void addVhdlNext(LineBuffer contents, String next, String count) {
    for (var code = 0; code < 9; code++) {
      final var choice =
          "\"" + bits(code + 1) + "\" {{when}} " + count + " = \"" + bits(code) + "\" {{else}}";
      if (code == 0) {
        contents.add(next + " <= " + choice);
      } else {
        contents.add("             " + choice);
      }
    }
    contents.add("             \"0000\";");
  }

  private static void addVerilogNext(LineBuffer contents, String next, String count) {
    for (var code = 0; code < 9; code++) {
      final var choice =
          "(" + count + " == 4'b" + bits(code) + ") ? 4'b" + bits(code + 1) + " :";
      if (code == 0) {
        contents.add("assign " + next + " = " + choice);
      } else {
        contents.add("    " + choice);
      }
    }
    contents.add("    4'b0000;");
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
