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
 * VHDL and Verilog for one 74HC534. The register loads on the rising edge and has no asynchronous
 * clear. The outputs are the complement of the stored bits while nOE is low, and high-impedance
 * otherwise. nOE does not steer the register.
 */
public class Ttl74534HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates a generator for the octal inverting positive-edge register. */
  public Ttl74534HdlGenerator() {
    super();
    myWires
        .addWire("state", 8)
        .addWire("next", 8);
    myPorts
        .add(Port.INPUT, "nOE", 1, Ttl74534.PORT_INDEX_nOE)
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl74534.PORT_INDEX_CLK)
        .add(Port.INPUT, "D1", 1, Ttl74534.PORT_INDEX_D1)
        .add(Port.INPUT, "D2", 1, Ttl74534.PORT_INDEX_D2)
        .add(Port.INPUT, "D3", 1, Ttl74534.PORT_INDEX_D3)
        .add(Port.INPUT, "D4", 1, Ttl74534.PORT_INDEX_D4)
        .add(Port.INPUT, "D5", 1, Ttl74534.PORT_INDEX_D5)
        .add(Port.INPUT, "D6", 1, Ttl74534.PORT_INDEX_D6)
        .add(Port.INPUT, "D7", 1, Ttl74534.PORT_INDEX_D7)
        .add(Port.INPUT, "D8", 1, Ttl74534.PORT_INDEX_D8)
        .add(Port.OUTPUT, "nQ1", 1, Ttl74534.PORT_INDEX_nQ1)
        .add(Port.OUTPUT, "nQ2", 1, Ttl74534.PORT_INDEX_nQ2)
        .add(Port.OUTPUT, "nQ3", 1, Ttl74534.PORT_INDEX_nQ3)
        .add(Port.OUTPUT, "nQ4", 1, Ttl74534.PORT_INDEX_nQ4)
        .add(Port.OUTPUT, "nQ5", 1, Ttl74534.PORT_INDEX_nQ5)
        .add(Port.OUTPUT, "nQ6", 1, Ttl74534.PORT_INDEX_nQ6)
        .add(Port.OUTPUT, "nQ7", 1, Ttl74534.PORT_INDEX_nQ7)
        .add(Port.OUTPUT, "nQ8", 1, Ttl74534.PORT_INDEX_nQ8);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer().pair("CLK", HdlPorts.CLOCK).pair("tick", HdlPorts.TICK);
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          next <= D8&D7&D6&D5&D4&D3&D2&D1 {{when}} {{tick}} = '1' {{else}} state;
          nQ1  <= {{not}}(state(0)) {{when}} nOE = '0' {{else}} 'Z';
          nQ2  <= {{not}}(state(1)) {{when}} nOE = '0' {{else}} 'Z';
          nQ3  <= {{not}}(state(2)) {{when}} nOE = '0' {{else}} 'Z';
          nQ4  <= {{not}}(state(3)) {{when}} nOE = '0' {{else}} 'Z';
          nQ5  <= {{not}}(state(4)) {{when}} nOE = '0' {{else}} 'Z';
          nQ6  <= {{not}}(state(5)) {{when}} nOE = '0' {{else}} 'Z';
          nQ7  <= {{not}}(state(6)) {{when}} nOE = '0' {{else}} 'Z';
          nQ8  <= {{not}}(state(7)) {{when}} nOE = '0' {{else}} 'Z';

          dffs : {{process}}({{CLK}}) {{is}}
             {{begin}}
                {{if}} (rising_edge({{CLK}})) {{then}} state <= next;
                {{end}} {{if}};
             {{end}} {{process}} dffs;
          """);
    } else {
      contents.add("""
          assign next = ({{tick}} == 1) ? {D8, D7, D6, D5, D4, D3, D2, D1} : state;
          assign nQ1 = (nOE == 0) ? ~state[0] : 1'bz;
          assign nQ2 = (nOE == 0) ? ~state[1] : 1'bz;
          assign nQ3 = (nOE == 0) ? ~state[2] : 1'bz;
          assign nQ4 = (nOE == 0) ? ~state[3] : 1'bz;
          assign nQ5 = (nOE == 0) ? ~state[4] : 1'bz;
          assign nQ6 = (nOE == 0) ? ~state[5] : 1'bz;
          assign nQ7 = (nOE == 0) ? ~state[6] : 1'bz;
          assign nQ8 = (nOE == 0) ? ~state[7] : 1'bz;

          always @(posedge {{CLK}})
          begin
             state <= next;
          end
          """);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
