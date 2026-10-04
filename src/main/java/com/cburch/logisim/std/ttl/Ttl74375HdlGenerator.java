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
 * VHDL and Verilog generator for the 74x375 quad transparent latch.
 *
 * <p>Each pair stores its data inputs while its enable is high, the same level-sensitive update
 * the memory library uses for a latch. The enables are not edge clocks, so they stay ordinary
 * inputs and {@code checkForGatedClocks()} stays false. The exporter forces the simulator tick
 * high unless a component is an edge-triggered flip-flop, so this description does not sample a
 * tick: a high enable is already the transparent condition.
 */
public class Ttl74375HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the four level-sensitive latches. */
  public Ttl74375HdlGenerator() {
    super();
    myWires
        .addRegister("s_q1", 1)
        .addRegister("s_q2", 1)
        .addRegister("s_q3", 1)
        .addRegister("s_q4", 1);
    myPorts
        .add(Port.INPUT, "D1", 1, Ttl74375.pinNrToPortNr(Ttl74375.D1))
        .add(Port.OUTPUT, "nQ1", 1, Ttl74375.pinNrToPortNr(Ttl74375.NQ1))
        .add(Port.OUTPUT, "Q1", 1, Ttl74375.pinNrToPortNr(Ttl74375.Q1))
        .add(Port.INPUT, "G12", 1, Ttl74375.pinNrToPortNr(Ttl74375.G12))
        .add(Port.OUTPUT, "Q2", 1, Ttl74375.pinNrToPortNr(Ttl74375.Q2))
        .add(Port.OUTPUT, "nQ2", 1, Ttl74375.pinNrToPortNr(Ttl74375.NQ2))
        .add(Port.INPUT, "D2", 1, Ttl74375.pinNrToPortNr(Ttl74375.D2))
        .add(Port.INPUT, "D3", 1, Ttl74375.pinNrToPortNr(Ttl74375.D3))
        .add(Port.OUTPUT, "nQ3", 1, Ttl74375.pinNrToPortNr(Ttl74375.NQ3))
        .add(Port.OUTPUT, "Q3", 1, Ttl74375.pinNrToPortNr(Ttl74375.Q3))
        .add(Port.INPUT, "G34", 1, Ttl74375.pinNrToPortNr(Ttl74375.G34))
        .add(Port.OUTPUT, "Q4", 1, Ttl74375.pinNrToPortNr(Ttl74375.Q4))
        .add(Port.OUTPUT, "nQ4", 1, Ttl74375.pinNrToPortNr(Ttl74375.NQ4))
        .add(Port.INPUT, "D4", 1, Ttl74375.pinNrToPortNr(Ttl74375.D4));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add(
          """
          Q1  <= s_q1;
          nQ1 <= {{not}}(s_q1);
          Q2  <= s_q2;
          nQ2 <= {{not}}(s_q2);
          Q3  <= s_q3;
          nQ3 <= {{not}}(s_q3);
          Q4  <= s_q4;
          nQ4 <= {{not}}(s_q4);

          latches12 : {{process}}(G12, D1, D2) {{is}}
             {{begin}}
                {{if}} (G12 = '1') {{then}}
                   s_q1 <= D1;
                   s_q2 <= D2;
                {{end}} {{if}};
             {{end}} {{process}} latches12;

          latches34 : {{process}}(G34, D3, D4) {{is}}
             {{begin}}
                {{if}} (G34 = '1') {{then}}
                   s_q3 <= D3;
                   s_q4 <= D4;
                {{end}} {{if}};
             {{end}} {{process}} latches34;
          """);
    } else {
      contents.add(
          """
          assign Q1  = s_q1;
          assign nQ1 = ~s_q1;
          assign Q2  = s_q2;
          assign nQ2 = ~s_q2;
          assign Q3  = s_q3;
          assign nQ3 = ~s_q3;
          assign Q4  = s_q4;
          assign nQ4 = ~s_q4;

          always @(*)
          begin
             if (G12 == 1) begin
                s_q1 <= D1;
                s_q2 <= D2;
             end
             if (G34 == 1) begin
                s_q3 <= D3;
                s_q4 <= D4;
             end
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
