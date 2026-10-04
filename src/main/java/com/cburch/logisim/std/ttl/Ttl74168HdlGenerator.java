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

/** HDL for the synchronous BCD decade up/down counter in {@link Ttl74168}. */
public class Ttl74168HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates the port and signal map for a BCD 74168 model. */
  public Ttl74168HdlGenerator() {
    super();
    myWires
        .addRegister("curState", 4)
        .addWire("nextState", 4)
        .addWire("loaded", 4)
        .addWire("upCount", 4)
        .addWire("downCount", 4)
        .addWire("counting", 1);
    myPorts
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl74168.PORT_INDEX_CP)
        .add(Port.INPUT, "UD", 1, Ttl74168.PORT_INDEX_UD)
        .add(Port.INPUT, "D0", 1, Ttl74168.PORT_INDEX_D0)
        .add(Port.INPUT, "D1", 1, Ttl74168.PORT_INDEX_D1)
        .add(Port.INPUT, "D2", 1, Ttl74168.PORT_INDEX_D2)
        .add(Port.INPUT, "D3", 1, Ttl74168.PORT_INDEX_D3)
        .add(Port.INPUT, "CEP", 1, Ttl74168.PORT_INDEX_CEP)
        .add(Port.INPUT, "PE", 1, Ttl74168.PORT_INDEX_PE)
        .add(Port.INPUT, "CET", 1, Ttl74168.PORT_INDEX_CET)
        .add(Port.OUTPUT, "Q0", 1, Ttl74168.PORT_INDEX_Q0)
        .add(Port.OUTPUT, "Q1", 1, Ttl74168.PORT_INDEX_Q1)
        .add(Port.OUTPUT, "Q2", 1, Ttl74168.PORT_INDEX_Q2)
        .add(Port.OUTPUT, "Q3", 1, Ttl74168.PORT_INDEX_Q3)
        .add(Port.OUTPUT, "TC", 1, Ttl74168.PORT_INDEX_TC);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist netlist, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer().pair("CLK", HdlPorts.CLOCK).pair("tick", HdlPorts.TICK);
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords();
      addVhdl(contents);
    } else {
      addVerilog(contents);
    }
    return contents.empty();
  }

  private static void addVhdl(LineBuffer contents) {
    contents.add("loaded <= D3&D2&D1&D0;");
    addVhdlSelect(contents, "upCount", Ttl74168.NEXT_UP);
    addVhdlSelect(contents, "downCount", Ttl74168.NEXT_DOWN);
    contents.add(
        """
        counting <= {{not}}(CEP) {{and}} {{not}}(CET) {{and}} PE;

        nextState <= curState {{when}} {{tick}} = '0' {{else}}
                     loaded {{when}} PE = '0' {{else}}
                     curState {{when}} counting = '0' {{else}}
                     upCount {{when}} UD = '1' {{else}}
                     downCount;

        dffs : {{process}}({{CLK}}) {{is}}
        {{begin}}
           {{if}} (rising_edge({{CLK}})) {{then}}
              curState <= nextState;
           {{end}} {{if}};
        {{end}} {{process}} dffs;

        Q0 <= curState(0);
        Q1 <= curState(1);
        Q2 <= curState(2);
        Q3 <= curState(3);
        TC <= '0' {{when}} CET = '0' {{and}} UD = '1' {{and}} curState(0) = '1'
                  {{and}} curState(3) = '1'
              {{else}} '0' {{when}} CET = '0' {{and}} UD = '0' {{and}} curState = "0000"
              {{else}} '1';
        """);
  }

  private static void addVerilog(LineBuffer contents) {
    contents.add("assign loaded = {D3, D2, D1, D0};");
    contents.add("assign upCount = " + verilogSelect(Ttl74168.NEXT_UP) + ";");
    contents.add("assign downCount = " + verilogSelect(Ttl74168.NEXT_DOWN) + ";");
    contents.add(
        """
        assign counting = ~CEP & ~CET & PE;
        assign nextState = {{tick}} == 0 ? curState :
                           PE == 0 ? loaded :
                           counting == 0 ? curState :
                           UD == 1 ? upCount : downCount;
        assign Q0 = curState[0];
        assign Q1 = curState[1];
        assign Q2 = curState[2];
        assign Q3 = curState[3];
        assign TC = (CET == 0 && UD == 1 && curState[0] == 1 && curState[3] == 1) ? 1'b0 :
                    (CET == 0 && UD == 0 && curState == 4'b0000) ? 1'b0 : 1'b1;

        always @(posedge {{CLK}})
        begin
           curState <= nextState;
        end
        """);
  }

  private static void addVhdlSelect(LineBuffer contents, String name, int[] table) {
    for (var code = 0; code < table.length; code++) {
      final var choice = "\"" + bits(table[code]) + "\"";
      final var current = "\"" + bits(code) + "\"";
      if (code == 0) {
        contents.add(
            "with curState {{select}} " + name + " <=");
        contents.add("  " + choice + " {{when}} " + current + ",");
      } else if (code == table.length - 1) {
        contents.add("  " + choice + " {{when}} {{others}};");
      } else {
        contents.add("  " + choice + " {{when}} " + current + ",");
      }
    }
  }

  private static String verilogSelect(int[] table) {
    final var text = new StringBuilder();
    for (var code = 0; code < table.length - 1; code++) {
      text.append("(curState == 4'b")
          .append(bits(code))
          .append(") ? 4'b")
          .append(bits(table[code]))
          .append(" : ");
    }
    return text.append("4'b").append(bits(table[table.length - 1])).toString();
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
