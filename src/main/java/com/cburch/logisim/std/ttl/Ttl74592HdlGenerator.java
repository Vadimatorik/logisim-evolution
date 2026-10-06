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

/** VHDL and Verilog for the input register, asynchronous counter controls, and ripple carry. */
public class Ttl74592HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the 74592. Open active-low controls stay high. */
  public Ttl74592HdlGenerator() {
    super();
    myWires
        .addWire("s_data", 8)
        .addWire("s_reg_next", 8)
        .addRegister("s_reg", 8)
        .addRegister("s_count", 8);
    // Active-low controls pull high, so an open device neither clears, loads, nor counts.
    // Data and both clocks pull low.
    myPorts
        .add(Port.INPUT, "A", 1, Ttl74592.pinNrToPortNr(Ttl74592.A), true)
        .add(Port.INPUT, "B", 1, Ttl74592.pinNrToPortNr(Ttl74592.B), true)
        .add(Port.INPUT, "C", 1, Ttl74592.pinNrToPortNr(Ttl74592.C), true)
        .add(Port.INPUT, "D", 1, Ttl74592.pinNrToPortNr(Ttl74592.D), true)
        .add(Port.INPUT, "E", 1, Ttl74592.pinNrToPortNr(Ttl74592.E), true)
        .add(Port.INPUT, "F", 1, Ttl74592.pinNrToPortNr(Ttl74592.F), true)
        .add(Port.INPUT, "G", 1, Ttl74592.pinNrToPortNr(Ttl74592.G), true)
        .add(Port.OUTPUT, "RCO", 1, Ttl74592.pinNrToPortNr(Ttl74592.RCO))
        .add(Port.INPUT, "CCLR", 1, Ttl74592.pinNrToPortNr(Ttl74592.CCLR), false)
        .add(Port.CLOCK, HdlPorts.getClockName(2), 1, Ttl74592.pinNrToPortNr(Ttl74592.CCK), true)
        .add(Port.INPUT, "CCKEN", 1, Ttl74592.pinNrToPortNr(Ttl74592.CCKEN), false)
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, Ttl74592.pinNrToPortNr(Ttl74592.RCK), true)
        .add(Port.INPUT, "CLOAD", 1, Ttl74592.pinNrToPortNr(Ttl74592.CLOAD), false)
        .add(Port.INPUT, "H", 1, Ttl74592.pinNrToPortNr(Ttl74592.H), true);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer()
            .pair("CLKR", HdlPorts.getClockName(1))
            .pair("CLKC", HdlPorts.getClockName(2))
            .pair("tickr", HdlPorts.getTickName(1))
            .pair("tickc", HdlPorts.getTickName(2));
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
        s_data <= H & G & F & E & D & C & B & A;
        RCO <= '1' {{when}} s_count = "11111111" {{else}} '0';
        s_reg_next <= s_data {{when}} {{tickr}} = '1' {{else}} s_reg;

        register : {{process}}({{CLKR}}) {{is}}
        {{begin}}
           {{if}} (rising_edge({{CLKR}})) {{then}}
              s_reg <= s_reg_next;
           {{end}} {{if}};
        {{end}} {{process}} register;

        counter : {{process}}({{CLKC}}, CCLR, CLOAD, s_reg) {{is}}
        {{begin}}
           {{if}} (CCLR = '0') {{then}}
              s_count <= "00000000";
           {{elsif}} (CLOAD = '0') {{then}}
              s_count <= s_reg;
           {{elsif}} (rising_edge({{CLKC}})) {{then}}
              {{if}} (CCKEN = '0' {{and}} {{tickc}} = '1') {{then}}
                 s_count <= std_logic_vector(unsigned(s_count) + 1);
              {{end}} {{if}};
           {{end}} {{if}};
        {{end}} {{process}} counter;
        """);
  }

  private static void addVerilog(LineBuffer contents) {
    contents.add(
        """
        assign s_data = {H, G, F, E, D, C, B, A};
        assign RCO = (s_count == 8'b11111111);
        assign s_reg_next = ({{tickr}} == 1) ? s_data : s_reg;

        always @(posedge {{CLKR}})
        begin
           s_reg <= s_reg_next;
        end

        always @(posedge {{CLKC}} or negedge CCLR or negedge CLOAD or s_reg)
        begin
           if (CCLR == 0)
              s_count <= 8'b00000000;
           else if (CLOAD == 0)
              s_count <= s_reg;
           else if ((CCKEN == 0) && ({{tickc}} == 1))
              s_count <= s_count + 8'b00000001;
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
