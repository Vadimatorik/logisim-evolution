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
 * VHDL and Verilog for one 74196. {@code CP0} toggles {@code Q0} on its falling edge. {@code CP1}
 * advances the divide-by-five section on its falling edge. {@code MR} low clears the outputs, and
 * that mode overrides a low {@code PL}, which copies {@code P0} to {@code P3} for as long as it
 * stays low.
 */
public class Ttl74196HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates a generator for the presettable decade ripple counter. */
  public Ttl74196HdlGenerator() {
    super();
    myWires
        .addRegister("stateA", 1)
        .addRegister("stateB", 3)
        .addWire("nextB", 3);
    // The width of a clock port selects its tick name, as in Ttl7476: 1 is tick, 2 is tick2.
    myPorts
        .add(Port.INPUT, "PL", 1, Ttl74196.PORT_INDEX_PL)
        .add(Port.OUTPUT, "Q2", 1, Ttl74196.PORT_INDEX_Q2)
        .add(Port.INPUT, "P2", 1, Ttl74196.PORT_INDEX_P2)
        .add(Port.INPUT, "P0", 1, Ttl74196.PORT_INDEX_P0)
        .add(Port.OUTPUT, "Q0", 1, Ttl74196.PORT_INDEX_Q0)
        .add(Port.CLOCK, HdlPorts.getClockName(2), 2, Ttl74196.PORT_INDEX_CP1)
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, Ttl74196.PORT_INDEX_CP0)
        .add(Port.OUTPUT, "Q1", 1, Ttl74196.PORT_INDEX_Q1)
        .add(Port.INPUT, "P1", 1, Ttl74196.PORT_INDEX_P1)
        .add(Port.INPUT, "P3", 1, Ttl74196.PORT_INDEX_P3)
        .add(Port.OUTPUT, "Q3", 1, Ttl74196.PORT_INDEX_Q3)
        .add(Port.INPUT, "MR", 1, Ttl74196.PORT_INDEX_MR);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer()
            .pair("CP0", HdlPorts.getClockName(1))
            .pair("CP1", HdlPorts.getClockName(2))
            .pair("tick", HdlPorts.getTickName(1))
            .pair("tick2", HdlPorts.getTickName(2));
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
          Q0 <= stateA;
          Q1 <= stateB(0);
          Q2 <= stateB(1);
          Q3 <= stateB(2);

          nextB <= "001" {{when}} stateB = "000" {{else}}
                   "010" {{when}} stateB = "001" {{else}}
                   "011" {{when}} stateB = "010" {{else}}
                   "100" {{when}} stateB = "011" {{else}}
                   "000";

          divideByTwo : {{process}}({{CP0}}, MR, PL, P0) {{is}}
             {{begin}}
                {{if}} (MR = '0') {{then}} stateA <= '0';
                {{elsif}} (PL = '0') {{then}} stateA <= P0;
                {{elsif}} (falling_edge({{CP0}})) {{then}}
                   {{if}} ({{tick}} = '1') {{then}} stateA <= {{not}} stateA;
                   {{end}} {{if}};
                {{end}} {{if}};
             {{end}} {{process}} divideByTwo;

          divideByFive : {{process}}({{CP1}}, MR, PL, P1, P2, P3) {{is}}
             {{begin}}
                {{if}} (MR = '0') {{then}} stateB <= "000";
                {{elsif}} (PL = '0') {{then}} stateB <= P3 & P2 & P1;
                {{elsif}} (falling_edge({{CP1}})) {{then}}
                   {{if}} ({{tick2}} = '1') {{then}} stateB <= nextB;
                   {{end}} {{if}};
                {{end}} {{if}};
             {{end}} {{process}} divideByFive;
          """);
    } else {
      contents.add("""
          assign Q0 = stateA;
          assign Q1 = stateB[0];
          assign Q2 = stateB[1];
          assign Q3 = stateB[2];

          assign nextB = (stateB == 3'b000) ? 3'b001 :
                         (stateB == 3'b001) ? 3'b010 :
                         (stateB == 3'b010) ? 3'b011 :
                         (stateB == 3'b011) ? 3'b100 :
                                              3'b000;

          always @(negedge {{CP0}} or negedge MR or negedge PL or P0)
          begin
             if (MR == 0) stateA <= 0;
             else if (PL == 0) stateA <= P0;
             else if ({{tick}} == 1) stateA <= ~stateA;
          end

          always @(negedge {{CP1}} or negedge MR or negedge PL or P1 or P2 or P3)
          begin
             if (MR == 0) stateB <= 0;
             else if (PL == 0) stateB <= {P3, P2, P1};
             else if ({{tick2}} == 1) stateB <= nextB;
          end
          """);
    }
    return contents;
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) {
      return false;
    }
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
