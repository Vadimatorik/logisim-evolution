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

/** VHDL and Verilog generator for the 74x238 decoder/demultiplexer. */
public class Ttl74238HdlGenerator extends AbstractHdlGeneratorFactory {

  /**
   * Creates an HDL generator. Unconnected active-low enables are pulled high and the active-high
   * enable is pulled low, so an unwired device stays disabled.
   */
  public Ttl74238HdlGenerator() {
    super();
    myWires.addWire("s_enabled", 1);
    myPorts
        .add(Port.INPUT, "A0", 1, Ttl74238.pinNrToPortNr(Ttl74238.A0), true)
        .add(Port.INPUT, "A1", 1, Ttl74238.pinNrToPortNr(Ttl74238.A1), true)
        .add(Port.INPUT, "A2", 1, Ttl74238.pinNrToPortNr(Ttl74238.A2), true)
        .add(Port.INPUT, "nE1", 1, Ttl74238.pinNrToPortNr(Ttl74238.NE1), false)
        .add(Port.INPUT, "nE2", 1, Ttl74238.pinNrToPortNr(Ttl74238.NE2), false)
        .add(Port.INPUT, "E3", 1, Ttl74238.pinNrToPortNr(Ttl74238.E3), true)
        .add(Port.OUTPUT, "Y0", 1, Ttl74238.pinNrToPortNr(Ttl74238.Y0))
        .add(Port.OUTPUT, "Y1", 1, Ttl74238.pinNrToPortNr(Ttl74238.Y1))
        .add(Port.OUTPUT, "Y2", 1, Ttl74238.pinNrToPortNr(Ttl74238.Y2))
        .add(Port.OUTPUT, "Y3", 1, Ttl74238.pinNrToPortNr(Ttl74238.Y3))
        .add(Port.OUTPUT, "Y4", 1, Ttl74238.pinNrToPortNr(Ttl74238.Y4))
        .add(Port.OUTPUT, "Y5", 1, Ttl74238.pinNrToPortNr(Ttl74238.Y5))
        .add(Port.OUTPUT, "Y6", 1, Ttl74238.pinNrToPortNr(Ttl74238.Y6))
        .add(Port.OUTPUT, "Y7", 1, Ttl74238.pinNrToPortNr(Ttl74238.Y7));
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add(
          """
          s_enabled <= ({{not}} nE1) {{and}} ({{not}} nE2) {{and}} E3;
          Y0 <= s_enabled {{and}} ({{not}} A2) {{and}} ({{not}} A1) {{and}} ({{not}} A0);
          Y1 <= s_enabled {{and}} ({{not}} A2) {{and}} ({{not}} A1) {{and}} A0;
          Y2 <= s_enabled {{and}} ({{not}} A2) {{and}} A1 {{and}} ({{not}} A0);
          Y3 <= s_enabled {{and}} ({{not}} A2) {{and}} A1 {{and}} A0;
          Y4 <= s_enabled {{and}} A2 {{and}} ({{not}} A1) {{and}} ({{not}} A0);
          Y5 <= s_enabled {{and}} A2 {{and}} ({{not}} A1) {{and}} A0;
          Y6 <= s_enabled {{and}} A2 {{and}} A1 {{and}} ({{not}} A0);
          Y7 <= s_enabled {{and}} A2 {{and}} A1 {{and}} A0;
          """);
    } else {
      contents.add(
          """
          assign s_enabled = ~nE1 & ~nE2 & E3;
          assign Y0 = s_enabled & ~A2 & ~A1 & ~A0;
          assign Y1 = s_enabled & ~A2 & ~A1 & A0;
          assign Y2 = s_enabled & ~A2 & A1 & ~A0;
          assign Y3 = s_enabled & ~A2 & A1 & A0;
          assign Y4 = s_enabled & A2 & ~A1 & ~A0;
          assign Y5 = s_enabled & A2 & ~A1 & A0;
          assign Y6 = s_enabled & A2 & A1 & ~A0;
          assign Y7 = s_enabled & A2 & A1 & A0;
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
