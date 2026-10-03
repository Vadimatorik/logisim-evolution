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
 * VHDL and Verilog for one 74HC191. Parallel load is level-sensitive and overrides the count. The
 * count changes on the rising edge when enabled. Terminal count and ripple clock are combinational.
 */
public class Ttl74191HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates a generator for the synchronous presettable binary up/down counter. */
  public Ttl74191HdlGenerator() {
    super();
    myWires
        .addRegister("state", 4)
        .addWire("next", 4);
    myPorts
        .add(Port.INPUT, "D1", 1, Ttl74191.PORT_INDEX_D1)
        .add(Port.OUTPUT, "Q1", 1, Ttl74191.PORT_INDEX_Q1)
        .add(Port.OUTPUT, "Q0", 1, Ttl74191.PORT_INDEX_Q0)
        .add(Port.INPUT, "CE", 1, Ttl74191.PORT_INDEX_CE)
        .add(Port.INPUT, "UD", 1, Ttl74191.PORT_INDEX_UD)
        .add(Port.OUTPUT, "Q2", 1, Ttl74191.PORT_INDEX_Q2)
        .add(Port.OUTPUT, "Q3", 1, Ttl74191.PORT_INDEX_Q3)
        .add(Port.INPUT, "D3", 1, Ttl74191.PORT_INDEX_D3)
        .add(Port.INPUT, "D2", 1, Ttl74191.PORT_INDEX_D2)
        .add(Port.INPUT, "PL", 1, Ttl74191.PORT_INDEX_PL)
        .add(Port.OUTPUT, "TC", 1, Ttl74191.PORT_INDEX_TC)
        .add(Port.OUTPUT, "RC", 1, Ttl74191.PORT_INDEX_RC)
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl74191.PORT_INDEX_CP)
        .add(Port.INPUT, "D0", 1, Ttl74191.PORT_INDEX_D0);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer().pair("CLK", HdlPorts.CLOCK).pair("tick", HdlPorts.TICK);
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          Q0 <= state(0);
          Q1 <= state(1);
          Q2 <= state(2);
          Q3 <= state(3);
          next <= std_logic_vector(unsigned(state) + 1) {{when}} {{tick}} = '1' {{and}} CE = '0' {{and}} UD = '0' {{else}}
                  std_logic_vector(unsigned(state) - 1) {{when}} {{tick}} = '1' {{and}} CE = '0' {{and}} UD = '1' {{else}}
                  state;
          TC <= '1' {{when}} (UD = '0' {{and}} state = "1111") {{or}} (UD = '1' {{and}} state = "0000") {{else}} '0';
          RC <= '0' {{when}} TC = '1' {{and}} CE = '0' {{and}} {{CLK}} = '0' {{else}} '1';

          count : {{process}}({{CLK}}, PL, D0, D1, D2, D3) {{is}}
             {{begin}}
                {{if}} (PL = '0') {{then}} state <= D3 & D2 & D1 & D0;
                {{elsif}} (rising_edge({{CLK}})) {{then}} state <= next;
                {{end}} {{if}};
             {{end}} {{process}} count;
          """);
    } else {
      contents.add("""
          assign Q0 = state[0];
          assign Q1 = state[1];
          assign Q2 = state[2];
          assign Q3 = state[3];
          assign next = ({{tick}} == 1 && CE == 0 && UD == 0) ? state + 1 :
                        ({{tick}} == 1 && CE == 0 && UD == 1) ? state - 1 : state;
          assign TC = ((UD == 0 && state == 4'b1111) || (UD == 1 && state == 4'b0000)) ? 1'b1 : 1'b0;
          assign RC = (TC == 1 && CE == 0 && {{CLK}} == 0) ? 1'b0 : 1'b1;

          always @(posedge {{CLK}} or negedge PL or D0 or D1 or D2 or D3)
          begin
             if (PL == 0) state <= {D3, D2, D1, D0};
             else state <= next;
          end
          """);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) {
      return false;
    }
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
