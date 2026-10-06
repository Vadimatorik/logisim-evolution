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

/** HDL model of the 74x1G00 single 2-input NAND gate. */
public class Ttl741G00HdlGenerator extends AbstractHdlGeneratorFactory {

  public Ttl741G00HdlGenerator() {
    super();
    myPorts
        .add(Port.INPUT, "B", 1, Ttl741G00.PORT_INDEX_B)
        .add(Port.INPUT, "A", 1, Ttl741G00.PORT_INDEX_A)
        .add(Port.OUTPUT, "Y", 1, Ttl741G00.PORT_INDEX_Y);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    return LineBuffer.getHdlBuffer().add("{{assign}}Y{{=}}{{not}}(A{{and}}B);");
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
