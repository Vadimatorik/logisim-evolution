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
 * VHDL and Verilog for the 74293 ripple counter.
 *
 * <p>{@code A} and {@code B} are ordinary inputs. Each has its own falling edge, and the generator's
 * clock-port map binds only the single name {@code clock}, so a second clock port would be left
 * open. Open inputs are pulled low: an unwired counter neither counts nor resets.
 */
public class Ttl74293HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the divide-by-two and divide-by-eight sections. */
  public Ttl74293HdlGenerator() {
    super();
    myWires
        .addRegister("stateA", 1)
        .addRegister("stateB", 3)
        .addWire("nextA", 1)
        .addWire("nextB", 3)
        .addWire("nMR", 1);
    myPorts
        .add(Port.OUTPUT, "QC", 1, Ttl74293.PORT_INDEX_QC)
        .add(Port.OUTPUT, "QB", 1, Ttl74293.PORT_INDEX_QB)
        .add(Port.OUTPUT, "QD", 1, Ttl74293.PORT_INDEX_QD)
        .add(Port.OUTPUT, "QA", 1, Ttl74293.PORT_INDEX_QA)
        .add(Port.INPUT, "A", 1, Ttl74293.PORT_INDEX_A, true)
        .add(Port.INPUT, "B", 1, Ttl74293.PORT_INDEX_B, true)
        .add(Port.INPUT, "R01", 1, Ttl74293.PORT_INDEX_R0_1, true)
        .add(Port.INPUT, "R02", 1, Ttl74293.PORT_INDEX_R0_2, true);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          QA <= stateA;
          QB <= stateB(0);
          QC <= stateB(1);
          QD <= stateB(2);
          nMR <= '0' {{when}} R01 = '1' {{and}} R02 = '1' {{else}} '1';
          nextA <= {{not}} stateA;
          nextB(0) <= {{not}} stateB(0);
          nextB(1) <= stateB(1) {{xor}} stateB(0);
          nextB(2) <= stateB(2) {{xor}} (stateB(1) {{and}} stateB(0));

          ffA : {{process}} (A, nMR) {{is}}
          {{begin}}
             {{if}} (nMR = '0') {{then}} stateA <= '0';
             {{elsif}} (falling_edge(A)) {{then}} stateA <= nextA;
             {{end}} {{if}};
          {{end}} {{process}} ffA;

          ffB : {{process}} (B, nMR) {{is}}
          {{begin}}
             {{if}} (nMR = '0') {{then}} stateB <= "000";
             {{elsif}} (falling_edge(B)) {{then}} stateB <= nextB;
             {{end}} {{if}};
          {{end}} {{process}} ffB;
          """);
    } else {
      contents.add(
          """
          assign QA = stateA;
          assign QB = stateB[0];
          assign QC = stateB[1];
          assign QD = stateB[2];
          assign nMR = ~(R01 & R02);
          assign nextA = ~stateA;
          assign nextB = {stateB[2] ^ (stateB[1] & stateB[0]), stateB[1] ^ stateB[0], ~stateB[0]};

          always @(negedge A or negedge nMR)
          begin
             if (nMR == 0) stateA <= 0;
             else stateA <= nextA;
          end

          always @(negedge B or negedge nMR)
          begin
             if (nMR == 0) stateB <= 0;
             else stateB <= nextB;
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
