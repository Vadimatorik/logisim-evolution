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

/** VHDL and Verilog generator for the 74x4002 dual 4-input NOR gate. */
public class Ttl744002HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for both NOR gates. */
  public Ttl744002HdlGenerator() {
    super();
    myPorts
        .add(Port.OUTPUT, "Y1", 1, Ttl744002.pinNrToPortNr(Ttl744002.Y1))
        .add(Port.INPUT, "A1", 1, Ttl744002.pinNrToPortNr(Ttl744002.A1))
        .add(Port.INPUT, "B1", 1, Ttl744002.pinNrToPortNr(Ttl744002.B1))
        .add(Port.INPUT, "C1", 1, Ttl744002.pinNrToPortNr(Ttl744002.C1))
        .add(Port.INPUT, "D1", 1, Ttl744002.pinNrToPortNr(Ttl744002.D1))
        .add(Port.INPUT, "A2", 1, Ttl744002.pinNrToPortNr(Ttl744002.A2))
        .add(Port.INPUT, "B2", 1, Ttl744002.pinNrToPortNr(Ttl744002.B2))
        .add(Port.INPUT, "C2", 1, Ttl744002.pinNrToPortNr(Ttl744002.C2))
        .add(Port.INPUT, "D2", 1, Ttl744002.pinNrToPortNr(Ttl744002.D2))
        .add(Port.OUTPUT, "Y2", 1, Ttl744002.pinNrToPortNr(Ttl744002.Y2));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    return LineBuffer.getHdlBuffer()
        .add("{{assign}}Y1{{=}}{{not}}(A1{{or}}B1{{or}}C1{{or}}D1);")
        .add("{{assign}}Y2{{=}}{{not}}(A2{{or}}B2{{or}}C2{{or}}D2);");
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
