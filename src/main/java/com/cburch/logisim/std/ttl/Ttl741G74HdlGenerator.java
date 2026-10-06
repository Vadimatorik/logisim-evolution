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

/**
 * VHDL and Verilog for the 74LVC1G74. Set and reset are active low and override the clock. Both
 * asserted stores two highs, because the outputs are not complements in that case. An open set or
 * reset pin is pulled high so it stays inactive.
 */
public class Ttl741G74HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the 741G74. */
  public Ttl741G74HdlGenerator() {
    super();
    myWires.addWire("s_next", 2);
    myWires.addRegister("s_state", 2);
    myPorts
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl741G74.PORT_CP, true)
        .add(Port.INPUT, "D", 1, Ttl741G74.PORT_D, true)
        .add(Port.OUTPUT, "nQ", 1, Ttl741G74.PORT_NQ)
        .add(Port.OUTPUT, "Q", 1, Ttl741G74.PORT_Q)
        .add(Port.INPUT, "RD", 1, Ttl741G74.PORT_RD, false)
        .add(Port.INPUT, "SD", 1, Ttl741G74.PORT_SD, false);
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
        Q  <= s_state(0);
        nQ <= s_state(1);
        s_next <= s_state {{when}} {{tick}} = '0' {{else}} ({{not}} D) & D;

        ff : {{process}}({{CLK}}, SD, RD) {{is}}
           {{begin}}
              {{if}} ((SD = '0') {{and}} (RD = '0')) {{then}} s_state <= "11";
              {{elsif}} (SD = '0') {{then}} s_state <= "01";
              {{elsif}} (RD = '0') {{then}} s_state <= "10";
              {{elsif}} (rising_edge({{CLK}})) {{then}} s_state <= s_next;
              {{end}} {{if}};
           {{end}} {{process}} ff;
        """);
  }

  private static void addVerilog(LineBuffer contents) {
    contents.add(
        """
        assign Q      = s_state[0];
        assign nQ     = s_state[1];
        assign s_next = ({{tick}} == 0) ? s_state : {~D, D};

        always @(posedge {{CLK}} or negedge SD or posedge SD or negedge RD or posedge RD)
        begin
           if ((SD == 0) && (RD == 0)) s_state <= 2'b11;
           else if (SD == 0) s_state <= 2'b01;
           else if (RD == 0) s_state <= 2'b10;
           else if ({{tick}} == 1) s_state <= s_next;
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
