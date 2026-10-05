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

/** VHDL and Verilog for the synchronous clear, shifts and three-state bus of a 74323. */
public class Ttl74323HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the 74323. Open active-low controls stay high. */
  public Ttl74323HdlGenerator() {
    super();
    myWires
        .addWire("s_load", 8)
        .addWire("s_right", 8)
        .addWire("s_left", 8)
        .addWire("s_next", 8)
        .addWire("s_oe", 1)
        .addRegister("s_reg", 8);
    // nCLR and the output enables pull high, so an open register neither clears nor drives the
    // bus. The mode selects pull low, which is hold. Serial inputs pull low.
    myPorts
        .add(Port.INPUT, "S0", 1, Ttl74323.pinNrToPortNr(Ttl74323.S0), true)
        .add(Port.INPUT, "nOE1", 1, Ttl74323.pinNrToPortNr(Ttl74323.nOE1), false)
        .add(Port.INPUT, "nOE2", 1, Ttl74323.pinNrToPortNr(Ttl74323.nOE2), false)
        .add(Port.INOUT, "IO6", 1, Ttl74323.pinNrToPortNr(Ttl74323.IO6), true)
        .add(Port.INOUT, "IO4", 1, Ttl74323.pinNrToPortNr(Ttl74323.IO4), true)
        .add(Port.INOUT, "IO2", 1, Ttl74323.pinNrToPortNr(Ttl74323.IO2), true)
        .add(Port.INOUT, "IO0", 1, Ttl74323.pinNrToPortNr(Ttl74323.IO0), true)
        .add(Port.OUTPUT, "Q0", 1, Ttl74323.pinNrToPortNr(Ttl74323.Q0))
        .add(Port.INPUT, "nCLR", 1, Ttl74323.pinNrToPortNr(Ttl74323.nCLR), false)
        .add(Port.INPUT, "SR", 1, Ttl74323.pinNrToPortNr(Ttl74323.SR), true)
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl74323.pinNrToPortNr(Ttl74323.CP), true)
        .add(Port.INOUT, "IO1", 1, Ttl74323.pinNrToPortNr(Ttl74323.IO1), true)
        .add(Port.INOUT, "IO3", 1, Ttl74323.pinNrToPortNr(Ttl74323.IO3), true)
        .add(Port.INOUT, "IO5", 1, Ttl74323.pinNrToPortNr(Ttl74323.IO5), true)
        .add(Port.INOUT, "IO7", 1, Ttl74323.pinNrToPortNr(Ttl74323.IO7), true)
        .add(Port.OUTPUT, "Q7", 1, Ttl74323.pinNrToPortNr(Ttl74323.Q7))
        .add(Port.INPUT, "SL", 1, Ttl74323.pinNrToPortNr(Ttl74323.SL), true)
        .add(Port.INPUT, "S1", 1, Ttl74323.pinNrToPortNr(Ttl74323.S1), true);
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
        s_load <= IO7 & IO6 & IO5 & IO4 & IO3 & IO2 & IO1 & IO0;
        s_right <= s_reg(6 {{downto}} 0) & SR;
        s_left <= SL & s_reg(7 {{downto}} 1);
        s_next <= s_reg {{when}} {{tick}} = '0' {{else}}
                  "00000000" {{when}} nCLR = '0' {{else}}
                  s_load {{when}} S1 = '1' {{and}} S0 = '1' {{else}}
                  s_right {{when}} S1 = '0' {{and}} S0 = '1' {{else}}
                  s_left {{when}} S1 = '1' {{and}} S0 = '0' {{else}}
                  s_reg;
        s_oe <= '1' {{when}} nOE1 = '0' {{and}} nOE2 = '0' {{and}} {{not}}(S0 = '1' {{and}} S1 = '1') {{else}}
                '0';
        Q0 <= s_reg(0);
        Q7 <= s_reg(7);
        """);
    for (var bit = 0; bit < 8; bit++) {
      contents.add(
          "IO" + bit + " <= s_reg(" + bit + ") {{when}} s_oe = '1' {{else}} 'Z';");
    }
    contents.add(
        """

        register : {{process}}({{CLK}}) {{is}}
        {{begin}}
           {{if}} (rising_edge({{CLK}})) {{then}}
              s_reg <= s_next;
           {{end}} {{if}};
        {{end}} {{process}} register;
        """);
  }

  private static void addVerilog(LineBuffer contents) {
    contents.add(
        """
        assign s_load = {IO7, IO6, IO5, IO4, IO3, IO2, IO1, IO0};
        assign s_right = {s_reg[6:0], SR};
        assign s_left = {SL, s_reg[7:1]};
        assign s_next = ({{tick}} == 0) ? s_reg :
                        (nCLR == 0) ? 8'b00000000 :
                        ((S1 == 1) && (S0 == 1)) ? s_load :
                        ((S1 == 0) && (S0 == 1)) ? s_right :
                        ((S1 == 1) && (S0 == 0)) ? s_left :
                        s_reg;
        assign s_oe = (nOE1 == 0) && (nOE2 == 0) && !((S0 == 1) && (S1 == 1));
        assign Q0 = s_reg[0];
        assign Q7 = s_reg[7];
        """);
    for (var bit = 0; bit < 8; bit++) {
      contents.add("assign IO" + bit + " = s_oe ? s_reg[" + bit + "] : 1'bZ;");
    }
    contents.add(
        """

        always @(posedge {{CLK}})
        begin
           s_reg <= s_next;
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
