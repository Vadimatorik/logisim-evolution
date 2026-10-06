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

/** VHDL and Verilog generator for the 74x260 dual 5-input NOR gate. */
public class Ttl74260HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for both NOR gates. */
  public Ttl74260HdlGenerator() {
    super();
    myPorts
        .add(Port.INPUT, "A1", 1, Ttl74260.pinNrToPortNr(Ttl74260.A1))
        .add(Port.INPUT, "B1", 1, Ttl74260.pinNrToPortNr(Ttl74260.B1))
        .add(Port.INPUT, "C1", 1, Ttl74260.pinNrToPortNr(Ttl74260.C1))
        .add(Port.INPUT, "D1", 1, Ttl74260.pinNrToPortNr(Ttl74260.D1))
        .add(Port.INPUT, "E1", 1, Ttl74260.pinNrToPortNr(Ttl74260.E1))
        .add(Port.OUTPUT, "Y1", 1, Ttl74260.pinNrToPortNr(Ttl74260.Y1))
        .add(Port.INPUT, "A2", 1, Ttl74260.pinNrToPortNr(Ttl74260.A2))
        .add(Port.INPUT, "B2", 1, Ttl74260.pinNrToPortNr(Ttl74260.B2))
        .add(Port.INPUT, "C2", 1, Ttl74260.pinNrToPortNr(Ttl74260.C2))
        .add(Port.INPUT, "D2", 1, Ttl74260.pinNrToPortNr(Ttl74260.D2))
        .add(Port.INPUT, "E2", 1, Ttl74260.pinNrToPortNr(Ttl74260.E2))
        .add(Port.OUTPUT, "Y2", 1, Ttl74260.pinNrToPortNr(Ttl74260.Y2));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    return LineBuffer.getHdlBuffer()
        .add("{{assign}}Y1{{=}}{{not}}(A1{{or}}B1{{or}}C1{{or}}D1{{or}}E1);")
        .add("{{assign}}Y2{{=}}{{not}}(A2{{or}}B2{{or}}C2{{or}}D2{{or}}E2);");
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
