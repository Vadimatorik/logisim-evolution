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

/** VHDL and Verilog generator for the 74HC160 synchronous BCD decade counter. */
public class Ttl74160HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates a generator for the hidden-power pinout. Bit 0 of the state is {@code Q0}. */
  public Ttl74160HdlGenerator() {
    super();
    myWires
        .addWire("curState", 4)
        .addWire("nextState", 4)
        .addWire("loaded", 4)
        .addWire("counted", 4)
        .addWire("n0", 1)
        .addWire("n1", 1)
        .addWire("n2", 1)
        .addWire("n3", 1);
    myPorts
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl74160.PORT_INDEX_CP)
        .add(Port.INPUT, "MR", 1, Ttl74160.PORT_INDEX_MR)
        .add(Port.INPUT, "D0", 1, Ttl74160.PORT_INDEX_D0)
        .add(Port.INPUT, "D1", 1, Ttl74160.PORT_INDEX_D1)
        .add(Port.INPUT, "D2", 1, Ttl74160.PORT_INDEX_D2)
        .add(Port.INPUT, "D3", 1, Ttl74160.PORT_INDEX_D3)
        .add(Port.INPUT, "CEP", 1, Ttl74160.PORT_INDEX_CEP)
        .add(Port.INPUT, "PE", 1, Ttl74160.PORT_INDEX_PE)
        .add(Port.INPUT, "CET", 1, Ttl74160.PORT_INDEX_CET)
        .add(Port.OUTPUT, "Q3", 1, Ttl74160.PORT_INDEX_Q3)
        .add(Port.OUTPUT, "Q2", 1, Ttl74160.PORT_INDEX_Q2)
        .add(Port.OUTPUT, "Q1", 1, Ttl74160.PORT_INDEX_Q1)
        .add(Port.OUTPUT, "Q0", 1, Ttl74160.PORT_INDEX_Q0)
        .add(Port.OUTPUT, "TC", 1, Ttl74160.PORT_INDEX_TC);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer().pair("CLK", HdlPorts.CLOCK).pair("tick", HdlPorts.TICK);
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          Q0 <= curState(0);
          Q1 <= curState(1);
          Q2 <= curState(2);
          Q3 <= curState(3);
          TC <= CET {{and}} curState(0) {{and}} {{not}}(curState(1))
                {{and}} {{not}}(curState(2)) {{and}} curState(3);

          loaded <= D3&D2&D1&D0;
          n0 <= {{not}}(curState(0));
          n1 <= curState(1) {{xor}} (curState(0) {{and}} {{not}}(curState(3)));
          n2 <= curState(2) {{xor}} (curState(0) {{and}} curState(1));
          n3 <= curState(3) {{xor}} ((curState(0) {{and}} curState(1) {{and}} curState(2))
                {{or}} (curState(0) {{and}} curState(3)));
          counted <= n3&n2&n1&n0;

          nextState <= loaded {{when}} {{tick}} = '1' {{and}} PE = '0' {{else}}
                       counted {{when}} {{tick}} = '1' {{and}} PE = '1'
                            {{and}} CEP = '1' {{and}} CET = '1' {{else}}
                       curState;

          dffs : {{process}}({{CLK}}, MR) {{is}}
          {{begin}}
             {{if}} (MR = '0') {{then}} curState <= "0000";
             {{elsif}} (rising_edge({{CLK}})) {{then}} curState <= nextState;
             {{end}} {{if}};
          {{end}} {{process}} dffs;
          """);
    } else {
      contents.add("""
          assign Q0 = curState[0];
          assign Q1 = curState[1];
          assign Q2 = curState[2];
          assign Q3 = curState[3];
          assign TC = CET & curState[0] & ~curState[1] & ~curState[2] & curState[3];
          assign loaded = {D3, D2, D1, D0};
          assign n0 = ~curState[0];
          assign n1 = curState[1] ^ (curState[0] & ~curState[3]);
          assign n2 = curState[2] ^ (curState[0] & curState[1]);
          assign n3 = curState[3] ^ ((curState[0] & curState[1] & curState[2])
                      | (curState[0] & curState[3]));
          assign counted = {n3, n2, n1, n0};
          assign nextState = (tick == 1 && PE == 0) ? loaded :
                             (tick == 1 && PE == 1 && CEP == 1 && CET == 1) ? counted :
                             curState;

          always @(posedge {{CLK}} or negedge MR)
          begin
             if (MR == 0) curState <= 4'b0000;
             else curState <= nextState;
          end
          """);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
