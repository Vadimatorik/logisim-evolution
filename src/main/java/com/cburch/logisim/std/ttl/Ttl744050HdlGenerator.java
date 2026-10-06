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

/** VHDL and Verilog generator for the 744050 hex non-inverting buffer. */
public class Ttl744050HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates a generator whose unconnected inputs are tied low. */
  public Ttl744050HdlGenerator() {
    super();
    myPorts
        .add(Port.OUTPUT, "Y1", 1, Ttl744050.PORT_INDEX_Y1)
        .add(Port.INPUT, "A1", 1, Ttl744050.PORT_INDEX_A1, true)
        .add(Port.OUTPUT, "Y2", 1, Ttl744050.PORT_INDEX_Y2)
        .add(Port.INPUT, "A2", 1, Ttl744050.PORT_INDEX_A2, true)
        .add(Port.OUTPUT, "Y3", 1, Ttl744050.PORT_INDEX_Y3)
        .add(Port.INPUT, "A3", 1, Ttl744050.PORT_INDEX_A3, true)
        .add(Port.INPUT, "A4", 1, Ttl744050.PORT_INDEX_A4, true)
        .add(Port.OUTPUT, "Y4", 1, Ttl744050.PORT_INDEX_Y4)
        .add(Port.INPUT, "A5", 1, Ttl744050.PORT_INDEX_A5, true)
        .add(Port.OUTPUT, "Y5", 1, Ttl744050.PORT_INDEX_Y5)
        .add(Port.INPUT, "A6", 1, Ttl744050.PORT_INDEX_A6, true)
        .add(Port.OUTPUT, "Y6", 1, Ttl744050.PORT_INDEX_Y6);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    return LineBuffer.getHdlBuffer()
        .add("{{assign}}Y1{{=}}A1;")
        .add("{{assign}}Y2{{=}}A2;")
        .add("{{assign}}Y3{{=}}A3;")
        .add("{{assign}}Y4{{=}}A4;")
        .add("{{assign}}Y5{{=}}A5;")
        .add("{{assign}}Y6{{=}}A6;");
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) {
      return false;
    }
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
