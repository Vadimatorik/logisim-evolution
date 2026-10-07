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

/** VHDL and Verilog generator for the 74x239 dual decoder/demultiplexer. */
public class Ttl74239HdlGenerator extends AbstractHdlGeneratorFactory {

  /**
   * Creates an HDL generator. Unconnected active-low enables are pulled high, so an unwired half
   * stays disabled.
   */
  public Ttl74239HdlGenerator() {
    super();
    myWires.addWire("s_enabled1", 1).addWire("s_enabled2", 1);
    myPorts
        .add(Port.INPUT, "nE1", 1, Ttl74239.pinNrToPortNr(Ttl74239.NE1), false)
        .add(Port.INPUT, "A0_1", 1, Ttl74239.pinNrToPortNr(Ttl74239.A0_1), true)
        .add(Port.INPUT, "A1_1", 1, Ttl74239.pinNrToPortNr(Ttl74239.A1_1), true)
        .add(Port.INPUT, "nE2", 1, Ttl74239.pinNrToPortNr(Ttl74239.NE2), false)
        .add(Port.INPUT, "A0_2", 1, Ttl74239.pinNrToPortNr(Ttl74239.A0_2), true)
        .add(Port.INPUT, "A1_2", 1, Ttl74239.pinNrToPortNr(Ttl74239.A1_2), true)
        .add(Port.OUTPUT, "Y0_1", 1, Ttl74239.pinNrToPortNr(Ttl74239.Y0_1))
        .add(Port.OUTPUT, "Y1_1", 1, Ttl74239.pinNrToPortNr(Ttl74239.Y1_1))
        .add(Port.OUTPUT, "Y2_1", 1, Ttl74239.pinNrToPortNr(Ttl74239.Y2_1))
        .add(Port.OUTPUT, "Y3_1", 1, Ttl74239.pinNrToPortNr(Ttl74239.Y3_1))
        .add(Port.OUTPUT, "Y0_2", 1, Ttl74239.pinNrToPortNr(Ttl74239.Y0_2))
        .add(Port.OUTPUT, "Y1_2", 1, Ttl74239.pinNrToPortNr(Ttl74239.Y1_2))
        .add(Port.OUTPUT, "Y2_2", 1, Ttl74239.pinNrToPortNr(Ttl74239.Y2_2))
        .add(Port.OUTPUT, "Y3_2", 1, Ttl74239.pinNrToPortNr(Ttl74239.Y3_2));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          s_enabled1 <= {{not}} nE1;
          Y0_1 <= s_enabled1 {{and}} ({{not}} A1_1) {{and}} ({{not}} A0_1);
          Y1_1 <= s_enabled1 {{and}} ({{not}} A1_1) {{and}} A0_1;
          Y2_1 <= s_enabled1 {{and}} A1_1 {{and}} ({{not}} A0_1);
          Y3_1 <= s_enabled1 {{and}} A1_1 {{and}} A0_1;
          s_enabled2 <= {{not}} nE2;
          Y0_2 <= s_enabled2 {{and}} ({{not}} A1_2) {{and}} ({{not}} A0_2);
          Y1_2 <= s_enabled2 {{and}} ({{not}} A1_2) {{and}} A0_2;
          Y2_2 <= s_enabled2 {{and}} A1_2 {{and}} ({{not}} A0_2);
          Y3_2 <= s_enabled2 {{and}} A1_2 {{and}} A0_2;
          """);
    } else {
      contents.add(
          """
          assign s_enabled1 = ~nE1;
          assign Y0_1 = s_enabled1 & ~A1_1 & ~A0_1;
          assign Y1_1 = s_enabled1 & ~A1_1 & A0_1;
          assign Y2_1 = s_enabled1 & A1_1 & ~A0_1;
          assign Y3_1 = s_enabled1 & A1_1 & A0_1;
          assign s_enabled2 = ~nE2;
          assign Y0_2 = s_enabled2 & ~A1_2 & ~A0_2;
          assign Y1_2 = s_enabled2 & ~A1_2 & A0_2;
          assign Y2_2 = s_enabled2 & A1_2 & ~A0_2;
          assign Y3_2 = s_enabled2 & A1_2 & A0_2;
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
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
