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

/** HDL generator for the 74386 quad 2-input exclusive-OR gate. */
public class Ttl74386HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator wired to the 74386 pin order, which is not the 7486 order. */
  public Ttl74386HdlGenerator() {
    super();
    myPorts
        .add(Port.INPUT, "A1", 1, Ttl74386.PORT_INDEX_1A)
        .add(Port.INPUT, "B1", 1, Ttl74386.PORT_INDEX_1B)
        .add(Port.OUTPUT, "Y1", 1, Ttl74386.PORT_INDEX_1Y)
        .add(Port.INPUT, "A2", 1, Ttl74386.PORT_INDEX_2A)
        .add(Port.INPUT, "B2", 1, Ttl74386.PORT_INDEX_2B)
        .add(Port.OUTPUT, "Y2", 1, Ttl74386.PORT_INDEX_2Y)
        .add(Port.INPUT, "A3", 1, Ttl74386.PORT_INDEX_3A)
        .add(Port.INPUT, "B3", 1, Ttl74386.PORT_INDEX_3B)
        .add(Port.OUTPUT, "Y3", 1, Ttl74386.PORT_INDEX_3Y)
        .add(Port.INPUT, "A4", 1, Ttl74386.PORT_INDEX_4A)
        .add(Port.INPUT, "B4", 1, Ttl74386.PORT_INDEX_4B)
        .add(Port.OUTPUT, "Y4", 1, Ttl74386.PORT_INDEX_4Y);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    return LineBuffer.getHdlBuffer()
        .add("{{assign}}Y1{{=}}A1{{xor}}B1;")
        .add("{{assign}}Y2{{=}}A2{{xor}}B2;")
        .add("{{assign}}Y3{{=}}A3{{xor}}B3;")
        .add("{{assign}}Y4{{=}}A4{{xor}}B4;");
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
