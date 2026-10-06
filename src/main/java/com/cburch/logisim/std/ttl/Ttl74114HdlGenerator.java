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
 * VHDL and Verilog generator for the 74x114 dual J-K flip-flop.
 *
 * <p>Both flip-flops use the one shared falling-edge clock and the one shared clear. Each
 * flip-flop is a single register, so the outputs stay complementary. Clear is checked before
 * preset: when both are low the register is cleared, which does not match the simulator state
 * where both outputs are high.
 */
public class Ttl74114HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the two falling-edge J-K flip-flops. */
  public Ttl74114HdlGenerator() {
    super();
    myWires
        .addRegister("state1", 1)
        .addRegister("state2", 1)
        .addWire("next1", 1)
        .addWire("next2", 1);
    myPorts
        .add(Port.INPUT, "nCLR", 1, Ttl74114.PORT_INDEX_CLR)
        .add(Port.INPUT, "K1", 1, Ttl74114.PORT_INDEX_1K)
        .add(Port.INPUT, "J1", 1, Ttl74114.PORT_INDEX_1J)
        .add(Port.INPUT, "nPR1", 1, Ttl74114.PORT_INDEX_1PR)
        .add(Port.OUTPUT, "Q1", 1, Ttl74114.PORT_INDEX_1Q)
        .add(Port.OUTPUT, "nQ1", 1, Ttl74114.PORT_INDEX_1NQ)
        .add(Port.OUTPUT, "nQ2", 1, Ttl74114.PORT_INDEX_2NQ)
        .add(Port.OUTPUT, "Q2", 1, Ttl74114.PORT_INDEX_2Q)
        .add(Port.INPUT, "nPR2", 1, Ttl74114.PORT_INDEX_2PR)
        .add(Port.INPUT, "J2", 1, Ttl74114.PORT_INDEX_2J)
        .add(Port.INPUT, "K2", 1, Ttl74114.PORT_INDEX_2K)
        .add(Port.CLOCK, HdlPorts.CLOCK, 1, Ttl74114.PORT_INDEX_CLK);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer()
            .pair("CLK", HdlPorts.CLOCK)
            .pair("tick", HdlPorts.TICK);
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
          Q1  <= state1;
          nQ1 <= {{not}}(state1);
          Q2  <= state2;
          nQ2 <= {{not}}(state2);

          next1 <= (J1 {{and}} {{not}}(state1)) {{or}} ({{not}}(K1) {{and}} state1);
          next2 <= (J2 {{and}} {{not}}(state2)) {{or}} ({{not}}(K2) {{and}} state2);

          ff1 : {{process}} ( {{CLK}} , nCLR , nPR1 ) {{is}}
             BEGIN
                {{if}} (nCLR = '0') {{then}} state1 <= '0';
                {{elsif}} (nPR1 = '0') {{then}} state1 <= '1';
                {{elsif}} (falling_edge({{CLK}})) {{then}}
                   {{if}} ({{tick}} = '1') {{then}} state1 <= next1; {{end}} {{if}};
                {{end}} {{if}};
             {{end}} {{process}} ff1;

          ff2 : {{process}} ( {{CLK}} , nCLR , nPR2 ) {{is}}
             BEGIN
                {{if}} (nCLR = '0') {{then}} state2 <= '0';
                {{elsif}} (nPR2 = '0') {{then}} state2 <= '1';
                {{elsif}} (falling_edge({{CLK}})) {{then}}
                   {{if}} ({{tick}} = '1') {{then}} state2 <= next2; {{end}} {{if}};
                {{end}} {{if}};
             {{end}} {{process}} ff2;
          """);
    } else {
      contents.add("""
          assign Q1    = state1;
          assign nQ1   = ~state1;
          assign Q2    = state2;
          assign nQ2   = ~state2;

          assign next1 = (J1 & ~state1) | (~K1 & state1);
          assign next2 = (J2 & ~state2) | (~K2 & state2);

          always @(negedge {{CLK}} or negedge nCLR or negedge nPR1)
          begin
             if (nCLR == 0) state1 <= 0;
             else if (nPR1 == 0) state1 <= 1;
             else if ({{tick}} == 1) state1 <= next1;
          end

          always @(negedge {{CLK}} or negedge nCLR or negedge nPR2)
          begin
             if (nCLR == 0) state2 <= 0;
             else if (nPR2 == 0) state2 <= 1;
             else if ({{tick}} == 1) state2 <= next2;
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
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
