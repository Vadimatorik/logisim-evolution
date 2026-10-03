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
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

/**
 * VHDL and Verilog for one 74HC563. {@code LE} stays an ordinary input: the latch is transparent
 * for the whole time the pin is high, so an edge and a clock tick would drop updates that happen
 * while the latch is already open.
 */
public class Ttl74563HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates a generator for the inverting octal latch. */
  public Ttl74563HdlGenerator() {
    super();
    myWires.addRegister("state", 8);
    myPorts
        .add(Port.INPUT, "nOE", 1, Ttl74563.PORT_INDEX_nOE)
        .add(Port.INPUT, "D0", 1, Ttl74563.PORT_INDEX_D0)
        .add(Port.INPUT, "D1", 1, Ttl74563.PORT_INDEX_D1)
        .add(Port.INPUT, "D2", 1, Ttl74563.PORT_INDEX_D2)
        .add(Port.INPUT, "D3", 1, Ttl74563.PORT_INDEX_D3)
        .add(Port.INPUT, "D4", 1, Ttl74563.PORT_INDEX_D4)
        .add(Port.INPUT, "D5", 1, Ttl74563.PORT_INDEX_D5)
        .add(Port.INPUT, "D6", 1, Ttl74563.PORT_INDEX_D6)
        .add(Port.INPUT, "D7", 1, Ttl74563.PORT_INDEX_D7)
        .add(Port.INPUT, "LE", 1, Ttl74563.PORT_INDEX_LE)
        .add(Port.OUTPUT, "Q0", 1, Ttl74563.PORT_INDEX_Q0)
        .add(Port.OUTPUT, "Q1", 1, Ttl74563.PORT_INDEX_Q1)
        .add(Port.OUTPUT, "Q2", 1, Ttl74563.PORT_INDEX_Q2)
        .add(Port.OUTPUT, "Q3", 1, Ttl74563.PORT_INDEX_Q3)
        .add(Port.OUTPUT, "Q4", 1, Ttl74563.PORT_INDEX_Q4)
        .add(Port.OUTPUT, "Q5", 1, Ttl74563.PORT_INDEX_Q5)
        .add(Port.OUTPUT, "Q6", 1, Ttl74563.PORT_INDEX_Q6)
        .add(Port.OUTPUT, "Q7", 1, Ttl74563.PORT_INDEX_Q7);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          Q0 <= state(0) {{when}} nOE = '0' {{else}} 'Z';
          Q1 <= state(1) {{when}} nOE = '0' {{else}} 'Z';
          Q2 <= state(2) {{when}} nOE = '0' {{else}} 'Z';
          Q3 <= state(3) {{when}} nOE = '0' {{else}} 'Z';
          Q4 <= state(4) {{when}} nOE = '0' {{else}} 'Z';
          Q5 <= state(5) {{when}} nOE = '0' {{else}} 'Z';
          Q6 <= state(6) {{when}} nOE = '0' {{else}} 'Z';
          Q7 <= state(7) {{when}} nOE = '0' {{else}} 'Z';

          latch : {{process}}(LE, D0, D1, D2, D3, D4, D5, D6, D7) {{is}}
             {{begin}}
                {{if}} (LE = '1') {{then}}
                   state <= {{not}}(D7&D6&D5&D4&D3&D2&D1&D0);
                {{end}} {{if}};
             {{end}} {{process}} latch;
          """);
    } else {
      contents.add("""
          assign Q0 = (nOE == 0) ? state[0] : 1'bz;
          assign Q1 = (nOE == 0) ? state[1] : 1'bz;
          assign Q2 = (nOE == 0) ? state[2] : 1'bz;
          assign Q3 = (nOE == 0) ? state[3] : 1'bz;
          assign Q4 = (nOE == 0) ? state[4] : 1'bz;
          assign Q5 = (nOE == 0) ? state[5] : 1'bz;
          assign Q6 = (nOE == 0) ? state[6] : 1'bz;
          assign Q7 = (nOE == 0) ? state[7] : 1'bz;

          always @(*)
          begin
             if (LE) state <= ~{D7, D6, D5, D4, D3, D2, D1, D0};
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
