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
 * VHDL and Verilog generator for the 74x109 dual J-K flip-flop.
 *
 * <p>Each flip-flop is a single register, so the outputs stay complementary. The K input is
 * already inverted relative to a 7476, so the next state is {@code (J AND NOT Q) OR (K AND Q)}.
 * Clear is checked before preset: when both are low the register is cleared, which does not match
 * the simulator state where both outputs are high.
 */
public class Ttl74109HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the two rising-edge J-K flip-flops. */
  public Ttl74109HdlGenerator() {
    super();
    myWires
        .addRegister("state1", 1)
        .addRegister("state2", 1)
        .addWire("next1", 1)
        .addWire("next2", 1);
    myPorts
        .add(Port.INPUT, "nRD1", 1, Ttl74109.PORT_INDEX_1RD)
        .add(Port.INPUT, "J1", 1, Ttl74109.PORT_INDEX_1J)
        .add(Port.INPUT, "K1", 1, Ttl74109.PORT_INDEX_1K)
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, Ttl74109.PORT_INDEX_1CP)
        .add(Port.INPUT, "nSD1", 1, Ttl74109.PORT_INDEX_1SD)
        .add(Port.OUTPUT, "Q1", 1, Ttl74109.PORT_INDEX_1Q)
        .add(Port.OUTPUT, "nQ1", 1, Ttl74109.PORT_INDEX_1NQ)
        .add(Port.OUTPUT, "nQ2", 1, Ttl74109.PORT_INDEX_2NQ)
        .add(Port.OUTPUT, "Q2", 1, Ttl74109.PORT_INDEX_2Q)
        .add(Port.INPUT, "nSD2", 1, Ttl74109.PORT_INDEX_2SD)
        .add(Port.CLOCK, HdlPorts.getClockName(2), 2, Ttl74109.PORT_INDEX_2CP)
        .add(Port.INPUT, "K2", 1, Ttl74109.PORT_INDEX_2K)
        .add(Port.INPUT, "J2", 1, Ttl74109.PORT_INDEX_2J)
        .add(Port.INPUT, "nRD2", 1, Ttl74109.PORT_INDEX_2RD);
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
      contents.addVhdlKeywords().add("""
          Q1  <= state1;
          nQ1 <= {{not}}(state1);
          Q2  <= state2;
          nQ2 <= {{not}}(state2);

          next1 <= (J1 {{and}} {{not}}(state1)) {{or}} (K1 {{and}} state1);
          next2 <= (J2 {{and}} {{not}}(state2)) {{or}} (K2 {{and}} state2);

          ff1 : {{process}} ( {{CLK1}} , nRD1 , nSD1 ) {{is}}
             BEGIN
                {{if}} (nRD1 = '0') {{then}} state1 <= '0';
                {{elsif}} (nSD1 = '0') {{then}} state1 <= '1';
                {{elsif}} (rising_edge({{CLK1}})) {{then}}
                   {{if}} ({{tick1}} = '1') {{then}} state1 <= next1; {{end}} {{if}};
                {{end}} {{if}};
             {{end}} {{process}} ff1;

          ff2 : {{process}} ( {{CLK2}} , nRD2 , nSD2 ) {{is}}
             BEGIN
                {{if}} (nRD2 = '0') {{then}} state2 <= '0';
                {{elsif}} (nSD2 = '0') {{then}} state2 <= '1';
                {{elsif}} (rising_edge({{CLK2}})) {{then}}
                   {{if}} ({{tick2}} = '1') {{then}} state2 <= next2; {{end}} {{if}};
                {{end}} {{if}};
             {{end}} {{process}} ff2;
          """);
    } else {
      contents.add("""
          assign Q1  = state1;
          assign nQ1 = ~state1;
          assign Q2  = state2;
          assign nQ2 = ~state2;

          assign next1 = (J1 & ~state1) | (K1 & state1);
          assign next2 = (J2 & ~state2) | (K2 & state2);

          always @(posedge {{CLK1}} or negedge nRD1 or negedge nSD1)
          begin
             if (nRD1 == 0) state1 <= 0;
             else if (nSD1 == 0) state1 <= 1;
             else if ({{tick1}} == 1) state1 <= next1;
          end

          always @(posedge {{CLK2}} or negedge nRD2 or negedge nSD2)
          begin
             if (nRD2 == 0) state2 <= 0;
             else if (nSD2 == 0) state2 <= 1;
             else if ({{tick2}} == 1) state2 <= next2;
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
