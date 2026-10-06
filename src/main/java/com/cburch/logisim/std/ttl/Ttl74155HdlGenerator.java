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

/** VHDL and Verilog generator for the 74x155 dual 2-line to 4-line decoder/demultiplexer. */
public class Ttl74155HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator whose unconnected inputs sit at the inactive level. */
  public Ttl74155HdlGenerator() {
    super();
    myPorts
        .add(Port.INPUT, "C1", 1, Ttl74155.pinNrToPortNr(Ttl74155.C1))
        .add(Port.INPUT, "G1", 1, Ttl74155.pinNrToPortNr(Ttl74155.G1), false)
        .add(Port.INPUT, "B", 1, Ttl74155.pinNrToPortNr(Ttl74155.B))
        .add(Port.OUTPUT, "Y1_3", 1, Ttl74155.pinNrToPortNr(Ttl74155.Y1_3))
        .add(Port.OUTPUT, "Y1_2", 1, Ttl74155.pinNrToPortNr(Ttl74155.Y1_2))
        .add(Port.OUTPUT, "Y1_1", 1, Ttl74155.pinNrToPortNr(Ttl74155.Y1_1))
        .add(Port.OUTPUT, "Y1_0", 1, Ttl74155.pinNrToPortNr(Ttl74155.Y1_0))
        .add(Port.OUTPUT, "Y2_0", 1, Ttl74155.pinNrToPortNr(Ttl74155.Y2_0))
        .add(Port.OUTPUT, "Y2_1", 1, Ttl74155.pinNrToPortNr(Ttl74155.Y2_1))
        .add(Port.OUTPUT, "Y2_2", 1, Ttl74155.pinNrToPortNr(Ttl74155.Y2_2))
        .add(Port.OUTPUT, "Y2_3", 1, Ttl74155.pinNrToPortNr(Ttl74155.Y2_3))
        .add(Port.INPUT, "A", 1, Ttl74155.pinNrToPortNr(Ttl74155.A))
        .add(Port.INPUT, "G2", 1, Ttl74155.pinNrToPortNr(Ttl74155.G2))
        .add(Port.INPUT, "C2", 1, Ttl74155.pinNrToPortNr(Ttl74155.C2), false);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords();
    }
    contents.add(
        """
        {{assign}} Y1_0 {{=}} {{not}}( {{not}}(B) {{and}} {{not}}(A) {{and}} {{not}}(G1) {{and}} C1 );
        {{assign}} Y1_1 {{=}} {{not}}( {{not}}(B) {{and}} A {{and}} {{not}}(G1) {{and}} C1 );
        {{assign}} Y1_2 {{=}} {{not}}( B {{and}} {{not}}(A) {{and}} {{not}}(G1) {{and}} C1 );
        {{assign}} Y1_3 {{=}} {{not}}( B {{and}} A {{and}} {{not}}(G1) {{and}} C1 );
        {{assign}} Y2_0 {{=}} {{not}}( {{not}}(B) {{and}} {{not}}(A) {{and}} G2 {{and}} {{not}}(C2) );
        {{assign}} Y2_1 {{=}} {{not}}( {{not}}(B) {{and}} A {{and}} G2 {{and}} {{not}}(C2) );
        {{assign}} Y2_2 {{=}} {{not}}( B {{and}} {{not}}(A) {{and}} G2 {{and}} {{not}}(C2) );
        {{assign}} Y2_3 {{=}} {{not}}( B {{and}} A {{and}} G2 {{and}} {{not}}(C2) );
        """);
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) {
      return false;
    }
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
