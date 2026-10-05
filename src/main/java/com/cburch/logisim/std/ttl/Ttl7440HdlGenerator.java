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

/** VHDL and Verilog generator for the 74x40 dual 4-input NAND buffer. */
public class Ttl7440HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for both NAND buffers. */
  public Ttl7440HdlGenerator() {
    super();
    myPorts
        .add(Port.OUTPUT, "Y1", 1, Ttl7440.pinNrToPortNr(Ttl7440.Y1))
        .add(Port.INPUT, "A1", 1, Ttl7440.pinNrToPortNr(Ttl7440.A1))
        .add(Port.INPUT, "B1", 1, Ttl7440.pinNrToPortNr(Ttl7440.B1))
        .add(Port.INPUT, "C1", 1, Ttl7440.pinNrToPortNr(Ttl7440.C1))
        .add(Port.INPUT, "D1", 1, Ttl7440.pinNrToPortNr(Ttl7440.D1))
        .add(Port.INPUT, "A2", 1, Ttl7440.pinNrToPortNr(Ttl7440.A2))
        .add(Port.INPUT, "B2", 1, Ttl7440.pinNrToPortNr(Ttl7440.B2))
        .add(Port.INPUT, "C2", 1, Ttl7440.pinNrToPortNr(Ttl7440.C2))
        .add(Port.INPUT, "D2", 1, Ttl7440.pinNrToPortNr(Ttl7440.D2))
        .add(Port.OUTPUT, "Y2", 1, Ttl7440.pinNrToPortNr(Ttl7440.Y2));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    return LineBuffer.getHdlBuffer()
        .add("{{assign}}Y1{{=}}{{not}}(A1{{and}}B1{{and}}C1{{and}}D1);")
        .add("{{assign}}Y2{{=}}{{not}}(A2{{and}}B2{{and}}C2{{and}}D2);");
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
