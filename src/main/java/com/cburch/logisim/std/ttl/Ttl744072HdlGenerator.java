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

/** VHDL and Verilog generator for the 74x4072 dual 4-input OR gate. */
public class Ttl744072HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator for both OR gates. */
  public Ttl744072HdlGenerator() {
    super();
    myPorts
        .add(Port.OUTPUT, "Y1", 1, Ttl744072.Y1)
        .add(Port.INPUT, "A1", 1, Ttl744072.A1)
        .add(Port.INPUT, "B1", 1, Ttl744072.B1)
        .add(Port.INPUT, "C1", 1, Ttl744072.C1)
        .add(Port.INPUT, "D1", 1, Ttl744072.D1)
        .add(Port.INPUT, "A2", 1, Ttl744072.A2)
        .add(Port.INPUT, "B2", 1, Ttl744072.B2)
        .add(Port.INPUT, "C2", 1, Ttl744072.C2)
        .add(Port.INPUT, "D2", 1, Ttl744072.D2)
        .add(Port.OUTPUT, "Y2", 1, Ttl744072.Y2);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    return LineBuffer.getHdlBuffer()
        .add("{{assign}}Y1{{=}}A1{{or}}B1{{or}}C1{{or}}D1;")
        .add("{{assign}}Y2{{=}}A2{{or}}B2{{or}}C2{{or}}D2;");
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) {
      return false;
    }
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
