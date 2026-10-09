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

/**
 * VHDL and Verilog for one 74HC243. Each channel is a three-state buffer. The two directions
 * cannot both be active, so the buses are not driven against each other.
 */
public class Ttl74243HdlGenerator extends AbstractHdlGeneratorFactory {

  /** Creates a generator for the non-inverting quad transceiver. */
  public Ttl74243HdlGenerator() {
    super();
    myPorts
        .add(Port.INPUT, "OEA", 1, Ttl74243.PORT_INDEX_OEA)
        .add(Port.INPUT, "OEB", 1, Ttl74243.PORT_INDEX_OEB)
        .add(Port.INOUT, "A0", 1, Ttl74243.PORT_INDEX_A0)
        .add(Port.INOUT, "A1", 1, Ttl74243.PORT_INDEX_A1)
        .add(Port.INOUT, "A2", 1, Ttl74243.PORT_INDEX_A2)
        .add(Port.INOUT, "A3", 1, Ttl74243.PORT_INDEX_A3)
        .add(Port.INOUT, "B0", 1, Ttl74243.PORT_INDEX_B0)
        .add(Port.INOUT, "B1", 1, Ttl74243.PORT_INDEX_B1)
        .add(Port.INOUT, "B2", 1, Ttl74243.PORT_INDEX_B2)
        .add(Port.INOUT, "B3", 1, Ttl74243.PORT_INDEX_B3);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().add("""
          B0 <= A0 {{when}} OEA = '0' {{and}} OEB = '0' {{else}} 'Z';
          B1 <= A1 {{when}} OEA = '0' {{and}} OEB = '0' {{else}} 'Z';
          B2 <= A2 {{when}} OEA = '0' {{and}} OEB = '0' {{else}} 'Z';
          B3 <= A3 {{when}} OEA = '0' {{and}} OEB = '0' {{else}} 'Z';
          A0 <= B0 {{when}} OEA = '1' {{and}} OEB = '1' {{else}} 'Z';
          A1 <= B1 {{when}} OEA = '1' {{and}} OEB = '1' {{else}} 'Z';
          A2 <= B2 {{when}} OEA = '1' {{and}} OEB = '1' {{else}} 'Z';
          A3 <= B3 {{when}} OEA = '1' {{and}} OEB = '1' {{else}} 'Z';
          """);
    } else {
      contents.add("""
          assign B0 = (OEA == 0 && OEB == 0) ? A0 : 1'bz;
          assign B1 = (OEA == 0 && OEB == 0) ? A1 : 1'bz;
          assign B2 = (OEA == 0 && OEB == 0) ? A2 : 1'bz;
          assign B3 = (OEA == 0 && OEB == 0) ? A3 : 1'bz;
          assign A0 = (OEA == 1 && OEB == 1) ? B0 : 1'bz;
          assign A1 = (OEA == 1 && OEB == 1) ? B1 : 1'bz;
          assign A2 = (OEA == 1 && OEB == 1) ? B2 : 1'bz;
          assign A3 = (OEA == 1 && OEB == 1) ? B3 : 1'bz;
          """);
    }
    return contents;
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
