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
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

/** VHDL and Verilog for the 8-input NOR/OR function of a 744078. */
public class Ttl744078HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for the 744078. */
  public Ttl744078HdlGenerator() {
    super();
    myPorts
        .add(Port.OUTPUT, "Y", 1, Ttl744078.pinNrToPortNr(Ttl744078.Y))
        .add(Port.INPUT, "A", 1, Ttl744078.pinNrToPortNr(Ttl744078.A))
        .add(Port.INPUT, "B", 1, Ttl744078.pinNrToPortNr(Ttl744078.B))
        .add(Port.INPUT, "C", 1, Ttl744078.pinNrToPortNr(Ttl744078.C))
        .add(Port.INPUT, "D", 1, Ttl744078.pinNrToPortNr(Ttl744078.D))
        .add(Port.INPUT, "E", 1, Ttl744078.pinNrToPortNr(Ttl744078.E))
        .add(Port.INPUT, "F", 1, Ttl744078.pinNrToPortNr(Ttl744078.F))
        .add(Port.INPUT, "G", 1, Ttl744078.pinNrToPortNr(Ttl744078.G))
        .add(Port.INPUT, "H", 1, Ttl744078.pinNrToPortNr(Ttl744078.H))
        .add(Port.OUTPUT, "X", 1, Ttl744078.pinNrToPortNr(Ttl744078.X));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    return LineBuffer.getHdlBuffer()
        .add("{{assign}}Y{{=}}A{{or}}B{{or}}C{{or}}D{{or}}E{{or}}F{{or}}G{{or}}H;")
        .add("{{assign}}X{{=}}{{not}}(A{{or}}B{{or}}C{{or}}D{{or}}E{{or}}F{{or}}G{{or}}H);");
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
