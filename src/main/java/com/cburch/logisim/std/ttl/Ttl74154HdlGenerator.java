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

/** VHDL and Verilog generator for the 74x154 decoder/demultiplexer. */
public class Ttl74154HdlGenerator extends AbstractHdlGeneratorFactory {

  /**
   * Creates an HDL generator. Unconnected active-low enables are pulled high, so an unwired device
   * stays disabled. Unconnected address bits are pulled low.
   */
  public Ttl74154HdlGenerator() {
    super();
    myWires.addWire("s_enabled", 1).addWire("s_addr", 4);
    myPorts
        .add(Port.OUTPUT, "Y0", 1, Ttl74154.pinNrToPortNr(Ttl74154.Y0))
        .add(Port.OUTPUT, "Y1", 1, Ttl74154.pinNrToPortNr(Ttl74154.Y1))
        .add(Port.OUTPUT, "Y2", 1, Ttl74154.pinNrToPortNr(Ttl74154.Y2))
        .add(Port.OUTPUT, "Y3", 1, Ttl74154.pinNrToPortNr(Ttl74154.Y3))
        .add(Port.OUTPUT, "Y4", 1, Ttl74154.pinNrToPortNr(Ttl74154.Y4))
        .add(Port.OUTPUT, "Y5", 1, Ttl74154.pinNrToPortNr(Ttl74154.Y5))
        .add(Port.OUTPUT, "Y6", 1, Ttl74154.pinNrToPortNr(Ttl74154.Y6))
        .add(Port.OUTPUT, "Y7", 1, Ttl74154.pinNrToPortNr(Ttl74154.Y7))
        .add(Port.OUTPUT, "Y8", 1, Ttl74154.pinNrToPortNr(Ttl74154.Y8))
        .add(Port.OUTPUT, "Y9", 1, Ttl74154.pinNrToPortNr(Ttl74154.Y9))
        .add(Port.OUTPUT, "Y10", 1, Ttl74154.pinNrToPortNr(Ttl74154.Y10))
        .add(Port.OUTPUT, "Y11", 1, Ttl74154.pinNrToPortNr(Ttl74154.Y11))
        .add(Port.OUTPUT, "Y12", 1, Ttl74154.pinNrToPortNr(Ttl74154.Y12))
        .add(Port.OUTPUT, "Y13", 1, Ttl74154.pinNrToPortNr(Ttl74154.Y13))
        .add(Port.OUTPUT, "Y14", 1, Ttl74154.pinNrToPortNr(Ttl74154.Y14))
        .add(Port.OUTPUT, "Y15", 1, Ttl74154.pinNrToPortNr(Ttl74154.Y15))
        .add(Port.INPUT, "nE1", 1, Ttl74154.pinNrToPortNr(Ttl74154.NE1), false)
        .add(Port.INPUT, "nE2", 1, Ttl74154.pinNrToPortNr(Ttl74154.NE2), false)
        .add(Port.INPUT, "A3", 1, Ttl74154.pinNrToPortNr(Ttl74154.A3), true)
        .add(Port.INPUT, "A2", 1, Ttl74154.pinNrToPortNr(Ttl74154.A2), true)
        .add(Port.INPUT, "A1", 1, Ttl74154.pinNrToPortNr(Ttl74154.A1), true)
        .add(Port.INPUT, "A0", 1, Ttl74154.pinNrToPortNr(Ttl74154.A0), true);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          s_enabled <= ({{not}} nE1) {{and}} ({{not}} nE2);
          s_addr <= A3 & A2 & A1 & A0;
          Y0 <= {{not}}(s_enabled {{and}} (s_addr = "0000"));
          Y1 <= {{not}}(s_enabled {{and}} (s_addr = "0001"));
          Y2 <= {{not}}(s_enabled {{and}} (s_addr = "0010"));
          Y3 <= {{not}}(s_enabled {{and}} (s_addr = "0011"));
          Y4 <= {{not}}(s_enabled {{and}} (s_addr = "0100"));
          Y5 <= {{not}}(s_enabled {{and}} (s_addr = "0101"));
          Y6 <= {{not}}(s_enabled {{and}} (s_addr = "0110"));
          Y7 <= {{not}}(s_enabled {{and}} (s_addr = "0111"));
          Y8 <= {{not}}(s_enabled {{and}} (s_addr = "1000"));
          Y9 <= {{not}}(s_enabled {{and}} (s_addr = "1001"));
          Y10 <= {{not}}(s_enabled {{and}} (s_addr = "1010"));
          Y11 <= {{not}}(s_enabled {{and}} (s_addr = "1011"));
          Y12 <= {{not}}(s_enabled {{and}} (s_addr = "1100"));
          Y13 <= {{not}}(s_enabled {{and}} (s_addr = "1101"));
          Y14 <= {{not}}(s_enabled {{and}} (s_addr = "1110"));
          Y15 <= {{not}}(s_enabled {{and}} (s_addr = "1111"));
          """);
    } else {
      contents.add(
          """
          assign s_enabled = ~nE1 & ~nE2;
          assign s_addr = {A3, A2, A1, A0};
          assign Y0 = ~(s_enabled & (s_addr == 4'b0000));
          assign Y1 = ~(s_enabled & (s_addr == 4'b0001));
          assign Y2 = ~(s_enabled & (s_addr == 4'b0010));
          assign Y3 = ~(s_enabled & (s_addr == 4'b0011));
          assign Y4 = ~(s_enabled & (s_addr == 4'b0100));
          assign Y5 = ~(s_enabled & (s_addr == 4'b0101));
          assign Y6 = ~(s_enabled & (s_addr == 4'b0110));
          assign Y7 = ~(s_enabled & (s_addr == 4'b0111));
          assign Y8 = ~(s_enabled & (s_addr == 4'b1000));
          assign Y9 = ~(s_enabled & (s_addr == 4'b1001));
          assign Y10 = ~(s_enabled & (s_addr == 4'b1010));
          assign Y11 = ~(s_enabled & (s_addr == 4'b1011));
          assign Y12 = ~(s_enabled & (s_addr == 4'b1100));
          assign Y13 = ~(s_enabled & (s_addr == 4'b1101));
          assign Y14 = ~(s_enabled & (s_addr == 4'b1110));
          assign Y15 = ~(s_enabled & (s_addr == 4'b1111));
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
