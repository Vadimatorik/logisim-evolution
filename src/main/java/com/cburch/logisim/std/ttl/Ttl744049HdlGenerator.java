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

/** VHDL and Verilog for the six independent inverters of a 744049. */
public class Ttl744049HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates an HDL generator. Unconnected inputs are not pulled to zero. */
  public Ttl744049HdlGenerator() {
    super();
    myPorts
        .add(Port.OUTPUT, "Y1", 1, Ttl744049.PORT_Y1)
        .add(Port.INPUT, "A1", 1, Ttl744049.PORT_A1, false)
        .add(Port.OUTPUT, "Y2", 1, Ttl744049.PORT_Y2)
        .add(Port.INPUT, "A2", 1, Ttl744049.PORT_A2, false)
        .add(Port.OUTPUT, "Y3", 1, Ttl744049.PORT_Y3)
        .add(Port.INPUT, "A3", 1, Ttl744049.PORT_A3, false)
        .add(Port.INPUT, "A4", 1, Ttl744049.PORT_A4, false)
        .add(Port.OUTPUT, "Y4", 1, Ttl744049.PORT_Y4)
        .add(Port.INPUT, "A5", 1, Ttl744049.PORT_A5, false)
        .add(Port.OUTPUT, "Y5", 1, Ttl744049.PORT_Y5)
        .add(Port.INPUT, "A6", 1, Ttl744049.PORT_A6, false)
        .add(Port.OUTPUT, "Y6", 1, Ttl744049.PORT_Y6);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          Y1 <= {{not}}(A1);
          Y2 <= {{not}}(A2);
          Y3 <= {{not}}(A3);
          Y4 <= {{not}}(A4);
          Y5 <= {{not}}(A5);
          Y6 <= {{not}}(A6);
          """);
    } else {
      contents.add(
          """
          assign Y1 = ~A1;
          assign Y2 = ~A2;
          assign Y3 = ~A3;
          assign Y4 = ~A4;
          assign Y5 = ~A5;
          assign Y6 = ~A6;
          """);
    }
    return contents.empty();
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) {
      return false;
    }
    return !attrs.getValue(TtlLibrary.VCC_GND);
  }
}
